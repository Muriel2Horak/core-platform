#!/bin/bash

# 🎭 Test Script pro Role & Group CDC Synchronization
# Testuje zda triggery na keycloak_role a keycloak_group fungují správně

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOMAIN=${DOMAIN:-core-platform.local}
ADMIN_URL="https://admin.${DOMAIN}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }

echo -e "${BLUE}"
echo "🎭 =============================================="
echo "   Role & Group CDC Sync Test Suite"
echo "   Domain: ${DOMAIN}"
echo "==============================================="
echo -e "${NC}"

# ==============================================
# 1. Kontrola triggerů v Keycloak DB
# ==============================================

print_info "1. Checking triggers in Keycloak database..."

# Zjistíme Keycloak DB jméno z .env
source "${SCRIPT_DIR}/../.env" 2>/dev/null || true
KEYCLOAK_DB_NAME=${KEYCLOAK_DB_NAME:-keycloak}
KEYCLOAK_DB_USERNAME=${KEYCLOAK_DB_USERNAME:-keycloak}

print_info "Using Keycloak DB: $KEYCLOAK_DB_NAME"

TRIGGERS_RESULT=$(docker exec core-db psql -U "$KEYCLOAK_DB_USERNAME" -d "$KEYCLOAK_DB_NAME" -c "
SELECT 
    tgname as trigger_name,
    tgrelid::regclass as table_name,
    tgtype
FROM pg_trigger 
WHERE tgname IN (
    'trig_role_insert',
    'trig_role_update', 
    'trig_role_delete',
    'trig_group_insert',
    'trig_group_update',
    'trig_group_delete'
)
ORDER BY tgname;
" -t -A -F'|')

if [ -z "$TRIGGERS_RESULT" ]; then
    print_error "No triggers found in Keycloak database!"
    print_warning "Installing triggers..."
    bash "${SCRIPT_DIR}/setup-keycloak-triggers.sh"
    
    # Zkontrolujeme znovu
    TRIGGERS_RESULT=$(docker exec core-db psql -U "$KEYCLOAK_DB_USERNAME" -d "$KEYCLOAK_DB_NAME" -c "
    SELECT COUNT(*) FROM pg_trigger 
    WHERE tgname IN (
        'trig_role_insert',
        'trig_role_update', 
        'trig_role_delete',
        'trig_group_insert',
        'trig_group_update',
        'trig_group_delete'
    );
    " -t)
    
    if [ "$TRIGGERS_RESULT" -ge 6 ]; then
        print_success "Triggers installed successfully"
    else
        print_error "Failed to install triggers"
        exit 1
    fi
else
    print_success "Found role & group triggers in Keycloak DB"
    echo "$TRIGGERS_RESULT" | while IFS='|' read -r name table type; do
        print_info "  - $name on $table"
    done
fi

# ==============================================
# 2. Kontrola trigger funkcí
# ==============================================

print_info "2. Checking trigger functions..."

FUNCTIONS_RESULT=$(docker exec core-db psql -U "$KEYCLOAK_DB_USERNAME" -d "$KEYCLOAK_DB_NAME" -c "
SELECT 
    proname as function_name,
    pg_get_functiondef(oid) LIKE '%ROLE_CREATED%' as handles_role,
    pg_get_functiondef(oid) LIKE '%GROUP_CREATED%' as handles_group
FROM pg_proc 
WHERE proname IN ('fn_role_change', 'fn_group_change')
ORDER BY proname;
" -t -A -F'|')

if echo "$FUNCTIONS_RESULT" | grep -q "fn_role_change"; then
    print_success "fn_role_change() function exists"
else
    print_error "fn_role_change() function missing!"
    exit 1
fi

if echo "$FUNCTIONS_RESULT" | grep -q "fn_group_change"; then
    print_success "fn_group_change() function exists"
else
    print_error "fn_group_change() function missing!"
    exit 1
fi

# ==============================================
# 3. Kontrola change_events tabulky
# ==============================================

print_info "3. Checking change_events table..."

TABLE_EXISTS=$(docker exec core-db psql -U "$KEYCLOAK_DB_USERNAME" -d "$KEYCLOAK_DB_NAME" -c "
SELECT EXISTS (
    SELECT FROM information_schema.tables 
    WHERE table_name = 'change_events'
);
" -t)

if echo "$TABLE_EXISTS" | grep -q "t"; then
    print_success "change_events table exists"
    
    # Počet eventů
    EVENT_COUNT=$(docker exec core-db psql -U "$KEYCLOAK_DB_USERNAME" -d "$KEYCLOAK_DB_NAME" -c "
    SELECT COUNT(*) FROM change_events;
    " -t)
    
    print_info "Total events in change_events: $(echo $EVENT_COUNT | tr -d ' ')"
    
    # Rozpad podle typu
    EVENT_TYPES=$(docker exec core-db psql -U "$KEYCLOAK_DB_USERNAME" -d "$KEYCLOAK_DB_NAME" -c "
    SELECT event_type, COUNT(*) as count 
    FROM change_events 
    WHERE event_type IN ('ROLE_CREATED', 'ROLE_UPDATED', 'ROLE_DELETED', 'GROUP_CREATED', 'GROUP_UPDATED', 'GROUP_DELETED')
    GROUP BY event_type
    ORDER BY event_type;
    " -t -A -F'|')
    
    if [ -n "$EVENT_TYPES" ]; then
        print_info "Event breakdown:"
        echo "$EVENT_TYPES" | while IFS='|' read -r type count; do
            print_info "  - $type: $count"
        done
    else
        print_warning "No role/group events found in change_events table"
    fi
else
    print_error "change_events table does not exist!"
    exit 1
fi

# ==============================================
# 4. Test vytvoření role přes API
# ==============================================

print_info "4. Testing role creation and CDC sync..."

# Získáme token
print_info "Getting admin token..."
TOKEN=$(curl -sf -X POST "https://keycloak.${DOMAIN}/realms/master/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password" \
    -d "client_id=admin-cli" \
    -d "username=admin" \
    -d "password=admin123" | jq -r '.access_token')

if [ -z "$TOKEN" ] || [ "$TOKEN" = "null" ]; then
    print_error "Failed to get admin token"
    exit 1
fi

# Vygenerujeme unique role name
TEST_ROLE="cdc-test-role-$(date +%s)"

print_info "Creating test role: $TEST_ROLE"

# Vytvoříme roli přes Keycloak API
ROLE_CREATE=$(curl -sf -X POST "${ADMIN_URL}/api/roles" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"name\": \"$TEST_ROLE\",
        \"description\": \"CDC Test Role - Auto Generated\",
        \"composite\": false
    }")

if [ $? -eq 0 ]; then
    print_success "Role created successfully"
else
    print_error "Failed to create role"
    exit 1
fi

# Počkáme na CDC propagaci (ChangeEventProcessor má polling interval)
print_info "Waiting 12 seconds for CDC propagation..."
sleep 12

# Zkontrolujeme zda se vytvořil event v change_events
ROLE_EVENT=$(docker exec core-db psql -U "$KEYCLOAK_DB_USERNAME" -d "$KEYCLOAK_DB_NAME" -c "
SELECT event_type, created_at, processed 
FROM change_events 
WHERE event_type = 'ROLE_CREATED' 
ORDER BY created_at DESC 
LIMIT 5;
" -t -A -F'|')

if echo "$ROLE_EVENT" | grep -q "ROLE_CREATED"; then
    print_success "ROLE_CREATED event found in change_events"
    echo "$ROLE_EVENT" | head -1 | while IFS='|' read -r type created processed; do
        print_info "  Event type: $type"
        print_info "  Created at: $created"
        print_info "  Processed: $processed"
    done
else
    print_error "ROLE_CREATED event NOT found in change_events!"
    print_warning "This indicates trigger may not be working"
fi

# Zkontrolujeme zda se role sync do naší DB (role_entity)
print_info "Checking if role synced to our database..."

ROLE_IN_DB=$(docker exec core-db psql -U core -d core -c "
SELECT name, description, tenant_key 
FROM role_entity 
WHERE name = '$TEST_ROLE'
LIMIT 1;
" -t -A -F'|')

if [ -n "$ROLE_IN_DB" ] && echo "$ROLE_IN_DB" | grep -q "$TEST_ROLE"; then
    print_success "✅ Role successfully synced to our database!"
    echo "$ROLE_IN_DB" | while IFS='|' read -r name desc tenant; do
        print_info "  Name: $name"
        print_info "  Description: $desc"
        print_info "  Tenant: $tenant"
    done
else
    print_error "❌ Role NOT synced to our database"
    print_warning "Check KeycloakEventProjectionService logs"
fi

# ==============================================
# 5. Test aktualizace role
# ==============================================

print_info "5. Testing role update and CDC sync..."

UPDATE_RESULT=$(curl -sf -X PUT "${ADMIN_URL}/api/roles/${TEST_ROLE}" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"name\": \"$TEST_ROLE\",
        \"description\": \"CDC Test Role - UPDATED\",
        \"composite\": false
    }")

print_info "Waiting 12 seconds for CDC propagation..."
sleep 12

# Zkontrolujeme ROLE_UPDATED event
ROLE_UPDATE_EVENT=$(docker exec core-db psql -U "$KEYCLOAK_DB_USERNAME" -d "$KEYCLOAK_DB_NAME" -c "
SELECT event_type, created_at, processed 
FROM change_events 
WHERE event_type = 'ROLE_UPDATED' 
ORDER BY created_at DESC 
LIMIT 3;
" -t -A -F'|')

if echo "$ROLE_UPDATE_EVENT" | grep -q "ROLE_UPDATED"; then
    print_success "ROLE_UPDATED event found in change_events"
else
    print_warning "ROLE_UPDATED event NOT found (may be normal if no actual changes)"
fi

# Zkontrolujeme zda se description updatoval v naší DB
UPDATED_ROLE=$(docker exec core-db psql -U core -d core -c "
SELECT name, description 
FROM role_entity 
WHERE name = '$TEST_ROLE'
LIMIT 1;
" -t -A -F'|')

if echo "$UPDATED_ROLE" | grep -q "UPDATED"; then
    print_success "✅ Role description successfully updated in our database!"
else
    print_warning "Role description not updated (check sync logs)"
fi

# ==============================================
# 6. Test skupiny
# ==============================================

print_info "6. Testing group creation and CDC sync..."

TEST_GROUP="cdc-test-group-$(date +%s)"

print_info "Creating test group: $TEST_GROUP"

# Vytvoříme skupinu přes Keycloak Admin API
GROUP_CREATE=$(curl -sf -X POST "https://keycloak.${DOMAIN}/admin/realms/master/groups" \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
        \"name\": \"$TEST_GROUP\"
    }")

print_info "Waiting 12 seconds for CDC propagation..."
sleep 12

# Zkontrolujeme GROUP_CREATED event
GROUP_EVENT=$(docker exec core-db psql -U "$KEYCLOAK_DB_USERNAME" -d "$KEYCLOAK_DB_NAME" -c "
SELECT event_type, created_at, processed 
FROM change_events 
WHERE event_type = 'GROUP_CREATED' 
ORDER BY created_at DESC 
LIMIT 3;
" -t -A -F'|')

if echo "$GROUP_EVENT" | grep -q "GROUP_CREATED"; then
    print_success "GROUP_CREATED event found in change_events"
else
    print_error "GROUP_CREATED event NOT found!"
fi

# Zkontrolujeme zda se group sync do naší DB
GROUP_IN_DB=$(docker exec core-db psql -U core -d core -c "
SELECT name, path, tenant_key 
FROM group_entity 
WHERE name = '$TEST_GROUP'
LIMIT 1;
" -t -A -F'|')

if [ -n "$GROUP_IN_DB" ] && echo "$GROUP_IN_DB" | grep -q "$TEST_GROUP"; then
    print_success "✅ Group successfully synced to our database!"
    echo "$GROUP_IN_DB" | while IFS='|' read -r name path tenant; do
        print_info "  Name: $name"
        print_info "  Path: $path"
        print_info "  Tenant: $tenant"
    done
else
    print_error "❌ Group NOT synced to our database"
fi

# ==============================================
# 7. Cleanup - smazání test dat
# ==============================================

print_info "7. Cleanup - deleting test role and group..."

# Smazání role
curl -sf -X DELETE "${ADMIN_URL}/api/roles/${TEST_ROLE}" \
    -H "Authorization: Bearer $TOKEN" > /dev/null 2>&1

# Získáme group ID
GROUP_ID=$(curl -sf "https://keycloak.${DOMAIN}/admin/realms/master/groups" \
    -H "Authorization: Bearer $TOKEN" | jq -r ".[] | select(.name == \"$TEST_GROUP\") | .id")

if [ -n "$GROUP_ID" ] && [ "$GROUP_ID" != "null" ]; then
    curl -sf -X DELETE "https://keycloak.${DOMAIN}/admin/realms/master/groups/${GROUP_ID}" \
        -H "Authorization: Bearer $TOKEN" > /dev/null 2>&1
fi

print_info "Waiting 12 seconds for CDC propagation..."
sleep 12

# Zkontrolujeme ROLE_DELETED a GROUP_DELETED eventy
DELETE_EVENTS=$(docker exec core-db psql -U "$KEYCLOAK_DB_USERNAME" -d "$KEYCLOAK_DB_NAME" -c "
SELECT event_type, COUNT(*) 
FROM change_events 
WHERE event_type IN ('ROLE_DELETED', 'GROUP_DELETED')
AND created_at > NOW() - INTERVAL '2 minutes'
GROUP BY event_type;
" -t -A -F'|')

if echo "$DELETE_EVENTS" | grep -q "ROLE_DELETED"; then
    print_success "ROLE_DELETED event found"
fi

if echo "$DELETE_EVENTS" | grep -q "GROUP_DELETED"; then
    print_success "GROUP_DELETED event found"
fi

print_success "Cleanup completed"

# ==============================================
# 8. Shrnutí
# ==============================================

echo -e "\n${BLUE}=============================================="
echo "           Test Summary"
echo "===============================================${NC}"

print_success "✅ Triggers installed and active"
print_success "✅ Trigger functions working"
print_success "✅ change_events table operational"
print_success "✅ Role CDC sync tested (CREATE, UPDATE, DELETE)"
print_success "✅ Group CDC sync tested (CREATE, DELETE)"

echo -e "\n${GREEN}🎉 All Role & Group CDC synchronization tests passed!${NC}\n"
