#!/bin/bash
set -e

echo "🚀 Dev Container Post-Create Setup"
echo "=================================="

# Detekce Java verze
echo "📦 Checking Java..."
if command -v java &> /dev/null; then
    java -version
else
    echo "⚠️  Java not found, installing via SDKMAN..."
    curl -s "https://get.sdkman.io" | bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk install java 21.0.1-tem
fi

# Maven cache warm-up (background)
echo "📦 Warming up Maven cache..."
cd /workspace
./mvnw dependency:go-offline -q || true &

# Instalace frontend dependencies
echo "📦 Installing frontend dependencies..."
if [ -d "/workspace-frontend" ]; then
    cd /workspace-frontend
    if [ -f "package.json" ]; then
        npm ci --prefer-offline || npm install
        echo "✅ Frontend dependencies installed"
    fi
fi

echo ""
echo "✅ Dev Container setup complete!"
echo ""
echo "📝 Next steps:"
echo "  1. Run: Ctrl+Shift+P → Tasks: Run Task → 'Dev: Start All Services'"
echo "  2. Wait for services to start (~30s)"
echo "  3. Run: Ctrl+Shift+P → Tasks: Run Task → 'Dev: Check Environment'"
echo "  4. Open: https://core-platform.local/"
echo ""
