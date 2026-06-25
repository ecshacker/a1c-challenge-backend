package org.a1cchallenge.backend.dto;

import org.a1cchallenge.backend.validation.ValidParticipantToken;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CheckInRequest {

    @ValidParticipantToken
    private String token; // populated from X-Participant-Token header by controller

    @NotNull
    @Min(value = 1, message = "Study week must be >= 1")
    private Integer studyWeek;

    @NotNull
    @Min(value = 1) @Max(value = 14)
    private Integer submittedAtDayOffset;

    private Boolean hempDayMon;
    private Boolean hempDayTue;
    private Boolean hempDayWed;
    private Boolean hempDayThu;
    private Boolean hempDayFri;
    private Boolean hempDaySat;
    private Boolean hempDaySun;
    private BigDecimal hempAmountG;

    private Boolean cannabisDayMon;
    private Boolean cannabisDayTue;
    private Boolean cannabisDayWed;
    private Boolean cannabisDayThu;
    private Boolean cannabisDayFri;
    private Boolean cannabisDaySat;
    private Boolean cannabisDaySun;

    @Pattern(regexp = "^(juice_smoothie|eaten_directly|cold_infusion|mixed|none_this_week)$")
    private String cannabisMethod;

    private BigDecimal cannabisAmountG;

    @Pattern(regexp = "^(not_selected|sativa|indica|balanced)$")
    private String cannabisStrainType;

    private Boolean cannabisThcaCbdaKnown;

    @Size(max = 200)
    private String cannabisProfileNotes;

    @Pattern(regexp = "^(mgdl|mmoll)$")
    private String glucoseUnit;

    // Glucose values are deliberately NOT range-annotated: Section 7.2.3 says
    // "flag but do not reject". Soft validation lives in CheckInValidationService.
    private BigDecimal glucoseMon;
    private BigDecimal glucoseTue;
    private BigDecimal glucoseWed;
    private BigDecimal glucoseThu;
    private BigDecimal glucoseFri;
    private BigDecimal glucoseSat;
    private BigDecimal glucoseSun;

    @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal cgmTirPct;
    @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal cgmTarPct;
    @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal cgmTbrPct;
    @DecimalMin("0.0") @DecimalMax("100.0")
    private BigDecimal cgmCvPct;

    @Pattern(regexp = "^(zero|1to2|3to4|5plus)$")
    private String exerciseDays;

    @Pattern(regexp = "^(aerobic|resistance|walking|mixed|none)$")
    private String exerciseTypeThisWeek;

    @Min(1) @Max(5) private Integer wbEnergy;
    @Min(1) @Max(5) private Integer wbMood;
    @Min(1) @Max(5) private Integer wbDigestion;
    @Min(1) @Max(5) private Integer wbSleep;
    @Min(1) @Max(5) private Integer wbHydration;
    @Min(1) @Max(5) private Integer wbPain;

    private BigDecimal weightValue;
    @Pattern(regexp = "^(lbs|kg)$")
    private String weightUnit;

    @Pattern(regexp = "^(no_changes|dose_reduced|medication_stopped|new_med_added)$")
    private String medicationChange;

    @Pattern(regexp = "^(no|yes_scheduled_visit|yes_lab_a1c|yes_other)$")
    private String standardCareContact;

    @Size(max = 500)
    private String freeTextNote;
}
