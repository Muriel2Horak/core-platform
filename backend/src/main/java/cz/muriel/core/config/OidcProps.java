package cz.muriel.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OidcProps {
  @Value("${OIDC_ISSUER:http://keycloak:8080/realms/core-platform}")
  private String issuer;

  @Value("${OIDC_CLIENT_ID:web}")
  private String clientId;

  @Value("${KEYCLOAK_BASE_URL:http://keycloak:8080}")
  private String keycloakBaseUrl;

  @Value("${KC_ADMIN_USER:admin}")
  private String adminUser;

  @Value("${KC_ADMIN_PASSWORD:admin}")
  private String adminPassword;

  public String getIssuer() {
    return issuer;
  }

  public String getClientId() {
    return clientId;
  }

  public String getKeycloakBaseUrl() {
    return keycloakBaseUrl;
  }

  public String getAdminUser() {
    return adminUser;
  }

  public String getAdminPassword() {
    return adminPassword;
  }

  public String getRealm() {
    int i = issuer.indexOf("/realms/");
    return i > 0 ? issuer.substring(i + "/realms/".length()) : "master";
  }

  public String getTokenEndpoint() {
    return issuer + "/protocol/openid-connect/token";
  }

  public String getUserinfoEndpoint() {
    return issuer + "/protocol/openid-connect/userinfo";
  }

  public String getLogoutEndpoint() {
    return issuer + "/protocol/openid-connect/logout";
  }

  public String getAdminTokenEndpoint() {
    return keycloakBaseUrl + "/realms/master/protocol/openid-connect/token";
  }

  public String getAdminUsersEndpoint() {
    return keycloakBaseUrl + "/admin/realms/" + getRealm() + "/users";
  }
}