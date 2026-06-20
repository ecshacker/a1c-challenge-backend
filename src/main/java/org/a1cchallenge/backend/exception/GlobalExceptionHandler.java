package org.a1cchallenge.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized error translation. Never leaks stack traces or request bodies
 * (which could carry a token) to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = (error instanceof FieldError fe) ? fe.getField() : error.getObjectName();
            errors.put(field, error.getDefaultMessage());
        });
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("details", errors);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("status", 409, "error", ex.getMessage())); // 409
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleArg(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("status", 400, "error", ex.getMessage())); // 400
    }

    @ExceptionHandler(StudyNotOpenException.class)
    public ResponseEntity<Map<String, Object>> handleStudyNotOpen(StudyNotOpenException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", 503, "error", "Service Unavailable", "message", ex.getMessage())); // 503
    }

    @ExceptionHandler(TokenLostException.class)
    public ResponseEntity<Map<String, Object>> handleTokenLost(TokenLostException ex) {
        // The "SchrÃ¶dinger's Token" reality: an unknown token is a 401, not a 500.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "TOKEN_LOST", "message", ex.getMessage())); // 401
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        // Generic catch-all: never echo the message of an unexpected error.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", 500, "error", "An unexpected error occurred."));
    }
}
