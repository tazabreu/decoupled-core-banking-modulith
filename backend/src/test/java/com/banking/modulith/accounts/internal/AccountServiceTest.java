package com.banking.modulith.accounts.internal;

import com.banking.modulith.accounts.Account;
import com.banking.modulith.accounts.AccountEvent;
import com.banking.modulith.accounts.AccountStatus;
import com.banking.modulith.accounts.AccountType;
import com.banking.modulith.accounts.CreateAccountCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AccountService accountService;

    @Test
    void create_ShouldCreateAccount_WhenValid() {
        var command = new CreateAccountCommand(
            "12345678900",
            "John Doe",
            AccountType.CHECKING,
            "USD"
        );

        when(accountRepository.existsByDocumentNumber(command.documentNumber())).thenReturn(false);
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = accountService.create(command);

        assertNotNull(result.id());
        assertEquals(command.documentNumber(), result.documentNumber());
        assertEquals(command.holderName(), result.holderName());
        assertEquals(command.type(), result.type());
        assertEquals(command.currency(), result.currency());
        assertEquals(AccountStatus.PENDING_ACTIVATION, result.status());
        assertEquals(BigDecimal.ZERO, result.balance());

        verify(eventPublisher).publishEvent(any(AccountEvent.AccountCreated.class));
    }

    @Test
    void create_ShouldThrowException_WhenDocumentNumberExists() {
        var command = new CreateAccountCommand(
            "12345678900",
            "John Doe",
            AccountType.CHECKING,
            "USD"
        );

        when(accountRepository.existsByDocumentNumber(command.documentNumber())).thenReturn(true);

        assertThrows(RuntimeException.class, () -> accountService.create(command));

        verify(accountRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void activate_ShouldActivateAccount_WhenPending() {
        var id = UUID.randomUUID();
        var account = new Account(
            id,
            "1234567890",
            "12345678900",
            "John Doe",
            AccountType.CHECKING,
            AccountStatus.PENDING_ACTIVATION,
            BigDecimal.ZERO,
            "USD",
            Instant.now(),
            Instant.now(),
            0
        );

        when(accountRepository.findById(id)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var result = accountService.activate(id);

        assertEquals(AccountStatus.ACTIVE, result.status());

        var eventCaptor = ArgumentCaptor.forClass(AccountEvent.AccountActivated.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        var event = eventCaptor.getValue();
        assertEquals(id, event.accountId());
        assertEquals(account.accountNumber(), event.accountNumber());
    }

    @Test
    void activate_ShouldThrowException_WhenNotPending() {
        var id = UUID.randomUUID();
        var account = new Account(
            id,
            "1234567890",
            "12345678900",
            "John Doe",
            AccountType.CHECKING,
            AccountStatus.ACTIVE,
            BigDecimal.ZERO,
            "USD",
            Instant.now(),
            Instant.now(),
            0
        );

        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        assertThrows(RuntimeException.class, () -> accountService.activate(id));

        verify(accountRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateBalance_ShouldUpdateBalance_WhenValid() {
        var id = UUID.randomUUID();
        var account = new Account(
            id,
            "1234567890",
            "12345678900",
            "John Doe",
            AccountType.CHECKING,
            AccountStatus.ACTIVE,
            BigDecimal.TEN,
            "USD",
            Instant.now(),
            Instant.now(),
            0
        );

        when(accountRepository.findById(id)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        var newBalance = BigDecimal.valueOf(20);
        accountService.updateBalance(id, newBalance);

        var eventCaptor = ArgumentCaptor.forClass(AccountEvent.BalanceUpdated.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        var event = eventCaptor.getValue();
        assertEquals(id, event.accountId());
        assertEquals(account.accountNumber(), event.accountNumber());
        assertEquals(account.balance(), event.oldBalance());
        assertEquals(newBalance, event.newBalance());
        assertEquals(account.currency(), event.currency());
    }

    @Test
    void updateBalance_ShouldThrowException_WhenAccountNotActive() {
        var id = UUID.randomUUID();
        var account = new Account(
            id,
            "1234567890",
            "12345678900",
            "John Doe",
            AccountType.CHECKING,
            AccountStatus.PENDING_ACTIVATION,
            BigDecimal.TEN,
            "USD",
            Instant.now(),
            Instant.now(),
            0
        );

        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        assertThrows(RuntimeException.class, () -> accountService.updateBalance(id, BigDecimal.TEN));

        verify(accountRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void updateBalance_ShouldThrowException_WhenNegativeBalance() {
        var id = UUID.randomUUID();
        var account = new Account(
            id,
            "1234567890",
            "12345678900",
            "John Doe",
            AccountType.CHECKING,
            AccountStatus.ACTIVE,
            BigDecimal.TEN,
            "USD",
            Instant.now(),
            Instant.now(),
            0
        );

        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        assertThrows(RuntimeException.class, () -> accountService.updateBalance(id, BigDecimal.valueOf(-10)));

        verify(accountRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void findById_ShouldReturnAccount_WhenExists() {
        var id = UUID.randomUUID();
        var account = new Account(
            id,
            "1234567890",
            "12345678900",
            "John Doe",
            AccountType.CHECKING,
            AccountStatus.ACTIVE,
            BigDecimal.TEN,
            "USD",
            Instant.now(),
            Instant.now(),
            0
        );

        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        var result = accountService.findById(id);

        assertTrue(result.isPresent());
        assertEquals(account.id(), result.get().id());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenNotExists() {
        var id = UUID.randomUUID();
        when(accountRepository.findById(id)).thenReturn(Optional.empty());

        var result = accountService.findById(id);

        assertTrue(result.isEmpty());
    }
} 