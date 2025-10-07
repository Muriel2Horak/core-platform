# -- Keycloak runtime image --
FROM quay.io/keycloak/keycloak:24.0.4

ENV KC_HEALTH_ENABLED=true

# 🎨 zkopíruj core-material custom theme (fungující verze)
COPY themes/core-material/ /opt/keycloak/themes/core-material/

# 🏗️ ADMIN REALM: zkopíruj admin realm konfiguraci pro automatický import
COPY docker/keycloak/realm-admin.json /opt/keycloak/data/import/

# 🔒 SSL CERTIFICATES: zkopíruj SSL certifikáty
COPY ssl/ /opt/keycloak/conf/

# 🔧 SSL PERMISSIONS: nastav správná oprávnění pro SSL certifikáty
USER root
RUN chmod 644 /opt/keycloak/conf/cert.pem && \
  chmod 600 /opt/keycloak/conf/key.pem && \
  chown keycloak:root /opt/keycloak/conf/cert.pem && \
  chown keycloak:root /opt/keycloak/conf/key.pem && \
  mkdir -p /opt/keycloak/data/import && \
  chown -R keycloak:root /opt/keycloak/data
USER keycloak

# sestav KC distribuci
RUN /opt/keycloak/bin/kc.sh build --db=postgres

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]

# 🔒 HTTPS MODE: HTTPS mód s SSL certifikáty + import realm
# 🆕 OPTIMIZED: --spi-import-if-exists=skip zajistí, že existující realmy se nepřepíší
CMD ["start", \
  "--https-port=8443", \
  "--https-certificate-file=/opt/keycloak/conf/cert.pem", \
  "--https-certificate-key-file=/opt/keycloak/conf/key.pem", \
  "--hostname-strict=false", \
  "--proxy=edge", \
  "--import-realm", \
  "--spi-import-if-exists=skip"]