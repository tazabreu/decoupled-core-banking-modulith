package com.banking.modulith.transfers.internal;

import com.banking.modulith.transfers.api.Transfer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransferRepository extends CrudRepository<Transfer, UUID> {
    // Add custom queries if needed
}