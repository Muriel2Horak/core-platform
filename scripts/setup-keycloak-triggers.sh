#!/bin/bash
set -e

echo "🔧 Installing Keycloak triggers..."
echo "⏳ Waiting for Keycloak to be ready and create tables..."

# Počkáme až Keycloak vytvoří své tabulky
for i in {1..30}; do
    if docker exec core-db psql -U keycloak -d keycloak -c "\dt user_entity" 2>/dev/null | grep -q user_entity; then
        echo "✅ Keycloak tables detected!"
        break
    fi
    echo "⏳ Waiting for Keycloak tables... ($i/30)"
    sleep 2
done

# Nainstalujeme triggery
echo "📝 Installing triggers..."
docker exec -i core-db psql -U keycloak -d keycloak < docker/db/keycloak-triggers-manual.sql

echo "✅ Keycloak triggers installed successfully!"
