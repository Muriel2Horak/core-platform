#!/usr/bin/env bash
set -euo pipefail

if [[ "${RUN_DAST:-0}" != "1" ]]; then
  echo "DAST skipped (RUN_DAST=1 to enable)."
  exit 0
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker not available for DAST."
  exit 1
fi

TARGET_URL="${DAST_TARGET_URL:-https://admin.core-platform.local}"

docker run --rm -t owasp/zap2docker-stable zap-baseline.py \
  -t "$TARGET_URL" \
  -r zap_report.html \
  -J zap_report.json

if [[ -f zap_report.json ]]; then
  python3 - <<'PY'
import json
import sys

with open("zap_report.json", "r", encoding="utf-8") as handle:
    data = json.load(handle)

alerts = []
for site in data.get("site", []):
    alerts.extend(site.get("alerts", []))

def is_medium_or_higher(alert):
    risk = alert.get("riskcode")
    if risk is None:
        risk = alert.get("risk", "")
    if isinstance(risk, int):
        return risk >= 2
    if isinstance(risk, str):
        return risk.lower() in {"medium", "high"}
    return False

count = sum(1 for alert in alerts if is_medium_or_higher(alert))
if count > 0:
    print(f"DAST findings medium/high: {count}")
    sys.exit(2)
PY
fi
