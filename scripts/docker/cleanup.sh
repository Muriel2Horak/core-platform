#!/usr/bin/env bash
set -euo pipefail

# 🎨 Barvy pro lepší čitelnost
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

cd "${REPO_ROOT}/docker"

echo -e "${RED}⚠️  POZOR: Tento příkaz smaže VŠECHNA DATA včetně:${NC}"
echo -e "${RED}   - Keycloak databázi (uživatelé, role, atributy)${NC}"
echo -e "${RED}   - PostgreSQL data${NC}"
echo -e "${RED}   - MinIO soubory (profilové fotky, dokumenty)${NC}"
echo -e "${RED}   - Grafana dashboardy a nastavení${NC}"
echo ""
echo -e "${YELLOW}Pokud chcete jen restartovat služby BEZ ztráty dat, použijte:${NC}"
echo -e "${CYAN}  scripts/docker/down.sh && scripts/docker/up.sh${NC}"
echo ""
read -p "Opravdu chcete smazat VŠECHNA DATA? (zadejte 'YES' pro potvrzení): " confirm

if [ "$confirm" != "YES" ]; then
    echo -e "${GREEN}Operace zrušena.${NC}"
    exit 0
fi

echo -e "${RED}Mažu všechna data...${NC}"
docker compose down -v --remove-orphans

echo -e "${YELLOW}Mažu nepoužívané Docker objekty...${NC}"
docker system prune -f
docker volume prune -f

echo -e "${GREEN}✅ Úplné vyčištění dokončeno.${NC}"