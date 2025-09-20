#!/bin/bash

# 🚀 Start Development Environment s Nginx Proxy
# Správná architektura: Browser → Nginx → Frontend/Backend

echo "🚀 Spouštím development s Nginx proxy architekturou..."
echo "📡 Aplikace bude dostupná na: http://localhost"
echo "🔒 Backend NENÍ přímo přístupný - jen přes Nginx proxy!"

# Zastavit starou konfiguraci
echo "🛑 Zastavujem staré kontejnery..."
docker-compose down

# Spustit novou architekturu
echo "🚀 Spouštím novou architekturu..."
docker-compose -f docker-compose.dev.yml up --build

echo "✅ Development prostředí je připravené!"
echo "🌐 Frontend: http://localhost"
echo "🔐 API: http://localhost/api"
echo "🔑 Keycloak: http://localhost/auth"