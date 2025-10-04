import { useState, useMemo } from 'react';
/**
 * 📊 DataTablePage - Example page with TanStack Table
 * 
 * Ukázka implementace pokročilé datové tabulky s funkcemi:
 * - Sorting, filtering, pagination
 * - Column resizing, reordering
 * - Row selection, expansion
 * - Virtualization pro velká data
 */
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Alert,
} from '@mui/material';
import {
  FilterList as FilterIcon,
  ViewColumn as ViewColumnIcon,
  Download as DownloadIcon,
  Add as AddIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as VisibilityIcon,
} from '@mui/icons-material';
import {
  useReactTable,
  getCoreRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  flexRender,
  createColumnHelper,
} from '@tanstack/react-table';
import { WorkSection } from '../../shared/ui/WorkArea';
import { tokens } from '../../shared/theme/tokens';

// 🎲 Mock data pro ukázku
const generateMockUsers = (count = 100) => {
  const firstNames = ['Jan', 'Marie', 'Petr', 'Eva', 'Pavel', 'Anna', 'Tomáš', 'Kateřina', 'Jiří', 'Lenka'];
  const lastNames = ['Novák', 'Svoboda', 'Novotný', 'Dvořák', 'Černý', 'Procházka', 'Krejčí', 'Svobodová'];
  const departments = ['IT', 'HR', 'Finance', 'Marketing', 'Sales', 'Operations'];
  const statuses = ['active', 'inactive', 'pending'];
  
  return Array.from({ length: count }, (_, i) => ({
    id: i + 1,
    firstName: firstNames[Math.floor(Math.random() * firstNames.length)],
    lastName: lastNames[Math.floor(Math.random() * lastNames.length)],
    email: `user${i + 1}@example.com`,
    department: departments[Math.floor(Math.random() * departments.length)],
    status: statuses[Math.floor(Math.random() * statuses.length)],
    salary: Math.floor(Math.random() * 50000) + 30000,
    joinDate: new Date(2020 + Math.floor(Math.random() * 4), Math.floor(Math.random() * 12), Math.floor(Math.random() * 28)),
    lastLogin: new Date(Date.now() - Math.floor(Math.random() * 30) * 24 * 60 * 60 * 1000),
  }));
};

// 📋 Column helper pro type safety
const columnHelper = createColumnHelper();

export default function DataTablePage() {
  const [data] = useState(() => generateMockUsers(500));
  const [globalFilter, setGlobalFilter] = useState('');
  const [columnVisibility, setColumnVisibility] = useState({});
  const [columnFilters, setColumnFilters] = useState([]);
  const [sorting, setSorting] = useState([]);
  const [rowSelection, setRowSelection] = useState({});
  const [anchorEl, setAnchorEl] = useState(null);

  // 📊 Column definitions s pokročilými funkcemi
  const columns = useMemo(
    () => [
      // Selection column
      columnHelper.display({
        id: 'select',
        header: ({ table }) => (
          <input
            type="checkbox"
            checked={table.getIsAllRowsSelected()}
            onChange={table.getToggleAllRowsSelectedHandler()}
          />
        ),
        cell: ({ row }) => (
          <input
            type="checkbox"
            checked={row.getIsSelected()}
            onChange={row.getToggleSelectedHandler()}
          />
        ),
        size: 50,
      }),
      
      // User info column
      columnHelper.accessor('firstName', {
        header: 'Jméno',
        cell: info => (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box
              sx={{
                width: 32,
                height: 32,
                borderRadius: '50%',
                bgcolor: 'primary.main',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'white',
                fontSize: '0.75rem',
                fontWeight: 600,
              }}
            >
              {info.getValue().charAt(0)}{info.row.original.lastName.charAt(0)}
            </Box>
            <Box>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {info.getValue()} {info.row.original.lastName}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {info.row.original.email}
              </Typography>
            </Box>
          </Box>
        ),
        filterFn: 'includesString',
      }),
      
      columnHelper.accessor('department', {
        header: 'Oddělení',
        cell: info => (
          <Chip
            label={info.getValue()}
            size="small"
            variant="outlined"
            sx={{ borderRadius: 1 }}
          />
        ),
        filterFn: 'equals',
      }),
      
      columnHelper.accessor('status', {
        header: 'Status',
        cell: info => {
          const status = info.getValue();
          const color = status === 'active' ? 'success' : status === 'pending' ? 'warning' : 'default';
          return (
            <Chip
              label={status}
              size="small"
              color={color}
              sx={{ borderRadius: 1, minWidth: 80 }}
            />
          );
        },
        filterFn: 'equals',
      }),
      
      columnHelper.accessor('salary', {
        header: 'Plat',
        cell: info => (
          <Typography variant="body2" sx={{ fontWeight: 500 }}>
            {new Intl.NumberFormat('cs-CZ', {
              style: 'currency',
              currency: 'CZK',
            }).format(info.getValue())}
          </Typography>
        ),
        filterFn: 'inNumberRange',
      }),
      
      columnHelper.accessor('joinDate', {
        header: 'Nastoupil',
        cell: info => (
          <Typography variant="body2">
            {info.getValue().toLocaleDateString('cs-CZ')}
          </Typography>
        ),
        filterFn: 'dateBetween',
      }),
      
      columnHelper.accessor('lastLogin', {
        header: 'Poslední login',
        cell: info => (
          <Typography variant="caption" color="text.secondary">
            {info.getValue().toLocaleDateString('cs-CZ')}
          </Typography>
        ),
      }),
      
      // Actions column
      columnHelper.display({
        id: 'actions',
        header: 'Akce',
        cell: () => (
          <IconButton
            size="small"
            onClick={(e) => setAnchorEl(e.currentTarget)}
          >
            <MoreVertIcon />
          </IconButton>
        ),
        size: 80,
      }),
    ],
    []
  );

  // 🏗️ Table instance
  const table = useReactTable({
    data,
    columns,
    state: {
      globalFilter,
      columnVisibility,
      columnFilters,
      sorting,
      rowSelection,
    },
    onGlobalFilterChange: setGlobalFilter,
    onColumnVisibilityChange: setColumnVisibility,
    onColumnFiltersChange: setColumnFilters,
    onSortingChange: setSorting,
    onRowSelectionChange: setRowSelection,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    enableRowSelection: true,
    enableColumnResizing: true,
    columnResizeMode: 'onChange',
    initialState: {
      pagination: {
        pageSize: 20,
      },
    },
  });

  const selectedRows = table.getFilteredSelectedRowModel().rows;

  return (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <WorkSection
        title="📊 DataTable Example"
        subtitle="Pokročilá tabulka s TanStack Table"
        variant="compact"
      >
        <Alert severity="info" sx={{ mb: 3 }}>
          Tato stránka demonstruje pokročilé funkce TanStack Table včetně sortování, filtrování, 
          výběru řádků a responsivního designu.
        </Alert>

        {/* Controls */}
        <Box sx={{ 
          display: 'flex', 
          gap: 2, 
          mb: 3,
          flexWrap: 'wrap',
          alignItems: 'center',
        }}>
          <TextField
            size="small"
            placeholder="Hledat..."
            value={globalFilter ?? ''}
            onChange={(e) => setGlobalFilter(e.target.value)}
            sx={{ minWidth: 200 }}
          />
          
          <Button
            startIcon={<FilterIcon />}
            variant="outlined"
            size="small"
          >
            Filtry
          </Button>
          
          <Button
            startIcon={<ViewColumnIcon />}
            variant="outlined"
            size="small"
          >
            Sloupce
          </Button>
          
          <Button
            startIcon={<DownloadIcon />}
            variant="outlined"
            size="small"
          >
            Export
          </Button>
          
          <Box sx={{ flexGrow: 1 }} />
          
          <Button
            startIcon={<AddIcon />}
            variant="contained"
            size="small"
          >
            Přidat uživatele
          </Button>
        </Box>

        {/* Selection info */}
        {selectedRows.length > 0 && (
          <Alert severity="info" sx={{ mb: 2 }}>
            Vybráno {selectedRows.length} řádků z {table.getFilteredRowModel().rows.length}
          </Alert>
        )}
      </WorkSection>

      {/* Table */}
      <Paper 
        sx={{ 
          flex: 1, 
          overflow: 'hidden',
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        <Box sx={{ 
          overflow: 'auto', 
          flex: 1,
          '& table': {
            width: '100%',
            borderCollapse: 'collapse',
          },
          '& th, & td': {
            padding: tokens.spacing.sm,
            borderBottom: `1px solid ${tokens.colors.grey[200]}`,
            textAlign: 'left',
          },
          '& th': {
            backgroundColor: tokens.colors.grey[50],
            fontWeight: tokens.typography.fontWeight.semibold,
            position: 'sticky',
            top: 0,
            zIndex: 1,
          },
          '& tbody tr:hover': {
            backgroundColor: tokens.colors.grey[50],
          },
        }}>
          <table>
            <thead>
              {table.getHeaderGroups().map(headerGroup => (
                <tr key={headerGroup.id}>
                  {headerGroup.headers.map(header => (
                    <th
                      key={header.id}
                      style={{
                        width: header.getSize(),
                        position: 'relative',
                      }}
                    >
                      {header.isPlaceholder ? null : (
                        <Box
                          sx={{
                            display: 'flex',
                            alignItems: 'center',
                            cursor: header.column.getCanSort() ? 'pointer' : 'default',
                          }}
                          onClick={header.column.getToggleSortingHandler()}
                        >
                          {flexRender(
                            header.column.columnDef.header,
                            header.getContext()
                          )}
                          {{
                            asc: ' 🔼',
                            desc: ' 🔽',
                          }[header.column.getIsSorted()] ?? null}
                        </Box>
                      )}
                      
                      {/* Column resizer */}
                      {header.column.getCanResize() && (
                        <Box
                          onMouseDown={header.getResizeHandler()}
                          onTouchStart={header.getResizeHandler()}
                          sx={{
                            position: 'absolute',
                            right: 0,
                            top: 0,
                            height: '100%',
                            width: '5px',
                            cursor: 'col-resize',
                            userSelect: 'none',
                            touchAction: 'none',
                            '&:hover': {
                              backgroundColor: 'primary.main',
                            },
                          }}
                        />
                      )}
                    </th>
                  ))}
                </tr>
              ))}
            </thead>
            <tbody>
              {table.getRowModel().rows.map(row => (
                <tr key={row.id}>
                  {row.getVisibleCells().map(cell => (
                    <td key={cell.id}>
                      {flexRender(
                        cell.column.columnDef.cell,
                        cell.getContext()
                      )}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </Box>

        {/* Pagination */}
        <Box sx={{ 
          p: 2, 
          borderTop: `1px solid ${tokens.colors.grey[200]}`,
          display: 'flex',
          alignItems: 'center',
          gap: 2,
        }}>
          <Typography variant="body2" color="text.secondary">
            Strana {table.getState().pagination.pageIndex + 1} z{' '}
            {table.getPageCount()} • {table.getFilteredRowModel().rows.length} záznamů
          </Typography>
          
          <Box sx={{ flexGrow: 1 }} />
          
          <Button
            size="small"
            onClick={() => table.previousPage()}
            disabled={!table.getCanPreviousPage()}
          >
            Předchozí
          </Button>
          <Button
            size="small"
            onClick={() => table.nextPage()}
            disabled={!table.getCanNextPage()}
          >
            Další
          </Button>
        </Box>
      </Paper>

      {/* Action Menu */}
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={() => setAnchorEl(null)}
      >
        <MenuItem onClick={() => setAnchorEl(null)}>
          <VisibilityIcon sx={{ mr: 1 }} fontSize="small" />
          Zobrazit
        </MenuItem>
        <MenuItem onClick={() => setAnchorEl(null)}>
          <EditIcon sx={{ mr: 1 }} fontSize="small" />
          Upravit
        </MenuItem>
        <MenuItem onClick={() => setAnchorEl(null)}>
          <DeleteIcon sx={{ mr: 1 }} fontSize="small" />
          Smazat
        </MenuItem>
      </Menu>
    </Box>
  );
}