# -- build SPI (Maven) --
FROM maven:3.9-eclipse-temurin-17 AS spi-build
WORKDIR /workspace

# stáhni dependency do cache
COPY keycloak-spi-event-webhook/pom.xml keycloak-spi-event-webhook/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -q -f keycloak-spi-event-webhook/pom.xml -DskipTests dependency:go-offline

# build JAR
COPY keycloak-spi-event-webhook/src keycloak-spi-event-webhook/src
RUN --mount=type=cache,target=/root/.m2 mvn -q -f keycloak-spi-event-webhook/pom.xml -DskipTests package

# -- Keycloak runtime image --
FROM quay.io/keycloak/keycloak:24.0.4

ENV KC_HEALTH_ENABLED=true

# zkopíruj SPI provider
COPY --from=spi-build /workspace/keycloak-spi-event-webhook/target/*.jar /opt/keycloak/providers/

# zkopíruj custom témata
COPY themes/ /opt/keycloak/themes/

# sestav KC distribuci, aby se provider zaregistroval
RUN /opt/keycloak/bin/kc.sh build --db=postgres

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start-dev", "--http-enabled=true", "--http-port=8080", "--hostname-strict=false"]