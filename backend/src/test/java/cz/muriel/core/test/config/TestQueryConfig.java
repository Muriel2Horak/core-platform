package cz.muriel.core.test.config;

import cz.muriel.core.reporting.service.QueryDeduplicator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Test configuration providing stub QueryDeduplicator for tests.
 */
@TestConfiguration
@Profile("test")
public class TestQueryConfig {

  @Bean
  @Primary
  public QueryDeduplicator testQueryDeduplicator() {
    // Stub implementation for tests - no deduplication logic
    return new QueryDeduplicator() {
      @Override
      public Map<String, Object> executeWithDeduplication(Map<String, Object> query, String tenantId, 
          Supplier<Map<String, Object>> executor) {
        return executor.get();
      }
    };
  }
}
