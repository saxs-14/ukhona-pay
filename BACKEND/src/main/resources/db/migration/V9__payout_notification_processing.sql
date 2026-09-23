ALTER TABLE payout_notification_events
    DROP CONSTRAINT IF EXISTS payout_notification_events_processing_status_check;

ALTER TABLE payout_notification_events
    ADD CONSTRAINT payout_notification_events_processing_status_check
    CHECK (processing_status IN ('RECEIVED','PROCESSING','PROCESSED','IGNORED','FAILED'));
