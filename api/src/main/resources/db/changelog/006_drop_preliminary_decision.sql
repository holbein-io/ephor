-- Drop the unused preliminary_decision hint from triage preparations
ALTER TABLE triage_preparations DROP COLUMN IF EXISTS preliminary_decision;
