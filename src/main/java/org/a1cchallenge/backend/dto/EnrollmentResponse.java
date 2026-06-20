package org.a1cchallenge.backend.dto;

import lombok.Getter;

@Getter
public class EnrollmentResponse {
    private final String token;
    private final String message;

    public EnrollmentResponse(String token, String message) {
        this.token = token;
        this.message = message;
    }
}
