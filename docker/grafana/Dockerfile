FROM grafana/grafana:10.4.0

USER root

# Install ca-certificates if not already present
RUN apk add --no-cache ca-certificates

# Copy Keycloak SSL certificate (from docker context, not grafana subdir)
COPY ssl/cert.pem /usr/local/share/ca-certificates/keycloak.crt

# Update CA certificates to trust Keycloak
RUN update-ca-certificates

# Switch back to grafana user
USER grafana

# Keep original entrypoint
CMD ["/run.sh"]
