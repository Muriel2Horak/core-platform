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

.PHONY: help test-mt report-mt test-and-report clean-artifacts backlog-new backlog-help
.PHONY: up down clean clean-fast rebuild doctor watch verify verify-full

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
	@echo "  rebuild         - Rebuild with cache (FAST ⚡)"
	@echo "  rebuild-clean   - Rebuild without cache (slow but clean)"
	@echo "  clean           - Clean restart + FULL E2E testing 🧪"
	@echo "  clean-fast      - Clean restart WITHOUT E2E (dev mode)"
	@echo ""
	@echo "🧪 Testing:"
	@echo "  test-backend          - Backend UNIT tests only (fast, 2-5 min)"
	@echo "  test-backend-full     - Backend ALL tests (unit + integration, 10-15 min)"
	@echo "  test-frontend         - Frontend unit tests"
	@echo "  test-all              - All unit tests (backend + frontend)"
	@echo "  test-mt               - Multitenancy tests"
	@echo "  test-monitoring       - Monitoring tests (deploy + runtime)"
	@echo "  test-e2e-pre          - PRE-DEPLOY smoke tests (fast gate)"
	@echo "  test-e2e-post         - POST-DEPLOY full E2E (with scaffold)"
	@echo "  test-e2e              - All E2E tests (pre + post)"
	@echo "  test-e2e-admin        - Admin CRUD E2E tests (55 tests, 3-5 min)"
	@echo "  test-e2e-sync         - Keycloak Sync E2E tests (10 tests)"
	@echo "  test-e2e-loki         - Loki monitoring UI E2E tests (LogViewer + CSV)"
	@echo "  smoke-test-loki       - Quick API validation (curl-based, 1-2 min)"
	@echo "  verify                - Quick smoke tests (health checks)"
	@echo "  verify-full           - Full integration tests"
	@echo ""
	@echo "🔍 Environment Validation:"
	@echo "  env-validate    - Quick .env validation (file exists, vars set)"
	@echo "  doctor          - Full health check (.env + service connectivity)"
	@echo ""
	@echo "� Backlog Management:"
	@echo "  backlog-new     - Create new User Story from template"
	@echo "  backlog-help    - Show backlog commands & examples"
	@echo ""
	@echo "�💡 Note: 'make rebuild' runs UNIT tests only (fast)"
	@echo "         'make test-backend-full' runs ALL tests (needs Docker)"
	@echo "         'make clean' runs FULL E2E pipeline (PRE + POST)"
	@echo "         PRE-DEPLOY E2E: Fast smoke tests (5-7 min)"
	@echo "         POST-DEPLOY E2E: Full scenarios (20-30 min)"
	@echo "         Use 'make clean-fast' for dev rebuild without E2E"
	@echo ""
	@echo "📚 More: make help-advanced"

# Advanced help
.PHONY: help-advanced
help-advanced:
	@echo "🔧 Advanced Commands:"
	@echo ""
	@echo "📊 Monitoring & Grafana:"
	@echo "  test-monitoring-deploy      - Pre-deploy config validation"
	@echo "  test-monitoring-runtime     - Post-deploy runtime tests"
	@echo "  test-monitoring             - Full monitoring test suite"
	@echo "  test-grafana-sso            - Grafana SSO sanity test (provisioning + org)"
	@echo "  diag-grafana-provisioning   - Grafana provisioning diagnostics"
	@echo "  validate-dashboard-structure - Validate dashboard file structure"
	@echo ""
	@echo "🎭 E2E Testing (Two-Tier):"
	@echo "  e2e-setup           - Install E2E dependencies + Playwright"
	@echo "  test-e2e-pre        - PRE-DEPLOY smoke tests (5-7 min)"
	@echo "  test-e2e-post       - POST-DEPLOY full E2E (20-30 min)"
	@echo "  test-e2e            - All E2E tests (pre + post)"
	@echo "  test-e2e-admin      - Admin CRUD E2E tests (55 tests, 3-5 min)"
	@echo "  test-e2e-sync       - Keycloak Sync E2E tests (10 tests)"
	@echo "  e2e-scaffold        - Create test data only"
	@echo "  e2e-teardown        - Cleanup test data only"
	@echo "  e2e-report          - Open HTML test report"
	@echo ""
	@echo "🏗️ Build Modes:"
	@echo "  clean               - Clean rebuild + FULL E2E (PRE + POST)"
	@echo "  clean-fast          - Clean rebuild WITHOUT E2E (dev mode)"
	@echo "  rebuild             - Fast rebuild (cache, no E2E)"
	@echo "  rebuild RUN_E2E_PRE=true   - Rebuild with PRE-DEPLOY E2E"
	@echo "  rebuild RUN_E2E_FULL=true  - Rebuild with ALL E2E tests"
	@echo ""
	@echo "� CI/CD Pipeline:"
	@echo "  ci-test-pipeline    - Full CI pipeline (unit + E2E gate)"
	@echo "  ci-post-deploy      - Post-deployment validation"
	@echo "  test-comprehensive  - Comprehensive test suite"
	@echo ""
	@echo "�🐳 Single Service Management:"
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
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🧪 MULTITENANCY SMOKE TESTS                                   ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@bash tests/multitenancy_smoke.sh 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/\[OK\]/✅/g' | \
		sed 's/\[FAIL\]/❌/g' | \
		sed 's/PASS/✅ PASS/g' | \
		sed 's/FAIL/❌ FAIL/g'
	@echo ""

# Generate report from test artifacts
report-mt:
	@echo "📊 Generating test report..."
	@bash tests/make_report.sh
	@echo "✅ Report generated: ./TEST_REPORT.md"

# Run tests and generate report
test-and-report: test-mt report-mt
	@echo ""
	@echo "🎉 Tests completed and report generated!"
	@echo "📄 Report: ./TEST_REPORT.md"

# Clean test artifacts
clean-artifacts:
	@echo "🧹 Cleaning test artifacts..."
	@rm -rf artifacts/
	@rm -f TEST_REPORT.md
	@echo "✅ Artifacts cleaned"

# Quick smoke tests (health checks only)
.PHONY: verify
verify:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🔍 QUICK SMOKE TESTS (HEALTH CHECKS)                         ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@bash scripts/build/post-deployment-check.sh 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/\[OK\]/✅/g' | \
		sed 's/\[FAIL\]/❌/g' | \
		sed 's/\[PASS\]/✅/g' | \
		sed 's/Checking/▶️  Checking/g'
	@echo ""
	@echo "✅ Smoke tests completed!"
	@echo ""

# Environment validation (quick check)
.PHONY: env-validate
env-validate:
	@bash scripts/env-validate.sh

# Full environment doctor check (validation + connectivity)
.PHONY: doctor
doctor:
	@bash scripts/env-validate.sh --full

# Full integration tests (includes multitenancy and streaming)
.PHONY: verify-full
verify-full:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🧪 FULL INTEGRATION TESTS                                     ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "▶️  [1/2] Running integration tests..."
	@RUN_FULL_TESTS=true bash scripts/build/post-deployment-check.sh 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/\[OK\]/✅/g' | \
		sed 's/\[FAIL\]/❌/g' | \
		sed 's/\[PASS\]/✅/g'
	@echo ""
	@echo "▶️  [2/2] Generating detailed report..."
	@$(MAKE) test-and-report
	@echo ""
	@echo "🎉 Full integration tests completed!"
	@echo "📊 Report: ./TEST_REPORT.md"
	@echo ""

# =============================================================================
# 🐳 DEV CONTAINER TARGETS (Hot Reload - DOPORUČENO)
# =============================================================================

# Start Dev Container environment with hot reload
.PHONY: dev-up
dev-up:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🐳 DEV ENVIRONMENT STARTUP                                    ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "📋 Режим Hot Reload:"
	@echo "   • Backend: Spring DevTools auto-restart (2-5s)"
	@echo "   • Frontend: Vite watch + nginx (3-7s)"
	@echo "   • První build: ~3-5 minut (jednou)"
	@echo ""
	@echo "▶️  Starting Docker Compose..."
	@docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env up -d 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/Container .* Started/  ✅ Container started/g' | \
		sed 's/Container .* Starting/  ⏳ Container starting/g'
	@echo ""
	@echo "✅ Dev prostředí běží!"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "📍 Access Points:"
	@echo "   🌐 Frontend:  https://core-platform.local/"
	@echo "   🔌 API:       https://core-platform.local/api"
	@echo "   🔐 Keycloak:  http://localhost:8081/admin/"
	@echo "   📊 Grafana:   http://localhost:3001/"
	@echo ""
	@echo "🐛 Debug: localhost:5005 (VS Code F5)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@echo "💡 Next Steps:"
	@echo "   1. make dev-check     - Health check"
	@echo "   2. make logs-backend  - Watch logs"
	@echo "   3. Edit code → auto-rebuild! 🚀"
	@echo ""

# Start with watch mode (foreground)
.PHONY: dev-watch
dev-watch:
	@echo "👀 Starting watch mode (foreground)..."
	@docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env watch

# Stop Dev Container
.PHONY: dev-down
dev-down:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🛑 STOPPING DEV ENVIRONMENT                                   ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "▶️  Stopping containers..."
	@docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env down 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/Container .* Stopped/  ✅ Container stopped/g' | \
		sed 's/Container .* Stopping/  ⏳ Container stopping/g'
	@echo ""
	@echo "✅ Dev environment stopped successfully!"
	@echo ""

# Restart dev services
.PHONY: dev-restart
dev-restart:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🔄 RESTARTING DEV SERVICES                                    ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "▶️  Restarting all containers..."
	@docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env restart 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/Container .* Restarted/  ✅ Container restarted/g' | \
		sed 's/Container .* Restarting/  ⏳ Container restarting/g'
	@echo ""
	@echo "✅ All dev services restarted!"
	@echo "💡 Use 'make dev-check' to verify health"
	@echo ""

# Clean restart dev environment
.PHONY: dev-clean
dev-clean: check-registries
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🧹 CLEAN DEV RESTART (WITH REBUILD)                          ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "⚠️  This will:"
	@echo "   • Stop all containers"
	@echo "   • Remove volumes (data will be lost!)"
	@echo "   • Rebuild images (with cache)"
	@echo "   • Start fresh environment"
	@echo ""
	@echo "▶️  [1/3] Stopping and removing containers + volumes..."
	@docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env down -v 2>&1 | \
		grep -v "^\[DEBUG\]" | tail -5
	@echo "  ✅ Cleanup complete"
	@echo ""
	@echo "▶️  [2/3] Rebuilding images..."
	@docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env build 2>&1 | \
		grep -E "(Building|built|CACHED)" | tail -10
	@echo "  ✅ Build complete"
	@echo ""
	@echo "▶️  [3/3] Starting environment..."
	@$(MAKE) dev-up
	@echo ""
	@echo "🎉 Clean restart completed!"
	@echo ""

# Health check
.PHONY: dev-check
dev-check:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🧪 DEV ENVIRONMENT HEALTH CHECK                               ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@bash scripts/devcontainer/test-env-check.sh 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/\[OK\]/✅/g' | \
		sed 's/\[FAIL\]/❌/g' | \
		sed 's/\[WARN\]/⚠️ /g' || echo "⚠️  Some checks failed (see details above)"
	@echo ""

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
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🚀 PRODUCTION ENVIRONMENT STARTUP                             ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "🔍 Checking Docker daemon..."
	@if ! docker info > /dev/null 2>&1; then \
		echo ""; \
		echo "❌ Docker daemon is not running!"; \
		echo ""; \
		echo "Please start Docker Desktop and try again."; \
		echo ""; \
		exit 1; \
	fi
	@echo "✅ Docker daemon is running"
	@echo ""
	@echo ">>> starting compose up at $(BUILD_TS)"
	@echo "📋 Environment: $${ENVIRONMENT:-development}"
	@echo "🌐 Domain: $${DOMAIN:-core-platform.local}"
	@echo ""
	@echo "🔨 Building Docker images..."
	@BUILD_OUTPUT=$$(DOCKER_BUILDKIT=1 docker compose -f docker/docker-compose.yml --env-file .env build 2>&1); \
	BUILD_EXIT_CODE=$$?; \
	echo "$$BUILD_OUTPUT" | grep -v "^\[DEBUG\]" | sed 's/^/  /'; \
	if [ $$BUILD_EXIT_CODE -ne 0 ]; then \
		echo ""; \
		echo "❌ Docker build failed with exit code $$BUILD_EXIT_CODE"; \
		echo ""; \
		echo "🔍 Check the build output above for details"; \
		echo "💡 Common issues:"; \
		echo "   - Missing dependencies in package.json"; \
		echo "   - Import errors (wrong module names)"; \
		echo "   - TypeScript compilation errors"; \
		echo ""; \
		exit 1; \
	fi
	@echo ""
	@echo "✅ Build successful"
	@echo ""
	@echo "▶️  Starting Docker Compose..."
	@docker compose -f docker/docker-compose.yml --env-file .env up -d --remove-orphans 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/Container .* Started/  ✅ Container started/g' | \
		sed 's/Container .* Starting/  ⏳ Container starting/g'
	@echo ""
	@echo "⏳ Waiting for containers to start..."
	@sleep 5
	@FAILED_CONTAINERS=$$(docker compose -f docker/docker-compose.yml ps --filter "status=exited" --format "{{.Service}}" 2>/dev/null); \
	if [ -n "$$FAILED_CONTAINERS" ]; then \
		echo ""; \
		echo "❌ Some containers failed to start:"; \
		echo "$$FAILED_CONTAINERS" | sed 's/^/   - /'; \
		echo ""; \
		echo "🔍 Check logs with: make logs"; \
		exit 1; \
	fi
	@echo "✅ All containers started"
	@echo ""
	@echo "⏳ Waiting for backend to be ready (this may take several minutes)..."
	@MAX_WAIT=420; \
	ELAPSED=0; \
	while [ $$ELAPSED -lt $$MAX_WAIT ]; do \
		if docker exec core-backend curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then \
			echo "✅ Backend is ready (took $${ELAPSED}s)"; \
			break; \
		fi; \
		if [ $$ELAPSED -eq 0 ]; then \
			echo "   Backend is starting up, please wait..."; \
		elif [ $$((ELAPSED % 30)) -eq 0 ]; then \
			echo "   Still waiting... ($${ELAPSED}s elapsed)"; \
		fi; \
		sleep 5; \
		ELAPSED=$$((ELAPSED + 5)); \
		if [ $$ELAPSED -ge $$MAX_WAIT ]; then \
			echo ""; \
			echo "❌ Backend failed to become ready within $${MAX_WAIT}s"; \
			echo "🔍 Check logs with: docker logs core-backend"; \
			exit 1; \
		fi; \
	done
	@echo "✅ Environment started successfully!"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "📍 Access Points:"
	@echo "   🌐 Admin Frontend: https://admin.$${DOMAIN:-core-platform.local}"
	@echo "   🔐 Keycloak:       https://localhost:8081"
	@echo "   📊 Grafana:        http://localhost:3001"
	@echo "   🗄️  PgAdmin:        http://localhost:5050"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@echo "⏳ Waiting for services to be ready... (this may take a few minutes)"
	@scripts/build/wait-healthy.sh --timeout 180
	@echo ""
	@echo "🧪 Running post-deployment checks..."
	@bash scripts/build/post-deployment-check.sh
	@echo ""

# Rebuild with progress tracking (called internally)
.PHONY: _rebuild_with_progress
_rebuild_with_progress:
	@OFFSET=$${STEP_OFFSET:-0}; \
	CURRENT=$$((1 + OFFSET)); \
	\
	bash scripts/build/build-progress-tracker.sh update $$CURRENT "IN_PROGRESS" ""; \
	PRE_START=$$(date +%s); \
	if bash scripts/build/pre-build-test.sh all $$CURRENT; then \
		PRE_END=$$(date +%s); \
		PRE_TIME=$$((PRE_END - PRE_START)); \
		bash scripts/build/build-progress-tracker.sh update $$CURRENT "DONE" "$${PRE_TIME}s"; \
	else \
		PRE_END=$$(date +%s); \
		PRE_TIME=$$((PRE_END - PRE_START)); \
		bash scripts/build/build-progress-tracker.sh update $$CURRENT "FAILED" "$${PRE_TIME}s"; \
		bash scripts/build/build-progress-tracker.sh error $$CURRENT "$$(ls -t diagnostics/tests/error-summary-*.md 2>/dev/null | head -1)"; \
		exit 1; \
	fi; \
	sleep 0.5; \
	\
	CURRENT=$$((CURRENT + 1)); \
	bash scripts/build/build-progress-tracker.sh update $$CURRENT "IN_PROGRESS" ""; \
	BUILD_START=$$(date +%s); \
	BUILD_LOG_FILE="/tmp/docker-build-$$$$.log"; \
	BUILD_EXIT_CODE=0; \
	if [ "$${NO_CACHE:-false}" = "true" ]; then \
		echo "🔨 Building with --no-cache (ensures all code changes included)..."; \
		echo "📊 Streaming build progress (this may take 5-10 minutes)..."; \
		echo ""; \
		DOCKER_BUILDKIT=1 docker compose -f docker/docker-compose.yml --env-file .env build --parallel --no-cache 2>&1 | tee $$BUILD_LOG_FILE | \
			grep --line-buffered -E "(#[0-9]+ |Building|built|CACHED|exporting|ERROR|failed|finished|Step [0-9]+/|RUN |COPY )" | \
			sed -u 's/^/  /'; \
		BUILD_EXIT_CODE=$${PIPESTATUS[0]}; \
	else \
		echo "🔨 Building with cache (faster but may miss changes)..."; \
		echo "📊 Streaming build progress..."; \
		echo ""; \
		DOCKER_BUILDKIT=1 docker compose -f docker/docker-compose.yml --env-file .env build --parallel 2>&1 | tee $$BUILD_LOG_FILE | \
			grep --line-buffered -E "(#[0-9]+ |Building|built|CACHED|exporting|ERROR|failed|finished|Step [0-9]+/|RUN |COPY )" | \
			sed -u 's/^/  /'; \
		BUILD_EXIT_CODE=$${PIPESTATUS[0]}; \
	fi; \
	BUILD_END=$$(date +%s); \
	BUILD_TIME=$$((BUILD_END - BUILD_START)); \
	if [ $$BUILD_EXIT_CODE -ne 0 ]; then \
		bash scripts/build/build-progress-tracker.sh update $$CURRENT "FAILED" "$${BUILD_TIME}s"; \
		echo ""; \
		echo "❌ Docker build failed with exit code $$BUILD_EXIT_CODE"; \
		echo ""; \
		echo "🔍 Last 50 lines of build output:"; \
		tail -50 $$BUILD_LOG_FILE; \
		echo ""; \
		rm -f $$BUILD_LOG_FILE; \
		exit 1; \
	fi; \
	rm -f $$BUILD_LOG_FILE; \
	bash scripts/build/build-progress-tracker.sh update $$CURRENT "DONE" "$${BUILD_TIME}s"; \
	sleep 0.5; \
	\
	CURRENT=$$((CURRENT + 1)); \
	bash scripts/build/build-progress-tracker.sh update $$CURRENT "IN_PROGRESS" ""; \
	START_SVC=$$(date +%s); \
	if $(MAKE) up 2>&1 | tail -20; then \
		END_SVC=$$(date +%s); \
		SVC_TIME=$$((END_SVC - START_SVC)); \
		bash scripts/build/build-progress-tracker.sh update $$CURRENT "DONE" "$${SVC_TIME}s"; \
	else \
		END_SVC=$$(date +%s); \
		SVC_TIME=$$((END_SVC - START_SVC)); \
		bash scripts/build/build-progress-tracker.sh update $$CURRENT "FAILED" "$${SVC_TIME}s"; \
		echo "❌ Failed to start services"; \
		exit 1; \
	fi; \
	sleep 0.5; \
	\
	if [ "$${RUN_E2E_FULL:-false}" = "true" ]; then \
		CURRENT=$$((CURRENT + 1)); \
		bash scripts/build/build-progress-tracker.sh update $$CURRENT "IN_PROGRESS" ""; \
		E2E_PRE_START=$$(date +%s); \
		if $(MAKE) test-e2e-pre; then \
			E2E_PRE_END=$$(date +%s); \
			E2E_PRE_TIME=$$((E2E_PRE_END - E2E_PRE_START)); \
			bash scripts/build/build-progress-tracker.sh update $$CURRENT "DONE" "$${E2E_PRE_TIME}s"; \
		else \
			E2E_PRE_END=$$(date +%s); \
			E2E_PRE_TIME=$$((E2E_PRE_END - E2E_PRE_START)); \
			bash scripts/build/build-progress-tracker.sh update $$CURRENT "FAILED" "$${E2E_PRE_TIME}s"; \
			exit 1; \
		fi; \
		sleep 0.5; \
		\
		CURRENT=$$((CURRENT + 1)); \
		bash scripts/build/build-progress-tracker.sh update $$CURRENT "IN_PROGRESS" ""; \
		E2E_POST_START=$$(date +%s); \
		if $(MAKE) test-e2e-post; then \
			E2E_POST_END=$$(date +%s); \
			E2E_POST_TIME=$$((E2E_POST_END - E2E_POST_START)); \
			bash scripts/build/build-progress-tracker.sh update $$CURRENT "DONE" "$${E2E_POST_TIME}s"; \
		else \
			E2E_POST_END=$$(date +%s); \
			E2E_POST_TIME=$$((E2E_POST_END - E2E_POST_START)); \
			bash scripts/build/build-progress-tracker.sh update $$CURRENT "FAILED" "$${E2E_POST_TIME}s"; \
			exit 1; \
		fi; \
	elif [ "$${RUN_E2E_PRE:-false}" = "true" ]; then \
		CURRENT=$$((CURRENT + 1)); \
		bash scripts/build/build-progress-tracker.sh update $$CURRENT "IN_PROGRESS" ""; \
		E2E_PRE_START=$$(date +%s); \
		if $(MAKE) test-e2e-pre; then \
			E2E_PRE_END=$$(date +%s); \
			E2E_PRE_TIME=$$((E2E_PRE_END - E2E_PRE_START)); \
			bash scripts/build/build-progress-tracker.sh update $$CURRENT "DONE" "$${E2E_PRE_TIME}s"; \
		else \
			E2E_PRE_END=$$(date +%s); \
			E2E_PRE_TIME=$$((E2E_PRE_END - E2E_PRE_START)); \
			bash scripts/build/build-progress-tracker.sh update $$CURRENT "FAILED" "$${E2E_PRE_TIME}s"; \
			exit 1; \
		fi; \
	fi; \
	echo ""; \
	echo "🎉 Pipeline completed successfully!"; \
	echo ""

# Production rebuild with Build Doctor
rebuild: check-registries
	@REBUILD_START=$$(date +%s); \
	scripts/build/wrapper.sh $(MAKE) _rebuild_inner 2>&1 | tee -a $(LOG_FILE); \
	EXIT_CODE=$$?; \
	bash scripts/build/build-summary.sh "$(LOG_FILE)" "$$REBUILD_START" "rebuild"; \
	exit $$EXIT_CODE

_rebuild_inner:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🏗️  PRODUCTION REBUILD (WITH CACHE)                          ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo ">>> rebuilding at $(BUILD_TS)"
	@if [ "$${RUN_E2E_FULL:-false}" = "true" ]; then \
		echo "📋 Mode: FULL E2E TESTING"; \
	elif [ "$${RUN_E2E_PRE:-false}" = "true" ]; then \
		echo "📋 Mode: PRE-DEPLOY E2E ONLY"; \
	else \
		echo "📋 Mode: SMOKE TESTS ONLY (no E2E)"; \
	fi
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "📍 STEP 1/6: Pre-Build Tests (unit tests before Docker)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@bash scripts/build/pre-build-test.sh all || (echo "" && echo "❌ STOPPED: Fix tests above or skip with SKIP_TEST_CLASSES" && exit 1)
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "📍 STEP 2/6: Building Docker Images"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@BUILD_LOG_FILE="/tmp/docker-build-rebuild-$$$$.log"; \
	echo "📊 Streaming build progress (backend + frontend in parallel)..."; \
	echo ""; \
	DOCKER_BUILDKIT=1 docker compose -f docker/docker-compose.yml --env-file .env build --parallel 2>&1 | tee $$BUILD_LOG_FILE | \
		grep --line-buffered -E "(#[0-9]+ |Building|built|CACHED|exporting|ERROR|failed|finished|Step [0-9]+/|RUN |COPY )" | \
		sed -u 's/^/  /'; \
	BUILD_EXIT_CODE=$${PIPESTATUS[0]}; \
	if [ $$BUILD_EXIT_CODE -ne 0 ]; then \
		echo ""; \
		echo "❌ Docker build failed with exit code $$BUILD_EXIT_CODE"; \
		echo ""; \
		echo "🔍 Last 50 lines of build output:"; \
		tail -50 $$BUILD_LOG_FILE; \
		echo ""; \
		echo "💡 Common issues:"; \
		echo "   - Missing dependencies in package.json"; \
		echo "   - Import errors (wrong module names)"; \
		echo "   - TypeScript compilation errors"; \
		echo "   - Java compilation errors"; \
		echo ""; \
		rm -f $$BUILD_LOG_FILE; \
		exit 1; \
	fi; \
	rm -f $$BUILD_LOG_FILE
	@echo "  ✅ Images built successfully"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "📍 STEP 3/6: Starting Services"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@$(MAKE) up
	@echo ""
	@if [ "$${RUN_E2E_FULL:-false}" = "true" ]; then \
		echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; \
		echo "📍 STEP 4/6: Pre-Deploy E2E Tests (smoke)"; \
		echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; \
		$(MAKE) test-e2e-pre || (echo "" && echo "❌ STOPPED: E2E pre-deploy failed"; exit 1); \
		echo ""; \
		echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; \
		echo "📍 STEP 5/6: Post-Deploy E2E Tests (full scenarios)"; \
		echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; \
		$(MAKE) test-e2e-post || (echo "" && echo "❌ STOPPED: E2E post-deploy failed"; exit 1); \
		echo ""; \
		echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; \
		echo "📍 STEP 6/6: All Tests Completed ✅"; \
		echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; \
	elif [ "$${RUN_E2E_PRE:-false}" = "true" ]; then \
		echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; \
		echo "📍 STEP 4/6: Pre-Deploy E2E Tests"; \
		echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"; \
		$(MAKE) test-e2e-pre || (echo "" && echo "❌ STOPPED: E2E tests failed"; exit 1); \
		echo ""; \
		echo "⏭️  STEP 5/6: POST-DEPLOY E2E skipped (set RUN_E2E_FULL=true to enable)"; \
		echo "⏭️  STEP 6/6: Skipped"; \
	else \
		echo "⏭️  STEP 4/6: E2E tests skipped (set RUN_E2E_PRE=true or RUN_E2E_FULL=true)"; \
		echo "⏭️  STEP 5/6: Skipped"; \
		echo "⏭️  STEP 6/6: Skipped"; \
	fi
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "🎉 REBUILD COMPLETE - All steps passed!"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""

# Force rebuild without cache (slower but ensures clean build)
.PHONY: rebuild-clean
rebuild-clean: check-registries
	@NO_CACHE=true scripts/build/wrapper.sh $(MAKE) _rebuild_inner 2>&1 | tee -a $(LOG_FILE)

# Clean with Build Doctor (FULL E2E TESTING)
clean: check-registries
	@CLEAN_START=$$(date +%s); \
	bash scripts/build/auto-split.sh "NO_CACHE=true SKIP_TEST_CLASSES=TenantFilterIntegrationTest,QueryDeduplicatorTest,MonitoringProxyServiceTest,PresenceServiceIntegrationTest,ReportingPropertiesTest,WorkflowVersionServiceTest RUN_E2E_FULL=true scripts/build/wrapper.sh $(MAKE) _clean_inner"; \
	EXIT_CODE=$$?; \
	bash scripts/build/build-summary.sh "$(LOG_FILE)" "$$CLEAN_START" "clean"; \
	exit $$EXIT_CODE

_clean_inner:
	@# Build dynamic step list and initialize tracker
	@E2E_STEPS=""; \
	if [ "$${RUN_E2E_FULL:-false}" = "true" ]; then \
		E2E_STEPS="\"E2E pre-deploy\" \"E2E post-deploy\""; \
	elif [ "$${RUN_E2E_PRE:-false}" = "true" ]; then \
		E2E_STEPS="\"E2E pre-deploy\""; \
	fi; \
	eval "bash scripts/build/build-progress-tracker.sh init \"MAKE CLEAN - FULL PIPELINE\" \
		\"Cleanup\" \"Pre-build tests\" \"Build images\" \"Start services\" $$E2E_STEPS"; \
	sleep 1; \
	\
	bash scripts/build/build-progress-tracker.sh update 1 "IN_PROGRESS" ""; \
	CLEANUP_START=$$(date +%s); \
	echo "🧹 Cleaning up containers, volumes and images..."; \
	docker compose -f docker/docker-compose.yml --env-file .env down --rmi local --volumes 2>&1 | \
		grep -v "^\[DEBUG\]" | tail -10; \
	echo "🧹 Cleaning Docker buildkit cache..."; \
	docker builder prune -f 2>&1 | tail -5; \
	echo "🧹 Cleaning local frontend/dist directory..."; \
	rm -rf frontend/dist; \
	CLEANUP_END=$$(date +%s); \
	CLEANUP_TIME=$$((CLEANUP_END - CLEANUP_START)); \
	bash scripts/build/build-progress-tracker.sh update 1 "DONE" "$${CLEANUP_TIME}s"; \
	sleep 0.5; \
	\
	$(MAKE) _rebuild_with_progress RUN_E2E_FULL=$${RUN_E2E_FULL:-false} RUN_E2E_PRE=$${RUN_E2E_PRE:-false} STEP_OFFSET=1
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "🎉 CLEAN RESTART COMPLETE - Full stack tested!"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""

# Fast clean without E2E (for development)
# Use PROGRESS_SPLIT=no to disable split-pane mode
.PHONY: clean-fast
clean-fast: check-registries
	@CLEAN_START=$$(date +%s); \
	bash scripts/build/auto-split.sh "scripts/build/wrapper.sh $(MAKE) _clean_fast_inner"; \
	EXIT_CODE=$$?; \
	bash scripts/build/build-summary.sh "$(LOG_FILE)" "$$CLEAN_START" "clean-fast"; \
	exit $$EXIT_CODE

_clean_fast_inner:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🧹 FAST CLEAN (NO E2E TESTS - DEV MODE)                      ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo ">>> fast cleaning at $(BUILD_TS)"
	@echo "⚠️  WARNING: This will DELETE all volumes and data!"
	@echo "📋 Testing: PRE-BUILD unit tests + POST-DEPLOY smoke tests only"
	@echo ""
	@echo "▶️  Removing containers, images, and volumes..."
	@docker compose -f docker/docker-compose.yml --env-file .env down --rmi local --volumes 2>&1 | \
		grep -v "^\[DEBUG\]" | tail -10
	@echo "▶️  Cleaning Docker buildkit cache..."
	@docker builder prune -f 2>&1 | tail -5
	@echo "▶️  Cleaning local frontend/dist directory..."
	@rm -rf frontend/dist
	@echo "  ✅ Cleanup complete"
	@echo ""
	@echo "▶️  Rebuilding from scratch (no E2E)..."
	@$(MAKE) rebuild
	@echo ""
	@echo "🎉 Fast clean restart completed!"
	@echo ""

# Crashloop watcher
watch:
	@echo "👁️  Watching for crashloops (Ctrl+C to stop)..."
	@bash scripts/build/watch-crashloop.sh

# Stop all services
down:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🛑 STOPPING PRODUCTION ENVIRONMENT                            ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "▶️  Stopping all containers..."
	@docker compose -f docker/docker-compose.yml --env-file .env down 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/Container .* Stopped/  ✅ Container stopped/g' | \
		sed 's/Container .* Stopping/  ⏳ Container stopping/g'
	@echo ""
	@echo "✅ All services stopped successfully!"
	@echo ""

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

# =============================================================================
# 🌐 DOCKER REGISTRY VALIDATION
# =============================================================================

# Check if Docker registries are accessible
.PHONY: check-registries
check-registries:
	@echo "🌐 Checking Docker registry availability..."
	@echo -n "  📦 Docker Hub (docker.io)... "
	@if curl -s --max-time 5 -I https://registry-1.docker.io/v2/ | grep -q "HTTP/[12]"; then \
		echo "✅ OK"; \
	else \
		echo "❌ UNAVAILABLE"; \
		echo ""; \
		echo "⚠️  ERROR: Docker Hub (docker.io) is not accessible!"; \
		echo "   This will cause build failures."; \
		echo ""; \
		echo "   Please wait a few minutes and try again."; \
		echo "   You can check status at: https://status.docker.com/"; \
		exit 1; \
	fi
	@echo -n "  📦 Quay.io (quay.io)... "
	@if curl -s --max-time 5 -I https://quay.io/v2/keycloak/keycloak/manifests/24.0.4 | grep -q "HTTP/[12]"; then \
		echo "✅ OK"; \
	else \
		echo "❌ UNAVAILABLE"; \
		echo ""; \
		echo "⚠️  ERROR: Quay.io registry is not accessible!"; \
		echo "   This will cause Keycloak build to fail."; \
		echo ""; \
		echo "   Please wait a few minutes and try again."; \
		echo "   You can check status at: https://status.quay.io/"; \
		exit 1; \
	fi
	@echo "✅ All Docker registries are accessible"
	@echo ""

# Build all images
.PHONY: build
build: check-registries
	@echo "🔨 Building all images (with cache)..."
	@$(MAKE) kc-image
	docker compose -f docker/docker-compose.yml --env-file .env build

# Show services status
.PHONY: status 
status:
	@echo "📊 Core Platform Services Status:"
	@echo "=================================="
	docker compose -f docker/docker-compose.yml --env-file .env ps

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
		echo "💡 Copy from template: cp .env.template .env"; \
		echo "💡 Then fill in ALL required values and run: make env-validate"; \
		exit 1; \
	fi
	@echo "🔍 Running comprehensive environment validation..."
	@if ! bash scripts/env-validate.sh >/dev/null 2>&1; then \
		echo ""; \
		echo "❌ Environment validation FAILED!"; \
		echo ""; \
		echo "📋 Run 'make env-validate' to see detailed errors"; \
		echo ""; \
		echo "⚠️  CRITICAL: docker-compose.yml NO LONGER has fallbacks for credentials!"; \
		echo "   ALL required variables MUST be set in .env before build."; \
		echo ""; \
		exit 1; \
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
kc-image: check-registries
	@echo "🔨 Generating realm configuration from template..."
	@bash docker/keycloak/generate-realm.sh
	@echo "🔨 Building Keycloak image..."
	docker build -f docker/keycloak/Dockerfile -t core-platform/keycloak:local .

# Build Keycloak image (no cache)
.PHONY: kc-image-no-cache
kc-image-no-cache: check-registries
	@echo "🔨 Generating realm configuration from template..."
	@bash docker/keycloak/generate-realm.sh
	@echo "🔨 Building Keycloak image (no cache)..."
	docker build --no-cache -f docker/keycloak/Dockerfile -t core-platform/keycloak:local .

# =============================================================================
# 🔧 SINGLE SERVICE MANAGEMENT TARGETS  
# =============================================================================

# Rebuild & restart backend only
.PHONY: rebuild-backend
rebuild-backend:
	@echo "🔨 Rebuilding backend service (NO CACHE - ensures code changes are included)..."
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

# Fast rebuild backend WITH cache (for iterations without code changes)
.PHONY: rebuild-backend-fast
rebuild-backend-fast:
	@echo "🔨 Fast rebuilding backend service (WITH CACHE)..."
	@echo "⚠️  WARNING: This uses Docker cache - code changes may be ignored!"
	@echo "⚠️  Use 'make rebuild-backend' if you have code changes."
	docker compose -f docker/docker-compose.yml --env-file .env stop backend
	docker compose -f docker/docker-compose.yml --env-file .env build backend
	docker compose -f docker/docker-compose.yml --env-file .env up -d backend
	@echo "✅ Backend fast rebuilt and restarted"

# Legacy alias (kept for backwards compatibility)
.PHONY: rebuild-backend-clean
rebuild-backend-clean: rebuild-backend

# Build backend on HOST (fastest for development iterations)
.PHONY: build-backend
build-backend:
	@echo "🔨 Building backend on host (Maven)..."
	@cd backend && ./mvnw clean package -DskipTests
	@echo "✅ Backend build complete - JAR in backend/target/"
	@echo "💡 Tip: Use 'make restart-backend' to restart with new JAR"

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
	@echo "🔨 Rebuilding frontend service (NO CACHE - ensures code changes are included)..."
	@echo "🛑 Stopping frontend service only..."
	docker compose -f docker/docker-compose.yml --env-file .env stop frontend
	@echo "🗑️  Removing frontend container completely..."
	-docker rm core-frontend 2>/dev/null || echo "Container already removed"
	@echo "🧹 Removing frontend image to force complete rebuild..."
	-docker rmi docker-frontend:latest 2>/dev/null || echo "Image already removed"
	@echo "🏗️  Building fresh frontend image (NO CACHE)..."
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

# Fast rebuild frontend WITH cache (for iterations without code changes)
.PHONY: rebuild-frontend-fast
rebuild-frontend-fast:
	@echo "🔨 Fast rebuilding frontend service (WITH CACHE)..."
	@echo "⚠️  WARNING: This uses Docker cache - code changes may be ignored!"
	@echo "⚠️  Use 'make rebuild-frontend' if you have code changes."
	docker compose -f docker/docker-compose.yml --env-file .env stop frontend
	-docker rm core-frontend 2>/dev/null || echo "Container already removed"
	-docker rmi docker-frontend:latest 2>/dev/null || echo "Image already removed"
	docker compose -f docker/docker-compose.yml --env-file .env build frontend
	docker compose -f docker/docker-compose.yml --env-file .env up -d --no-deps frontend
	@echo "✅ Frontend fast rebuilt and restarted"

# Legacy alias (kept for backwards compatibility)
.PHONY: rebuild-frontend-clean
rebuild-frontend-clean: rebuild-frontend

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

# Build frontend on HOST (fastest for development iterations)
.PHONY: build-frontend
build-frontend:
	@echo "🔨 Building frontend on host (npm run build)..."
	@cd frontend && npm run build
	@echo "✅ Frontend build complete - files in frontend/dist/"
	@echo "💡 Tip: Use 'make dev-hot' to copy to running container"

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

# Legacy alias (kept for backwards compatibility)
.PHONY: dev-build
dev-build: build-frontend

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
	@cd backend && ./mvnw test 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		grep -v "^2025-" | \
		grep -v "DEBUG \[tenant:" | \
		grep -v "Mockito is currently" | \
		grep -v "OpenJDK 64-Bit" | \
		grep -v "WARNING: A Java agent" | \
		grep -v "WARNING: If a serviceability" | \
		grep -v "WARNING: Dynamic loading" | \
		grep -v "CREATE TABLE\|create table\|ALTER TABLE\|alter table" | \
		grep -v "primary key\|foreign key\|references\|constraint" | \
		grep -v "CREATE EXTENSION\|CREATE OR REPLACE\|CREATE INDEX" | \
		grep -v "installed_rank\|flyway_schema" | \
		grep -v "Spring Boot ::" | \
		grep -v "^  .   ____\|^ /\\\\\|^( ( )\|^ \\\\/\|^  '\|^ =========" | \
		grep -v "java.net.SocketException\|at java.base\|at io.netty" | \
		grep -v "testcontainers.reuse.enable" | \
		grep -E "(Running|Tests:|BUILD|Failures|Errors|Skipped|Time elapsed|\[INFO\]|\[ERROR\]|\[WARNING\]|Test.*(passed|failed))" | \
		sed 's/\[INFO\]/ℹ️ /g' | \
		sed 's/\[ERROR\]/❌/g' | \
		sed 's/\[WARNING\]/⚠️ /g' | \
		sed 's/BUILD SUCCESS/✅ BUILD SUCCESS/g' | \
		sed 's/BUILD FAILURE/❌ BUILD FAILURE/g' | \
		sed 's/Tests run:/📊 Tests:/g'

# Alias for backward compatibility
.PHONY: test-backend
test-backend: test-backend-unit

# Run Grafana integration tests with beautiful UX logs
.PHONY: test-grafana
test-grafana:
	@echo ""
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  📊 GRAFANA PROVISIONING TESTS                                ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@cd backend && ./mvnw test -Dtest=GrafanaProvisioningServiceIT 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		grep -v "^Mockito" | \
		grep -v "^WARNING:" | \
		grep -v "^OpenJDK" | \
		grep -v "org.flywaydb" | \
		grep -E "(📝|🔧|🚀|🧪|✅|✓|❌|Tests run:|BUILD|INFO.*Grafana)"
	@echo ""

# Run FULL backend tests (unit + integration) - requires Docker/Testcontainers
.PHONY: test-backend-full
test-backend-full:
	@echo ""
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🧪 BACKEND FULL TEST SUITE (Unit + Integration)              ║"
	@echo "║  ⚠️  Requires: Docker running (Kafka, PostgreSQL)              ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "🐳 Checking Docker availability..."
	@if ! docker ps >/dev/null 2>&1; then \
		echo "❌ ERROR: Docker is not running!"; \
		echo ""; \
		echo "Integration tests require Docker for Testcontainers (Kafka, PostgreSQL)"; \
		echo "Please start Docker and try again."; \
		echo ""; \
		echo "Or use 'make test-backend' for unit tests only (no Docker needed)"; \
		exit 1; \
	fi
	@echo "✅ Docker is running"
	@echo ""
	@echo "▶️  Running ALL backend tests (unit + integration)..."
	@mkdir -p artifacts
	@cd backend && ./mvnw test 2>&1 | \
		tee ../artifacts/backend_full_tests.log | \
		grep -v "^\[DEBUG\]" | \
		grep -v "^2025-" | \
		grep -v "DEBUG \[tenant:" | \
		grep -v "Mockito is currently" | \
		grep -v "OpenJDK 64-Bit" | \
		grep -v "WARNING: A Java agent" | \
		grep -v "WARNING: If a serviceability" | \
		grep -v "WARNING: Dynamic loading" | \
		grep -v "CREATE TABLE\|create table\|ALTER TABLE\|alter table" | \
		grep -v "primary key\|foreign key\|references\|constraint" | \
		grep -v "CREATE EXTENSION\|CREATE OR REPLACE\|CREATE INDEX" | \
		grep -v "installed_rank\|flyway_schema" | \
		grep -v "Spring Boot ::" | \
		grep -v "^  .   ____\|^ /\\\\\|^( ( )\|^ \\\\/\|^  '\|^ =========" | \
		grep -v "java.net.SocketException\|at java.base\|at io.netty" | \
		grep -v "testcontainers.reuse.enable" | \
		grep -E "(Running|Tests:|BUILD|Failures|Errors|Skipped|Time elapsed|\[INFO\]|\[ERROR\]|\[WARNING\]|Test.*(passed|failed))" | \
		sed 's/\[INFO\]/ℹ️ /g' | \
		sed 's/\[ERROR\]/❌/g' | \
		sed 's/\[WARNING\]/⚠️ /g' | \
		sed 's/BUILD SUCCESS/✅ BUILD SUCCESS/g' | \
		sed 's/BUILD FAILURE/❌ BUILD FAILURE/g' | \
		sed 's/Tests run:/📊 Tests:/g' || \
		(echo "" && echo "❌ Full test suite failed - check artifacts/backend_full_tests.log" && exit 1)
	@echo ""
	@echo "✅ Full backend test suite completed!"
	@echo "📁 Log: artifacts/backend_full_tests.log"
	@echo ""

.PHONY: test-backend-integration
test-backend-integration:
	@echo "🔗 Running backend integration tests..."
	@mkdir -p artifacts
	@cd backend && ./mvnw test -Dtest="**/*IT,**/*IntegrationTest" 2>&1 | \
		tee ../artifacts/backend_integration_tests.log | \
		grep -v "^\[DEBUG\]" | \
		grep -v "^2025-" | \
		grep -v "DEBUG \[tenant:" | \
		grep -v "Mockito is currently" | \
		grep -v "OpenJDK 64-Bit" | \
		grep -v "WARNING: A Java agent" | \
		grep -v "WARNING: If a serviceability" | \
		grep -v "WARNING: Dynamic loading" | \
		grep -v "CREATE TABLE\|create table\|ALTER TABLE\|alter table" | \
		grep -v "primary key\|foreign key\|references\|constraint" | \
		grep -v "CREATE EXTENSION\|CREATE OR REPLACE\|CREATE INDEX" | \
		grep -v "installed_rank\|flyway_schema" | \
		grep -v "Spring Boot ::" | \
		grep -v "^  .   ____\|^ /\\\\\|^( ( )\|^ \\\\/\|^  '\|^ =========" | \
		grep -v "java.net.SocketException\|at java.base\|at io.netty" | \
		grep -v "testcontainers.reuse.enable" | \
		grep -E "(Running|Tests:|BUILD|Failures|Errors|Skipped|Time elapsed|\[INFO\]|\[ERROR\]|\[WARNING\]|Test.*(passed|failed))" | \
		sed 's/\[INFO\]/ℹ️ /g' | \
		sed 's/\[ERROR\]/❌/g' | \
		sed 's/\[WARNING\]/⚠️ /g' | \
		sed 's/BUILD SUCCESS/✅ BUILD SUCCESS/g' | \
		sed 's/BUILD FAILURE/❌ BUILD FAILURE/g' | \
		sed 's/Tests run:/📊 Tests:/g' || \
		(echo "" && echo "❌ Integration tests failed - check artifacts/backend_integration_tests.log" && exit 1)
	@echo "✅ Integration tests completed"

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
test-backend-all:
	@echo ""
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🧪 RUNNING COMPLETE BACKEND TEST SUITE                       ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "📋 Test Plan:"
	@echo "  1️⃣  Unit Tests          - All Java unit tests"
	@echo "  2️⃣  Integration Tests   - Database, API, Grafana"
	@echo "  3️⃣  Health Checks       - Application health"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@echo "▶️  [1/3] Running Unit Tests..."
	@$(MAKE) test-backend-unit || (echo "❌ Unit tests failed!" && exit 1)
	@echo ""
	@echo "✅ [1/3] Unit Tests PASSED"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@echo "▶️  [2/3] Running Integration Tests..."
	@$(MAKE) test-backend-integration || (echo "❌ Integration tests failed!" && exit 1)
	@echo ""
	@echo "✅ [2/3] Integration Tests PASSED"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@echo "▶️  [3/3] Running Health Checks..."
	@$(MAKE) test-backend-health || (echo "❌ Health checks failed!" && exit 1)
	@echo ""
	@echo "✅ [3/3] Health Checks PASSED"
	@echo ""
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🎉 ALL BACKEND TESTS COMPLETED SUCCESSFULLY! 🎉               ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""

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
# Run frontend tests
.PHONY: test-frontend
test-frontend:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🧪 FRONTEND UNIT TESTS                                        ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "▶️  Running Vitest tests..."
	@cd frontend && npm test -- --run 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/✓/  ✅/g' | \
		sed 's/✗/  ❌/g' | \
		sed 's/PASS/✅ PASS/g' | \
		sed 's/FAIL/❌ FAIL/g' | \
		sed 's/Test Files/📊 Test Files/g'
	@echo ""
	@echo "✅ Frontend tests completed!"
	@echo ""

# =============================================================================
# 📊 MONITORING TESTS (Axiom Monitoring Package)
# =============================================================================

# PRE-DEPLOY: Validate monitoring configuration
.PHONY: test-monitoring-deploy
test-monitoring-deploy:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🧪 MONITORING PRE-DEPLOY TESTS                                ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@bash scripts/test-monitoring-deploy.sh
	@echo ""

# POST-DEPLOY: Runtime monitoring tests
.PHONY: test-monitoring-runtime
test-monitoring-runtime:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🎭 MONITORING RUNTIME TESTS                                   ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@bash scripts/test-monitoring-runtime.sh
	@echo ""

# Full monitoring test suite
.PHONY: test-monitoring
test-monitoring: test-monitoring-deploy test-monitoring-runtime
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  ✅ ALL MONITORING TESTS COMPLETED                             ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""

# Grafana provisioning diagnostics
.PHONY: diag-grafana-provisioning
diag-grafana-provisioning:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  📊 GRAFANA PROVISIONING DIAGNOSTICS                           ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@bash scripts/test-grafana-provisioning.sh
	@echo ""

# Grafana SSO sanity test (provisioning + org setup)
.PHONY: test-grafana-sso
test-grafana-sso:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🔐 GRAFANA SSO SANITY TEST                                    ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "🧪 Testing: test_admin provisioning, org membership, dashboard access"
	@echo ""
	@bash scripts/grafana/sanity-test.sh
	@echo ""

# Validate Grafana dashboard structure
.PHONY: validate-dashboard-structure
validate-dashboard-structure:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  📋 VALIDATING DASHBOARD STRUCTURE                             ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@bash scripts/validate-dashboard-structure.sh
	@echo ""

# =============================================================================
# 🎭 E2E TESTING (Two-Tier Strategy)
# =============================================================================

# PRE-DEPLOY: Fast smoke tests (gate before deployment)
.PHONY: test-e2e-pre
test-e2e-pre:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🎭 PRE-DEPLOY E2E SMOKE TESTS (FAST GATE)                    ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "⚠️  Requires: Running environment (make dev-up or make up)"
	@echo "📋 Tests: Login, RBAC, Grid/Form, Workflow panel"
	@echo "⏱️  Duration: ~5-7 minutes"
	@echo ""
	@if [ ! -d "e2e/node_modules" ]; then \
		echo "📦 Installing E2E dependencies..."; \
		cd e2e && npm install; \
	fi
	@echo "▶️  Running smoke tests..."
	@cd e2e && npm run test:pre 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/✓/  ✅/g' | \
		sed 's/✗/  ❌/g' | \
		sed 's/passed/✅ passed/g' | \
		sed 's/failed/❌ failed/g'
	@echo ""
	@echo "✅ PRE-DEPLOY smoke tests completed!"
	@echo "📊 Report: e2e/playwright-report/index.html"
	@echo ""

# POST-DEPLOY: Full E2E tests with ephemeral data
.PHONY: test-e2e-post
test-e2e-post:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🎭 POST-DEPLOY E2E FULL TESTS                                 ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "⚠️  Requires: Deployed environment (staging/production)"
	@echo "📋 Tests: Full scenarios with scaffold/teardown"
	@echo "⏱️  Duration: ~20-30 minutes"
	@echo ""
	@if [ ! -d "e2e/node_modules" ]; then \
		echo "📦 Installing E2E dependencies..."; \
		cd e2e && npm install; \
	fi
	@echo "▶️  [1/3] Creating ephemeral test data..."
	@cd e2e && npm run scaffold 2>&1 | tail -10
	@echo "  ✅ Test data created"
	@echo ""
	@echo "▶️  [2/3] Running full E2E tests..."
	@cd e2e && npm run test:post 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/✓/  ✅/g' | \
		sed 's/✗/  ❌/g' | \
		sed 's/passed/✅ passed/g' | \
		sed 's/failed/❌ failed/g' || \
	(echo "  ❌ Tests failed!"; cd e2e && npm run teardown; exit 1)
	@echo ""
	@echo "▶️  [3/3] Cleaning up test data..."
	@cd e2e && npm run teardown 2>&1 | tail -5
	@echo "  ✅ Cleanup complete"
	@echo ""
	@echo "✅ POST-DEPLOY E2E tests completed!"
	@echo "📊 Report: e2e/playwright-report/index.html"
	@echo ""

# Run all E2E tests (PRE + POST)
.PHONY: test-e2e
test-e2e:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🎭 ALL E2E TESTS (PRE + POST DEPLOY)                         ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "▶️  [1/2] PRE-DEPLOY smoke tests (fast gate)..."
	@$(MAKE) test-e2e-pre
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@echo "▶️  [2/2] POST-DEPLOY full scenarios..."
	@$(MAKE) test-e2e-post
	@echo ""
	@echo "🎉 All E2E tests completed successfully!"
	@echo ""

# Install E2E dependencies and Playwright browsers
.PHONY: e2e-setup
e2e-setup:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  📦 E2E SETUP (DEPENDENCIES + PLAYWRIGHT)                     ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"

# E2E: Loki Monitoring Tests (Native UI)
.PHONY: test-e2e-loki
test-e2e-loki:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  📊 LOKI MONITORING E2E TESTS (NATIVE UI)                      ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "⚠️  Requires: Running environment + Loki ingesting logs"
	@echo "📋 Tests: LogViewer, MetricCard, Tenant isolation, CSV export"
	@echo "⏱️  Duration: ~5-8 minutes"
	@echo ""
	@if [ ! -d "e2e/node_modules" ]; then \
		echo "📦 Installing E2E dependencies..."; \
		cd e2e && npm install; \
	fi
	@echo "▶️  Running Loki monitoring tests..."
	@cd e2e && npx playwright test --project=monitoring \
		specs/monitoring/loki-log-viewer.spec.ts \
		specs/monitoring/loki-csv-export.spec.ts \
		2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/✓/  ✅/g' | \
		sed 's/✗/  ❌/g' | \
		sed 's/passed/✅ passed/g' | \
		sed 's/failed/❌ failed/g'
	@echo ""

# Smoke Test: Loki Migration Validation
.PHONY: smoke-test-loki
smoke-test-loki:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🔥 SMOKE TEST: De-Grafana → Native Loki UI Migration        ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "⚠️  Requires: Running environment + Valid JWT token"
	@echo "📋 Tests: Feature flags, BFF API, Prometheus metrics, Rate limit"
	@echo "⏱️  Duration: ~1-2 minutes"
	@echo ""
	@if [ -z "$$AT" ]; then \
		echo "❌ Error: AT environment variable not set"; \
		echo ""; \
		echo "Options:"; \
		echo "  1) Export JWT from browser cookie:"; \
		echo "     export AT=\"<paste_cookie_at_value>\""; \
		echo ""; \
		echo "  2) Use Keycloak credentials:"; \
		echo "     export KC_USERNAME=test_admin KC_PASSWORD=admin123"; \
		echo ""; \
		exit 1; \
	fi
	@bash scripts/smoke-test-loki-migration.sh
	@echo ""
	@echo "✅ Loki monitoring E2E tests completed!"
	@echo "📊 Report: e2e/playwright-report/index.html"
	@echo ""

# Admin CRUD E2E tests (55 tests: users, roles, groups, tenants, keycloak-sync)
.PHONY: test-e2e-admin
test-e2e-admin:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  👥 ADMIN CRUD E2E TESTS (55 tests)                            ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "⚠️  Requires: Running environment (make dev-up or make up)"
	@echo "📋 Tests: Users, Roles, Groups, Tenants, Keycloak Sync"
	@echo "⏱️  Duration: ~3-5 minutes"
	@echo ""
	@if [ ! -d "e2e/node_modules" ]; then \
		echo "📦 Installing E2E dependencies..."; \
		cd e2e && npm install; \
	fi
	@echo "▶️  Running admin CRUD tests..."
	@cd e2e && npx playwright test specs/admin/ 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/✓/  ✅/g' | \
		sed 's/✗/  ❌/g' | \
		sed 's/passed/✅ passed/g' | \
		sed 's/failed/❌ failed/g'
	@echo ""
	@echo "✅ Admin CRUD E2E tests completed!"
	@echo "📊 Report: e2e/playwright-report/index.html"
	@echo ""

# Keycloak Sync E2E tests (10 tests)
.PHONY: test-e2e-sync
test-e2e-sync:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🔄 KEYCLOAK SYNC E2E TESTS (10 tests)                         ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "⚠️  Requires: Running environment + Keycloak"
	@echo "📋 Tests: User sync, Role sync, Group sync, Full sync, RBAC"
	@echo "⏱️  Duration: ~1-2 minutes"
	@echo ""
	@if [ ! -d "e2e/node_modules" ]; then \
		echo "📦 Installing E2E dependencies..."; \
		cd e2e && npm install; \
	fi
	@echo "▶️  Running Keycloak Sync tests..."
	@cd e2e && npx playwright test specs/admin/keycloak-sync.spec.ts 2>&1 | \
		grep -v "^\[DEBUG\]" | \
		sed 's/✓/  ✅/g' | \
		sed 's/✗/  ❌/g' | \
		sed 's/passed/✅ passed/g' | \
		sed 's/failed/❌ failed/g'
	@echo ""
	@echo "✅ Keycloak Sync E2E tests completed!"
	@echo "📊 Report: e2e/playwright-report/index.html"
	@echo ""

# E2E setup
	@echo ""
	@echo "▶️  [1/2] Installing npm dependencies..."
	@cd e2e && npm install 2>&1 | tail -5
	@echo "  ✅ Dependencies installed"
	@echo ""
	@echo "▶️  [2/2] Installing Playwright browsers (chromium)..."
	@cd e2e && npx playwright install --with-deps chromium 2>&1 | tail -5
	@echo "  ✅ Browsers installed"
	@echo ""
	@echo "✅ E2E setup complete!"
	@echo ""

# Open E2E test report
.PHONY: e2e-report
e2e-report:
	@echo "📊 Opening E2E test report..."
	@cd e2e && npm run report

# Run E2E scaffold only (for debugging)
.PHONY: e2e-scaffold
e2e-scaffold:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🏗️  E2E SCAFFOLD (CREATE TEST DATA)                          ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "▶️  Creating ephemeral test data..."
	@cd e2e && npm run scaffold 2>&1 | grep -v "^\[DEBUG\]"
	@echo ""
	@echo "✅ Test data created!"
	@echo ""

# Run E2E teardown only (for cleanup)
.PHONY: e2e-teardown
e2e-teardown:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🧹 E2E TEARDOWN (CLEANUP TEST DATA)                          ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "▶️  Cleaning up test data..."
	@cd e2e && npm run teardown 2>&1 | grep -v "^\[DEBUG\]"
	@echo ""
	@echo "✅ Cleanup complete!"
	@echo ""

# Run all pre-build tests (unit tests only)
.PHONY: test-all
test-all:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🧪 ALL UNIT TESTS (BACKEND + FRONTEND)                       ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "▶️  [1/2] Backend unit tests..."
	@$(MAKE) test-backend
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@echo "▶️  [2/2] Frontend unit tests..."
	@$(MAKE) test-frontend
	@echo ""
	@echo "🎉 All unit tests completed successfully!"
	@echo ""

# Run comprehensive test suite (unit + integration + E2E PRE)
.PHONY: test-comprehensive
test-comprehensive:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🧪 COMPREHENSIVE TEST SUITE                                   ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "▶️  [1/2] All unit tests (backend + frontend)..."
	@$(MAKE) test-all
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""
	@echo "▶️  [2/2] PRE-DEPLOY E2E smoke tests..."
	@$(MAKE) test-e2e-pre
	@echo ""
	@echo "🎉 Comprehensive testing completed successfully!"
	@echo ""

# CI/CD: Full test pipeline with E2E gate
.PHONY: ci-test-pipeline
ci-test-pipeline:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🚀 CI/CD TEST PIPELINE                                        ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "Phase 1: Unit Tests"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@$(MAKE) test-all
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "Phase 2: Environment Startup"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@$(MAKE) up
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "Phase 3: PRE-DEPLOY E2E Gate (CRITICAL)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@$(MAKE) test-e2e-pre || (echo ""; echo "❌ E2E gate failed! Deployment blocked."; exit 1)
	@echo ""
	@echo "🎉 CI/CD pipeline successful! Ready to deploy."
	@echo ""

# CI/CD: Post-deployment validation
.PHONY: ci-post-deploy
ci-post-deploy:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  🚀 POST-DEPLOYMENT VALIDATION                                 ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@$(MAKE) test-e2e-post
	@echo ""
	@echo "✅ Post-deployment validation complete!"
	@echo ""

# =============================================================================
# 📊 GRAFANA SSO DIAGNOSTICS
# =============================================================================

.PHONY: diag-grafana-embed
diag-grafana-embed:
	@echo "╔════════════════════════════════════════════════════════════════╗"
	@echo "║  📊 GRAFANA EMBED MULTI-TENANT DIAGNOSTICS                     ║"
	@echo "╚════════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "🔍 Test 1: Auth Bridge (/_auth/grafana with valid cookie)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@# Internal call from backend - should return 200 + Grafana-JWT header
	@docker exec core-backend curl -s -w "\nHTTP Status: %{http_code}\n" \
		-H "Cookie: at=test-jwt-token" \
		http://localhost:8080/internal/auth/grafana 2>/dev/null || echo "❌ Auth bridge unreachable"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "🔍 Test 2: /core-admin/monitoring/ WITHOUT auth (should be 401)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@curl -skL -w "\nHTTP Status: %{http_code}\n" \
		https://admin.core-platform.local/core-admin/monitoring/api/health 2>/dev/null | tail -2
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "🔍 Test 3: Grafana assets (should be CSS, not HTML)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@curl -skL -I "https://admin.core-platform.local/core-admin/monitoring/public/build/grafana.light.css" 2>/dev/null | \
		grep -E "HTTP|Content-Type" | head -2
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "🔍 Test 4: Redirect /core-admin/monitoring → /core-admin/monitoring/"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@curl -sk -w "\nHTTP Status: %{http_code}\nRedirect: %{redirect_url}\n" \
		-o /dev/null \
		https://admin.core-platform.local/core-admin/monitoring 2>/dev/null | tail -3
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "🔍 Test 5: Grafana config (root_url, serve_from_sub_path)"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@docker exec core-grafana grep -E "^(root_url|serve_from_sub_path|domain)" /etc/grafana/grafana.ini | grep -v "^#"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "🔍 Test 6: Check for domain duplication in logs"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@docker logs core-grafana 2>&1 | grep -i "admin\.admin\|double.*subdomain" | tail -5 || echo "✅ No domain duplication in logs"
	@echo ""
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo "✅ Diagnostics complete!"
	@echo "Expected results:"
	@echo "  ✓ Test 1: HTTP 200 with Grafana-JWT header"
	@echo "  ✓ Test 2: HTTP 401 (auth required)"
	@echo "  ✓ Test 3: Content-Type: text/css"
	@echo "  ✓ Test 4: HTTP 301/308 redirect to trailing slash"
	@echo "  ✓ Test 5: root_url = https://%(domain)s/core-admin/monitoring"
	@echo "  ✓ Test 5: serve_from_sub_path = true"
	@echo "  ✓ Test 6: No admin.admin.core-platform.local errors"
	@echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
	@echo ""

# Legacy alias
.PHONY: diag-grafana-sso
diag-grafana-sso: diag-grafana-embed

# =============================================================================
# 📋 BACKLOG MANAGEMENT (EPIC-001)
# =============================================================================

.PHONY: backlog-new backlog-help

# Create new User Story from template
backlog-new:
	@bash scripts/backlog/new_story.sh $(if $(STORY),--title "$(STORY)") $(if $(EPIC),--epic "$(EPIC)") $(if $(PRIORITY),--priority "$(PRIORITY)") $(if $(ESTIMATE),--estimate "$(ESTIMATE)") $(if $(ASSIGNEE),--assignee "$(ASSIGNEE)")

# Backlog help
backlog-help:
	@echo "📋 Backlog Management (EPIC-001)"
	@echo ""
	@echo "Commands:"
	@echo "  backlog-new          - Create new story (interactive)"
	@echo "  backlog-new STORY='Feature Name' - Quick create with title"
	@echo ""
	@echo "Options:"
	@echo "  STORY='Feature Name'       - Story title (required)"
	@echo "  EPIC='EPIC-XXX'            - Epic ID (default: EPIC-001-backlog-system)"
	@echo "  PRIORITY='P1|P2|P3'        - Priority (default: P1)"
	@echo "  ESTIMATE='X days'          - Estimate (default: 1 day)"
	@echo "  ASSIGNEE='Name'            - Assignee name"
	@echo ""
	@echo "Examples:"
	@echo "  make backlog-new"
	@echo "  make backlog-new STORY='Git Commit Tracker' EPIC='EPIC-001-backlog-system' PRIORITY='P2' ESTIMATE='2 days'"
	@echo "  make backlog-new STORY='User Login' EPIC='EPIC-002-auth' PRIORITY='P1'"
	@echo ""
	@echo "Generated files:"
	@echo "  backlog/EPIC-XXX/stories/CORE-YYY-title/README.md"
	@echo "  Git branch: feature/CORE-YYY-title"
	@echo ""
