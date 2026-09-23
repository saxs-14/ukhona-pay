-- UKHONA PAY production safety migration
-- Apply once to an existing production database before deploying the
-- application version that uses PENDING withdrawals.
-- Do not run this against the demo database without a backup.

ALTER TABLE bank_withdrawals
    DROP CONSTRAINT IF EXISTS bank_withdrawals_status_check;

ALTER TABLE bank_withdrawals
    ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE bank_withdrawals
    ADD CONSTRAINT bank_withdrawals_status_check
    CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'));

-- Existing rows that were incorrectly recorded as completed must be reviewed
-- against the bank/provider settlement record before being treated as paid.
-- This migration intentionally does not rewrite historical COMPLETED rows.
