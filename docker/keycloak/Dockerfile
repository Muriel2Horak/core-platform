FROM quay.io/keycloak/keycloak:26.2.0

# Kopírování custom theme z repozitáře
# Témata jsou v themes/core-material/, ne v docker/keycloak/themes/
COPY themes/core-material /opt/keycloak/themes/core-material

# Nastavení ownership a permissions
USER root
RUN chown -R keycloak:keycloak /opt/keycloak/themes/core-material && \
  chmod -R 755 /opt/keycloak/themes/core-material

# Přepnutí zpět na keycloak uživatele
USER keycloak

# Keycloak 26.x vyžaduje build krok před spuštěním
RUN /opt/keycloak/bin/kc.sh build

# Nastav výchozí log level na DEBUG a verbose spuštění
ENV KC_LOG_LEVEL=DEBUG
ENV ROOT_LOGLEVEL=DEBUG

# ENTRYPOINT s verbose výstupem pro lepší debugging
ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--verbose"]