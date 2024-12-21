package com.banking.modulith.transfers.api;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("transfers")
public record Transfer(
        @Id UUID id,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        String currency,
        TransferStatus status,
        String description,
        Instant requestedAt,
        Instant completedAt,
        @Version Integer version
) {
    public static Transfer initiate(CreateTransferCommand command) {
        return new Transfer(
                UUID.randomUUID(),
                command.sourceAccountId(),
                command.targetAccountId(),
                command.amount(),
                command.currency(),
                TransferStatus.PENDING,
                command.description(),
                Instant.now(),
                null,
                null
        );
    }

    public Transfer markDebited() {
        if (status != TransferStatus.PENDING) {
            throw new IllegalStateException("Transfer is not pending");
        }
        return new Transfer(
                id, sourceAccountId, targetAccountId, amount, currency,
                TransferStatus.DEBITED, description, requestedAt,
                null, version
        );
    }

    public Transfer complete() {
        if (status != TransferStatus.DEBITED) {
            throw new IllegalStateException("Transfer is not debited");
        }
        return new Transfer(
                id, sourceAccountId, targetAccountId, amount, currency,
                TransferStatus.COMPLETED, description, requestedAt,
                Instant.now(), version
        );
    }

    public Transfer markCompensated(String reason) {
        return new Transfer(
                id, sourceAccountId, targetAccountId, amount, currency,
                TransferStatus.COMPENSATED, reason, requestedAt,
                Instant.now(), version
        );
    }

    public Transfer fail(String reason) {
        return new Transfer(
                id, sourceAccountId, targetAccountId, amount, currency,
                TransferStatus.FAILED, reason, requestedAt,
                Instant.now(), version
        );
    }
}

