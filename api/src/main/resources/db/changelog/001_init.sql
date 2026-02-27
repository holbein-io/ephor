-- Ephor - Complete Database Schema
-- Consolidated from incremental migrations into a single init script.

-- ============================================================================
-- Functions
-- ============================================================================

-- Generic BEFORE UPDATE trigger: sets updated_at = CURRENT_TIMESTAMP
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- AFTER INSERT on vulnerability_instances: updates vulnerabilities.last_seen
CREATE OR REPLACE FUNCTION update_vulnerability_last_seen()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE vulnerabilities
    SET last_seen = NOW()
    WHERE id = NEW.vulnerability_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- Tables (in dependency order)
-- ============================================================================

-- 1. Scan tracking
CREATE TABLE scans (
    id SERIAL PRIMARY KEY,
    namespace VARCHAR(255) NOT NULL,
    scan_label VARCHAR(255) NOT NULL,
    scan_group_id UUID,
    status VARCHAR(20) NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    trivy_version VARCHAR(50),
    scan_config JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 2. CVE records
CREATE TABLE vulnerabilities (
    id SERIAL PRIMARY KEY,
    cve_id VARCHAR(50) NOT NULL,
    package_name VARCHAR(255) NOT NULL,
    package_version VARCHAR(100),
    severity VARCHAR(20) NOT NULL,
    title TEXT,
    description TEXT,
    primary_url VARCHAR(500),
    published_date TIMESTAMPTZ,
    fixed_version VARCHAR(100),
    scanner_type VARCHAR(100),
    first_detected TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_seen TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_vulnerability UNIQUE(cve_id, package_name, package_version, scanner_type)
);

-- 3. Kubernetes workload resources
CREATE TABLE workloads (
    id SERIAL PRIMARY KEY,
    scan_id INTEGER REFERENCES scans(id),
    last_scan_id INTEGER REFERENCES scans(id),
    namespace VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    kind VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_workload_identity UNIQUE(namespace, name, kind)
);

-- 4. Container images per workload
CREATE TABLE containers (
    id SERIAL PRIMARY KEY,
    workload_id INTEGER NOT NULL REFERENCES workloads(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    image_name VARCHAR(500),
    image_tag VARCHAR(255),
    image_created TIMESTAMPTZ,
    base_image_created TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_container_identity UNIQUE(workload_id, name)
);

-- 5. Links vulnerabilities to containers with status lifecycle
CREATE TABLE vulnerability_instances (
    id SERIAL PRIMARY KEY,
    vulnerability_id INTEGER NOT NULL REFERENCES vulnerabilities(id),
    container_id INTEGER NOT NULL REFERENCES containers(id),
    scan_id INTEGER REFERENCES scans(id),
    status VARCHAR(50) DEFAULT 'open',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMPTZ,
    resolution_type VARCHAR(20),
    resolution_reason TEXT,
    resolved_by_scan_id INTEGER REFERENCES scans(id),

    CONSTRAINT unique_vuln_container UNIQUE(vulnerability_id, container_id)
);

-- 6. Triage, escalation, and resolution comments on vulnerabilities
CREATE TABLE comments (
    id SERIAL PRIMARY KEY,
    vulnerability_id INTEGER REFERENCES vulnerabilities(id),
    author VARCHAR(255),
    comment TEXT NOT NULL,
    comment_type VARCHAR(50) DEFAULT 'triage',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 7. Escalation tracking
CREATE TABLE escalations (
    id SERIAL PRIMARY KEY,
    vulnerability_id INTEGER REFERENCES vulnerabilities(id),
    escalation_level INTEGER DEFAULT 1,
    escalated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    escalated_by VARCHAR(255),
    reason TEXT,
    ms_teams_message_id VARCHAR(255),
    status VARCHAR(50) DEFAULT 'pending',
    priority VARCHAR(50) DEFAULT 'medium',
    due_date TIMESTAMPTZ
);

-- 8. Triage session lifecycle (PREPARING -> ACTIVE -> COMPLETED)
CREATE TABLE triage_sessions (
    id SERIAL PRIMARY KEY,
    session_date DATE NOT NULL,
    attendees TEXT[],
    status VARCHAR(20) DEFAULT 'PREPARING',
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    prep_completed_at TIMESTAMPTZ,
    session_started_at TIMESTAMPTZ,
    prep_lead VARCHAR(255),
    prep_notes TEXT,
    session_duration_minutes INTEGER,
    prep_duration_minutes INTEGER,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 9. Preparation phase items per triage session
CREATE TABLE triage_preparations (
    id SERIAL PRIMARY KEY,
    session_id INTEGER REFERENCES triage_sessions(id) ON DELETE CASCADE,
    vulnerability_id INTEGER REFERENCES vulnerabilities(id) ON DELETE CASCADE,
    prep_status VARCHAR(50) NOT NULL DEFAULT 'pending',
    prep_notes TEXT,
    preliminary_decision VARCHAR(50),
    priority_flag VARCHAR(20) DEFAULT 'medium',
    prep_by VARCHAR(255) NOT NULL,
    prep_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT unique_prep_vuln_session UNIQUE(session_id, vulnerability_id)
);

-- 10. Per-vulnerability triage decisions
CREATE TABLE triage_decisions (
    id SERIAL PRIMARY KEY,
    session_id INTEGER REFERENCES triage_sessions(id) ON DELETE CASCADE,
    vulnerability_id INTEGER REFERENCES vulnerabilities(id),
    decision VARCHAR(50),
    assigned_to VARCHAR(100),
    target_date DATE,
    notes TEXT,
    decided_by VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 11. Bulk action plans with JSONB filters
CREATE TABLE triage_bulk_plans (
    id SERIAL PRIMARY KEY,
    session_id INTEGER REFERENCES triage_sessions(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    filters JSONB NOT NULL,
    estimated_count INTEGER DEFAULT 0,
    actual_count INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'planned',
    action VARCHAR(50) NOT NULL,
    metadata JSONB,
    created_during_prep BOOLEAN DEFAULT true,
    executed_at TIMESTAMPTZ,
    executed_by VARCHAR(255),
    created_by VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 12. Execution log of bulk operations
CREATE TABLE triage_bulk_operations (
    id SERIAL PRIMARY KEY,
    session_id INTEGER REFERENCES triage_sessions(id) ON DELETE CASCADE,
    bulk_plan_id INTEGER REFERENCES triage_bulk_plans(id) ON DELETE CASCADE,
    vulnerability_ids INTEGER[],
    operation_type VARCHAR(50) NOT NULL,
    metadata JSONB,
    executed_by VARCHAR(255) NOT NULL,
    executed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 13. Session analytics (PK = session_id, FK to triage_sessions)
CREATE TABLE triage_session_metrics (
    session_id INTEGER PRIMARY KEY REFERENCES triage_sessions(id) ON DELETE CASCADE,
    total_vulnerabilities INTEGER NOT NULL DEFAULT 0,
    decisions_made INTEGER NOT NULL DEFAULT 0,
    bulk_operations_count INTEGER DEFAULT 0,
    individual_decisions_count INTEGER DEFAULT 0,
    prep_duration_minutes INTEGER,
    session_duration_minutes INTEGER,
    efficiency_score DECIMAL(5,2),
    prep_completion_rate DECIMAL(5,2),
    decision_breakdown JSONB,
    calculated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 14. Remediation lifecycle
CREATE TABLE remediations (
    id SERIAL PRIMARY KEY,
    vulnerability_id INTEGER NOT NULL REFERENCES vulnerabilities(id) ON DELETE CASCADE,
    triage_decision_id INTEGER REFERENCES triage_decisions(id) ON DELETE SET NULL,
    assigned_to VARCHAR(255),
    target_date DATE,
    priority VARCHAR(20),
    status VARCHAR(50) DEFAULT 'planned',
    completed_at TIMESTAMPTZ,
    completion_method VARCHAR(50),
    completed_by VARCHAR(255),
    notes TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 15. Append-only audit log for remediations
CREATE TABLE remediation_comments (
    id SERIAL PRIMARY KEY,
    remediation_id INTEGER NOT NULL REFERENCES remediations(id) ON DELETE CASCADE,
    author VARCHAR(255) NOT NULL DEFAULT 'system',
    comment TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 16. Legacy notification settings (kept for backward compatibility)
CREATE TABLE notification_settings (
    id SERIAL PRIMARY KEY,
    setting_name VARCHAR(100) UNIQUE NOT NULL,
    setting_value JSONB NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 17. Categorized system configuration with defaults
CREATE TABLE system_settings (
    id SERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value JSONB NOT NULL,
    description TEXT,
    default_value JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(category, setting_key)
);

-- ============================================================================
-- Indexes
-- ============================================================================

-- scans
CREATE INDEX idx_scans_status ON scans(status);
CREATE INDEX idx_scans_started_at ON scans(started_at);
CREATE INDEX idx_scans_scan_group_id ON scans(scan_group_id);

-- vulnerabilities
CREATE INDEX idx_vulns_cve ON vulnerabilities(cve_id);
CREATE INDEX idx_vulns_severity ON vulnerabilities(severity);
CREATE INDEX idx_vulns_first_detected ON vulnerabilities(first_detected);

-- workloads
CREATE INDEX idx_workloads_identity ON workloads(namespace, name, kind);
CREATE INDEX idx_workloads_last_scan ON workloads(last_scan_id);

-- containers
CREATE INDEX idx_containers_workload ON containers(workload_id);

-- vulnerability_instances
CREATE INDEX idx_instances_status ON vulnerability_instances(status);
CREATE INDEX idx_instances_resolved_at ON vulnerability_instances(resolved_at);
CREATE INDEX idx_instances_resolution_type ON vulnerability_instances(resolution_type);
CREATE INDEX idx_vuln_instances_container ON vulnerability_instances(container_id);
CREATE INDEX idx_vuln_instances_vulnerability ON vulnerability_instances(vulnerability_id);

-- escalations
CREATE INDEX idx_escalations_status ON escalations(status);
CREATE INDEX idx_escalations_priority ON escalations(priority);
CREATE INDEX idx_escalations_due_date ON escalations(due_date);

-- triage_sessions
CREATE INDEX idx_triage_sessions_date ON triage_sessions(session_date);
CREATE INDEX idx_triage_sessions_status ON triage_sessions(status);

-- triage_preparations
CREATE INDEX idx_triage_preparations_session ON triage_preparations(session_id);
CREATE INDEX idx_triage_preparations_vuln ON triage_preparations(vulnerability_id);
CREATE INDEX idx_triage_preparations_status ON triage_preparations(prep_status);
CREATE INDEX idx_triage_preparations_priority ON triage_preparations(priority_flag);

-- triage_decisions
CREATE INDEX idx_triage_decisions_session ON triage_decisions(session_id);
CREATE INDEX idx_triage_decisions_vuln ON triage_decisions(vulnerability_id);
CREATE INDEX idx_triage_decisions_decision ON triage_decisions(decision);

-- triage_bulk_plans
CREATE INDEX idx_triage_bulk_plans_session ON triage_bulk_plans(session_id);
CREATE INDEX idx_triage_bulk_plans_status ON triage_bulk_plans(status);

-- triage_bulk_operations
CREATE INDEX idx_triage_bulk_operations_session ON triage_bulk_operations(session_id);
CREATE INDEX idx_triage_bulk_operations_plan ON triage_bulk_operations(bulk_plan_id);

-- remediations
CREATE INDEX idx_remediations_vuln ON remediations(vulnerability_id);
CREATE INDEX idx_remediations_status ON remediations(status);
CREATE INDEX idx_remediations_assigned ON remediations(assigned_to);
CREATE INDEX idx_remediations_target_date ON remediations(target_date);
CREATE INDEX idx_remediations_triage_decision ON remediations(triage_decision_id);

-- remediation_comments
CREATE INDEX idx_remediation_comments_remediation_id ON remediation_comments(remediation_id);
CREATE INDEX idx_remediation_comments_created_at ON remediation_comments(created_at);

-- system_settings
CREATE INDEX idx_system_settings_category ON system_settings(category);
CREATE INDEX idx_system_settings_key ON system_settings(setting_key);

-- ============================================================================
-- Triggers
-- ============================================================================

-- updated_at triggers (only on tables with updated_at column)
CREATE TRIGGER update_scans_updated_at
    BEFORE UPDATE ON scans
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_workloads_updated_at
    BEFORE UPDATE ON workloads
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_containers_updated_at
    BEFORE UPDATE ON containers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_remediations_updated_at
    BEFORE UPDATE ON remediations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_system_settings_updated_at
    BEFORE UPDATE ON system_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_triage_sessions_updated_at
    BEFORE UPDATE ON triage_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_notification_settings_updated_at
    BEFORE UPDATE ON notification_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- vulnerability last_seen trigger
CREATE TRIGGER trigger_update_vulnerability_last_seen
    AFTER INSERT ON vulnerability_instances
    FOR EACH ROW EXECUTE FUNCTION update_vulnerability_last_seen();

-- ============================================================================
-- Seed Data
-- ============================================================================

INSERT INTO system_settings (category, setting_key, setting_value, description, default_value)
VALUES
    ('notifications', 'ms_teams_webhook_url', '""', 'Microsoft Teams webhook URL for notifications', '""'),
    ('notifications', 'ms_teams_channel', '""', 'Microsoft Teams channel for notifications', '""'),
    ('remediation', 'auto_complete_enabled', 'true', 'Enable automatic completion of remediations when all workloads are resolved', 'true'),
    ('remediation', 'grace_period_days', '7', 'Days to wait after all workloads are resolved before auto-completing', '7'),
    ('retention', 'resolved_vulnerabilities_days', '90', 'Days to keep resolved vulnerability data before cleanup', '90'),
    ('retention', 'completed_remediations_days', '365', 'Days to keep completed remediation data before archiving', '365'),
    ('retention', 'scan_history_days', '180', 'Days to keep scan history', '180'),
    ('scanner', 'auto_resolve_enabled', 'true', 'Automatically resolve vulnerability instances when no longer detected', 'true'),
    ('scanner', 'workload_deduplication_enabled', 'true', 'Enable workload deduplication based on labels', 'true')
ON CONFLICT (category, setting_key) DO NOTHING;
