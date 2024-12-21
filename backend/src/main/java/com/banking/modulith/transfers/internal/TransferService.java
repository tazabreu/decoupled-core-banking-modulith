package com.banking.modulith.transfers.internal;

import com.banking.modulith.accounts.AccountOperations;
import com.banking.modulith.transfers.api.CreateTransferCommand;
import com.banking.modulith.transfers.api.Transfer;
import com.banking.modulith.transfers.api.TransferDTO;

import com.banking.modulith.transfers.api.TransferStatus;
import com.banking.modulith.transfers.api.events.TransferStateChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
public class TransferService {
    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    
    private final TransferRepository transferRepository;
    private final AccountOperations accountOperations;
    private final ApplicationEventPublisher eventPublisher;

    public TransferService(
        TransferRepository transferRepository,
        AccountOperations accountOperations,
        ApplicationEventPublisher eventPublisher
    ) {
        this.transferRepository = transferRepository;
        this.accountOperations = accountOperations;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Optional<TransferDTO> findById(UUID id) {
        return transferRepository.findById(id).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<TransferDTO> findAll() {
        return StreamSupport.stream(transferRepository.findAll().spliterator(), false)
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public TransferDTO create(CreateTransferCommand command) {
        // Basic validation
        if (command.sourceAccountId().equals(command.targetAccountId())) {
            throw new IllegalArgumentException("Source and target accounts must be different");
        }
        if (command.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        // Validate source account has sufficient funds
        if (!accountOperations.validateBalance(command.sourceAccountId(), command.amount(), command.currency())) {
            throw new IllegalStateException("Insufficient funds or invalid source account");
        }

        // Create transfer in PENDING state
        var transfer = Transfer.initiate(command);
        var savedTransfer = transferRepository.save(transfer);

        // Publish event to trigger async processing
        eventPublisher.publishEvent(new TransferStateChanged.TransferInitiated(
            savedTransfer.id(),
            savedTransfer.sourceAccountId(),
            savedTransfer.targetAccountId(),
            savedTransfer.amount(),
            savedTransfer.currency()
        ));

        return toDTO(savedTransfer);
    }

    // This will be called by the event listener
    @Transactional
    public TransferDTO processDebit(UUID transferId) {
        var transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));

        try {
            accountOperations.debit(
                transfer.sourceAccountId(), 
                transfer.amount(), 
                transfer.currency(),
                transfer.id()
            );

            var debitedTransfer = transfer.markDebited();
            var savedTransfer = transferRepository.save(debitedTransfer);

            eventPublisher.publishEvent(new TransferStateChanged.TransferDebited(
                savedTransfer.id(),
                savedTransfer.sourceAccountId(),
                savedTransfer.amount(),
                savedTransfer.currency()
            ));

            return toDTO(savedTransfer);
        } catch (Exception e) {
            handleTransferFailure(transfer.id(), "Debit failed: " + e.getMessage());
            throw e;
        }
    }

    @Transactional
    public TransferDTO processCredit(UUID transferId) {
        var transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));

        try {
            accountOperations.credit(
                transfer.targetAccountId(), 
                transfer.amount(), 
                transfer.currency(),
                transfer.id()
            );

            var completedTransfer = transfer.complete();
            var savedTransfer = transferRepository.save(completedTransfer);

            eventPublisher.publishEvent(new TransferStateChanged.TransferCompleted(
                savedTransfer.id(),
                savedTransfer.sourceAccountId(),
                savedTransfer.targetAccountId(),
                savedTransfer.amount(),
                savedTransfer.currency()
            ));

            return toDTO(savedTransfer);
        } catch (Exception e) {
            // If credit fails, we need to reverse the debit
            handleTransferFailure(transfer.id(), "Credit failed: " + e.getMessage());
            compensateDebit(transfer);
            throw e;
        }
    }

    private void compensateDebit(Transfer transfer) {
        try {
            accountOperations.credit( // Reverse the debit
                transfer.sourceAccountId(),
                transfer.amount(),
                transfer.currency(),
                transfer.id()
            );

            var compensatedTransfer = transfer.markCompensated("Credit failed, debit reversed");
            transferRepository.save(compensatedTransfer);

            eventPublisher.publishEvent(new TransferStateChanged.TransferCompensated(
                transfer.id(),
                transfer.sourceAccountId(),
                transfer.amount(),
                transfer.currency()
            ));
        } catch (Exception e) {
            log.error("Compensation failed for transfer {} - Manual intervention needed", transfer.id(), e);
            // Here we could notify operations team or create an incident
        }
    }

    @Transactional
    public void handleTransferFailure(UUID transferId, String reason) {
        var transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));

        var failedTransfer = transfer.fail(reason);
        transferRepository.save(failedTransfer);
        
        // If the transfer was DEBITED, we need to compensate
        if (transfer.status() == TransferStatus.DEBITED) {
            compensateDebit(transfer);
        }

        eventPublisher.publishEvent(new TransferStateChanged.TransferFailed(
            transfer.id(),
            reason
        ));
    }

    @Transactional
    public TransferDTO complete(UUID transferId) {
        var transfer = transferRepository.findById(transferId)
            .orElseThrow(() -> new RuntimeException("Transfer not found: " + transferId));

        if (transfer.status() != TransferStatus.DEBITED) {
            throw new IllegalStateException("Transfer is not debited");
        }

        return processCredit(transferId);
    }

    private TransferDTO toDTO(Transfer transfer) {
        return new TransferDTO(
            transfer.id(),
            transfer.sourceAccountId(),
            transfer.targetAccountId(),
            transfer.amount(),
            transfer.currency(),
            transfer.status(),
            transfer.description(),
            transfer.requestedAt().atOffset(OffsetDateTime.now().getOffset()),
            Optional.ofNullable(transfer.completedAt()).map(completedAt -> completedAt.atOffset(OffsetDateTime.now().getOffset())).orElse(null)
        );
    }
}