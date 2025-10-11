# S1: Naming Conventions - Implementation Summary

**Phase**: S1 of Platform Hardening Epic  
**Status**: 🟢 75% Complete (Infrastructure Done)  
**Time**: 6h / 8h  
**Branch**: `feature/platform-hardening-epic`  
**Commits**: 4 commits (a8318fd → 3de07a2)

---

## 📦 Deliverables

### ✅ 1. Naming Guide (docs/NAMING_GUIDE.md)

**530+ lines** of comprehensive naming conventions covering all platform layers.

#### Coverage:
- **Entity & Domain Model**: PascalCase singular (User, UserDirectory)
- **Database**:
  - Tables: snake_case plural (users, user_directories)
  - Columns: snake_case singular (first_name, created_at)
  - Mandatory: id, tenant_id, created_at, updated_at, version
  - Foreign keys: {entity}_id pattern
  - Indexes: idx_{table}_{columns}
  
- **REST API**:
  - URLs: kebab-case plural (/api/users, /api/user-directories)
  - Query params: camelCase (sortBy, pageSize)
  - Controllers: {EntityPlural}Controller
  
- **JSON (Backend & Frontend)**:
  - Keys: camelCase (firstName, userId, createdAt)
  - Consistent across DTOs and interfaces
  
- **Cube.js Analytics**:
  - Cubes: PascalCase plural (Users, UserDirectories)
  - Measures/Dimensions: camelCase (firstName, avgAge, totalCount)
  - Pre-aggregations: camelCase (usersByDay)
  
- **Kafka Topics**:
  - Pattern: product.context.entity.event
  - All segments: kebab-case
  - Suffixes: -retry, -dlq
  - Example: core.user-management.user.created
  
- **Prometheus Metrics**:
  - Convention: snake_case with suffixes
  - Suffixes: _seconds, _total, _bytes, _ratio
  - Example: reporting_query_duration_seconds

#### Special Features:
- Full-stack examples (User, UserDirectory entities)
- Migration/compatibility guide (@Deprecated, nginx redirects)
- Anti-patterns section (what to avoid)
- Checklist for new features (14-point verification)
- Conversion utilities (PascalCase → snake_case, etc.)

---

### ✅ 2. Automated Linting (tools/naming-lint/)

**Node.js 20+ tooling** with 4 specialized linters.

#### Linters:

##### lint-metamodel.js
- Validates metamodel JSON files
- Checks:
  - File name: PascalCase singular
  - Entity name matches file name
  - Field names: camelCase
  - Required fields: id, tenantId, createdAt, updatedAt, version
  - Table name: snake_case plural
- **Result**: No metamodel files found (OK - not yet implemented)

##### lint-api.js
- Validates Spring REST controllers
- Checks:
  - Path segments: kebab-case
  - Resource names: plural
  - Controller name matches path
- **Result**: 4 warnings (legacy controller naming, acceptable)
  - ReportViewController → /api/reports/views
  - ReportQueryController → /api/reports
  - EntityCrudController → /api/entities
  - BulkUpdateController → /api

##### lint-kafka.js
- Validates Kafka topic names
- Checks:
  - Pattern: product.context.entity.event
  - All segments: kebab-case
  - Minimum 3 segments
  - Skips Spring EL expressions (#{...})
- **Result**: No Kafka topics found (OK - not yet implemented)

##### lint-db.js
- Validates Flyway migrations
- Checks:
  - File name: V{YYYYMMDDHHMM}__{description}.sql or R__{description}.sql
  - Description: snake_case
  - Tables: snake_case, must have tenant_id (with allowlist)
  - Columns: snake_case (SQL keywords filtered)
- **Result**: 3 warnings (legacy V1__, V2__ patterns, acceptable)
- **Allowlist**: 16 tables (system, junction, audit, master tables)

#### Utilities:

**casing.js** (conversion & validation):
- `isPascalCase()`, `isCamelCase()`, `isSnakeCase()`, `isKebabCase()`
- `pascalToSnake()`, `pascalToKebab()`
- `pluralize()`, `singularize()` (basic English rules)

**reporter.js** (colorized output):
- Error/warning tracking
- Colorized terminal output (chalk)
- Summary with checked file count
- Exit codes: 0 (pass), 1 (fail)

#### NPM Scripts:
```bash
npm run lint:metamodel
npm run lint:api
npm run lint:kafka
npm run lint:db
npm run lint:all  # Run all lints
npm test          # Unit tests (pending)
```

#### Accuracy:
- **0 errors** (CI will pass ✅)
- **7 warnings** (all legacy patterns, acceptable)
  - 4 from lint:api (controller naming)
  - 3 from lint:db (V1__/V2__ migration patterns)

---

### ✅ 3. CI Integration (.github/workflows/naming-lint.yml)

**GitHub Actions workflow** to enforce naming conventions.

#### Configuration:
- **Triggers**:
  - Pull requests → main, develop, feature/**
  - Push → main, develop
  - Paths: backend/, frontend/, tools/naming-lint/
  
- **Steps**:
  1. Checkout code
  2. Setup Node.js 20 (with npm cache)
  3. Install dependencies (npm ci)
  4. Run lint:metamodel
  5. Run lint:api
  6. Run lint:kafka
  7. Run lint:db
  8. Summary (always runs)

- **Timeout**: 5 minutes
- **Exit**: Fails build on errors (not warnings)

#### Merge Gate:
All PRs must pass naming lints before merge.

---

### ⏳ 4. Refactoring + Aliases (Pending - 2h)

**Not yet started** - planned for S1 completion.

#### Scope:
1. **REST Path Refactoring**:
   - ReportViewController: /api/reports/views → /api/report-views
   - ReportQueryController: /api/reports → /api/reports (OK)
   - EntityCrudController: /api/entities → /api/entities (OK)
   - BulkUpdateController: /api → /api/bulk-updates

2. **JSON DTO Updates**:
   - Audit all DTOs for snake_case keys
   - Convert to camelCase (firstName, userId, createdAt)

3. **Cube Schema Standardization**:
   - Ensure all cubes: PascalCase plural (Users, Groups, Tenants)
   - Ensure all measures/dimensions: camelCase

4. **Migration Aliases**:
   - Add @Deprecated annotations to old controllers
   - Nginx 301 redirects for legacy endpoints
   - Document in CHANGELOG.md

---

## 📊 Statistics

### Files Created:
- `docs/NAMING_GUIDE.md` (530 lines)
- `tools/naming-lint/package.json` (23 lines)
- `tools/naming-lint/README.md` (180 lines)
- `tools/naming-lint/src/lint-metamodel.js` (85 lines)
- `tools/naming-lint/src/lint-api.js` (95 lines)
- `tools/naming-lint/src/lint-kafka.js` (110 lines)
- `tools/naming-lint/src/lint-db.js` (130 lines)
- `tools/naming-lint/src/utils/casing.js` (95 lines)
- `tools/naming-lint/src/utils/reporter.js` (60 lines)
- `.github/workflows/naming-lint.yml` (45 lines)
- `docs/PLATFORM_HARDENING_EPIC.md` (272 lines)

**Total**: 11 files, ~1,625 lines

### Git Commits:
1. **a8318fd**: feat(platform): S1 - Add naming conventions guide and automated linters
2. **f26e21d**: fix(naming-lint): Improve lint accuracy - reduce false positives
3. **5f3b93b**: chore(naming-lint): Add package-lock.json for reproducible builds
4. **3de07a2**: docs(epic): Update S1 progress - 75% complete

**Total**: 4 commits, 1,962 insertions(+)

### NPM Dependencies:
- `glob` ^10.3.10 (file searching)
- `chalk` ^5.3.0 (colorized output)
- Total: 44 packages (0 vulnerabilities)

---

## 🎯 DoD Progress

| Deliverable | Status | Notes |
|-------------|--------|-------|
| Naming guide created | ✅ Done | 530+ lines, all layers covered |
| Linting tools implemented | ✅ Done | 4 linters + utilities |
| CI integration added | ✅ Done | GitHub Actions workflow |
| Refactored existing code | ⏳ Pending | 2h remaining |
| Migration aliases | ⏳ Pending | Part of refactoring |

**Progress**: 3/5 deliverables (60% DoD)  
**Time**: 6h / 8h (75% time)

---

## 🚀 Next Steps (2h remaining)

### Priority 1: Refactor REST Paths (1h)
- Update controller `@RequestMapping` to kebab-case plural
- Add `@Deprecated` aliases for old paths
- Test endpoints still work

### Priority 2: Add Nginx Redirects (0.5h)
- Add 301 redirects in nginx config
- Test redirects with curl

### Priority 3: JSON DTO Audit (0.5h)
- Scan all DTOs for snake_case keys
- Convert to camelCase if needed
- Update frontend interfaces

**Estimated Completion**: +2h from now

---

## 📝 Lessons Learned

### What Went Well:
1. **Comprehensive Guide**: 530+ lines covering all layers
2. **Accurate Linting**: 0 errors, minimal false positives
3. **Clean CI Integration**: 5-minute timeout, clear failure modes
4. **Good Documentation**: README with examples and troubleshooting

### Challenges:
1. **False Positives**: Initial DB lint flagged SQL keywords as column names
   - **Solution**: Added SQL keyword filtering
2. **Legacy Patterns**: Existing code uses V1__, V2__ migration naming
   - **Solution**: Added legacy pattern support (warnings, not errors)
3. **Dynamic Topics**: Kafka lint flagged Spring EL expressions
   - **Solution**: Skip patterns with #{...} or $

### Improvements for Future Phases:
1. Add unit tests for linters (use Node.js test runner)
2. Add VS Code extension for inline naming hints
3. Auto-fix capabilities (suggest kebab-case alternatives)

---

## 🔗 References

- **Epic**: docs/PLATFORM_HARDENING_EPIC.md
- **Naming Guide**: docs/NAMING_GUIDE.md
- **Lint Tools**: tools/naming-lint/
- **CI Workflow**: .github/workflows/naming-lint.yml
- **CHANGELOG**: CHANGELOG.md (S1 entry added)

---

**Last Updated**: 2025-10-11  
**Author**: Platform Team  
**Epic Phase**: S1 (Naming Conventions)  
**Status**: 🟢 75% Complete
