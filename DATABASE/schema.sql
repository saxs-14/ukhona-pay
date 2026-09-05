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
