#!/bin/bash

echo "🚀 Starting Grafana with organization pre-provisioning..."
echo "================================================"
echo "  Grafana Multi-Tenant Initialization"
echo "================================================"

# NOTE: JWKS fetch is now handled by separate jwks-refresher sidecar container
# No need to fetch here or run background loop

# Wait for PostgreSQL
echo "⏳ Waiting for PostgreSQL..."
until PGPASSWORD="${GF_DATABASE_PASSWORD}" psql -h "${GF_DATABASE_HOST%%:*}" -U "${GF_DATABASE_USER}" -d "${GF_DATABASE_NAME}" -c '\q' 2>/dev/null; do
    echo "   PostgreSQL is unavailable - sleeping"
    sleep 2
done
echo "✅ PostgreSQL is ready"

# Wait for JWKS file from refresher sidecar
echo "⏳ Waiting for JWKS file from refresher..."
RETRY=0
MAX_RETRIES=30
while [ $RETRY -lt $MAX_RETRIES ]; do
    if [ -f "/var/lib/grafana/jwks.json" ]; then
        echo "✅ JWKS file found"
        break
    fi
    echo "   JWKS not ready yet - sleeping"
    sleep 2
    RETRY=$((RETRY + 1))
done

if [ ! -f "/var/lib/grafana/jwks.json" ]; then
    echo "⚠️  WARNING: JWKS file not found after waiting, Grafana may fail JWT validation"
fi

# Background job to create organizations after Grafana starts
(
    echo "⏳ [Background] Waiting for Grafana API..."
    RETRY=0
    MAX_RETRIES=60
    
    while [ $RETRY -lt $MAX_RETRIES ]; do
        if curl -sf http://localhost:3000/api/health >/dev/null 2>&1; then
            echo "✅ [Background] Grafana API is ready"
            
            # Create Org 2 for tenant "admin"
            echo "🏢 [Background] Creating tenant organizations..."
            ORG_RESPONSE=$(curl -s -u admin:${GF_SECURITY_ADMIN_PASSWORD:-admin} \
                -H "Content-Type: application/json" \
                -X POST http://localhost:3000/api/orgs \
                -d '{"name":"Tenant: admin"}')
            
            ORG_ID=$(echo "$ORG_RESPONSE" | jq -r '.orgId // empty')
            
            if [ -z "$ORG_ID" ]; then
                # Org may already exist, try to get it
                echo "ℹ️  Org 'Tenant: admin' may already exist, trying to fetch it..."
                ORG_ID=$(curl -s -u admin:${GF_SECURITY_ADMIN_PASSWORD:-admin} \
                    http://localhost:3000/api/orgs/name/Tenant:%20admin | jq -r '.id // empty')
            fi
            
            if [ -n "$ORG_ID" ]; then
                echo "✅ Using Org $ORG_ID: Tenant: admin"
                
                # Create Loki datasource for Org 2
                echo "🔌 Creating Loki datasource for Org $ORG_ID..."
                DS_LOKI=$(curl -s -u admin:${GF_SECURITY_ADMIN_PASSWORD:-admin} \
                    -H "Content-Type: application/json" \
                    -H "X-Grafana-Org-Id: $ORG_ID" \
                    -X POST http://localhost:3000/api/datasources \
                    -d '{
                        "name": "Loki",
                        "type": "loki",
                        "uid": "DS_LOKI",
                        "access": "proxy",
                        "url": "http://loki:3100",
                        "isDefault": true,
                        "editable": true
                    }')
                
                if echo "$DS_LOKI" | jq -e '.id' >/dev/null 2>&1; then
                    echo "✅ Created Loki datasource for Org $ORG_ID"
                else
                    echo "ℹ️  Loki datasource may already exist"
                fi
                
                # Create Prometheus datasource for Org 2
                echo "🔌 Creating Prometheus datasource for Org $ORG_ID..."
                DS_PROM=$(curl -s -u admin:${GF_SECURITY_ADMIN_PASSWORD:-admin} \
                    -H "Content-Type: application/json" \
                    -H "X-Grafana-Org-Id: $ORG_ID" \
                    -X POST http://localhost:3000/api/datasources \
                    -d '{
                        "name": "Prometheus",
                        "type": "prometheus",
                        "uid": "DS_PROMETHEUS",
                        "access": "proxy",
                        "url": "http://prometheus:9090",
                        "isDefault": false,
                        "editable": true
                    }')
                
                if echo "$DS_PROM" | jq -e '.id' >/dev/null 2>&1; then
                    echo "✅ Created Prometheus datasource for Org $ORG_ID"
                else
                    echo "ℹ️  Prometheus datasource may already exist"
                fi
                
                # Provision dashboards from ALL categories for Org 2
                echo "📊 Provisioning dashboards for Org $ORG_ID..."
                DASHBOARD_BASE="/etc/grafana/provisioning/dashboards"
                
                # Import dashboards from all subdirectories (system, streaming, security, etc.)
                for category_dir in "$DASHBOARD_BASE"/*; do
                    if [ -d "$category_dir" ] && [ "$(basename "$category_dir")" != "disabled" ]; then
                        echo "  📁 Category: $(basename "$category_dir")"
                        
                        for dashboard_file in "$category_dir"/*.json; do
                            if [ -f "$dashboard_file" ]; then
                                echo "    📄 Importing $(basename "$dashboard_file")..."
                                
                                # Read dashboard JSON and wrap it in required format
                                DASHBOARD_JSON=$(jq -c '{dashboard: ., overwrite: true, folderId: 0}' "$dashboard_file")
                                
                                curl -s -X POST \
                                    -H "Content-Type: application/json" \
                                    -H "X-Grafana-Org-Id: $ORG_ID" \
                                    -u admin:admin \
                                    "http://localhost:3000/api/dashboards/db" \
                                    -d "$DASHBOARD_JSON" > /dev/null 2>&1
                                
                                echo "    ✅ Imported $(basename "$dashboard_file")"
                            fi
                        done
                    fi
                done
                
                echo "🎉 Grafana initialization complete!"
                echo "================================================"
            else
                echo "❌ Failed to get or create Org 'Tenant: admin'"
            fi
            
            exit 0
        fi
        
        RETRY=$((RETRY + 1))
        sleep 2
    done
    
    echo "❌ [Background] Failed to create organizations - Grafana API never became available"
) &

# Start Grafana in foreground (replace this process)
echo "🎯 Starting Grafana server..."
exec /run.sh
