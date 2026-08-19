-- Widen the columns Trivy can overflow (comma-joined fix lists, long advisory URLs),
-- and drop the last_seen trigger as redundant: ScanIngestionService writes last_seen in
-- the same transaction, and the trigger's second write path over those rows deadlocked.
ALTER TABLE vulnerabilities
    ALTER COLUMN fixed_version    TYPE TEXT,
    ALTER COLUMN primary_url      TYPE TEXT,
    ALTER COLUMN package_version  TYPE VARCHAR(255);

DROP TRIGGER IF EXISTS trigger_update_vulnerability_last_seen ON vulnerability_instances;
DROP FUNCTION IF EXISTS update_vulnerability_last_seen();
