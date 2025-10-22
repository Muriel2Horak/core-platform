FROM grafana/grafana:10.4.0

USER root

# Install dependencies: ca-certificates, postgresql-client, curl, jq
RUN apk add --no-cache ca-certificates postgresql-client curl jq bash

# Copy Keycloak SSL certificate (from docker context, not grafana subdir)
COPY ssl/cert.pem /usr/local/share/ca-certificates/keycloak.crt

# Update CA certificates to trust Keycloak
RUN update-ca-certificates

# Copy custom entrypoint script
COPY grafana/scripts/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Switch back to grafana user
USER grafana

# Use custom entrypoint that handles org creation before provisioning
ENTRYPOINT ["/entrypoint.sh"]
