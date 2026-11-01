CREATE TABLE IF NOT EXISTS payment_refunds (
 id BIGSERIAL PRIMARY KEY,
 provider_refund_reference VARCHAR(120) NOT NULL UNIQUE,
 provider_transaction_reference VARCHAR(120) NOT NULL,
 payment_intent_id BIGINT NOT NULL REFERENCES payment_intents(id),
 amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
 currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
   CHECK (status IN ('PENDING','COMPLETED','FAILED')),
 reason VARCHAR(255),
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 completed_at TIMESTAMP
);

ALTER TABLE payment_intents
 ADD COLUMN IF NOT EXISTS refunded_amount NUMERIC(12,2) NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_payment_refunds_payment_intent
 ON payment_refunds(payment_intent_id);
CREATE INDEX IF NOT EXISTS idx_payment_refunds_transaction
 ON payment_refunds(provider_transaction_reference);
