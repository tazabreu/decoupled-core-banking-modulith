package com.banking.modulith.transfers.api;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransferCommand(
    UUID sourceAccountId,
    UUID targetAccountId,
    BigDecimal amount,
    String currency,
    String description
) {} 