package com.banking.modulith.accounts.internal;

import com.banking.modulith.accounts.AccountDTO;
import com.banking.modulith.accounts.CreateAccountCommand;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
public class AccountGraphqlController {
    private final AccountService accountService;

    public AccountGraphqlController(AccountService accountService) {
        this.accountService = accountService;
    }

    @QueryMapping
    public AccountDTO account(@Argument UUID id) {
        return accountService.findById(id)
            .orElseThrow(() -> new RuntimeException("Account not found: " + id));
    }

    @QueryMapping
    public List<AccountDTO> accounts() {
        return accountService.findAll();
    }

    @MutationMapping
    public AccountDTO createAccount(@Argument CreateAccountCommand command) {
        return accountService.create(command);
    }

    @MutationMapping
    public AccountDTO activateAccount(@Argument UUID id) {
        return accountService.activate(id);
    }
}