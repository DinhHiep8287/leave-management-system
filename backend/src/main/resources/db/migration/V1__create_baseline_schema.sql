-- =====================================================================
-- V1: Baseline schema for Leave Management System
-- Tables: departments, users, leave_types, leave_balances, holidays,
--         leave_requests, approval_actions, audit_log, refresh_tokens
-- See docs/DATABASE.md for design notes.
-- =====================================================================

-- ===== departments =====
CREATE TABLE departments (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(50)  NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    head_user_id    BIGINT,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

-- ===== users =====
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    employee_code   VARCHAR(50)  NOT NULL UNIQUE,
    email           VARCHAR(200) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    role            VARCHAR(20)  NOT NULL,
    department_id   BIGINT       NOT NULL,
    manager_id      BIGINT,
    join_date       DATE         NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT ck_users_role CHECK (role IN ('EMPLOYEE','MANAGER','HR','ADMIN'))
);

CREATE INDEX idx_users_department ON users(department_id);
CREATE INDEX idx_users_manager    ON users(manager_id);

-- ===== Add cross-FKs after both tables exist =====
ALTER TABLE users
    ADD CONSTRAINT fk_users_department FOREIGN KEY (department_id) REFERENCES departments(id),
    ADD CONSTRAINT fk_users_manager    FOREIGN KEY (manager_id)    REFERENCES users(id);

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_head FOREIGN KEY (head_user_id) REFERENCES users(id);

-- ===== leave_types =====
CREATE TABLE leave_types (
    id                   BIGSERIAL PRIMARY KEY,
    code                 VARCHAR(50)  NOT NULL UNIQUE,
    name                 VARCHAR(200) NOT NULL,
    description          TEXT,
    default_quota_days   NUMERIC(5,1) NOT NULL,
    requires_balance     BOOLEAN      NOT NULL DEFAULT TRUE,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL,
    created_by           VARCHAR(100),
    updated_by           VARCHAR(100)
);

-- ===== leave_balances =====
CREATE TABLE leave_balances (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    leave_type_id   BIGINT       NOT NULL REFERENCES leave_types(id),
    year            INT          NOT NULL,
    total_days      NUMERIC(5,1) NOT NULL,
    used_days       NUMERIC(5,1) NOT NULL DEFAULT 0,
    adjusted_days   NUMERIC(5,1) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT uq_balance_user_type_year UNIQUE (user_id, leave_type_id, year)
);

CREATE INDEX idx_balance_user ON leave_balances(user_id);

-- ===== holidays =====
CREATE TABLE holidays (
    id              BIGSERIAL PRIMARY KEY,
    holiday_date    DATE         NOT NULL UNIQUE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100)
);

-- ===== leave_requests =====
CREATE TABLE leave_requests (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id),
    leave_type_id   BIGINT       NOT NULL REFERENCES leave_types(id),
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    start_half      VARCHAR(20)  NOT NULL,
    end_half        VARCHAR(20)  NOT NULL,
    total_days      NUMERIC(5,1) NOT NULL,
    reason          TEXT         NOT NULL,
    status          VARCHAR(20)  NOT NULL,
    manager_id      BIGINT       NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    CONSTRAINT ck_request_status   CHECK (status     IN ('PENDING','APPROVED','REJECTED','CANCELLED')),
    CONSTRAINT ck_request_start_h  CHECK (start_half IN ('FULL_DAY','MORNING','AFTERNOON')),
    CONSTRAINT ck_request_end_h    CHECK (end_half   IN ('FULL_DAY','MORNING','AFTERNOON')),
    CONSTRAINT ck_request_dates    CHECK (end_date >= start_date),
    CONSTRAINT ck_request_days_pos CHECK (total_days > 0)
);

CREATE INDEX idx_request_user    ON leave_requests(user_id);
CREATE INDEX idx_request_manager ON leave_requests(manager_id);
CREATE INDEX idx_request_status  ON leave_requests(status);
CREATE INDEX idx_request_dates   ON leave_requests(start_date, end_date);

-- ===== approval_actions =====
CREATE TABLE approval_actions (
    id                BIGSERIAL PRIMARY KEY,
    leave_request_id  BIGINT      NOT NULL REFERENCES leave_requests(id) ON DELETE CASCADE,
    actor_id          BIGINT      NOT NULL REFERENCES users(id),
    action            VARCHAR(20) NOT NULL,
    previous_status   VARCHAR(20),
    new_status        VARCHAR(20),
    comment           TEXT,
    created_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_action_action CHECK (action IN ('CREATED','UPDATED','APPROVED','REJECTED','CANCELLED','OVERRIDE'))
);

CREATE INDEX idx_approval_request ON approval_actions(leave_request_id);

-- ===== audit_log =====
CREATE TABLE audit_log (
    id            BIGSERIAL PRIMARY KEY,
    actor_id      BIGINT      REFERENCES users(id),
    action        VARCHAR(50) NOT NULL,
    target_type   VARCHAR(50) NOT NULL,
    target_id     BIGINT,
    old_value     JSONB,
    new_value     JSONB,
    created_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_target ON audit_log(target_type, target_id);
CREATE INDEX idx_audit_actor  ON audit_log(actor_id);

-- ===== refresh_tokens =====
CREATE TABLE refresh_tokens (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash    VARCHAR(255) NOT NULL UNIQUE,
    expires_at    TIMESTAMPTZ  NOT NULL,
    revoked_at    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_refresh_user  ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_token ON refresh_tokens(token_hash);
