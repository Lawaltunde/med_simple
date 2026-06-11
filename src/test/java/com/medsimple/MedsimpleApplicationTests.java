package com.medsimple;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// Loads the full application context against an in-memory H2 datasource.
// Flyway is disabled in the test profile — it only runs against real PostgreSQL.
// Integration tests that require real PostgreSQL should use Testcontainers.
@SpringBootTest
@ActiveProfiles("test")
class MedsimpleApplicationTests {

    @Test
    void contextLoads() {
    }

}
