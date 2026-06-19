package com.a1cchallenge.dto;

import com.a1cchallenge.validation.ValidEnrollment;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@ValidEnrollment // triggers cross-field validation (Section 7.1)
@Getter
@Setter
public class EnrollmentRequest {

    @NotBlank
    @Pattern(regexp = "^(18-29|30-44|45-59|60-74|75plus)$", message = "Invalid age range")
    private String ageRange;

    @NotBlank
    @Pattern(regexp = "^(female|male|intersex|prefer_not_to_say)$", message = "Invalid biological sex")
    private String biologicalSex;

    @Pattern(regexp = "^(american_indian_alaska_native|asian|black_african_american|hispanic_latino|middle_eastern_north_african|native_hawaiian_pacific_islander|white_european|multiracial|other|prefer_not_to_say)$")
    private String ethnicity;

    @Size(max = 120)
    private String tribalNation;

    @NotBlank
    private String countryRegion;

    @NotBlank
    @Pattern(regexp = "^(type1|type2|prediabetes|unknown)$")
    private String diabetesType;

    @NotNull
    @DecimalMin(value = "5.7", message = "Baseline A1C must be >= 5.7%")
    @DecimalMax(value = "20.0", message = "Baseline A1C must be <= 20.0%")
    private BigDecimal baselineA1c;

    @NotBlank
    @Pattern(regexp = "^(lab|home_kit|clinic_pharmacy)$")
    private String baselineA1cTestType;

    @NotNull
    @Min(value = 1, message = "Month must be 1-12")
    @Max(value = 12, message = "Month must be 1-12")
    private Integer baselineA1cMonth;

    @NotNull
    @Min(value = 2024, message = "Year must be 2024-2030")
    @Max(value = 2030, message = "Year must be 2024-2030")
    private Integer baselineA1cYear;

    @DecimalMin(value = "0.0")
    private BigDecimal baselineFructosamine;

    @Pattern(regexp = "^(home_kit|lab|clinic)$")
    private String baselineFructosamineTestType;

    @NotBlank
    @Pattern(regexp = "^(none|fingerstick|cgm)$")
    private String glucoseMonitoringType;

    @Size(max = 60)
    private String cgmDevice;

    @NotBlank
    @Pattern(regexp = "^(adding_to_standard_care|replacing_standard_care|no_current_standard_care)$")
    private String careApproach;

    @NotNull private Boolean medMetformin;
    @NotNull private Boolean medGlp1;
    @NotNull private Boolean medSglt2;
    @NotNull private Boolean medInsulin;
    @NotNull private Boolean medSulfonylurea;
    @NotNull private Boolean medNone;

    @DecimalMin(value = "0.0")
    private BigDecimal hempIntendedDailyG;

    @Pattern(regexp = "^(under50|50to100|100to150|150to200|over200)$")
    private String carbIntakeBand;

    @Pattern(regexp = "^(zero|1to2|3to4|5plus)$")
    private String exerciseDaysPerWeek;

    private Boolean exerciseTypeAerobic;
    private Boolean exerciseTypeResistance;
    private Boolean exerciseTypeWalking;
    private Boolean exerciseTypeYoga;
    private Boolean exerciseTypeMixed;

    @Pattern(regexp = "^(omnivore|vegetarian|vegan|keto|mediterranean|other|prefer_not_to_say)$")
    private String dietaryPattern;

    private BigDecimal weightValue;
    @Pattern(regexp = "^(lbs|kg)$")
    private String weightUnit;

    private BigDecimal heightValue;
    @Pattern(regexp = "^(in|cm)$")
    private String heightUnit;

    private BigDecimal waistCircumferenceValue;
    @Pattern(regexp = "^(in|cm)$")
    private String waistCircumferenceUnit;

    @NotNull private Boolean condNafld;
    @NotNull private Boolean condPcos;
    @NotNull private Boolean condHypertension;
    @NotNull private Boolean condHypothyroid;
    @NotNull private Boolean condIbdCrohns;
    @NotNull private Boolean condIbs;
    @NotNull private Boolean condFibromyalgia;
    @NotNull private Boolean condAnxietyDepression;
    @NotNull private Boolean condSleepDisorder;
    @NotNull private Boolean condDyslipidemia;
    @NotNull private Boolean condChronicPain;
    @NotNull private Boolean condPancreatitisHistory;
    @NotNull private Boolean condNone;

    @Min(1) @Max(5) private Integer wbBaselineEnergy;
    @Min(1) @Max(5) private Integer wbBaselineMood;
    @Min(1) @Max(5) private Integer wbBaselineDigestion;
    @Min(1) @Max(5) private Integer wbBaselineSleep;
    @Min(1) @Max(5) private Integer wbBaselineHydration;
    @Min(1) @Max(5) private Integer wbBaselinePain;
}
