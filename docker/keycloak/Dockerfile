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

# 🔒 SSL CERTIFICATES: zkopíruj SSL certifikáty
COPY ssl/ /opt/keycloak/conf/

# 🔧 SSL PERMISSIONS: nastav správná oprávnění pro SSL certifikáty
USER root
RUN chmod 644 /opt/keycloak/conf/cert.pem && \
  chmod 600 /opt/keycloak/conf/key.pem && \
  chown keycloak:root /opt/keycloak/conf/cert.pem && \
  chown keycloak:root /opt/keycloak/conf/key.pem
USER keycloak

# sestav KC distribuci, aby se provider zaregistroval
RUN /opt/keycloak/bin/kc.sh build --db=postgres

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]

# 🔒 HTTPS MODE: HTTPS mód s SSL certifikáty
CMD ["start", \
  "--https-port=8443", \
  "--https-certificate-file=/opt/keycloak/conf/cert.pem", \
  "--https-certificate-key-file=/opt/keycloak/conf/key.pem", \
  "--hostname-strict=false", \
  "--proxy=edge"]