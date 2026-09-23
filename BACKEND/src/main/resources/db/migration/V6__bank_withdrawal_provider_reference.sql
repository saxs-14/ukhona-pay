ALTER TABLE bank_withdrawals
    ADD COLUMN IF NOT EXISTS provider_reference VARCHAR(120);

ALTER TABLE bank_withdrawals
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

UPDATE bank_withdrawals
   SET updated_at = created_at
 WHERE updated_at IS NULL;

ALTER TABLE bank_withdrawals
    ALTER COLUMN updated_at SET DEFAULT now();

ALTER TABLE bank_withdrawals
    ALTER COLUMN updated_at SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_bank_withdrawals_provider_reference
    ON bank_withdrawals(provider_reference)
    WHERE provider_reference IS NOT NULL;
