# Changelog

# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

#### Backlog Management System (EPIC-001) - 2025-11-06

**Test-First Development & Bug Tracking (CORE-007)** - 2025-11-06
- **Complete test-driven development workflow with bug tracking**
  - Bug template: `backlog/templates/bug.md` (533 lines, comprehensive bug tracking)
  - Story template update: AC → Test Mapping section added
  - Test validator: `scripts/backlog/test_validator.sh` (489 lines Bash)
  - TDD workflow guide: `docs/development/test-driven-workflow.md` (734 lines)
  - Backlog README: Bug tracking & test-first sections added
- **Bug Template Features**:
  - YAML frontmatter: `caused_by_story`, `caused_by_commit`, `regression_test`
  - Severity classification: critical | high | medium | low
  - Status lifecycle: reported → investigating → in-progress → fixed → verified → closed
  - Mandatory regression test (@BUG-XXX @regression tags)
  - Fix DoD with verification checklist
  - Full traceability: Bug → Story → Commit
  - Timeline tracking: time_to_detect, time_to_fix
- **Story Template Enhancement**:
  - 🧪 AC to Test Mapping section (MANDATORY)
  - Test types per AC: Unit | Integration | E2E | Performance
  - Test status tracking: ⏳ Not Written | ✍️ Written | ✅ Passing | ❌ Failing
  - Coverage % per AC (target: 100%)
  - Test tagging: @CORE-XXX @ACN
  - Test validator integration
- **Test Validator Features**:
  - CLI: `--story CORE-XXX`, `--epic EPIC-XXX`, `--format` (text|json), `--min-coverage NN`
  - Parses AC sections from story README
  - Extracts test mappings from AC → Tests tables
  - Validates test file existence
  - Reports coverage per AC and overall
  - Color-coded output: ✅ Complete | ⚠️ Partial | ❌ Missing
  - JSON output for CI/CD automation
  - Quality gate: Fail if coverage < min-coverage
- **TDD Workflow Documentation** (750+ lines):
  - Why test-first development? (problem vs solution)
  - Red-Green-Refactor cycle (detailed phases)
  - Story to Test workflow (7-step process)
  - Bug tracking & regression prevention
  - Test tagging conventions (@story, @bug, @regression)
  - CI/CD integration (GitHub Actions example)
  - Tools & automation reference
  - Complete examples (CORE-012 export feature, BUG-042 email fix)
- **Usage Examples**:
  ```bash
  # Create bug report
  cp backlog/templates/bug.md backlog/bugs/BUG-042-email-validation.md
  
  # Write regression test (RED phase)
  # → Test reproduces bug
  
  # Fix bug (GREEN phase)
  # → Test passes
  
  # Verify regression prevention
  # → Revert fix, test fails again
  
  # Validate test coverage
  bash scripts/backlog/test_validator.sh --story CORE-012
  # Output: AC1: 100% (3/3 tests) ✅, Overall: 100% ✅
  
  # Enforce 80% minimum coverage
  bash scripts/backlog/test_validator.sh --story CORE-012 --min-coverage 80
  
  # Run regression tests
  npx playwright test --grep @regression
  npx playwright test --grep @BUG-042
  ```
- **DoD Requirements Added**:
  - AC to Test Mapping filled (min 1 test per AC)
  - Test-first workflow followed (tests BEFORE code)
  - Test validator passed (100% AC coverage)
  - Regression tests tagged (@BUG-XXX @regression)
  - All tests passing (CI/CD green)
- **Quality Gates**:
  - Cannot merge without test_validator passing
  - Bug fixes require regression test
  - Regression tests prevent recurrence
  - Full story → commit → bug traceability
- **Value**:
  - 🧪 Enforces test-first development (every feature has tests)
  - 🐛 Standardized bug tracking (no ad-hoc reports)
  - 🔗 Full traceability (bug → story → commit)
  - ✅ Quality gates (cannot merge without tests)
  - 📊 Test coverage visibility (AC-level granularity)
  - 🔄 Regression prevention (bugs have tests)

**Path Mapping Validator (CORE-006)** - 2025-11-06
- **Automatic path mapping validation & coverage reporting**
  - Main tool: `scripts/backlog/path_validator.py` (269 lines Python)
  - Library modules (755 lines total):
    - `scripts/backlog/lib/yaml_parser.py` (198 lines) - YAML frontmatter parsing
    - `scripts/backlog/lib/path_checker.py` (236 lines) - File existence validation
    - `scripts/backlog/lib/coverage_reporter.py` (294 lines) - Text + JSON reporting
  - Integration tests: `scripts/backlog/test_integration.py` (353 lines, 33 assertions ✅)
- **CLI Options**: `--story CORE-XXX`, `--epic EPIC-XXX`, `--format` (text|json), `--show-zero`
- **Features**:
  - Parse `path_mapping` from story YAML frontmatter (code_paths, test_paths, docs_paths)
  - Validate file existence (literal paths + glob patterns like `backend/**/*.java`)
  - Generate coverage reports: "code_paths: 1/1 (100%), test_paths: 0/1 (0%), docs_paths: 3/3 (100%)"
  - Story-level reporting (single story validation)
  - Epic-level aggregation (all stories in epic)
  - Text format: Human-readable with emojis (✅/⚠️/❌)
  - JSON format: Machine-readable for automation (`jq` compatible)
  - Error handling: Clear messages for missing stories, invalid YAML
- **Performance**: <130ms for 100 stories (actual: 6ms for 5 stories) - Target <5s ✅
- **Usage Examples**:
  ```bash
  # Validate single story (text format)
  python3 scripts/backlog/path_validator.py --story CORE-005
  # Output: ✅ code_paths 1/1 (100%), ⚠️ test_paths 0/1 (0%), ✅ docs_paths 3/3 (100%)
  #         📈 Overall: 80% (4/5 paths exist)
  
  # Validate entire epic
  python3 scripts/backlog/path_validator.py --epic EPIC-001
  # Output: Aggregated report for all stories
  
  # JSON output for automation
  python3 scripts/backlog/path_validator.py --story CORE-005 --format json | jq .
  # Output: {"story_id":"CORE-005","coverage":{...},"overall":{"total":5,"exist":4,"percentage":80.0}}
  ```
- **Use Cases**:
  - 🔍 Path accuracy validation (detect typos, missing files, stale paths)
  - 📊 Coverage tracking (measure DoD completion - how many declared files exist)
  - ⚠️ Pre-merge validation (ensure path_mapping is accurate before merge)
  - 🤖 CI/CD integration (JSON output for automated quality gates)
- **Testing**:
  - 33/33 integration test assertions passing ✅
  - Tested on CORE-005: 80% coverage (4/5 paths exist - test file missing as expected)
  - Performance: 5 stories in 6ms, epic aggregation in 22ms
  - Edge cases: Stories without path_mapping (graceful handling), missing files (proper tracking)
- **Documentation**:
  - `scripts/backlog/README.md` - NEW: Complete tool documentation
  - `backlog/README.md` - Updated "Future Automation" section (CORE-006 marked as IMPLEMENTED)
  - `docs/development/backlog-workflow.md` - TBD: Path validation workflow
- **Dependencies**: PyYAML (`pip3 install pyyaml --user`)
- **Commits**: 437a155 (story), dab7ac3 (YAML parser), ef5333c (path checker), 97997a8 (reporter), b8f91c3 (CLI), d2f14f2 (tests)

**Git Commit Tracker (CORE-005)**
- **Automatic commit → story mapping** for Git activity tracking
  - Script: `scripts/backlog/git_tracker.sh` (405 lines bash)
  - **CLI Options**: `--epic`, `--story`, `--format` (text|json), `--show-zero`, `--help`
- **Features**:
  - Parse git log for CORE-XXX references (supports: feat|fix|chore|docs|test|refactor patterns)
  - Count commits per story ID
  - Handle multi-story commits (`CORE-001,CORE-003`)
  - Text report with emojis (✅ has commits, 📋 no commits)
  - JSON output for automation/dashboards
  - Zero-commit detection (`--show-zero` flag)
- **Performance**: <0.3s for EPIC-001 (target <2s) ✅
- **Usage Examples**:
  ```bash
  # Show commits for epic
  bash scripts/backlog/git_tracker.sh --epic EPIC-001-backlog-system
  
  # Specific story
  bash scripts/backlog/git_tracker.sh --story CORE-003
  
  # JSON output
  bash scripts/backlog/git_tracker.sh --epic EPIC-001-backlog-system --format json | jq .
  ```
- **Use Cases**:
  - 📊 Progress tracking (see which stories have Git activity)
  - 🔍 Audit trail (map commits back to stories for compliance)
  - 📋 Stale detection (find stories without commits)
  - 🤖 Automation (JSON output for dashboards/reports)
- **Testing**: Verified on EPIC-001 commits (CORE-001: 1, CORE-003: 2, CORE-005: 1+)
- **Documentation**:
  - `backlog/README.md` - Added Git Tracker section under Automation
  - `docs/development/backlog-workflow.md` - Added "Tracking Git Activity" section
- **Commit**: 7699f33 (story definition), 269fd38 (parser), 68a5ade (filters), TBD (docs)

**Story Generator (CORE-003)**
- **Automatic story creation** from templates with interactive wizard
  - `make backlog-new` - Interactive mode (prompts for all inputs)
  - `make backlog-new STORY="Name" EPIC="EPIC-XXX" PRIORITY="P2"` - Non-interactive mode
  - Script: `scripts/backlog/new_story.sh` (339 lines bash)
- **Features**:
  - Auto ID assignment (finds next available CORE-XXX)
  - Template copy & placeholder replacement (7 substitutions)
  - Git branch auto-creation (`feature/CORE-XXX-title`)
  - Makefile integration (`backlog-new`, `backlog-help` targets)
- **Time savings**: 5-10 min manual → 30 sec automated (80-90% faster)
- **Replaced placeholders**: `CORE-XXX`, `EPIC-XXX`, `[Story Title]`, `YYYY-MM-DD`, `P1`, `X days`, assignee
- **Testing**: Manual test successful (CORE-004 created via generator)
- **Documentation**:
  - `docs/development/backlog-workflow.md` - Developer workflow guide (520 lines)
  - `backlog/README.md` - Updated with automation section
  - `backlog/templates/README.md` - Updated with generator usage

**Git-Native Backlog Templates (CORE-001)**
- **3 templates** for User Stories, Subtasks, and Epics:
  - `backlog/templates/story.md` (485 lines) - 8 required sections
  - `backlog/templates/subtask.md` (245 lines) - 6 sections for implementation tasks
  - `backlog/templates/epic.md` (445 lines) - 10 sections for large initiatives
- **Documentation**:
  - `backlog/README.md` (560 lines) - System overview, workflow, tooling
  - `backlog/templates/README.md` (590 lines) - Template usage guide
  - `backlog/index.md` - Dashboard with metrics
- **Features**:
  - GitHub Copilot optimized (stories as prompts)
  - Path mapping (Story ↔ Code ↔ Tests ↔ Docs linking)
  - DoR/DoD checklists for quality gates
  - Self-documenting (meta-epic EPIC-001)
- **Commit**: 83871eb (8 files, 2,809 insertions)

**CI/CD Optimization**
- Skip GitHub Actions workflows for backlog and documentation changes
  - Modified workflows: `ci.yml`, `pre-deploy.yml`, `code-quality.yml`
  - `paths-ignore`: `backlog/**`, `scripts/backlog/**`, `**.md`, `docs/**`
  - Saves CI minutes, reduces noise for non-code changes

#### Post-Deployment Verification
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

