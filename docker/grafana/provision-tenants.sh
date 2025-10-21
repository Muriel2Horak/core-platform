#!/bin/bash
# Grafana Multi-Tenant Provisioning Script
# This script creates organizations, service accounts, and tokens for each tenant
# and stores the bindings in PostgreSQL database

set -e

GRAFANA_URL="${GRAFANA_URL:-http://localhost:3000}"
GRAFANA_ADMIN_USER="${GRAFANA_ADMIN_USER:-admin}"
GRAFANA_ADMIN_PASSWORD="${GRAFANA_ADMIN_PASSWORD:-admin}"

DB_HOST="${DB_HOST:-db}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-core}"
DB_USER="${DB_USER:-core}"
DB_PASSWORD="${DB_PASSWORD:-core}"

# ⚠️ DEPRECATED: Hardcoded tenants list (fallback only)
# Tenants to provision (space-separated)
# TENANTS="${TENANTS:-admin test-tenant company-b}"

echo "🚀 Starting Grafana tenant provisioning..."
echo "📍 Grafana URL: $GRAFANA_URL"
echo "📍 Database: $DB_HOST:$DB_PORT/$DB_NAME"

# Wait for database to be ready FIRST (need to query tenants)
echo "⏳ Waiting for database to be ready..."
for i in {1..30}; do
  if PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT 1" > /dev/null 2>&1; then
    echo "✅ Database is ready!"
    break
  fi
  echo "   Attempt $i/30..."
  sleep 2
done

# 🔥 NEW: Load tenants dynamically from database
echo "🔍 Loading tenants from database..."
TENANTS=$(PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COALESCE(string_agg(key, ' '), 'admin') FROM tenants;" | xargs)

if [ -z "$TENANTS" ]; then
  echo "⚠️  No tenants found in database, using fallback: admin"
  TENANTS="admin"
fi

echo "� Tenants to provision: $TENANTS"
echo ""

# Wait for Grafana to be ready
echo "⏳ Waiting for Grafana to be ready..."
for i in {1..30}; do
  if curl -s -f -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" "$GRAFANA_URL/api/health" > /dev/null 2>&1; then
    echo "✅ Grafana is ready!"
    break
  fi
  echo "   Attempt $i/30..."
  sleep 2
done

# Create or truncate the table
echo "📊 Ensuring grafana_tenant_bindings table exists..."
PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" <<EOF
CREATE TABLE IF NOT EXISTS grafana_tenant_bindings (
  tenant_id VARCHAR(255) PRIMARY KEY,
  grafana_org_id BIGINT NOT NULL,
  service_account_id BIGINT NOT NULL,
  service_account_name VARCHAR(255) NOT NULL,
  service_account_token VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
EOF

# Process each tenant
for TENANT in $TENANTS; do
  echo ""
  echo "🏢 Processing tenant: $TENANT"
  
  ORG_NAME="Tenant: $TENANT"
  SA_NAME="tenant-${TENANT}-monitoring"
  TOKEN_NAME="${TENANT}-monitoring-token-$(date +%s)"  # Add timestamp for uniqueness
  
  # Check if tenant already provisioned
  EXISTING=$(PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -t -c "SELECT COUNT(*) FROM grafana_tenant_bindings WHERE tenant_id = '$TENANT';")
  
  if [ "$EXISTING" -gt 0 ]; then
    echo "ℹ️  Tenant $TENANT already provisioned, skipping..."
    continue
  fi
  
  # Step 1: Create or find organization
  echo "  📝 Creating organization: $ORG_NAME"
  ORG_RESPONSE=$(curl -s -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" \
    -X POST -H "Content-Type: application/json" \
    -d "{\"name\":\"$ORG_NAME\"}" \
    "$GRAFANA_URL/api/orgs")
  
  if echo "$ORG_RESPONSE" | grep -q "Organization name taken"; then
    echo "  ℹ️  Organization already exists, finding it..."
    ORG_ID=$(curl -s -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" \
      "$GRAFANA_URL/api/orgs" | jq -r ".[] | select(.name==\"$ORG_NAME\") | .id")
  else
    ORG_ID=$(echo "$ORG_RESPONSE" | jq -r '.orgId')
  fi
  
  echo "  ✅ Organization ID: $ORG_ID"
  
  # Step 2: Create service account
  echo "  🔑 Creating service account: $SA_NAME"
  SA_RESPONSE=$(curl -s -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" \
    -H "X-Grafana-Org-Id: $ORG_ID" \
    -X POST -H "Content-Type: application/json" \
    -d "{\"name\":\"$SA_NAME\",\"role\":\"Admin\"}" \
    "$GRAFANA_URL/api/serviceaccounts")
  
  if echo "$SA_RESPONSE" | grep -q "already exists"; then
    echo "  ℹ️  Service account already exists, finding it..."
    SA_ID=$(curl -s -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" \
      -H "X-Grafana-Org-Id: $ORG_ID" \
      "$GRAFANA_URL/api/serviceaccounts/search" | jq -r ".serviceAccounts[] | select(.name==\"$SA_NAME\") | .id")
  else
    SA_ID=$(echo "$SA_RESPONSE" | jq -r '.id')
  fi
  
  echo "  ✅ Service Account ID: $SA_ID"
  
  # Step 3: Create token
  echo "  🎫 Creating API token: $TOKEN_NAME"
  TOKEN_RESPONSE=$(curl -s -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" \
    -H "X-Grafana-Org-Id: $ORG_ID" \
    -X POST -H "Content-Type: application/json" \
    -d "{\"name\":\"$TOKEN_NAME\",\"role\":\"Admin\"}" \
    "$GRAFANA_URL/api/serviceaccounts/$SA_ID/tokens")
  
  TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.key')
  
  if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    echo "  ❌ Failed to create token!"
    echo "  Response: $TOKEN_RESPONSE"
    exit 1
  fi
  
  echo "  ✅ Token created (length: ${#TOKEN})"
  
  # Step 3.5: Switch admin user to tenant org (CRITICAL for datasource creation)
  echo "  🔄 Switching admin user to org $ORG_ID..."
  SWITCH_RESPONSE=$(curl -s -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" \
    -X POST "$GRAFANA_URL/api/user/using/$ORG_ID")
  
  if echo "$SWITCH_RESPONSE" | grep -q "Active organization changed"; then
    echo "  ✅ Admin user switched to org $ORG_ID"
  else
    echo "  ⚠️  Org switch response: $SWITCH_RESPONSE"
  fi
  
  # Step 4: Create datasources in the tenant org
  echo "  📊 Creating datasources in org $ORG_ID..."
  
  # Create Loki datasource
  echo "    → Creating Loki datasource (DS_LOKI)..."
  LOKI_RESPONSE=$(curl -s -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" \
    -H "X-Grafana-Org-Id: $ORG_ID" \
    -X POST -H "Content-Type: application/json" \
    -d '{"name":"Loki","type":"loki","uid":"DS_LOKI","access":"proxy","url":"http://loki:3100","isDefault":true}' \
    "$GRAFANA_URL/api/datasources")
  
  if echo "$LOKI_RESPONSE" | grep -q "Datasource added\|already exists"; then
    echo "    ✅ Loki datasource ready"
  else
    echo "    ⚠️  Loki datasource: $LOKI_RESPONSE"
  fi
  
  # Create Prometheus datasource
  echo "    → Creating Prometheus datasource (DS_PROMETHEUS)..."
  PROM_RESPONSE=$(curl -s -u "$GRAFANA_ADMIN_USER:$GRAFANA_ADMIN_PASSWORD" \
    -H "X-Grafana-Org-Id: $ORG_ID" \
    -X POST -H "Content-Type: application/json" \
    -d '{"name":"Prometheus","type":"prometheus","uid":"DS_PROMETHEUS","access":"proxy","url":"http://prometheus:9090","isDefault":false}' \
    "$GRAFANA_URL/api/datasources")
  
  if echo "$PROM_RESPONSE" | grep -q "Datasource added\|already exists"; then
    echo "    ✅ Prometheus datasource ready"
  else
    echo "    ⚠️  Prometheus datasource: $PROM_RESPONSE"
  fi
  
  # Step 5: Save to database
  echo "  💾 Saving to database..."
  PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" <<EOF
INSERT INTO grafana_tenant_bindings 
  (tenant_id, grafana_org_id, service_account_id, service_account_name, service_account_token)
VALUES 
  ('$TENANT', $ORG_ID, $SA_ID, '$SA_NAME', '$TOKEN')
ON CONFLICT (tenant_id) DO UPDATE SET
  grafana_org_id = EXCLUDED.grafana_org_id,
  service_account_id = EXCLUDED.service_account_id,
  service_account_name = EXCLUDED.service_account_name,
  service_account_token = EXCLUDED.service_account_token;
EOF
  
  echo "  ✅ Tenant $TENANT provisioned successfully!"
done

echo ""
echo "🎉 Grafana tenant provisioning completed!"
echo ""
echo "📊 Summary:"
PGPASSWORD=$DB_PASSWORD psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT tenant_id, grafana_org_id, service_account_id FROM grafana_tenant_bindings;"
