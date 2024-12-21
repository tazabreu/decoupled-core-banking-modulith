-- V2__transfers_schema.sql
CREATE TABLE transfers (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           source_account_id UUID NOT NULL REFERENCES accounts(id),
                           target_account_id UUID NOT NULL REFERENCES accounts(id),
                           amount DECIMAL(19,4) NOT NULL CHECK (amount > 0),
                           currency VARCHAR(3) NOT NULL,
                           status VARCHAR(20) NOT NULL,
                           description VARCHAR(255),
                           requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
                           completed_at TIMESTAMP WITH TIME ZONE,
                           version INTEGER NOT NULL DEFAULT 0,
                           CONSTRAINT transfers_different_accounts CHECK (source_account_id != target_account_id)
);

CREATE INDEX idx_transfers_accounts ON transfers(source_account_id, target_account_id);
CREATE INDEX idx_transfers_status ON transfers(status);
CREATE INDEX idx_transfers_dates ON transfers(requested_at, completed_at);