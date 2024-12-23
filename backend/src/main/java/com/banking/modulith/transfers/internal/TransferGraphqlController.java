package com.banking.modulith.transfers.internal;

import com.banking.modulith.transfers.api.CreateTransferCommand;
import com.banking.modulith.transfers.api.TransferDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.UUID;

// TODO: add elegant error handling
// TODO: add asynchronicity to this API
// TODO: replace open telemetry for new relic... for logs, traces, metrics... and exemplars? or correlated spans!
@Controller
public class TransferGraphqlController {
    private final TransferService transferService;
    private static final Logger log = LoggerFactory.getLogger(TransferGraphqlController.class);

    public TransferGraphqlController(TransferService transferService) {
        this.transferService = transferService;
    }

    @GetMapping("/test")
    @ResponseBody
    public String test() {
        log.info("Test endpoint called");
        return "test";
    }

    @QueryMapping
    public TransferDTO queryTransfer(@Argument UUID id) {
        return transferService.findById(id)
            .orElseThrow(() -> new RuntimeException("Transfer not found: " + id));
    }

    @QueryMapping
    public List<TransferDTO> queryTransfers() {
        return transferService.findAll();
    }

    @MutationMapping
    public TransferDTO createTransfer(@Argument CreateTransferCommand command) {
        log.info("Creating transfer: {}", command);
        return transferService.create(command);
    }

    @MutationMapping
    public TransferDTO completeTransfer(@Argument UUID id) {
        return transferService.complete(id);
    }
}