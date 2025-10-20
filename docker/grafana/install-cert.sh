#!/bin/bash
# Install Keycloak SSL certificate to Grafana's trust store
# This allows Grafana to verify HTTPS connections to Keycloak JWK endpoint

set -e

echo "🔐 Installing Keycloak SSL certificate..."

# Check if certificate exists
if [ ! -f /etc/ssl/certs/keycloak.crt ]; then
    echo "⚠️ Keycloak certificate not found at /etc/ssl/certs/keycloak.crt"
    exit 0
fi

# Install certificate to system trust store
# Alpine Linux uses ca-certificates package
if command -v update-ca-certificates >/dev/null 2>&1; then
    # Copy to trusted certificates directory
    cp /etc/ssl/certs/keycloak.crt /usr/local/share/ca-certificates/keycloak.crt
    
    # Update CA certificates
    update-ca-certificates
    
    echo "✅ Keycloak certificate installed successfully"
else
    echo "⚠️ update-ca-certificates not found, skipping certificate installation"
fi

exit 0
