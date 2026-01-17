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
while IFS= read -r pattern; do
  [[ -z "$pattern" ]] && continue
  if echo "$added_lines" | rg -n --regexp "$pattern" >/dev/null; then
    matches+=("$pattern")
  fi
done < "$PATTERNS"

if [[ ${#matches[@]} -gt 0 ]]; then
  echo "Risky patterns detected:"
  for pattern in "${matches[@]}"; do
    echo " - $pattern"
    echo "$added_lines" | rg -n --regexp "$pattern" || true
  done
  exit 1
fi

echo "No risky patterns detected."
