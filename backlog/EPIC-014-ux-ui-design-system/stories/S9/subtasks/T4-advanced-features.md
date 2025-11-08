# T4: Advanced Table Features

**Parent Story:** S9 - Data Table Components  
**LOC:** ~150 | **Effort:** ~2h

## Objective
Add advanced features: column visibility, export, bulk actions.

## Implementation

```tsx
// frontend/src/components/table/AdvancedTable.tsx
import { GridToolbar } from '@mui/x-data-grid';

export const AdvancedTable = () => {
  const [columnVisibility, setColumnVisibility] = useState({});
  const [selectedRows, setSelectedRows] = useState<GridRowSelectionModel>([]);
  
  const handleExport = () => {
    const csv = rows.map(row => 
      columns.map(col => row[col.field]).join(',')
    ).join('\n');
    
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'export.csv';
    a.click();
  };
  
  const handleBulkDelete = async () => {
    await api.deleteMany(selectedRows);
    setSelectedRows([]);
  };
  
  return (
    <>
      <Box display="flex" gap={2} mb={2}>
        <Button 
          startIcon={<Download />} 
          onClick={handleExport}
        >
          Export CSV
        </Button>
        {selectedRows.length > 0 && (
          <Button 
            color="error"
            startIcon={<Delete />}
            onClick={handleBulkDelete}
          >
            Delete ({selectedRows.length})
          </Button>
        )}
      </Box>
      
      <DataGrid
        rows={rows}
        columns={columns}
        checkboxSelection
        onRowSelectionModelChange={setSelectedRows}
        columnVisibilityModel={columnVisibility}
        onColumnVisibilityModelChange={setColumnVisibility}
        slots={{
          toolbar: GridToolbar,
        }}
        slotProps={{
          toolbar: {
            showQuickFilter: true,
            csvOptions: { allColumns: true },
          },
        }}
      />
    </>
  );
};
```

## Acceptance Criteria
- [ ] Column visibility toggle
- [ ] Export CSV/JSON
- [ ] Bulk actions (delete, update)
- [ ] Quick filter (toolbar)
- [ ] Column resizing
- [ ] Column reordering (drag-drop)

## Files
- `frontend/src/components/table/AdvancedTable.tsx`
- `frontend/src/utils/tableExport.ts`
