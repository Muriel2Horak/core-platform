# T3: Backend API Endpoints
**Effort:** ~4h | **LOC:** ~600

## Objective
REST API pro dashboard data

## Files
- `backend/src/main/java/cz/muriel/core/monitoring/DashboardDataController.java`
- `backend/src/main/java/cz/muriel/core/monitoring/DashboardService.java`
- `backend/src/main/java/cz/muriel/core/monitoring/dto/TenantOverviewDTO.java`
- `backend/src/main/java/cz/muriel/core/monitoring/dto/UserActivityDTO.java`
- `backend/src/main/java/cz/muriel/core/monitoring/dto/SystemHealthDTO.java`

## Acceptance Criteria
- GET `/api/admin/monitoring/dashboards/overview` → 200 OK
- GET `/api/admin/monitoring/dashboards/user-activity` → 200 OK
- GET `/api/admin/monitoring/dashboards/system-health` → 200 OK
- Response time < 500ms

## Implementation
See [S8 Story](../S8.md#t3-backend-api-endpoints)
