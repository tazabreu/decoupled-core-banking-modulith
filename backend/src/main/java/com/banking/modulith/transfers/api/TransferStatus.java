package com.banking.modulith.transfers.api;

public enum TransferStatus {
    PENDING,
    DEBITED,      // Source account debited
    COMPLETED,    // Target account credited
    FAILED,       // Something went wrong
    COMPENSATED   // Failure occurred and was compensated
}
