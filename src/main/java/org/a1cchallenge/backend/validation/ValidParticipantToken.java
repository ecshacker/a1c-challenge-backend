package com.a1cchallenge.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TokenValidator.class)
@Documented
public @interface ValidParticipantToken {
    String message() default "Invalid token format. Must be XXXX-XXXX-XXXX using A-H, J-N, P-Z, 2-9.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
