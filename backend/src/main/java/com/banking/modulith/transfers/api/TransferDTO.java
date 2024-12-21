package com.banking.modulith.transfers.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TransferDTO(
    UUID id,
    UUID sourceAccountId,
    UUID targetAccountId,
    BigDecimal amount,
    String currency,
    TransferStatus status,
    String description,
    OffsetDateTime requestedAt,
    OffsetDateTime completedAt
) {} 