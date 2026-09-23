ALTER TABLE payment_intents
    ADD COLUMN IF NOT EXISTS redirect_url VARCHAR(500);
