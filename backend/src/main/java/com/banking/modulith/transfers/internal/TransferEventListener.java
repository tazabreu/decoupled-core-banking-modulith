package com.banking.modulith.transfers.internal;

import com.banking.modulith.transfers.api.events.TransferStateChanged;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

// TODO: where is the traceId and spanId?
// TODO: handle chaos / disaster scenarios: eg: DB Out?
// TODO: how to improve this code so that it can work in horizontally-scaled modulith scenarios?
// TODO: can we trace/test the performance of this modulith? Is it slower due to the fact of having externalized event_publication? Can we speed it up with Redis or something?
// TODO: how to add correlation?
@Component
class TransferEventListener {
    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);
    private static final String DEBIT_QUEUE_KEY = "transfer:debit:queue";
    private static final String CREDIT_QUEUE_KEY = "transfer:credit:queue";
    private static final String LOCK_KEY_PREFIX = "transfer:lock:";

    @Value("${service.max.batch.size:10}")
    private int maxBatchSize;

    @Value("${service.lock.timeout:10}")
    private int lockTimeoutSeconds;

    private final TransferService transferService;
    private final RedisTemplate<String, TransferBatchItem> redisTemplate;
    private final RedisLockRegistry lockRegistry;

    TransferEventListener(
            TransferService transferService,
            RedisTemplate<String, TransferBatchItem> redisTemplate,
            RedisLockRegistry lockRegistry) {
        this.transferService = transferService;
        this.redisTemplate = redisTemplate;
        this.lockRegistry = lockRegistry;
    }

    record TransferBatchItem(UUID transferId, int retryCount, TransferType type) {
        enum TransferType { DEBIT, CREDIT }
    }

    @Retry(name = "queue-operation")
    @CircuitBreaker(name = "queue-operation")
    @RateLimiter(name = "queue-operation")
    @ApplicationModuleListener
    public void onTransferInitiated(TransferStateChanged.TransferInitiated event) {
        String lockKey = LOCK_KEY_PREFIX + "initiated:" + event.transferId();
        Lock lock = lockRegistry.obtain(lockKey);

        try {
            if (lock.tryLock(lockTimeoutSeconds, TimeUnit.SECONDS)) {
                try {
                    var item = new TransferBatchItem(
                            event.transferId(),
                            0,
                            TransferBatchItem.TransferType.DEBIT
                    );
                    redisTemplate.opsForList().rightPush(DEBIT_QUEUE_KEY, item);
                    log.debug("Queued debit for transfer: {}", event.transferId());
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Failed to acquire lock for transfer initiation: {}", event.transferId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock for transfer: {}", event.transferId());
        }
    }

    @Retry(name = "queue-operation")
    @CircuitBreaker(name = "queue-operation")
    @RateLimiter(name = "queue-operation")
    @ApplicationModuleListener
    public void onTransferDebited(TransferStateChanged.TransferDebited event) {
        String lockKey = LOCK_KEY_PREFIX + "debited:" + event.transferId();
        Lock lock = lockRegistry.obtain(lockKey);

        try {
            if (lock.tryLock(lockTimeoutSeconds, TimeUnit.SECONDS)) {
                try {
                    var item = new TransferBatchItem(
                            event.transferId(),
                            0,
                            TransferBatchItem.TransferType.CREDIT
                    );
                    redisTemplate.opsForList().rightPush(CREDIT_QUEUE_KEY, item);
                    log.debug("Queued credit for transfer: {}", event.transferId());
                } finally {
                    lock.unlock();
                }
            } else {
                log.warn("Failed to acquire lock for transfer debit: {}", event.transferId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock for transfer: {}", event.transferId());
        }
    }

    @Scheduled(fixedDelayString = "${service.max.batch.wait.time:1000}")
    @Bulkhead(name = "batch-processing")
    @TimeLimiter(name = "batch-processing")
    public void processDebitBatch() {
        String lockKey = LOCK_KEY_PREFIX + "batch:debit";
        Lock lock = lockRegistry.obtain(lockKey);

        try {
            if (lock.tryLock(lockTimeoutSeconds, TimeUnit.SECONDS)) {
                try {
                    processBatch(DEBIT_QUEUE_KEY);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock for debit batch processing");
        }
    }

    @Scheduled(fixedDelayString = "${service.max.batch.wait.time:1000}")
    @Bulkhead(name = "batch-processing")
    @TimeLimiter(name = "batch-processing")
    public void processCreditBatch() {
        String lockKey = LOCK_KEY_PREFIX + "batch:credit";
        Lock lock = lockRegistry.obtain(lockKey);

        try {
            if (lock.tryLock(lockTimeoutSeconds, TimeUnit.SECONDS)) {
                try {
                    processBatch(CREDIT_QUEUE_KEY);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock for credit batch processing");
        }
    }

    private void processBatch(String queueKey) {
        List<TransferBatchItem> batch = new ArrayList<>();
        for (int i = 0; i < maxBatchSize; i++) {
            TransferBatchItem item = redisTemplate.opsForList().leftPop(queueKey);
            if (item == null) break;
            batch.add(item);
        }

        if (!batch.isEmpty()) {
            processBatchItems(batch);
        }
    }

    @Transactional
    @Retry(name = "transfer-processing")
    @CircuitBreaker(name = "transfer-processing")
    protected void processBatchItems(List<TransferBatchItem> batch) {
        batch.forEach(this::processTransferWithLock);
    }

    private void processTransferWithLock(TransferBatchItem item) {
        String lockKey = LOCK_KEY_PREFIX + "process:" + item.transferId();
        Lock lock = lockRegistry.obtain(lockKey);

        try {
            if (lock.tryLock(lockTimeoutSeconds, TimeUnit.SECONDS)) {
                try {
                    processTransfer(item);
                } finally {
                    lock.unlock();
                }
            } else {
                handleLockTimeout(item);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock for processing transfer: {}",
                    item.transferId());
        }
    }

    private void processTransfer(TransferBatchItem item) {
        try {
            switch (item.type()) {
                case DEBIT -> transferService.processDebit(item.transferId());
                case CREDIT -> transferService.processCredit(item.transferId());
            }
            log.debug("Processed {} for transfer: {}", item.type(), item.transferId());

        } catch (OptimisticLockingFailureException e) {
            handleOptimisticLockingFailure(item);
        } catch (Exception e) {
            log.error("Error processing transfer: {}", item.transferId(), e);
            handleTransferFailure(item.transferId(), e.getMessage());
        }
    }

    private void handleLockTimeout(TransferBatchItem item) {
        if (item.retryCount() < 3) {
            requeueItem(item);
        } else {
            handleTransferFailure(item.transferId(),
                    "Failed to acquire lock after maximum retries");
        }
    }

    private void handleOptimisticLockingFailure(TransferBatchItem item) {
        if (item.retryCount() < 3) {
            requeueItem(item);
        } else {
            handleTransferFailure(item.transferId(),
                    "Max retries exceeded due to concurrent modifications");
        }
    }

    private void requeueItem(TransferBatchItem item) {
        log.warn("Requeuing transfer: {}, retry attempt: {}",
                item.transferId(), item.retryCount() + 1);

        var retryItem = new TransferBatchItem(
                item.transferId(),
                item.retryCount() + 1,
                item.type()
        );

        String queueKey = item.type() == TransferBatchItem.TransferType.DEBIT
                ? DEBIT_QUEUE_KEY
                : CREDIT_QUEUE_KEY;

        redisTemplate.opsForList().rightPush(queueKey, retryItem);
    }

    @ApplicationModuleListener
    public void onTransferFailed(TransferStateChanged.TransferFailed event) {
        log.error("Transfer failed: {}, reason: {}", event.transferId(), event.reason());
    }

    private void handleTransferFailure(UUID transferId, String reason) {
        log.error("Transfer processing failed - Transfer: {}, Reason: {}", transferId, reason);
        transferService.handleTransferFailure(transferId, reason);
    }
}