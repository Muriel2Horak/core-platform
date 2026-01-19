#!/bin/bash
set -e

if [ -f /run/secrets/backend.env ]; then
  set -a
  . /run/secrets/backend.env
  set +a
  echo "🔐 Loaded Vault secrets from /run/secrets/backend.env"
fi

echo "🚀 Starting Spring Boot backend..."
echo "📋 SSL certificate was imported during Docker build"

# Start the Java application
exec java $JAVA_OPTS -jar /app/app.jar
