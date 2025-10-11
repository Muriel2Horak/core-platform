#!/bin/bash

##
# 🚀 E2E Test Environment Setup
# 
# Starts Docker Compose stack with streaming profile for E2E tests
##

set -e

echo "🚀 Starting E2E test environment..."

# Check if docker compose is available
if ! command -v docker &> /dev/null; then
    echo "❌ Docker is not installed"
    exit 1
fi

# Navigate to project root
cd "$(dirname "$0")/../.."

# Start streaming stack
echo "📦 Starting Docker Compose with streaming profile..."
docker compose --profile streaming up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."

# Wait for backend
MAX_WAIT=60
ELAPSED=0
until curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; do
    if [ $ELAPSED -ge $MAX_WAIT ]; then
        echo "❌ Backend did not start in time"
        docker compose --profile streaming logs backend
        exit 1
    fi
    echo "  Waiting for backend... ($ELAPSED/$MAX_WAIT)"
    sleep 2
    ELAPSED=$((ELAPSED + 2))
done

echo "✅ Backend is ready"

# Wait for Kafka
ELAPSED=0
until docker compose --profile streaming exec -T kafka kafka-topics.sh --bootstrap-server localhost:9092 --list > /dev/null 2>&1; do
    if [ $ELAPSED -ge $MAX_WAIT ]; then
        echo "❌ Kafka did not start in time"
        docker compose --profile streaming logs kafka
        exit 1
    fi
    echo "  Waiting for Kafka... ($ELAPSED/$MAX_WAIT)"
    sleep 2
    ELAPSED=$((ELAPSED + 2))
done

echo "✅ Kafka is ready"

# Wait for Grafana
ELAPSED=0
until curl -s http://localhost:3001/api/health > /dev/null 2>&1; do
    if [ $ELAPSED -ge $MAX_WAIT ]; then
        echo "❌ Grafana did not start in time"
        docker compose --profile streaming logs grafana
        exit 1
    fi
    echo "  Waiting for Grafana... ($ELAPSED/$MAX_WAIT)"
    sleep 2
    ELAPSED=$((ELAPSED + 2))
done

echo "✅ Grafana is ready"

echo ""
echo "🎉 E2E environment is ready!"
echo ""
echo "Services:"
echo "  Backend:    http://localhost:8080"
echo "  Frontend:   https://localhost:443"
echo "  Grafana:    http://localhost:3001"
echo "  Prometheus: http://localhost:9090"
echo ""
echo "Run E2E tests:"
echo "  cd frontend && npm run test:e2e"
echo ""
