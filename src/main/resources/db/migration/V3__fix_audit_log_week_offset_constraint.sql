-- Fix audit_log.event_week_offset constraint: 1-10 → 1-7
-- Field stores day of week (1=Mon...7=Sun), not a submission window offset.
ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS audit_log_event_week_offset_check;
ALTER TABLE audit_log ADD CONSTRAINT audit_log_event_week_offset_check
    CHECK (event_week_offset BETWEEN 1 AND 7);
