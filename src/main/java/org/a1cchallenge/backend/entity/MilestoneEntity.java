package com.a1cchallenge.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "milestones")
@Getter
@Setter
public class MilestoneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "milestone_id")
    private UUID milestoneId;

    @Column(name = "token", nullable = false, length = 14)
    private String token;

    @Column(name = "study_week", nullable = false)
    private Integer studyWeek;

    @Column(name = "milestone_type", nullable = false, length = 20)
    private String milestoneType;

    // --- Glycemic ---
    @Column(name = "milestone_a1c", nullable = false, precision = 4, scale = 1)
    private BigDecimal milestoneA1c;
    @Column(name = "milestone_a1c_test_type", nullable = false, length = 20)
    private String milestoneA1cTestType;
    @Column(name = "milestone_fructosamine", precision = 6, scale = 1)
    private BigDecimal milestoneFructosamine;
    @Column(name = "milestone_fructosamine_test_type", length = 20)
    private String milestoneFructosamineTestType;

    // --- Wellbeing ---
    @Column(name = "wb_energy") private Integer wbEnergy;
    @Column(name = "wb_mood") private Integer wbMood;
    @Column(name = "wb_digestion") private Integer wbDigestion;
    @Column(name = "wb_sleep") private Integer wbSleep;
    @Column(name = "wb_hydration") private Integer wbHydration;
    @Column(name = "wb_pain") private Integer wbPain;

    // --- Adherence & Meds ---
    @Column(name = "self_reported_adherence", nullable = false, length = 20) private String selfReportedAdherence;
    @Column(name = "medication_change_overall", nullable = false, length = 30) private String medicationChangeOverall;
    @Column(name = "what_next", nullable = false, length = 30) private String whatNext;
    @Column(name = "free_text_note", length = 1000) private String freeTextNote;

    // --- Computed Outcome Fields (Section 4.6) ---
    @Column(name = "delta_a1c", nullable = false, precision = 4, scale = 1) private BigDecimal deltaA1c;
    @Column(name = "delta_a1c_clinically_meaningful", nullable = false) private Boolean deltaA1cClinicallyMeaningful;
    @Column(name = "delta_fructosamine", precision = 6, scale = 1) private BigDecimal deltaFructosamine;
    @Column(name = "checkins_submitted", nullable = false) private Integer checkinsSubmitted;
    @Column(name = "checkins_compliant", nullable = false) private Integer checkinsCompliant;
    @Column(name = "adherence_tier", nullable = false, length = 20) private String adherenceTier;
    @Column(name = "cecd_composite_baseline", precision = 4, scale = 2) private BigDecimal cecdCompositeBaseline;
    @Column(name = "cecd_composite_milestone", precision = 4, scale = 2) private BigDecimal cecdCompositeMilestone;
    @Column(name = "delta_cecd_composite", precision = 4, scale = 2) private BigDecimal deltaCecdComposite;
}
