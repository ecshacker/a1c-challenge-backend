package org.a1cchallenge.backend.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Complete editable baseline set held by the week-1 tab (not a sparse patch):
 * for nullable fields, null means cleared. A1C is required and never nulled.
 */
@Getter
@Setter
public class BaselineUpdateRequest {

    @NotNull
    @DecimalMin(value = "5.7", message = "Baseline A1C must be at least 5.7.")
    @DecimalMax(value = "20.0", message = "Baseline A1C looks out of range.")
    private BigDecimal baselineA1c;

    private String baselineA1cTestType;            // 'lab'|'home_kit'|'clinic_pharmacy'
    private BigDecimal baselineFructosamine;        // nullable -> clears
    private String baselineFructosamineTestType;    // 'home_kit'|'lab'|'clinic'
    private BigDecimal weightValue;
    private String weightUnit;                      // 'kg' | 'lbs'
    private BigDecimal heightValue;
    private String heightUnit;                      // 'cm' | 'in'
}
