# Multi-stage build for Keycloak with custom SPI
FROM maven:3.9.4-eclipse-temurin-21-alpine AS spi-builder

# Copy SPI source code
COPY . /workspace
WORKDIR /workspace

# Build the SPI JAR file
RUN mvn clean package -DskipTests

# Production Keycloak image - změněno na funkční verzi 24.0.5
FROM quay.io/keycloak/keycloak:24.0.5

# Copy our SPI JAR from build stage
COPY --from=spi-builder /workspace/target/keycloak-spi-event-webhook-1.0.0.jar /opt/keycloak/providers/

# Set ownership and permissions for SPI JAR
USER root
RUN chown keycloak:keycloak /opt/keycloak/providers/keycloak-spi-event-webhook-1.0.0.jar

# Switch back to keycloak user
USER keycloak

# Build Keycloak with our provider
RUN /opt/keycloak/bin/kc.sh build

# Set production mode
ENV KC_HOSTNAME_STRICT=false
ENV KC_HOSTNAME_STRICT_HTTPS=false
ENV KC_HTTP_ENABLED=true
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Enable event listener
ENV KC_SPI_EVENTS_LISTENER_CORE_PLATFORM_WEBHOOK_ENABLED=true

# Set default log level
ENV KC_LOG_LEVEL=DEBUG
ENV ROOT_LOGLEVEL=DEBUG

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--verbose"]