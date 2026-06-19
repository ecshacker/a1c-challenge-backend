package com.a1cchallenge.dto;

import com.a1cchallenge.validation.ValidParticipantToken;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Draft endpoint payload. Accepts the entire (possibly incomplete) check-in as
 * a flexible JSON object; no strict field validation since it is transient state.
 */
@Getter
@Setter
public class DraftCheckInRequest {

    @ValidParticipantToken
    @NotBlank
    private String token;

    @NotNull
    @Min(value = 1)
    private Integer studyWeek;

    @NotNull
    @Min(value = 1) @Max(value = 7)
    private Integer lastSavedOffset;

    @NotNull
    private Map<String, Object> draftData;
}
