package com.banking.modulith.accounts;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountDTO(
    UUID id,
    String accountNumber,
    String documentNumber, 
    String holderName,
    AccountType type,
    AccountStatus status,
    BigDecimal balance,
    String currency,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {} 