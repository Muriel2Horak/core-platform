#!/bin/sh
set -e

# Generate nginx.conf from template with DOMAIN substitution
echo "🔧 Generating nginx.conf from template..."
envsubst '$DOMAIN' < /etc/nginx/nginx-ssl.conf.template > /etc/nginx/nginx.conf

echo "✅ nginx.conf generated with DOMAIN=${DOMAIN}"

# Execute original nginx entrypoint
exec /docker-entrypoint.sh "$@"
