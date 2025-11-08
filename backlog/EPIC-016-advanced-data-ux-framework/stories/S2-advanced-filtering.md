# S2: Advanced Filtering & Search

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO** | **Priority:** 🔴 **P0** | **Effort:** ~90h | **Sprint:** 2-3

---

## 📋 USER STORY

**Jako** Admin/Analyst, **chci** pokročilé filtrování (multi-select, date ranges, drill-down) a export (XLS/CSV/PDF), **abych** mohl analyzovat data a sdílet výsledky.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Multi-Select Filters**: Filtr na více hodnot (`status IN ['ACTIVE', 'PENDING']`)
2. **Date Range Picker**: Od-Do s presets (Last 7 days, This month)
3. **Drill-Down**: Klik na chart → filtr na detail (Users by Tenant → filtr tenantId)
4. **Export XLS**: Export table/chart data do Excel (.xlsx)
5. **Export CSV**: Export s custom delimiter (`,` nebo `;`)
6. **Export PDF**: PDF report s charts + tables (jsPDF)

---

## 🏗️ TASK BREAKDOWN (~90h)

### T1: Multi-Select Filter Component (15h)
- MUI Autocomplete s multiple select
- Cube.js filter transformation (`status: ['ACTIVE', 'PENDING']`)
- Apply/Clear buttons

### T2: Date Range Picker (10h)
- React Date Range library
- Presets: Today, Last 7 days, Last 30 days, This month, Last month
- Cube.js time dimension filters

### T3: Drill-Down System (20h)
- Click handler na chart bars/lines
- Extract filter from clicked data point
- Update parent DataView filters
- Breadcrumb navigation (Users > Tenant A > Active Users)

### T4: Export to Excel (15h)
- xlsx library integration
- Transform DataView data → Excel workbook
- Multi-sheet support (Table + Charts)
- Download trigger

### T5: Export to CSV (8h)
- csv-stringify library
- Custom delimiter option
- UTF-8 BOM for Excel compatibility

### T6: Export to PDF (20h)
- jsPDF + jsPDF-AutoTable
- Render charts as images (html2canvas)
- Multi-page support
- Custom header/footer (company logo, date)

### T7: Testing (2h E2E)

---

## 📦 DEPENDENCIES

- **EPIC-004 S1**: Cube.js schemas ✅
- **Libraries**: xlsx, csv-stringify, jsPDF, react-daterange-picker

---

## 📊 SUCCESS METRICS

- Filter apply < 500ms
- Export 1000 rows XLS < 2s
- PDF generation < 5s

