package cz.muriel.core.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 🔧 JPA konfigurace pro Keycloak databázi
 * 
 * Vytváří samostatný EntityManager pro čtení z change_events tabulky v Keycloak
 * DB
 */
@Configuration @EnableTransactionManagement @EnableJpaRepositories(basePackages = "cz.muriel.core.repository.keycloak", entityManagerFactoryRef = "keycloakEntityManagerFactory", transactionManagerRef = "keycloakTransactionManager") @ConditionalOnProperty(name = "keycloak.datasource.enabled", havingValue = "true", matchIfMissing = false)
public class KeycloakJpaConfig {

  @Bean(name = "keycloakEntityManagerFactory")
  public LocalContainerEntityManagerFactoryBean keycloakEntityManagerFactory(
      @Qualifier("keycloakDataSource") DataSource keycloakDataSource) {

    Map<String, Object> properties = new HashMap<>();
    properties.put("hibernate.hbm2ddl.auto", "none"); // Neměň schéma!
    properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    properties.put("hibernate.show_sql", false);
    properties.put("hibernate.format_sql", false);

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(keycloakDataSource);
    factory.setJpaVendorAdapter(vendorAdapter);
    factory.setPackagesToScan("cz.muriel.core.entity");
    factory.setPersistenceUnitName("keycloak");
    factory.setJpaPropertyMap(properties);
    return factory;
  }

  @Bean(name = "keycloakTransactionManager")
  public PlatformTransactionManager keycloakTransactionManager(
      @Qualifier("keycloakEntityManagerFactory") EntityManagerFactory keycloakEntityManagerFactory) {
    return new JpaTransactionManager(keycloakEntityManagerFactory);
  }
}
