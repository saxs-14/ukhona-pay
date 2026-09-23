ALTER TABLE bank_withdrawals
    ADD COLUMN IF NOT EXISTS provider_encryption_key TEXT;

ALTER TABLE bank_withdrawals
    ADD COLUMN IF NOT EXISTS provider_status INTEGER;

ALTER TABLE bank_withdrawals
    ADD COLUMN IF NOT EXISTS provider_sub_status INTEGER;

ALTER TABLE bank_withdrawals
    ADD COLUMN IF NOT EXISTS provider_error VARCHAR(500);

ALTER TABLE bank_withdrawals
    ADD CONSTRAINT bank_withdrawals_provider_reference_unique
    UNIQUE (provider_reference);
INSERT INTO ledger_accounts(account_code, account_type, currency)
VALUES ('PAYOUT_CLEARING_ZAR','PAYOUT_CLEARING','ZAR')
ON CONFLICT (account_code) DO NOTHING;

INSERT INTO ledger_accounts(account_code, account_type, currency)
VALUES ('OZOW_PAYOUT_FLOAT_ZAR','EXTERNAL_FLOAT','ZAR')
ON CONFLICT (account_code) DO NOTHING;
