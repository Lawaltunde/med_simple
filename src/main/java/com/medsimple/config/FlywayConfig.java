package com.medsimple.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String[] locations;

    // Spring Boot 4.x removed Flyway auto-configuration.
    // initMethod = "migrate" ensures migrations run before the JPA
    // EntityManagerFactory validates the schema.
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .load();
    }
}
