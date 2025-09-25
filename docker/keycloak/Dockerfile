# První stage pro build Event Listeneru
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build
COPY keycloak-event-listener/pom.xml .
COPY keycloak-event-listener/src ./src

RUN mvn clean package

# Druhý stage pro Keycloak s nainstalovaným Event Listenerem
FROM quay.io/keycloak/keycloak:24.0.5

# Kopírujeme sestavený JAR do providers složky
COPY --from=builder /build/target/keycloak-event-listener-1.0.0-SNAPSHOT.jar /opt/keycloak/providers/

# Kopírování custom theme z repozitáře
COPY themes/core-material /opt/keycloak/themes/core-material

# Nastavení ownership a permissions
USER root
RUN chown -R keycloak:keycloak /opt/keycloak/themes/core-material && \
  chmod -R 755 /opt/keycloak/themes/core-material

# Přepnutí zpět na keycloak uživatele
USER keycloak

# Keycloak 26.x build s potřebnými build-time options
RUN /opt/keycloak/bin/kc.sh build --db=postgres --features=scripts,token-exchange,admin-fine-grained-authz

# Kopírování skriptů a konfigurací z docker/keycloak adresáře
COPY docker/keycloak/generate-realm.sh /opt/keycloak/
COPY docker/keycloak/realm-core-platform.json /opt/keycloak/data/import/

# Nastav výchozí log level na DEBUG
ENV KC_LOG_LEVEL=DEBUG
ENV ROOT_LOGLEVEL=DEBUG

# ENTRYPOINT s verbose výstupem pro lepší debugging
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--verbose"]