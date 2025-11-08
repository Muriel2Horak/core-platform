# S7: Extended Widget Types

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO** | **Priority:** 🟡 **P1** | **Effort:** ~150h | **Sprint:** 6-8

---

## 📋 USER STORY

**Jako** Dashboard Designer, **chci** rozšířené widget typy (Heatmap, Sankey, Treemap, Network, KPI tiles), **abych** mohl vizualizovat komplexní data.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Heatmap**: 2D density map (workflow activity by hour × day)
2. **Sankey**: Flow diagram (user journey through workflows)
3. **Treemap**: Hierarchical data (tenant sizes)
4. **Network Graph**: Relationship visualization (user → tenant → workflows)
5. **KPI Tiles**: Big number + trend indicator (↑ 12% vs. last week)

---

## 🏗️ TASK BREAKDOWN (~150h)

### T1: Heatmap Widget (25h)
- Nivo Charts ResponsiveHeatMap
- Data transformation (2D array)

### T2: Sankey Widget (30h)
- Nivo Sankey diagram
- Node/link data structure

### T3: Treemap Widget (20h)
- Nivo Treemap
- Hierarchical data support

### T4: Network Graph Widget (40h)
- D3.js force-directed graph
- Node drag, zoom, pan

### T5: KPI Tile Widget (20h)
- Big number display
- Trend calculation (sparkline, arrow, %)

### T6: Widget Configuration UI (10h)
- Settings panel per widget type

### T7: Testing (5h)

---

## 📦 DEPENDENCIES

- **EPIC-014 S9**: Table component (for data grids in widgets)
- **Libraries**: @nivo/heatmap, @nivo/sankey, D3.js

---

## 📊 SUCCESS METRICS

- 10+ widget types available
- Widget render < 1s

