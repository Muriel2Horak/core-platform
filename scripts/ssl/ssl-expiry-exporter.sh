#!/usr/bin/env sh
set -eu

apk add --no-cache openssl jq >/dev/null

mkdir -p /metrics

while true; do
  /scripts/ssl/export-expiry-metric.sh > /metrics/ssl.prom
  sleep "${METRIC_INTERVAL:-3600}"
done &

busybox httpd -f -p 9108 -h /metrics
