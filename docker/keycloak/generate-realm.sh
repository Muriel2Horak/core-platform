#!/bin/bash

# 🌐 Dynamické generování Keycloak realm exportu s aktuální DOMAIN
# Použije environment proměnnou DOMAIN pro generování správných redirect URIs

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REALM_TEMPLATE="$SCRIPT_DIR/realm-core-platform.template.json"
REALM_OUTPUT="$SCRIPT_DIR/realm-core-platform.json"

# Načti DOMAIN z .env souboru
if [[ -f "$SCRIPT_DIR/../../.env" ]]; then
    export $(grep -v '^#' "$SCRIPT_DIR/../../.env" | xargs)
fi

# Fallback na default hodnotu
DOMAIN=${DOMAIN:-core-platform.local}

echo "🌐 Generating Keycloak realm export for domain: $DOMAIN"

# Nahraď všechny template proměnné v realm exportu
envsubst '$DOMAIN' < "$REALM_TEMPLATE" > "$REALM_OUTPUT"

echo "✅ Realm export generated: $REALM_OUTPUT"
echo "   - Frontend URL: https://$DOMAIN"
echo "   - Redirect URIs include: https://$DOMAIN/*"