#!/bin/bash

##
# 🧹 E2E Test Environment Teardown
# 
# Stops Docker Compose stack after E2E tests
##

set -e

echo "🧹 Stopping E2E test environment..."

# Navigate to project root
cd "$(dirname "$0")/../.."

# Stop streaming stack
echo "📦 Stopping Docker Compose..."
docker compose --profile streaming down

echo "✅ E2E environment cleaned up"
