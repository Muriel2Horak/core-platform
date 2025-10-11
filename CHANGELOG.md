# Changelog

All notable changes to the Core Platform project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

