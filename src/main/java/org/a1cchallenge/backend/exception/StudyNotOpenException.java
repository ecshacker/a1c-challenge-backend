package com.a1cchallenge.exception;

/** Thrown when an enrollment attempt arrives while the study is not OPEN. */
public class StudyNotOpenException extends RuntimeException {
    public StudyNotOpenException(String message) {
        super(message);
    }
}
