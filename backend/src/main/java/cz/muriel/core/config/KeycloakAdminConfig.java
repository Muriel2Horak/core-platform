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

  @Value("${keycloak.admin.realm:master}")
  private String adminRealm;

  @Value("${keycloak.admin.client-id:backend-admin-service}")
  private String adminClientId;
  
  @Value("${keycloak.admin.client-secret:}")
  private String adminClientSecret;

  /**
   * 🔧 Keycloak Admin Client Bean
   * 
   * Používá client_credentials grant type pro service account
   * Confidential client s povoleným service account musí být nakonfigurován v Keycloak
   */
  @Bean
  public Keycloak keycloakAdminClient() {
    log.info("🔐 Initializing Keycloak Admin Client: server={}, realm={}, clientId={}",
        keycloakServerUrl, adminRealm, adminClientId);

    return KeycloakBuilder.builder()
        .serverUrl(keycloakServerUrl)
        .realm(adminRealm)
        .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
        .clientId(adminClientId)
        .clientSecret(adminClientSecret)
        .build();
  }
}
