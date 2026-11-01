CREATE TABLE IF NOT EXISTS payout_notification_events (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(40) NOT NULL,
    payout_reference VARCHAR(120) NOT NULL,
    merchant_reference VARCHAR(30) NOT NULL,
    status INTEGER NOT NULL,
    sub_status INTEGER NOT NULL,
    hash_verified BOOLEAN NOT NULL DEFAULT FALSE,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED'
        CHECK (processing_status IN ('RECEIVED','PROCESSED','IGNORED','FAILED')),
    payload TEXT NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT now(),
    processed_at TIMESTAMP,
    error_message VARCHAR(500),
    CONSTRAINT uq_payout_notification_provider_ref
        UNIQUE (provider, payout_reference, status, sub_status)
);

CREATE INDEX IF NOT EXISTS idx_payout_notification_reference
    ON payout_notification_events(provider, payout_reference);