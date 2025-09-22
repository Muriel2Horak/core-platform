#!/usr/bin/env bash
set -euo pipefail

# 🔒 SSL Development Startup Script
# Automaticky konfiguruje HTTPS prostředí s lokální doménou core-platform.local

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
DOCKER_DIR="${REPO_ROOT}/docker"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }

echo -e "${CYAN}"
echo "🔒 =================================="
echo "   Core Platform - SSL Setup"
echo "   https://core-platform.local"
echo "==================================="
echo -e "${NC}"

# 1. Kontrola požadavků
print_info "Kontroluji požadavky..."

if ! command -v docker &> /dev/null; then
    print_error "Docker není nainstalovaný!"
    exit 1
fi

if ! command -v openssl &> /dev/null; then
    print_error "OpenSSL není nainstalovaný!"
    exit 1
fi

# 2. Přidání lokální domény do /etc/hosts
print_info "Konfiguruji lokální doménu..."

if ! grep -q "core-platform.local" /etc/hosts; then
    print_warning "Přidávám core-platform.local do /etc/hosts (vyžaduje sudo)..."
    echo "127.0.0.1    core-platform.local" | sudo tee -a /etc/hosts > /dev/null
    print_success "Lokální doména přidána"
else
    print_success "Lokální doména již existuje"
fi

# 3. Kontrola SSL certifikátů
print_info "Kontroluji SSL certifikáty..."

SSL_DIR="${DOCKER_DIR}/ssl"
CERT_FILE="${SSL_DIR}/cert.pem"
KEY_FILE="${SSL_DIR}/key.pem"

if [[ -f "$CERT_FILE" && -f "$KEY_FILE" ]]; then
    # Kontrola, zda certifikát obsahuje správnou doménu
    if openssl x509 -in "$CERT_FILE" -text -noout | grep -q "core-platform.local"; then
        print_success "SSL certifikáty jsou platné pro core-platform.local"
    else
        print_warning "Existující certifikáty neobsahují core-platform.local, generuji nové..."
        rm -f "$CERT_FILE" "$KEY_FILE"
    fi
fi

if [[ ! -f "$CERT_FILE" || ! -f "$KEY_FILE" ]]; then
    print_info "Generuji SSL certifikáty pro core-platform.local..."
    
    mkdir -p "$SSL_DIR"
    
    # Vytvoření self-signed certifikátu s SAN pro lokální doménu
    openssl req -x509 -nodes -days 365 \
        -newkey rsa:2048 \
        -keyout "$KEY_FILE" \
        -out "$CERT_FILE" \
        -config <(
        echo '[dn]'
        echo 'CN=core-platform.local'
        echo '[req]'
        echo 'distinguished_name = dn'
        echo '[SAN]'
        echo 'subjectAltName=DNS:core-platform.local,DNS:*.core-platform.local,DNS:localhost,IP:127.0.0.1'
        echo '[v3_req]'
        echo 'basicConstraints = CA:FALSE'
        echo 'keyUsage = nonRepudiation, digitalSignature, keyEncipherment'
        echo 'subjectAltName = @SAN'
        ) \
        -extensions v3_req
    
    print_success "SSL certifikáty vytvořeny"
fi

# 4. Zastavení existujících kontejnerů
print_info "Zastavuji existující kontejnery..."
cd "$DOCKER_DIR"

if docker compose ps -q | grep -q .; then
    docker compose down
    print_success "Kontejnery zastaveny"
fi

# 4.5. Automatické generování Keycloak realm konfigurace pro development
print_info "Generuji Keycloak realm konfiguraci pro development..."
if [[ -f "${DOCKER_DIR}/keycloak/generate-realm.sh" ]]; then
    cd "${DOCKER_DIR}/keycloak"
    # Nastavíme DOMAIN pro development
    export DOMAIN=core-platform.local
    ./generate-realm.sh
    print_success "Realm konfigurace vygenerována pro core-platform.local"
    cd "$DOCKER_DIR"
else
    print_warning "generate-realm.sh nenalezen - pokračuji bez generování"
fi

# 5. Spuštění SSL prostředí
print_info "Spouštím SSL prostředí..."

# Instalace Loki plugin pokud není nainstalovaný
"${SCRIPT_DIR}/install_loki_plugin.sh" || true

# Spuštění s SSL konfigurací
docker compose up -d --build

print_success "Kontejnery spuštěny s SSL konfigurací"

# 6. Čekání na spuštění služeb
print_info "Čekám na spuštění služeb..."

sleep 10

# 7. Health check s HTTPS
check_service() {
    local name="$1"
    local url="$2"
    local max_attempts=15
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -k -s -f "$url" >/dev/null 2>&1; then
            print_success "$name je dostupné"
            return 0
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            print_warning "$name neodpovídá (možná se stále spouští)"
            return 1
        fi
        
        echo -n "."
        sleep 3
        ((attempt++))
    done
}

print_info "Kontroluji dostupnost služeb..."

check_service "Frontend (HTTPS)" "https://core-platform.local" 
check_service "Backend API" "https://core-platform.local/api/health"
check_service "Keycloak" "https://core-platform.local/realms/core-platform"
check_service "Keycloak Admin" "http://core-platform.local:8081/health"

# 8. Přehled dostupných služeb
echo ""
print_success "🎉 SSL prostředí je připraveno!"
echo ""
echo -e "${CYAN}📋 Dostupné služby (HTTPS):${NC}"
echo "  🌐 Frontend:     https://core-platform.local"
echo "  🔐 API:          https://core-platform.local/api"
echo "  🔑 Keycloak:     https://core-platform.local/realms/core-platform"
echo "  👤 Admin:        http://core-platform.local:8081 (admin/admin)"
echo ""
echo -e "${CYAN}📊 Monitoring (HTTP):${NC}"
echo "  📊 Grafana:      http://localhost:3001 (admin/admin)"
echo "  🗃️  PgAdmin:      http://localhost:5050 (admin@local.dev/admin)"
echo "  📈 Prometheus:   http://localhost:9091"
echo ""
echo -e "${CYAN}🔐 Testovací účet:${NC}"
echo "  👤 Username:     test"
echo "  🔑 Password:     Test.1234"
echo ""
echo -e "${YELLOW}⚠️  SSL Varování:${NC}"
echo "  • Certifikát je self-signed - prohlížeč zobrazí varování"
echo "  • Klikněte 'Advanced' → 'Proceed to core-platform.local'"
echo "  • Nebo přidejte certifikát do keychain:"
echo "    sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain docker/ssl/cert.pem"
echo ""

# 9. Kontrola logů pro případné chyby
print_info "Kontroluji logy pro chyby..."
if docker compose logs --tail=20 | grep -i error | grep -v "connection refused" | head -5; then
    print_warning "Nalezeny některé chyby v lozích - zkontrolujte docker compose logs"
else
    print_success "Žádné kritické chyby v lozích"
fi

echo ""
print_success "🚀 Přejděte na: https://core-platform.local"