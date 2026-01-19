# T2: Metric Cards & Charts Library
**Effort:** ~6h | **LOC:** ~800

## Goal
Vytvorit reusable komponenty pro metriky a grafy.

## Files
- `frontend/src/components/monitoring/MetricCard.tsx`
- `frontend/src/components/monitoring/charts/LineChartWidget.tsx`
- `frontend/src/components/monitoring/charts/BarChartWidget.tsx`
- `frontend/src/components/monitoring/charts/PieChartWidget.tsx`
- `frontend/src/components/monitoring/charts/GaugeWidget.tsx`
- `frontend/src/components/monitoring/charts/HeatmapWidget.tsx`

## Tasks
- [ ] Implementovat MetricCard s loading/error states.
- [ ] Dopsat zakladni chart widgety (line/bar/pie/gauge/heatmap).
- [ ] Pridat theme a barevne palety pro severity.
- [ ] Osetrit responsive sizing a empty state.

## Output
- Komponenty pro metriky a grafy pouzitelne napric dashboardy.

## Acceptance Criteria
- Všechny chart typy fungují
- Responsive sizing
- Custom color schemes
- Loading states
- MUI X Charts integration
