package cz.muriel.core.phase2;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 🧪 Phase 2 Integration Tests
 * 
 * Tests for WebSocket, Workflow, Documents and Search functionality
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class Phase2IntegrationTest {

    @LocalServerPort
    private int port;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    @Container
    static GenericContainer<?> minio = new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
        .withCommand("server /data")
        .withEnv("MINIO_ROOT_USER", "minioadmin")
        .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
        .withExposedPorts(9000);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Postgres
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        
        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
        
        // MinIO
        registry.add("minio.endpoint", () -> 
            String.format("http://%s:%d", minio.getHost(), minio.getFirstMappedPort())
        );
    }

    @BeforeAll
    static void setUp() {
        // Wait for containers to be ready
        assertTrue(postgres.isRunning());
        assertTrue(redis.isRunning());
        assertTrue(minio.isRunning());
    }

    @Test
    void contextLoads() {
        // Basic smoke test
        assertTrue(port > 0);
    }

    @Test
    void testDatabaseMigrations() {
        // TODO: Verify V5 migration ran successfully
        // Check entity_state, state_transition, document tables exist
    }

    @Test
    void testRedisConnection() {
        // TODO: Test Redis connectivity
        // Test presence key creation
    }

    @Test
    void testMinIOConnection() {
        // TODO: Test MinIO connectivity
        // Test bucket creation
    }

    // TODO: Add more tests:
    // - Workflow state transitions
    // - Document upload/download
    // - Fulltext search
    // - WebSocket presence tracking
}
