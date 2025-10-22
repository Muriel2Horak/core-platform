#!/bin/bash
set -e

echo "🏢 Initializing Grafana organizations..."

# Wait for Grafana to be ready
until curl -s -f -u admin:admin http://localhost:3000/api/health > /dev/null 2>&1; do
  echo "⏳ Waiting for Grafana to be ready..."
  sleep 2
done

echo "✅ Grafana is ready"

# Create Org 2 if it doesn't exist
echo "📊 Creating Org 2: Tenant: admin"
ORG_ID=$(curl -s -u admin:admin -H "Content-Type: application/json" \
  -X POST http://localhost:3000/api/orgs \
  -d '{"name":"Tenant: admin"}' | jq -r '.orgId // empty')

if [ -n "$ORG_ID" ]; then
  echo "✅ Created Org $ORG_ID: Tenant: admin"
else
  echo "ℹ️  Org 'Tenant: admin' may already exist"
fi

echo "🎉 Organization initialization complete"
