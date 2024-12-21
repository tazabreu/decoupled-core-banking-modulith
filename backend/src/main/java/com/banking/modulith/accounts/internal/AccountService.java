package com.banking.modulith.accounts.internal;

import com.banking.modulith.accounts.Account;
import com.banking.modulith.accounts.AccountEvent;
import com.banking.modulith.accounts.AccountStatus;
import com.banking.modulith.accounts.AccountDTO;
import com.banking.modulith.accounts.AccountOperations;
import com.banking.modulith.accounts.CreateAccountCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
public class AccountService implements AccountOperations {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AccountService(AccountRepository accountRepository, ApplicationEventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Optional<AccountDTO> findById(UUID id) {
        return accountRepository.findById(id).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public List<AccountDTO> findAll() {
        return StreamSupport.stream(accountRepository.findAll().spliterator(), false)
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public AccountDTO create(CreateAccountCommand command) {
        log.info("Account creation request received - Command: {}", command);

        if (accountRepository.existsByDocumentNumber(command.documentNumber())) {
            log.error("Account creation failed - Command: {}, Reason: Document Number Already Exists", command);
            throw new RuntimeException("Account already exists for document number: " + command.documentNumber());
        }

        var account = Account.create(
            command.documentNumber(),
            command.holderName(),
            command.type(),
            command.currency()
        );

        var savedAccount = accountRepository.save(account);
        log.info("Account created successfully - Account: {}", savedAccount);

        eventPublisher.publishEvent(new AccountEvent.AccountCreated(
            savedAccount.id(),
            savedAccount.accountNumber(),
            savedAccount.holderName(),
            savedAccount.documentNumber()
        ));

        return toDTO(savedAccount);
    }

    @Transactional
    public AccountDTO activate(UUID id) {
        var account = accountRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Account activation failed - Account ID: {}, Reason: Not Found", id);
                return new RuntimeException("Account not found: " + id);
            });

        if (account.status() != AccountStatus.PENDING_ACTIVATION) {
            log.error("Account activation failed - Account: {}, Current Status: {}", account, account.status());
            throw new RuntimeException("Account %s cannot be activated - Current status: %s"
                .formatted(id, account.status()));
        }

        var activatedAccount = new Account(
            account.id(),
            account.accountNumber(),
            account.documentNumber(),
            account.holderName(),
            account.type(),
            AccountStatus.ACTIVE,
            account.balance(),
            account.currency(),
            account.createdAt(),
            Instant.now(),
            account.version()
        );

        var savedAccount = accountRepository.save(activatedAccount);
        log.info("Account activated successfully - Account: {}", savedAccount);

        eventPublisher.publishEvent(new AccountEvent.AccountActivated(
            savedAccount.id(),
            savedAccount.accountNumber()
        ));

        return toDTO(savedAccount);
    }

    @Transactional
    public void updateBalance(UUID id, BigDecimal newBalance) {
        var account = accountRepository.findById(id)
            .orElseThrow(() -> {
                log.error("Balance update failed - Account ID: {}, New Balance: {}, Reason: Account Not Found", 
                    id, newBalance);
                return new RuntimeException("Account not found: " + id);
            });

        if (account.status() != AccountStatus.ACTIVE) {
            log.error("Balance update failed - Account: {}, New Balance: {}, Reason: Account Not Active", 
                account, newBalance);
            throw new RuntimeException("Account %s is not active - Current status: %s"
                .formatted(id, account.status()));
        }

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.error("Balance update failed - Account: {}, New Balance: {}, Reason: Negative Balance", 
                account, newBalance);
            throw new RuntimeException("Cannot set negative balance for account %s: %s %s"
                .formatted(id, newBalance, account.currency()));
        }

        var oldBalance = account.balance();
        var updatedAccount = new Account(
            account.id(),
            account.accountNumber(),
            account.documentNumber(),
            account.holderName(),
            account.type(),
            account.status(),
            newBalance,
            account.currency(),
            account.createdAt(),
            Instant.now(),
            account.version()
        );

        var savedAccount = accountRepository.save(updatedAccount);
        
        log.info("Balance updated successfully - Account: {}, Old Balance: {}, New Balance: {}", 
            savedAccount, oldBalance, newBalance);

        eventPublisher.publishEvent(new AccountEvent.BalanceUpdated(
            savedAccount.id(),
            savedAccount.accountNumber(),
            oldBalance,
            savedAccount.balance(),
            savedAccount.currency()
        ));
    }

    @Override
    @Transactional
    public void debit(UUID accountId, BigDecimal amount, String currency, UUID transferId) {
        var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        var newBalance = account.balance().subtract(amount);
        updateBalance(accountId, newBalance);
    }

    @Override
    @Transactional
    public void credit(UUID accountId, BigDecimal amount, String currency, UUID transferId) {
        var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        var newBalance = account.balance().add(amount);
        updateBalance(accountId, newBalance);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateBalance(UUID accountId, BigDecimal requiredAmount, String currency) {
        return accountRepository.findById(accountId)
            .map(account -> account.status() == AccountStatus.ACTIVE 
                && account.balance().compareTo(requiredAmount) >= 0)
            .orElse(false);
    }

    private AccountDTO toDTO(Account account) {
        return new AccountDTO(
            account.id(),
            account.accountNumber(),
            account.documentNumber(),
            account.holderName(),
            account.type(),
            account.status(),
            account.balance(),
            account.currency(),
            account.createdAt().atOffset(OffsetDateTime.now().getOffset()),
            account.updatedAt().atOffset(OffsetDateTime.now().getOffset())
        );
    }
}