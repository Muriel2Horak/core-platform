#!/usr/bin/env bash
set -euo pipefail

files_found=0
nginx_hosts=(
  --add-host backend:127.0.0.1
  --add-host bff:127.0.0.1
  --add-host frontend:127.0.0.1
  --add-host grafana:127.0.0.1
  --add-host keycloak:127.0.0.1
  --add-host loki:127.0.0.1
  --add-host prometheus:127.0.0.1
)
tmp_cert="$PWD/.tmp-nginx-cert.pem"
tmp_key="$PWD/.tmp-nginx-key.pem"
cleanup_cert=0
if [ ! -f "$tmp_cert" ] || [ ! -f "$tmp_key" ]; then
  cleanup_cert=1
  openssl req -x509 -nodes -newkey rsa:2048 -days 1 -subj "/CN=localhost" \
    -keyout "$tmp_key" -out "$tmp_cert" >/dev/null 2>&1
fi
while IFS= read -r file; do
  files_found=1
  config_path="/work/${file#./}"
  echo "Linting nginx config: ${file}"
  if grep -q "^\s*server\s*{" "$file"; then
    docker run --rm "${nginx_hosts[@]}" -v "$PWD:/work" \
      -v "$tmp_cert:/etc/nginx/ssl/cert.pem:ro" \
      -v "$tmp_key:/etc/nginx/ssl/key.pem:ro" \
      -e NGINX_INCLUDE="/work/${file#./}" \
      nginx:1.25-alpine sh -c 'cat > /tmp/nginx.conf <<EOF
events {}
http {
  include ${NGINX_INCLUDE};
}
EOF
nginx -t -c /tmp/nginx.conf'
  else
    docker run --rm "${nginx_hosts[@]}" -v "$PWD:/work" -w /work \
      -v "$tmp_cert:/etc/nginx/ssl/cert.pem:ro" \
      -v "$tmp_key:/etc/nginx/ssl/key.pem:ro" \
      nginx:1.25-alpine nginx -t -c "$config_path"
  fi
done < <(find . -type f -name 'nginx*.conf' | grep -v 'node_modules' || true)

if [ "$files_found" -eq 0 ]; then
  echo "No nginx config files found."
  exit 0
fi

if [ "$cleanup_cert" -eq 1 ]; then
  rm -f "$tmp_cert" "$tmp_key"
fi
