-- Cleanup: collapse duplicate SBOM documents down to the latest per image.
--
-- Why: before the content-hash fix, every scan re-stored an identical SBOM because the
-- hash covered Trivy's per-run serialNumber/timestamp. This accumulates tens of thousands
-- of duplicate sbom_documents and millions of sbom_packages rows per image. This script
-- keeps only the newest document per image_reference and deletes the rest. sbom_packages
-- rows are removed automatically via ON DELETE CASCADE.
--
-- SAFETY: as shipped, this file ROLLS BACK. Running it changes nothing -- it is a rehearsal
-- that prints the before/after counts. When the numbers look right, change the final
-- ROLLBACK to COMMIT and run it again to apply.
--
-- Recommended: suspend the scanner first so nothing inserts mid-cleanup --
--   kubectl patch cronjob ephor-scanner -n ephor -p '{"spec":{"suspend":true}}'
-- and resume it afterwards with suspend:false.
--
-- Run:
--   kubectl exec -i -n ephor ephor-postgresql-0 -- \
--     sh -c 'PGPASSWORD=$POSTGRES_PASSWORD psql -U ephor -d ephor' < scripts/cleanup-sbom-bloat.sql

\echo '=== Pre-flight (read-only) ==='
SELECT 'documents'                  AS metric, count(*)                          AS value FROM sbom_documents
UNION ALL
SELECT 'packages',                          count(*)                                   FROM sbom_packages
UNION ALL
SELECT 'distinct images',                   count(DISTINCT image_reference)            FROM sbom_documents
UNION ALL
SELECT 'documents to keep',                 count(DISTINCT image_reference)            FROM sbom_documents
UNION ALL
SELECT 'documents to delete',               count(*) - count(DISTINCT image_reference) FROM sbom_documents;

BEGIN;

-- Keep the newest document per image; tie-break deterministically.
WITH keep AS (
    SELECT DISTINCT ON (image_reference) id
    FROM sbom_documents
    ORDER BY image_reference, last_seen DESC, first_seen DESC, id DESC
)
DELETE FROM sbom_documents d
WHERE NOT EXISTS (SELECT 1 FROM keep k WHERE k.id = d.id);

\echo '=== After (pending, inside transaction) ==='
SELECT 'documents' AS metric, count(*) AS value FROM sbom_documents
UNION ALL
SELECT 'packages',         count(*)          FROM sbom_packages;

-- Rehearsal by default. Change ROLLBACK -> COMMIT to apply.
ROLLBACK;

-- After COMMIT, reclaim disk. Run these separately (they cannot run in a transaction and
-- take a brief exclusive lock -- keep the scanner suspended until they finish):
--   VACUUM (FULL, ANALYZE) sbom_packages;
--   VACUUM (FULL, ANALYZE) sbom_documents;
