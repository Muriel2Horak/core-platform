#!/usr/bin/env bash
set -euo pipefail

# Install and enable the Grafana Loki Docker logging plugin if missing/disabled
if ! docker plugin ls --format '{{.Name}}' | grep -qx 'loki'; then
  echo 'Installing Loki Docker logging plugin...'
  docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions
else
  echo 'Loki Docker logging plugin already installed.'
fi

if ! docker plugin inspect loki --format '{{.Enabled}}' | grep -qi true; then
  echo 'Enabling Loki plugin...'
  docker plugin enable loki
fi

echo 'Loki plugin ready.'
