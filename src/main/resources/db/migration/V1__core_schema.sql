-- =============================================================================
-- V1: Core schema for The A1C Challenge
-- Source: A1C_Challenge_Data_Schema_v2.docx (Sections 3-7)
-- Anonymous-by-design: no PII, no IP, no wall-clock timestamps tied to a token.
-- Hard-rejection rules enforced here; "flag but do not reject" rules are left
-- to the Spring Boot service layer (see CheckInValidationService).
-- =============================================================================

-- =============================================================================
-- TABLE: participants
-- =============================================================================
CREATE TABLE participants (
    token VARCHAR(14) PRIMARY KEY,
    start_date DATE,   -- null = enrolled, not started
    enrolled_at_week SMALLINT NOT NULL CHECK (enrolled_at_week BETWEEN 1 AND 53),
    enrolled_at_year SMALLINT NOT NULL CHECK (enrolled_at_year BETWEEN 2024 AND 2030),
    schema_version SMALLINT NOT NULL DEFAULT 1,

    -- Demographics
    age_range VARCHAR(8) NOT NULL CHECK (age_range IN ('18-29', '30-44', '45-59', '60-74', '75plus')),
    biological_sex VARCHAR(20) NOT NULL CHECK (biological_sex IN ('female', 'male', 'intersex', 'prefer_not_to_say')),
    ethnicity VARCHAR(40) CHECK (ethnicity IN ('american_indian_alaska_native', 'asian', 'black_african_american', 'hispanic_latino', 'middle_eastern_north_african', 'native_hawaiian_pacific_islander', 'white_european', 'multiracial', 'other', 'prefer_not_to_say')),
    tribal_nation VARCHAR(120),
    country_region VARCHAR(60) NOT NULL,

    -- Diabetes & Baseline Glycemic
    diabetes_type VARCHAR(20) NOT NULL CHECK (diabetes_type IN ('type1', 'type2', 'prediabetes', 'unknown')),
    baseline_a1c DECIMAL(4,1) NOT NULL CHECK (baseline_a1c >= 5.7 AND baseline_a1c <= 20.0),
    baseline_a1c_test_type VARCHAR(20) NOT NULL CHECK (baseline_a1c_test_type IN ('lab', 'home_kit', 'clinic_pharmacy')),
    baseline_a1c_month SMALLINT NOT NULL CHECK (baseline_a1c_month BETWEEN 1 AND 12),
    baseline_a1c_year SMALLINT NOT NULL CHECK (baseline_a1c_year BETWEEN 2024 AND 2030),
    baseline_fructosamine DECIMAL(6,1),
    baseline_fructosamine_test_type VARCHAR(20) CHECK (baseline_fructosamine_test_type IN ('home_kit', 'lab', 'clinic')),
    glucose_monitoring_type VARCHAR(20) NOT NULL CHECK (glucose_monitoring_type IN ('none', 'fingerstick', 'cgm')),
    cgm_device VARCHAR(60),

    -- Care & Medications
    care_approach VARCHAR(30) NOT NULL CHECK (care_approach IN ('adding_to_standard_care', 'replacing_standard_care', 'no_current_standard_care')),
    med_metformin BOOLEAN,
    med_glp1 BOOLEAN,
    med_sglt2 BOOLEAN,
    med_insulin BOOLEAN,
    med_sulfonylurea BOOLEAN,
    med_none BOOLEAN,

    -- Diet & Exercise Baseline
    hemp_intended_daily_g DECIMAL(5,1),
    carb_intake_band VARCHAR(12) CHECK (carb_intake_band IN ('under50', '50to100', '100to150', '150to200', 'over200')),
    exercise_days_per_week VARCHAR(6) CHECK (exercise_days_per_week IN ('zero', '1to2', '3to4', '5plus')),
    exercise_type_aerobic BOOLEAN,
    exercise_type_resistance BOOLEAN,
    exercise_type_walking BOOLEAN,
    exercise_type_yoga BOOLEAN,
    exercise_type_mixed BOOLEAN,
    dietary_pattern VARCHAR(30) CHECK (dietary_pattern IN ('omnivore', 'vegetarian', 'vegan', 'keto', 'mediterranean', 'other', 'prefer_not_to_say')),

    -- Anthropometrics
    weight_value DECIMAL(6,1),
    weight_unit VARCHAR(3) CHECK (weight_unit IN ('lbs', 'kg')),
    height_value DECIMAL(5,1),
    height_unit VARCHAR(2) CHECK (height_unit IN ('in', 'cm')),
    waist_circumference_value DECIMAL(5,1),
    waist_circumference_unit VARCHAR(2) CHECK (waist_circumference_unit IN ('in', 'cm')),

    -- Underlying Conditions
    cond_nafld BOOLEAN,
    cond_pcos BOOLEAN,
    cond_hypertension BOOLEAN,
    cond_hypothyroid BOOLEAN,
    cond_ibd_crohns BOOLEAN,
    cond_ibs BOOLEAN,
    cond_fibromyalgia BOOLEAN,
    cond_anxiety_depression BOOLEAN,
    cond_sleep_disorder BOOLEAN,
    cond_dyslipidemia BOOLEAN,
    cond_chronic_pain BOOLEAN,
    cond_pancreatitis_history BOOLEAN,
    cond_none BOOLEAN,

    -- Baseline Wellbeing
    wb_baseline_energy SMALLINT CHECK (wb_baseline_energy BETWEEN 1 AND 5),
    wb_baseline_mood SMALLINT CHECK (wb_baseline_mood BETWEEN 1 AND 5),
    wb_baseline_digestion SMALLINT CHECK (wb_baseline_digestion BETWEEN 1 AND 5),
    wb_baseline_sleep SMALLINT CHECK (wb_baseline_sleep BETWEEN 1 AND 5),
    wb_baseline_hydration SMALLINT CHECK (wb_baseline_hydration BETWEEN 1 AND 5),
    wb_baseline_pain SMALLINT CHECK (wb_baseline_pain BETWEEN 1 AND 5),

    -- Complex CHECK constraints (Section 7.1)
    CONSTRAINT chk_token_format CHECK (token ~ '^[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$'),
    CONSTRAINT chk_start_date_monday CHECK (start_date IS NULL OR EXTRACT(DOW FROM start_date) = 1),
    CONSTRAINT chk_tribal_nation_req CHECK (ethnicity = 'american_indian_alaska_native' OR tribal_nation IS NULL),
    CONSTRAINT chk_cgm_device_req CHECK (glucose_monitoring_type = 'cgm' OR cgm_device IS NULL),
    CONSTRAINT chk_med_none_exclusive CHECK (NOT (med_none = TRUE AND (med_metformin = TRUE OR med_glp1 = TRUE OR med_sglt2 = TRUE OR med_insulin = TRUE OR med_sulfonylurea = TRUE))),
    CONSTRAINT chk_cond_none_exclusive CHECK (NOT (cond_none = TRUE AND (cond_nafld = TRUE OR cond_pcos = TRUE OR cond_hypertension = TRUE OR cond_hypothyroid = TRUE OR cond_ibd_crohns = TRUE OR cond_ibs = TRUE OR cond_fibromyalgia = TRUE OR cond_anxiety_depression = TRUE OR cond_sleep_disorder = TRUE OR cond_dyslipidemia = TRUE OR cond_chronic_pain = TRUE OR cond_pancreatitis_history = TRUE))),
    CONSTRAINT chk_height_weight_pairing CHECK ((height_value IS NULL AND weight_value IS NULL) OR (height_value IS NOT NULL AND weight_value IS NOT NULL)),
    CONSTRAINT chk_height_weight_units_req CHECK ((height_value IS NULL OR height_unit IS NOT NULL) AND (weight_value IS NULL OR weight_unit IS NOT NULL)),
    CONSTRAINT chk_waist_pairing CHECK ((waist_circumference_value IS NULL AND waist_circumference_unit IS NULL) OR (waist_circumference_value IS NOT NULL AND waist_circumference_unit IS NOT NULL)),
    CONSTRAINT chk_fructosamine_test_type_req CHECK (baseline_fructosamine IS NULL OR baseline_fructosamine_test_type IS NOT NULL)
);

-- =============================================================================
-- TABLE: checkins
-- =============================================================================
CREATE TABLE checkins (
    checkin_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(14) NOT NULL REFERENCES participants(token),
    study_week SMALLINT NOT NULL CHECK (study_week >= 1),
    submitted_at_day_offset SMALLINT NOT NULL CHECK (submitted_at_day_offset BETWEEN 1 AND 14),

    -- Hemp Adherence
    hemp_day_mon BOOLEAN,
    hemp_day_tue BOOLEAN,
    hemp_day_wed BOOLEAN,
    hemp_day_thu BOOLEAN,
    hemp_day_fri BOOLEAN,
    hemp_day_sat BOOLEAN,
    hemp_day_sun BOOLEAN,
    hemp_amount_g DECIMAL(5,1) CHECK (hemp_amount_g >= 0),

    -- Cannabis Adherence
    cannabis_day_mon BOOLEAN,
    cannabis_day_tue BOOLEAN,
    cannabis_day_wed BOOLEAN,
    cannabis_day_thu BOOLEAN,
    cannabis_day_fri BOOLEAN,
    cannabis_day_sat BOOLEAN,
    cannabis_day_sun BOOLEAN,
    cannabis_method VARCHAR(20) CHECK (cannabis_method IN ('juice_smoothie', 'eaten_directly', 'cold_infusion', 'mixed', 'none_this_week')),
    cannabis_amount_g DECIMAL(5,1) CHECK (cannabis_amount_g >= 0),
    cannabis_strain_type VARCHAR(12) CHECK (cannabis_strain_type IN ('not_selected', 'sativa', 'indica', 'balanced')),
    cannabis_thca_cbda_known BOOLEAN,
    cannabis_profile_notes VARCHAR(200),

    -- Fasting Glucose
    glucose_unit VARCHAR(6) CHECK (glucose_unit IN ('mgdl', 'mmoll')),
    glucose_mon DECIMAL(5,1),
    glucose_tue DECIMAL(5,1),
    glucose_wed DECIMAL(5,1),
    glucose_thu DECIMAL(5,1),
    glucose_fri DECIMAL(5,1),
    glucose_sat DECIMAL(5,1),
    glucose_sun DECIMAL(5,1),

    -- CGM Metrics
    cgm_tir_pct DECIMAL(5,2) CHECK (cgm_tir_pct BETWEEN 0.0 AND 100.0),
    cgm_tar_pct DECIMAL(5,2) CHECK (cgm_tar_pct BETWEEN 0.0 AND 100.0),
    cgm_tbr_pct DECIMAL(5,2) CHECK (cgm_tbr_pct BETWEEN 0.0 AND 100.0),
    cgm_cv_pct DECIMAL(5,2) CHECK (cgm_cv_pct BETWEEN 0.0 AND 100.0),

    -- Exercise & Wellbeing
    exercise_days VARCHAR(6) CHECK (exercise_days IN ('zero', '1to2', '3to4', '5plus')),
    exercise_type_this_week VARCHAR(20) CHECK (exercise_type_this_week IN ('aerobic', 'resistance', 'walking', 'mixed', 'none')),
    wb_energy SMALLINT CHECK (wb_energy BETWEEN 1 AND 5),
    wb_mood SMALLINT CHECK (wb_mood BETWEEN 1 AND 5),
    wb_digestion SMALLINT CHECK (wb_digestion BETWEEN 1 AND 5),
    wb_sleep SMALLINT CHECK (wb_sleep BETWEEN 1 AND 5),
    wb_hydration SMALLINT CHECK (wb_hydration BETWEEN 1 AND 5),
    wb_pain SMALLINT CHECK (wb_pain BETWEEN 1 AND 5),

    -- Clinical & Admin
    weight_value DECIMAL(6,1),
    weight_unit VARCHAR(3) CHECK (weight_unit IN ('lbs', 'kg')),
    medication_change VARCHAR(30) CHECK (medication_change IN ('no_changes', 'dose_reduced', 'medication_stopped', 'new_med_added')),
    standard_care_contact VARCHAR(30) CHECK (standard_care_contact IN ('no', 'yes_scheduled_visit', 'yes_lab_a1c', 'yes_other')),
    free_text_note VARCHAR(500),

    -- Computed Compliance (Section 3.9) - populated server-side, never by client
    hemp_days_count SMALLINT NOT NULL CHECK (hemp_days_count BETWEEN 0 AND 7),
    cannabis_days_count SMALLINT NOT NULL CHECK (cannabis_days_count BETWEEN 0 AND 7),
    combined_compliance_score DECIMAL(4,3) NOT NULL CHECK (combined_compliance_score BETWEEN 0.000 AND 1.000),
    week_compliant BOOLEAN NOT NULL,

    -- Complex CHECK constraints (Section 7.2)
    CONSTRAINT chk_checkin_unique_token_week UNIQUE (token, study_week),
    CONSTRAINT chk_glucose_unit_req CHECK (
        glucose_unit IS NOT NULL OR
        (glucose_mon IS NULL AND glucose_tue IS NULL AND glucose_wed IS NULL AND glucose_thu IS NULL
         AND glucose_fri IS NULL AND glucose_sat IS NULL AND glucose_sun IS NULL)
    )
);

-- =============================================================================
-- TABLE: milestones
-- =============================================================================
CREATE TABLE milestones (
    milestone_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(14) NOT NULL REFERENCES participants(token),
    study_week SMALLINT NOT NULL CHECK (study_week >= 4),
    milestone_type VARCHAR(20) NOT NULL CHECK (milestone_type IN ('four_week', 'eight_week', 'continuation')),

    -- Primary Glycemic Outcome
    milestone_a1c DECIMAL(4,1) NOT NULL CHECK (milestone_a1c BETWEEN 3.0 AND 20.0),
    milestone_a1c_test_type VARCHAR(20) NOT NULL CHECK (milestone_a1c_test_type IN ('home_kit', 'lab', 'clinic_pharmacy')),
    milestone_fructosamine DECIMAL(6,1),
    milestone_fructosamine_test_type VARCHAR(20) CHECK (milestone_fructosamine_test_type IN ('home_kit', 'lab', 'clinic')),

    -- Wellbeing Scores
    wb_energy SMALLINT CHECK (wb_energy BETWEEN 1 AND 5),
    wb_mood SMALLINT CHECK (wb_mood BETWEEN 1 AND 5),
    wb_digestion SMALLINT CHECK (wb_digestion BETWEEN 1 AND 5),
    wb_sleep SMALLINT CHECK (wb_sleep BETWEEN 1 AND 5),
    wb_hydration SMALLINT CHECK (wb_hydration BETWEEN 1 AND 5),
    wb_pain SMALLINT CHECK (wb_pain BETWEEN 1 AND 5),

    -- Self-reported Adherence & Meds
    self_reported_adherence VARCHAR(20) NOT NULL CHECK (self_reported_adherence IN ('nearly_every_day', 'most_days_80to99', 'more_than_half', 'struggled_under50')),
    medication_change_overall VARCHAR(30) NOT NULL CHECK (medication_change_overall IN ('yes_reduced', 'yes_stopped', 'no_changes', 'discussed_with_doctor')),

    -- Continuation Intent
    what_next VARCHAR(30) NOT NULL CHECK (what_next IN ('continue_weekly', 'pause_for_8week', 'done')),
    free_text_note VARCHAR(1000),

    -- Computed Outcome Fields (Section 4.6) - populated server-side
    delta_a1c DECIMAL(4,1) NOT NULL,
    delta_a1c_clinically_meaningful BOOLEAN NOT NULL,
    delta_fructosamine DECIMAL(6,1),
    checkins_submitted SMALLINT NOT NULL CHECK (checkins_submitted >= 0),
    checkins_compliant SMALLINT NOT NULL CHECK (checkins_compliant >= 0),
    adherence_tier VARCHAR(20) NOT NULL CHECK (adherence_tier IN ('full_cohort', 'partial', 'per_protocol', 'high_adherence')),
    cecd_composite_baseline DECIMAL(4,2) CHECK (cecd_composite_baseline BETWEEN 1.00 AND 5.00),
    cecd_composite_milestone DECIMAL(4,2) CHECK (cecd_composite_milestone BETWEEN 1.00 AND 5.00),
    delta_cecd_composite DECIMAL(4,2),

    CONSTRAINT chk_milestone_unique_token_week UNIQUE (token, study_week)
);

-- =============================================================================
-- TABLE: draft_checkins
-- =============================================================================
CREATE TABLE draft_checkins (
    draft_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(14) NOT NULL REFERENCES participants(token) ON DELETE CASCADE,
    study_week SMALLINT NOT NULL,
    last_saved_offset SMALLINT NOT NULL CHECK (last_saved_offset BETWEEN 1 AND 7),
    draft_data JSONB NOT NULL,

    CONSTRAINT chk_draft_unique_token_week UNIQUE (token, study_week)
);

-- =============================================================================
-- TABLE: audit_log
-- =============================================================================
CREATE TABLE audit_log (
    log_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(30) NOT NULL CHECK (event_type IN ('enrollment', 'start_set', 'checkin_submit', 'draft_save', 'milestone_submit', 'token_lookup')),
    token_hash VARCHAR(64) NOT NULL,
    study_week SMALLINT,
    event_week_offset SMALLINT NOT NULL CHECK (event_week_offset BETWEEN 1 AND 7),
    event_year_week VARCHAR(8) NOT NULL CHECK (event_year_week ~ '^[0-9]{4}-[0-9]{2}$'),
    anomaly_flag BOOLEAN NOT NULL DEFAULT FALSE
);

-- =============================================================================
-- Indexes
-- =============================================================================
CREATE INDEX idx_participants_token ON participants(token);

CREATE INDEX idx_checkins_token ON checkins(token);
CREATE INDEX idx_checkins_token_week ON checkins(token, study_week);

CREATE INDEX idx_milestones_token ON milestones(token);
CREATE INDEX idx_milestones_token_week ON milestones(token, study_week);

CREATE INDEX idx_draft_checkins_token_week ON draft_checkins(token, study_week);

CREATE INDEX idx_audit_token_hash ON audit_log(token_hash);
CREATE INDEX idx_audit_event_type ON audit_log(event_type);
CREATE INDEX idx_audit_year_week ON audit_log(event_year_week);

-- =============================================================================
-- Cross-table validation trigger: CGM metrics only from CGM users (Section 7.2.4)
-- =============================================================================
CREATE OR REPLACE FUNCTION check_cgm_fields_rule()
RETURNS TRIGGER AS $$
BEGIN
    IF (NEW.cgm_tir_pct IS NOT NULL OR NEW.cgm_tar_pct IS NOT NULL OR NEW.cgm_tbr_pct IS NOT NULL OR NEW.cgm_cv_pct IS NOT NULL) THEN
        IF NOT EXISTS (
            SELECT 1 FROM participants
            WHERE token = NEW.token AND glucose_monitoring_type = 'cgm'
        ) THEN
            RAISE EXCEPTION 'CGM metrics can only be submitted by participants with glucose_monitoring_type = ''cgm''';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_check_cgm_fields
BEFORE INSERT OR UPDATE ON checkins
FOR EACH ROW
EXECUTE FUNCTION check_cgm_fields_rule();
