#!/bin/sh
set -e

echo "🚀 Starting Nginx with envsubst template rendering..."
echo "📝 DOMAIN: ${DOMAIN}"

# Render template with envsubst
envsubst '$DOMAIN' < /etc/nginx/templates/nginx-ssl.conf.template > /etc/nginx/nginx.conf

echo "📋 Generated nginx.conf (first 40 lines):"
head -40 /etc/nginx/nginx.conf

echo "🌐 Starting Nginx..."
exec nginx -g 'daemon off;'