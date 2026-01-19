#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

rg -n -F "metrics/{metric}" backend/src/main/java/cz/muriel/core/monitoring/bff/MonitoringBffController.java >/dev/null
rg -n "monitoring.prometheus.base-url" backend/src/main/resources/application.properties >/dev/null
rg -n "MonitoringLiveWebSocketHandler" backend/src/main/java/cz/muriel/core/monitoring/websocket/MonitoringWebSocketConfig.java >/dev/null

rg -n "MetricsDashboard" frontend/src/pages/Admin/MonitoringPage.tsx >/dev/null
rg -n "LiveMetricsWidget" frontend/src/components/Monitoring/MetricsDashboard.tsx >/dev/null

if [[ ! -f frontend/src/components/Monitoring/MetricsDashboard.tsx ]]; then
  echo "❌ missing MetricsDashboard component"
  exit 1
fi

if [[ ! -f frontend/src/components/Monitoring/LiveMetricsWidget.tsx ]]; then
  echo "❌ missing LiveMetricsWidget component"
  exit 1
fi

echo "✅ EPIC-003 completion checks passed"
