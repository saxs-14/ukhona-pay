ALTER TABLE bank_accounts
    ADD COLUMN IF NOT EXISTS bank_group_id VARCHAR(36);

CREATE INDEX IF NOT EXISTS idx_bank_accounts_bank_group_id
    ON bank_accounts(bank_group_id);