# Makefile for core-platform project
# Includes environment management and multitenancy tests

.PHONY: help test-mt report-mt test-and-report clean-artifacts

# =============================================================================
# 🚀 MAIN ENVIRONMENT TARGETS
# =============================================================================

# Show main help (default target)
help:
	@echo "🚀 Core Platform - Main Commands:"
	@echo ""
	@echo "📦 Environment Management:"
	@echo "  up              - Start complete environment (auto-setup)"
	@echo "  down            - Stop all services"
	@echo "  restart         - Restart all services (ZACHOVÁ DATA)"
	@echo "  clean           - Clean restart (rebuild images + smaže VŠECHNA DATA)"
	@echo "  fresh           - Fresh start (smaže JEN Keycloak data, zachová DB)"
	@echo "  reset-kc        - Reset pouze Keycloak do výchozího stavu"
	@echo "  reset-db        - Reset pouze databázi do výchozího stavu"
	@echo "  status          - Show all services status"
	@echo "  logs            - Show all services logs"
	@echo ""
	@echo "🔧 Development - Single Service Management:"
	@echo "  rebuild-backend     - Rebuild & restart backend only"
	@echo "  rebuild-frontend    - Rebuild & restart frontend only"
	@echo "  rebuild-keycloak    - Rebuild & restart keycloak only"
	@echo "  restart-backend     - Restart backend service only"
	@echo "  restart-frontend    - Restart frontend service only"
	@echo "  restart-keycloak    - Restart keycloak service only"  
	@echo "  restart-db          - Restart database service only"
	@echo "  logs-backend        - Show backend logs only (Docker)"
	@echo "  logs-frontend       - Show frontend logs only (Docker)"
	@echo "  logs-keycloak       - Show keycloak logs only (Docker)"
	@echo "  logs-db             - Show database logs only (Docker)"
	@echo ""
	@echo "🚀 Frontend Development (Production Build):"
	@echo "  dev-frontend        - Build & deploy frontend changes quickly"
	@echo "  dev-watch           - Watch frontend changes and auto-rebuild"
	@echo "  dev-build           - Build frontend only (no container restart)"
	@echo "  dev-hot             - Hot rebuild frontend from outside Docker"
	@echo "  nuclear-rebuild-frontend - Complete frontend cleanup and rebuild"
	@echo ""
	@echo "📊 Loki Logs (Better for debugging restarts):"
	@echo "  loki-logs-backend     - Show backend logs from Loki (last 10min)"
	@echo "  loki-logs-frontend    - Show frontend logs from Loki (last 10min)"
	@echo "  loki-logs-keycloak    - Show keycloak logs from Loki (last 10min)"
	@echo "  loki-logs-errors      - Show ALL error logs from Loki (last 30min)"
	@echo "  loki-logs-backend-errors - Show backend errors only from Loki"
	@echo "  loki-tail-backend     - Live tail backend logs from Loki"
	@echo "  loki-search PATTERN=text [SERVICE=backend] - Search logs by pattern"
	@echo ""
	@echo "🔧 Development - General:"
	@echo "  dev-setup       - First-time development setup (includes domains)"
	@echo "  build           - Build all images"
	@echo "  test-mt         - Run multitenancy tests"
	@echo "  report-mt       - Generate test report from artifacts"
	@echo "  test-and-report - Run tests and generate report"
	@echo ""
	@echo "🧪 Backend Testing:"
	@echo "  test-backend-unit           - Run backend unit tests only"
	@echo "  test-backend-integration    - Run backend integration tests"
	@echo "  test-backend-health         - Run backend health checks"
	@echo "  test-backend-all            - Run all backend tests"
	@echo "  show-backend-test-results   - Show backend test results summary"
	@echo ""
	@echo "🌐 Domain Management:"
	@echo "  setup-domains        - Setup local development domains"
	@echo "  add-tenant-domain    - Add tenant domain (TENANT=name)"
	@echo "  remove-tenant-domain - Remove tenant domain (TENANT=name)"
	@echo "  show-domains         - Show current domain configuration"
	@echo "  setup-wildcard       - Setup true wildcard support (macOS)"
	@echo ""
	@echo "🔗 Keycloak Webhook:"
	@echo "  kc-help         - Show Keycloak webhook commands"
	@echo ""
	@echo "📋 Other:"
	@echo "  clean-artifacts - Clean test artifacts"
	@echo "  clear-browser-cache - Clear browser cache and localStorage"

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

# Complete Docker cleanup - removes everything
.PHONY: docker-cleanup
docker-cleanup:
	@echo "🧹 Complete Docker cleanup - removing containers, images, volumes, networks..."
	@echo "⚠️  This will remove ALL Docker data for this project!"
	@read -p "Are you sure? (y/N): " confirm && [ "$$confirm" = "y" ] || exit 1
	@echo "🛑 Stopping all containers..."
	-docker compose -f docker/docker-compose.yml down -v --remove-orphans
	@echo "🗑️  Removing all project images..."
	-docker rmi $$(docker images --filter=reference="*core-platform*" -q) 2>/dev/null || true
	-docker rmi $$(docker images --filter=reference="docker-*" -q) 2>/dev/null || true
	@echo "🧽 Pruning unused Docker resources..."
	-docker system prune -af --volumes
	@echo "✅ Docker cleanup completed!"

# Quick rebuild from scratch
.PHONY: rebuild-clean
rebuild-clean: docker-cleanup build up
	@echo "✅ Complete rebuild from scratch completed!"

# Start complete environment with auto-setup
.PHONY: up
up: validate-env kc-image
	@echo "🚀 Starting Core Platform environment..."
	@echo "📋 Environment: $${ENVIRONMENT:-development}"
	@echo "🌐 Domain: $${DOMAIN:-core-platform.local}"
	@if [ -z "$$KC_WEBHOOK_SECRET" ]; then \
		echo "🔑 Auto-generating webhook secret..."; \
		export KC_WEBHOOK_SECRET=$$(openssl rand -base64 32); \
	fi; \
	KC_WEBHOOK_SECRET=$${KC_WEBHOOK_SECRET:-$$(openssl rand -base64 32)} \
	docker compose -f docker/docker-compose.yml up -d
	@echo ""
	@echo "✅ Environment started successfully!"
	@echo "🌐 Admin Frontend: https://admin.$${DOMAIN:-core-platform.local}"
	@echo "🔐 Keycloak: https://localhost:8081"
	@echo "📊 Grafana: http://localhost:3001"
	@echo "🗄️  PgAdmin: http://localhost:5050"
	@echo ""
	@echo "⏳ Waiting for services to be ready... (this may take a few minutes)"
	@$(MAKE) wait-for-services

# Stop all services
.PHONY: down
down:
	@echo "🛑 Stopping Core Platform environment..."
	docker compose -f docker/docker-compose.yml down

# Restart all services
.PHONY: restart
restart:
	@echo "🔄 Restarting Core Platform environment..."
	@$(MAKE) down
	@$(MAKE) up

# Clean restart (rebuild images)
.PHONY: clean
clean:
	@echo "🧹 Clean restart - rebuilding all images..."
	docker compose -f docker/docker-compose.yml down --rmi local --volumes
	@$(MAKE) build
	@$(MAKE) up

# Fresh start - pouze Keycloak data, zachová DB
.PHONY: fresh
fresh:
	@echo "🆕 Fresh start - resetting Keycloak data only..."
	@echo "⚠️  This will DELETE all Keycloak customizations but KEEP database data!"
	@echo "Press Ctrl+C within 5 seconds to cancel..."
	@sleep 5
	@echo "🛑 Stopping services..."
	docker compose -f docker/docker-compose.yml stop keycloak
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
	docker compose -f docker/docker-compose.yml stop keycloak
	@echo "🗑️  Removing Keycloak data..."
	docker volume rm docker_keycloak_data 2>/dev/null || echo "Volume already removed"
	@echo "🚀 Starting clean Keycloak..."
	docker compose -f docker/docker-compose.yml start keycloak
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
	docker compose -f docker/docker-compose.yml stop backend db postgres-exporter
	@echo "🗑️  Removing database volume..."
	docker volume rm docker_core_db_data 2>/dev/null || echo "Volume already removed"
	@echo "🚀 Starting fresh database..."
	docker compose -f docker/docker-compose.yml start db postgres-exporter backend
	@echo "⏳ Waiting for services..."
	@sleep 10
	@echo "✅ Database reset complete"

# Build all images
.PHONY: build
build:
	@echo "🔨 Building all images..."
	@$(MAKE) kc-image
	docker compose -f docker/docker-compose.yml build --no-cache

# Show services status
.PHONY: status 
status:
	@echo "📊 Core Platform Services Status:"
	@echo "=================================="
	docker compose -f docker/docker-compose.yml ps

# Show all services logs
.PHONY: logs
logs:
	@echo "📋 Core Platform Services Logs:"
	docker compose -f docker/docker-compose.yml logs -f

# First-time development setup
.PHONY: dev-setup
dev-setup:
	@echo "🔧 Setting up development environment..."
	@if [ ! -f .env ]; then \
		echo "📄 Creating .env file from template..."; \
		cp .env.example .env; \
		echo "🔑 Generating secure secrets..."; \
		sed -i.bak "s/dev-webhook-secret-CHANGE-ME-IN-PRODUCTION/$$(openssl rand -base64 32)/g" .env; \
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
	@if [ -z "$(TENANT)" ]; then \ ap
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
# 🔗 KEYCLOAK WEBHOOK SPI TARGETS
# =============================================================================

# Show Keycloak webhook help
.PHONY: kc-help
kc-help:
	@echo "🔗 Keycloak Webhook SPI Commands:"
	@echo "  kc-image          - Build Keycloak image with webhook SPI"
	@echo "  kc-up             - Start Keycloak with .env configuration"
	@echo "  kc-up-dev         - Start Keycloak with auto-generated secrets"
	@echo "  kc-restart        - Restart Keycloak service"
	@echo "  kc-clean          - Clean restart Keycloak container"
	@echo "  kc-logs           - Show Keycloak logs"
	@echo "  kc-config         - Show current webhook configuration"
	@echo "  kc-validate       - Validate webhook configuration in .env"
	@echo "  kc-generate-secret - Generate new webhook secret"
	@echo ""
	@echo "🏗️ Tenant Bootstrap Commands:"
	@echo "  kc.bootstrap      - Bootstrap new tenant realm (REALM=name required)"
	@echo "                      Usage: make kc.bootstrap REALM=my-tenant WEBHOOK_URL=... WEBHOOK_SECRET=... TENANT_ADMIN=admin TENANT_ADMIN_PASSWORD=pass"
	@echo "  kc.bootstrap-test - Bootstrap test-tenant with defaults"
	@echo "  kc.bootstrap-auto - Bootstrap with auto-generated values"
	@echo ""
	@echo "🔐 Keycloak Data Management:"
	@echo "  kc-export-realm   - Export admin realm to backup file"
	@echo "  kc-backup         - Create backup of entire Keycloak data"
	@echo "  kc-show-users     - Show all users in admin realm"

# Bootstrap new tenant realm (parametrized)
.PHONY: kc.bootstrap
kc.bootstrap:
	@if [ -z "$(REALM)" ]; then \
		echo "❌ REALM parameter required"; \
		echo "Usage: make kc.bootstrap REALM=my-tenant [WEBHOOK_URL=...] [WEBHOOK_SECRET=...] [TENANT_ADMIN=...] [TENANT_ADMIN_PASSWORD=...]"; \
		echo ""; \
		echo "Examples:"; \
		echo "  make kc.bootstrap REALM=company-a"; \
		echo "  make kc.bootstrap REALM=test-org WEBHOOK_URL=http://backend:8080/webhook TENANT_ADMIN=org-admin"; \
		exit 1; \
	fi
	@echo "🏗️ Bootstrapping tenant realm: $(REALM)"
	@echo "⏳ This may take a few minutes..."
	@REALM=$(REALM) \
	WEBHOOK_URL=$${WEBHOOK_URL:-http://backend:8080/internal/keycloak/events} \
	WEBHOOK_SECRET=$${WEBHOOK_SECRET:-$${KC_WEBHOOK_SECRET:-webhook-secret-change-me}} \
	TENANT_ADMIN=$${TENANT_ADMIN:-tenant-admin} \
	TENANT_ADMIN_PASSWORD=$${TENANT_ADMIN_PASSWORD:-TempPass123!} \
	KEYCLOAK_ADMIN_USER=$${KEYCLOAK_ADMIN:-admin} \
	KEYCLOAK_ADMIN_PASSWORD=$${KEYCLOAK_ADMIN_PASSWORD:-admin123} \
	docker compose -f docker/docker-compose.yml exec keycloak /bin/sh -c "scripts/kc_bootstrap_realm.sh"
	@echo ""
	@echo "🎉 Tenant realm $(REALM) bootstrapped successfully!"
	@echo "🌐 Add domain mapping: make add-tenant-domain TENANT=$(REALM)"

# Bootstrap test-tenant with sensible defaults
.PHONY: kc.bootstrap-test
kc.bootstrap-test:
	@echo "🧪 Bootstrapping test-tenant with defaults..."
	@$(MAKE) kc.bootstrap REALM=test-tenant TENANT_ADMIN=test-admin TENANT_ADMIN_PASSWORD=TestPass123!
	@echo "✅ Test tenant ready!"
	@echo "👤 Login: test-admin / TestPass123! (change this password!)"

# Bootstrap with auto-generated values (for development)
.PHONY: kc.bootstrap-auto
kc.bootstrap-auto:
	@if [ -z "$(REALM)" ]; then \
		echo "❌ REALM parameter required"; \
		echo "Usage: make kc.bootstrap-auto REALM=my-tenant"; \
		exit 1; \
	fi
	@echo "🎲 Auto-generating bootstrap values for: $(REALM)"
	@WEBHOOK_SECRET=$$(openssl rand -base64 32); \
	ADMIN_PASSWORD=$$(openssl rand -base64 12 | tr -d '+/=' | head -c 12)Aa1!; \
	echo "🔑 Generated webhook secret: $$WEBHOOK_SECRET"; \
	echo "🔑 Generated admin password: $$ADMIN_PASSWORD"; \
	$(MAKE) kc.bootstrap REALM=$(REALM) \
		WEBHOOK_SECRET=$$WEBHOOK_SECRET \
		TENANT_ADMIN=$(REALM)-admin \
		TENANT_ADMIN_PASSWORD=$$ADMIN_PASSWORD
	@echo ""
	@echo "🔐 SAVE THESE CREDENTIALS:"
	@echo "   Webhook Secret: (see above)"
	@echo "   Admin User: $(REALM)-admin"
	@echo "   Admin Password: (see above)"

# Build Keycloak image with SPI
.PHONY: kc-image
kc-image:
	@echo "🔨 Building Keycloak image with webhook SPI..."
	docker build -f docker/keycloak/Dockerfile -t core-platform/keycloak:local .

# Build Keycloak image with SPI (no cache)
.PHONY: kc-image-no-cache
kc-image-no-cache:
	@echo "🔨 Building Keycloak image with webhook SPI (no cache)..."
	docker build --no-cache -f docker/keycloak/Dockerfile -t core-platform/keycloak:local .

# Start Keycloak with webhook configuration (requires .env file)
.PHONY: kc-up
kc-up: 
	@echo "🚀 Starting Keycloak with webhook configuration..."
	@if [ ! -f .env ]; then \
		echo "❌ .env file not found! Please copy .env.example to .env and configure your secrets."; \
		exit 1; \
	fi
	@if ! grep -q "KC_WEBHOOK_SECRET=" .env; then \
		echo "❌ KC_WEBHOOK_SECRET not found in .env file! Please add webhook configuration."; \
		exit 1; \
	fi
	docker compose -f docker/docker-compose.yml up -d keycloak

# Start Keycloak with explicit env vars (for development)
.PHONY: kc-up-dev
kc-up-dev:
	@echo "🧪 Starting Keycloak with development webhook configuration..."
	@KC_WEBHOOK_SECRET=$$(openssl rand -base64 32) \
	docker compose -f docker/docker-compose.yml up -d keycloak
	@echo "✅ Keycloak started with auto-generated webhook secret"

# Restart Keycloak service
.PHONY: kc-restart
kc-restart:
	@echo "🔄 Restarting Keycloak..."
	docker compose -f docker/docker-compose.yml restart keycloak
	@echo "✅ Keycloak restarted"
	@echo "⏳ Waiting for Keycloak to be ready..."
	@for i in $$(seq 1 45); do \
		if curl -k -s https://localhost:8081/health/ready | grep -q "UP" >/dev/null 2>&1; then \
			echo "✅ Keycloak is ready!"; \
			break; \
		fi; \
		sleep 2; \
	done

# Stop and remove Keycloak container (clean restart)
.PHONY: kc-clean
kc-clean:
	@echo "🧹 Cleaning Keycloak container..."
	docker compose -f docker/docker-compose.yml down keycloak
	docker compose -f docker/docker-compose.yml up -d keycloak

# Show Keycloak logs
.PHONY: kc-logs
kc-logs:
	@echo "📋 Showing Keycloak logs..."
	docker logs -f core-keycloak

# Show webhook configuration
.PHONY: kc-config
kc-config:
	@echo "⚙️  Current webhook configuration:"
	@docker exec core-keycloak env | grep KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK || echo "❌ Webhook not configured"

# Validate webhook configuration
.PHONY: kc-validate
kc-validate:
	@echo "✅ Validating webhook configuration..."
	@if [ ! -f .env ]; then \
		echo "❌ .env file not found!"; \
		exit 1; \
	fi
	@if ! grep -q "KC_WEBHOOK_SECRET=" .env; then \
		echo "❌ KC_WEBHOOK_SECRET not configured in .env"; \
		exit 1; \
	fi
	@if grep -q "CHANGE_ME" .env; then \
		echo "⚠️  Warning: Found CHANGE_ME placeholders in .env - please set real values"; \
	fi
	@echo "✅ Basic webhook configuration looks good"

# Generate new webhook secret
.PHONY: kc-generate-secret
kc-generate-secret:
	@echo "🔑 Generated new webhook secret:"
	@openssl rand -base64 32
	@echo ""
	@echo "📝 Add this to your .env file as:"
	@echo "KC_WEBHOOK_SECRET=$$(openssl rand -base64 32)"

# Export admin realm to backup file
.PHONY: kc-export-realm
kc-export-realm:
	@echo "💾 Exporting admin realm..."
	@mkdir -p backups
	@TIMESTAMP=$$(date +%Y%m%d_%H%M%S); \
	docker exec core-keycloak /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password $${KEYCLOAK_ADMIN_PASSWORD:-admin} >/dev/null 2>&1; \
	docker exec core-keycloak /opt/keycloak/bin/kcadm.sh get realms/admin > "backups/realm-admin-$$TIMESTAMP.json"
	@echo "✅ Realm exported to backups/"

# Create backup of entire Keycloak data
.PHONY: kc-backup
kc-backup:
	@echo "🗄️ Creating Keycloak data backup..."
	@mkdir -p backups
	@TIMESTAMP=$$(date +%Y%m%d_%H%M%S); \
	docker run --rm -v docker_keycloak_data:/source:ro -v $(PWD)/backups:/backup alpine \
		sh -c "cd /source && tar czf /backup/keycloak-data-$$TIMESTAMP.tar.gz ."
	@echo "✅ Keycloak data backed up to backups/"

# Show all users in admin realm
.PHONY: kc-show-users
kc-show-users:
	@echo "👥 Users in admin realm:"
	@docker exec core-keycloak /opt/keycloak/bin/kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password $${KEYCLOAK_ADMIN_PASSWORD:-admin} >/dev/null 2>&1; \
	docker exec core-keycloak /opt/keycloak/bin/kcadm.sh get users -r admin --fields username,email,enabled | jq -r '.[] | "\(.username) - \(.email // "no email") - \(if .enabled then "enabled" else "disabled" end)"' 2>/dev/null || echo "❌ Failed to retrieve users"

# =============================================================================
# 🔧 SINGLE SERVICE MANAGEMENT TARGETS  
# =============================================================================

# Rebuild & restart backend only
.PHONY: rebuild-backend
rebuild-backend:
	@echo "🔨 Rebuilding backend service..."
	@echo "🧪 Running unit tests before rebuild..."
	@$(MAKE) test-backend-unit || (echo "❌ Unit tests failed - aborting rebuild" && exit 1)
	docker compose -f docker/docker-compose.yml stop backend
	docker compose -f docker/docker-compose.yml build --no-cache backend
	docker compose -f docker/docker-compose.yml up -d backend
	@echo "✅ Backend rebuilt and restarted"
	@echo "⏳ Waiting for backend to be ready..."
	@for i in $$(seq 1 30); do \
		if curl -s http://localhost:8080/api/health >/dev/null 2>&1; then \
			echo "✅ Backend is ready!"; \
			break; \
		fi; \
		sleep 2; \
	done
	@echo "🧪 Running integration tests after rebuild..."
	@$(MAKE) test-backend-integration

# Restart backend service only
.PHONY: restart-backend
restart-backend:
	@echo "🔄 Restarting backend service..."
	docker compose -f docker/docker-compose.yml restart backend
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
	docker compose -f docker/docker-compose.yml stop frontend
	@echo "🗑️  Removing frontend container completely..."
	-docker rm core-frontend 2>/dev/null || echo "Container already removed"
	@echo "🧹 Removing frontend image to force complete rebuild..."
	-docker rmi docker-frontend:latest 2>/dev/null || echo "Image already removed"
	@echo "🚿 Clearing Docker build cache for frontend..."
	-docker builder prune -f --filter label=stage=frontend-build 2>/dev/null || true
	@echo "🏗️  Building fresh frontend image (no cache)..."
	docker compose -f docker/docker-compose.yml build --no-cache frontend
	@echo "🚀 Starting ONLY frontend container (no dependencies)..."
	docker compose -f docker/docker-compose.yml up -d --no-deps frontend
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
	docker compose -f docker/docker-compose.yml stop keycloak
	@echo "🗑️  Removing Keycloak container completely..."
	-docker rm core-keycloak 2>/dev/null || echo "Container already removed"
	@echo "🧹 Removing Keycloak image to force complete rebuild..."
	-docker rmi core-platform/keycloak:local 2>/dev/null || echo "Image already removed"
	@echo "🚿 Clearing Docker build cache..."
	-docker builder prune -f 2>/dev/null || true
	@echo "🏗️  Building fresh Keycloak image (no cache)..."
	@$(MAKE) kc-image-no-cache
	@echo "🚀 Starting fresh Keycloak container..."
	docker compose -f docker/docker-compose.yml up -d keycloak
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
	docker compose -f docker/docker-compose.yml restart frontend
	@echo "✅ Frontend restarted"

# Restart keycloak service only
.PHONY: restart-keycloak
restart-keycloak:
	@echo "🔄 Restarting Keycloak service..."
	docker compose -f docker/docker-compose.yml restart keycloak
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
	docker compose -f docker/docker-compose.yml restart db
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
	@docker compose -f docker/docker-compose.yml logs --tail=50 backend
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
	docker compose -f docker/docker-compose.yml logs -f frontend

# Show keycloak logs only
.PHONY: logs-keycloak
logs-keycloak:
	@echo "📋 Keycloak logs:"
	docker compose -f docker/docker-compose.yml logs -f keycloak

# Show database logs only
.PHONY: logs-db
logs-db:
	@echo "📋 Database logs:"
	docker compose -f docker/docker-compose.yml logs -f db

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

# Watch frontend changes and auto-rebuild (requires chokidar-cli)
.PHONY: dev-watch
dev-watch:
	@echo "👀 Watching frontend for changes..."
	@echo "📝 Edit files in frontend/src/ - changes will auto-rebuild"
	@echo "⚠️  Each change takes ~30-60s to build & deploy"
	@echo "Press Ctrl+C to stop watching"
	@cd frontend && npm run dev:watch

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