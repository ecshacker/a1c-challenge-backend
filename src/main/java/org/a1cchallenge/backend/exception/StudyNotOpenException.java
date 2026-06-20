package org.a1cchallenge.backend.exception;

/** Thrown when an enrollment attempt arrives while the study is not OPEN. */
public class StudyNotOpenException extends RuntimeException {
    public StudyNotOpenException(String message) {
        super(message);
    }
}
