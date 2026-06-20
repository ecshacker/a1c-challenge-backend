package org.a1cchallenge.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnrollmentCrossFieldValidator.class)
@Documented
public @interface ValidEnrollment {
    String message() default "Enrollment data violates cross-field constraints";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
