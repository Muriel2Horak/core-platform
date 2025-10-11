import { useState, useEffect, useMemo, useCallback } from 'react';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-material.css';
import {
  Box,
  Toolbar,
  Typography,
  Button,
  IconButton,
  Tooltip,
  CircularProgress,
  Alert,
  Snackbar
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  GetApp as ExportIcon,
  CheckBox as CheckBoxIcon,
  CheckBoxOutlineBlank as CheckBoxOutlineBlankIcon
} from '@mui/icons-material';
import axios from 'axios';

/**
 * ExplorerGrid - Advanced data grid with server-side operations
 * 
 * Features:
 * - Server-side pagination, sorting, filtering
 * - Inline cell editing with optimistic locking (If-Match header)
 * - Bulk selection and batch operations
 * - CSV export
 * - Auto-fetch entity metadata spec
 * 
 * @param {Object} props
 * @param {string} props.entity - Entity name (e.g., 'users_directory')
 * @param {Object} props.initialFilters - Initial filters
 * @param {Function} props.onRowClick - Row click handler
 * @param {Function} props.onDrillDown - Drill-down handler
 */
export function ExplorerGrid({ entity, initialFilters = {}, onRowClick, onDrillDown }) {
  const [data, setData] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [spec, setSpec] = useState(null);
  const [selectedRows, setSelectedRows] = useState([]);
  const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'info' });
  const [filters, setFilters] = useState(initialFilters);

  // Pagination state
  const [paginationModel, setPaginationModel] = useState({
    page: 0,
    pageSize: 25
  });
  const [rowCount, setRowCount] = useState(0);

  // Sort state
  const [sortModel, setSortModel] = useState([]);

  // Fetch entity spec on mount
  useEffect(() => {
    async function fetchSpec() {
      try {
        const response = await axios.get(`/api/reports/metadata/${entity}/spec`);
        setEntitySpec(response.data);
        console.log(`Loaded spec for ${entity}:`, response.data);
      } catch (err) {
        console.error('Failed to load entity spec:', err);
        setError('Failed to load entity specification');
      }
    }
    fetchSpec();
  }, [entity]);

  // Fetch data
  const fetchData = useCallback(async () => {
    if (!entitySpec) return;

    setLoading(true);
    setError(null);

    try {
      const dimensions = entitySpec.defaultView?.columns || 
                        Array.from(entitySpec.allowedDimensions).slice(0, 5);
      
      const queryRequest = {
        entity,
        dimensions,
        measures: [],
        filters: filters,
        limit: paginationModel.pageSize,
        offset: paginationModel.page * paginationModel.pageSize,
        sort: sortModel.length > 0 ? {
          field: sortModel[0].colId,
          order: sortModel[0].sort
        } : undefined
      };

      const response = await axios.post('/api/reports/query', queryRequest);
      
      setRowData(response.data.data || []);
      setRowCount(response.data.metadata?.totalCount || response.data.data?.length || 0);
      
      console.log('Cache status:', response.headers['x-cache']);
    } catch (err) {
      console.error('Failed to fetch data:', err);
      setError(err.response?.data?.message || 'Failed to load data');
      showSnackbar('Failed to load data', 'error');
    } finally {
      setLoading(false);
    }
  }, [entity, entitySpec, filters, paginationModel, sortModel]);

  // Fetch on dependencies change
  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // Column definitions from entity spec
  const columnDefs = useMemo(() => {
    if (!entitySpec) return [];

    return entitySpec.fields
      .filter(field => !field.sensitive) // Exclude sensitive fields
      .filter(field => entitySpec.defaultView?.columns?.includes(field.name) || 
                      entitySpec.allowedDimensions.has(field.name))
      .slice(0, 10) // Limit to 10 columns
      .map(field => ({
        field: field.name,
        headerName: field.label || field.name,
        sortable: field.sortable,
        filter: field.filterable,
        editable: field.editable && entitySpec.editableFields?.has(field.name),
        cellDataType: getCellDataType(field.type),
        valueFormatter: getValueFormatter(field.type),
        valueSetter: field.editable ? createValueSetter(field) : undefined,
        cellStyle: field.editable ? { backgroundColor: '#f0f8ff' } : undefined
      }));
  }, [entitySpec]);

  // Cell edit handler with optimistic locking
  const handleCellValueChanged = async (params) => {
    const { data, colDef, newValue, oldValue, node } = params;
    
    if (newValue === oldValue) return;

    const fieldName = colDef.field;
    const rowId = data.id;
    const version = data.version || 0;

    try {
      // Optimistic update
      const updatedRow = { ...data, [fieldName]: newValue };
      node.setData(updatedRow);

      // API call with If-Match header for optimistic locking
      await axios.patch(
        `/api/entities/${entity}/${rowId}`,
        { [fieldName]: newValue },
        {
          headers: {
            'If-Match': version.toString()
          }
        }
      );

      // Update version
      node.setDataValue('version', version + 1);
      
      showSnackbar('Updated successfully', 'success');
    } catch (err) {
      // Revert on error
      node.setDataValue(fieldName, oldValue);
      
      if (err.response?.status === 409) {
        showSnackbar('Conflict: Record was updated by another user. Please refresh.', 'error');
      } else {
        showSnackbar('Update failed: ' + (err.response?.data?.message || err.message), 'error');
      }
      
      console.error('Cell edit failed:', err);
    }
  };

  // Bulk action handler
  const handleBulkAction = async (action) => {
    if (selectedRows.length === 0) {
      showSnackbar('No rows selected', 'warning');
      return;
    }

    const confirmed = window.confirm(
      `Apply ${action} to ${selectedRows.length} selected row(s)?`
    );
    if (!confirmed) return;

    try {
      const response = await axios.post(`/api/entities/${entity}/bulk-update`, {
        where: {
          id: { in: selectedRows.map(row => row.id) }
        },
        patch: { status: action },
        idempotencyKey: `bulk-${Date.now()}-${Math.random()}`
      });

      showSnackbar(`Bulk job ${response.data.jobId} started`, 'success');
      
      // Refresh data after a delay
      setTimeout(fetchData, 2000);
    } catch (err) {
      showSnackbar('Bulk update failed: ' + (err.response?.data?.message || err.message), 'error');
    }
  };

  // Export to CSV
  const handleExport = () => {
    const csv = [
      // Header
      columnDefs.map(col => col.headerName).join(','),
      // Rows
      ...rowData.map(row => 
        columnDefs.map(col => {
          const value = row[col.field];
          // Escape commas and quotes
          return typeof value === 'string' && (value.includes(',') || value.includes('"'))
            ? `"${value.replace(/"/g, '""')}"`
            : value ?? '';
        }).join(',')
      )
    ].join('\n');

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${entity}_export_${new Date().toISOString()}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  };

  // Selection change handler
  const onSelectionChanged = useCallback((event) => {
    const selected = event.api.getSelectedRows();
    setSelectedRows(selected);
  }, []);

  // Helpers
  const showSnackbar = (message, severity = 'info') => {
    setSnackbar({ open: true, message, severity });
  };

  const handleCloseSnackbar = () => {
    setSnackbar({ ...snackbar, open: false });
  };

  if (!entitySpec) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height={400}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Toolbar */}
      <Toolbar sx={{ borderBottom: 1, borderColor: 'divider', gap: 1 }}>
        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          {entity} Explorer
          {selectedRows.length > 0 && ` (${selectedRows.length} selected)`}
        </Typography>

        {selectedRows.length > 0 && (
          <>
            <Button
              size="small"
              variant="outlined"
              onClick={() => handleBulkAction('ACTIVE')}
            >
              Activate
            </Button>
            <Button
              size="small"
              variant="outlined"
              onClick={() => handleBulkAction('INACTIVE')}
            >
              Deactivate
            </Button>
          </>
        )}

        <Tooltip title="Refresh">
          <IconButton onClick={fetchData} disabled={loading}>
            <RefreshIcon />
          </IconButton>
        </Tooltip>

        <Tooltip title="Export CSV">
          <IconButton onClick={handleExport} disabled={rowData.length === 0}>
            <ExportIcon />
          </IconButton>
        </Tooltip>
      </Toolbar>

      {/* Error Alert */}
      {error && (
        <Alert severity="error" onClose={() => setError(null)} sx={{ m: 1 }}>
          {error}
        </Alert>
      )}

      {/* AG Grid */}
      <Box sx={{ flexGrow: 1, width: '100%' }} className="ag-theme-material">
        <AgGridReact
          rowData={rowData}
          columnDefs={columnDefs}
          loading={loading}
          rowSelection="multiple"
          onSelectionChanged={onSelectionChanged}
          onCellValueChanged={handleCellValueChanged}
          onRowClicked={(event) => onRowClick?.(event.data)}
          pagination={true}
          paginationPageSize={paginationModel.pageSize}
          paginationPageSizeSelector={[10, 25, 50, 100]}
          onPaginationChanged={(event) => {
            const newPage = event.api.paginationGetCurrentPage();
            const newPageSize = event.api.paginationGetPageSize();
            if (newPage !== paginationModel.page || newPageSize !== paginationModel.pageSize) {
              setPaginationModel({ page: newPage, pageSize: newPageSize });
            }
          }}
          onSortChanged={(event) => {
            const sortState = event.api.getColumnState()
              .filter(col => col.sort != null)
              .map(col => ({ colId: col.colId, sort: col.sort }));
            setSortModel(sortState);
          }}
          domLayout="normal"
          suppressRowClickSelection={true}
          enableCellTextSelection={true}
          defaultColDef={{
            resizable: true,
            sortable: true,
            filter: true,
            flex: 1,
            minWidth: 100
          }}
        />
      </Box>

      {/* Snackbar for notifications */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={6000}
        onClose={handleCloseSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      >
        <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}

// Helper functions
function getCellDataType(fieldType) {
  switch (fieldType) {
    case 'long':
    case 'integer':
    case 'double':
    case 'decimal':
      return 'number';
    case 'boolean':
      return 'boolean';
    case 'timestamp':
    case 'date':
      return 'date';
    default:
      return 'text';
  }
}

function getValueFormatter(fieldType) {
  if (fieldType === 'timestamp' || fieldType === 'date') {
    return (params) => {
      if (!params.value) return '';
      const date = new Date(params.value);
      return date.toLocaleString();
    };
  }
  return undefined;
}

function createValueSetter(field) {
  return (params) => {
    params.data[field.name] = params.newValue;
    return true;
  };
}
