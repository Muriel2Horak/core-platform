#!/bin/bash

# 🧹 Cleanup script pro Core Platform
set -e

echo "🧹 Cleaning up Core Platform environment..."

# Stop všechny running containery
echo "🛑 Stopping all running containers..."
if docker-compose ps | grep -q "Up\|running"; then
    docker-compose down --remove-orphans --volumes
    echo "✅ Docker compose stack stopped"
fi

# Vyčisti všechny core- containery (i ty co zůstaly)
echo "🗑️  Removing any remaining core- containers..."
docker ps -aq --filter "name=core-" | xargs -r docker rm -f 2>/dev/null || true
echo "✅ Remaining containers removed"

# Vyčisti nepoužívané images
echo "🧹 Cleaning up unused Docker images..."
docker image prune -f
echo "✅ Unused images removed"

# Vyčisti nepoužívané networks
echo "🌐 Cleaning up unused networks..."
docker network prune -f
echo "✅ Unused networks removed"

# Volitelně - vyčisti volumes (opatrně!)
read -p "🗄️  Do you want to remove database volumes? This will DELETE ALL DATA! (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🚨 Removing all volumes..."
    docker volume prune -f
    echo "✅ All volumes removed"
else
    echo "📦 Volumes preserved"
fi

# Vyčisti SSL certifikáty pokud existují
if [ -d "ssl" ]; then
    read -p "🔐 Remove SSL certificates? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        rm -rf ssl/
        echo "✅ SSL certificates removed"
    fi
fi

echo ""
echo "🎉 Cleanup completed!"
echo ""
echo "🚀 To start fresh environment:"
echo "   Development:  ./scripts/docker/dev-start-ssl.sh"
echo "   Staging:      ./scripts/docker/staging-deploy.sh"
echo "   Production:   ./scripts/docker/prod-deploy.sh"