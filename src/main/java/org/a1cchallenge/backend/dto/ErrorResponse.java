package com.a1cchallenge.dto;

import lombok.Getter;

@Getter
public class ErrorResponse {
    private final String error;
    public ErrorResponse(String error) { this.error = error; }
}
