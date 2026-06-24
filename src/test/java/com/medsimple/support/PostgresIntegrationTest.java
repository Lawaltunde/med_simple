package com.medsimple.support;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Base for compliance integration tests that need a REAL PostgreSQL instance with the production
 * Flyway migrations applied.
 *
 * <p>Why real Postgres and not the H2 {@code test} profile: the guarantees under test are enforced
 * at the database layer (the {@code audit_logs} immutability trigger in V2, and ciphertext written
 * to a real column). H2 has no triggers from those PostgreSQL-specific migrations and the {@code
 * test} profile disables Flyway entirely, so those tables don't even exist there. Testcontainers
 * gives each run a throwaway Postgres with the exact migrations production uses.
 *
 * <p><b>Docker is required.</b> When no Docker daemon is reachable, these tests are SKIPPED (JUnit
 * assumption), never failed — so the gate lane stays green on machines without Docker and only
 * runs the real assertions where a daemon exists (CI / the integration host). The container is
 * lazily created on first use and shared across subclasses for the JVM, so the image is pulled and
 * migrated once.
 */
@SpringBootTest
@ActiveProfiles("pgtest")
@Testcontainers
public abstract class PostgresIntegrationTest {

    private static PostgreSQLContainer<?> postgres;

    private static synchronized PostgreSQLContainer<?> container() {
        if (postgres == null) {
            @SuppressWarnings("resource")
            PostgreSQLContainer<?> c = new PostgreSQLContainer<>("postgres:16-alpine");
            c.start();
            postgres = c;
        }
        return postgres;
    }

    /**
     * Skip (not fail) the whole class when Docker is unavailable. Spring resolves the
     * {@link DynamicPropertySource} before any test method, so the assumption is checked here in a
     * {@code @BeforeAll} to short-circuit context startup cleanly.
     */
    @BeforeAll
    static void requireDocker() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker not available — skipping PostgreSQL integration tests");
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        // Only touch Docker if it's actually there; otherwise the assumption in @BeforeAll skips.
        if (!DockerClientFactory.instance().isDockerAvailable()) {
            return;
        }
        PostgreSQLContainer<?> c = container();
        registry.add("spring.datasource.url", c::getJdbcUrl);
        registry.add("spring.datasource.username", c::getUsername);
        registry.add("spring.datasource.password", c::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        // Flyway owns the schema; Hibernate only validates it. This proves the entities and the
        // migrated tables agree, the same contract production runs under.
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
}
