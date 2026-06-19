package com.a1cchallenge.dto;

import lombok.Getter;

import java.math.BigDecimal;

/** Personal dashboard metrics returned to the client (retention hook). */
@Getter
public class MilestoneResponse {
    private final String message;
    private final Integer studyWeek;
    private final BigDecimal deltaA1c;
    private final Boolean deltaA1cClinicallyMeaningful;
    private final String adherenceTier;

    public MilestoneResponse(String message, Integer studyWeek, BigDecimal deltaA1c,
                             Boolean deltaA1cClinicallyMeaningful, String adherenceTier) {
        this.message = message;
        this.studyWeek = studyWeek;
        this.deltaA1c = deltaA1c;
        this.deltaA1cClinicallyMeaningful = deltaA1cClinicallyMeaningful;
        this.adherenceTier = adherenceTier;
    }
}
