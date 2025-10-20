#!/bin/bash
# Test script for Grafana SSO authentication

echo "🔍 Testing Grafana SSO Authentication Flow"
echo "=========================================="
echo ""

echo "1️⃣ Check if services are running..."
docker ps --filter name=core-backend --filter name=core-grafana --filter name=core-nginx --format "table {{.Names}}\t{{.Status}}"
echo ""

echo "2️⃣ Test backend auth endpoint (should return 401 without cookie)..."
docker exec core-nginx curl -s -i http://backend:8080/internal/auth/grafana 2>&1 | head -10
echo ""

echo "3️⃣ Check Grafana JWT configuration..."
docker logs core-grafana 2>&1 | grep -E "GF_AUTH_JWT|jwk_set_url|serve_from_sub_path" | tail -10
echo ""

echo "4️⃣ Check recent Nginx access logs for /core-admin/monitoring..."
docker logs core-nginx 2>&1 | grep "/core-admin/monitoring" | tail -10
echo ""

echo "5️⃣ Check recent backend logs for Grafana auth..."
docker logs core-backend 2>&1 | grep -E "Grafana auth|/internal/auth/grafana" | tail -10
echo ""

echo "✅ Test complete!"
echo ""
echo "📝 Next steps:"
echo "  1. Open https://admin.core-platform.local/core-admin/"
echo "  2. Login with Keycloak (test_admin / admin)"
echo "  3. Navigate to Monitoring page"
echo "  4. Check browser console for errors"
echo "  5. Check if Grafana dashboard loads without login prompt"
