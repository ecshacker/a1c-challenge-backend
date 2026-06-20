package org.a1cchallenge.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "checkins")
@Getter
@Setter
public class CheckInEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "checkin_id")
    private UUID checkinId;

    @Column(name = "token", nullable = false, length = 14)
    private String token;

    @Column(name = "study_week", nullable = false)
    private Integer studyWeek;

    @Column(name = "submitted_at_day_offset", nullable = false)
    private Integer submittedAtDayOffset;

    // --- Hemp & Cannabis Adherence ---
    @Column(name = "hemp_day_mon") private Boolean hempDayMon = false;
    @Column(name = "hemp_day_tue") private Boolean hempDayTue = false;
    @Column(name = "hemp_day_wed") private Boolean hempDayWed = false;
    @Column(name = "hemp_day_thu") private Boolean hempDayThu = false;
    @Column(name = "hemp_day_fri") private Boolean hempDayFri = false;
    @Column(name = "hemp_day_sat") private Boolean hempDaySat = false;
    @Column(name = "hemp_day_sun") private Boolean hempDaySun = false;
    @Column(name = "hemp_amount_g") private BigDecimal hempAmountG;

    @Column(name = "cannabis_day_mon") private Boolean cannabisDayMon = false;
    @Column(name = "cannabis_day_tue") private Boolean cannabisDayTue = false;
    @Column(name = "cannabis_day_wed") private Boolean cannabisDayWed = false;
    @Column(name = "cannabis_day_thu") private Boolean cannabisDayThu = false;
    @Column(name = "cannabis_day_fri") private Boolean cannabisDayFri = false;
    @Column(name = "cannabis_day_sat") private Boolean cannabisDaySat = false;
    @Column(name = "cannabis_day_sun") private Boolean cannabisDaySun = false;
    @Column(name = "cannabis_method", length = 20) private String cannabisMethod;
    @Column(name = "cannabis_amount_g") private BigDecimal cannabisAmountG;
    @Column(name = "cannabis_strain_type", length = 12) private String cannabisStrainType;
    @Column(name = "cannabis_thca_cbda_known") private Boolean cannabisThcaCbdaKnown;
    @Column(name = "cannabis_profile_notes", length = 200) private String cannabisProfileNotes;

    // --- Glucose ---
    @Column(name = "glucose_unit", length = 6) private String glucoseUnit;
    @Column(name = "glucose_mon", precision = 5, scale = 1) private BigDecimal glucoseMon;
    @Column(name = "glucose_tue", precision = 5, scale = 1) private BigDecimal glucoseTue;
    @Column(name = "glucose_wed", precision = 5, scale = 1) private BigDecimal glucoseWed;
    @Column(name = "glucose_thu", precision = 5, scale = 1) private BigDecimal glucoseThu;
    @Column(name = "glucose_fri", precision = 5, scale = 1) private BigDecimal glucoseFri;
    @Column(name = "glucose_sat", precision = 5, scale = 1) private BigDecimal glucoseSat;
    @Column(name = "glucose_sun", precision = 5, scale = 1) private BigDecimal glucoseSun;

    // --- CGM ---
    @Column(name = "cgm_tir_pct", precision = 5, scale = 2) private BigDecimal cgmTirPct;
    @Column(name = "cgm_tar_pct", precision = 5, scale = 2) private BigDecimal cgmTarPct;
    @Column(name = "cgm_tbr_pct", precision = 5, scale = 2) private BigDecimal cgmTbrPct;
    @Column(name = "cgm_cv_pct", precision = 5, scale = 2) private BigDecimal cgmCvPct;

    // --- Exercise & Wellbeing ---
    @Column(name = "exercise_days", length = 6) private String exerciseDays;
    @Column(name = "exercise_type_this_week", length = 20) private String exerciseTypeThisWeek;
    @Column(name = "wb_energy") private Integer wbEnergy;
    @Column(name = "wb_mood") private Integer wbMood;
    @Column(name = "wb_digestion") private Integer wbDigestion;
    @Column(name = "wb_sleep") private Integer wbSleep;
    @Column(name = "wb_hydration") private Integer wbHydration;
    @Column(name = "wb_pain") private Integer wbPain;

    // --- Clinical & Admin ---
    @Column(name = "weight_value", precision = 6, scale = 1) private BigDecimal weightValue;
    @Column(name = "weight_unit", length = 3) private String weightUnit;
    @Column(name = "medication_change", length = 30) private String medicationChange;
    @Column(name = "standard_care_contact", length = 30) private String standardCareContact;
    @Column(name = "free_text_note", length = 500) private String freeTextNote;

    // --- Computed Compliance (Section 3.9) ---
    @Column(name = "hemp_days_count", nullable = false) private Integer hempDaysCount;
    @Column(name = "cannabis_days_count", nullable = false) private Integer cannabisDaysCount;
    @Column(name = "combined_compliance_score", nullable = false, precision = 4, scale = 3) private BigDecimal combinedComplianceScore;
    @Column(name = "week_compliant", nullable = false) private Boolean weekCompliant;

    /** Explicit boolean getter used in MilestoneService stream filters. */
    public boolean isWeekCompliant() {
        return Boolean.TRUE.equals(weekCompliant);
    }
}
