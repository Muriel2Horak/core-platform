package cz.muriel.core.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 🔧 PRIMARY JPA Configuration for main application database
 * 
 * Handles User Directory, Tenants, and all core application entities. This is
 * marked as @Primary to be the default EntityManagerFactory.
 */
@Configuration @EnableTransactionManagement @EnableJpaRepositories(basePackages = {
    "cz.muriel.core.repository", "cz.muriel.core.locks", "cz.muriel.core.reporting.repo",
    "cz.muriel.core.monitoring.grafana.repository" }, excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.REGEX, pattern = "cz\\.muriel\\.core\\.repository\\.keycloak\\..*"), entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "transactionManager")
public class PrimaryJpaConfig {

  @Value("${spring.datasource.url}")
  private String jdbcUrl;

  @Value("${spring.datasource.username}")
  private String username;

  @Value("${spring.datasource.password}")
  private String password;

  @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
  private String driverClassName;

  @Primary @Bean(name = "dataSource")
  public DataSource dataSource() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(jdbcUrl);
    config.setUsername(username);
    config.setPassword(password);
    config.setDriverClassName(driverClassName);
    config.setMaximumPoolSize(10);
    config.setMinimumIdle(2);
    config.setConnectionTimeout(10000);
    config.setIdleTimeout(300000);
    config.setMaxLifetime(600000);
    config.setPoolName("CoreDB-Pool");
    return new HikariDataSource(config);
  }

  @Primary @Bean(name = "entityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      @Qualifier("dataSource") DataSource dataSource) {

    Map<String, Object> properties = new HashMap<>();
    properties.put("hibernate.hbm2ddl.auto", "update");
    properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    properties.put("hibernate.show_sql", false);
    properties.put("hibernate.format_sql", true);

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setDataSource(dataSource);
    factory.setJpaVendorAdapter(vendorAdapter);
    factory.setPackagesToScan("cz.muriel.core.entity", "cz.muriel.core.locks",
        "cz.muriel.core.reporting.model", "cz.muriel.core.monitoring.grafana.entity");
    factory.setPersistenceUnitName("default");
    factory.setJpaPropertyMap(properties);
    return factory;
  }

  @Primary @Bean(name = "transactionManager")
  public PlatformTransactionManager transactionManager(
      @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory);
  }
}
