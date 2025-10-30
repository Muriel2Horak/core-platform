# -- Keycloak runtime image --
FROM quay.io/keycloak/keycloak:24.0.4

ENV KC_HEALTH_ENABLED=true

# 🎨 zkopíruj core-material custom theme (fungující verze)
COPY themes/core-material/ /opt/keycloak/themes/core-material/

# 🏗️ ADMIN REALM: zkopíruj admin realm konfiguraci pro automatický import
COPY docker/keycloak/realm-admin.json /opt/keycloak/data/import/

# 🔒 SSL CERTIFICATES: zkopíruj SSL certifikáty
COPY ssl/ /opt/keycloak/conf/

# 🔧 INIT SCRIPTS: zkopíruj init scripty pro realm management
COPY docker/keycloak/init-realm.sh /opt/keycloak/
COPY docker/keycloak/entrypoint.sh /opt/keycloak/

# 🔧 SSL PERMISSIONS: nastav správná oprávnění pro SSL certifikáty a scripty
USER root
RUN chmod 644 /opt/keycloak/conf/cert.pem && \
  chmod 600 /opt/keycloak/conf/key.pem && \
  chown keycloak:root /opt/keycloak/conf/cert.pem && \
  chown keycloak:root /opt/keycloak/conf/key.pem && \
  mkdir -p /opt/keycloak/data/import && \
  chown -R keycloak:root /opt/keycloak/data && \
  chmod +x /opt/keycloak/init-realm.sh && \
  chmod +x /opt/keycloak/entrypoint.sh
USER keycloak

# sestav KC distribuci
RUN /opt/keycloak/bin/kc.sh build --db=postgres

# � Custom entrypoint s realm init
ENTRYPOINT ["/opt/keycloak/entrypoint.sh"]