# T3: Backend API Endpoints
**Effort:** ~4h | **LOC:** ~600

## Goal
Pripravit REST API pro data business dashboardu.

## Files
- `backend/src/main/java/cz/muriel/core/monitoring/DashboardDataController.java`
- `backend/src/main/java/cz/muriel/core/monitoring/DashboardService.java`
- `backend/src/main/java/cz/muriel/core/monitoring/dto/TenantOverviewDTO.java`
- `backend/src/main/java/cz/muriel/core/monitoring/dto/UserActivityDTO.java`
- `backend/src/main/java/cz/muriel/core/monitoring/dto/SystemHealthDTO.java`

## Tasks
- [ ] Navrhnout endpointy pro overview, user-activity a system-health.
- [ ] Zajistit tenant scoping (realm/tenant_id filter).
- [ ] Pridat cache pro agregace s vysokou zatezi.
- [ ] Doplnit basic contract testy.

## Output
- BFF API pro business dashboardy s tenant izolaci.

## Acceptance Criteria
- GET `/api/admin/monitoring/dashboards/overview` → 200 OK
- GET `/api/admin/monitoring/dashboards/user-activity` → 200 OK
- GET `/api/admin/monitoring/dashboards/system-health` → 200 OK
- Response time < 500ms
