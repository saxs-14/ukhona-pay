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

-- Vendors (5) - PIN 1234, bcrypt hash of "1234" (placeholder, real signup rehashes)
INSERT INTO users (phone_number, pin_hash, user_type, name, email, phone_verified) VALUES
('0711234501', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'VENDOR', 'Lucky Taxi', 'lucky.taxi@demo.co.za', TRUE),
('0711234502', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'VENDOR', 'Thandi''s Spaza Shop', 'thandi.spaza@demo.co.za', TRUE),
('0711234503', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'VENDOR', 'Mama Joy Kitchen', 'mamajoy@demo.co.za', TRUE),
('0711234504', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'VENDOR', 'Sipho Electrical Services', 'sipho.electrical@demo.co.za', TRUE),
('0711234505', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'VENDOR', 'Nomsa Fashion Retail', 'nomsa.fashion@demo.co.za', TRUE);

-- Corporate employees (10) - PIN 1234, first one matches demo script (0798765432)
INSERT INTO users (phone_number, pin_hash, user_type, name, email, phone_verified) VALUES
('0798765432', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'EMPLOYEE', 'Karabo Mokoena', 'karabo.m@corp.co.za', TRUE),
('0798765433', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'EMPLOYEE', 'Lerato Dube', 'lerato.d@corp.co.za', TRUE),
('0798765434', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'EMPLOYEE', 'Sizwe Nkosi', 'sizwe.n@corp.co.za', TRUE),
('0798765435', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'EMPLOYEE', 'Amanda van der Merwe', 'amanda.vdm@corp.co.za', TRUE),
('0798765436', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'EMPLOYEE', 'Bongani Zulu', 'bongani.z@corp.co.za', TRUE),
('0798765437', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'EMPLOYEE', 'Precious Khumalo', 'precious.k@corp.co.za', TRUE),
('0798765438', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'EMPLOYEE', 'Johan Botha', 'johan.b@corp.co.za', TRUE),
('0798765439', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'EMPLOYEE', 'Zanele Mahlangu', 'zanele.m@corp.co.za', TRUE),
('0798765440', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'EMPLOYEE', 'Ryan Naidoo', 'ryan.n@corp.co.za', TRUE),
('0798765441', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'EMPLOYEE', 'Nokuthula Ndlovu', 'nokuthula.n@corp.co.za', TRUE);

-- Wallets for all 15 users (vendors start with earnings, employees start with R1,000)
INSERT INTO wallets (user_id, balance, cashback_balance)
-- Commuter "wallet" balance is a seed-data bookkeeping convenience only (real
-- commuters pay via their own banking app in the target architecture, not a
-- UKHONA PAY balance) - set high enough to cover 90 days of simulated rides
-- across 10 round-robin commuters without tripping the non-negative constraint.
SELECT id, CASE WHEN user_type = 'EMPLOYEE' THEN 100000.00 ELSE 0.00 END, 0.00
FROM users;

-- Vendor profiles
INSERT INTO vendors (user_id, business_name, category, location_name, latitude, longitude, qr_code, verified, rating_avg, rating_count) VALUES
((SELECT id FROM users WHERE phone_number = '0711234501'), 'Lucky Taxi', 'TAXI', 'Mbombela Taxi Rank, Nelspruit CBD', -25.4745, 30.9703, 'UKP-VENDOR-LUCKYTAXI-001', TRUE, 4.8, 24),
((SELECT id FROM users WHERE phone_number = '0711234502'), 'Thandi''s Spaza Shop', 'RETAIL', 'KaNyamazane, Mbombela', -25.4308, 31.2085, 'UKP-VENDOR-THANDISPAZA-002', TRUE, 4.6, 18),
((SELECT id FROM users WHERE phone_number = '0711234503'), 'Mama Joy Kitchen', 'FOOD', 'Sonheuwel, Mbombela', -25.4667, 30.9575, 'UKP-VENDOR-MAMAJOY-003', TRUE, 4.9, 31),
((SELECT id FROM users WHERE phone_number = '0711234504'), 'Sipho Electrical Services', 'SERVICES', 'Kabokweni, Mbombela', -25.4132, 31.1425, 'UKP-VENDOR-SIPHOELEC-004', TRUE, 4.7, 12),
((SELECT id FROM users WHERE phone_number = '0711234505'), 'Nomsa Fashion Retail', 'RETAIL', 'Riverside Mall, Mbombela', -25.4501, 31.0126, 'UKP-VENDOR-NOMSAFASHION-005', FALSE, 4.3, 7);

-- 20 sample transactions across the month, weighted TAXI 40% / FOOD 30% / SERVICES 20% / OTHER(RETAIL) 10%
-- cashback_rate 2.5%, amounts sum to R5,000
INSERT INTO transactions (reference, sender_id, receiver_id, vendor_id, amount, cashback_amount, cashback_rate, status, description, created_at) VALUES
('TXN-0001', (SELECT id FROM users WHERE phone_number='0798765432'), (SELECT id FROM users WHERE phone_number='0711234501'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-LUCKYTAXI-001'), 100.00, 2.50, 0.025, 'COMPLETED', 'Taxi fare - Nelspruit CBD to KaNyamazane', now() - interval '28 days'),
('TXN-0002', (SELECT id FROM users WHERE phone_number='0798765433'), (SELECT id FROM users WHERE phone_number='0711234501'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-LUCKYTAXI-001'), 150.00, 3.75, 0.025, 'COMPLETED', 'Taxi fare - return trip', now() - interval '27 days'),
('TXN-0003', (SELECT id FROM users WHERE phone_number='0798765434'), (SELECT id FROM users WHERE phone_number='0711234503'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-MAMAJOY-003'), 85.00, 2.13, 0.025, 'COMPLETED', 'Lunch order - Sonheuwel', now() - interval '26 days'),
('TXN-0004', (SELECT id FROM users WHERE phone_number='0798765435'), (SELECT id FROM users WHERE phone_number='0711234502'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-THANDISPAZA-002'), 220.00, 5.50, 0.025, 'COMPLETED', 'Groceries - KaNyamazane', now() - interval '25 days'),
('TXN-0005', (SELECT id FROM users WHERE phone_number='0798765436'), (SELECT id FROM users WHERE phone_number='0711234501'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-LUCKYTAXI-001'), 100.00, 2.50, 0.025, 'COMPLETED', 'Taxi fare - Riverside to Sonheuwel', now() - interval '24 days'),
('TXN-0006', (SELECT id FROM users WHERE phone_number='0798765437'), (SELECT id FROM users WHERE phone_number='0711234504'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-SIPHOELEC-004'), 450.00, 11.25, 0.025, 'COMPLETED', 'Home wiring repair - Kabokweni', now() - interval '23 days'),
('TXN-0007', (SELECT id FROM users WHERE phone_number='0798765438'), (SELECT id FROM users WHERE phone_number='0711234503'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-MAMAJOY-003'), 90.00, 2.25, 0.025, 'COMPLETED', 'Lunch order - Sonheuwel', now() - interval '22 days'),
('TXN-0008', (SELECT id FROM users WHERE phone_number='0798765439'), (SELECT id FROM users WHERE phone_number='0711234501'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-LUCKYTAXI-001'), 200.00, 5.00, 0.025, 'COMPLETED', 'Taxi fare - Kruger Mpumalanga Airport run', now() - interval '21 days'),
('TXN-0009', (SELECT id FROM users WHERE phone_number='0798765440'), (SELECT id FROM users WHERE phone_number='0711234505'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-NOMSAFASHION-005'), 350.00, 8.75, 0.025, 'COMPLETED', 'Clothing purchase - Riverside Mall', now() - interval '20 days'),
('TXN-0010', (SELECT id FROM users WHERE phone_number='0798765441'), (SELECT id FROM users WHERE phone_number='0711234501'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-LUCKYTAXI-001'), 100.00, 2.50, 0.025, 'COMPLETED', 'Taxi fare - Nelspruit CBD', now() - interval '19 days'),
('TXN-0011', (SELECT id FROM users WHERE phone_number='0798765432'), (SELECT id FROM users WHERE phone_number='0711234503'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-MAMAJOY-003'), 120.00, 3.00, 0.025, 'COMPLETED', 'Team lunch order', now() - interval '18 days'),
('TXN-0012', (SELECT id FROM users WHERE phone_number='0798765433'), (SELECT id FROM users WHERE phone_number='0711234501'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-LUCKYTAXI-001'), 150.00, 3.75, 0.025, 'COMPLETED', 'Taxi fare - White River route', now() - interval '17 days'),
('TXN-0013', (SELECT id FROM users WHERE phone_number='0798765434'), (SELECT id FROM users WHERE phone_number='0711234502'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-THANDISPAZA-002'), 180.00, 4.50, 0.025, 'COMPLETED', 'Groceries - KaNyamazane', now() - interval '16 days'),
('TXN-0014', (SELECT id FROM users WHERE phone_number='0798765435'), (SELECT id FROM users WHERE phone_number='0711234501'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-LUCKYTAXI-001'), 100.00, 2.50, 0.025, 'COMPLETED', 'Taxi fare - Mbombela CBD', now() - interval '15 days'),
('TXN-0015', (SELECT id FROM users WHERE phone_number='0798765436'), (SELECT id FROM users WHERE phone_number='0711234504'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-SIPHOELEC-004'), 380.00, 9.50, 0.025, 'COMPLETED', 'Appliance install - White River', now() - interval '14 days'),
('TXN-0016', (SELECT id FROM users WHERE phone_number='0798765437'), (SELECT id FROM users WHERE phone_number='0711234501'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-LUCKYTAXI-001'), 200.00, 5.00, 0.025, 'COMPLETED', 'Taxi fare - Kruger Mpumalanga Airport run', now() - interval '13 days'),
('TXN-0017', (SELECT id FROM users WHERE phone_number='0798765438'), (SELECT id FROM users WHERE phone_number='0711234503'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-MAMAJOY-003'), 95.00, 2.38, 0.025, 'COMPLETED', 'Lunch order - Sonheuwel', now() - interval '10 days'),
('TXN-0018', (SELECT id FROM users WHERE phone_number='0798765439'), (SELECT id FROM users WHERE phone_number='0711234501'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-LUCKYTAXI-001'), 150.00, 3.75, 0.025, 'COMPLETED', 'Taxi fare - Riverside to CBD', now() - interval '7 days'),
('TXN-0019', (SELECT id FROM users WHERE phone_number='0798765440'), (SELECT id FROM users WHERE phone_number='0711234502'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-THANDISPAZA-002'), 250.00, 6.25, 0.025, 'COMPLETED', 'Groceries - KaNyamazane', now() - interval '3 days'),
('TXN-0020', (SELECT id FROM users WHERE phone_number='0798765441'), (SELECT id FROM users WHERE phone_number='0711234501'), (SELECT id FROM vendors WHERE qr_code='UKP-VENDOR-LUCKYTAXI-001'), 100.00, 2.50, 0.025, 'COMPLETED', 'Taxi fare - Nelspruit CBD', now() - interval '1 days');

-- Taxi ranks / trading points in and around Mbombela / Nelspruit
INSERT INTO taxi_ranks (name, location_name) VALUES
('Mbombela Taxi Rank', 'Nelspruit CBD'),
('KaNyamazane Rank', 'KaNyamazane, Mbombela'),
('Sonheuwel Rank', 'Sonheuwel, Mbombela'),
('Kabokweni Rank', 'Kabokweni, Mbombela'),
('Riverside Mall Rank', 'Riverside Mall, Mbombela');
