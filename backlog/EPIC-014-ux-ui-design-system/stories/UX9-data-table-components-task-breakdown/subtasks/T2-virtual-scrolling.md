# T2: Virtual Scrolling Performance

**Parent Story:** S9 - Data Table Components  
**LOC:** ~250 | **Effort:** ~4h

## Objective
Implement virtual scrolling for large datasets (10k+ rows) with <100ms render time.

## Implementation

```tsx
// frontend/src/components/table/VirtualTable.tsx
import { DataGridPro } from '@mui/x-data-grid-pro';
import { useMemo } from 'react';

export const VirtualTable = <T extends { id: string | number }>({
  columns,
  rows,
  height = 600,
}: {
  columns: GridColDef[];
  rows: T[];
  height?: number;
}) => {
  // Memoize columns to prevent re-renders
  const memoizedColumns = useMemo(() => columns, [columns]);
  
  return (
    <DataGridPro
      rows={rows}
      columns={memoizedColumns}
      pagination
      pageSizeOptions={[100, 500, 1000]}
      initialState={{
        pagination: { paginationModel: { pageSize: 100 } },
      }}
      // Virtual scrolling enabled by default in DataGridPro
      rowBuffer={10}
      columnBuffer={2}
      sx={{ height }}
      density="compact"
    />
  );
};

// Performance optimizations
const OptimizedTable = () => {
  const [rows, setRows] = useState<Data[]>([]);
  
  // Virtualized row rendering
  const renderRow = useCallback((params: GridRowParams) => {
    return <TableRow {...params} />;
  }, []);
  
  // Debounced filtering
  const debouncedFilter = useMemo(
    () => debounce((filter: string) => {
      // Apply filter
    }, 300),
    []
  );
  
  return (
    <VirtualTable
      rows={rows}
      columns={columns}
      height={600}
    />
  );
};
```

## Acceptance Criteria
- [ ] 10k+ rows render <100ms
- [ ] Smooth scrolling (60fps)
- [ ] Row buffer for smooth experience
- [ ] Memoized columns/rows
- [ ] Debounced filtering (300ms)
- [ ] Memory efficient (only render visible)
- [ ] No layout shift

## Files
- `frontend/src/components/table/VirtualTable.tsx`
- `frontend/src/hooks/useVirtualScroll.ts`
- `package.json` (add @mui/x-data-grid-pro)
