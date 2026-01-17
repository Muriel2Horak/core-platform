#!/usr/bin/env bash
set -euo pipefail

SUMMARY_PATH="${1:-gate-summary/summary.json}"
OUTPUT_DIR="${2:-compliance-evidence}"

if [[ ! -f "$SUMMARY_PATH" ]]; then
  echo "Summary not found: $SUMMARY_PATH" >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"

SUMMARY_PATH="$SUMMARY_PATH" OUTPUT_DIR="$OUTPUT_DIR" python3 - <<'PY'
import json
import os
from datetime import datetime, timezone
from pathlib import Path

summary_path = Path(os.environ["SUMMARY_PATH"])
output_dir = Path(os.environ["OUTPUT_DIR"])

with summary_path.open("r", encoding="utf-8") as handle:
    summary = json.load(handle)

gate_mapping = {
    "unit-tests": "EPIC-000: quality baseline",
    "sast": "EPIC-000: secure coding",
    "sca": "EPIC-000: dependency security",
    "secret-scan": "EPIC-000: secret handling",
    "iac-lint": "EPIC-000: infra hardening",
    "dast": "EPIC-000: OWASP top 10",
    "security-regression": "EPIC-000: tenant isolation & RBAC",
    "container-scan": "EPIC-000: container security",
    "ai-guardrails": "EPIC-000: AI code hygiene",
}

evidence = {
    "generated_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    "event": summary.get("event"),
    "overall": summary.get("overall"),
    "gates": [],
}

for gate, payload in summary.get("results", {}).items():
    evidence["gates"].append({
        "name": gate,
        "status": payload.get("status"),
        "details": payload.get("details"),
        "baseline": gate_mapping.get(gate, "EPIC-000: unspecified"),
    })

output_path = output_dir / "compliance-summary.json"
with output_path.open("w", encoding="utf-8") as handle:
    json.dump(evidence, handle, indent=2, sort_keys=True)
    handle.write("\n")
PY

if [[ -n "${LOKI_URL:-}" ]]; then
  payload=$(OUTPUT_DIR="$OUTPUT_DIR" python3 - <<'PY'
import json
import os
import time
from pathlib import Path

output_dir = Path(os.environ["OUTPUT_DIR"])
summary = (output_dir / "compliance-summary.json").read_text(encoding="utf-8")
ts_ns = str(int(time.time() * 1_000_000_000))
payload = {
    "streams": [
        {
            "stream": {"service": "quality-gates", "source": "ci"},
            "values": [[ts_ns, summary]],
        }
    ]
}
print(json.dumps(payload))
PY
)
  curl -s -X POST "${LOKI_URL}/loki/api/v1/push" \
    -H "Content-Type: application/json" \
    --data-binary "$payload" || true
fi
