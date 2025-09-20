#!/bin/bash

# 🧪 Staging deployment script
set -e

echo "🚀 Starting Core Platform in STAGING mode..."

# Set environment
export ENVIRONMENT=staging

# Validate required environment variables
required_vars=("DOMAIN_NAME" "SSL_EMAIL" "KEYCLOAK_ADMIN_PASSWORD" "GRAFANA_PASSWORD")
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "❌ Error: Environment variable $var is required for staging deployment"
        echo "Please set it in your CI/CD system or .env.staging file"
        exit 1
    fi
done

echo "🌐 Domain: $DOMAIN_NAME"
echo "📧 SSL Email: $SSL_EMAIL"

# Generate SSL certificates with Let's Encrypt
echo "🔐 Setting up SSL certificates with Let's Encrypt..."
docker-compose --env-file .env.staging --profile ssl run --rm certbot

# Start the stack
echo "🐳 Starting Docker containers..."
docker-compose --env-file .env.staging up -d

echo "⏳ Waiting for services to be ready..."
sleep 30

# Health checks
echo "🔍 Checking service health..."
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

# Test endpoints
echo "🧪 Testing endpoints..."
if curl -k --max-time 10 https://$DOMAIN_NAME/api/actuator/health &>/dev/null; then
    echo "✅ Backend health check passed"
else
    echo "❌ Backend health check failed"
    exit 1
fi

echo ""
echo "🎉 Core Platform STAGING is ready!"
echo ""
echo "📋 Access URLs:"
echo "   🌐 Frontend:  https://$DOMAIN_NAME"
echo "   🔐 API:       https://$DOMAIN_NAME/api"
echo "   🔑 Keycloak:  https://$DOMAIN_NAME/auth"
echo "   📊 Grafana:   https://$DOMAIN_NAME:3001"
echo ""
echo "🔧 Commands:"
echo "   View logs:    docker-compose --env-file .env.staging logs -f [service]"
echo "   Stop:         docker-compose --env-file .env.staging down"
echo "   Update SSL:   docker-compose --env-file .env.staging --profile ssl run --rm certbot renew"