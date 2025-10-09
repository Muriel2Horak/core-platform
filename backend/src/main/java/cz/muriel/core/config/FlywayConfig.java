package cz.muriel.core.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

/**
 * 🔧 Flyway Configuration
 * 
 * Ensures Flyway uses the PRIMARY DataSource (core DB), not keycloakDataSource.
 * Without this, Flyway may incorrectly use keycloakDataSource when multiple
 * DataSources exist.
 */
@Configuration @Slf4j
public class FlywayConfig {

  /**
   * Force Flyway to use primary DataSource
   */
  @Bean
  public Flyway flyway(@Qualifier("dataSource") DataSource primaryDataSource) {
    log.info("🔧 Configuring Flyway with PRIMARY DataSource (core DB)");

    return Flyway.configure().dataSource(primaryDataSource).locations("classpath:db/migration")
        .baselineOnMigrate(true).baselineVersion("0") // Start from 0, so V1 will run
        .load();
  }

  /**
   * Migration strategy - run migrations on startup
   */
  @Bean
  public FlywayMigrationStrategy flywayMigrationStrategy() {
    return flyway -> {
      log.info("🚀 Running Flyway migrations on PRIMARY DataSource");
      flyway.migrate();
    };
  }
}
