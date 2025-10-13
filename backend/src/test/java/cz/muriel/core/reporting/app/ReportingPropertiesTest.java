package cz.muriel.core.reporting.app;

import cz.muriel.core.test.MockTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ReportingProperties configuration.
 */
@SpringBootTest
@Import(MockTestConfig.class)
@ActiveProfiles("test")
@TestPropertySource(properties = { "reporting.enabled=true",
    "reporting.max-rows=50000", "reporting.max-interval-days=92",
    "reporting.default-ttl-seconds=60", "reporting.cache.provider=redis",
    "reporting.cache.key-prefix=rpt:", "reporting.rate-limit.per-tenant-per-min=120",
    "reporting.cube.base-url=http://cube:4000", "reporting.cube.api-token=test-token",
    "reporting.bulk.chunk-size=1000", "reporting.bulk.max-affect-rows=500000",
    "reporting.bulk.queue-concurrency=2" })
class ReportingPropertiesTest {

  @Autowired
  private ReportingProperties properties;

  @Test
  void shouldLoadConfiguration() {
    assertThat(properties).isNotNull();
    assertThat(properties.isEnabled()).isTrue();
    assertThat(properties.getMaxRows()).isEqualTo(50000);
    assertThat(properties.getMaxIntervalDays()).isEqualTo(92);
    assertThat(properties.getDefaultTtlSeconds()).isEqualTo(60);
  }

  @Test
  void shouldLoadCacheConfiguration() {
    assertThat(properties.getCache()).isNotNull();
    assertThat(properties.getCache().getProvider()).isEqualTo("redis");
    assertThat(properties.getCache().getKeyPrefix()).isEqualTo("rpt:");
  }

  @Test
  void shouldLoadRateLimitConfiguration() {
    assertThat(properties.getRateLimit()).isNotNull();
    assertThat(properties.getRateLimit().getPerTenantPerMin()).isEqualTo(120);
  }

  @Test
  void shouldLoadCubeConfiguration() {
    assertThat(properties.getCube()).isNotNull();
    assertThat(properties.getCube().getBaseUrl()).isEqualTo("http://cube:4000");
    assertThat(properties.getCube().getApiToken()).isEqualTo("test-token");
    assertThat(properties.getCube().getConnectTimeoutMs()).isEqualTo(5000);
    assertThat(properties.getCube().getReadTimeoutMs()).isEqualTo(30000);
  }

  @Test
  void shouldLoadBulkConfiguration() {
    assertThat(properties.getBulk()).isNotNull();
    assertThat(properties.getBulk().getChunkSize()).isEqualTo(1000);
    assertThat(properties.getBulk().getMaxAffectRows()).isEqualTo(500000);
    assertThat(properties.getBulk().getQueueConcurrency()).isEqualTo(2);
    assertThat(properties.getBulk().getTimeoutSeconds()).isEqualTo(300);
  }
}
