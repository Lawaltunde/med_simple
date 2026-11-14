package com.medsimple.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
// Respects spring.flyway.enabled=false so tests or environments without DB can disable migrations.
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywayConfig {

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String[] locations;

    // Spring Boot 4.x removed Flyway auto-configuration.
    // migrate() is called eagerly inside the bean method so it completes
    // before any other bean that depends on DataSource (including JPA) can run.
    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .load();
        flyway.migrate();
        return flyway;
    }

    // Nothing else depends on the flyway bean, so without this the JPA EntityManagerFactory can be
    // built first (a repository injected into an earlier component pulls it in). On an already
    // migrated database that goes unnoticed; on a fresh one Hibernate meets an empty schema and
    // ddl-auto=validate fails with "missing table". This makes JPA wait for the migration.
    @Bean
    static EntityManagerFactoryDependsOnPostProcessor entityManagerFactoryDependsOnFlyway() {
        return new EntityManagerFactoryDependsOnPostProcessor("flyway");
    }
}
