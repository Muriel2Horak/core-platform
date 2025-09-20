#!/bin/bash
set -euo pipefail

echo "🔧 Building Core Material Keycloak Theme..."

# Přejít do theme složky
cd "$(dirname "$0")"

# Vyčistit předchozí build
echo "📦 Cleaning previous build..."
npm run clean || rm -rf dist/

# Instalovat dependencies (pokud potřeba)
if [ ! -d "node_modules" ]; then
    echo "📥 Installing dependencies..."
    npm install
fi

# Build React aplikace
echo "🏗️ Building React components..."
npm run build

# Zkopírovat theme.properties do dist/
echo "📋 Copying theme configuration..."
cp theme.properties dist/

# Vytvořit potřebné složky pro Keycloak
echo "📁 Creating Keycloak theme structure..."
mkdir -p dist/login
mkdir -p dist/account
mkdir -p dist/email

# Zkopírovat základní template soubory (pokud existují)
if [ -d "templates" ]; then
    echo "📄 Copying template files..."
    cp -r templates/* dist/
fi

echo "✅ Theme build completed!"
echo ""
echo "📍 Built files are in: ./dist/"
echo "🔗 Next steps:"
echo "   1. Build theme: npm run build"
echo "   2. Start Docker: ./scripts/docker/up.sh"
echo "   3. Configure Keycloak Admin Console:"
echo "      - Login Theme: core-material"
echo "      - Account Theme: core-material"
echo ""