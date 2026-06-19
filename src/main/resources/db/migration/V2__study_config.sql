-- =============================================================================
-- V2: Study governance - the "kill switch" / OSF gating single-row config table.
-- Source: build-trail Section 2.1 (Study Governance). System starts locked.
-- =============================================================================
CREATE TABLE study_config (
    id SMALLINT PRIMARY KEY CHECK (id = 1),
    status VARCHAR(30) NOT NULL CHECK (status IN ('PRE_LAUNCH', 'OPEN', 'PAUSED_INVESTIGATION', 'CONCLUDED')),
    launch_date TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100)
);

-- Initialize the system in a locked state until OSF registration is timestamped.
INSERT INTO study_config (id, status, updated_by)
VALUES (1, 'PRE_LAUNCH', 'system_initialization');
