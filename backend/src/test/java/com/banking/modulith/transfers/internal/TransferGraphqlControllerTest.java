package com.banking.modulith.transfers.internal;

import com.banking.modulith.transfers.api.CreateTransferCommand;
import com.banking.modulith.transfers.api.TransferDTO;
import com.banking.modulith.transfers.api.TransferStatus;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.graphql.test.tester.GraphQlTester;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@GraphQlTest(TransferGraphqlController.class)
class TransferGraphqlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Mock
    private TransferService transferService;

    @Test
    void transfer_ShouldReturnTransfer_WhenFound() {
        var id = UUID.randomUUID();
        var transfer = new TransferDTO(
                id,
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.TEN,
                "USD",
                TransferStatus.COMPLETED,
                "Test transfer",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(transferService.findById(id)).thenReturn(Optional.of(transfer));

        graphQlTester.document("""
                        query($id: ID!) {
                            transfer(id: $id) {
                                id
                                sourceAccountId
                                targetAccountId
                                amount
                                currency
                                status
                                description
                                requestedAt
                                completedAt
                            }
                        }
                        """)
                .variable("id", id)
                .execute()
                .path("transfer")
                .matchesJson("""
                        {
                            "id": "%s",
                            "sourceAccountId": "%s",
                            "targetAccountId": "%s",
                            "amount": 10,
                            "currency": "USD",
                            "status": "COMPLETED",
                            "description": "Test transfer"
                        }
                        """.formatted(id, transfer.sourceAccountId(), transfer.targetAccountId()));
    }

    @Test
    void transfers_ShouldReturnAllTransfers() {
        var transfer1 = new TransferDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.TEN,
                "USD",
                TransferStatus.COMPLETED,
                "Test transfer 1",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        var transfer2 = new TransferDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.ONE,
                "EUR",
                TransferStatus.PENDING,
                "Test transfer 2",
                OffsetDateTime.now(),
                null
        );

        when(transferService.findAll()).thenReturn(List.of(transfer1, transfer2));

        graphQlTester.document("""
                        query {
                            transfers {
                                id
                                sourceAccountId
                                targetAccountId
                                amount
                                currency
                                status
                                description
                            }
                        }
                        """)
                .execute()
                .path("transfers")
                .entityList(TransferDTO.class)
                .hasSize(2);
    }

    @Test
    void createTransfer_ShouldCreateAndReturnTransfer() {
        var command = new CreateTransferCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.TEN,
                "USD",
                "Test transfer"
        );

        var createdTransfer = new TransferDTO(
                UUID.randomUUID(),
                command.sourceAccountId(),
                command.targetAccountId(),
                command.amount(),
                command.currency(),
                TransferStatus.PENDING,
                command.description(),
                OffsetDateTime.now(),
                null
        );

        when(transferService.create(any())).thenReturn(createdTransfer);

        graphQlTester.document("""
                        mutation($command: CreateTransferInput!) {
                            createTransfer(command: $command) {
                                id
                                sourceAccountId
                                targetAccountId
                                amount
                                currency
                                status
                                description
                            }
                        }
                        """)
                .variable("command", command)
                .execute()
                .path("createTransfer")
                .matchesJson("""
                        {
                            "id": "%s",
                            "sourceAccountId": "%s",
                            "targetAccountId": "%s",
                            "amount": 10,
                            "currency": "USD",
                            "status": "PENDING",
                            "description": "Test transfer"
                        }
                        """.formatted(createdTransfer.id(), command.sourceAccountId(), command.targetAccountId()));
    }

    @Test
    void completeTransfer_ShouldCompleteAndReturnTransfer() {
        var id = UUID.randomUUID();
        var completedTransfer = new TransferDTO(
                id,
                UUID.randomUUID(),
                UUID.randomUUID(),
                BigDecimal.TEN,
                "USD",
                TransferStatus.COMPLETED,
                "Test transfer",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(transferService.complete(id)).thenReturn(completedTransfer);

        graphQlTester.document("""
                        mutation($id: ID!) {
                            completeTransfer(id: $id) {
                                id
                                status
                                completedAt
                            }
                        }
                        """)
                .variable("id", id)
                .execute()
                .path("completeTransfer")
                .matchesJson("""
                        {
                            "id": "%s",
                            "status": "COMPLETED"
                        }
                        """.formatted(id));
    }
} 