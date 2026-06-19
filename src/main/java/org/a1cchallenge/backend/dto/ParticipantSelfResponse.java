package com.a1cchallenge.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Minimal /me projection: baselines, start, computed status. No demographics,
 * meds, or conditions - a smaller echo is less surface if a token leaks.
 */
@Getter
@Setter
public class ParticipantSelfResponse {
    private LocalDate startDate;          // null until Day One
    private Integer studyWeek;            // null if not started / start in future
    private boolean baselineEditable;     // false once week 1 has closed

    private BigDecimal baselineA1c;
    private String baselineA1cTestType;
    private Integer baselineA1cMonth;
    private Integer baselineA1cYear;
    private BigDecimal baselineFructosamine;
    private String baselineFructosamineTestType;
    private BigDecimal weightValue;
    private String weightUnit;
    private BigDecimal heightValue;
    private String heightUnit;
}
