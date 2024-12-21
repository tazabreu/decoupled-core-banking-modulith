package com.banking.modulith.accounts;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Table("accounts")
public record Account(
        @Id UUID id,
        String accountNumber,
        String documentNumber,
        String holderName,
        AccountType type,
        AccountStatus status,
        BigDecimal balance,
        String currency,
        Instant createdAt,
        Instant updatedAt,
        @Version Integer version
) {
    public static Account create(String documentNumber, String holderName, AccountType type, String currency) {
        return new Account(
                UUID.randomUUID(),
                generateAccountNumber(),
                documentNumber,
                holderName,
                type,
                AccountStatus.PENDING_ACTIVATION,
                BigDecimal.ZERO,
                currency,
                Instant.now(),
                Instant.now(),
                null
        );
    }

    private static String generateAccountNumber() {
        return String.format("%010d", System.nanoTime() % 10000000000L);
    }
}

