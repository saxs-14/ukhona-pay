-- ============================================================================
-- UKHONA PAY - PostgreSQL Schema
-- EDHE Studentpreneurs Indaba 2026 - Market Connectivity Challenge
-- ============================================================================

DROP TABLE IF EXISTS bank_withdrawals CASCADE;
DROP TABLE IF EXISTS bank_accounts CASCADE;
DROP TABLE IF EXISTS transactions CASCADE;
DROP TABLE IF EXISTS vendors CASCADE;
DROP TABLE IF EXISTS wallets CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS taxi_ranks CASCADE;
DROP TABLE IF EXISTS taxi_associations CASCADE;

-- ============================================================================
-- TAXI ASSOCIATIONS  (reference data - which association a driver/admin belongs to)
-- ============================================================================
CREATE TABLE taxi_associations (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- Case-insensitive uniqueness ("Top Star" and "top star" are the same
-- association) - a plain UNIQUE on name is case-sensitive and would let two
-- concurrent signups create duplicate rows differing only by case.
CREATE UNIQUE INDEX ux_taxi_associations_name_lower ON taxi_associations (LOWER(name));

-- ============================================================================
-- TAXI RANKS  (reference data - which rank a vendor trades at / admin oversees)
-- ============================================================================
CREATE TABLE taxi_ranks (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    location_name   VARCHAR(150),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- Same case-insensitive uniqueness reasoning as taxi_associations above.
CREATE UNIQUE INDEX ux_taxi_ranks_name_lower ON taxi_ranks (LOWER(name));

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
-- WALLETS  (one wallet per user OR per taxi association: main balance + cashback)
-- A wallet belongs to exactly one of a user or a taxi association - the latter
-- lets drivers pay their taxi owner/association directly (see transactions
-- below) without that association needing its own login. Association wallets
-- are created lazily by the backend the first time money is sent to one.
-- ============================================================================
CREATE TABLE wallets (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    association_id  BIGINT UNIQUE REFERENCES taxi_associations(id),
    balance         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    cashback_balance NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (cashback_balance >= 0),
    currency        VARCHAR(3) NOT NULL DEFAULT 'ZAR',
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    CHECK ((user_id IS NOT NULL AND association_id IS NULL) OR (user_id IS NULL AND association_id IS NOT NULL))
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
    photo_url       VARCHAR(255),
    -- Only meaningful for TAXI_DRIVER: a driver starts PENDING and can't
    -- accept payments until the taxi association administrator approves the
    -- registration (checking the vehicle is genuinely registered with that
    -- association). Plain VENDOR signups default straight to APPROVED - see
    -- AuthService.
    status          VARCHAR(20) NOT NULL DEFAULT 'APPROVED' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    -- Driver-only.
    vehicle_registration VARCHAR(20),
    association_id  BIGINT REFERENCES taxi_associations(id),
    -- Vendor-only.
    rank_id         BIGINT REFERENCES taxi_ranks(id),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_vendors_category ON vendors(category);

-- ============================================================================
-- TRANSACTIONS  (immutable ledger - vendor payments and driver-to-association
-- transfers). A transaction's receiver is either a user or a taxi association,
-- never both - see the CHECK constraint below.
-- ============================================================================
CREATE TABLE transactions (
    id                  BIGSERIAL PRIMARY KEY,
    reference           VARCHAR(20) NOT NULL UNIQUE,
    -- Null sender = an external payment (a commuter paying via their own
    -- banking app - they never hold a UKHONA PAY account/user row).
    sender_id           BIGINT REFERENCES users(id),
    receiver_id         BIGINT REFERENCES users(id),
    receiver_association_id BIGINT REFERENCES taxi_associations(id),
    vendor_id           BIGINT REFERENCES vendors(id),
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    cashback_amount     NUMERIC(12,2) NOT NULL DEFAULT 0,
    cashback_rate       NUMERIC(4,3) NOT NULL DEFAULT 0.025,
    status              VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    description         VARCHAR(255),
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    CHECK ((receiver_id IS NOT NULL AND receiver_association_id IS NULL) OR (receiver_id IS NULL AND receiver_association_id IS NOT NULL))
);

CREATE INDEX idx_transactions_sender ON transactions(sender_id);
CREATE INDEX idx_transactions_receiver ON transactions(receiver_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

-- ============================================================================
-- BANK ACCOUNTS  (one saved bank account per driver/vendor, for bank withdrawals)
-- ============================================================================
CREATE TABLE bank_accounts (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    account_holder_name VARCHAR(150) NOT NULL,
    bank_name           VARCHAR(100) NOT NULL,
    account_number      VARCHAR(20) NOT NULL,
    branch_code         VARCHAR(10) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now()
);

-- ============================================================================
-- BANK WITHDRAWALS  (simulated payout to a saved bank account - debits the
-- main wallet balance immediately, no real bank rail integration)
-- ============================================================================
CREATE TABLE bank_withdrawals (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
    reference       VARCHAR(20) NOT NULL UNIQUE,
    amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED', 'FAILED')),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_bank_withdrawals_user ON bank_withdrawals(user_id);

-- ============================================================================
-- NO SEED DATA
-- Every row in every table - including taxi_associations and taxi_ranks - is
-- created through real usage, not pre-loaded here. An Association
-- Administrator creates their association on signup; a vendor or admin
-- creates a taxi rank the same way if it doesn't exist yet. See
-- ReferenceDataController's POST /api/taxi-associations and /api/taxi-ranks.
-- ============================================================================
<<<<<<< HEAD
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

-- Taxi Associations (Mbombela Region)
INSERT INTO taxi_associations (name) VALUES
('Mbombela Local Taxi Association (MALTA)'),
('KaNyamazane Taxi Association (KATA)'),
('White River Taxi Association (WRTA)'),
('Ehlanzeni District Taxi Council');

-- Taxi Ranks (Mbombela Region)
INSERT INTO taxi_ranks (name, location_name) VALUES
('Mbombela Taxi Rank', 'Mbombela CBD'),
('KaNyamazane Taxi Rank', 'KaNyamazane'),
('Sonheuwel Taxi Rank', 'Sonheuwel'),
('Kabokweni Taxi Rank', 'Kabokweni'),
('White River Taxi Rank', 'White River');

-- ATM locations (Mbombela / Nelspruit, Mpumalanga)
INSERT INTO atm_locations (name, address, city, latitude, longitude, bank) VALUES
('ABSA ATM - Riverside Mall', 'Bester St, Riverside Park', 'Mbombela', -25.4501, 31.0126, 'ABSA'),
('ABSA ATM - Ilanga Mall', 'Elephant Walk, Sonpark', 'Mbombela', -25.4638, 30.9926, 'ABSA'),
('ABSA ATM - Nelspruit CBD', 'Brown St, Nelspruit Central', 'Mbombela', -25.4745, 30.9703, 'ABSA'),
('ABSA ATM - Sonpark Boulevard', 'Nel St, Sonpark', 'Mbombela', -25.4560, 30.9558, 'ABSA'),
('ABSA ATM - White River', 'Main Rd, White River', 'Mbombela', -25.3242, 31.0189, 'ABSA');

-- Vendors & Drivers (5) - PIN 1234
INSERT INTO users (phone_number, pin_hash, user_type, name, surname, id_number, email, phone_verified) VALUES
('0711234501', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'VENDOR', 'Lucky', 'Taxi', '9001015001081', 'lucky.taxi@demo.co.za', TRUE),
('0711234502', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'VENDOR', 'Thandi', 'Spaza', '9102025002082', 'thandi.spaza@demo.co.za', TRUE),
('0711234503', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'VENDOR', 'Mama', 'Joy', '9203035003083', 'mamajoy@demo.co.za', TRUE),
('0711234504', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'VENDOR', 'Sipho', 'Electrical', '9304045004084', 'sipho.electrical@demo.co.za', TRUE),
('0711234505', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'VENDOR', 'Nomsa', 'Fashion', '9405055005085', 'nomsa.fashion@demo.co.za', TRUE);

-- Taxi Drivers & Association Admins (10) - PIN 1234
INSERT INTO users (phone_number, pin_hash, user_type, name, surname, id_number, email, phone_verified) VALUES
('0798765432', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'TAXI_DRIVER', 'Karabo', 'Mokoena', '9506065006086', 'karabo.m@taxi.co.za', TRUE),
('0798765433', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'TAXI_DRIVER', 'Lerato', 'Dube', '9607075007087', 'lerato.d@taxi.co.za', TRUE),
('0798765434', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'TAXI_DRIVER', 'Sizwe', 'Nkosi', '9708085008088', 'sizwe.n@taxi.co.za', TRUE),
('0798765435', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'TAXI_DRIVER', 'Amanda', 'van der Merwe', '9809095009089', 'amanda.vdm@taxi.co.za', TRUE),
('0798765436', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'TAXI_DRIVER', 'Bongani', 'Zulu', '9910105010080', 'bongani.z@taxi.co.za', TRUE),
('0798765437', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'TAXI_ASSOCIATION_ADMIN', 'Precious', 'Khumalo', '8511115011081', 'precious.k@taxiassoc.co.za', TRUE),
('0798765438', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'TAXI_ASSOCIATION_ADMIN', 'Johan', 'Botha', '8612125012082', 'johan.b@taxiassoc.co.za', TRUE),
('0798765439', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'TAXI_DRIVER', 'Zanele', 'Mahlangu', '8701135013083', 'zanele.m@taxi.co.za', TRUE),
('0798765440', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'TAXI_DRIVER', 'Ryan', 'Naidoo', '8802145014084', 'ryan.n@taxi.co.za', TRUE),
('0798765441', '$2a$10$3Qc.4DmIvCCW4zKtF4w//ebf8KJRJxV3iGHTv1IDRAjw60W6UZT5O', 'TAXI_DRIVER', 'Nokuthula', 'Ndlovu', '8903155015085', 'nokuthula.n@taxi.co.za', TRUE);

-- Wallets for all 15 users (all accounts initialized with standard balances)
INSERT INTO wallets (user_id, balance, cashback_balance)
SELECT id, CASE WHEN user_type = 'TAXI_ASSOCIATION_ADMIN' THEN 10000.00 ELSE 1000.00 END, 0.00
FROM users;

-- Vendor profiles
INSERT INTO vendors (user_id, business_name, category, location_name, latitude, longitude, qr_code, verified, rating_avg, rating_count) VALUES
((SELECT id FROM users WHERE phone_number = '0711234501'), 'Lucky Taxi', 'TAXI', 'Mbombela Taxi Rank', -25.4745, 30.9703, 'UKP-VENDOR-LUCKYTAXI-001', TRUE, 4.8, 24),
((SELECT id FROM users WHERE phone_number = '0711234502'), 'Thandi''s Spaza Shop', 'RETAIL', 'KaNyamazane Taxi Rank', -25.4308, 31.2085, 'UKP-VENDOR-THANDISPAZA-002', TRUE, 4.6, 18),
((SELECT id FROM users WHERE phone_number = '0711234503'), 'Mama Joy Kitchen', 'FOOD', 'Sonheuwel Taxi Rank', -25.4667, 30.9575, 'UKP-VENDOR-MAMAJOY-003', TRUE, 4.9, 31),
((SELECT id FROM users WHERE phone_number = '0711234504'), 'Sipho Electrical Services', 'SERVICES', 'Kabokweni Taxi Rank', -25.4132, 31.1425, 'UKP-VENDOR-SIPHOELEC-004', TRUE, 4.7, 12),
((SELECT id FROM users WHERE phone_number = '0711234505'), 'Nomsa Fashion Retail', 'RETAIL', 'White River Taxi Rank', -25.4501, 31.0126, 'UKP-VENDOR-NOMSAFASHION-005', FALSE, 4.3, 7);

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
=======
>>>>>>> 64b97030878b67831e527c719de4297ec8551cac
