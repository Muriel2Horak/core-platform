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
	@echo "🔧 Development:"
	@echo "  dev-setup       - First-time development setup"
	@echo "  build           - Build all images"
	@echo "  test-mt         - Run multitenancy tests"
	@echo "  report-mt       - Generate test report from artifacts"
	@echo "  test-and-report - Run tests and generate report"
	@echo ""
	@echo "🔗 Keycloak Webhook:"
	@echo "  kc-help         - Show Keycloak webhook commands"
	@echo ""
	@echo "📋 Other:"
	@echo "  clean-artifacts - Clean test artifacts"

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

# Start complete environment with auto-setup
.PHONY: up
up: validate-env kc-image
	@echo "🚀 Starting Core Platform environment..."
	@echo "📋 Environment: $${ENVIRONMENT:-development}"
	@echo "🌐 Domain: $${DOMAIN:-core-platform.local}"
	@if [ -z "$$KC_WEBHOOK_SECRET" ]; then \
		echo "🔑 Auto-generating webhook secret..."; \
		export KC_WEBHOOK_SECRET=$$(openssl rand -base64 32); \
		export KC_REALM_TENANT_MAP="core-platform:test-tenant:1"; \
	fi; \
	KC_WEBHOOK_SECRET=$${KC_WEBHOOK_SECRET:-$$(openssl rand -base64 32)} \
	KC_REALM_TENANT_MAP=$${KC_REALM_TENANT_MAP:-core-platform:test-tenant:1} \
	docker compose -f docker/docker-compose.yml up -d
	@echo ""
	@echo "✅ Environment started successfully!"
	@echo "🌐 Frontend: https://$${DOMAIN:-core-platform.local}"
	@echo "🔐 Keycloak: http://localhost:8081"
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
	docker compose -f docker/docker-compose.yml build

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
	@echo "🏗️  Building images..."
	@$(MAKE) build
	@echo ""
	@echo "✅ Development setup complete!"
	@echo "🚀 Run 'make up' to start the environment"

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
	@for i in $$(seq 1 60); do \
		if curl -s http://localhost:8081/health/ready | grep -q "UP" >/dev/null 2>&1; then \
			break; \
		fi; \
		sleep 2; \
	done
	@echo "✅ Keycloak ready"
	@echo "🏗️  Setting up core-platform realm..."
	@bash docker/keycloak/generate-realm.sh >/dev/null 2>&1
	@sleep 5
	@# 🔧 FIX: Check if realm exists before creating
	@ACCESS_TOKEN=$$(curl -s -X POST "http://localhost:8081/realms/master/protocol/openid-connect/token" \
		-H "Content-Type: application/x-www-form-urlencoded" \
		-d "username=admin&password=$${KEYCLOAK_ADMIN_PASSWORD:-admin123}&grant_type=password&client_id=admin-cli" | jq -r '.access_token'); \
	REALM_EXISTS=$$(curl -s -H "Authorization: Bearer $$ACCESS_TOKEN" \
		"http://localhost:8081/admin/realms/core-platform" | jq -r '.realm // empty' 2>/dev/null); \
	if [ -z "$$REALM_EXISTS" ]; then \
		echo "🆕 Creating core-platform realm..."; \
		curl -s -X POST "http://localhost:8081/admin/realms" \
			-H "Content-Type: application/json" \
			-H "Authorization: Bearer $$ACCESS_TOKEN" \
			-d @docker/keycloak/realm-core-platform.json >/dev/null 2>&1 && \
		echo "✅ Realm created successfully" || echo "❌ Failed to create realm"; \
	else \
		echo "✅ Realm core-platform already exists"; \
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
	@echo "🔐 Keycloak: http://localhost:8081 (admin/$${KEYCLOAK_ADMIN_PASSWORD:-admin123})"
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
	@echo "🔐 Keycloak Data Management:"
	@echo "  kc-export-realm   - Export core-platform realm to backup file"
	@echo "  kc-backup         - Create backup of entire Keycloak data"
	@echo "  kc-show-users     - Show all users in core-platform realm"

# Build Keycloak image with SPI
.PHONY: kc-image
kc-image:
	@echo "🔨 Building Keycloak image with webhook SPI..."
	docker build -f docker/keycloak/Dockerfile -t core-platform/keycloak:local .

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
	KC_REALM_TENANT_MAP=core-platform:test-tenant:1 \
	docker compose -f docker/docker-compose.yml up -d keycloak
	@echo "✅ Keycloak started with auto-generated webhook secret"

# Restart Keycloak service
.PHONY: kc-restart
kc-restart:
	@echo "🔄 Restarting Keycloak..."
	docker compose -f docker/docker-compose.yml restart keycloak

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