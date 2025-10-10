package cz.muriel.core.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests using Testcontainers.
 * 
 * Provides:
 * - PostgreSQL container for main datasource
 * - Automatic Spring Boot context loading
 * - Test profile activation
 * - Dynamic property configuration
 * 
 * Usage:
 * <pre>
 * {@code
 * @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
 * class MyIntegrationTest extends AbstractIntegrationTest {
 *     // tests
 * }
 * }
 * </pre>
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    protected static final PostgreSQLContainer<?> postgresContainer = 
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true); // Reuse container across tests for speed

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Main datasource
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        
        // JPA/Hibernate configuration for tests - use validate with Flyway
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        
        // Keycloak datasource disabled in tests (using @ConditionalOnProperty)
        registry.add("keycloak.datasource.enabled", () -> "false");
        
        // Rate limiting disabled in tests
        registry.add("app.rate-limit.enabled", () -> "false");
        
        // Flyway configuration
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");
    }
}
