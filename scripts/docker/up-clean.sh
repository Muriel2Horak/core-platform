#!/usr/bin/env bash
set -euo pipefail

# 🎨 Barvy pro lepší čitelnost
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 📊 Funkce pro čistý výstup
print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# 🚀 Hlavní funkce
main() {
    local SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    local REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
    
    echo -e "${CYAN}🚀 Core Platform - Clean Startup${NC}"
    echo "=====================================\\n"
    
    # 1. Instalace Loki pluginu
    print_step "Checking Loki Docker plugin..."
    if ! docker plugin ls --format '{{.Name}}' | grep -qx 'loki'; then
        print_step "Installing Loki Docker plugin..."
        docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions >/dev/null 2>&1
        print_success "Loki plugin installed"
    else
        print_success "Loki plugin already installed"
    fi
    
    # 2. Přechod do docker složky
    cd "${REPO_ROOT}/docker"
    
    # 3. Kontrola stávajících kontejnerů
    print_step "Checking existing containers..."
    local running_containers=$(docker compose ps --services --filter "status=running" 2>/dev/null | wc -l)
    if [ "$running_containers" -gt 0 ]; then
        print_warning "Found $running_containers running containers - stopping them first"
        docker compose down --remove-orphans >/dev/null 2>&1
        print_success "Previous containers stopped"
    fi
    
    # 4. Spuštění služeb po částech s informativním výstupem
    print_step "Starting core infrastructure..."
    
    # Database first
    print_info "Starting PostgreSQL database..."
    docker compose up -d db >/dev/null 2>&1
    
    # Wait for DB to be ready
    print_info "Waiting for database to be ready..."
    while ! docker compose exec -T db pg_isready -U core -d core >/dev/null 2>&1; do
        sleep 2
        echo -n "."
    done
    echo ""
    print_success "Database ready"
    
    # Keycloak
    print_info "Starting Keycloak..."
    docker compose up -d keycloak >/dev/null 2>&1
    print_success "Keycloak started"
    
    # Monitoring stack
    print_info "Starting monitoring stack (Loki, Grafana, Promtail)..."
    docker compose up -d loki grafana promtail >/dev/null 2>&1
    print_success "Monitoring stack ready"
    
    # Backend
    print_info "Building and starting backend..."
    docker compose up -d --build backend >/dev/null 2>&1
    print_success "Backend started"
    
    # Frontend
    print_info "Building and starting frontend..."
    docker compose up -d --build frontend >/dev/null 2>&1
    print_success "Frontend started"
    
    # Optional services
    print_info "Starting optional services (PgAdmin, Prometheus, etc.)..."
    docker compose up -d pgadmin prometheus postgres-exporter cadvisor >/dev/null 2>&1
    print_success "Optional services started"
    
    # 5. Zdravotní kontroly
    echo ""
    print_step "Running health checks..."
    
    # Wait a bit for services to initialize
    sleep 5
    
    # Check each service
    check_service "Frontend" "http://localhost:3000" 
    check_service "Backend" "http://localhost:8080/actuator/health"
    check_service "Grafana" "http://localhost:3001/api/health"
    check_service "Loki" "http://localhost:3100/ready"
    check_service "Keycloak" "http://localhost:8081/health"
    
    # 6. Finální přehled
    echo ""
    print_success "🎉 All services started successfully!"
    echo ""
    echo -e "${CYAN}📋 Service URLs:${NC}"
    echo "  🎨 Frontend:    http://localhost:3000"
    echo "  ⚙️  Backend:     http://localhost:8080"
    echo "  📊 Grafana:     http://localhost:3001 (admin/admin)"
    echo "  🗃️  PgAdmin:     http://localhost:5050"
    echo "  🔐 Keycloak:    http://localhost:8081"
    echo "  📈 Prometheus:  http://localhost:9091"
    echo ""
    echo -e "${CYAN}🐳 Container Status:${NC}"
    docker compose ps --format "table {{.Name}}\\t{{.Status}}\\t{{.Ports}}"
}

# 🏥 Funkce pro kontrolu zdraví služeb
check_service() {
    local name="$1"
    local url="$2"
    local max_attempts=10
    local attempt=1
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$url" >/dev/null 2>&1; then
            print_success "$name is healthy"
            return 0
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            print_warning "$name is not responding (may still be starting)"
            return 1
        fi
        
        sleep 2
        ((attempt++))
    done
}

# Spuštění
main "$@"