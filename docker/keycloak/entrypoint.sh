#!/bin/bash
set -e

# Start Keycloak in background
echo "🚀 Starting Keycloak..."
/opt/keycloak/bin/kc.sh start \
  --https-port=8443 \
  --https-certificate-file=/opt/keycloak/conf/cert.pem \
  --https-certificate-key-file=/opt/keycloak/conf/key.pem \
  --hostname-strict=false \
  --proxy=edge \
  --import-realm \
  --spi-import-if-exists=skip &

KC_PID=$!

# Wait a bit for Keycloak to start
sleep 10

# Run realm initialization
echo "🔧 Running realm initialization..."
/opt/keycloak/init-realm.sh || echo "⚠️ Realm init failed, will retry on next start"

# Keep Keycloak running
wait $KC_PID
