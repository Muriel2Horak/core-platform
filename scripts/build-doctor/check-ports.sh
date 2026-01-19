#!/usr/bin/env bash
set -euo pipefail

REQUIRED_PORTS=(80 443 8080 5432 6379 9092)
BLOCKED_PORTS=()

check_with_lsof() {
  local port="$1"
  lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
}

check_with_ss() {
  local port="$1"
  ss -lnt "( sport = :$port )" | tail -n +2 | grep -q "$port"
}

check_with_netstat() {
  local port="$1"
  netstat -an 2>/dev/null | grep -E "LISTEN" | grep -q "\.$port "
}

for port in "${REQUIRED_PORTS[@]}"; do
  if command -v lsof >/dev/null 2>&1; then
    if check_with_lsof "$port"; then
      BLOCKED_PORTS+=("$port")
    fi
  elif command -v ss >/dev/null 2>&1; then
    if check_with_ss "$port"; then
      BLOCKED_PORTS+=("$port")
    fi
  elif command -v netstat >/dev/null 2>&1; then
    if check_with_netstat "$port"; then
      BLOCKED_PORTS+=("$port")
    fi
  else
    echo "❌ No port inspection tool found (lsof/ss/netstat)"
    echo "💡 Install lsof or iproute2"
    exit 1
  fi
done

if [[ ${#BLOCKED_PORTS[@]} -eq 0 ]]; then
  exit 0
fi

echo "❌ Ports already in use: ${BLOCKED_PORTS[*]}"
echo "💡 Free ports with: sudo lsof -ti:<port> | xargs kill -9"
exit 1
