#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  check-ai-risky-patterns.sh [--base <sha>] [--head <sha>] [--patterns <path>]
EOF
}

BASE=""
HEAD=""
PATTERNS="scripts/ci/ai-risky-patterns.txt"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base)
      BASE="$2"
      shift 2
      ;;
    --head)
      HEAD="$2"
      shift 2
      ;;
    --patterns)
      PATTERNS="$2"
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

if [[ -z "$BASE" || -z "$HEAD" ]]; then
  echo "Base/head not provided; using last commit." >&2
  BASE="HEAD~1"
  HEAD="HEAD"
fi

if [[ ! -f "$PATTERNS" ]]; then
  echo "Patterns file not found: $PATTERNS" >&2
  exit 2
fi

added_lines=$(git diff "$BASE" "$HEAD" --unified=0 --no-color | grep -E '^\+[^+]' || true)

if [[ -z "$added_lines" ]]; then
  echo "No added lines to scan."
  exit 0
fi

matches=()
if command -v rg >/dev/null 2>&1; then
  search_cmd="rg -n --regexp"
else
  search_cmd=""
fi
while IFS= read -r pattern; do
  [[ -z "$pattern" ]] && continue
  if [[ -n "$search_cmd" ]]; then
    if echo "$added_lines" | $search_cmd "$pattern" >/dev/null; then
      matches+=("$pattern")
    fi
  fi
done < "$PATTERNS"

if [[ -z "$search_cmd" ]]; then
  tmpfile=$(mktemp)
  echo "$added_lines" > "$tmpfile"
  python3 - <<'PY' "$tmpfile" "$PATTERNS"
import re
import sys

added_path = sys.argv[1]
patterns_path = sys.argv[2]

with open(added_path, "r", encoding="utf-8") as handle:
    lines = handle.read().splitlines()

with open(patterns_path, "r", encoding="utf-8") as handle:
    patterns = [line.strip() for line in handle if line.strip()]

matches = []
for pattern in patterns:
    regex = re.compile(pattern)
    for idx, line in enumerate(lines, start=1):
        if regex.search(line):
            matches.append((pattern, idx, line))

if matches:
    print("Risky patterns detected:")
    for pattern, idx, line in matches:
        print(f" - {pattern}")
        print(f"{idx}:{line}")
    sys.exit(1)
print("No risky patterns detected.")
PY
  status=$?
  rm -f "$tmpfile"
  exit "$status"
fi

if [[ ${#matches[@]} -gt 0 ]]; then
  echo "Risky patterns detected:"
  for pattern in "${matches[@]}"; do
    echo " - $pattern"
    echo "$added_lines" | $search_cmd "$pattern" || true
  done
  exit 1
fi

echo "No risky patterns detected."
