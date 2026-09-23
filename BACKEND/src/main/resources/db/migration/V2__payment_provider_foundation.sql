-- Provider-backed payment intents and webhook idempotency.
CREATE TABLE IF NOT EXISTS payment_intents (
 id BIGSERIAL PRIMARY KEY,
 internal_reference VARCHAR(40) NOT NULL UNIQUE,
 provider VARCHAR(40) NOT NULL,
 provider_reference VARCHAR(120) UNIQUE,
 idempotency_key VARCHAR(120) NOT NULL UNIQUE,
 vendor_id BIGINT NOT NULL REFERENCES vendors(id),
 payer_user_id BIGINT REFERENCES users(id),
 amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
 currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
   CHECK (status IN ('PENDING','COMPLETED','FAILED','EXPIRED')),
 failure_reason VARCHAR(255),
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 completed_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_payment_intents_payer ON payment_intents(payer_user_id);
CREATE INDEX IF NOT EXISTS idx_payment_intents_vendor ON payment_intents(vendor_id);
CREATE INDEX IF NOT EXISTS idx_payment_intents_provider_reference ON payment_intents(provider_reference);

CREATE TABLE IF NOT EXISTS payment_webhook_events (
 id BIGSERIAL PRIMARY KEY,
 provider VARCHAR(40) NOT NULL,
 provider_event_id VARCHAR(160) NOT NULL,
 event_type VARCHAR(120) NOT NULL,
 signature_verified BOOLEAN NOT NULL DEFAULT FALSE,
 processing_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED'
   CHECK (processing_status IN ('RECEIVED','PROCESSED','IGNORED','FAILED')),
 payload TEXT NOT NULL,
 received_at TIMESTAMP NOT NULL DEFAULT now(),
 processed_at TIMESTAMP,
 error_message VARCHAR(500),
 CONSTRAINT uq_payment_webhook_provider_event UNIQUE (provider, provider_event_id)
);
CREATE INDEX IF NOT EXISTS idx_payment_webhook_received ON payment_webhook_events(received_at);
