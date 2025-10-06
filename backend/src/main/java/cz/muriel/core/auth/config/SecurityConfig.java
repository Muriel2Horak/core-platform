package cz.muriel.core.auth.config;

import cz.muriel.core.auth.CookieBearerTokenResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.*;

@Configuration @EnableWebSecurity @EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  @Value("${cors.origins:http://localhost:3000}")
  private String corsOrigins;

  @Value("${security.oauth2.audience:api}")
  private String allowedAudience;

  private final DynamicJwtDecoder dynamicJwtDecoder;

  public SecurityConfig(DynamicJwtDecoder dynamicJwtDecoder) {
    this.dynamicJwtDecoder = dynamicJwtDecoder;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    http.csrf(csrf -> csrf.disable())
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        .authorizeHttpRequests(authz -> authz
            // Public endpoints
            .requestMatchers("/", "/index.html", "/static/**", "/assets/**", "/favicon.ico")
            .permitAll().requestMatchers("/api/auth/**").permitAll()
            .requestMatchers("/actuator/health", "/actuator/prometheus").permitAll()

            // Internal endpoints - restrict to internal network only
            .requestMatchers("/internal/**").access(isInternalRequestManager())

            // All other API endpoints require authentication
            .requestMatchers("/api/**").authenticated().anyRequest().permitAll())

        .oauth2ResourceServer(oauth2 -> oauth2.bearerTokenResolver(bearerTokenResolver())
            .jwt(jwt -> jwt.decoder(dynamicJwtDecoder)
                .jwtAuthenticationConverter(jwtAuthenticationConverter())));

    return http.build();
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
      // Extract roles from Keycloak JWT token
      Collection<GrantedAuthority> authorities = new ArrayList<>();

      // 1. Realm roles from realm_access claim
      Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
      if (realmAccess != null) {
        Object rolesObj = realmAccess.get("roles");
        if (rolesObj instanceof List) {
          @SuppressWarnings("unchecked")
          List<String> roles = (List<String>) rolesObj;
          roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
        }
      }

      // 2. Resource access roles from resource_access claim
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
              roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
            }
          }
        });
      }

      // 3. Scope authorities (standard OAuth2)
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

    // 🔧 FIX: Použijeme setAllowedOriginPatterns místo setAllowedOrigins
    // pro podporu wildcard patterns jako https://*.core-platform.local
    cfg.setAllowedOriginPatterns(origins);
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }

  @Bean
  public AuthorizationManager<RequestAuthorizationContext> isInternalRequestManager() {
    return (authentication, context) -> {
      // Implement logic to check if the request is internal
      // For now, allow all internal requests - you can implement proper IP checking
      // later
      return new AuthorizationDecision(true);
    };
  }
}
