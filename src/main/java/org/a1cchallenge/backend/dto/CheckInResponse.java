package com.a1cchallenge.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class CheckInResponse {
    private String message;
    private Integer studyWeek;
    private BigDecimal combinedComplianceScore;
    private Boolean weekCompliant;
    private List<String> warnings; // populated if soft rules are tripped (7.2)

    public CheckInResponse(String message, Integer studyWeek,
                           BigDecimal combinedComplianceScore, Boolean weekCompliant) {
        this.message = message;
        this.studyWeek = studyWeek;
        this.combinedComplianceScore = combinedComplianceScore;
        this.weekCompliant = weekCompliant;
    }
}
