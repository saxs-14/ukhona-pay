-- Immutable double-entry accounting layer.
CREATE TABLE IF NOT EXISTS ledger_accounts (
 id BIGSERIAL PRIMARY KEY,
 account_code VARCHAR(80) NOT NULL UNIQUE,
 account_type VARCHAR(30) NOT NULL,
 user_id BIGINT REFERENCES users(id),
 association_id BIGINT REFERENCES taxi_associations(id),
 currency VARCHAR(3) NOT NULL DEFAULT 'ZAR',
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT now(),
 CHECK (NOT (user_id IS NOT NULL AND association_id IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS ledger_transactions (
 id BIGSERIAL PRIMARY KEY,
 reference VARCHAR(40) NOT NULL UNIQUE,
 transaction_type VARCHAR(40) NOT NULL,
 external_reference VARCHAR(160),
 status VARCHAR(20) NOT NULL DEFAULT 'POSTED' CHECK (status IN ('POSTED','REVERSED')),
 description VARCHAR(255),
 created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ledger_transactions_external_reference ON ledger_transactions(external_reference);
CREATE INDEX IF NOT EXISTS idx_ledger_transactions_created_at ON ledger_transactions(created_at);
CREATE TABLE IF NOT EXISTS ledger_entries (
 id BIGSERIAL PRIMARY KEY,
 ledger_transaction_id BIGINT NOT NULL REFERENCES ledger_transactions(id),
 ledger_account_id BIGINT NOT NULL REFERENCES ledger_accounts(id),
 direction VARCHAR(6) NOT NULL CHECK (direction IN ('DEBIT','CREDIT')),
 amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
 created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_transaction ON ledger_entries(ledger_transaction_id);
CREATE INDEX IF NOT EXISTS idx_ledger_entries_account ON ledger_entries(ledger_account_id);
INSERT INTO ledger_accounts(account_code,account_type,currency)
VALUES ('PLATFORM_FEE_REVENUE_ZAR','PLATFORM_REVENUE','ZAR')
ON CONFLICT (account_code) DO NOTHING;
INSERT INTO ledger_accounts(account_code,account_type,currency)
VALUES ('PAYMENT_CLEARING_ZAR','PAYMENT_CLEARING','ZAR')
ON CONFLICT (account_code) DO NOTHING;
