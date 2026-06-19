package com.a1cchallenge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Participant baseline record. The token is the primary key and the only
 * identifier in the system. No name, email, IP, or device id is ever stored.
 */
@Entity
@Table(name = "participants")
@Getter
@Setter
public class ParticipantEntity {

    @Id
    @Column(name = "token", length = 14)
    private String token;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "enrolled_at_week", nullable = false)
    private Integer enrolledAtWeek;

    @Column(name = "enrolled_at_year", nullable = false)
    private Integer enrolledAtYear;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion = 1;

    // --- Demographics ---
    @Column(name = "age_range", nullable = false, length = 8)
    private String ageRange;
    @Column(name = "biological_sex", nullable = false, length = 20)
    private String biologicalSex;
    @Column(name = "ethnicity", length = 40)
    private String ethnicity;
    @Column(name = "tribal_nation", length = 120)
    private String tribalNation;
    @Column(name = "country_region", nullable = false, length = 60)
    private String countryRegion;

    // --- Glycemic ---
    @Column(name = "diabetes_type", nullable = false, length = 20)
    private String diabetesType;
    @Column(name = "baseline_a1c", nullable = false, precision = 4, scale = 1)
    private BigDecimal baselineA1c;
    @Column(name = "baseline_a1c_test_type", nullable = false, length = 20)
    private String baselineA1cTestType;
    @Column(name = "baseline_a1c_month", nullable = false)
    private Integer baselineA1cMonth;
    @Column(name = "baseline_a1c_year", nullable = false)
    private Integer baselineA1cYear;
    @Column(name = "baseline_fructosamine", precision = 6, scale = 1)
    private BigDecimal baselineFructosamine;
    @Column(name = "baseline_fructosamine_test_type", length = 20)
    private String baselineFructosamineTestType;
    @Column(name = "glucose_monitoring_type", nullable = false, length = 20)
    private String glucoseMonitoringType;
    @Column(name = "cgm_device", length = 60)
    private String cgmDevice;

    // --- Care & Meds ---
    @Column(name = "care_approach", nullable = false, length = 30)
    private String careApproach;
    @Column(name = "med_metformin") private Boolean medMetformin;
    @Column(name = "med_glp1") private Boolean medGlp1;
    @Column(name = "med_sglt2") private Boolean medSglt2;
    @Column(name = "med_insulin") private Boolean medInsulin;
    @Column(name = "med_sulfonylurea") private Boolean medSulfonylurea;
    @Column(name = "med_none") private Boolean medNone;

    // --- Diet & Exercise ---
    @Column(name = "hemp_intended_daily_g", precision = 5, scale = 1)
    private BigDecimal hempIntendedDailyG;
    @Column(name = "carb_intake_band", length = 12)
    private String carbIntakeBand;
    @Column(name = "exercise_days_per_week", length = 6)
    private String exerciseDaysPerWeek;
    @Column(name = "exercise_type_aerobic") private Boolean exerciseTypeAerobic;
    @Column(name = "exercise_type_resistance") private Boolean exerciseTypeResistance;
    @Column(name = "exercise_type_walking") private Boolean exerciseTypeWalking;
    @Column(name = "exercise_type_yoga") private Boolean exerciseTypeYoga;
    @Column(name = "exercise_type_mixed") private Boolean exerciseTypeMixed;
    @Column(name = "dietary_pattern", length = 30)
    private String dietaryPattern;

    // --- Anthropometrics ---
    @Column(name = "weight_value", precision = 6, scale = 1) private BigDecimal weightValue;
    @Column(name = "weight_unit", length = 3) private String weightUnit;
    @Column(name = "height_value", precision = 5, scale = 1) private BigDecimal heightValue;
    @Column(name = "height_unit", length = 2) private String heightUnit;
    @Column(name = "waist_circumference_value", precision = 5, scale = 1) private BigDecimal waistCircumferenceValue;
    @Column(name = "waist_circumference_unit", length = 2) private String waistCircumferenceUnit;

    // --- Conditions ---
    @Column(name = "cond_nafld") private Boolean condNafld;
    @Column(name = "cond_pcos") private Boolean condPcos;
    @Column(name = "cond_hypertension") private Boolean condHypertension;
    @Column(name = "cond_hypothyroid") private Boolean condHypothyroid;
    @Column(name = "cond_ibd_crohns") private Boolean condIbdCrohns;
    @Column(name = "cond_ibs") private Boolean condIbs;
    @Column(name = "cond_fibromyalgia") private Boolean condFibromyalgia;
    @Column(name = "cond_anxiety_depression") private Boolean condAnxietyDepression;
    @Column(name = "cond_sleep_disorder") private Boolean condSleepDisorder;
    @Column(name = "cond_dyslipidemia") private Boolean condDyslipidemia;
    @Column(name = "cond_chronic_pain") private Boolean condChronicPain;
    @Column(name = "cond_pancreatitis_history") private Boolean condPancreatitisHistory;
    @Column(name = "cond_none") private Boolean condNone;

    // --- Baseline Wellbeing ---
    @Column(name = "wb_baseline_energy") private Integer wbBaselineEnergy;
    @Column(name = "wb_baseline_mood") private Integer wbBaselineMood;
    @Column(name = "wb_baseline_digestion") private Integer wbBaselineDigestion;
    @Column(name = "wb_baseline_sleep") private Integer wbBaselineSleep;
    @Column(name = "wb_baseline_hydration") private Integer wbBaselineHydration;
    @Column(name = "wb_baseline_pain") private Integer wbBaselinePain;
}
