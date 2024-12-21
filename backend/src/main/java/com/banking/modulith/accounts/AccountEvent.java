package com.banking.modulith.accounts;

import java.math.BigDecimal;
import java.util.UUID;

sealed public interface AccountEvent {
    record AccountCreated(
            UUID accountId,
            String accountNumber,
            String holderName,
            String documentNumber
    ) implements AccountEvent {}

    record AccountActivated(
            UUID accountId,
            String accountNumber
    ) implements AccountEvent {}

    record BalanceUpdated(
            UUID accountId,
            String accountNumber,
            BigDecimal oldBalance,
            BigDecimal newBalance,
            String currency
    ) implements AccountEvent {}
}