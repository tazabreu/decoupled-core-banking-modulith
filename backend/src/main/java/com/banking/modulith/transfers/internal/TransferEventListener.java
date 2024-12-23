package com.banking.modulith.transfers.internal;

import com.banking.modulith.transfers.api.events.TransferStateChanged;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO: handle chaos / disaster scenarios: eg: DB Out?
// TODO: how to improve this code so that it can work in horizontally-scaled modulith scenarios?
// TODO: can we trace/test the performance of this modulith? Is it slower due to the fact of having externalized event_publication? Can we speed it up with Redis or something?
@Component
class TransferEventListener {
    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);
    @Value("${service.max.batch.size:10}")
    private int MAX_BATCH_SIZE;

    private final TransferService transferService;
    private final ConcurrentLinkedQueue<BatchItem> debitQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<BatchItem> creditQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isProcessingDebits = new AtomicBoolean(false);
    private final AtomicBoolean isProcessingCredits = new AtomicBoolean(false);

    TransferEventListener(TransferService transferService) {
        this.transferService = transferService;
    }

    private record BatchItem(UUID transferId, int retryCount) {}

    @Retry(name = "transfer-processing", fallbackMethod = "handleTransferProcessingFailure")
    @CircuitBreaker(name = "transfer-processing", fallbackMethod = "handleTransferProcessingFailure")
    @ApplicationModuleListener
    public void onTransferInitiated(TransferStateChanged.TransferInitiated event) {
        log.debug("Queueing debit for transfer: {}", event.transferId());
        debitQueue.offer(new BatchItem(event.transferId(), 0));
    }

    @Retry(name = "transfer-processing", fallbackMethod = "handleTransferProcessingFailure")
    @CircuitBreaker(name = "transfer-processing", fallbackMethod = "handleTransferProcessingFailure")
    @ApplicationModuleListener
    public void onTransferDebited(TransferStateChanged.TransferDebited event) {
        log.debug("Queueing credit for transfer: {}", event.transferId());
        creditQueue.offer(new BatchItem(event.transferId(), 0));
    }

    @Scheduled(fixedDelayString = "${service.max.batch.wait.time:1000}")
    public void processDebitBatch() {
        if (!isProcessingDebits.compareAndSet(false, true)) {
            return; // Another thread is already processing
        }

        try {
            List<BatchItem> batch = new ArrayList<>();
            while (batch.size() < MAX_BATCH_SIZE && !debitQueue.isEmpty()) {
                BatchItem item = debitQueue.poll();
                if (item != null) {
                    batch.add(item);
                }
            }

            if (!batch.isEmpty()) {
                processBatchWithRetry(batch, true);
            }
        } finally {
            isProcessingDebits.set(false);
        }
    }

    @Scheduled(fixedDelayString = "${service.max.batch.wait.time:1000}")
    public void processCreditBatch() {
        if (!isProcessingCredits.compareAndSet(false, true)) {
            return; // Another thread is already processing
        }

        try {
            List<BatchItem> batch = new ArrayList<>();
            while (batch.size() < MAX_BATCH_SIZE && !creditQueue.isEmpty()) {
                BatchItem item = creditQueue.poll();
                if (item != null) {
                    batch.add(item);
                }
            }

            if (!batch.isEmpty()) {
                processBatchWithRetry(batch, false);
            }
        } finally {
            isProcessingCredits.set(false);
        }
    }

    @Transactional
    protected void processBatchWithRetry(List<BatchItem> batch, boolean isDebit) {
        List<BatchItem> failedItems = new ArrayList<>();

        for (BatchItem item : batch) {
            try {
                if (isDebit) {
                    transferService.processDebit(item.transferId());
                } else {
                    transferService.processCredit(item.transferId());
                }
                log.debug("Successfully processed {} for transfer: {}", 
                    isDebit ? "debit" : "credit", item.transferId());
                
            } catch (OptimisticLockingFailureException e) {
                // Retry logic for optimistic locking failures
                if (item.retryCount() < 3) {
                    log.warn("Optimistic locking failure for transfer: {}, retry attempt: {}", 
                        item.transferId(), item.retryCount() + 1);
                    failedItems.add(new BatchItem(item.transferId(), item.retryCount() + 1));
                } else {
                    log.error("Max retries exceeded for transfer: {}", item.transferId());
                    handleTransferFailure(item.transferId(), 
                        "Max retries exceeded due to concurrent modifications");
                }
            } catch (Exception e) {
                log.error("Error processing transfer: {}", item.transferId(), e);
                handleTransferFailure(item.transferId(), e.getMessage());
            }
        }

        // Requeue failed items for retry
        if (!failedItems.isEmpty()) {
            if (isDebit) {
                failedItems.forEach(debitQueue::offer);
            } else {
                failedItems.forEach(creditQueue::offer);
            }
        }
    }

    @ApplicationModuleListener
    public void onTransferFailed(TransferStateChanged.TransferFailed event) {
        log.error("Transfer failed: {}, reason: {}", event.transferId(), event.reason());
        // Could implement escalation logic here
    }

    private void handleTransferFailure(UUID transferId, String reason) {
        log.error("Transfer processing failed - Transfer: {}, Reason: {}", transferId, reason);
        transferService.handleTransferFailure(transferId, reason);
    }

} 