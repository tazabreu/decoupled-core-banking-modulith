package com.banking.modulith.accounts.internal;

import com.banking.modulith.accounts.Account;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface AccountRepository extends CrudRepository<Account, UUID> {
    boolean existsByDocumentNumber(String documentNumber);
    boolean existsByAccountNumber(String accountNumber);
}