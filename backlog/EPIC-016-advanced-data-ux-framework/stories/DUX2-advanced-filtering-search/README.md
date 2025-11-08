# S2: Advanced Filtering & Search

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🔴 **P0 - CRITICAL**  
**Effort:** ~90 hours  
**Sprint:** 2-3  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Admin / Analyst / Power User,  
**chci** pokročilé filtrování s multi-select, date ranges, drill-down a export do XLS/CSV/PDF,  
**abych** mohl:
- Filtrovat data na více hodnot současně (status IN ['ACTIVE', 'PENDING'])
- Vybrat časové rozsahy s presety (Last 7 days, This month, Custom range)
- Kliknout na chart bar → automaticky filtrovat detail view
- Exportovat výsledky do Excel, CSV nebo PDF reportu
- Sdílet data s kolegy mimo platformu

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Multi-Select Filters

**GIVEN** DataView s Users entitou  
**WHEN** otevřu filter panel a vyberu "Status" filter  
**THEN** zobrazí se multi-select dropdown:
- Možnosti: ACTIVE, PENDING, SUSPENDED, ARCHIVED
- Checkbox u každé hodnoty
- "Select All" / "Clear All" buttons
- Apply button → filtr se aplikuje na DataView

**AND** Cube.js query obsahuje:
```json
{
  "filters": [{
    "member": "Users.status",
    "operator": "equals",
    "values": ["ACTIVE", "PENDING"]
  }]
}
```

### AC2: Date Range Picker

**GIVEN** DataView s časovou dimenzí (createdAt)  
**WHEN** otevřu date filter  
**THEN** zobrazí se date range picker:
- **Preset buttons**: Today, Last 7 days, Last 30 days, This month, Last month, Custom
- **Custom mode**: Od-Do kalendář (react-daterange-picker)
- **Apply button** → filtr na časový rozsah

**AND** Cube.js query:
```json
{
  "timeDimensions": [{
    "dimension": "Users.createdAt",
    "dateRange": ["2024-10-01", "2024-10-31"]
  }]
}
```

### AC3: Drill-Down Navigation

**GIVEN** chart zobrazující "Users by Tenant" (bar chart)  
**WHEN** kliknu na bar pro "Tenant A"  
**THEN** DataView se přefiltruje:
- Zobrazí pouze users z Tenant A
- Breadcrumb: `All Users > Tenant A`
- Back button v breadcrumb → vrátí původní view

**AND** URL state update:
```
/users → /users?tenantId=123
```

### AC4: Export to Excel

**GIVEN** DataView s 500 rows  
**WHEN** kliknu "Export → Excel"  
**THEN** stáhne se `.xlsx` soubor:
- **Sheet 1**: Table data (všechny columns)
- **Sheet 2**: Charts (jako obrázky)
- **Formatting**: Header row bold, auto-width columns
- **Metadata**: Export date v footer

### AC5: Export to CSV

**GIVEN** DataView s table data  
**WHEN** kliknu "Export → CSV"  
**THEN** zobrazí se dialog:
- **Delimiter option**: `,` (default) nebo `;` (pro Excel)
- **Encoding**: UTF-8 with BOM (Excel compatibility)
- **Download** → stáhne `.csv` soubor

### AC6: Export to PDF

**GIVEN** DataView s charts + table  
**WHEN** kliknu "Export → PDF"  
**THEN** vygeneruje se PDF report:
- **Page 1**: Charts (rendered jako images)
- **Page 2+**: Table data (multi-page support)
- **Header**: Company logo, report title, date
- **Footer**: Page numbers

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Multi-Select Filter Component** (15h)

**Cíl:** Reusable multi-select filter component pro všechny dimensions

**Implementation:**

```typescript
// frontend/src/components/filters/MultiSelectFilter.tsx
import React, { useState } from 'react';
import {
  Autocomplete,
  TextField,
  Checkbox,
  Button,
  Box
} from '@mui/material';
import { useCubeQuery } from '@/hooks/useCubeQuery';

interface MultiSelectFilterProps {
  entity: string;
  dimension: string;
  label: string;
  onApply: (values: string[]) => void;
  initialValues?: string[];
}

export const MultiSelectFilter: React.FC<MultiSelectFilterProps> = ({
  entity,
  dimension,
  label,
  onApply,
  initialValues = []
}) => {
  const [selectedValues, setSelectedValues] = useState<string[]>(initialValues);

  // Load distinct values for dimension
  const { data: distinctValues } = useCubeQuery({
    dimensions: [`${entity}.${dimension}`],
    measures: [`${entity}.count`],
    order: { [`${entity}.${dimension}`]: 'asc' }
  });

  const options = distinctValues?.map(row => row[`${entity}.${dimension}`]) || [];

  const handleApply = () => {
    onApply(selectedValues);
  };

  const handleClear = () => {
    setSelectedValues([]);
  };

  const handleSelectAll = () => {
    setSelectedValues(options);
  };

  return (
    <Box>
      <Autocomplete
        multiple
        options={options}
        value={selectedValues}
        onChange={(_, newValue) => setSelectedValues(newValue)}
        disableCloseOnSelect
        renderInput={(params) => (
          <TextField {...params} label={label} placeholder="Select values" />
        )}
        renderOption={(props, option, { selected }) => (
          <li {...props}>
            <Checkbox checked={selected} />
            {option}
          </li>
        )}
      />

      <Box sx={{ mt: 2, display: 'flex', gap: 1 }}>
        <Button size="small" onClick={handleSelectAll}>
          Select All
        </Button>
        <Button size="small" onClick={handleClear}>
          Clear
        </Button>
        <Button
          variant="contained"
          onClick={handleApply}
          disabled={selectedValues.length === 0}
        >
          Apply ({selectedValues.length})
        </Button>
      </Box>
    </Box>
  );
};
```

**Usage:**

```typescript
// In DataView component
<MultiSelectFilter
  entity="Users"
  dimension="status"
  label="Status"
  onApply={(values) => {
    setFilters([
      ...filters,
      {
        member: 'Users.status',
        operator: 'equals',
        values
      }
    ]);
  }}
/>
```

**Deliverable:** Reusable multi-select filter with Cube.js integration

---

#### **T2: Date Range Picker** (10h)

**Cíl:** Date range selector s presety a custom range

**Implementation:**

```typescript
// frontend/src/components/filters/DateRangePicker.tsx
import React, { useState } from 'react';
import { DateRangePicker as ReactDateRangePicker } from 'react-date-range';
import { Button, ButtonGroup, Box, Popover } from '@mui/material';
import { addDays, startOfMonth, endOfMonth, subMonths } from 'date-fns';
import 'react-date-range/dist/styles.css';
import 'react-date-range/dist/theme/default.css';

interface DateRange {
  startDate: Date;
  endDate: Date;
}

interface DateRangePickerProps {
  onApply: (startDate: string, endDate: string) => void;
  initialRange?: DateRange;
}

const PRESETS = [
  {
    label: 'Today',
    getValue: () => ({
      startDate: new Date(),
      endDate: new Date()
    })
  },
  {
    label: 'Last 7 Days',
    getValue: () => ({
      startDate: addDays(new Date(), -7),
      endDate: new Date()
    })
  },
  {
    label: 'Last 30 Days',
    getValue: () => ({
      startDate: addDays(new Date(), -30),
      endDate: new Date()
    })
  },
  {
    label: 'This Month',
    getValue: () => ({
      startDate: startOfMonth(new Date()),
      endDate: endOfMonth(new Date())
    })
  },
  {
    label: 'Last Month',
    getValue: () => {
      const lastMonth = subMonths(new Date(), 1);
      return {
        startDate: startOfMonth(lastMonth),
        endDate: endOfMonth(lastMonth)
      };
    }
  }
];

export const DateRangePicker: React.FC<DateRangePickerProps> = ({
  onApply,
  initialRange
}) => {
  const [range, setRange] = useState<DateRange>(
    initialRange || PRESETS[2].getValue() // Default: Last 30 days
  );
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const handlePresetClick = (preset: typeof PRESETS[0]) => {
    const newRange = preset.getValue();
    setRange(newRange);
    onApply(
      newRange.startDate.toISOString().split('T')[0],
      newRange.endDate.toISOString().split('T')[0]
    );
  };

  const handleCustomApply = () => {
    onApply(
      range.startDate.toISOString().split('T')[0],
      range.endDate.toISOString().split('T')[0]
    );
    setAnchorEl(null);
  };

  return (
    <Box>
      {/* Preset Buttons */}
      <ButtonGroup size="small" sx={{ mb: 1 }}>
        {PRESETS.map(preset => (
          <Button
            key={preset.label}
            onClick={() => handlePresetClick(preset)}
          >
            {preset.label}
          </Button>
        ))}
        <Button onClick={(e) => setAnchorEl(e.currentTarget)}>
          Custom
        </Button>
      </ButtonGroup>

      {/* Custom Range Popover */}
      <Popover
        open={Boolean(anchorEl)}
        anchorEl={anchorEl}
        onClose={() => setAnchorEl(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
      >
        <Box sx={{ p: 2 }}>
          <ReactDateRangePicker
            ranges={[{
              startDate: range.startDate,
              endDate: range.endDate,
              key: 'selection'
            }]}
            onChange={(item: any) => setRange(item.selection)}
            moveRangeOnFirstSelection={false}
          />
          <Button
            fullWidth
            variant="contained"
            sx={{ mt: 2 }}
            onClick={handleCustomApply}
          >
            Apply Custom Range
          </Button>
        </Box>
      </Popover>

      {/* Current Selection Display */}
      <Box sx={{ mt: 1, fontSize: '0.875rem', color: 'text.secondary' }}>
        Selected: {range.startDate.toLocaleDateString()} - {range.endDate.toLocaleDateString()}
      </Box>
    </Box>
  );
};
```

**Deliverable:** Date range picker s 5 presety + custom range

---

#### **T3: Drill-Down System** (20h)

**Cíl:** Click na chart → auto-filter + breadcrumb navigation

**Implementation:**

```typescript
// frontend/src/components/drill-down/DrillDownManager.tsx
import React, { createContext, useContext, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { Breadcrumbs, Link, Typography } from '@mui/material';
import { NavigateNext } from '@mui/icons-material';

interface DrillDownLevel {
  label: string;
  filter: CubeFilter;
  url: string;
}

interface DrillDownContextType {
  levels: DrillDownLevel[];
  drillDown: (label: string, filter: CubeFilter) => void;
  drillUp: (levelIndex: number) => void;
  reset: () => void;
}

const DrillDownContext = createContext<DrillDownContextType | null>(null);

export const DrillDownProvider: React.FC<{ children: React.ReactNode }> = ({
  children
}) => {
  const [levels, setLevels] = useState<DrillDownLevel[]>([]);
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const drillDown = (label: string, filter: CubeFilter) => {
    const newLevel: DrillDownLevel = {
      label,
      filter,
      url: window.location.pathname + window.location.search
    };

    setLevels(prev => [...prev, newLevel]);

    // Update URL with filter
    const filterParam = encodeURIComponent(JSON.stringify(filter));
    searchParams.set('drill', filterParam);
    setSearchParams(searchParams);
  };

  const drillUp = (levelIndex: number) => {
    const targetLevel = levels[levelIndex];
    setLevels(levels.slice(0, levelIndex + 1));
    navigate(targetLevel.url);
  };

  const reset = () => {
    setLevels([]);
    searchParams.delete('drill');
    setSearchParams(searchParams);
  };

  return (
    <DrillDownContext.Provider value={{ levels, drillDown, drillUp, reset }}>
      {children}
    </DrillDownContext.Provider>
  );
};

export const useDrillDown = () => {
  const context = useContext(DrillDownContext);
  if (!context) throw new Error('useDrillDown must be used within DrillDownProvider');
  return context;
};

// Breadcrumb Component
export const DrillDownBreadcrumb: React.FC = () => {
  const { levels, drillUp, reset } = useDrillDown();

  if (levels.length === 0) return null;

  return (
    <Breadcrumbs
      separator={<NavigateNext fontSize="small" />}
      sx={{ mb: 2 }}
    >
      <Link
        component="button"
        variant="body2"
        onClick={reset}
        sx={{ cursor: 'pointer' }}
      >
        All
      </Link>
      {levels.map((level, index) => (
        index === levels.length - 1 ? (
          <Typography key={index} variant="body2" color="text.primary">
            {level.label}
          </Typography>
        ) : (
          <Link
            key={index}
            component="button"
            variant="body2"
            onClick={() => drillUp(index)}
            sx={{ cursor: 'pointer' }}
          >
            {level.label}
          </Link>
        )
      ))}
    </Breadcrumbs>
  );
};
```

**Chart Integration:**

```typescript
// In ChartViewRenderer
import { useDrillDown } from './DrillDownManager';

const ChartViewRenderer = ({ data, schema }) => {
  const { drillDown } = useDrillDown();

  const handleBarClick = (barData: any) => {
    // Extract filter from clicked bar
    const dimension = schema.dimensions[0].name;
    const value = barData.name;

    drillDown(
      `${schema.dimensions[0].title}: ${value}`,
      {
        member: `${schema.entity}.${dimension}`,
        operator: 'equals',
        values: [value]
      }
    );
  };

  return (
    <>
      <DrillDownBreadcrumb />
      <BarChart data={data} onClick={handleBarClick}>
        {/* ... */}
      </BarChart>
    </>
  );
};
```

**Deliverable:** Drill-down system s breadcrumb navigation

---

#### **T4: Export to Excel** (15h)

**Cíl:** Export DataView data do Excel (.xlsx) s multi-sheet

**Implementation:**

```typescript
// frontend/src/utils/export/excelExport.ts
import * as XLSX from 'xlsx';
import { saveAs } from 'file-saver';

interface ExportOptions {
  filename: string;
  sheets: {
    name: string;
    data: any[];
    columns?: string[];
  }[];
  metadata?: {
    title?: string;
    author?: string;
    exportDate?: string;
  };
}

export async function exportToExcel(options: ExportOptions): Promise<void> {
  const workbook = XLSX.utils.book_new();

  // Add each sheet
  options.sheets.forEach(sheet => {
    const worksheet = XLSX.utils.json_to_sheet(sheet.data, {
      header: sheet.columns
    });

    // Auto-width columns
    const columnWidths = sheet.columns?.map(col => ({
      wch: Math.max(
        col.length,
        ...sheet.data.map(row => String(row[col] || '').length)
      )
    })) || [];
    worksheet['!cols'] = columnWidths;

    // Bold header row
    const range = XLSX.utils.decode_range(worksheet['!ref'] || 'A1');
    for (let col = range.s.c; col <= range.e.c; col++) {
      const cellAddress = XLSX.utils.encode_cell({ r: 0, c: col });
      if (!worksheet[cellAddress]) continue;
      worksheet[cellAddress].s = {
        font: { bold: true },
        fill: { fgColor: { rgb: 'E0E0E0' } }
      };
    }

    XLSX.utils.book_append_sheet(workbook, worksheet, sheet.name);
  });

  // Add metadata sheet
  if (options.metadata) {
    const metaSheet = XLSX.utils.json_to_sheet([options.metadata]);
    XLSX.utils.book_append_sheet(workbook, metaSheet, 'Metadata');
  }

  // Generate Excel file
  const excelBuffer = XLSX.write(workbook, {
    bookType: 'xlsx',
    type: 'array'
  });

  const blob = new Blob([excelBuffer], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  });

  saveAs(blob, `${options.filename}.xlsx`);
}
```

**Usage in DataView:**

```typescript
const handleExportExcel = async () => {
  await exportToExcel({
    filename: `users-export-${new Date().toISOString().split('T')[0]}`,
    sheets: [
      {
        name: 'Users',
        data: tableData,
        columns: ['id', 'name', 'email', 'status', 'createdAt']
      },
      {
        name: 'Summary',
        data: [
          { Metric: 'Total Users', Value: tableData.length },
          { Metric: 'Active Users', Value: tableData.filter(u => u.status === 'ACTIVE').length }
        ]
      }
    ],
    metadata: {
      title: 'Users Export',
      author: currentUser.name,
      exportDate: new Date().toISOString()
    }
  });
};
```

**Deliverable:** Excel export s formatting a metadata

---

#### **T5: Export to CSV** (8h)

**Implementation:**

```typescript
// frontend/src/utils/export/csvExport.ts
import { stringify } from 'csv-stringify/browser/esm/sync';
import { saveAs } from 'file-saver';

interface CSVExportOptions {
  filename: string;
  data: any[];
  columns?: string[];
  delimiter?: ',' | ';';
  includeHeaders?: boolean;
  encoding?: 'utf-8' | 'utf-8-bom';
}

export function exportToCSV(options: CSVExportOptions): void {
  const {
    filename,
    data,
    columns,
    delimiter = ',',
    includeHeaders = true,
    encoding = 'utf-8-bom'
  } = options;

  // Generate CSV string
  const csvString = stringify(data, {
    header: includeHeaders,
    columns: columns,
    delimiter: delimiter,
    quoted: true
  });

  // Add UTF-8 BOM for Excel compatibility
  const bom = encoding === 'utf-8-bom' ? '\uFEFF' : '';
  const csvContent = bom + csvString;

  const blob = new Blob([csvContent], {
    type: 'text/csv;charset=utf-8;'
  });

  saveAs(blob, `${filename}.csv`);
}
```

**UI Dialog:**

```typescript
const [csvDialogOpen, setCsvDialogOpen] = useState(false);
const [csvDelimiter, setCsvDelimiter] = useState<',' | ';'>(',');

<Dialog open={csvDialogOpen} onClose={() => setCsvDialogOpen(false)}>
  <DialogTitle>Export to CSV</DialogTitle>
  <DialogContent>
    <FormControl>
      <FormLabel>Delimiter</FormLabel>
      <RadioGroup value={csvDelimiter} onChange={(e) => setCsvDelimiter(e.target.value)}>
        <FormControlLabel value="," control={<Radio />} label="Comma (,) - Standard" />
        <FormControlLabel value=";" control={<Radio />} label="Semicolon (;) - Excel Europe" />
      </RadioGroup>
    </FormControl>
  </DialogContent>
  <DialogActions>
    <Button onClick={() => setCsvDialogOpen(false)}>Cancel</Button>
    <Button
      variant="contained"
      onClick={() => {
        exportToCSV({
          filename: 'users-export',
          data: tableData,
          delimiter: csvDelimiter
        });
        setCsvDialogOpen(false);
      }}
    >
      Export
    </Button>
  </DialogActions>
</Dialog>
```

**Deliverable:** CSV export s custom delimiter a BOM

---

#### **T6: Export to PDF** (20h)

**Implementation:**

```typescript
// frontend/src/utils/export/pdfExport.ts
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import html2canvas from 'html2canvas';

interface PDFExportOptions {
  filename: string;
  title: string;
  charts?: HTMLElement[];
  tableData?: any[];
  tableColumns?: string[];
  metadata?: {
    author?: string;
    date?: string;
  };
}

export async function exportToPDF(options: PDFExportOptions): Promise<void> {
  const pdf = new jsPDF('p', 'mm', 'a4');
  let yPosition = 20;

  // Header
  pdf.setFontSize(20);
  pdf.text(options.title, 20, yPosition);
  yPosition += 10;

  // Metadata
  if (options.metadata) {
    pdf.setFontSize(10);
    pdf.setTextColor(128);
    pdf.text(`Exported by: ${options.metadata.author}`, 20, yPosition);
    yPosition += 5;
    pdf.text(`Date: ${options.metadata.date}`, 20, yPosition);
    yPosition += 15;
    pdf.setTextColor(0);
  }

  // Charts (as images)
  if (options.charts && options.charts.length > 0) {
    for (const chartElement of options.charts) {
      const canvas = await html2canvas(chartElement);
      const imgData = canvas.toDataURL('image/png');
      
      const imgWidth = 170;
      const imgHeight = (canvas.height * imgWidth) / canvas.width;

      if (yPosition + imgHeight > 270) {
        pdf.addPage();
        yPosition = 20;
      }

      pdf.addImage(imgData, 'PNG', 20, yPosition, imgWidth, imgHeight);
      yPosition += imgHeight + 10;
    }
  }

  // Table
  if (options.tableData && options.tableColumns) {
    if (yPosition > 250) {
      pdf.addPage();
      yPosition = 20;
    }

    autoTable(pdf, {
      head: [options.tableColumns],
      body: options.tableData.map(row => 
        options.tableColumns!.map(col => row[col])
      ),
      startY: yPosition,
      styles: { fontSize: 8 },
      headStyles: { fillColor: [66, 139, 202] }
    });
  }

  // Footer (page numbers)
  const pageCount = pdf.getNumberOfPages();
  for (let i = 1; i <= pageCount; i++) {
    pdf.setPage(i);
    pdf.setFontSize(10);
    pdf.setTextColor(128);
    pdf.text(
      `Page ${i} of ${pageCount}`,
      pdf.internal.pageSize.width / 2,
      pdf.internal.pageSize.height - 10,
      { align: 'center' }
    );
  }

  pdf.save(`${options.filename}.pdf`);
}
```

**Usage:**

```typescript
const handleExportPDF = async () => {
  const chartElements = document.querySelectorAll('.chart-widget');
  
  await exportToPDF({
    filename: 'users-report',
    title: 'Users Analytics Report',
    charts: Array.from(chartElements) as HTMLElement[],
    tableData: tableData,
    tableColumns: ['name', 'email', 'status'],
    metadata: {
      author: currentUser.name,
      date: new Date().toLocaleDateString()
    }
  });
};
```

**Deliverable:** PDF export s charts, tables, headers, footers

---

## 🧪 TESTING

### E2E Tests

```typescript
// e2e/specs/filters/advanced-filtering.spec.ts
import { test, expect } from '@playwright/test';

test('Multi-select filter works', async ({ page }) => {
  await page.goto('/users');

  // Open filter panel
  await page.click('button:has-text("Filters")');

  // Select status filter
  await page.click('input[placeholder="Select values"]');
  await page.click('li:has-text("ACTIVE")');
  await page.click('li:has-text("PENDING")');

  // Apply filter
  await page.click('button:has-text("Apply (2)")');

  // Verify table filtered
  await expect(page.locator('tbody tr')).toHaveCount(2);
});

test('Date range preset works', async ({ page }) => {
  await page.goto('/users');

  // Click Last 30 Days preset
  await page.click('button:has-text("Last 30 Days")');

  // Verify URL updated
  expect(page.url()).toContain('dateRange=');
});

test('Drill-down navigation works', async ({ page }) => {
  await page.goto('/analytics/users-by-tenant');

  // Click on chart bar (Tenant A)
  await page.click('.recharts-bar >> nth=0');

  // Verify breadcrumb appeared
  await expect(page.locator('nav[aria-label="breadcrumb"]')).toBeVisible();
  await expect(page.locator('text=Tenant: Tenant A')).toBeVisible();

  // Verify table filtered
  await expect(page.locator('tbody tr')).toHaveCount(5);
});

test('Export to Excel works', async ({ page }) => {
  await page.goto('/users');

  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.click('button:has-text("Export → Excel")')
  ]);

  expect(download.suggestedFilename()).toMatch(/users-export.*\.xlsx/);
});
```

---

## 📊 SUCCESS METRICS

- ✅ Filter apply < 500ms
- ✅ Multi-select supports 1000+ options (virtualized dropdown)
- ✅ Date range picker responsive on mobile
- ✅ Drill-down navigation preserves state on browser back
- ✅ Export 1000 rows XLS < 2s
- ✅ PDF generation < 5s (with 3 charts + table)
- ✅ CSV export UTF-8 BOM for Excel compatibility

---

## 🔗 DEPENDENCIES

- **EPIC-004 S1:** Cube.js schemas ✅
- **S1:** DataView component (filter target)
- **Libraries:** xlsx, csv-stringify, jsPDF, jsPDF-autoTable, html2canvas, react-date-range, file-saver

---

## 📚 DOCUMENTATION

- [ ] User Guide: Advanced Filtering & Export
- [ ] Developer Guide: How to add custom filter types
- [ ] API Doc: Export utilities (excelExport, csvExport, pdfExport)

---

**Status:** 📋 TODO → Ready for implementation  
**Next:** S3: Dashboard Grid Layout

