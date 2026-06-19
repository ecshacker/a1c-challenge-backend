package org.a1cchallenge.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class A1cChallengeBackendApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the full Spring context (entities, repositories, services,
        // controllers, validators, scheduled job) wires together correctly.
    }

}
