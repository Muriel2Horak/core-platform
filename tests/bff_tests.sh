#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

required_files=(
  "bff/package.json"
  "bff/tsconfig.json"
  "bff/Dockerfile"
  "bff/src/index.ts"
  "bff/src/schema.ts"
  "bff/src/context.ts"
  "bff/src/resolvers/index.ts"
  "bff/src/clients/backend.ts"
  "bff/src/dataloaders/user.ts"
  "docker/docker-compose.yml"
  "docker/nginx/nginx-ssl.conf.template"
)

for file in "${required_files[@]}"; do
  if [[ ! -f "$file" ]]; then
    echo "❌ Missing file: $file"
    exit 1
  fi
done

if ! rg -n "^\s*bff:" docker/docker-compose.yml >/dev/null; then
  echo "❌ docker-compose.yml missing bff service"
  exit 1
fi

if ! rg -n "BFF_BACKEND_URL" docker/docker-compose.yml >/dev/null; then
  echo "❌ bff service missing BFF_BACKEND_URL"
  exit 1
fi

if ! rg -n "location /graphql" docker/nginx/nginx-ssl.conf.template >/dev/null; then
  echo "❌ nginx config missing /graphql proxy"
  exit 1
fi

if ! rg -n "Promise\.all" bff/src/resolvers/index.ts >/dev/null; then
  echo "❌ resolvers should use Promise.all for parallel calls"
  exit 1
fi

if ! rg -n "type Query" bff/src/schema.ts >/dev/null; then
  echo "❌ schema missing Query type"
  exit 1
fi

jq -e '.dependencies["apollo-server-express"]' bff/package.json >/dev/null
jq -e '.dependencies["graphql"]' bff/package.json >/dev/null
jq -e '.dependencies["dataloader"]' bff/package.json >/dev/null
jq -e '.dependencies["ioredis"]' bff/package.json >/dev/null
jq -e '.dependencies["axios"]' bff/package.json >/dev/null
jq -e '.dependencies["opossum"]' bff/package.json >/dev/null
jq -e '.dependencies["jsonwebtoken"]' bff/package.json >/dev/null

echo "✅ BFF wiring checks passed"
