package cz.muriel.core.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class KeycloakClient {
  private final RestTemplate rt = new RestTemplate();
  private final ObjectMapper om;

  private final String issuer;
  private final String clientId;
  private final String preferredScopes;

  public KeycloakClient(ObjectMapper om,
      @Value("${OIDC_ISSUER:http://keycloak:8080/realms/admin}") String issuer,
      @Value("${keycloak.client-id:web}") String clientId,
      @Value("${keycloak.token-scopes:openid profile email}") String preferredScopes) {
    this.om = om;
    this.issuer = issuer.endsWith("/") ? issuer.substring(0, issuer.length() - 1) : issuer;
    this.clientId = clientId;
    this.preferredScopes = preferredScopes;
  }

  public JsonNode tokenByPassword(String username, String password) {
    String url = issuer + "/protocol/openid-connect/token";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    // Nejprve zkus preferované scopy, při invalid_scope fallback na "openid"
    try {
      return requestToken(url, headers, username, password, preferredScopes);
    } catch (HttpStatusCodeException ex) {
      if (ex.getStatusCode().value() == 400
          && safeBody(ex).toLowerCase().contains("invalid_scope")) {
        try {
          return requestToken(url, headers, username, password, "openid");
        } catch (HttpStatusCodeException ex2) {
          throw kcException("token", ex2);
        }
      }
      throw kcException("token", ex);
    }
  }

  private JsonNode requestToken(String url, HttpHeaders headers, String username, String password,
      String scope) throws HttpStatusCodeException {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "password");
    form.add("client_id", clientId);
    form.add("username", username);
    form.add("password", password);
    if (scope != null && !scope.isBlank()) {
      form.add("scope", scope.trim());
    }
    ResponseEntity<String> res = rt.postForEntity(url, new HttpEntity<>(form, headers),
        String.class);
    return readJson(res.getBody());
  }

  public JsonNode userinfo(String accessToken) {
    String url = issuer + "/protocol/openid-connect/userinfo";
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);
    try {
      ResponseEntity<String> res = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
          String.class);
      return readJson(res.getBody());
    } catch (HttpStatusCodeException ex) {
      throw kcException("userinfo", ex);
    }
  }

  public void logout(String refreshToken) {
    String url = issuer + "/protocol/openid-connect/logout";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", clientId);
    form.add("refresh_token", refreshToken);

    try {
      rt.postForEntity(url, new HttpEntity<>(form, headers), String.class);
    } catch (HttpStatusCodeException ex) {
      throw kcException("logout", ex);
    }
  }

  public void changePassword(String userId, String newPassword) {
    String adminToken = getAdminToken();
    String url = issuer.replace("/realms/", "/admin/realms/") + "/users/" + userId
        + "/reset-password";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> payload = Map.of("type", "password", "temporary", false, "value",
        newPassword);

    try {
      rt.exchange(url, HttpMethod.PUT, new HttpEntity<>(payload, headers), Void.class);
    } catch (HttpStatusCodeException ex) {
      throw kcException("changePassword", ex);
    }
  }

  public void updateUserProfile(String userId, String firstName, String lastName, String email) {
    String adminToken = getAdminToken();
    String url = issuer.replace("/realms/", "/admin/realms/") + "/users/" + userId;

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);
    headers.setContentType(MediaType.APPLICATION_JSON);

    Map<String, Object> payload = Map.of("firstName", firstName != null ? firstName : "",
        "lastName", lastName != null ? lastName : "", "email", email != null ? email : "");

    try {
      rt.exchange(url, HttpMethod.PUT, new HttpEntity<>(payload, headers), Void.class);
    } catch (HttpStatusCodeException ex) {
      throw kcException("updateUserProfile", ex);
    }
  }

  public JsonNode getUserProfile(String userId) {
    String adminToken = getAdminToken();
    String url = issuer.replace("/realms/", "/admin/realms/") + "/users/" + userId;

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(adminToken);

    try {
      ResponseEntity<String> res = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(headers),
          String.class);
      return readJson(res.getBody());
    } catch (HttpStatusCodeException ex) {
      throw kcException("getUserProfile", ex);
    }
  }

  private String getAdminToken() {
    // Implementujte získání admin tokenu pro Keycloak
    // Toto je zjednodušená verze, v produkci použijte bezpečnější způsob
    String url = issuer.substring(0, issuer.indexOf("/realms/"))
        + "/realms/master/protocol/openid-connect/token";
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", "admin-cli");
    // V reálné aplikaci byste měli použít client secret nebo jinou formu
    // autentizace klienta
    // Pro tento příklad předpokládáme, že admin-cli klient nemá secret

    try {
      ResponseEntity<String> res = rt.postForEntity(url, new HttpEntity<>(form, headers),
          String.class);
      JsonNode tokenNode = readJson(res.getBody());
      return tokenNode.path("access_token").asText();
    } catch (HttpStatusCodeException ex) {
      throw kcException("getAdminToken", ex);
    }
  }

  private JsonNode readJson(String body) {
    try {
      return om.readTree(body);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse JSON", e);
    }
  }

  private RuntimeException kcException(String op, HttpStatusCodeException ex) {
    String msg = String.format("Keycloak %s error %d %s: %s", op, ex.getStatusCode().value(),
        ex.getStatusText(), safeBody(ex));
    return new RuntimeException(msg, ex);
  }

  private String safeBody(HttpStatusCodeException ex) {
    try {
      return ex.getResponseBodyAsString();
    } catch (Exception ignored) {
      return "";
    }
  }
}
