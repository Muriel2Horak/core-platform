# T2: Widget Library

**Story:** [S3: Dashboard Grid Layout](README.md)  
**Effort:** 20 hours  
**Priority:** P0  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Implementovat widget types: KPI Tile, Chart, Table, Text.

---

## 🎯 ACCEPTANCE CRITERIA

1. KPI Tile widget (number + trend)
2. Chart widget (bar, line, pie)
3. Table widget (mini data grid)
4. Text widget (markdown)

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/dashboard/widgets/KPIWidget.tsx
export const KPIWidget: React.FC<{ title: string; value: number; trend: number }> = ({ title, value, trend }) => {
  return (
    <Card>
      <CardContent>
        <Typography variant="h6">{title}</Typography>
        <Typography variant="h3">{value}</Typography>
        <Typography color={trend > 0 ? 'success.main' : 'error.main'}>
          {trend > 0 ? '↑' : '↓'} {Math.abs(trend)}%
        </Typography>
      </CardContent>
    </Card>
  );
};

// frontend/src/components/dashboard/widgets/ChartWidget.tsx
export const ChartWidget: React.FC<{ data: any[]; chartType: ChartType }> = ({ data, chartType }) => {
  return (
    <Card>
      <CardContent>
        <ChartRenderer data={data} type={chartType} />
      </CardContent>
    </Card>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] 4 widget types
- [ ] Widget configuration
- [ ] Responsive sizing
- [ ] Unit tests

---

**Estimated:** 20 hours
