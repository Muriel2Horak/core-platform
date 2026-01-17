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

ZAP_CONFIG_ARGS=()
exclude_index=0
if [[ -n "${DAST_EXCLUDE_REGEX:-}" ]]; then
  IFS=',' read -ra exclude_list <<< "${DAST_EXCLUDE_REGEX}"
  for regex in "${exclude_list[@]}"; do
    trimmed=$(echo "$regex" | xargs)
    [[ -z "$trimmed" ]] && continue
    ZAP_CONFIG_ARGS+=("-config" "globalexcludeurl.url_list.url(${exclude_index}).regex=${trimmed}")
    ZAP_CONFIG_ARGS+=("-config" "globalexcludeurl.url_list.url(${exclude_index}).description=DAST-exclude-${exclude_index}")
    ZAP_CONFIG_ARGS+=("-config" "globalexcludeurl.url_list.url(${exclude_index}).enabled=true")
    exclude_index=$((exclude_index + 1))
  done
fi

if [[ -n "${DAST_AUTH_HEADER:-}" ]]; then
  ZAP_CONFIG_ARGS+=("-config" "replacer.full_list(0).description=DAST-Auth")
  ZAP_CONFIG_ARGS+=("-config" "replacer.full_list(0).enabled=true")
  ZAP_CONFIG_ARGS+=("-config" "replacer.full_list(0).matchtype=REQ_HEADER")
  ZAP_CONFIG_ARGS+=("-config" "replacer.full_list(0).matchstr=Authorization")
  ZAP_CONFIG_ARGS+=("-config" "replacer.full_list(0).regex=false")
  ZAP_CONFIG_ARGS+=("-config" "replacer.full_list(0).replacement=${DAST_AUTH_HEADER}")
fi

ZAP_CONFIG_STRING=""
if [[ ${#ZAP_CONFIG_ARGS[@]} -gt 0 ]]; then
  ZAP_CONFIG_STRING="$(printf '%s ' "${ZAP_CONFIG_ARGS[@]}")"
fi

zap_args=(zap-baseline.py -t "$TARGET_URL" -r zap_report.html -J zap_report.json)
if [[ -n "$ZAP_CONFIG_STRING" ]]; then
  zap_args+=(-z "$ZAP_CONFIG_STRING")
fi

docker run --rm -t owasp/zap2docker-stable "${zap_args[@]}"

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
