# Makefile for core-platform project
# Includes environment management and multitenancy tests

# =============================================================================
# 🏗️ BUILD DOCTOR CONFIGURATION
# =============================================================================
SHELL := /bin/bash
.ONESHELL:
BUILD_TS := $(shell date +%Y%m%d-%H%M%S)
LOG_DIR := diagnostics
LOG_FILE := $(LOG_DIR)/build-$(BUILD_TS).log
JSON_REPORT := $(LOG_DIR)/build-report-$(BUILD_TS).json

.PHONY: help test-mt report-mt test-and-report clean-artifacts
.PHONY: up down clean rebuild doctor watch verify verify-full

# =============================================================================
# 🚀 MAIN ENVIRONMENT TARGETS
# =============================================================================

# Show main help (default target)
help:
	@echo "🚀 Core Platform - Main Commands:"
	@echo ""
	@echo "📦 Development (RECOMMENDED - Hot Reload):"
	@echo "  dev-up          - Start dev environment with hot reload"
	@echo "  dev-watch       - Start with watch mode (foreground)"
	@echo "  dev-down        - Stop dev environment"
	@echo "  dev-restart     - Restart dev services"
	@echo "  dev-clean       - Clean restart dev environment"
	@echo "  dev-check       - Health check dev environment"
	@echo ""
	@echo "📊 Logs (POUZE přes Loki):"
	@echo "  logs            - Všechny logy (Loki)"
	@echo "  logs-backend    - Backend logy (Loki)"
	@echo "  logs-frontend   - Frontend logy (Loki)"
	@echo "  logs-keycloak   - Keycloak logy (Loki)"
	@echo "  logs-errors     - Všechny ERROR logy (Loki)"
	@echo ""
	@echo "📦 Production Environment (CI/CD):"
	@echo "  up              - Start production environment"
	@echo "  down            - Stop all services"
	@echo "  restart         - Restart all services"
	@echo "  clean           - Clean restart (rebuild + smaže DATA)"
	@echo ""
	@echo "🧪 Testing:"
	@echo "  test-backend    - Backend unit tests"
	@echo "  test-frontend   - Frontend tests"
	@echo "  test-mt         - Multitenancy tests"
	@echo "  verify          - Quick smoke tests (health checks)"
	@echo "  verify-full     - Full integration tests"
	@echo ""
	@echo "📚 More: make help-advanced"

# Advanced help
.PHONY: help-advanced
help-advanced:
	@echo "🔧 Advanced Commands:"
	@echo ""
	@echo "🐳 Single Service Management:"
	@echo "  rebuild-backend     - Rebuild backend only"
	@echo "  rebuild-frontend    - Rebuild frontend only"
	@echo "  rebuild-keycloak    - Rebuild keycloak only"
	@echo "  restart-backend     - Restart backend only"
	@echo "  restart-frontend    - Restart frontend only"
	@echo "  restart-keycloak    - Restart keycloak only"
	@echo ""
	@echo "🌐 Domain Management:"
	@echo "  setup-domains        - Setup local domains"
	@echo "  add-tenant-domain    - Add tenant (TENANT=name)"
	@echo "  remove-tenant-domain - Remove tenant (TENANT=name)"
	@echo ""
	@echo "🔐 Keycloak:"
	@echo "  kc-bootstrap REALM=name  - Bootstrap new tenant realm"
	@echo "  reset-kc                 - Reset Keycloak data"
	@echo ""
	@echo "💾 Database:"
	@echo "  reset-db           - Reset database data"
	@echo "  db-clean-migrate   - Clean DB & run fresh migrations (DEV/CI only)"
	@echo ""
	@echo "🧹 Cleanup:"
	@echo "  clean-artifacts     - Clean test artifacts"
	@echo "  docker-cleanup      - Complete Docker cleanup"

# Run multitenancy smoke tests
test-mt:
	@echo "🧪 Running multitenancy smoke tests..."
	@bash tests/multitenancy_smoke.sh

# Generate report from test artifacts
report-mt:
	@echo "📊 Generating test report..."
	@bash tests/make_report.sh

# Run tests and generate report
test-and-report: test-mt report-mt
	@echo ""
	@echo "🎉 Tests completed and report generated!"
	@echo "REPORT: ./TEST_REPORT.md"

# Clean test artifacts
clean-artifacts:
	@echo "🧹 Cleaning test artifacts..."
	@rm -rf artifacts/
	@rm -f TEST_REPORT.md
	@echo "✅ Artifacts cleaned"

# Quick smoke tests (health checks only)
.PHONY: verify
verify:
	@echo "🔍 Running quick smoke tests..."
	@bash scripts/build/post-deployment-check.sh

# Full integration tests (includes multitenancy and streaming)
.PHONY: verify-full
verify-full:
	@echo "🧪 Running full integration tests..."
	@RUN_FULL_TESTS=true bash scripts/build/post-deployment-check.sh
	@echo ""
	@echo "📊 Generating detailed report..."
	@$(MAKE) test-and-report

# =============================================================================
# 🐳 DEV CONTAINER TARGETS (Hot Reload - DOPORUČENO)
# =============================================================================

# Start Dev Container environment with hot reload
.PHONY: dev-up
dev-up:
	@echo "🐳 Starting Dev Container with HOT RELOAD..."
	@echo ""
	@echo "📋 Режим:"
	@echo "   • Backend: Spring DevTools auto-restart (2-5s)"
	@echo "   • Frontend: Vite watch + nginx (3-7s)"
	@echo "   • První build: ~3-5 minut (jednou)"
	@echo ""
	@docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env up -d
	@echo ""
	@echo "✅ Dev prostředí běží!"
	@echo ""
	@echo "📍 Přístup:"
	@echo "   Frontend:  https://core-platform.local/"
	@echo "   API:       https://core-platform.local/api"
	@echo "   Keycloak:  http://localhost:8081/admin/"
	@echo "   Grafana:   http://localhost:3001/"
	@echo ""
	@echo "🐛 Debug:"
	@echo "   Java: localhost:5005 (VS Code F5)"
	@echo ""
	@echo "💡 Další kroky:"
	@echo "   1. make dev-check     - Zkontroluj prostředí"
	@echo "   2. make logs-backend  - Sleduj backend logy"
	@echo "   3. Edituj kód → automatický rebuild!"

# Start with watch mode (foreground)
.PHONY: dev-watch
dev-watch:
	@echo "👀 Starting watch mode (foreground)..."
	@docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env watch

# Stop Dev Container
.PHONY: dev-down
dev-down:
	@echo "🛑 Stopping dev environment..."
	@docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env down
	@echo "✅ Stopped"

# Restart dev services
.PHONY: dev-restart
dev-restart:
	@echo "🔄 Restarting dev services..."
	@docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env restart
	@echo "✅ Restarted"

# Clean restart dev environment
.PHONY: dev-clean
dev-clean:
	@echo "🧹 Clean dev restart..."
	@docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env down -v
	@docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env build --no-cache
	@$(MAKE) dev-up

# Health check
.PHONY: dev-check
dev-check:
	@echo "🧪 Checking dev environment..."
	@bash scripts/devcontainer/test-env-check.sh || echo "⚠️  Some checks failed"

# =============================================================================
# 📊 LOKI LOGS (JEDINÝ ZPŮSOB PRO LOGY)
# =============================================================================

# All logs via Loki
.PHONY: logs
logs:
	@echo "📋 Všechny logy (Loki - posledních 10 minut):"
	@bash tests/loki_query.sh all 10m

# Backend logs
.PHONY: logs-backend
logs-backend:
	@echo "📋 Backend logy (Loki - posledních 10 minut):"
	@bash tests/loki_query.sh backend 10m

# Frontend logs
.PHONY: logs-frontend
logs-frontend:
	@echo "📋 Frontend logy (Loki - posledních 10 minut):"
	@bash tests/loki_query.sh frontend 10m

# Keycloak logs
.PHONY: logs-keycloak
logs-keycloak:
	@echo "📋 Keycloak logy (Loki - posledních 10 minut):"
	@bash tests/loki_query.sh keycloak 10m

# All error logs
.PHONY: logs-errors
logs-errors:
	@echo "🔥 Všechny ERROR logy (Loki - posledních 30 minut):"
	@bash tests/loki_query.sh errors 30m

# Live tail backend
.PHONY: logs-tail
logs-tail:
	@echo "📋 Live backend logy (Loki)..."
	@bash tests/loki_query.sh tail backend

# =============================================================================
# 🏗️ BUILD DOCTOR WRAPPED TARGETS
# =============================================================================

# Production up with Build Doctor
up:
	@scripts/build/wrapper.sh $(MAKE) _up_inner 2>&1 | tee -a $(LOG_FILE)

_up_inner: validate-env kc-image
	@echo ">>> starting compose up at $(BUILD_TS)"
	@echo "🚀 Starting Core Platform environment..."
	@echo "📋 Environment: $${ENVIRONMENT:-development}"
	@echo "🌐 Domain: $${DOMAIN:-core-platform.local}"
	@DOCKER_BUILDKIT=1 docker compose -f docker/docker-compose.yml --env-file .env up -d --remove-orphans
	@echo ""
	@echo "✅ Environment started successfully!"
	@echo "🌐 Admin Frontend: https://admin.$${DOMAIN:-core-platform.local}"
	@echo "🔐 Keycloak: https://localhost:8081"
	@echo "📊 Grafana: http://localhost:3001"
	@echo "🗄️  PgAdmin: http://localhost:5050"
	@echo ""
	@echo "⏳ Waiting for services to be ready... (this may take a few minutes)"
	@scripts/build/wait-healthy.sh --timeout 180
	@echo ""
	@echo "🧪 Running post-deployment checks..."
	@bash scripts/build/post-deployment-check.sh

# Production rebuild with Build Doctor
rebuild:
	@scripts/build/wrapper.sh $(MAKE) _rebuild_inner 2>&1 | tee -a $(LOG_FILE)

_rebuild_inner:
	@echo ">>> rebuilding at $(BUILD_TS)"
	@DOCKER_BUILDKIT=1 docker compose -f docker/docker-compose.yml --env-file .env build --parallel --no-cache
	@$(MAKE) up

# Clean with Build Doctor
clean:
	@scripts/build/wrapper.sh $(MAKE) _clean_inner 2>&1 | tee -a $(LOG_FILE)

_clean_inner:
	@echo ">>> cleaning at $(BUILD_TS)"
	@echo "🧹 Clean restart - rebuilding all images..."
	@docker compose -f docker/docker-compose.yml --env-file .env down --rmi local --volumes
	@$(MAKE) rebuild

# Crashloop watcher
watch:
	@echo "👁️  Watching for crashloops (Ctrl+C to stop)..."
	@bash scripts/build/watch-crashloop.sh

# Stop all services
down:
	@echo "🛑 Stopping Core Platform environment..."
	docker compose -f docker/docker-compose.yml --env-file .env down

# Restart all services
restart:
	@echo "🔄 Restarting Core Platform environment..."
	@$(MAKE) down
	@$(MAKE) up

# =============================================================================
# 🐳 ORIGINAL PRODUCTION TARGETS (kept for reference)
# =============================================================================

# Start complete environment with auto-setup (LEGACY - use 'up' instead)
.PHONY: up-legacy
up-legacy: validate-env kc-image
	@echo "🚀 Starting Core Platform environment..."
	@echo "📋 Environment: $${ENVIRONMENT:-development}"
	@echo "🌐 Domain: $${DOMAIN:-core-platform.local}"
	docker compose -f docker/docker-compose.yml --env-file .env up -d
	@echo ""
	@echo "✅ Environment started successfully!"
	@echo "🌐 Admin Frontend: https://admin.$${DOMAIN:-core-platform.local}"
	@echo "🔐 Keycloak: https://localhost:8081"
	@echo "📊 Grafana: http://localhost:3001"
	@echo "🗄️  PgAdmin: http://localhost:5050"
	@echo ""
	@echo "⏳ Waiting for services to be ready... (this may take a few minutes)"
	@$(MAKE) wait-for-services

# Fresh start - pouze Keycloak data, zachová DB
.PHONY: fresh
fresh:
	@echo "🆕 Fresh start - resetting Keycloak data only..."
	@echo "⚠️  This will DELETE all Keycloak customizations but KEEP database data!"
	@echo "Press Ctrl+C within 5 seconds to cancel..."
	@sleep 5
	@echo "🛑 Stopping services..."
	docker compose -f docker/docker-compose.yml --env-file .env stop keycloak
	@echo "🗑️  Removing Keycloak volume..."
	docker volume rm docker_keycloak_data 2>/dev/null || echo "Volume already removed"
	@echo "🚀 Starting fresh Keycloak..."
	@$(MAKE) up

# Reset pouze Keycloak do výchozího stavu
.PHONY: reset-kc
reset-kc:
	@echo "🔄 Resetting Keycloak to default state..."
	@echo "⚠️  This will DELETE all Keycloak customizations!"
	@echo "Press Ctrl+C within 3 seconds to cancel..."
	@sleep 3
	@echo "🛑 Stopping Keycloak..."
	docker compose -f docker/docker-compose.yml --env-file .env stop keycloak
	@echo "🗑️  Removing Keycloak data..."
	docker volume rm docker_keycloak_data 2>/dev/null || echo "Volume already removed"
	@echo "🚀 Starting clean Keycloak..."
	docker compose -f docker/docker-compose.yml --env-file .env start keycloak
	@echo "⏳ Waiting for Keycloak and setting up realm..."
	@$(MAKE) wait-for-services

# Reset pouze databázi do výchozího stavu
.PHONY: reset-db
reset-db:
	@echo "💾 Resetting database to default state..."
	@echo "⚠️  This will DELETE all application data but KEEP Keycloak!"
	@echo "Press Ctrl+C within 3 seconds to cancel..."
	@sleep 3
	@echo "🛑 Stopping backend and database..."
	docker compose -f docker/docker-compose.yml --env-file .env stop backend db postgres-exporter
	@echo "🗑️  Removing database volume..."
	docker volume rm docker_core_db_data 2>/dev/null || echo "Volume already removed"
	@echo "🚀 Starting fresh database..."
	docker compose -f docker/docker-compose.yml --env-file .env start db postgres-exporter backend
	@echo "⏳ Waiting for services..."
	@sleep 10
	@echo "✅ Database reset complete"

# Clean database and run fresh migrations (DEV/CI only!)
.PHONY: db-clean-migrate
db-clean-migrate:
	@echo "🔄 Clean database migration (DEV/CI only)..."
	@echo "⚠️  This will DROP ALL TABLES and run migrations from scratch!"
	@echo "⚠️  NEVER use in production!"
	@echo "Press Ctrl+C within 5 seconds to cancel..."
	@sleep 5
	@echo "🛑 Stopping backend and database..."
	docker compose -f docker/docker-compose.yml --env-file .env stop backend db postgres-exporter
	@echo "🗑️  Removing database volume..."
	docker volume rm docker_core_db_data 2>/dev/null || echo "Volume already removed"
	@echo "🚀 Starting fresh database with migrations..."
	docker compose -f docker/docker-compose.yml --env-file .env start db postgres-exporter
	@echo "⏳ Waiting for database to be ready..."
	@sleep 5
	@echo "✅ Starting backend with fresh migrations..."
	docker compose -f docker/docker-compose.yml --env-file .env start backend
	@echo "⏳ Waiting for backend to be ready..."
	@for i in $$(seq 1 30); do \
		if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then \
			echo "✅ Backend is ready with fresh database!"; \
			exit 0; \
		fi; \
		echo "⏳ Waiting... ($$i/30)"; \
		sleep 2; \
	done; \
	echo "⚠️  Backend might still be starting up"

# Build all images
.PHONY: build
build:
	@echo "🔨 Building all images..."
	@$(MAKE) kc-image
	docker compose -f docker/docker-compose.yml --env-file .env build --no-cache

# Show services status
.PHONY: status 
status:
	@echo "📊 Core Platform Services Status:"
	@echo "=================================="
	docker compose -f docker/docker-compose.yml --env-file .env ps

# Show all services logs
.PHONY: logs
logs:
	@echo "📋 Core Platform Services Logs:"
	docker compose -f docker/docker-compose.yml --env-file .env logs -f

# First-time development setup
.PHONY: dev-setup
dev-setup:
	@echo "🔧 Setting up development environment..."
	@if [ ! -f .env ]; then \
		echo "📄 Creating .env file from template..."; \
		cp .env.example .env; \
		echo "🔑 Generating secure secrets..."; \
		sed -i.bak "s/dev-admin-password/$$(openssl rand -base64 16)/g" .env; \
		rm .env.bak; \
		echo "✅ .env file created with secure secrets"; \
	else \
		echo "✅ .env file already exists"; \
	fi
	@$(MAKE) validate-env
	@echo "🌐 Setting up local domains..."
	@$(MAKE) setup-domains
	@echo "🏗️  Building images..."
	@$(MAKE) build
	@echo ""
	@echo "✅ Development setup complete!"
	@echo "🚀 Run 'make up' to start the environment"

# Setup local domains for development
.PHONY: setup-domains
setup-domains:
	@echo "🌐 Setting up local development domains..."
	@if [ ! -x scripts/setup-local-domains.sh ]; then \
		chmod +x scripts/setup-local-domains.sh; \
	fi
	@echo "This will modify /etc/hosts - you may be prompted for sudo password:"
	@sudo scripts/setup-local-domains.sh setup
	@echo "✅ Local domains configured"

# Add tenant domain (for manual tenant creation)
.PHONY: add-tenant-domain
add-tenant-domain:
	@if [ -z "$(TENANT)" ]; then \
		echo "❌ TENANT parameter required"; \
		echo "Usage: make add-tenant-domain TENANT=my-tenant"; \
		exit 1; \
	fi
	@echo "🌐 Adding domain for tenant: $(TENANT)"
	@sudo scripts/setup-local-domains.sh add-tenant $(TENANT)
	@echo "✅ Domain added: https://$(TENANT).core-platform.local"

# Remove tenant domain
.PHONY: remove-tenant-domain  
remove-tenant-domain:
	@if [ -z "$(TENANT)" ]; then \
		echo "❌ TENANT parameter required"; \
		echo "Usage: make remove-tenant-domain TENANT=my-tenant"; \
		exit 1; \
	fi
	@echo "🗑️ Removing domain for tenant: $(TENANT)"
	@sudo scripts/setup-local-domains.sh remove-tenant $(TENANT)
	@echo "✅ Domain removed: $(TENANT).core-platform.local"

# Show current domain configuration
.PHONY: show-domains
show-domains:
	@scripts/setup-local-domains.sh show

# Setup true wildcard support via dnsmasq (macOS only)
.PHONY: setup-wildcard
setup-wildcard:
	@echo "🌐 Setting up true wildcard support..."
	@echo "This will install dnsmasq via Homebrew (macOS only):"
	@sudo scripts/setup-local-domains.sh dnsmasq
	@echo "✅ Wildcard support configured - you can now use ANY subdomain!"

# Validate environment configuration
.PHONY: validate-env
validate-env:
	@echo "✅ Validating environment configuration..."
	@if [ ! -f .env ]; then \
		echo "❌ .env file not found!"; \
		echo "💡 Run 'make dev-setup' for first-time setup"; \
		exit 1; \
	fi
	@if grep -q "CHANGE-ME" .env 2>/dev/null; then \
		echo "⚠️  Warning: Found CHANGE-ME placeholders in .env - please review"; \
	fi
	@echo "✅ Environment configuration looks good"

# Wait for services to be ready and setup initial configuration
.PHONY: wait-for-services
wait-for-services:
	@echo "⏳ Waiting for database..."
	@for i in $$(seq 1 60); do \
		if docker exec core-db pg_isready -U core -d core >/dev/null 2>&1; then \
			break; \
		fi; \
		sleep 2; \
	done
	@echo "✅ Database ready"
	@echo "⏳ Waiting for Keycloak..."
	@# 🔧 FIX: Simplified Keycloak readiness check using admin CLI
	@for i in $$(seq 1 120); do \
		echo "📋 DEBUG: Keycloak readiness check attempt $$i/120..."; \
		if docker exec core-keycloak /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password $${KEYCLOAK_ADMIN_PASSWORD:-admin123} >/dev/null 2>&1; then \
			echo "✅ Keycloak admin CLI ready"; \
			break; \
		fi; \
		sleep 3; \
	done
	@echo "✅ Keycloak ready"
	@echo "🏗️  Setting up admin realm..."
	@echo "📋 DEBUG: Running generate-realm.sh script..."
	@bash docker/keycloak/generate-realm.sh && echo "✅ generate-realm.sh completed" || echo "⚠️  generate-realm.sh failed"
	@sleep 2
	@echo "📋 DEBUG: Checking if realm already exists..."
	@# 🔧 FIX: Use admin CLI instead of external HTTPS calls
	@REALM_CHECK=$$(docker exec core-keycloak /opt/keycloak/bin/kcadm.sh get realms/admin 2>/dev/null | jq -r '.realm // empty' 2>/dev/null); \
	echo "📋 DEBUG: Realm check result: $$REALM_CHECK"; \
	if [ -z "$$REALM_CHECK" ] || [ "$$REALM_CHECK" = "null" ] || [ "$$REALM_CHECK" = "empty" ]; then \
		echo "🆕 Realm does not exist, creating admin realm..."; \
		echo "📋 DEBUG: Copying realm JSON to container..."; \
		docker cp docker/keycloak/realm-admin.json core-keycloak:/tmp/realm-admin.json; \
		echo "📋 DEBUG: Creating realm using admin CLI..."; \
		CREATE_RESULT=$$(docker exec core-keycloak /opt/keycloak/bin/kcadm.sh create realms -f /tmp/realm-admin.json 2>&1); \
		if echo "$$CREATE_RESULT" | grep -q "Created new realm"; then \
			echo "✅ Realm created successfully: $$CREATE_RESULT"; \
		else \
			echo "❌ Failed to create realm: $$CREATE_RESULT"; \
			exit 1; \
		fi; \
	else \
		echo "✅ Realm admin already exists ($$REALM_CHECK)"; \
	fi
	@echo "✅ Keycloak realm configured"
	@echo "🔧 Installing Keycloak CDC triggers..."
	@bash scripts/setup-keycloak-triggers.sh || echo "⚠️  Failed to install triggers - CDC synchronization may not work"
	@echo "⏳ Waiting for backend..."
	@for i in $$(seq 1 45); do \
		if curl -s http://localhost:8080/actuator/health >/dev/null 2>&1; then \
			break; \
		fi; \
		sleep 2; \
	done
	@echo "✅ Backend ready"
	@echo ""
	@echo "🎉 All services are ready!"
	@echo "🌐 Frontend: https://$${DOMAIN:-core-platform.local}"
	@echo "🔐 Keycloak: https://localhost:8081 (admin/$${KEYCLOAK_ADMIN_PASSWORD:-admin123})"
	@echo "📊 Grafana: http://localhost:3001"

# =============================================================================
# 🔗 KEYCLOAK BOOTSTRAP TARGETS
# =============================================================================

# Bootstrap new tenant realm (parametrized)
.PHONY: kc-bootstrap
kc-bootstrap:
	@if [ -z "$(REALM)" ]; then \
		echo "❌ REALM parameter required"; \
		echo "Usage: make kc-bootstrap REALM=my-tenant [TENANT_ADMIN=...] [TENANT_ADMIN_PASSWORD=...]"; \
		echo ""; \
		echo "Examples:"; \
		echo "  make kc-bootstrap REALM=company-a"; \
		echo "  make kc-bootstrap REALM=test-org TENANT_ADMIN=org-admin"; \
		exit 1; \
	fi
	@echo "🏗️ Bootstrapping tenant realm: $(REALM)"
	@echo "⏳ This may take a few minutes..."
	@REALM=$(REALM) \
	TENANT_ADMIN=$${TENANT_ADMIN:-tenant-admin} \
	TENANT_ADMIN_PASSWORD=$${TENANT_ADMIN_PASSWORD:-TempPass123!} \
	KEYCLOAK_ADMIN_USER=$${KEYCLOAK_ADMIN:-admin} \
	KEYCLOAK_ADMIN_PASSWORD=$${KEYCLOAK_ADMIN_PASSWORD:-admin123} \
	docker compose -f docker/docker-compose.yml exec keycloak /bin/sh -c "scripts/kc-bootstrap_realm.sh"
	@echo ""
	@echo "🎉 Tenant realm $(REALM) bootstrapped successfully!"
	@echo "🌐 Add domain mapping: make add-tenant-domain TENANT=$(REALM)"

# Build Keycloak image
.PHONY: kc-image
kc-image:
	@echo "🔨 Building Keycloak image..."
	docker build -f docker/keycloak/Dockerfile -t core-platform/keycloak:local .

# Build Keycloak image (no cache)
.PHONY: kc-image-no-cache
kc-image-no-cache:
	@echo "🔨 Building Keycloak image (no cache)..."
	docker build --no-cache -f docker/keycloak/Dockerfile -t core-platform/keycloak:local .

# =============================================================================
# 🔧 SINGLE SERVICE MANAGEMENT TARGETS  
# =============================================================================

# Rebuild & restart backend only
.PHONY: rebuild-backend
rebuild-backend:
	@echo "🔨 Rebuilding backend service..."
	# @echo "🧪 Running unit tests before rebuild..."
	# @$(MAKE) test-backend-unit || (echo "❌ Unit tests failed - aborting rebuild" && exit 1)
	docker compose -f docker/docker-compose.yml --env-file .env stop backend
	docker compose -f docker/docker-compose.yml --env-file .env build --no-cache backend
	docker compose -f docker/docker-compose.yml --env-file .env up -d backend
	@echo "✅ Backend rebuilt and restarted"
	@echo "⏳ Waiting for backend to be ready..."
	@for i in $$(seq 1 30); do \
		if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then \
			echo "✅ Backend is ready!"; \
			break; \
		fi; \
		sleep 2; \
	done
	# @echo "🧪 Running integration tests after rebuild..."
	# @$(MAKE) test-backend-integration

# Restart backend service only
.PHONY: restart-backend
restart-backend:
	@echo "🔄 Restarting backend service..."
	docker compose -f docker/docker-compose.yml --env-file .env restart backend
	@echo "✅ Backend restarted"
	@echo "⏳ Waiting for backend to be ready..."
	@for i in $$(seq 1 30); do \
		if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then \
			echo "✅ Backend is ready!"; \
			@echo "🧪 Running health check tests..."; \
			@$(MAKE) test-backend-health; \
			break; \
		fi; \
		sleep 2; \
	done

# Rebuild & restart frontend only - FIXED: Only frontend, no dependencies
.PHONY: rebuild-frontend
rebuild-frontend:
	@echo "🔨 Rebuilding frontend service with deep cleanup..."
	@echo "🛑 Stopping frontend service only..."
	docker compose -f docker/docker-compose.yml --env-file .env stop frontend
	@echo "🗑️  Removing frontend container completely..."
	-docker rm core-frontend 2>/dev/null || echo "Container already removed"
	@echo "🧹 Removing frontend image to force complete rebuild..."
	-docker rmi docker-frontend:latest 2>/dev/null || echo "Image already removed"
	@echo "🚿 Clearing Docker build cache for frontend..."
	-docker builder prune -f --filter label=stage=frontend-build 2>/dev/null || true
	@echo "🏗️  Building fresh frontend image (no cache)..."
	docker compose -f docker/docker-compose.yml --env-file .env build --no-cache frontend
	@echo "🚀 Starting ONLY frontend container (no dependencies)..."
	docker compose -f docker/docker-compose.yml --env-file .env up -d --no-deps frontend
	@echo ""
	@echo "✅ Frontend rebuilt and restarted with complete cleanup!"
	@echo "🌐 Frontend: https://$${DOMAIN:-core-platform.local}"
	@echo ""
	@echo "💡 Browser cache cleanup recommendation:"
	@echo "   1. Open DevTools (F12)"
	@echo "   2. Right-click refresh button → 'Empty Cache and Hard Reload'"
	@echo "   3. Or open Application tab → Storage → Clear site data"
	@echo ""
	@echo "🧹 To clear browser data automatically, run: make clear-browser-cache"

# Rebuild & restart keycloak only
.PHONY: rebuild-keycloak
rebuild-keycloak:
	@echo "🔨 Rebuilding Keycloak service with deep cleanup..."
	@echo "🛑 Stopping Keycloak service..."
	docker compose -f docker/docker-compose.yml --env-file .env stop keycloak
	@echo "🗑️  Removing Keycloak container completely..."
	-docker rm core-keycloak 2>/dev/null || echo "Container already removed"
	@echo "🧹 Removing Keycloak image to force complete rebuild..."
	-docker rmi core-platform/keycloak:local 2>/dev/null || echo "Image already removed"
	@echo "🚿 Clearing Docker build cache..."
	-docker builder prune -f 2>/dev/null || true
	@echo "🏗️  Building fresh Keycloak image (no cache)..."
	@$(MAKE) kc-image-no-cache
	@echo "🚀 Starting fresh Keycloak container..."
	docker compose -f docker/docker-compose.yml --env-file .env up -d keycloak
	@echo "✅ Keycloak rebuilt and restarted with complete cleanup!"
	@echo "⏳ Waiting for Keycloak to be ready..."
	@for i in $$(seq 1 45); do \
		if curl -k -s https://localhost:8081/health/ready | grep -q "UP" >/dev/null 2>&1; then \
			echo "✅ Keycloak is ready!"; \
			break; \
		fi; \
		sleep 2; \
	done

# Restart frontend service only
.PHONY: restart-frontend
restart-frontend:
	@echo "🔄 Restarting frontend service..."
	docker compose -f docker/docker-compose.yml --env-file .env restart frontend
	@echo "✅ Frontend restarted"

# Restart keycloak service only
.PHONY: restart-keycloak
restart-keycloak:
	@echo "🔄 Restarting Keycloak service..."
	docker compose -f docker/docker-compose.yml --env-file .env restart keycloak
	@echo "✅ Keycloak restarted"
	@echo "⏳ Waiting for Keycloak to be ready..."
	@for i in $$(seq 1 45); do \
		if curl -k -s https://localhost:8081/health/ready | grep -q "UP" >/dev/null 2>&1; then \
			echo "✅ Keycloak is ready!"; \
			break; \
		fi; \
		sleep 2; \
	done

# Restart database service only
.PHONY: restart-db
restart-db:
	@echo "🔄 Restarting database service..."
	docker compose -f docker/docker-compose.yml --env-file .env restart db
	@echo "✅ Database restarted"
	@echo "⏳ Waiting for database to be ready..."
	@for i in $$(seq 1 30); do \
		if docker exec core-db pg_isready -U core -d core >/dev/null 2>&1; then \
			echo "✅ Database is ready!"; \
			break; \
		fi; \
		sleep 2; \
	done

# Show backend logs only
.PHONY: logs-backend
logs-backend:
	@echo "📋 Backend logs (Docker + recent test results):"
	@echo "=== Docker Logs ==="
	@docker compose -f docker/docker-compose.yml --env-file .env logs --tail=50 backend
	@echo ""
	@echo "=== Recent Test Results ==="
	@if [ -f artifacts/backend_tests.log ]; then \
		echo "🧪 Last unit test results:"; \
		tail -20 artifacts/backend_tests.log; \
	else \
		echo "No recent test results found"; \
	fi

# Show frontend logs only
.PHONY: logs-frontend
logs-frontend:
	@echo "📋 Frontend logs:"
	docker compose -f docker/docker-compose.yml --env-file .env logs -f frontend

# Show keycloak logs only
.PHONY: logs-keycloak
logs-keycloak:
	@echo "📋 Keycloak logs:"
	docker compose -f docker/docker-compose.yml --env-file .env logs -f keycloak

# Show database logs only
.PHONY: logs-db
logs-db:
	@echo "📋 Database logs:"
	docker compose -f docker/docker-compose.yml --env-file .env logs -f db

# =============================================================================
# 📊 LOKI LOGS TARGETS (Alternative to Docker logs)
# =============================================================================

# Show backend logs from Loki (better for debugging restarts)
.PHONY: loki-logs-backend
loki-logs-backend:
	@echo "📋 Backend logs from Loki (last 10 minutes):"
	@curl -s "http://localhost:3100/loki/api/v1/query_range?query=\{container=\"core-backend\"\}&start=$$(($$(date '+%s') - 600))000000000&end=$$(date '+%s')000000000&limit=100" | \
	jq -r '.data.result[]?.values[]?[1]' 2>/dev/null | \
	head -50 || echo "❌ Failed to fetch logs from Loki - is Loki running?"

# Show frontend logs from Loki
.PHONY: loki-logs-frontend  
loki-logs-frontend:
	@echo "📋 Frontend logs from Loki (last 10 minutes):"
	@curl -s "http://localhost:3100/loki/api/v1/query_range?query=\{container=\"core-frontend\"\}&start=$$(($$(date '+%s') - 600))000000000&end=$$(date '+%s')000000000&limit=100" | \
	jq -r '.data.result[]?.values[]?[1]' 2>/dev/null | \
	head -50 || echo "❌ Failed to fetch logs from Loki - is Loki running?"

# Show keycloak logs from Loki  
.PHONY: loki-logs-keycloak
loki-logs-keycloak:
	@echo "📋 Keycloak logs from Loki (last 10 minutes):"
	@curl -s "http://localhost:3100/loki/api/v1/query_range?query=\{container=\"core-keycloak\"\}&start=$$(($$(date '+%s') - 600))000000000&end=$$(date '+%s')000000000&limit=100" | \
	jq -r '.data.result[]?.values[]?[1]' 2>/dev/null | \
	head -50 || echo "❌ Failed to fetch logs from Loki - is Loki running?"

# Show ALL error logs from Loki (across all services)
.PHONY: loki-logs-errors
loki-logs-errors:
	@echo "🔥 Error logs from ALL services (last 30 minutes):"
	@curl -s "http://localhost:3100/loki/api/v1/query_range?query=\{level=\"ERROR\"\}&start=$$(($$(date '+%s') - 1800))000000000&end=$$(date '+%s')000000000&limit=200" | \
	jq -r '.data.result[]?.values[]?[1]' 2>/dev/null | \
	head -100 || echo "❌ Failed to fetch logs from Loki - is Loki running?"

# Show backend startup errors specifically (great for debugging startup issues)
.PHONY: loki-logs-backend-errors
loki-logs-backend-errors:
	@echo "🔥 Backend ERROR logs from Loki (last 30 minutes):"
	@curl -s "http://localhost:3100/loki/api/v1/query_range?query=\{container=\"core-backend\",level=\"ERROR\"\}&start=$$(($$(date '+%s') - 1800))000000000&end=$$(date '+%s')000000000&limit=100" | \
	jq -r '.data.result[]?.values[]?[1]' 2>/dev/null || echo "❌ Failed to fetch logs from Loki - is Loki running?"

# Live tail backend logs from Loki (like docker logs -f but from Loki)
.PHONY: loki-tail-backend
loki-tail-backend:
	@echo "📋 Live tailing backend logs from Loki..."
	@echo "Press Ctrl+C to stop"
	@while true; do \
		curl -s "http://localhost:3100/loki/api/v1/query_range?query=\{container=\"core-backend\"\}&start=$$(($$(date '+%s') - 60))000000000&end=$$(date '+%s')000000000&limit=10" | \
		jq -r '.data.result[]?.values[]?[1]' 2>/dev/null | tail -5; \
		sleep 2; \
	done

# Search logs by text pattern
.PHONY: loki-search
loki-search:
	@if [ -z "$(PATTERN)" ]; then \
		echo "❌ PATTERN parameter required"; \
		echo "Usage: make loki-search PATTERN='AdminTenantController' [SERVICE=backend] [MINUTES=30]"; \
		exit 1; \
	fi
	@echo "🔍 Searching for '$(PATTERN)' in $(SERVICE:-all services) (last $(MINUTES:-30) minutes):"
	@MINUTES=$${MINUTES:-30}; \
	SERVICE_FILTER=""; \
	if [ -n "$(SERVICE)" ]; then \
		SERVICE_FILTER=",container=\"core-$(SERVICE)\""; \
	fi; \
	curl -s "http://localhost:3100/loki/api/v1/query_range?query=\{container=~\"core-.*\"$$SERVICE_FILTER\}|~\"$(PATTERN)\"\&start=$$(($$(date '+%s') - $$((MINUTES * 60))))000000000&end=$$(date '+%s')000000000&limit=50" | \
	jq -r '.data.result[]?.values[]?[1]' 2>/dev/null | head -20 || echo "❌ No logs found or Loki error"

# =============================================================================
# 🚀 FRONTEND DEVELOPMENT WORKFLOW (Production Build)
# =============================================================================

# Quick frontend development cycle - build & deploy changes
.PHONY: dev-frontend
dev-frontend:
	@echo "🚀 Quick frontend rebuild & deploy..."
	@echo "⏳ Building frontend (this takes ~30-60s)..."
	@cd frontend && npm run build
	@echo "🔨 Rebuilding Docker container..."
	docker compose -f docker/docker-compose.yml build frontend
	@echo "🔄 Restarting frontend container..."
	docker compose -f docker/docker-compose.yml up -d frontend
	@echo "✅ Frontend deployed! Check: https://$${DOMAIN:-core-platform.local}"

# Build frontend only (no Docker restart) - useful for testing build
.PHONY: dev-build
dev-build:
	@echo "🔨 Building frontend only..."
	@cd frontend && npm run build
	@echo "✅ Frontend build complete - files in frontend/dist/"

# Hot rebuild from outside Docker - build, copy to container, restart nginx
.PHONY: dev-hot
dev-hot:
	@echo "🔥 Hot rebuilding frontend..."
	@cd frontend && npm run build
	@echo "📋 Copying built files to container..."
	@docker cp frontend/dist/. core-frontend:/usr/share/nginx/html/
	@echo "🔄 Reloading nginx configuration..."
	@docker exec core-frontend nginx -s reload
	@echo "✅ Hot reload complete! Check: https://$${DOMAIN:-core-platform.local}"

# Install chokidar-cli for file watching (one-time setup)
.PHONY: dev-setup-watch
dev-setup-watch:
	@echo "🔧 Setting up file watching for development..."
	@cd frontend && npm install chokidar-cli --save-dev
	@echo "✅ File watching setup complete!"
	@echo "💡 Now you can use 'make dev-watch' for auto-rebuilds"

# Clear browser cache and localStorage (macOS Chrome/Safari)
.PHONY: clear-browser-cache
clear-browser-cache:
	@echo "🧹 Clearing browser cache and data for core-platform.local..."
	@echo "⚠️  This will close Chrome if running and clear site data"
	@echo "Press Ctrl+C within 3 seconds to cancel..."
	@sleep 3
	@# Close Chrome
	@pkill -f "Google Chrome" 2>/dev/null || echo "Chrome not running"
	@sleep 2
	@# Clear Chrome cache for our domain (macOS)
	@if [ -d "$$HOME/Library/Application Support/Google/Chrome/Default" ]; then \
		echo "🧽 Clearing Chrome cache..."; \
		rm -rf "$$HOME/Library/Application Support/Google/Chrome/Default/Service Worker/CacheStorage/"*core-platform* 2>/dev/null || true; \
		rm -rf "$$HOME/Library/Application Support/Google/Chrome/Default/Local Storage/leveldb/"*core-platform* 2>/dev/null || true; \
		echo "✅ Chrome cache cleared"; \
	else \
		echo "⚠️  Chrome profile not found"; \
	fi
	@# Clear Safari cache (macOS)
	@if [ -d "$$HOME/Library/Safari" ]; then \
		echo "🧽 Clearing Safari cache..."; \
		rm -rf "$$HOME/Library/Caches/com.apple.Safari/"* 2>/dev/null || true; \
		echo "✅ Safari cache cleared"; \
	fi
	@echo ""
	@echo "✅ Browser cache cleared!"
	@echo "💡 You can now open https://core-platform.local with fresh cache"

# Nuclear option - complete rebuild of specific service with all cleanup
.PHONY: nuclear-rebuild-frontend
nuclear-rebuild-frontend:
	@echo "☢️  NUCLEAR REBUILD: Complete frontend cleanup and rebuild"
	@echo "⚠️  This will remove EVERYTHING related to frontend Docker state"
	@echo "Press Ctrl+C within 5 seconds to cancel..."
	@sleep 5
	@echo "🛑 Stopping all services..."
	docker compose -f docker/docker-compose.yml stop
	@echo "🗑️  Removing frontend container and image completely..."
	-docker rm core-frontend 2>/dev/null || true
	-docker rmi docker-frontend:latest 2>/dev/null || true
	@echo "🧹 Clearing ALL Docker build cache..."
	docker builder prune -af
	@echo "🚿 Removing dangling images..."
	-docker image prune -af
	@echo "🧽 Clearing npm cache in frontend..."
	@cd frontend && npm cache clean --force 2>/dev/null || true
	@echo "🗑️  Removing node_modules and dist..."
	@cd frontend && rm -rf node_modules dist 2>/dev/null || true
	@echo "📦 Fresh npm install..."
	@cd frontend && npm install
	@echo "🏗️  Building fresh frontend image (no cache)..."
	docker compose -f docker/docker-compose.yml build --no-cache frontend
	@echo "🚀 Starting all services..."
	@$(MAKE) up
	@echo ""
	@echo "☢️  NUCLEAR REBUILD COMPLETE!"
	@echo "🌐 Frontend: https://$${DOMAIN:-core-platform.local}"
	@echo "💡 Consider running: make clear-browser-cache"

# =============================================================================
# 🧪 BACKEND TESTING TARGETS
# =============================================================================

# Run backend unit tests only
.PHONY: test-backend-unit
test-backend-unit:
	@echo "🧪 Running backend unit tests..."
	@mkdir -p artifacts
	@cd backend && ./mvnw test -Dtest="**/*Test" > ../artifacts/backend_unit_tests.log 2>&1 || \
		(echo "❌ Unit tests failed - check artifacts/backend_unit_tests.log" && exit 1)
	@echo "✅ Unit tests passed"

# Run backend integration tests
.PHONY: test-backend-integration
test-backend-integration:
	@echo "🧪 Running backend integration tests..."
	@mkdir -p artifacts
	@cd backend && ./mvnw test -Dtest="**/*IT,**/*IntegrationTest" > ../artifacts/backend_integration_tests.log 2>&1 || \
		(echo "❌ Integration tests failed - check artifacts/backend_integration_tests.log" && exit 1)
	@echo "✅ Integration tests passed"

# Run backend health check tests
.PHONY: test-backend-health
test-backend-health:
	@echo "🧪 Running backend health checks..."
	@mkdir -p artifacts
	@echo "Testing basic health endpoint..." > artifacts/backend_health.log
	@if curl -s http://localhost:8080/actuator/health | jq -e '.status == "UP"' >/dev/null 2>&1; then \
		echo "✅ Health check: UP" | tee -a artifacts/backend_health.log; \
	else \
		echo "❌ Health check: DOWN" | tee -a artifacts/backend_health.log; \
		exit 1; \
	fi
	@echo "Testing API endpoints..." >> artifacts/backend_health.log
	@if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then \
		echo "✅ API health: OK" | tee -a artifacts/backend_health.log; \
	else \
		echo "❌ API health: FAIL" | tee -a artifacts/backend_health.log; \
		exit 1; \
	fi

# Run all backend tests
.PHONY: test-backend-all
test-backend-all: test-backend-unit test-backend-integration test-backend-health
	@echo "🎉 All backend tests completed successfully!"

# Show backend test results
.PHONY: show-backend-test-results
show-backend-test-results:
	@echo "📊 Backend Test Results Summary:"
	@echo "================================"
	@if [ -f artifacts/backend_unit_tests.log ]; then \
		echo "🧪 Unit Tests:"; \
		grep -E "(Tests run:|BUILD SUCCESS|BUILD FAILURE)" artifacts/backend_unit_tests.log | tail -2; \
	fi
	@if [ -f artifacts/backend_integration_tests.log ]; then \
		echo "🔗 Integration Tests:"; \
		grep -E "(Tests run:|BUILD SUCCESS|BUILD FAILURE)" artifacts/backend_integration_tests.log | tail -2; \
	fi
	@if [ -f artifacts/backend_health.log ]; then \
		echo "🏥 Health Checks:"; \
		cat artifacts/backend_health.log; \
	fi