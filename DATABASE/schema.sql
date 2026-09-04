-- ============================================================================
-- UKHONA PAY - PostgreSQL Schema
-- EDHE Studentpreneurs Indaba 2026 - Market Connectivity Challenge
-- ============================================================================

DROP TABLE IF EXISTS ratings CASCADE;
DROP TABLE IF EXISTS withdrawals CASCADE;
DROP TABLE IF EXISTS cashback CASCADE;
DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS vendors CASCADE;
DROP TABLE IF EXISTS wallets CASCADE;
DROP TABLE IF EXISTS atm_locations CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS taxi_ranks CASCADE;
DROP TABLE IF EXISTS taxi_associations CASCADE;

-- ============================================================================
-- TAXI ASSOCIATIONS  (reference data - which association a driver/admin belongs to)
-- ============================================================================
CREATE TABLE taxi_associations (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL UNIQUE,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- ============================================================================
-- TAXI RANKS  (reference data - which rank a vendor trades at / admin oversees)
-- ============================================================================
CREATE TABLE taxi_ranks (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL UNIQUE,
    location_name   VARCHAR(150),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- ============================================================================
-- USERS
-- ============================================================================
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    phone_number    VARCHAR(10) NOT NULL UNIQUE CHECK (phone_number ~ '^0[0-9]{9}$'),
    pin_hash        VARCHAR(255) NOT NULL,
    user_type       VARCHAR(25) NOT NULL CHECK (user_type IN ('VENDOR', 'TAXI_DRIVER', 'TAXI_ASSOCIATION_ADMIN')),
    name            VARCHAR(120) NOT NULL,
    surname         VARCHAR(120) NOT NULL,
    id_number       VARCHAR(13) NOT NULL UNIQUE CHECK (id_number ~ '^[0-9]{13}$'),
    email           VARCHAR(150),
    phone_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    -- Association Administrator only - which association/rank they administer.
    association_id  BIGINT REFERENCES taxi_associations(id),
    rank_id         BIGINT REFERENCES taxi_ranks(id),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- ============================================================================
-- WALLETS  (one wallet per user: main balance + cashback savings)
-- ============================================================================
CREATE TABLE wallets (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    cashback_balance NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (cashback_balance >= 0),
    currency        VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- ============================================================================
-- VENDORS  (extends a user of type VENDOR or TAXI_DRIVER)
-- ============================================================================
CREATE TABLE vendors (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    business_name   VARCHAR(150) NOT NULL,
    category        VARCHAR(20) NOT NULL CHECK (category IN ('TAXI', 'FOOD', 'SERVICES', 'RETAIL', 'OTHER')),
    location_name   VARCHAR(150) NOT NULL,
    latitude        NUMERIC(9,6),
    longitude       NUMERIC(9,6),
    qr_code         VARCHAR(64) NOT NULL UNIQUE,
    verified        BOOLEAN NOT NULL DEFAULT FALSE,
    rating_avg      NUMERIC(2,1) NOT NULL DEFAULT 0,
    rating_count    INTEGER NOT NULL DEFAULT 0,
    photo_url       VARCHAR(255),
    -- Driver-only.
    vehicle_registration VARCHAR(20),
    association_id  BIGINT REFERENCES taxi_associations(id),
    -- Vendor-only.
    rank_id         BIGINT REFERENCES taxi_ranks(id),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_vendors_category ON vendors(category);

-- ============================================================================
-- ATM LOCATIONS  (ABSA network - mocked)
-- ============================================================================
CREATE TABLE atm_locations (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    address         VARCHAR(255) NOT NULL,
    city            VARCHAR(80) NOT NULL,
    latitude        NUMERIC(9,6) NOT NULL,
    longitude       NUMERIC(9,6) NOT NULL,
    bank            VARCHAR(50) NOT NULL DEFAULT 'ABSA'
);

-- ============================================================================
-- TRANSACTIONS  (immutable ledger - vendor payments)
-- ============================================================================
CREATE TABLE transactions (
    id                  BIGSERIAL PRIMARY KEY,
    reference           VARCHAR(20) NOT NULL UNIQUE,
    sender_id           BIGINT NOT NULL REFERENCES users(id),
    receiver_id         BIGINT NOT NULL REFERENCES users(id),
    vendor_id           BIGINT REFERENCES vendors(id),
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    cashback_amount     NUMERIC(12,2) NOT NULL DEFAULT 0,
    cashback_rate       NUMERIC(4,3) NOT NULL DEFAULT 0.025,
    status              VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    description         VARCHAR(255),
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_sender ON transactions(sender_id);
CREATE INDEX idx_transactions_receiver ON transactions(receiver_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

-- ============================================================================
-- CASHBACK  (per-transaction cashback ledger)
-- ============================================================================
CREATE TABLE cashback (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    transaction_id  BIGINT NOT NULL REFERENCES transactions(id),
    amount          NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'EARNED' CHECK (status IN ('EARNED', 'WITHDRAWN')),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_cashback_user ON cashback(user_id);

-- ============================================================================
-- WITHDRAWALS  (ATM cash-out only - no bank transfers)
-- ============================================================================
CREATE TABLE withdrawals (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    atm_location_id BIGINT NOT NULL REFERENCES atm_locations(id),
    amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    withdrawal_pin  VARCHAR(4) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'COMPLETED', 'EXPIRED', 'CANCELLED')),
    requested_at    TIMESTAMP NOT NULL DEFAULT now(),
    expires_at      TIMESTAMP NOT NULL,
    completed_at    TIMESTAMP
);

CREATE INDEX idx_withdrawals_user ON withdrawals(user_id);

-- ============================================================================
-- RATINGS  (customer -> vendor)
-- ============================================================================
CREATE TABLE ratings (
    id              BIGSERIAL PRIMARY KEY,
    vendor_id       BIGINT NOT NULL REFERENCES vendors(id),
    reviewer_id     BIGINT NOT NULL REFERENCES users(id),
    transaction_id  BIGINT REFERENCES transactions(id),
    stars           SMALLINT NOT NULL CHECK (stars BETWEEN 1 AND 5),
    review          VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE(vendor_id, transaction_id)
);

-- ============================================================================
-- REFERENCE DATA
-- Taxi associations and ranks are infrastructure lookups the signup dropdowns
-- need to have anything to select - not demo users/transactions. No user,
-- wallet, vendor, transaction, cashback, withdrawal, or rating rows are
-- seeded; every account is created through real signup.
-- ============================================================================

-- ATM locations (Mbombela / Nelspruit, Mpumalanga)
INSERT INTO atm_locations (name, address, city, latitude, longitude, bank) VALUES
('ABSA ATM - Riverside Mall', 'Bester St, Riverside Park', 'Mbombela', -25.4501, 31.0126, 'ABSA'),
('ABSA ATM - Ilanga Mall', 'Elephant Walk, Sonpark', 'Mbombela', -25.4638, 30.9926, 'ABSA'),
('ABSA ATM - Nelspruit CBD', 'Brown St, Nelspruit Central', 'Mbombela', -25.4745, 30.9703, 'ABSA'),
('ABSA ATM - Sonpark Boulevard', 'Nel St, Sonpark', 'Mbombela', -25.4560, 30.9558, 'ABSA'),
('ABSA ATM - White River', 'Main Rd, White River', 'Mbombela', -25.3242, 31.0189, 'ABSA');

-- Taxi associations operating in and around Mbombela / Nelspruit
INSERT INTO taxi_associations (name) VALUES
('Mbombela Long Distance Taxi Association'),
('Nelspruit CBD Taxi Association'),
('KaNyamazane Taxi Association'),
('White River Taxi Association'),
('Kabokweni Taxi Association');

-- Taxi ranks / trading points in and around Mbombela / Nelspruit
INSERT INTO taxi_ranks (name, location_name) VALUES
('Mbombela Taxi Rank', 'Nelspruit CBD'),
('KaNyamazane Rank', 'KaNyamazane, Mbombela'),
('Sonheuwel Rank', 'Sonheuwel, Mbombela'),
('Kabokweni Rank', 'Kabokweni, Mbombela'),
('Riverside Mall Rank', 'Riverside Mall, Mbombela');
