/**
 * 📊 DataTablePage - Example page with TanStack Table
 * 
 * Ukázka implementace pokročilé datové tabulky s funkcemi:
 * - Sorting, filtering, pagination
 * - Column resizing, reordering
 * - Row selection, expansion
 * - Virtualization pro velká data
 */
import { useState, useMemo } from 'react';
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
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Checkbox,
  Toolbar,
  Tooltip,
} from '@mui/material';
import {
  FilterList as FilterIcon,
  ViewColumn as ViewColumnIcon,
  Download as DownloadIcon,
  Add as AddIcon,
  Visibility as VisibilityIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  ArrowUpward,
  ArrowDownward,
} from '@mui/icons-material';
import {
  useReactTable,
  getCoreRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  flexRender,
  ColumnDef,
  ColumnFiltersState,
  SortingState,
} from '@tanstack/react-table';
import { WorkSection } from '../../shared/ui/WorkArea';

// 🎯 User interface
interface User {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  department: string;
  status: 'active' | 'inactive' | 'pending';
  salary: number;
  joinDate: Date;
  lastLogin: Date;
}

// 🎲 Mock data pro ukázku
const generateMockUsers = (count = 100): User[] => {
  const firstNames = ['Jan', 'Marie', 'Petr', 'Eva', 'Pavel', 'Anna', 'Tomáš', 'Kateřina', 'Jiří', 'Lenka'];
  const lastNames = ['Novák', 'Svoboda', 'Novotný', 'Dvořák', 'Černý', 'Procházka', 'Krejčí', 'Svobodová'];
  const departments = ['IT', 'HR', 'Finance', 'Marketing', 'Sales', 'Operations'];
  const statuses: ('active' | 'inactive' | 'pending')[] = ['active', 'inactive', 'pending'];

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

interface DataTablePageProps {
  user?: {
    email?: string;
    username?: string;
    firstName?: string;
    lastName?: string;
    roles?: string[];
  };
}

export default function DataTablePage({ user }: DataTablePageProps) {
  const [data] = useState(() => generateMockUsers(500));
  const [globalFilter, setGlobalFilter] = useState('');
  const [columnVisibility, setColumnVisibility] = useState({});
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [sorting, setSorting] = useState<SortingState>([]);
  const [rowSelection, setRowSelection] = useState({});
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);

  // Definice sloupců
  const columns = useMemo<ColumnDef<User>[]>(() => [
    {
      id: 'select',
      header: ({ table }) => (
        <Checkbox
          checked={table.getIsAllRowsSelected()}
          indeterminate={table.getIsSomeRowsSelected()}
          onChange={table.getToggleAllRowsSelectedHandler()}
        />
      ),
      cell: ({ row }) => (
        <Checkbox
          checked={row.getIsSelected()}
          onChange={row.getToggleSelectedHandler()}
        />
      ),
    },
    {
      accessorKey: 'firstName',
      header: 'Jméno',
      cell: info => String(info.getValue()),
    },
    {
      accessorKey: 'lastName',
      header: 'Příjmení',
      cell: info => String(info.getValue()),
    },
    {
      accessorKey: 'email',
      header: 'Email',
      cell: info => String(info.getValue()),
    },
    {
      accessorKey: 'department',
      header: 'Oddělení',
      cell: info => (
        <Chip
          label={String(info.getValue())}
          size="small"
          color="primary"
          variant="outlined"
        />
      ),
    },
    {
      accessorKey: 'status',
      header: 'Stav',
      cell: info => {
        const status = info.getValue() as string;
        const color = status === 'active' ? 'success' : status === 'inactive' ? 'error' : 'warning';
        return (
          <Chip
            label={String(status)}
            size="small"
            color={color}
          />
        );
      },
    },
    {
      accessorKey: 'salary',
      header: 'Plat',
      cell: info => `${(info.getValue() as number).toLocaleString('cs-CZ')} Kč`,
    },
    {
      id: 'actions',
      header: 'Akce',
      cell: () => (
        <Box>
          <Tooltip title="Zobrazit">
            <IconButton size="small">
              <VisibilityIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Upravit">
            <IconButton size="small">
              <EditIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Smazat">
            <IconButton size="small">
              <DeleteIcon />
            </IconButton>
          </Tooltip>
        </Box>
      ),
    },
  ], []);

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
    enableRowSelection: true,
    onGlobalFilterChange: setGlobalFilter,
    onColumnVisibilityChange: setColumnVisibility,
    onColumnFiltersChange: setColumnFilters,
    onSortingChange: setSorting,
    onRowSelectionChange: setRowSelection,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
  });

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
          výběru řádků a responsivního designu. {user ? `Přihlášen jako: ${user.email || 'Uživatel'}` : ''}
        </Alert>

        {/* Toolbar */}
        <Toolbar sx={{ px: 0, minHeight: 'auto', gap: 2, mb: 2 }}>
          <TextField
            placeholder="Hledat..."
            value={globalFilter ?? ''}
            onChange={(e) => setGlobalFilter(e.target.value)}
            size="small"
            sx={{ minWidth: 200 }}
          />

          <Button
            startIcon={<FilterIcon />}
            variant="outlined"
            onClick={(e) => setAnchorEl(e.currentTarget)}
          >
            Filtry
          </Button>

          <Button
            startIcon={<ViewColumnIcon />}
            variant="outlined"
          >
            Sloupce
          </Button>

          <Button
            startIcon={<DownloadIcon />}
            variant="outlined"
          >
            Export
          </Button>

          <Button
            startIcon={<AddIcon />}
            variant="contained"
          >
            Nový záznam
          </Button>
        </Toolbar>

        {/* Table */}
        <Paper elevation={0} sx={{ border: 1, borderColor: 'grey.200' }}>
          <TableContainer>
            <Table>
              <TableHead>
                {table.getHeaderGroups().map(headerGroup => (
                  <TableRow key={headerGroup.id}>
                    {headerGroup.headers.map(header => (
                      <TableCell
                        key={header.id}
                        sx={{
                          cursor: header.column.getCanSort() ? 'pointer' : 'default',
                          userSelect: 'none',
                        }}
                        onClick={header.column.getToggleSortingHandler()}
                      >
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          {flexRender(header.column.columnDef.header, header.getContext())}
                          {header.column.getIsSorted() && (
                            header.column.getIsSorted() === 'desc' ? <ArrowDownward fontSize="small" /> : <ArrowUpward fontSize="small" />
                          )}
                        </Box>
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableHead>
              <TableBody>
                {table.getRowModel().rows.map(row => (
                  <TableRow key={row.id} selected={row.getIsSelected()}>
                    {row.getVisibleCells().map(cell => (
                      <TableCell key={cell.id}>
                        {flexRender(cell.column.columnDef.cell, cell.getContext())}
                      </TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>

          {/* Pagination */}
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 2 }}>
            <Typography variant="body2">
              Zobrazeno {table.getState().pagination.pageIndex * table.getState().pagination.pageSize + 1} - {Math.min((table.getState().pagination.pageIndex + 1) * table.getState().pagination.pageSize, table.getFilteredRowModel().rows.length)} z {table.getFilteredRowModel().rows.length} záznamů
            </Typography>

            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button
                onClick={() => table.previousPage()}
                disabled={!table.getCanPreviousPage()}
                variant="outlined"
                size="small"
              >
                Předchozí
              </Button>
              <Button
                onClick={() => table.nextPage()}
                disabled={!table.getCanNextPage()}
                variant="outlined"
                size="small"
              >
                Další
              </Button>
            </Box>
          </Box>
        </Paper>

        {/* Menu */}
        <Menu
          anchorEl={anchorEl}
          open={Boolean(anchorEl)}
          onClose={() => setAnchorEl(null)}
        >
          <MenuItem onClick={() => setAnchorEl(null)}>
            <FilterIcon sx={{ mr: 2 }} />
            Filtr podle oddělení
          </MenuItem>
          <MenuItem onClick={() => setAnchorEl(null)}>
            <FilterIcon sx={{ mr: 2 }} />
            Filtr podle stavu
          </MenuItem>
        </Menu>
      </WorkSection>
    </Box>
  );
}