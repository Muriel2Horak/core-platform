#!/usr/bin/env bash
set -euo pipefail

mapfile -t files < <(find . -type f -name 'nginx*.conf' | grep -v 'node_modules' || true)

if [ ${#files[@]} -eq 0 ]; then
  echo "No nginx config files found."
  exit 0
fi

for file in "${files[@]}"; do
  config_path="/work/${file#./}"
  echo "Linting nginx config: ${file}"
  docker run --rm -v "$PWD:/work" -w /work nginx:1.25-alpine nginx -t -c "$config_path"
done
