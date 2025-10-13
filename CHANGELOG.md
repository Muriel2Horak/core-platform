# Changelog

# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- **Post-Deployment Verification**: Automatické smoke testy po `make up/rebuild`
  - Kontrola container health, API endpoints, frontend přístupnosti
  - Verifikace observability stacku (Grafana, Loki, Prometheus)
  - Volitelné plné integration testy (`make verify-full`)
  - Nové příkazy: `make verify`, `make verify-full`
  - Dokumentace: `docs/POST_DEPLOYMENT_VERIFICATION.md`

## [Previous entries...]

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed - Build System (2025-10-11)

#### Maven Dependency Convergence Resolution
- **Fixed build failures** due to Maven Enforcer dependency convergence errors
- **Added `<dependencyManagement>`** to pin conflicting transitive dependencies:
  - `commons-compress`: 1.24.0 (was conflicting 1.21 vs 1.24.0)
  - `apache-mime4j-core/dom`: 0.8.11 (was conflicting 0.8.9 vs 0.8.11)
  - `commons-io`: 2.14.0 (was conflicting 2.7/2.11.0/2.14.0)
  - `bcprov-jdk18on`: 1.76 (was conflicting 1.72 vs 1.76)
  - `error_prone_annotations`: 2.40.0 (was conflicting 2.21.1 vs 2.40.0)
  - `checker-qual`: 3.49.3 (was conflicting 3.37.0 vs 3.49.3)
  - `asm`: 9.7.1 (was conflicting 9.6 vs 9.7.1)
- **Added exclusions** to `keycloak-admin-client` and `spring-boot-starter-webflux` for commons-logging conflicts
- **Enhanced enforcer configuration** to ignore safe duplicate classes in logging bridges
- **Re-enabled `dependencyConvergence` rule** with property control (`enforcer.skip`)
- **Verified**: Build now passes with `-Denforcer.skip=false`
- **Documentation**: See `docs/BUILD_DEPS_CONVERGENCE_FIX.md` for details

### Added - Platform Hardening Epic (S2: Real-Time Presence + Kafka Lifecycle)

#### Real-Time Presence Tracking System (2025-10-11)
- **Backend Infrastructure**:
  - `PresenceService`: Redis-backed presence tracking with field-level locks
    - WebSocket presence tracking with auto-expiry (60s TTL)
    - Atomic field locks with `SET NX PX` (120s TTL)
    - Stale flag management for concurrent edit prevention
    - Version tracking per entity
  - `WebSocketConfig` + `PresenceWebSocketHandler`: WebSocket endpoint `/ws/presence`
    - Protocol: SUB/UNSUB/HB/LOCK/UNLOCK messages
    - Auto-heartbeat every 30s
    - Session cleanup on disconnect
  - `EntityLifecycleProducer` + `EntityLifecycleConsumer`: Kafka lifecycle events
    - Topics: `core.entities.lifecycle.mutating`, `core.entities.lifecycle.mutated`
    - Retry strategy: 3× exponential backoff (1s/3s/9s) + DLQ
    - Stale flag synchronization with Redis
  - `PresenceController`: REST API for debugging/monitoring
    - GET `/api/presence/{tenantId}/{entity}/{id}` - Get presence state
    - POST `/api/presence/.../subscribe` - Manual subscribe
    - POST `/api/presence/.../lock/{field}` - Acquire field lock

- **Frontend Infrastructure**:
  - `PresenceClient`: TypeScript WebSocket client
    - Auto-reconnect with exponential backoff (max 5 attempts)
    - Heartbeat management (30s interval)
    - Lock acquisition/release
  - `usePresence()` hook: React hook for presence tracking
    - Returns: users, stale, busyBy, version, connected
    - Methods: acquireLock(field), releaseLock(field)
  - `PresenceIndicator` component: UI for active users + stale badge
  - `FieldLockIndicator` component: Visual lock indicators for fields

- **Documentation**:
  - `docs/PRESENCE_SYSTEM_README.md`: Complete architecture guide
    - Backend/frontend components overview
    - Usage examples and integration guide
    - Redis key schema documentation
    - Testing strategies and monitoring
    - Troubleshooting guide

**Dependencies**: Leverages existing Redis + Kafka infrastructure (no new dependencies needed)

### Added - Platform Hardening Epic (S1: Naming Conventions)

#### Naming Guide & Linting Infrastructure (2025-10-11)
- **docs/NAMING_GUIDE.md** (530+ lines): Comprehensive naming conventions guide
  - Entity & Domain Model: PascalCase singular (User, UserDirectory)
  - Database: snake_case tables (plural), columns (singular), mandatory tenant_id
  - REST API: kebab-case plural URLs (/api/users, /api/user-directories)
  - JSON: camelCase keys (firstName, userId)
  - Cube.js: PascalCase plural cubes (Users), camelCase measures/dimensions
  - Kafka: product.context.entity.event in kebab-case + -retry/-dlq suffixes
  - Prometheus: snake_case with _seconds/_total/_bytes suffixes
  - Full-stack examples for User and UserDirectory entities
  - Migration/compatibility guide with deprecation strategy
  - Anti-patterns and checklist for new features

- **tools/naming-lint/**: Automated naming convention linters (Node.js/ESM)
  - `lint-metamodel.js`: Validates metamodel JSON (PascalCase entities, camelCase fields, required fields)
  - `lint-api.js`: Validates Spring controllers (kebab-case plural paths, controller naming)
  - `lint-kafka.js`: Validates Kafka topics (product.context.entity.event pattern)
  - `lint-db.js`: Validates Flyway migrations (V{YYYYMMDDHHMM}__ pattern, tenant_id presence)
  - Utilities: casing.js (isPascalCase, isCamelCase, isSnakeCase, isKebabCase, pluralize)
  - Reporter: Colorized terminal output with error/warning counts

- **CI Integration**: `.github/workflows/naming-lint.yml`
  - Runs on PR to main/develop/feature branches
  - Node.js 20 with npm cache
  - 4 lint steps: metamodel, API, Kafka, DB
  - Fails build on naming violations
  - 5-minute timeout

**DoD Checklist**:
- [x] docs/NAMING_GUIDE.md created (530+ lines, all layers covered)
- [x] tools/naming-lint/ implemented (4 linters + utilities)
- [x] CI workflow .github/workflows/naming-lint.yml added
- [x] README.md in tools/naming-lint/ with usage examples
- [x] Linters executable with npm scripts (lint:all, lint:metamodel, etc.)
- [x] Exit codes: 0 (pass), 1 (fail)

**Next Steps (S1 Completion)**:
- [x] Refactor UserDirectoryController: /api/users-directory → /api/user-directories
  - Added deprecated alias for backward compatibility (removal: v2.3.0)
  - @Deprecated annotation added with forRemoval=true
- [ ] Refactor existing code to match conventions (JSON keys, Cube schemas, Kafka topics)
- [ ] Add migration aliases (nginx redirects, Cube name mapping)
- [ ] Update integration tests for new paths
- [ ] Update OpenAPI/Swagger documentation

### Changed

#### REST API Path Refactoring (2025-10-11)
- **BREAKING:** UserDirectoryController path changed
  - OLD: `/api/users-directory` ❌
  - NEW: `/api/user-directories` ✅ (kebab-case plural, per NAMING_GUIDE.md)
  - Old path remains as deprecated alias (will be removed in v2.3.0)
  - Frontend already uses correct path (no changes needed)

### Deprecated

- **UserDirectoryController** old path `/api/users-directory`
  - Use `/api/user-directories` instead
  - Removal planned: v2.3.0 (2 minor versions)
  - Both paths currently supported via `@RequestMapping` array

### Migration Guide

#### Frontend API Calls
```typescript
// No changes needed - frontend already uses correct path
const response = await fetch('/api/user-directories');
```

#### Backend Integration Tests
```java
// Update test URLs if using old path
mockMvc.perform(get("/api/user-directories"))  // ✅ NEW
// mockMvc.perform(get("/api/users-directory")) // ❌ DEPRECATED
```
- [ ] Update CHANGELOG with refactoring details

### Added - Reporting Module (Audit Closure)

#### PHASE 2: Metamodel UI Spec Generator + Endpoint (2025-10-11)
- **MetamodelSpecService**: Extended to generate complete entity specifications for UI rendering
  - Full spec includes: dimensions, measures, filters, editable fields, relations, validations, enums, default views, drilldowns
  - Checksum versioning (SHA-256) for spec change detection
  - Helper methods: formatLabel, isSensitiveField, buildValidationMessage, buildDefaultView, buildDrilldowns
- **EntitySpec DTO**: Extended with new properties for UI components
  - Added: editableFields, relations, validations, enums, defaultView, drilldowns
  - Enhanced FieldSpec with: label, required, sensitive, adminOnly flags
  - RelationSpec for drill-down navigation (manyToOne/oneToMany/manyToMany)
  - ValidationSpec for client-side validation rules
  - DefaultViewSpec for initial grid configuration
  - DrilldownSpec for entity navigation paths
- **ReportQueryController**: New endpoint `GET /api/reports/metadata/{entity}/spec`
  - Returns full EntitySpec with X-Spec-Version header
  - Cached for 1 hour (Cache-Control: public, max-age=3600)
- **Unit Tests**: MetamodelSpecServiceTest with 100% coverage
  - Tests: full spec generation, sensitive field exclusion, checksum consistency, label formatting, measure identification, operator mapping

**Frontend Integration Ready**:
- FE can fetch spec and dynamically render forms/grids
- Editable fields clearly identified
- Validation rules available for client-side checks
- Relations enable drill-down navigation

**DoD Checklist**:
- [x] MetamodelSpecService.getFullEntitySpec() implemented
- [x] Endpoint /api/reports/metadata/{entity}/spec functional
- [x] Spec contains dimensions, measures, relations, validations, enums
- [x] Checksum versioning implemented
- [x] Unit tests 100% coverage
- [x] JavaDoc documentation complete

#### PHASE 1: Cube.js Infrastructure + Schema + RLS (2025-10-11)
- **Cube.js Service**: Added Cube.js semantic layer to docker-compose.yml with PostgreSQL and Redis integration
- **Cube Schemas**: Created three base cubes (Users, Tenants, Groups) with:
  - Row-Level Security (RLS) via `SECURITY_CONTEXT.tenantId` filter
  - Pre-aggregations for daily/weekly rollups (hourly/6-hour refresh)
  - Comprehensive dimensions and measures for reporting
- **Documentation**: Added `docs/CUBE_SETUP.md` with setup guide, RLS implementation, pre-aggregation strategies, and troubleshooting
- **Health Check Script**: Created `scripts/cube/check-cube.sh` for validating Cube.js installation and schema loading
- **Environment Configuration**: Added CUBE_PORT, CUBE_API_SECRET, CUBE_DEV_MODE to .env

**Security**:
- Enforced tenant isolation in all cube SQL queries
- Redis-backed caching with secure API token
- Health checks for service availability

**Performance**:
- Pre-aggregations reduce query time by 10-100x
- Redis caching layer for repeated queries
- Incremental refresh strategy for large datasets

**DoD Checklist**:
- [x] Cube.js service running in Docker (port 4000)
- [x] 3+ entity schemas created (Users, Tenants, Groups)
- [x] RLS implemented with SECURITY_CONTEXT filter
- [x] Pre-aggregations defined with refresh strategies
- [x] Documentation complete (CUBE_SETUP.md)
- [x] Health check script executable

---

## [0.1.0] - Initial Release

### Added
- Core platform infrastructure
- Multi-tenant authentication via Keycloak
- User and group directory
- Monitoring stack (Grafana, Loki, Prometheus)

