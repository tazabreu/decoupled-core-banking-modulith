package com.banking.modulith.transfers.internal;

import com.banking.modulith.transfers.api.CreateTransferCommand;
import com.banking.modulith.transfers.api.TransferDTO;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

// TODO: add elegant error handling
// TODO: add asynchronicity to this API
@Controller
public class TransferGraphqlController {
    private final TransferService transferService;

    public TransferGraphqlController(TransferService transferService) {
        this.transferService = transferService;
    }

    @QueryMapping
    public TransferDTO transfer(@Argument UUID id) {
        return transferService.findById(id)
            .orElseThrow(() -> new RuntimeException("Transfer not found: " + id));
    }

    @QueryMapping
    public List<TransferDTO> transfers() {
        return transferService.findAll();
    }

    @MutationMapping
    public TransferDTO createTransfer(@Argument CreateTransferCommand command) {
        return transferService.create(command);
    }

    @MutationMapping
    public TransferDTO completeTransfer(@Argument UUID id) {
        return transferService.complete(id);
    }
}