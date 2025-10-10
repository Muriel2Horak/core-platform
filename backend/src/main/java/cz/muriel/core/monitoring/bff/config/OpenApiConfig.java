package cz.muriel.core.monitoring.bff.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration @OpenAPIDefinition(info = @Info(title = "Core Platform - Monitoring BFF API", version = "1.0.0", description = """
    Backend-for-Frontend (BFF) proxy for Grafana with automatic tenant isolation.

    ## Features
    - **Tenant Isolation**: Automatic mapping of JWT to Grafana organization
    - **Security**: Service account tokens never exposed to browser
    - **Rate Limiting**: 100 requests/minute per tenant
    - **Circuit Breaker**: Resilience4j circuit breaker for Grafana API calls
    - **Caching**: 30-second TTL for query results and dashboard metadata

    ## Authentication
    All endpoints (except /health) require Bearer JWT token in Authorization header.
    The JWT must contain tenant claim to identify the Grafana organization.

    ## Rate Limits
    - 100 requests/minute per tenant
    - 429 status code when limit exceeded
    - Retry-After header indicates wait time in seconds
    """, contact = @Contact(name = "Platform Team", email = "platform@core-platform.local"), license = @License(name = "Proprietary", url = "https://core-platform.local/license")), servers = {
    @Server(url = "https://app.core-platform.local", description = "Production server"),
    @Server(url = "http://localhost:8080", description = "Local development server") }) @SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT", description = "JWT token obtained from Keycloak OAuth2 flow. "
        + "Token must contain tenant claim (e.g., 'tenant-id') for Grafana org mapping.")
public class OpenApiConfig {
}
