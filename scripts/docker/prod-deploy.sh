#!/bin/bash

# 🚀 Production deployment script
set -e

echo "🚀 Starting Core Platform in PRODUCTION mode..."

# Set environment
export ENVIRONMENT=production

# Validate required environment variables
required_vars=("DOMAIN_NAME" "SSL_EMAIL" "KEYCLOAK_ADMIN_PASSWORD" "GRAFANA_PASSWORD" "KEYCLOAK_CLIENT_SECRET" "APP_SECRET_KEY" "JWT_SIGNING_KEY")
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ Error: Environment variable $var is required for production deployment"
        echo "Please set it in your CI/CD system or secrets management"
        exit 1
    fi
done

echo "🌐 Domain: $DOMAIN_NAME"
echo "📧 SSL Email: $SSL_EMAIL"

# Backup database if exists
if docker-compose ps | grep -q "db.*Up"; then
    echo "💾 Creating database backup..."
    docker-compose exec db pg_dump -U core core > backup_$(date +%Y%m%d_%H%M%S).sql
    echo "✅ Database backup created"
fi

# Pull latest images
echo "📦 Pulling latest Docker images..."
docker-compose --env-file .env.production pull

# Generate SSL certificates with Let's Encrypt
echo "🔐 Setting up SSL certificates with Let's Encrypt..."
docker-compose --env-file .env.production --profile ssl run --rm certbot

# Start the stack with zero-downtime deployment
echo "🐳 Starting Docker containers..."
docker-compose --env-file .env.production up -d --remove-orphans

echo "⏳ Waiting for services to be ready..."
sleep 45

# Health checks
echo "🔍 Performing health checks..."
services=("nginx" "frontend" "backend" "keycloak" "db")
for service in "${services[@]}"; do
    if docker-compose ps | grep -q "$service.*Up"; then
        echo "✅ $service is running"
    else
        echo "❌ $service is not running"
        docker-compose logs $service
        exit 1
    fi
done

# Extended health checks
echo "🧪 Testing endpoints..."
endpoints=(
    "https://$DOMAIN_NAME"
    "https://$DOMAIN_NAME/api/actuator/health"
    "https://$DOMAIN_NAME/auth/realms/core-platform"
)

for endpoint in "${endpoints[@]}"; do
    if curl -k --max-time 15 "$endpoint" &>/dev/null; then
        echo "✅ $endpoint is responding"
    else
        echo "❌ $endpoint is not responding"
        exit 1
    fi
done

# Cleanup old images
echo "🧹 Cleaning up old Docker images..."
docker image prune -f

echo ""
echo "🎉 Core Platform PRODUCTION is ready!"
echo ""
echo "📋 Access URLs:"
echo "   🌐 Frontend:  https://$DOMAIN_NAME"
echo "   🔐 API:       https://$DOMAIN_NAME/api"
echo "   🔑 Keycloak:  https://$DOMAIN_NAME/auth"
echo "   📊 Grafana:   https://$DOMAIN_NAME:3001"
echo ""
echo "🔧 Commands:"
echo "   View logs:    docker-compose --env-file .env.production logs -f [service]"
echo "   Stop:         docker-compose --env-file .env.production down"
echo "   Update SSL:   docker-compose --env-file .env.production --profile ssl run --rm certbot renew"
echo "   Backup DB:    docker-compose exec db pg_dump -U core core > backup.sql"