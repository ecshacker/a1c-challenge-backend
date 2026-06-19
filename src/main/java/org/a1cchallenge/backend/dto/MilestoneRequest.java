package com.a1cchallenge.dto;

import com.a1cchallenge.validation.ValidParticipantToken;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * All computed fields (deltaA1c, adherenceTier, cecdComposite...) are
 * intentionally absent - they are computed exclusively server-side (4.6).
 */
@Getter
@Setter
public class MilestoneRequest {

    @ValidParticipantToken
    @NotBlank
    private String token;

    @NotNull
    @Min(value = 4, message = "Milestones can only be submitted at week 4 or later")
    private Integer studyWeek;

    @NotBlank
    @Pattern(regexp = "^(four_week|eight_week|continuation)$", message = "Invalid milestone type")
    private String milestoneType;

    @NotNull(message = "Milestone A1C is required")
    @DecimalMin(value = "3.0", message = "A1C must be >= 3.0%")
    @DecimalMax(value = "20.0", message = "A1C must be <= 20.0%")
    private BigDecimal milestoneA1c;

    @NotBlank
    @Pattern(regexp = "^(home_kit|lab|clinic_pharmacy)$")
    private String milestoneA1cTestType;

    @DecimalMin("50.0") @DecimalMax("1000.0")
    private BigDecimal milestoneFructosamine;

    @Pattern(regexp = "^(home_kit|lab|clinic)$")
    private String milestoneFructosamineTestType;

    @Min(1) @Max(5) private Integer wbEnergy;
    @Min(1) @Max(5) private Integer wbMood;
    @Min(1) @Max(5) private Integer wbDigestion;
    @Min(1) @Max(5) private Integer wbSleep;
    @Min(1) @Max(5) private Integer wbHydration;
    @Min(1) @Max(5) private Integer wbPain;

    @NotBlank
    @Pattern(regexp = "^(nearly_every_day|most_days_80to99|more_than_half|struggled_under50)$")
    private String selfReportedAdherence;

    @NotBlank
    @Pattern(regexp = "^(yes_reduced|yes_stopped|no_changes|discussed_with_doctor)$")
    private String medicationChangeOverall;

    @NotBlank
    @Pattern(regexp = "^(continue_weekly|pause_for_8week|done)$")
    private String whatNext;

    @Size(max = 1000)
    private String freeTextNote;
}
