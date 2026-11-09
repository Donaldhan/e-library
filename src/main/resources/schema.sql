-- Schema is owned by explicit SQL rather than Hibernate DDL generation: SQLite's ALTER support is
-- limited and partial unique indexes are the backbone of the business invariants below.

CREATE TABLE IF NOT EXISTS members (
    id                  TEXT PRIMARY KEY,
    name                TEXT NOT NULL,
    email               TEXT NOT NULL UNIQUE,
    membership_type     TEXT NOT NULL,
    max_concurrent_loans INTEGER NOT NULL CHECK (max_concurrent_loans >= 0),
    token               TEXT NOT NULL UNIQUE,
    created_at          TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS books (
    id                 TEXT PRIMARY KEY,
    title              TEXT NOT NULL,
    author             TEXT NOT NULL,
    isbn               TEXT NOT NULL,
    category           TEXT NOT NULL,
    publisher          TEXT NOT NULL,
    published_year     INTEGER NOT NULL,
    language           TEXT NOT NULL,
    description        TEXT NOT NULL DEFAULT '',
    cover_url          TEXT,
    total_licenses     INTEGER NOT NULL CHECK (total_licenses >= 0),
    available_licenses INTEGER NOT NULL
        CHECK (available_licenses >= 0 AND available_licenses <= total_licenses),
    loan_period_days   INTEGER NOT NULL CHECK (loan_period_days > 0),
    created_at         TEXT NOT NULL,
    updated_at         TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS loans (
    id          TEXT PRIMARY KEY,
    book_id     TEXT NOT NULL REFERENCES books (id),
    member_id   TEXT NOT NULL REFERENCES members (id),
    borrowed_at TEXT NOT NULL,
    due_at      TEXT NOT NULL,
    returned_at TEXT
);

-- One member can hold at most one active loan per title, enforced by the database.
CREATE UNIQUE INDEX IF NOT EXISTS ux_loans_active_title
    ON loans (book_id, member_id) WHERE returned_at IS NULL;
CREATE INDEX IF NOT EXISTS ix_loans_member ON loans (member_id, returned_at);
CREATE INDEX IF NOT EXISTS ix_loans_book ON loans (book_id, returned_at);

CREATE TABLE IF NOT EXISTS reservations (
    id         TEXT PRIMARY KEY,
    book_id    TEXT NOT NULL REFERENCES books (id),
    member_id  TEXT NOT NULL REFERENCES members (id),
    status     TEXT NOT NULL,
    created_at TEXT NOT NULL,
    ready_at   TEXT,
    expires_at TEXT,
    updated_at TEXT NOT NULL
);

-- One member can wait for a title at most once.
CREATE UNIQUE INDEX IF NOT EXISTS ux_reservations_active_title
    ON reservations (book_id, member_id) WHERE status IN ('WAITING', 'READY');
CREATE INDEX IF NOT EXISTS ix_reservations_queue ON reservations (book_id, status, created_at);
CREATE INDEX IF NOT EXISTS ix_reservations_member ON reservations (member_id, status);
