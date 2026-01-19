#!/usr/bin/env sh
set -e

if [ -n "${VAULT_ENV_FILE:-}" ] && [ -f "$VAULT_ENV_FILE" ]; then
  set -a
  . "$VAULT_ENV_FILE"
  set +a
fi

if [ "$#" -gt 0 ]; then
  exec "$@"
fi

for candidate in /docker-entrypoint.sh /entrypoint.sh; do
  if [ -x "$candidate" ]; then
    exec "$candidate"
  fi
done

if command -v docker-entrypoint.sh >/dev/null 2>&1; then
  exec docker-entrypoint.sh
fi

if [ -x /cube/bin/server ]; then
  exec node /cube/bin/server
fi

if command -v cubejs-server >/dev/null 2>&1; then
  exec cubejs-server
fi

echo "No command provided and no default entrypoint found." >&2
exit 1
