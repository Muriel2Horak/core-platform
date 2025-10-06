#!/bin/bash
set -e

echo "🚀 Starting Spring Boot backend..."
echo "📋 SSL certificate was imported during Docker build"

# Start the Java application
exec java $JAVA_OPTS -jar /app/app.jar