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