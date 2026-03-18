-- User Session & RBAC Migration
-- Creates known_users and audit_log tables, alters existing tables for user attribution

-- 1. Known users table (lazy registry populated from OAuth2-proxy headers)
CREATE TABLE known_users (
    username VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255),
    display_name VARCHAR(255),
    groups_csv VARCHAR(1000),
    first_seen_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_known_users_last_seen ON known_users(last_seen_at);

-- 2. Audit log table
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    performed_by VARCHAR(255),
    details JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_performed_by ON audit_log(performed_by);
CREATE INDEX idx_audit_log_created_at ON audit_log(created_at);

-- 3. Alter escalations: add audit columns
ALTER TABLE escalations ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);
ALTER TABLE escalations ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE escalations ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

-- 4. Alter comments: rename columns, add polymorphic support
ALTER TABLE comments RENAME COLUMN author TO created_by;
ALTER TABLE comments RENAME COLUMN comment TO body;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS entity_type VARCHAR(50) DEFAULT 'VULNERABILITY';
ALTER TABLE comments ADD COLUMN IF NOT EXISTS entity_id BIGINT;
ALTER TABLE comments ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE comments ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

-- Backfill entity_id from vulnerability_id for existing records
UPDATE comments SET entity_id = vulnerability_id WHERE entity_id IS NULL AND vulnerability_id IS NOT NULL;

-- 5. Alter triage_decisions: add audit columns
ALTER TABLE triage_decisions ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);
ALTER TABLE triage_decisions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;
