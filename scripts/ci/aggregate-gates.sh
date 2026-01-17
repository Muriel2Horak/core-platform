#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  aggregate-gates.sh --event <pr|nightly|release> [--matrix <path>] [--results <dir>] [--output <dir>]
EOF
}

EVENT=""
MATRIX="scripts/ci/gate-matrix.json"
RESULTS_DIR="gate-results"
OUTPUT_DIR="gate-summary"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --event)
      EVENT="$2"
      shift 2
      ;;
    --matrix)
      MATRIX="$2"
      shift 2
      ;;
    --results)
      RESULTS_DIR="$2"
      shift 2
      ;;
    --output)
      OUTPUT_DIR="$2"
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

if [[ -z "$EVENT" ]]; then
  echo "Missing --event argument." >&2
  usage
  exit 1
fi

mkdir -p "$OUTPUT_DIR"

EVENT="$EVENT" MATRIX="$MATRIX" RESULTS_DIR="$RESULTS_DIR" OUTPUT_DIR="$OUTPUT_DIR" python3 - <<'PY'
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

event = os.environ["EVENT"]
matrix_path = Path(os.environ["MATRIX"])
results_dir = Path(os.environ["RESULTS_DIR"])
output_dir = Path(os.environ["OUTPUT_DIR"])

if not matrix_path.exists():
    print(f"Matrix file not found: {matrix_path}", file=sys.stderr)
    sys.exit(2)

with matrix_path.open("r", encoding="utf-8") as handle:
    matrix = json.load(handle)

required = matrix.get(event)
if not required:
    print(f"No gate matrix defined for event '{event}'", file=sys.stderr)
    sys.exit(2)

results = {}
missing = []
for gate in required:
    result_file = results_dir / f"{gate}.json"
    if not result_file.exists():
        missing.append(gate)
        continue
    with result_file.open("r", encoding="utf-8") as handle:
        results[gate] = json.load(handle)

failed = []
warnings = []
for gate in required:
    payload = results.get(gate)
    if not payload:
        continue
    status = payload.get("status")
    if status == "warn":
        warnings.append(gate)
        continue
    if status != "pass":
        failed.append(gate)

overall = "pass" if not missing and not failed else "fail"

summary = {
    "event": event,
    "overall": overall,
    "required_gates": required,
    "missing_gates": missing,
    "failed_gates": failed,
    "warning_gates": warnings,
    "results": results,
    "generated_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
}

summary_path = output_dir / "summary.json"
with summary_path.open("w", encoding="utf-8") as handle:
    json.dump(summary, handle, indent=2, sort_keys=True)
    handle.write("\n")

lines = []
lines.append(f"# Quality Gates Summary ({event})")
lines.append("")
lines.append(f"Overall: **{overall.upper()}**")
lines.append("")
lines.append("## Required Gates")
for gate in required:
    payload = results.get(gate)
    status = payload.get("status") if payload else "missing"
    lines.append(f"- {gate}: {status}")
if missing:
    lines.append("")
    lines.append("## Missing Results")
    for gate in missing:
        lines.append(f"- {gate}")
if failed:
    lines.append("")
    lines.append("## Failed Gates")
    for gate in failed:
        lines.append(f"- {gate}")
if warnings:
    lines.append("")
    lines.append("## Warning Gates")
    for gate in warnings:
        lines.append(f"- {gate}")

summary_md = "\n".join(lines) + "\n"
summary_md_path = output_dir / "summary.md"
summary_md_path.write_text(summary_md, encoding="utf-8")

github_summary = os.environ.get("GITHUB_STEP_SUMMARY")
if github_summary:
    Path(github_summary).write_text(summary_md, encoding="utf-8")

if overall != "pass":
    sys.exit(1)
PY

echo "Quality gate summary written to ${OUTPUT_DIR}/summary.json"
