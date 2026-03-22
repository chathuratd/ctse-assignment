package com.ctse.userservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * UserServiceApplicationTests — Smoke Test
 * ==========================================
 * @SpringBootTest loads the full Spring application context.
 * If this test passes, it means:
 *   - All beans are correctly configured
 *   - No circular dependencies
 *   - No missing @Bean definitions
 *   - Configuration properties are resolved
 *
 * @ActiveProfiles is NOT needed here because placing application.yml
 * in src/test/resources automatically overrides the main config.
 * The test application.yml points to H2 (no Supabase needed).
 *
 * HOW TO RUN:
 *   mvn test                    → runs all tests
 *   mvn test -Dtest=UserServiceApplicationTests → runs just this test
 */
@SpringBootTest
class UserServiceApplicationTests {

    /**
     * Context loads test — verifies the entire Spring context starts without errors.
     * An empty test body is intentional; the test passes if context loads successfully.
     */
    @Test
    void contextLoads() {
        // If this runs without exception, the application context is healthy
    }
}
