-- V1__accounts_schema.sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE accounts (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          account_number VARCHAR(10) NOT NULL UNIQUE,
                          document_number VARCHAR(20) NOT NULL,
                          holder_name VARCHAR(100) NOT NULL,
                          type VARCHAR(20) NOT NULL,
                          status VARCHAR(20) NOT NULL,
                          balance DECIMAL(19,4) NOT NULL DEFAULT 0,
                          currency VARCHAR(3) NOT NULL,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                          version INTEGER NOT NULL DEFAULT 0,
                          CONSTRAINT accounts_balance_check CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_document_number ON accounts(document_number);
CREATE INDEX idx_accounts_status ON accounts(status);