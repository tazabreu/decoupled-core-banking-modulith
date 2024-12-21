package com.banking.modulith.transfers.internal;

import com.banking.modulith.accounts.AccountStatus;
import com.banking.modulith.accounts.AccountType;
import com.banking.modulith.accounts.AccountDTO;
import com.banking.modulith.accounts.internal.AccountService;
import com.banking.modulith.transfers.api.CreateTransferCommand;
import com.banking.modulith.transfers.api.Transfer;
import com.banking.modulith.transfers.api.TransferStatus;
import com.banking.modulith.transfers.api.events.TransferStateChanged;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {
    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TransferService transferService;

    @Captor
    private ArgumentCaptor<TransferStateChanged.TransferInitiated> initiatedEventCaptor;

    @Captor
    private ArgumentCaptor<TransferStateChanged.TransferCompleted> completedEventCaptor;

    @Test
    void create_ShouldCreateTransfer_WhenValid() {
        // Arrange
        var sourceAccountId = UUID.randomUUID();
        var targetAccountId = UUID.randomUUID();
        var command = new CreateTransferCommand(
            sourceAccountId,
            targetAccountId,
            BigDecimal.TEN,
            "USD",
            "Test transfer"
        );

        var sourceAccount = createAccountDTO(sourceAccountId, BigDecimal.valueOf(100), "USD");
        var targetAccount = createAccountDTO(targetAccountId, BigDecimal.valueOf(50), "USD");

        when(accountService.validateBalance(any(), any(), any())).thenReturn(true);
        when(transferRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = transferService.create(command);

        // Assert
        assertNotNull(result);
        assertEquals(command.sourceAccountId(), result.sourceAccountId());
        assertEquals(command.targetAccountId(), result.targetAccountId());
        assertEquals(command.amount(), result.amount());
        assertEquals(command.currency(), result.currency());
        assertEquals(TransferStatus.PENDING, result.status());
        assertNotNull(result.requestedAt());
        assertNull(result.completedAt());

        verify(eventPublisher).publishEvent(initiatedEventCaptor.capture());
        var event = initiatedEventCaptor.getValue();
        assertEquals(result.id(), event.transferId());
        assertEquals(sourceAccountId, event.sourceAccountId());
        assertEquals(targetAccountId, event.targetAccountId());
        assertEquals(BigDecimal.TEN, event.amount());
        assertEquals("USD", event.currency());
    }

    @Test
    void create_ShouldThrowException_WhenSameSourceAndTarget() {
        var accountId = UUID.randomUUID();
        var command = new CreateTransferCommand(
            accountId, accountId, BigDecimal.TEN, "USD", "Test"
        );

        var exception = assertThrows(RuntimeException.class, () -> transferService.create(command));
        assertTrue(exception.getMessage().contains("must be different"));
        verify(transferRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void create_ShouldThrowException_WhenNegativeAmount() {
        var command = new CreateTransferCommand(
            UUID.randomUUID(), UUID.randomUUID(), 
            BigDecimal.valueOf(-10), "USD", "Test"
        );

        var exception = assertThrows(RuntimeException.class, () -> transferService.create(command));
        assertTrue(exception.getMessage().contains("must be positive"));
        verify(transferRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void create_ShouldThrowException_WhenInsufficientFunds() {
        var sourceAccountId = UUID.randomUUID();
        var targetAccountId = UUID.randomUUID();
        var command = new CreateTransferCommand(
            sourceAccountId, targetAccountId,
            BigDecimal.valueOf(200), "USD", "Test"
        );

        var sourceAccount = createAccountDTO(sourceAccountId, BigDecimal.valueOf(100), "USD");
        var targetAccount = createAccountDTO(targetAccountId, BigDecimal.valueOf(50), "USD");

        var exception = assertThrows(RuntimeException.class, () -> transferService.create(command));
        assertTrue(exception.getMessage().contains("Insufficient funds"));
        verify(transferRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void complete_ShouldCompleteTransfer_WhenValid() {
        // Arrange
        var transferId = UUID.randomUUID();
        var sourceAccountId = UUID.randomUUID();
        var targetAccountId = UUID.randomUUID();
        
        var transfer = createPendingTransfer(transferId, sourceAccountId, targetAccountId);
        var sourceAccount = createAccountDTO(sourceAccountId, BigDecimal.valueOf(100), "USD");
        var targetAccount = createAccountDTO(targetAccountId, BigDecimal.valueOf(50), "USD");

        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));
        when(accountService.findById(sourceAccountId)).thenReturn(Optional.of(sourceAccount));
        when(accountService.findById(targetAccountId)).thenReturn(Optional.of(targetAccount));
        when(transferRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        // Act
        var result = transferService.complete(transferId);

        // Assert
        assertNotNull(result);
        assertEquals(TransferStatus.DEBITED, result.status());
        assertNotNull(result.completedAt());

        verify(accountService).updateBalance(sourceAccountId, BigDecimal.valueOf(90)); // 100 - 10
        verify(accountService).updateBalance(targetAccountId, BigDecimal.valueOf(60)); // 50 + 10
        
        verify(eventPublisher).publishEvent(completedEventCaptor.capture());
        var event = completedEventCaptor.getValue();
        assertEquals(transferId, event.transferId());
        assertEquals(sourceAccountId, event.sourceAccountId());
        assertEquals(targetAccountId, event.targetAccountId());
        assertEquals(BigDecimal.TEN, event.amount());
        assertEquals("USD", event.currency());
    }

    @Test
    void complete_ShouldThrowException_WhenAlreadyCompleted() {
        var transferId = UUID.randomUUID();
        var transfer = createCompletedTransfer(transferId);
        when(transferRepository.findById(transferId)).thenReturn(Optional.of(transfer));

        var exception = assertThrows(RuntimeException.class, () -> transferService.complete(transferId));
        assertTrue(exception.getMessage().contains("is not pending"));
        verify(accountService, never()).updateBalance(any(), any());
        verify(eventPublisher, never()).publishEvent(any(TransferStateChanged.TransferCompleted.class));
    }

    private AccountDTO createAccountDTO(UUID id, BigDecimal balance, String currency) {
        return new AccountDTO(
            id,
            "ACC-" + id.toString().substring(0, 8),
            "DOC-123",
            "Test Account",
            AccountType.CHECKING,
            AccountStatus.ACTIVE,
            balance,
            currency,
            OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }

    private Transfer createPendingTransfer(UUID id, UUID sourceId, UUID targetId) {
        return new Transfer(
            id,
            sourceId,
            targetId,
            BigDecimal.TEN,
            "USD",
            TransferStatus.PENDING,
            "Test transfer",
            Instant.now(),
            null,
            0
        );
    }

    private Transfer createCompletedTransfer(UUID id) {
        return new Transfer(
            id,
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.TEN,
            "USD",
            TransferStatus.COMPLETED,
            "Test transfer",
            Instant.now(),
            Instant.now(),
            0
        );
    }
} 