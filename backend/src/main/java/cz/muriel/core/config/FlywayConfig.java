package cz.muriel.core.config;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 🔧 Flyway Configuration
 * 
 * CRITICAL: Runs Flyway migrations BEFORE JPA/Hibernate initialization! -
 * Disables Spring Boot's FlywayAutoConfiguration (runs too late) - Manually
 * runs migrations in @PostConstruct (runs early) - Uses PRIMARY DataSource
 * (core DB), not keycloakDataSource
 */
@Configuration @Slf4j @Order(1) // Run before other configs
@EnableAutoConfiguration(exclude = FlywayAutoConfiguration.class)
public class FlywayConfig {

  private final DataSource primaryDataSource;

  public FlywayConfig(@Qualifier("dataSource") DataSource primaryDataSource) {
    this.primaryDataSource = primaryDataSource;
  }

  /**
   * Run Flyway migrations IMMEDIATELY after bean construction This ensures
   * migrations run BEFORE JPA entities are scanned
   */
  @PostConstruct
  public void migrate() {
    log.info("🔧 Configuring Flyway with PRIMARY DataSource (core DB)");
    log.info("🚀 Running Flyway migrations in @PostConstruct (BEFORE JPA)");

    Flyway flyway = Flyway.configure().dataSource(primaryDataSource)
        .locations("classpath:db/migration").cleanDisabled(false) // ENABLE clean for development
        .load();

    // CLEAN database first (removes ALL objects in the schema)
    log.warn("⚠️ Cleaning database (removes ALL tables, views, functions...)");
    flyway.clean();

    int migrationsRun = flyway.migrate().migrationsExecuted;

    log.info("✅ Flyway migrations completed: {} migrations executed", migrationsRun);
  }
}
