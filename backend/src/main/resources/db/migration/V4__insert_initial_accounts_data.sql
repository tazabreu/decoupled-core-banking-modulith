-- V4__initial_accounts_data.sql

-- Insert a MASTER account to hold the initial system balance.
-- This account acts as the liquidity source for new accounts or test transfers.
INSERT INTO accounts (
    id,
    account_number,
    document_number,
    holder_name,
    type,
    status,
    balance,
    currency,
    created_at,
    updated_at,
    version
)
VALUES (
           '34bbfb29-af91-4fea-9117-e49a9eddbede',  -- Feel free to change to a UUID of your choosing
           '9999999999',                           -- Or another unique identifier for your "master" account
           '00.000.000/0001-00',                   -- Example tax or document number
           'CoreBanking Master',                   -- Name indicating master ownership
           'CHECKING',                               -- "type" can be anything that indicates a system-level account
           'ACTIVE',
           1000000.0000,                           -- Initial balance to seed the system
           'USD',                                  -- ISO currency code
           NOW(),
           NOW(),
           0
       );