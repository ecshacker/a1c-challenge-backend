package org.a1cchallenge.backend.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.StringJoiner;

/**
 * Cryptographically secure token generation. 12 characters in 3 hyphen-separated
 * groups, drawn from an alphabet that excludes visually ambiguous O, 0, I, 1.
 */
@Service
public class TokenGeneratorService {

    private static final String ALLOWED_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int GROUP_LENGTH = 4;
    private static final int NUM_GROUPS = 3;

    public String generateToken() {
        StringJoiner joiner = new StringJoiner("-");
        for (int i = 0; i < NUM_GROUPS; i++) {
            StringBuilder group = new StringBuilder(GROUP_LENGTH);
            for (int j = 0; j < GROUP_LENGTH; j++) {
                group.append(ALLOWED_CHARS.charAt(SECURE_RANDOM.nextInt(ALLOWED_CHARS.length())));
            }
            joiner.add(group.toString());
        }
        return joiner.toString();
    }
}
