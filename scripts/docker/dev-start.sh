#!/bin/bash

# 🚀 Development startup script - VŠE V KONTEJNERECH
# Správná architektura: Browser → Nginx (port 80) → Frontend/Backend (kontejnery)

echo "🐳 Spouštím development prostředí - vše v kontejnerech..."
echo ""
echo "📡 Přístupové body:"
echo "  🌐 Aplikace (Nginx proxy):  http://localhost"
echo "  🔧 Frontend (direct):       http://localhost:3000"
echo "  🔧 Backend (direct):        http://localhost:8080"
echo "  🗄️ Database:               localhost:5432"
echo ""
echo "🔒 Produkční workflow: Používej http://localhost (přes Nginx)"
echo "🔧 Development debugging: Můžeš použít přímé porty"

# Zastavit současné kontejnery
echo "🛑 Zastavuji staré kontejnery..."
docker-compose down

# Spustit development architekturu
echo "🚀 Spouštím development s kontejnery..."
cd /Users/martinhorak/Projects/core-platform/docker
docker-compose -f docker-compose.dev.yml up --build

echo "✅ Development prostředí je připravené!"