#!/bin/bash

echo "🚀 Starting Grafana (Standalone Mode - OIDC SSO)"
echo "================================================"

# Wait for PostgreSQL
echo "⏳ Waiting for PostgreSQL..."
until PGPASSWORD="${GF_DATABASE_PASSWORD}" psql -h "${GF_DATABASE_HOST%%:*}" -U "${GF_DATABASE_USER}" -d "${GF_DATABASE_NAME}" -c '\q' 2>/dev/null; do
    echo "   PostgreSQL is unavailable - sleeping"
    sleep 2
done
echo "✅ PostgreSQL is ready"

echo "✅ Grafana starting with OIDC SSO (no org provisioning needed)"

# Start Grafana in foreground (replace this process)
echo "🎯 Starting Grafana server..."
exec /run.sh
