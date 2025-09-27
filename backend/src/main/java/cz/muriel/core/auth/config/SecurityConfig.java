package cz.muriel.core.auth.config;

import cz.muriel.core.auth.CookieBearerTokenResolver;
import cz.muriel.core.auth.AudienceValidator;
import cz.muriel.core.auth.security.WebhookHmacFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;

@Configuration @EnableWebSecurity @EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  @Value("${cors.origins:http://localhost:3000}")
  private String corsOrigins;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
  private String issuerUri;

  @Value("${security.oauth2.audience:api}")
  private String allowedAudience;

  @Value("${OIDC_JWK_SET_URI:}")
  private String optionalJwkSetUri;

  private final WebhookHmacFilter webhookHmacFilter;

  public SecurityConfig(WebhookHmacFilter webhookHmacFilter) {
    this.webhookHmacFilter = webhookHmacFilter;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http,
      BearerTokenResolver bearerTokenResolver,
      JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
    http.csrf(csrf -> csrf
        // CSRF vypnuto pro webhook endpoint a globálně pro REST API
        .ignoringRequestMatchers("/internal/keycloak/events").disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // 🔐 Keycloak webhook endpoint - permitAll (autentizace přes HMAC filter)
            .requestMatchers(HttpMethod.POST, "/internal/keycloak/events").permitAll()
            // Veřejné endpointy
            .requestMatchers("/api/health", "/api/auth/login", "/api/auth/logout",
                "/api/auth/session", "/actuator/health", "/actuator/prometheus",
                "/actuator/metrics")
            .permitAll()
            // CORS preflight requesty
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            // Frontend logs - pouze authenticated, bez role požadavků
            .requestMatchers(HttpMethod.POST, "/api/frontend-logs").authenticated()
            // Self-service endpointy - pouze authenticated, role kontrola v metodách
            .requestMatchers("/api/me/**").authenticated()
            // User management endpointy - vyžadují CORE_ROLE_USER_MANAGER nebo
            // CORE_ROLE_ADMIN
            .requestMatchers("/api/users/**", "/api/roles/**")
            .hasAnyAuthority("CORE_ROLE_USER_MANAGER", "CORE_ROLE_ADMIN")
            // Všechny ostatní requesty vyžadují autentifikaci
            .anyRequest().authenticated())
        .oauth2ResourceServer(oauth2 -> oauth2.bearerTokenResolver(bearerTokenResolver)
            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

    // 🔐 Přidáme HMAC filter před standardní autentizační filtr
    http.addFilterBefore(webhookHmacFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  JwtDecoder jwtDecoder() {
    // Použij NimbusJwtDecoder s explicitním JWK Set URI místo issuer discovery
    String jwkSetUri = optionalJwkSetUri != null && !optionalJwkSetUri.trim().isEmpty()
        ? optionalJwkSetUri
        : issuerUri + "/protocol/openid-connect/certs";

    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

    // 🔧 FIX: Použijeme upravený AudienceValidator který akceptuje prázdné audience
    OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
    OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(allowedAudience);
    OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer,
        audienceValidator);

    // Nastav oba validátory
    decoder.setJwtValidator(validator);

    return decoder;
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter());
    return converter;
  }

  @Bean
  Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter() {
    return jwt -> {
      // Extrahování rolí z Keycloak JWT tokenu
      Collection<GrantedAuthority> authorities = new ArrayList<>();

      // 1. Realm roles z realm_access claim (primární pro CORE_ROLE_*)
      Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
      if (realmAccess != null) {
        Object rolesObj = realmAccess.get("roles");
        if (rolesObj instanceof List) {
          @SuppressWarnings("unchecked")
          List<String> roles = (List<String>) rolesObj;
          // Mapujeme role 1:1 bez přidávání prefixu "ROLE_"
          roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        }
      }

      // 2. Resource access roles z resource_access claim (volitelné pro budoucí
      // použití)
      Map<String, Object> resourceAccess = jwt.getClaimAsMap("resource_access");
      if (resourceAccess != null) {
        resourceAccess.forEach((clientId, clientAccess) -> {
          if (clientAccess instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> clientAccessMap = (Map<String, Object>) clientAccess;
            Object rolesObj = clientAccessMap.get("roles");
            if (rolesObj instanceof List) {
              @SuppressWarnings("unchecked")
              List<String> roles = (List<String>) rolesObj;
              // Mapujeme client-specific role také 1:1 bez prefixu
              roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
            }
          }
        });
      }

      // 3. Scope authorities (standardní OAuth2)
      String scope = jwt.getClaimAsString("scope");
      if (scope != null) {
        Arrays.stream(scope.split(" ")).forEach(
            scopeValue -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scopeValue)));
      }

      return authorities;
    };
  }

  @Bean
  BearerTokenResolver bearerTokenResolver() {
    return new CookieBearerTokenResolver("at");
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    List<String> origins = Arrays.stream(corsOrigins.split(",")).map(String::trim)
        .filter(s -> !s.isEmpty()).toList();
    cfg.setAllowedOrigins(origins);
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }
}
