-- ============================================================================
-- Omnibus — V1 Initial Schema
-- PostgreSQL 16 | All money columns use NUMERIC(19,4) | UUIDs for PKs
-- ============================================================================

-- ==================== USERS ====================
CREATE TABLE users (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    username        VARCHAR(100)    NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    role            VARCHAR(20)     NOT NULL DEFAULT 'USER',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email)
);

-- ==================== ACCOUNTS ====================
CREATE TABLE accounts (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    account_number  VARCHAR(20)     NOT NULL,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'USD',
    balance         NUMERIC(19,4)   NOT NULL DEFAULT 0,
    status          VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT uq_accounts_number UNIQUE (account_number),
    CONSTRAINT fk_accounts_user   FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);

-- ==================== TRANSACTIONS ====================
CREATE TABLE transactions (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key     VARCHAR(255),
    type                VARCHAR(30)     NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    source_account_id   UUID,
    target_account_id   UUID,
    amount              NUMERIC(19,4)   NOT NULL,
    description         TEXT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    completed_at        TIMESTAMPTZ,

    CONSTRAINT uq_transactions_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_transactions_source FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    CONSTRAINT fk_transactions_target FOREIGN KEY (target_account_id) REFERENCES accounts(id),
    CONSTRAINT chk_transaction_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transaction_type CHECK (type IN ('TRANSFER', 'DEPOSIT', 'WITHDRAWAL', 'FEE')),
    CONSTRAINT chk_transaction_status CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED'))
);

-- ==================== LEDGER ENTRIES (Double-Entry) ====================
CREATE TABLE ledger_entries (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID            NOT NULL,
    account_id      UUID            NOT NULL,
    entry_type      VARCHAR(6)      NOT NULL,
    amount          NUMERIC(19,4)   NOT NULL,
    balance_after   NUMERIC(19,4)   NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),

    CONSTRAINT fk_ledger_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
    CONSTRAINT fk_ledger_account     FOREIGN KEY (account_id) REFERENCES accounts(id),
    CONSTRAINT chk_entry_type        CHECK (entry_type IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_ledger_amount     CHECK (amount > 0)
);

CREATE INDEX idx_ledger_entries_txn     ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_account ON ledger_entries(account_id, created_at DESC);

-- ==================== IDEMPOTENCY KEYS ====================
CREATE TABLE idempotency_keys (
    key             VARCHAR(255)    PRIMARY KEY,
    user_id         UUID            NOT NULL,
    http_status     INTEGER,
    response_body   JSONB,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);

-- ==================== AUDIT LOGS ====================
CREATE TABLE audit_logs (
    id              BIGINT          GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       UUID            NOT NULL,
    action          VARCHAR(20)     NOT NULL,
    actor_id        UUID,
    before_snapshot JSONB,
    after_snapshot  JSONB,
    balance_before  NUMERIC(19,4),
    balance_after   NUMERIC(19,4),
    ip_address      INET,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_entity ON audit_logs(entity_type, entity_id, created_at DESC);
CREATE INDEX idx_audit_actor  ON audit_logs(actor_id, created_at DESC);
