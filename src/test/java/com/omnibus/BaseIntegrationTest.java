package com.Omnibus;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton Testcontainers base class.
 * Starts ONE PostgreSQL 16 container per JVM — shared across all test classes.
 * Spring Boot test context caching ensures the container serves all tests.
 */
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("Omnibus_test")
                .withUsername("test")
                .withPassword("test");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // Use a shorter JWT secret for tests
        registry.add("app.jwt.secret",
                () -> "TestSecretKeyThatIsAtLeast256BitsLongForHS256Algorithm!!");
        registry.add("app.jwt.expiration-ms", () -> "900000");
        // Higher pool size for concurrency tests — main tx + REQUIRES_NEW audit tx
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "40");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "30000");
    }
}
