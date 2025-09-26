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
    echo "====================================="
    echo ""
    
    # 1. Instalace Loki pluginu (opcionálně)
    print_step "Checking Loki Docker plugin..."
    if ! docker plugin ls --format '{{.Name}}' | grep -qx 'loki' 2>/dev/null; then
        print_step "Installing Loki Docker plugin..."
        if docker plugin install grafana/loki-docker-driver:latest --alias loki --grant-all-permissions >/dev/null 2>&1; then
            print_success "Loki plugin installed"
        else
            print_warning "Could not install Loki plugin - continuing without it"
        fi
    else
        print_success "Loki plugin already installed"
    fi
    
    # 2. Přechod do docker složky
    cd "${REPO_ROOT}/docker" || {
        print_error "Could not change to docker directory"
        exit 1
    }
    
    # 3. Clean stop existing containers
    print_step "Stopping and removing existing containers..."
    docker-compose down --remove-orphans --volumes 2>/dev/null || true
    print_success "Previous containers cleaned up"
    
    # 4. Pull latest images
    print_step "Pulling latest images..."
    docker-compose pull --ignore-pull-failures 2>/dev/null || true
    print_success "Images updated"
    
    # 5. Build and start all services
    print_step "Building and starting all services..."
    docker-compose up -d --build --force-recreate
    
    # 6. Wait for core services
    print_step "Waiting for core services to start..."
    sleep 10
    
    # 7. Health checks with correct URLs
    echo ""
    print_step "Running health checks..."
    
    # Check database first
    print_info "Checking database connection..."
    local db_attempts=0
    while [ $db_attempts -lt 30 ]; do
        if docker-compose exec -T db pg_isready -U core -d core >/dev/null 2>&1; then
            print_success "Database is ready"
            break
        fi
        sleep 2
        ((db_attempts++))
        if [ $db_attempts -eq 30 ]; then
            print_warning "Database health check timed out"
        fi
    done
    
    # Check other services
    check_service "Backend" "http://localhost:8080/actuator/health"
    check_service "Frontend" "http://localhost:3000"
    check_service "Grafana" "http://localhost:3001/api/health"
    check_service "Keycloak" "http://localhost:8081/health/ready"
    
    # 8. Final overview
    echo ""
    print_success "🎉 Core Platform started!"
    echo ""
    echo -e "${CYAN}📋 Service URLs:${NC}"
    echo "  🌐 Frontend:    http://localhost:3000"
    echo "  🔧 Backend:     http://localhost:8080"
    echo "  📊 Grafana:     http://localhost:3001 (admin/admin)"
    echo "  🗄️  PgAdmin:     http://localhost:5050 (admin@local.dev/admin)"
    echo "  🔐 Keycloak:    http://localhost:8081 (admin/admin)"
    echo "  📈 Prometheus:  http://localhost:9091"
    echo ""
    echo -e "${CYAN}🐳 Container Status:${NC}"
    docker-compose ps
    echo ""
    print_info "Use 'docker-compose logs -f [service]' to view logs"
}

# 🏥 Funkce pro kontrolu zdraví služeb
check_service() {
    local name="$1"
    local url="$2"
    local max_attempts=15
    local attempt=1
    
    print_info "Checking $name..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s -f -m 5 "$url" >/dev/null 2>&1; then
            print_success "$name is healthy"
            return 0
        fi
        
        if [ $attempt -eq $max_attempts ]; then
            print_warning "$name is not responding (may still be starting)"
            return 1
        fi
        
        sleep 3
        ((attempt++))
    done
}

# Spuštění
main "$@"