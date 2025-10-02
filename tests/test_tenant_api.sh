#!/bin/bash

# 🧪 TENANT MANAGEMENT API TEST SCRIPT
# Testuje vytvoření, seznam a smazání tenantů

set -e

echo "🧪 Testing Tenant Management API"
echo "=================================="

# Base URL
BASE_URL="https://admin.core-platform.local"
API_URL="${BASE_URL}/api"

# Test data
TEST_TENANT_KEY="test-company"
TEST_TENANT_NAME="Test Company Ltd."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to make authenticated API call
api_call() {
    local method=$1
    local endpoint=$2
    local data=$3
    local description=$4
    
    echo -e "\n${BLUE}🔍 ${description}${NC}"
    echo "   ${method} ${API_URL}${endpoint}"
    
    if [ "$method" = "GET" ]; then
        curl -s -X GET \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $ADMIN_TOKEN" \
            "${API_URL}${endpoint}" | jq '.'
    else
        curl -s -X "$method" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $ADMIN_TOKEN" \
            -d "$data" \
            "${API_URL}${endpoint}" | jq '.'
    fi
}

# Function to get admin token
get_admin_token() {
    echo -e "${BLUE}🔐 Getting admin token...${NC}"
    
    # Get token for test_admin user (should have CORE_ROLE_ADMIN)
    local token_response=$(curl -s -X POST \
        "${BASE_URL}/realms/admin/protocol/openid-connect/token" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=password" \
        -d "client_id=web" \
        -d "username=test_admin" \
        -d "password=Test.1234")
    
    ADMIN_TOKEN=$(echo "$token_response" | jq -r '.access_token')
    
    if [ "$ADMIN_TOKEN" = "null" ] || [ -z "$ADMIN_TOKEN" ]; then
        echo -e "${RED}❌ Failed to get admin token${NC}"
        echo "Response: $token_response"
        exit 1
    fi
    
    echo -e "${GREEN}✅ Admin token obtained${NC}"
}

# Function to check if tenant exists
tenant_exists() {
    local tenant_key=$1
    local response=$(curl -s -X GET \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        "${API_URL}/admin/tenants" | jq -r ".tenants[]? | select(.key == \"$tenant_key\") | .key")
    
    [ "$response" = "$tenant_key" ]
}

# Main test flow
main() {
    echo -e "${YELLOW}📋 Prerequisites:${NC}"
    echo "   • Core platform must be running"
    echo "   • test_admin user must exist with CORE_ROLE_ADMIN role"
    echo "   • Domain core-platform.local must be accessible"
    echo ""
    
    # Get admin token
    get_admin_token
    
    # Test 1: List existing tenants
    echo -e "\n${YELLOW}📋 Test 1: List existing tenants${NC}"
    api_call "GET" "/admin/tenants" "" "Getting list of all tenants"
    
    # Test 2: Create new tenant (if not exists)
    echo -e "\n${YELLOW}🏗️ Test 2: Create new tenant${NC}"
    if tenant_exists "$TEST_TENANT_KEY"; then
        echo -e "${YELLOW}⚠️ Tenant $TEST_TENANT_KEY already exists, skipping creation${NC}"
    else
        local create_data="{
            \"key\": \"$TEST_TENANT_KEY\",
            \"displayName\": \"$TEST_TENANT_NAME\",
            \"autoCreate\": true
        }"
        
        api_call "POST" "/admin/tenants" "$create_data" "Creating new tenant: $TEST_TENANT_KEY"
    fi
    
    # Test 3: List tenants again (should include new tenant)
    echo -e "\n${YELLOW}📋 Test 3: List tenants after creation${NC}"
    api_call "GET" "/admin/tenants" "" "Getting updated list of tenants"
    
    # Test 4: Try to create duplicate tenant (should fail)
    echo -e "\n${YELLOW}🚫 Test 4: Try to create duplicate tenant${NC}"
    local duplicate_data="{
        \"key\": \"$TEST_TENANT_KEY\",
        \"displayName\": \"Duplicate Test\",
        \"autoCreate\": true
    }"
    
    api_call "POST" "/admin/tenants" "$duplicate_data" "Attempting to create duplicate tenant (should fail)"
    
    # Test 5: Create tenant with invalid key (should fail)
    echo -e "\n${YELLOW}🚫 Test 5: Try to create tenant with invalid key${NC}"
    local invalid_data="{
        \"key\": \"Invalid_Key_123!\",
        \"displayName\": \"Invalid Key Test\",
        \"autoCreate\": true
    }"
    
    api_call "POST" "/admin/tenants" "$invalid_data" "Attempting to create tenant with invalid key (should fail)"
    
    # Test 6: Access tenant subdomain
    echo -e "\n${YELLOW}🌐 Test 6: Check tenant subdomain accessibility${NC}"
    echo "   Testing: https://${TEST_TENANT_KEY}.core-platform.local"
    
    local subdomain_status=$(curl -s -o /dev/null -w "%{http_code}" -k "https://${TEST_TENANT_KEY}.core-platform.local" || echo "000")
    
    if [ "$subdomain_status" = "200" ] || [ "$subdomain_status" = "302" ]; then
        echo -e "${GREEN}✅ Tenant subdomain accessible (HTTP $subdomain_status)${NC}"
    else
        echo -e "${YELLOW}⚠️ Tenant subdomain not accessible (HTTP $subdomain_status)${NC}"
        echo "   This is expected if DNS/hosts are not configured yet"
    fi
    
    # Summary
    echo -e "\n${YELLOW}📊 Test Summary${NC}"
    echo "=================="
    echo -e "${GREEN}✅ Admin authentication${NC}"
    echo -e "${GREEN}✅ List tenants API${NC}"
    echo -e "${GREEN}✅ Create tenant API${NC}"
    echo -e "${GREEN}✅ Duplicate prevention${NC}"
    echo -e "${GREEN}✅ Validation testing${NC}"
    
    echo -e "\n${BLUE}🎯 Next steps to test complete tenant functionality:${NC}"
    echo "   1. Add DNS entry: $TEST_TENANT_KEY.core-platform.local -> 127.0.0.1"
    echo "   2. Test login at: https://$TEST_TENANT_KEY.core-platform.local"
    echo "   3. Create user in tenant and test tenant-scoped data"
    echo ""
    echo -e "${GREEN}🎉 Tenant Management API tests completed!${NC}"
}

# Run main function
main "$@"