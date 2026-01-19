#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

if [[ ! -f .env ]]; then
  echo "❌ .env file not found"
  echo "💡 Copy from template: cp .env.template .env"
  exit 1
fi

TEMPLATE_FILES=$(rg --files -g '*.template*' -g '!*.bak' -g '!*.old' -g '!.env.template*' -g '!tests/.env.template')

if [[ -z "$TEMPLATE_FILES" ]]; then
  echo "❌ No template files found"
  exit 1
fi

# Disallow non-envsubst fallback syntax (${VAR:default})
INVALID_SYNTAX=$(rg -n '\$\{[A-Z0-9_]+:[^}-]' $TEMPLATE_FILES || true)
if [[ -n "$INVALID_SYNTAX" ]]; then
  echo "❌ Invalid template syntax detected (use \${VAR:-default} instead of \${VAR:default}):"
  echo "$INVALID_SYNTAX"
  exit 1
fi

TEMPLATE_VARS=$(rg --no-filename -o '\$\{[A-Z0-9_]+(?::-[^}]*)?\}' $TEMPLATE_FILES | \
  sed -E 's/^\$\{([A-Z0-9_]+).*/\1/' | sort -u)

set -a
source .env
set +a

MISSING_VARS=()
for var in $TEMPLATE_VARS; do
  if [[ -z "${!var:-}" ]]; then
    MISSING_VARS+=("$var")
  fi
done

if [[ ${#MISSING_VARS[@]} -gt 0 ]]; then
  echo "❌ Missing template variables in .env:"
  for var in "${MISSING_VARS[@]}"; do
    echo "  - $var"
  done
  exit 1
fi

ENV_KEYS=$(awk -F= '/^[A-Z0-9_]+=/ {print $1}' .env | sort -u)
UNUSED_VARS=()
for key in $ENV_KEYS; do
  if ! echo "$TEMPLATE_VARS" | grep -qx "$key"; then
    UNUSED_VARS+=("$key")
  fi
done

echo "✅ All template variables are defined in .env"

if [[ ${#UNUSED_VARS[@]} -gt 0 ]]; then
  echo "⚠️  .env variables not used in templates:"
  for key in "${UNUSED_VARS[@]}"; do
    echo "  - $key"
  done
fi
