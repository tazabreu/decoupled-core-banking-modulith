package com.banking.modulith.accounts.internal;

import com.banking.modulith.accounts.AccountDTO;
import com.banking.modulith.accounts.AccountStatus;
import com.banking.modulith.accounts.AccountType;
import com.banking.modulith.accounts.CreateAccountCommand;
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

@GraphQlTest(AccountGraphqlController.class)
class AccountGraphqlControllerTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Mock
    private AccountService accountService;

    @Test
    void account_ShouldReturnAccount_WhenFound() {
        var id = UUID.randomUUID();
        var account = new AccountDTO(
                id,
                "1234567890",
                "12345678900",
                "John Doe",
                AccountType.CHECKING,
                AccountStatus.ACTIVE,
                BigDecimal.TEN,
                "USD",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(accountService.findById(id)).thenReturn(Optional.of(account));

        graphQlTester.document("""
                        query($id: ID!) {
                            account(id: $id) {
                                id
                                accountNumber
                                documentNumber
                                holderName
                                type
                                status
                                balance
                                currency
                                createdAt
                                updatedAt
                            }
                        }
                        """)
                .variable("id", id)
                .execute()
                .path("account")
                .matchesJson("""
                        {
                            "id": "%s",
                            "accountNumber": "1234567890",
                            "documentNumber": "12345678900",
                            "holderName": "John Doe",
                            "type": "CHECKING",
                            "status": "ACTIVE",
                            "balance": 10,
                            "currency": "USD"
                        }
                        """.formatted(id));
    }

    @Test
    void account_ShouldThrowException_WhenNotFound() {
        var id = UUID.randomUUID();
        when(accountService.findById(id)).thenReturn(Optional.empty());

        graphQlTester.document("""
                        query($id: ID!) {
                            account(id: $id) {
                                id
                            }
                        }
                        """)
                .variable("id", id)
                .execute()
                .errors()
                .satisfy(errors -> {
                    var error = errors.get(0);
                    assert error.getMessage().contains("Account not found");
                });
    }

    @Test
    void accounts_ShouldReturnAllAccounts() {
        var account1 = new AccountDTO(
                UUID.randomUUID(),
                "1234567890",
                "12345678900",
                "John Doe",
                AccountType.CHECKING,
                AccountStatus.ACTIVE,
                BigDecimal.TEN,
                "USD",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        var account2 = new AccountDTO(
                UUID.randomUUID(),
                "0987654321",
                "09876543210",
                "Jane Doe",
                AccountType.SAVINGS,
                AccountStatus.ACTIVE,
                BigDecimal.ONE,
                "EUR",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(accountService.findAll()).thenReturn(List.of(account1, account2));

        graphQlTester.document("""
                        query {
                            accounts {
                                id
                                accountNumber
                                documentNumber
                                holderName
                                type
                                status
                                balance
                                currency
                            }
                        }
                        """)
                .execute()
                .path("accounts")
                .entityList(AccountDTO.class)
                .hasSize(2);
    }

    @Test
    void createAccount_ShouldCreateAndReturnAccount() {
        var command = new CreateAccountCommand(
                "12345678900",
                "John Doe",
                AccountType.CHECKING,
                "USD"
        );

        var createdAccount = new AccountDTO(
                UUID.randomUUID(),
                "1234567890",
                command.documentNumber(),
                command.holderName(),
                command.type(),
                AccountStatus.PENDING_ACTIVATION,
                BigDecimal.ZERO,
                command.currency(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(accountService.create(any())).thenReturn(createdAccount);

        graphQlTester.document("""
                        mutation($command: CreateAccountInput!) {
                            createAccount(command: $command) {
                                id
                                accountNumber
                                documentNumber
                                holderName
                                type
                                status
                                balance
                                currency
                            }
                        }
                        """)
                .variable("command", command)
                .execute()
                .path("createAccount")
                .matchesJson("""
                        {
                            "id": "%s",
                            "accountNumber": "1234567890",
                            "documentNumber": "12345678900",
                            "holderName": "John Doe",
                            "type": "CHECKING",
                            "status": "PENDING_ACTIVATION",
                            "balance": 0,
                            "currency": "USD"
                        }
                        """.formatted(createdAccount.id()));
    }

    @Test
    void activateAccount_ShouldActivateAndReturnAccount() {
        var id = UUID.randomUUID();
        var activatedAccount = new AccountDTO(
                id,
                "1234567890",
                "12345678900",
                "John Doe",
                AccountType.CHECKING,
                AccountStatus.ACTIVE,
                BigDecimal.ZERO,
                "USD",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(accountService.activate(id)).thenReturn(activatedAccount);

        graphQlTester.document("""
                        mutation($id: ID!) {
                            activateAccount(id: $id) {
                                id
                                status
                            }
                        }
                        """)
                .variable("id", id)
                .execute()
                .path("activateAccount")
                .matchesJson("""
                        {
                            "id": "%s",
                            "status": "ACTIVE"
                        }
                        """.formatted(id));
    }
} 