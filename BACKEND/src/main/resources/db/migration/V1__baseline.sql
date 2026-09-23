-- Production baseline schema. Demo data is intentionally excluded.
-- Existing production databases must be baselined before enabling Flyway.
CREATE TABLE IF NOT EXISTS taxi_associations (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(150) NOT NULL,
 dues_amount NUMERIC(10,2) NOT NULL DEFAULT 250.00 CHECK (dues_amount >= 0),
 created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_taxi_associations_name_lower ON taxi_associations (LOWER(name));
CREATE TABLE IF NOT EXISTS taxi_ranks (
 id BIGSERIAL PRIMARY KEY, name VARCHAR(150) NOT NULL, location_name VARCHAR(150),
 association_id BIGINT REFERENCES taxi_associations(id), created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX IF NOT EXISTS ux_taxi_ranks_name_lower ON taxi_ranks (LOWER(name));
CREATE TABLE IF NOT EXISTS users (
 id BIGSERIAL PRIMARY KEY, phone_number VARCHAR(10) NOT NULL CHECK (phone_number ~ '^0[0-9]{9}$'),
 pin_hash VARCHAR(255) NOT NULL, user_type VARCHAR(25) NOT NULL,
 name VARCHAR(120) NOT NULL, surname VARCHAR(120) NOT NULL, id_number VARCHAR(13) NOT NULL UNIQUE,
 email VARCHAR(150), phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
 association_id BIGINT REFERENCES taxi_associations(id), rank_id BIGINT REFERENCES taxi_ranks(id),
 failed_login_attempts INT NOT NULL DEFAULT 0, locked_until TIMESTAMP,
 created_at TIMESTAMP NOT NULL DEFAULT now(), updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS wallets (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
 association_id BIGINT UNIQUE REFERENCES taxi_associations(id),
 balance NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
 cashback_balance NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (cashback_balance >= 0),
 savings_balance NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (savings_balance >= 0),
 maintenance_balance NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (maintenance_balance >= 0),
 currency VARCHAR(3) NOT NULL DEFAULT 'ZAR', updated_at TIMESTAMP NOT NULL DEFAULT now(),
 CHECK ((user_id IS NOT NULL AND association_id IS NULL) OR (user_id IS NULL AND association_id IS NOT NULL))
);
CREATE TABLE IF NOT EXISTS vendors (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
 business_name VARCHAR(150) NOT NULL, category VARCHAR(20) NOT NULL, location_name VARCHAR(150) NOT NULL,
 latitude NUMERIC(9,6), longitude NUMERIC(9,6), qr_code VARCHAR(64) NOT NULL UNIQUE,
 verified BOOLEAN NOT NULL DEFAULT FALSE, photo_url VARCHAR(255),
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING', vehicle_registration VARCHAR(20),
 association_id BIGINT REFERENCES taxi_associations(id), rank_id BIGINT REFERENCES taxi_ranks(id),
 created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_vendors_category ON vendors(category);
CREATE TABLE IF NOT EXISTS transactions (
 id BIGSERIAL PRIMARY KEY, reference VARCHAR(30) NOT NULL UNIQUE,
 sender_id BIGINT REFERENCES users(id), receiver_id BIGINT REFERENCES users(id),
 receiver_association_id BIGINT REFERENCES taxi_associations(id), vendor_id BIGINT REFERENCES vendors(id),
 amount NUMERIC(12,2) NOT NULL CHECK (amount > 0), cashback_amount NUMERIC(12,2) NOT NULL DEFAULT 0,
 cashback_rate NUMERIC(4,3) NOT NULL DEFAULT 0, platform_fee NUMERIC(12,2) NOT NULL DEFAULT 0,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING', description VARCHAR(255), created_at TIMESTAMP NOT NULL DEFAULT now(),
 CHECK ((receiver_id IS NOT NULL AND receiver_association_id IS NULL) OR (receiver_id IS NULL AND receiver_association_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_transactions_sender ON transactions(sender_id);
CREATE INDEX IF NOT EXISTS idx_transactions_receiver ON transactions(receiver_id);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at);
CREATE TABLE IF NOT EXISTS bank_accounts (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
 account_holder_name VARCHAR(150) NOT NULL, bank_name VARCHAR(100) NOT NULL,
 account_number VARCHAR(20) NOT NULL, branch_code VARCHAR(10) NOT NULL,
 created_at TIMESTAMP NOT NULL DEFAULT now(), updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE TABLE IF NOT EXISTS bank_withdrawals (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id),
 bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id), reference VARCHAR(30) NOT NULL UNIQUE,
 amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','COMPLETED','FAILED')),
 created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_bank_withdrawals_user ON bank_withdrawals(user_id);
CREATE TABLE IF NOT EXISTS service_purchases (
 id BIGSERIAL PRIMARY KEY, user_id BIGINT NOT NULL REFERENCES users(id), type VARCHAR(20) NOT NULL,
 reference VARCHAR(20) NOT NULL UNIQUE, amount NUMERIC(12,2) NOT NULL CHECK (amount > 0),
 network VARCHAR(20), recipient_phone VARCHAR(15), meter_number VARCHAR(20), municipality VARCHAR(100),
 biller_name VARCHAR(100), biller_category VARCHAR(60), payat_reference VARCHAR(30),
 account_name VARCHAR(150), voucher_token VARCHAR(40) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_service_purchases_user ON service_purchases(user_id);
