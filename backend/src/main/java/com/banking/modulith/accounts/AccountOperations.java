package com.banking.modulith.accounts;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountOperations {
    boolean validateBalance(UUID accountId, BigDecimal amount, String currency);
    void debit(UUID accountId, BigDecimal amount, String currency, UUID transferId);
    void credit(UUID accountId, BigDecimal amount, String currency, UUID transferId);
} 