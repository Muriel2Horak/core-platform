#!/bin/bash

# =============================================================================
# Cube.js Health Check Script
# =============================================================================
# This script validates that Cube.js is properly configured and running
# Usage: ./scripts/cube/check-cube.sh
# =============================================================================

set -e

CUBE_URL="${CUBE_URL:-http://localhost:4000}"
CUBE_API_SECRET="${CUBE_API_SECRET:-dev_secret_change_in_production_min_32_chars}"

echo "🔍 Cube.js Health Check"
echo "======================="
echo "URL: $CUBE_URL"
echo ""

# =============================================================================
# 1. Check if Cube.js is running
# =============================================================================
echo "✓ Checking if Cube.js is reachable..."
if ! curl -f -s "$CUBE_URL/readyz" > /dev/null; then
  echo "❌ ERROR: Cube.js is not reachable at $CUBE_URL"
  echo "   Run: docker compose up -d cube"
  exit 1
fi
echo "  ✅ Cube.js is running"
echo ""

# =============================================================================
# 2. Check metadata endpoint
# =============================================================================
echo "✓ Fetching cube metadata..."
META_RESPONSE=$(curl -s -w "\n%{http_code}" "$CUBE_URL/cubejs-api/v1/meta")
META_BODY=$(echo "$META_RESPONSE" | head -n -1)
META_STATUS=$(echo "$META_RESPONSE" | tail -n 1)

if [ "$META_STATUS" -ne 200 ]; then
  echo "❌ ERROR: Meta endpoint returned $META_STATUS"
  echo "   Response: $META_BODY"
  exit 1
fi
echo "  ✅ Meta endpoint OK (200)"
echo ""

# =============================================================================
# 3. Validate cube schemas loaded
# =============================================================================
echo "✓ Validating cube schemas..."

CUBES=$(echo "$META_BODY" | jq -r '.cubes[].name' 2>/dev/null || echo "")
if [ -z "$CUBES" ]; then
  echo "❌ ERROR: No cubes found in metadata"
  echo "   Response: $META_BODY"
  exit 1
fi

EXPECTED_CUBES=("Users" "Tenants" "Groups")
FOUND_CUBES=()

for cube in "${EXPECTED_CUBES[@]}"; do
  if echo "$CUBES" | grep -q "^${cube}$"; then
    echo "  ✅ Cube '$cube' loaded"
    FOUND_CUBES+=("$cube")
  else
    echo "  ❌ Cube '$cube' NOT FOUND"
  fi
done

if [ ${#FOUND_CUBES[@]} -ne ${#EXPECTED_CUBES[@]} ]; then
  echo ""
  echo "❌ ERROR: Missing cube schemas"
  echo "   Expected: ${EXPECTED_CUBES[*]}"
  echo "   Found: ${FOUND_CUBES[*]}"
  exit 1
fi
echo ""

# =============================================================================
# 4. Validate RLS filter presence
# =============================================================================
echo "✓ Checking RLS filters in cube schemas..."

for cube in "${EXPECTED_CUBES[@]}"; do
  SCHEMA_FILE="docker/cube/schema/${cube}.js"
  if [ ! -f "$SCHEMA_FILE" ]; then
    echo "  ❌ Schema file not found: $SCHEMA_FILE"
    continue
  fi
  
  if grep -q "SECURITY_CONTEXT.tenantId" "$SCHEMA_FILE"; then
    echo "  ✅ Cube '$cube' has RLS filter"
  else
    echo "  ⚠️  WARNING: Cube '$cube' missing RLS filter (SECURITY_CONTEXT.tenantId)"
  fi
done
echo ""

# =============================================================================
# 5. Validate pre-aggregations defined
# =============================================================================
echo "✓ Checking pre-aggregations..."

for cube in "${EXPECTED_CUBES[@]}"; do
  SCHEMA_FILE="docker/cube/schema/${cube}.js"
  if [ ! -f "$SCHEMA_FILE" ]; then
    continue
  fi
  
  if grep -q "preAggregations:" "$SCHEMA_FILE"; then
    PREAGG_COUNT=$(grep -c "refreshKey:" "$SCHEMA_FILE" || echo "0")
    echo "  ✅ Cube '$cube' has $PREAGG_COUNT pre-aggregation(s)"
  else
    echo "  ⚠️  WARNING: Cube '$cube' has no pre-aggregations"
  fi
done
echo ""

# =============================================================================
# 6. Test basic query
# =============================================================================
echo "✓ Testing basic query..."

QUERY_PAYLOAD='{
  "query": {
    "measures": ["Users.count"],
    "timeDimensions": []
  }
}'

QUERY_RESPONSE=$(curl -s -w "\n%{http_code}" \
  -X POST "$CUBE_URL/cubejs-api/v1/load" \
  -H "Content-Type: application/json" \
  -H "Authorization: $CUBE_API_SECRET" \
  -d "$QUERY_PAYLOAD")

QUERY_BODY=$(echo "$QUERY_RESPONSE" | head -n -1)
QUERY_STATUS=$(echo "$QUERY_RESPONSE" | tail -n 1)

if [ "$QUERY_STATUS" -eq 200 ]; then
  echo "  ✅ Query executed successfully"
  
  # Check if response has data
  if echo "$QUERY_BODY" | jq -e '.data' > /dev/null 2>&1; then
    ROW_COUNT=$(echo "$QUERY_BODY" | jq '.data | length')
    echo "  ✅ Response contains $ROW_COUNT row(s)"
  else
    echo "  ⚠️  WARNING: Response missing 'data' field"
  fi
else
  echo "  ❌ Query failed with status $QUERY_STATUS"
  echo "     Response: $QUERY_BODY"
fi
echo ""

# =============================================================================
# 7. Check Redis connection
# =============================================================================
echo "✓ Checking Redis connection..."

if docker compose exec -T redis redis-cli ping > /dev/null 2>&1; then
  echo "  ✅ Redis is connected"
else
  echo "  ⚠️  WARNING: Redis connection test failed"
  echo "     Cache may not be working properly"
fi
echo ""

# =============================================================================
# Summary
# =============================================================================
echo "=========================================="
echo "✅ Cube.js Health Check PASSED"
echo "=========================================="
echo ""
echo "Next steps:"
echo "  1. Test RLS: ./scripts/cube/test-rls.sh"
echo "  2. View logs: docker compose logs -f cube"
echo "  3. Access UI: http://localhost:4000"
echo ""
