-- ============================================================================
-- UKHONA PAY - PostgreSQL Schema & Demo Seed Data
-- EDHE Studentpreneurs Indaba 2026 - Market Connectivity Challenge
-- ============================================================================

DROP TABLE IF EXISTS service_purchases CASCADE;
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
    -- Membership/registration due - a once-off amount the association sets
    -- for itself and revisits only occasionally (e.g. yearly, as costs
    -- change), not a per-ride fee. Editable by that association's own admin
    -- or by the platform admin - see VendorController#updateAssociationDues
    -- and AdminService#updateAssociation.
    dues_amount     NUMERIC(10,2) NOT NULL DEFAULT 250.00 CHECK (dues_amount >= 0),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_taxi_associations_name_lower ON taxi_associations (LOWER(name));

-- ============================================================================
-- TAXI RANKS  (reference data - which rank a vendor trades at / admin oversees)
-- ============================================================================
CREATE TABLE taxi_ranks (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    location_name   VARCHAR(150),
    -- Which association this rank falls under - nullable at the DB level
    -- (a rank predating this column, or an edge case, shouldn't become
    -- unreferenceable) but required going forward at the application layer:
    -- signup and admin both pick an association first, then a rank scoped
    -- to it (see ReferenceDataController#listRanks, CreateTaxiRankRequest).
    association_id  BIGINT REFERENCES taxi_associations(id),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX ux_taxi_ranks_name_lower ON taxi_ranks (LOWER(name));

-- ============================================================================
-- USERS
-- ============================================================================
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    phone_number    VARCHAR(10) NOT NULL UNIQUE CHECK (phone_number ~ '^0[0-9]{9}$'),
    pin_hash        VARCHAR(255) NOT NULL,
    user_type       VARCHAR(25) NOT NULL CHECK (user_type IN ('VENDOR', 'TAXI_DRIVER', 'TAXI_ASSOCIATION_ADMIN', 'ADMIN')),
    name            VARCHAR(120) NOT NULL,
    surname         VARCHAR(120) NOT NULL,
    id_number       VARCHAR(13) NOT NULL UNIQUE CHECK (id_number ~ '^[0-9]{13}$'),
    email           VARCHAR(150),
    phone_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    association_id  BIGINT REFERENCES taxi_associations(id),
    rank_id         BIGINT REFERENCES taxi_ranks(id),
    -- Login brute-force lockout (see AuthService.login) - a handful of wrong
    -- PINs locks the account for a cooldown period rather than allowing
    -- unlimited guesses against a 10,000-combination 4-digit PIN space.
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

-- ============================================================================
-- WALLETS  (one wallet per user OR per taxi association)
-- ============================================================================
CREATE TABLE wallets (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    association_id  BIGINT UNIQUE REFERENCES taxi_associations(id),
    balance         NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    cashback_balance NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (cashback_balance >= 0),
    -- Auto-allocated on every incoming fare payment (see PaymentService) -
    -- earmarked pots the driver can see but that aren't part of the
    -- available `balance` used for bank withdrawals/transfers/payments.
    savings_balance     NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (savings_balance >= 0),
    maintenance_balance NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (maintenance_balance >= 0),
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
    status          VARCHAR(20) NOT NULL DEFAULT 'APPROVED' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    vehicle_registration VARCHAR(20),
    association_id  BIGINT REFERENCES taxi_associations(id),
    rank_id         BIGINT REFERENCES taxi_ranks(id),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_vendors_category ON vendors(category);

-- ============================================================================
-- TRANSACTIONS  (immutable ledger - vendor payments and driver transfers)
-- ============================================================================
CREATE TABLE transactions (
    id                  BIGSERIAL PRIMARY KEY,
    reference           VARCHAR(30) NOT NULL UNIQUE,
    sender_id           BIGINT REFERENCES users(id),
    receiver_id         BIGINT REFERENCES users(id),
    receiver_association_id BIGINT REFERENCES taxi_associations(id),
    vendor_id           BIGINT REFERENCES vendors(id),
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    cashback_amount     NUMERIC(12,2) NOT NULL DEFAULT 0,
    cashback_rate       NUMERIC(4,3) NOT NULL DEFAULT 0.025,
    -- Flat platform fee deducted from this transaction (0 for types it
    -- doesn't apply to, e.g. fines) - amount stays the gross figure charged
    -- to the sender/paid by the external payer; the receiver's actual credit
    -- is amount - platform_fee. See WalletService.PLATFORM_FEE.
    platform_fee        NUMERIC(12,2) NOT NULL DEFAULT 0,
    status              VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
    description         VARCHAR(255),
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    CHECK ((receiver_id IS NOT NULL AND receiver_association_id IS NULL) OR (receiver_id IS NULL AND receiver_association_id IS NOT NULL))
);

CREATE INDEX idx_transactions_sender ON transactions(sender_id);
CREATE INDEX idx_transactions_receiver ON transactions(receiver_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);

-- ============================================================================
-- BANK ACCOUNTS  (saved bank account for bank cashout / withdrawals)
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
-- BANK WITHDRAWALS  (vendor/driver payouts to bank)
-- ============================================================================
CREATE TABLE bank_withdrawals (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    bank_account_id BIGINT NOT NULL REFERENCES bank_accounts(id),
    reference       VARCHAR(30) NOT NULL UNIQUE,
    amount          NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    status          VARCHAR(20) NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED', 'FAILED')),
    created_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_bank_withdrawals_user ON bank_withdrawals(user_id);

-- ============================================================================
-- SERVICE PURCHASES  (airtime, prepaid electricity, Pay@ bills - real,
-- server-recorded purchases; see ServicePurchaseService for why the money
-- leaves the wallet without crediting any other UKHONA PAY wallet)
-- ============================================================================
CREATE TABLE service_purchases (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    type                VARCHAR(20) NOT NULL CHECK (type IN ('AIRTIME', 'ELECTRICITY', 'PAYAT_BILL')),
    reference           VARCHAR(20) NOT NULL UNIQUE,
    amount              NUMERIC(12,2) NOT NULL CHECK (amount > 0),
    network             VARCHAR(20),
    recipient_phone     VARCHAR(15),
    meter_number        VARCHAR(20),
    municipality        VARCHAR(100),
    biller_name         VARCHAR(100),
    biller_category     VARCHAR(60),
    payat_reference     VARCHAR(30),
    account_name        VARCHAR(150),
    voucher_token       VARCHAR(40) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_service_purchases_user ON service_purchases(user_id);

-- ============================================================================
-- SEED DATA: REFERENCE ASSOCIATIONS & RANKS
-- ============================================================================

INSERT INTO taxi_associations (id, name) VALUES
(1, 'Mbombela Local Taxi Association (MALTA)'),
(2, 'KaNyamazane Taxi Association (KATA)'),
(3, 'White River Taxi Association (WRTA)'),
(4, 'Ehlanzeni District Taxi Council');

INSERT INTO taxi_ranks (id, name, location_name, association_id) VALUES
(1, 'Mbombela Taxi Rank', 'Mbombela CBD', 1),
(2, 'KaNyamazane Taxi Rank', 'KaNyamazane', 2),
(3, 'Sonheuwel Taxi Rank', 'Sonheuwel', 1),
(4, 'Kabokweni Taxi Rank', 'Kabokweni', 1),
(5, 'White River Taxi Rank', 'White River', 3);

SELECT setval('taxi_associations_id_seq', (SELECT max(id) FROM taxi_associations));
SELECT setval('taxi_ranks_id_seq', (SELECT max(id) FROM taxi_ranks));

-- ============================================================================
-- SEED DATA: USERS
-- Each account below has its own unique PIN hash - PINs are distributed to
-- the team privately, not committed here. See README.md.
-- ============================================================================

INSERT INTO users (id, phone_number, pin_hash, user_type, name, surname, id_number, email, phone_verified, association_id, rank_id) VALUES
-- Demo Accounts
-- Each row below has its own unique PIN hash (never share one hash across
-- accounts in a public repo: a leaked/cracked hash would unlock every
-- account that reused it, including the platform ADMIN). The actual PINs
-- are NOT committed here - they're distributed to the team out-of-band.
(1, '0711234501', '$2a$10$EiyMmxDnxcT08z3bIn2m0OwdSJBFY.fe4i4gBxoE.XoZgPPOBH7ju', 'TAXI_DRIVER', 'Lucky', 'Taxi', '9001015001081', 'lucky.taxi@demo.co.za', TRUE, 1, 1),
(2, '0798765432', '$2a$10$EUOJ9ko.eYmWsUFC9ohqDekmLkfaIYPC33T8xPQgnW0gEXemJ2nr2', 'TAXI_DRIVER', 'Karabo', 'Mokoena', '9506065006086', 'karabo.m@taxi.co.za', TRUE, 1, 1),
(3, '0711234502', '$2a$10$UbwYPXrUWjK41FiFjud7zO2t687ZV/nFNhqcaJmI6Hatl5GbPesPW', 'VENDOR', 'Thandi', 'Spaza', '9102025002082', 'thandi.spaza@demo.co.za', TRUE, NULL, 2),
(4, '0711234503', '$2a$10$17MGp9BXnpV4lxycQEFEMe9wYunrBO3YKp3uvr2rxfB.MFxyt3BQa', 'VENDOR', 'Mama', 'Joy', '9203035003083', 'mamajoy@demo.co.za', TRUE, NULL, 3),
(5, '0711234504', '$2a$10$SGFK3Irqa7nfe8et7kZsguH4vamlwUcgVuycyMUnSgWZ7smrjsEGS', 'VENDOR', 'Sipho', 'Electrical', '9304045004084', 'sipho.electrical@demo.co.za', TRUE, NULL, 4),
(6, '0711234505', '$2a$10$Bf1m/5S5p1Ot2kCzWGWYT.QqvfWUr2ZSk1BCS0g6ZM.gOdrjdNL82', 'VENDOR', 'Nomsa', 'Fashion', '9405055005085', 'nomsa.fashion@demo.co.za', TRUE, NULL, 5),
(7, '0798765437', '$2a$10$X4JU47tQ.R2TD8qos9Jg1OLMEwiKiSHCbIUwMYHTghDOPZomBe6.S', 'TAXI_ASSOCIATION_ADMIN', 'Precious', 'Khumalo', '8511115011081', 'precious.k@taxiassoc.co.za', TRUE, 1, 1),

-- Dedicated Collaborator Accounts (unique PIN per account - see note above)
(8, '0712345678', '$2a$10$Ak7IXs0hd0qdu9gVyP12VOBbWEGZGuefHD0dQWpU0ibFynZF1nZoO', 'TAXI_DRIVER', 'Hlayiseko', 'Bennet', '9601015001082', 'nhlayisekobennet07@gmail.com', TRUE, 1, 1),
(9, '0712345679', '$2a$10$U4R3YD46kYuzNMPOP7fnKOq7f9UGqzxK8AHtouhwVcXBbf2.ZCk6i', 'TAXI_DRIVER', 'Vuyo', 'Mthembu', '9702025002083', 'vuyo@demo.co.za', TRUE, 1, 1),
(10, '0712345680', '$2a$10$nue5K.P9BCocxkYZF9ry.OqE2Flif5NnfmPY/3FaCHVx4yA.3ekP.', 'TAXI_DRIVER', 'Banele', 'Sithole', '9803035003084', 'banele@demo.co.za', TRUE, 1, 1),
(11, '0791234567', '$2a$10$ozwlvCUhUi9krcjQP7e.gO0HBAc0dn7NsjkbHihuVwj/5JG7tLzri', 'TAXI_ASSOCIATION_ADMIN', 'Phathutshedzo', 'Mamagau', '9904045004085', '230157688@ump.ac.za', TRUE, 1, 1),

-- Platform administrator - full-control account, not reachable through public
-- signup (AuthService rejects userType=ADMIN there). No association/rank: an
-- admin oversees the whole platform, not one association.
(12, '0700000001', '$2a$10$qhnPaI6mM/bSDVf5lHi6FekKh4dG8dF8/UlIqocbrt/XmS7CGd76a', 'ADMIN', 'Platform', 'Admin', '0000000000001', 'admin@ukhonapay.co.za', TRUE, NULL, NULL);

SELECT setval('users_id_seq', (SELECT max(id) FROM users));

-- ============================================================================
-- SEED DATA: WALLETS
-- ============================================================================

INSERT INTO wallets (user_id, balance, cashback_balance, currency) VALUES
(1, 1500.00, 0.00, 'ZAR'),
(2, 1200.00, 0.00, 'ZAR'),
(3, 2450.00, 0.00, 'ZAR'),
(4, 1890.00, 0.00, 'ZAR'),
(5, 800.00, 0.00, 'ZAR'),
(6, 450.00, 0.00, 'ZAR'),
(7, 15000.00, 0.00, 'ZAR'),
(8, 1000.00, 0.00, 'ZAR'),
(9, 1000.00, 0.00, 'ZAR'),
(10, 1000.00, 0.00, 'ZAR'),
(11, 10000.00, 0.00, 'ZAR'),
-- Platform administrator's wallet - collects the R1 platform fee off every
-- user-initiated transaction (see WalletService.getLockedPlatformFeeWallet).
(12, 0.00, 0.00, 'ZAR');

-- Association Wallet for MALTA
INSERT INTO wallets (association_id, balance, cashback_balance, currency) VALUES
(1, 25000.00, 0.00, 'ZAR');

-- ============================================================================
-- SEED DATA: VENDORS
-- ============================================================================

INSERT INTO vendors (id, user_id, business_name, category, location_name, latitude, longitude, qr_code, verified, status, vehicle_registration, association_id, rank_id) VALUES
(1, 1, 'Lucky Taxi', 'TAXI', 'Mbombela Taxi Rank', -25.4745, 30.9703, 'UKP-VENDOR-LUCKYTAXI-001', TRUE, 'APPROVED', 'DX45FGMP', 1, 1),
(2, 2, 'Karabo Transport', 'TAXI', 'Mbombela Taxi Rank', -25.4745, 30.9703, 'UKP-VENDOR-KARABO-002', TRUE, 'APPROVED', 'CA123456', 1, 1),
(3, 3, 'Thandi''s Spaza Shop', 'RETAIL', 'KaNyamazane Taxi Rank', -25.4308, 31.2085, 'UKP-VENDOR-THANDISPAZA-003', TRUE, 'APPROVED', NULL, NULL, 2),
(4, 4, 'Mama Joy Kitchen', 'FOOD', 'Sonheuwel Taxi Rank', -25.4667, 30.9575, 'UKP-VENDOR-MAMAJOY-004', TRUE, 'APPROVED', NULL, NULL, 3),
(5, 5, 'Sipho Electrical Services', 'SERVICES', 'Kabokweni Taxi Rank', -25.4132, 31.1425, 'UKP-VENDOR-SIPHOELEC-005', TRUE, 'APPROVED', NULL, NULL, 4),
(6, 6, 'Nomsa Fashion Retail', 'RETAIL', 'White River Taxi Rank', -25.4501, 31.0126, 'UKP-VENDOR-NOMSAFASHION-006', FALSE, 'APPROVED', NULL, NULL, 5),
(7, 8, 'Hlayiseko Express', 'TAXI', 'Mbombela Taxi Rank', -25.4745, 30.9703, 'UKP-VENDOR-HLAYISEKO-007', TRUE, 'APPROVED', 'MP2026HL', 1, 1),
(8, 9, 'Vuyo Cabs', 'TAXI', 'Mbombela Taxi Rank', -25.4745, 30.9703, 'UKP-VENDOR-VUYO-008', TRUE, 'APPROVED', 'MP2026VY', 1, 1),
(9, 10, 'Banele Transport', 'TAXI', 'Mbombela Taxi Rank', -25.4745, 30.9703, 'UKP-VENDOR-BANELE-009', TRUE, 'APPROVED', 'MP2026BN', 1, 1);

SELECT setval('vendors_id_seq', (SELECT max(id) FROM vendors));

-- ============================================================================
-- SEED DATA: SAVED BANK ACCOUNTS
-- ============================================================================

INSERT INTO bank_accounts (user_id, account_holder_name, bank_name, account_number, branch_code) VALUES
(1, 'Lucky Taxi', 'ABSA Bank', '4050607080', '632005'),
(2, 'Karabo Mokoena', 'First National Bank (FNB)', '62012345678', '250655'),
(3, 'Thandi Spaza', 'Capitec Bank', '1234567890', '470010'),
(4, 'Mama Joy', 'Standard Bank', '020304050', '051001'),
(8, 'Hlayiseko Bennet', 'ABSA Bank', '4098765432', '632005'),
(9, 'Vuyo Mthembu', 'Capitec Bank', '1890123456', '470010'),
(10, 'Banele Sithole', 'First National Bank (FNB)', '62890123456', '250655');

-- ============================================================================
-- SEED DATA: SAMPLE TRANSACTIONS
-- ============================================================================

INSERT INTO transactions (reference, sender_id, receiver_id, vendor_id, amount, cashback_amount, cashback_rate, status, description, created_at) VALUES
('TXN-1001', NULL, 1, 1, 150.00, 3.75, 0.025, 'COMPLETED', 'Taxi fare - Nelspruit CBD to KaNyamazane', now() - interval '25 days'),
('TXN-1002', NULL, 1, 1, 180.00, 4.50, 0.025, 'COMPLETED', 'Taxi fare - return trip', now() - interval '23 days'),
('TXN-1003', NULL, 3, 3, 240.00, 6.00, 0.025, 'COMPLETED', 'Spaza groceries', now() - interval '20 days'),
('TXN-1004', NULL, 4, 4, 95.00, 2.38, 0.025, 'COMPLETED', 'Lunch order - Sonheuwel', now() - interval '18 days'),
('TXN-1005', NULL, 1, 1, 200.00, 5.00, 0.025, 'COMPLETED', 'Taxi fare - Airport transfer', now() - interval '15 days'),
('TXN-1006', NULL, 1, 1, 150.00, 3.75, 0.025, 'COMPLETED', 'Taxi fare - White River run', now() - interval '12 days'),
('TXN-1007', NULL, 3, 3, 310.00, 7.75, 0.025, 'COMPLETED', 'Household goods', now() - interval '10 days'),
('TXN-1008', NULL, 1, 1, 180.00, 4.50, 0.025, 'COMPLETED', 'Taxi fare - KaNyamazane', now() - interval '8 days'),
('TXN-1009', NULL, 4, 4, 120.00, 3.00, 0.025, 'COMPLETED', 'Lunch order', now() - interval '6 days'),
('TXN-1010', NULL, 1, 1, 150.00, 3.75, 0.025, 'COMPLETED', 'Taxi fare - Nelspruit CBD', now() - interval '4 days'),
('TXN-1011', NULL, 1, 1, 220.00, 5.50, 0.025, 'COMPLETED', 'Taxi fare - Sonheuwel run', now() - interval '2 days'),
('TXN-1012', NULL, 1, 1, 180.00, 4.50, 0.025, 'COMPLETED', 'Taxi fare - Morning commute', now() - interval '1 days'),
('TXN-1013', NULL, 8, 7, 200.00, 5.00, 0.025, 'COMPLETED', 'Taxi fare - Nelspruit CBD', now() - interval '1 days');

INSERT INTO transactions (reference, sender_id, receiver_association_id, amount, status, description, created_at) VALUES
('TXN-ASSOC-001', 1, 1, 250.00, 'COMPLETED', 'Driver Association Dues - MALTA', now() - interval '5 days'),
('TXN-ASSOC-002', 8, 1, 250.00, 'COMPLETED', 'Driver Association Dues - MALTA', now() - interval '2 days');
