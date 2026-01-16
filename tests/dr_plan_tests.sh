#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

required_files=(
  "docs/disaster-recovery/DR_PLAN.md"
  "docs/disaster-recovery/DR_DRILLS.md"
  "scripts/dr/failover-to-secondary.sh"
  "scripts/dr/restore-from-offsite.sh"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "❌ Missing file: $file"
    exit 1
  fi
done

rg -n "RTO" docs/disaster-recovery/DR_PLAN.md >/dev/null
rg -n "RPO" docs/disaster-recovery/DR_PLAN.md >/dev/null

rg -n "Scenario 1: Database Corruption" docs/disaster-recovery/DR_PLAN.md >/dev/null
rg -n "Scenario 2: Complete Server Failure" docs/disaster-recovery/DR_PLAN.md >/dev/null
rg -n "Scenario 3: Ransomware Attack" docs/disaster-recovery/DR_PLAN.md >/dev/null
rg -n "Scenario 4: AWS Region Outage" docs/disaster-recovery/DR_PLAN.md >/dev/null

rg -n "Failover script" docs/disaster-recovery/DR_PLAN.md >/dev/null
rg -n "DR Drills" docs/disaster-recovery/DR_PLAN.md >/dev/null

rg -n -- "--dry-run" scripts/dr/failover-to-secondary.sh >/dev/null
rg -n "HOSTED_ZONE_ID" scripts/dr/failover-to-secondary.sh >/dev/null

rg -n "OFFSITE_BUCKET" scripts/dr/restore-from-offsite.sh >/dev/null

echo "✅ DR plan checks passed"
