package org.a1cchallenge.backend.exception;

/**
 * Thrown when a request presents a token that does not exist. Surfaced as a
 * 401 with a TOKEN_LOST payload, per the "SchrÃ¶dinger's Token" UX reality.
 */
public class TokenLostException extends RuntimeException {
    public TokenLostException(String message) {
        super(message);
    }
}
