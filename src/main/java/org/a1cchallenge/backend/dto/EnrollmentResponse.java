package com.a1cchallenge.dto;

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
