#!/usr/bin/env bash
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  echo "❌ Docker CLI not found"
  echo "💡 Install Docker Desktop or Docker Engine"
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "❌ Docker daemon is not running"
  echo "💡 Start Docker Desktop and retry"
  exit 1
fi
