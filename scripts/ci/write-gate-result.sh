#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  write-gate-result.sh --name <gate> --status <pass|fail|warn|skip> [--details <text>] [--output <path>]
EOF
}

NAME=""
STATUS=""
DETAILS=""
OUTPUT=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --name)
      NAME="$2"
      shift 2
      ;;
    --status)
      STATUS="$2"
      shift 2
      ;;
    --details)
      DETAILS="$2"
      shift 2
      ;;
    --output)
      OUTPUT="$2"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ -z "$NAME" || -z "$STATUS" ]]; then
  echo "Missing required arguments." >&2
  usage
  exit 1
fi

if [[ -z "$OUTPUT" ]]; then
  OUTPUT="gate-results/${NAME}.json"
fi

mkdir -p "$(dirname "$OUTPUT")"

NAME="$NAME" STATUS="$STATUS" DETAILS="$DETAILS" OUTPUT="$OUTPUT" python3 - <<PY
import json
from datetime import datetime, timezone
import os

payload = {
    "name": os.environ["NAME"],
    "status": os.environ["STATUS"],
    "details": os.environ.get("DETAILS") or "",
    "timestamp": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
}

output_path = os.environ["OUTPUT"]
with open(output_path, "w", encoding="utf-8") as handle:
    json.dump(payload, handle, indent=2, sort_keys=True)
    handle.write("\n")
PY

echo "Wrote gate result to $OUTPUT"
