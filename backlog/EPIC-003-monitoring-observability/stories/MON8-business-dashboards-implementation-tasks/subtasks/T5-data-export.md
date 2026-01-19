# T5: Data Export (CSV/JSON/PDF)
**Effort:** ~3h | **LOC:** ~200

## Goal
Export dashboard dat do ruznych formatu.

## Files
- `frontend/src/utils/exportDashboardData.ts`
- `backend/src/main/java/cz/muriel/core/monitoring/DashboardExportController.java`

## Tasks
- [ ] Pridat export CSV/JSON v backendu i FE.
- [ ] Generovat PDF z aktualniho layoutu (server/client).
- [ ] Respektovat tenant scope a auditovat export akce.

## Output
- Funkcni export dat z dashboardu do CSV/JSON/PDF.

## Acceptance Criteria
- CSV export funguje
- JSON export funguje
- PDF export zachovává layout
- Download trigger správný filename
