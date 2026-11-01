ALTER TABLE payment_intents
    ADD COLUMN IF NOT EXISTS provider_payment_reference VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_payment_intents_provider_payment_reference
    ON payment_intents(provider_payment_reference);
