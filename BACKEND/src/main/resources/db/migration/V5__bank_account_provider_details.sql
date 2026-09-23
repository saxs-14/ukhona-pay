-- Provider payout metadata required for a real bank payout integration.
-- Existing bank accounts must be updated by the account owner with the
-- provider's bankGroupId before they can be used for live payouts.
ALTER TABLE bank_accounts
    ADD COLUMN IF NOT EXISTS bank_group_id VARCHAR(36);

CREATE INDEX IF NOT EXISTS idx_bank_accounts_bank_group_id
    ON bank_accounts(bank_group_id);
