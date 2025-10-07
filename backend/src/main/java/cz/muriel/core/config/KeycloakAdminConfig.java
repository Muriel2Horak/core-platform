package cz.muriel.core.config;

import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 🔐 Keycloak Admin Client Configuration
 * 
 * Vytváří Keycloak Admin Client pro správu uživatelů a rolí Používá service
 * account credentials pro admin operace
 */
@Slf4j @Configuration
public class KeycloakAdminConfig {

  @Value("${keycloak.auth-server-url:http://keycloak:8080}")
  private String keycloakServerUrl;

  @Value("${keycloak.admin.username:admin}")
  private String adminUsername;

  @Value("${keycloak.admin.password:admin}")
  private String adminPassword;

  @Value("${keycloak.admin.realm:master}")
  private String adminRealm;

  @Value("${keycloak.admin.client-id:admin-cli}")
  private String adminClientId;

  /**
   * 🔧 Keycloak Admin Client Bean
   * 
   * Používá password grant type pro admin účet Master realm je použit pro
   * cross-realm operace
   */
  @Bean
  public Keycloak keycloakAdminClient() {
    log.info("🔐 Initializing Keycloak Admin Client: server={}, realm={}, user={}",
        keycloakServerUrl, adminRealm, adminUsername);

    return KeycloakBuilder.builder().serverUrl(keycloakServerUrl).realm(adminRealm)
        .grantType(OAuth2Constants.PASSWORD).clientId(adminClientId).username(adminUsername)
        .password(adminPassword).build();
  }
}
