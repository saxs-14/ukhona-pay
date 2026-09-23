-- Prevent concurrent duplicate webhook deliveries from processing the same event.
ALTER TABLE payment_webhook_events
    DROP CONSTRAINT IF EXISTS payment_webhook_events_processing_status_check;

ALTER TABLE payment_webhook_events
    ADD CONSTRAINT payment_webhook_events_processing_status_check
    CHECK (processing_status IN ('RECEIVED','PROCESSING','PROCESSED','IGNORED','FAILED'));
