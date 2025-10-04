#!/bin/bash

# 🏗️ Keycloak Bootstrap Script pro tenant realmy
# Idempotentní skript pro vytvoření/aktualizaci realm s kompletní konfigurací

set -e

# 📋 Parametry (z environment nebo defaulty)
REALM=${REALM:-"test-tenant"}
WEBHOOK_URL=${WEBHOOK_URL:-"http://backend:8080/internal/keycloak/events"}
WEBHOOK_SECRET=${WEBHOOK_SECRET:-"webhook-secret-change-me-in-production"}
TENANT_ADMIN=${TENANT_ADMIN:-"tenant-admin"}
TENANT_ADMIN_PASSWORD=${TENANT_ADMIN_PASSWORD:-"TempPass123!"}
KEYCLOAK_ADMIN_USER=${KEYCLOAK_ADMIN_USER:-"admin"}
KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD:-"admin123"}
KC_BASE_URL=${KC_BASE_URL:-"http://localhost:8080"}

echo "🚀 Keycloak Bootstrap Script"
echo "=================================="
echo "🏢 Realm: $REALM"
echo "🔗 Webhook URL: $WEBHOOK_URL"
echo "👤 Admin User: $TENANT_ADMIN"
echo "🌐 Keycloak: $KC_BASE_URL"
echo ""

# 🔐 Login k admin CLI
echo "🔐 Authenticating with Keycloak..."
/opt/keycloak/bin/kcadm.sh config credentials --server "$KC_BASE_URL" --realm master --user "$KEYCLOAK_ADMIN_USER" --password "$KEYCLOAK_ADMIN_PASSWORD"

# 🏗️ Funkce: Vytvoř/aktualizuj realm
setup_realm() {
    echo "🏗️ Setting up realm: $REALM"
    
    # Zkontroluj, zda realm existuje
    if /opt/keycloak/bin/kcadm.sh get realms/$REALM >/dev/null 2>&1; then
        echo "✅ Realm $REALM already exists, updating..."
        REALM_EXISTS=true
    else
        echo "🆕 Creating new realm: $REALM"
        REALM_EXISTS=false
    fi
    
    # Vytvoř nebo aktualizuj realm
    if [ "$REALM_EXISTS" = "false" ]; then
        /opt/keycloak/bin/kcadm.sh create realms -s realm="$REALM" -s enabled=true -s displayName="$REALM"
    fi
    
    # Aktualizuj základní nastavení realmu
    /opt/keycloak/bin/kcadm.sh update realms/$REALM -s enabled=true \
        -s loginWithEmailAllowed=true \
        -s duplicateEmailsAllowed=false \
        -s resetPasswordAllowed=true \
        -s rememberMe=true \
        -s verifyEmail=false \
        -s editUsernameAllowed=false \
        -s registrationAllowed=false \
        -s defaultRoles="[\"user\"]"
    
    # 🎯 Konfigurace events a webhook SPI
    echo "🔗 Configuring events and webhook..."
    /opt/keycloak/bin/kcadm.sh update realms/$REALM \
        -s eventsEnabled=true \
        -s adminEventsEnabled=true \
        -s 'eventsListeners=["jboss-logging","muriel-webhook"]'
    
    # 📋 Nastavení konkrétních event typů pro webhook
    /opt/keycloak/bin/kcadm.sh update realms/$REALM \
        -s 'enabledEventTypes=["LOGIN","LOGIN_ERROR","REGISTER","REGISTER_ERROR","LOGOUT","LOGOUT_ERROR","UPDATE_PROFILE","UPDATE_PROFILE_ERROR","UPDATE_PASSWORD","UPDATE_PASSWORD_ERROR","UPDATE_EMAIL","UPDATE_EMAIL_ERROR"]'
    
    # 🔧 SPI konfigurace přes realm attributes (backup metoda)
    /opt/keycloak/bin/kcadm.sh update realms/$REALM \
        -s 'attributes.muriel_webhook_url="'$WEBHOOK_URL'"' \
        -s 'attributes.muriel_webhook_secret="'$WEBHOOK_SECRET'"'
    
    echo "✅ Realm $REALM configured"
}

# 👥 Funkce: Vytvoř klienty
setup_clients() {
    echo "👥 Setting up clients..."
    
    # 🌐 core-frontend (public client)
    if ! /opt/keycloak/bin/kcadm.sh get clients -r $REALM --fields clientId | grep -q "core-frontend" 2>/dev/null; then
        echo "🆕 Creating core-frontend client..."
        /opt/keycloak/bin/kcadm.sh create clients -r $REALM \
            -s clientId=core-frontend \
            -s name="Core Frontend" \
            -s protocol=openid-connect \
            -s publicClient=true \
            -s enabled=true \
            -s standardFlowEnabled=true \
            -s directAccessGrantsEnabled=true \
            -s 'redirectUris=["*"]' \
            -s 'webOrigins=["*"]'
    else
        echo "✅ core-frontend client already exists"
    fi
    
    # 🏗️ core-backend (confidential client)
    if ! /opt/keycloak/bin/kcadm.sh get clients -r $REALM --fields clientId | grep -q "core-backend" 2>/dev/null; then
        echo "🆕 Creating core-backend client..."
        /opt/keycloak/bin/kcadm.sh create clients -r $REALM \
            -s clientId=core-backend \
            -s name="Core Backend" \
            -s protocol=openid-connect \
            -s publicClient=false \
            -s enabled=true \
            -s serviceAccountsEnabled=true \
            -s standardFlowEnabled=false \
            -s directAccessGrantsEnabled=false
    else
        echo "✅ core-backend client already exists"
    fi
    
    echo "✅ Clients configured"
}

# 🎭 Funkce: Vytvoř role
setup_roles() {
    echo "🎭 Setting up roles..."
    
    # 📂 Client role na core-backend
    BACKEND_CLIENT_ID=$(/opt/keycloak/bin/kcadm.sh get clients -r $REALM --fields id,clientId | grep '"clientId" : "core-backend"' -B1 | grep '"id"' | sed 's/.*"id" : "\([^"]*\)".*/\1/')
    
    if [ -n "$BACKEND_CLIENT_ID" ]; then
        # USER_DIRECTORY role
        for ROLE in USER_DIRECTORY_VIEW USER_DIRECTORY_RW USER_DIRECTORY_ADMIN; do
            if ! /opt/keycloak/bin/kcadm.sh get clients/$BACKEND_CLIENT_ID/roles -r $REALM --fields name | grep -q "$ROLE" 2>/dev/null; then
                echo "🆕 Creating client role: $ROLE"
                /opt/keycloak/bin/kcadm.sh create clients/$BACKEND_CLIENT_ID/roles -r $REALM -s name=$ROLE -s description="$ROLE role for user directory access"
            else
                echo "✅ Client role $ROLE already exists"
            fi
        done
    fi
    
    # 👤 Realm role
    for ROLE in user tenant-admin; do
        if ! /opt/keycloak/bin/kcadm.sh get roles -r $REALM --fields name | grep -q "\"$ROLE\"" 2>/dev/null; then
            echo "🆕 Creating realm role: $ROLE"
            /opt/keycloak/bin/kcadm.sh create roles -r $REALM -s name=$ROLE -s description="$ROLE role"
        else
            echo "✅ Realm role $ROLE already exists"
        fi
    done
    
    # 🔗 Kompozitní role - tenant-admin
    echo "🔗 Setting up composite roles..."
    
    # tenant-admin → všechny USER_DIRECTORY role
    for CLIENT_ROLE in USER_DIRECTORY_VIEW USER_DIRECTORY_RW USER_DIRECTORY_ADMIN; do
        /opt/keycloak/bin/kcadm.sh add-roles -r $REALM --rname tenant-admin --cclientid core-backend --rolename $CLIENT_ROLE 2>/dev/null || true
    done
    
    # user → USER_DIRECTORY_VIEW
    /opt/keycloak/bin/kcadm.sh add-roles -r $REALM --rname user --cclientid core-backend --rolename USER_DIRECTORY_VIEW 2>/dev/null || true
    
    echo "✅ Roles configured"
}

# 👥 Funkce: Vytvoř skupiny
setup_groups() {
    echo "👥 Setting up groups..."
    
    # 🏢 tenant-admins skupina
    if ! /opt/keycloak/bin/kcadm.sh get groups -r $REALM --fields name | grep -q "tenant-admins" 2>/dev/null; then
        echo "🆕 Creating tenant-admins group..."
        GROUP_ID=$(/opt/keycloak/bin/kcadm.sh create groups -r $REALM -s name=tenant-admins -s 'attributes.description=["Administrators of tenant '${REALM}'"]' -i)
    else
        echo "✅ tenant-admins group already exists"
        GROUP_ID=$(/opt/keycloak/bin/kcadm.sh get groups -r $REALM --fields id,name | grep '"name" : "tenant-admins"' -B1 | grep '"id"' | sed 's/.*"id" : "\([^"]*\)".*/\1/')
    fi
    
    if [ -n "$GROUP_ID" ]; then
        # Přiřaď realm-management role
        echo "🔐 Assigning realm-management roles to tenant-admins group..."
        for ROLE in view-users query-users manage-users view-groups query-groups; do
            /opt/keycloak/bin/kcadm.sh add-roles -r $REALM --gid $GROUP_ID --cclientid realm-management --rolename $ROLE 2>/dev/null || true
        done
        
        # Přiřaď app role z core-backend
        for ROLE in USER_DIRECTORY_VIEW USER_DIRECTORY_RW USER_DIRECTORY_ADMIN; do
            /opt/keycloak/bin/kcadm.sh add-roles -r $REALM --gid $GROUP_ID --cclientid core-backend --rolename $ROLE 2>/dev/null || true
        done
        
        # Přiřaď realm role
        /opt/keycloak/bin/kcadm.sh add-roles -r $REALM --gid $GROUP_ID --rolename tenant-admin 2>/dev/null || true
    fi
    
    echo "✅ Groups configured"
}

# 👤 Funkce: Vytvoř tenant admin uživatele
setup_admin_user() {
    echo "👤 Setting up admin user: $TENANT_ADMIN"
    
    # Zkontroluj, zda uživatel existuje
    if /opt/keycloak/bin/kcadm.sh get users -r $REALM -q username=$TENANT_ADMIN --fields username | grep -q "$TENANT_ADMIN" 2>/dev/null; then
        echo "✅ User $TENANT_ADMIN already exists"
        USER_ID=$(/opt/keycloak/bin/kcadm.sh get users -r $REALM -q username=$TENANT_ADMIN --fields id | grep '"id"' | sed 's/.*"id" : "\([^"]*\)".*/\1/')
    else
        echo "🆕 Creating admin user: $TENANT_ADMIN"
        USER_ID=$(/opt/keycloak/bin/kcadm.sh create users -r $REALM \
            -s username=$TENANT_ADMIN \
            -s enabled=true \
            -s emailVerified=true \
            -s firstName="Tenant" \
            -s lastName="Administrator" \
            -s email="${TENANT_ADMIN}@${REALM}.local" -i)
        
        # Nastav dočasné heslo
        /opt/keycloak/bin/kcadm.sh set-password -r $REALM --username $TENANT_ADMIN --new-password "$TENANT_ADMIN_PASSWORD" --temporary
    fi
    
    # Přiřaď do tenant-admins skupiny
    if [ -n "$USER_ID" ]; then
        GROUP_ID=$(/opt/keycloak/bin/kcadm.sh get groups -r $REALM --fields id,name | grep '"name" : "tenant-admins"' -B1 | grep '"id"' | sed 's/.*"id" : "\([^"]*\)".*/\1/')
        if [ -n "$GROUP_ID" ]; then
            /opt/keycloak/bin/kcadm.sh update users/$USER_ID/groups/$GROUP_ID -r $REALM -s realm=$REALM -s userId=$USER_ID -s groupId=$GROUP_ID -n 2>/dev/null || true
            echo "✅ User $TENANT_ADMIN added to tenant-admins group"
        fi
    fi
    
    echo "✅ Admin user configured"
}

# 🚀 Hlavní execution
echo "🚀 Starting bootstrap process..."

setup_realm
setup_clients  
setup_roles
setup_groups
setup_admin_user

echo ""
echo "🎉 Bootstrap completed successfully!"
echo "=================================="
echo "🏢 Realm: $REALM"
echo "👤 Admin: $TENANT_ADMIN (password: $TENANT_ADMIN_PASSWORD - CHANGE IT!)"
echo "🔗 Webhook: Configured for $WEBHOOK_URL"
echo "📧 Events: Enabled with muriel-webhook listener"
echo ""
echo "🌐 Next steps:"
echo "  1. Login to Keycloak admin console"
echo "  2. Change the temporary password for $TENANT_ADMIN"
echo "  3. Test webhook functionality"
echo "  4. Create additional users as needed"