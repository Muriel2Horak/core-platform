#!/bin/bash

# Script to generate Keycloak realm configuration from template
# Usage: ./generate-realm.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Input template and output file (overridable for checks/tests)
REALM_TEMPLATE="${REALM_TEMPLATE:-$SCRIPT_DIR/realm-admin.template.json}"
REALM_OUTPUT="${REALM_OUTPUT:-$SCRIPT_DIR/realm-admin.json}"

# 🌐 Dynamické generování Keycloak realm exportu s aktuální DOMAIN
# Použije environment proměnnou DOMAIN pro generování správných redirect URIs
# 
# Usage: 
#   ./generate-realm.sh           # běžný režim
#   ./generate-realm.sh --debug   # s debug výstupy
#   DEBUG=true ./generate-realm.sh # s debug výstupy přes env

# Kontrola debug režimu
DEBUG_MODE=${DEBUG:-false}
if [[ "$1" == "--debug" ]]; then
    DEBUG_MODE=true
fi

debug_echo() {
    if [[ "$DEBUG_MODE" == "true" ]]; then
        echo "🔍 DEBUG: $1"
    fi
}

debug_echo "Starting generate-realm.sh script"
debug_echo "Script arguments: $@"
debug_echo "Current working directory: $(pwd)"

debug_echo "SCRIPT_DIR = $SCRIPT_DIR"
debug_echo "REALM_TEMPLATE = $REALM_TEMPLATE"
debug_echo "REALM_OUTPUT = $REALM_OUTPUT"

# Kontrola existence template souboru
if [[ ! -f "$REALM_TEMPLATE" ]]; then
    echo "❌ ERROR: Template file not found: $REALM_TEMPLATE"
    exit 1
fi
debug_echo "Template file exists: $REALM_TEMPLATE"

# Načti DOMAIN z .env souboru
ENV_FILE="$SCRIPT_DIR/../../.env"
debug_echo "Looking for .env file at: $ENV_FILE"

if [[ -f "$ENV_FILE" ]]; then
    debug_echo ".env file found, loading variables"
    set -a
    source "$ENV_FILE"
    set +a
    if [[ "$DEBUG_MODE" == "true" ]]; then
        echo "🔍 DEBUG: Variables from .env:"
        grep -v '^#' "$ENV_FILE" | while read line; do
            echo "    $line"
        done
    fi
else
    debug_echo ".env file not found at $ENV_FILE"
fi

# Fallback na default hodnotu
DOMAIN=${DOMAIN:-core-platform.local}

# 🔧 FIX: Admin realm používá admin subdoménu pro konzistenci s ostatními realmy
ADMIN_DOMAIN="admin.${DOMAIN}"

debug_echo "Final DOMAIN value: $DOMAIN"
debug_echo "Final ADMIN_DOMAIN value: $ADMIN_DOMAIN"
debug_echo "KEYCLOAK_ADMIN_CLIENT_SECRET: ${KEYCLOAK_ADMIN_CLIENT_SECRET:-'NOT SET'}"
debug_echo "TEST_USER_PASSWORD: ${TEST_USER_PASSWORD:-'NOT SET'}"
debug_echo "TEST_ADMIN_PASSWORD: ${TEST_ADMIN_PASSWORD:-'NOT SET'}"

echo "🌐 Generating Keycloak realm export for admin realm"
echo "   - Base domain: $DOMAIN"
echo "   - Admin subdomain: $ADMIN_DOMAIN"

# Nahraď všechny template proměnné v realm exportu
debug_echo "Running envsubst command..."

# 🔧 FIX: Nejprve nahradíme fallback syntaxi standardní syntaxí pro envsubst
# Používáme ADMIN_DOMAIN místo DOMAIN pro admin realm
debug_echo "Processing fallback syntax..."
sed 's/\${TEST_USER_PASSWORD:[^}]*}/${TEST_USER_PASSWORD}/g; s/\${TEST_ADMIN_PASSWORD:[^}]*}/${TEST_ADMIN_PASSWORD}/g; s/\${GRAFANA_OIDC_SECRET:[^}]*}/${GRAFANA_OIDC_SECRET}/g' "$REALM_TEMPLATE" | \
DOMAIN="$ADMIN_DOMAIN" GRAFANA_OIDC_SECRET="${GRAFANA_OIDC_SECRET:-grafana-secret-change-in-prod}" envsubst '$DOMAIN $KEYCLOAK_ADMIN_CLIENT_SECRET $TEST_USER_PASSWORD $TEST_ADMIN_PASSWORD $GRAFANA_OIDC_SECRET' > "$REALM_OUTPUT"

# Kontrola, že výstupní soubor byl vytvořen
if [[ ! -f "$REALM_OUTPUT" ]]; then
    echo "❌ ERROR: Output file was not created: $REALM_OUTPUT"
    exit 1
fi

echo "✅ Realm export generated: $REALM_OUTPUT"
echo "   - Frontend URL: https://$ADMIN_DOMAIN"
echo "   - Redirect URIs include: https://$ADMIN_DOMAIN/*"
debug_echo "Output file size: $(wc -c < "$REALM_OUTPUT") bytes"
debug_echo "generate-realm.sh completed"
