-- Workload labels (separate table)
CREATE TABLE workload_labels (
    id BIGSERIAL PRIMARY KEY,
    workload_id BIGINT NOT NULL REFERENCES workloads(id) ON DELETE CASCADE,
    label_key VARCHAR(100) NOT NULL,
    label_value VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(workload_id, label_key)
);
CREATE INDEX idx_workload_labels_workload ON workload_labels(workload_id);
CREATE INDEX idx_workload_labels_key ON workload_labels(label_key);

-- Container new columns
ALTER TABLE containers ADD COLUMN detected_ecosystems JSONB;
ALTER TABLE containers ADD COLUMN os_family VARCHAR(50);
ALTER TABLE containers ADD COLUMN os_name VARCHAR(50);
ALTER TABLE containers ADD COLUMN repo_digests JSONB;

-- Vulnerability new columns
ALTER TABLE vulnerabilities ADD COLUMN package_class VARCHAR(20);
ALTER TABLE vulnerabilities ADD COLUMN package_type VARCHAR(50);
ALTER TABLE vulnerabilities ADD COLUMN "references" JSONB;
ALTER TABLE vulnerabilities ADD COLUMN cvss_v3_vector VARCHAR(200);
ALTER TABLE vulnerabilities ADD COLUMN cvss_v3_score DOUBLE PRECISION;

CREATE INDEX idx_vulnerabilities_package_class ON vulnerabilities(package_class);
CREATE INDEX idx_vulnerabilities_cvss_score ON vulnerabilities(cvss_v3_score);
