package com.medsimple.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base for compliance integration tests that need the REAL, Flyway-migrated PostgreSQL schema —
 * the {@code audit_logs} immutability trigger (V2) and ciphertext written to a real column are
 * enforced at the database layer, not in JPA, so the H2 {@code test} profile (Flyway off, no
 * triggers) can't exercise them.
 *
 * <p>Runs against the docker-compose dev Postgres (localhost:5333) via the {@code pgcompliance}
 * profile. Chosen over Testcontainers because the Testcontainers Docker-API probe returns HTTP 400
 * in this environment, which makes those tests SKIP — and a compliance check that skips verifies
 * nothing. This runs the assertions for real wherever {@code docker-compose up -d} is up.
 */
@SpringBootTest
@ActiveProfiles("pgcompliance")
public abstract class PostgresIntegrationTest {
}
