package com.banking.modulith.accounts;

public record CreateAccountCommand(
    String documentNumber,
    String holderName,
    AccountType type,
    String currency
) {} 