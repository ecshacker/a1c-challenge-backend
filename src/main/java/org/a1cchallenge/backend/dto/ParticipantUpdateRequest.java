package org.a1cchallenge.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ParticipantUpdateRequest {
    private LocalDate startDate;   // a Monday, sent from Day One
}
