package com.banking.modulith.transfers.internal;

import com.banking.modulith.transfers.api.events.TransferStateChanged;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
class TransferEventListener {
    private static final Logger log = LoggerFactory.getLogger(TransferEventListener.class);
    private final TransferService transferService;

    TransferEventListener(TransferService transferService) {
        this.transferService = transferService;
    }

    @Retry(name = "transfer-processing", fallbackMethod = "handleTransferProcessingFailure")
    @CircuitBreaker(name = "transfer-processing", fallbackMethod = "handleTransferProcessingFailure")
    @ApplicationModuleListener
    public void onTransferInitiated(TransferStateChanged.TransferInitiated event) {
        log.info("Processing debit for transfer: {}", event.transferId());
        transferService.processDebit(event.transferId());
    }

    @Retry(name = "transfer-processing", fallbackMethod = "handleTransferProcessingFailure")
    @CircuitBreaker(name = "transfer-processing", fallbackMethod = "handleTransferProcessingFailure")
    @ApplicationModuleListener
    public void onTransferDebited(TransferStateChanged.TransferDebited event) {
        log.info("Processing credit for transfer: {}", event.transferId());
        transferService.processCredit(event.transferId());
    }

    @ApplicationModuleListener
    public void onTransferFailed(TransferStateChanged.TransferFailed event) {
        log.error("Transfer failed: {}, reason: {}", event.transferId(), event.reason());
        // Could implement escalation logic here
    }

    private void handleTransferProcessingFailure(TransferStateChanged.TransferInitiated event, Exception e) {
        log.error("Transfer processing failed after retries - Transfer: {}, Error: {}", 
            event.transferId(), e.getMessage(), e);
        transferService.handleTransferFailure(event.transferId(), 
            "Processing failed after retries: " + e.getMessage());
    }

    private void handleTransferProcessingFailure(TransferStateChanged.TransferDebited event, Exception e) {
        log.error("Transfer credit failed after retries - Transfer: {}, Error: {}", 
            event.transferId(), e.getMessage(), e);
        transferService.handleTransferFailure(event.transferId(), 
            "Credit processing failed after retries: " + e.getMessage());
    }
} 