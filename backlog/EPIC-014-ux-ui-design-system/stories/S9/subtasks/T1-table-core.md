# T1: Table Core Features

**Parent Story:** S9 - Data Table Components  
**LOC:** ~300 | **Effort:** ~4h

## Objective
Implement core table features: sorting, filtering, pagination, row selection.

## Implementation

```tsx
// frontend/src/components/table/DataTable.tsx
import { DataGrid, GridColDef, GridRowSelectionModel } from '@mui/x-data-grid';

interface DataTableProps<T> {
  columns: GridColDef[];
  rows: T[];
  loading?: boolean;
  onRowSelectionChange?: (selection: GridRowSelectionModel) => void;
  pageSize?: number;
}

export const DataTable = <T extends { id: string | number }>({
  columns,
  rows,
  loading,
  onRowSelectionChange,
  pageSize = 10,
}: DataTableProps<T>) => {
  return (
    <DataGrid
      rows={rows}
      columns={columns}
      loading={loading}
      checkboxSelection
      disableRowSelectionOnClick
      onRowSelectionModelChange={onRowSelectionChange}
      pageSizeOptions={[10, 25, 50, 100]}
      initialState={{
        pagination: { paginationModel: { pageSize } },
      }}
      sx={{
        border: 'none',
        '& .MuiDataGrid-cell:focus': {
          outline: 'none',
        },
      }}
    />
  );
};

// Column definitions example
const columns: GridColDef[] = [
  { 
    field: 'name', 
    headerName: 'Name', 
    flex: 1,
    sortable: true,
    filterable: true,
  },
  { 
    field: 'email', 
    headerName: 'Email', 
    flex: 1,
  },
  { 
    field: 'status', 
    headerName: 'Status',
    width: 120,
    renderCell: (params) => (
      <Chip 
        label={params.value} 
        color={params.value === 'active' ? 'success' : 'default'}
        size="small"
      />
    ),
  },
  {
    field: 'actions',
    headerName: 'Actions',
    width: 100,
    sortable: false,
    renderCell: (params) => (
      <IconButton onClick={() => handleEdit(params.row)}>
        <Edit />
      </IconButton>
    ),
  },
];
```

## Acceptance Criteria
- [ ] Sortable columns (click header)
- [ ] Filterable columns (menu)
- [ ] Pagination (10/25/50/100 per page)
- [ ] Row selection (checkboxes)
- [ ] Custom cell renderers
- [ ] Loading state
- [ ] No data state

## Files
- `frontend/src/components/table/DataTable.tsx`
- `frontend/src/components/table/TableFilters.tsx`
- `package.json` (add @mui/x-data-grid)
