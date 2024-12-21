package com.banking.modulith.transfers.api.events;

import java.math.BigDecimal;
import java.util.UUID;

public sealed interface TransferStateChanged {
    record TransferInitiated(
        UUID transferId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        String currency
    ) implements TransferStateChanged {}

    record TransferValidated(
        UUID transferId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        String currency
    ) implements TransferStateChanged {}

    record TransferCompleted(
        UUID transferId,
        UUID sourceAccountId,
        UUID targetAccountId,
        BigDecimal amount,
        String currency
    ) implements TransferStateChanged {}

    record TransferFailed(
        UUID transferId,
        String reason
    ) implements TransferStateChanged {}

    record TransferDebited(
        UUID transferId,
        UUID sourceAccountId,
        BigDecimal amount,
        String currency
    ) implements TransferStateChanged {}

    record TransferCompensated(
        UUID transferId,
        UUID sourceAccountId,
        BigDecimal amount,
        String currency
    ) implements TransferStateChanged {}
} 