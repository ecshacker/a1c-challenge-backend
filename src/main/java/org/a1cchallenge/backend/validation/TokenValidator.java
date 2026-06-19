package com.a1cchallenge.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TokenValidator implements ConstraintValidator<ValidParticipantToken, String> {

    private static final String TOKEN_REGEX = "^[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}-[A-HJ-NP-Z2-9]{4}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // let @NotNull / @NotBlank handle nulls
        return value.matches(TOKEN_REGEX);
    }
}
