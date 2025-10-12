package cz.muriel.core.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration for API documentation.
 *
 * <p>Provides interactive API documentation at: - Swagger UI: http://localhost:8080/swagger-ui.html
 * - OpenAPI JSON: http://localhost:8080/api/docs - OpenAPI YAML: http://localhost:8080/api/docs.yaml
 *
 * <p>Security: - JWT Bearer authentication required for all endpoints - Obtain JWT from Keycloak:
 * POST /realms/{tenant}/protocol/openid-connect/token
 *
 * @since S9: Docs & Security
 */
@Configuration
public class OpenApiConfig {

  @Value("${security.oauth2.base-domain:core-platform.local}")
  private String baseDomain;

  @Value("${springdoc.info.version:1.0.0}")
  private String apiVersion;

  /**
   * Configure OpenAPI specification with JWT security scheme.
   *
   * @return OpenAPI configuration
   */
  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Core Platform API")
                .version(apiVersion)
                .description(
                    """
                    Multi-tenant platform with user management, roles, permissions, and analytics.

                    **Features:**
                    - 👥 User Management (CRUD, search, role assignment)
                    - 🏢 Tenant Management (multi-tenancy, isolation)
                    - 🔐 Role & Permission Management (RBAC, dynamic permissions)
                    - 📊 Cube.js Analytics (model generation, pre-aggregations)
                    - 🔄 Real-time Streaming (Kafka-based entity lifecycle events)
                    - 📈 Grafana Integration (multi-tenant dashboards, user sync)

                    **Authentication:**
                    - JWT Bearer tokens from Keycloak
                    - Tenant-specific realms: https://{tenant}.core-platform.local/realms/{tenant}
                    - Example: `https://test-tenant.core-platform.local/realms/test-tenant`

                    **Authorization:**
                    - Role-based access control (RBAC)
                    - Permissions: `users:read`, `users:write`, `tenants:admin`, etc.
                    - Tenant isolation enforced at database and API levels

                    **Rate Limiting:**
                    - Not currently enforced (planned for future release)

                    **API Versioning:**
                    - Current version: v1.0.0
                    - Breaking changes will increment major version
                    """)
                .contact(
                    new Contact()
                        .name("Core Platform Team")
                        .email("support@core-platform.local")
                        .url("https://github.com/Muriel2Horak/core-platform"))
                .license(new License().name("Proprietary").url("#")))
        .servers(
            List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server"),
                new Server()
                    .url("https://api." + baseDomain)
                    .description("Production API Server")))
        .components(
            new Components()
                .addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            """
                            JWT Bearer token from Keycloak.

                            **How to obtain token:**
                            ```bash
                            curl -X POST https://test-tenant.core-platform.local/realms/test-tenant/protocol/openid-connect/token \\
                              -H "Content-Type: application/x-www-form-urlencoded" \\
                              -d "grant_type=password" \\
                              -d "client_id=backend" \\
                              -d "username=admin@test.com" \\
                              -d "password=admin"
                            ```

                            **Response:**
                            ```json
                            {
                              "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
                              "expires_in": 300,
                              "refresh_expires_in": 1800,
                              "refresh_token": "...",
                              "token_type": "Bearer"
                            }
                            ```

                            **Usage in Swagger UI:**
                            1. Click "Authorize" button
                            2. Paste access_token (without "Bearer " prefix)
                            3. Click "Authorize" to apply
                            4. Try API endpoints with authenticated requests
                            """)))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}
