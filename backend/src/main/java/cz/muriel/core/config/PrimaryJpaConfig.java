package cz.muriel.core.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 🔧 PRIMARY JPA Configuration for main application database
 * 
 * Handles User Directory, Tenants, and all core application entities. This is
 * marked as @Primary to be the default EntityManagerFactory.
 */
@Configuration @EnableTransactionManagement @EnableJpaRepositories(basePackages = "cz.muriel.core.repository", excludeFilters = @org.springframework.context.annotation.ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.REGEX, pattern = "cz\\.muriel\\.core\\.repository\\.keycloak\\..*"), entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "transactionManager")
public class PrimaryJpaConfig {

  @Primary @Bean(name = "dataSourceProperties") @ConfigurationProperties("spring.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Primary @Bean(name = "dataSource")
  public DataSource dataSource(@Qualifier("dataSourceProperties") DataSourceProperties properties) {
    return properties.initializeDataSourceBuilder().build();
  }

  @Primary @Bean(name = "entityManagerFactory")
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      EntityManagerFactoryBuilder builder, @Qualifier("dataSource") DataSource dataSource) {

    Map<String, Object> properties = new HashMap<>();
    properties.put("hibernate.hbm2ddl.auto", "update");
    properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
    properties.put("hibernate.show_sql", false);
    properties.put("hibernate.format_sql", true);

    return builder.dataSource(dataSource).packages("cz.muriel.core.entity")
        .persistenceUnit("default").properties(properties).build();
  }

  @Primary @Bean(name = "transactionManager")
  public PlatformTransactionManager transactionManager(
      @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {
    return new JpaTransactionManager(entityManagerFactory);
  }
}
