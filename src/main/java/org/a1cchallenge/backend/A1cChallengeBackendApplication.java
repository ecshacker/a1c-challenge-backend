package org.a1cchallenge.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The A1C Challenge backend.
 *
 * Anonymous-by-design data collection platform. No PII is collected, stored, or
 * inferable; the participant token (held only in the browser's localStorage) is
 * the sole key. See the build-trail privacy checklist for the "must-nots".
 */
@SpringBootApplication
@EnableScheduling
public class A1cChallengeBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(A1cChallengeBackendApplication.class, args);
    }

}
