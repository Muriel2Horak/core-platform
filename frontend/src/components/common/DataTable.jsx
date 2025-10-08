import React, { useState } from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TablePagination,
  TableSortLabel,
  Paper,
  IconButton,
  Tooltip,
  Chip,
  Box,
  Typography,
  alpha
} from '@mui/material';
import {
  Visibility as ViewIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon
} from '@mui/icons-material';

/**
 * 📊 Univerzální DataTable komponent s podporou metamodelu
 * 
 * @param {Array} columns - Definice sloupců podle metamodelu
 * @param {Array} data - Data k zobrazení
 * @param {Function} onRowClick - Callback při kliknutí na řádek
 * @param {Function} onDelete - Callback pro smazání řádku
 * @param {Function} onRefresh - Callback pro refresh dat
 * @param {Boolean} loading - Indikátor načítání
 * @param {Number} totalCount - Celkový počet záznamů (pro server-side pagination)
 * @param {Function} onPageChange - Callback při změně stránky
 * @param {Function} onSortChange - Callback při změně třídění
 */
export const DataTable = ({
  columns = [],
  data = [],
  onRowClick,
  onDelete,
  onRefresh,
  loading = false,
  totalCount,
  onPageChange,
  onSortChange,
  defaultRowsPerPage = 10
}) => {
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(defaultRowsPerPage);
  const [orderBy, setOrderBy] = useState('');
  const [order, setOrder] = useState('asc');

  const handleChangePage = (event, newPage) => {
    setPage(newPage);
    if (onPageChange) {
      onPageChange(newPage, rowsPerPage);
    }
  };

  const handleChangeRowsPerPage = (event) => {
    const newRowsPerPage = parseInt(event.target.value, 10);
    setRowsPerPage(newRowsPerPage);
    setPage(0);
    if (onPageChange) {
      onPageChange(0, newRowsPerPage);
    }
  };

  const handleSort = (columnId) => {
    const isAsc = orderBy === columnId && order === 'asc';
    const newOrder = isAsc ? 'desc' : 'asc';
    setOrder(newOrder);
    setOrderBy(columnId);
    
    if (onSortChange) {
      onSortChange(columnId, newOrder);
    }
  };

  /**
   * 🎨 Renderuje buňku podle typu definovaného v metamodelu
   */
  const renderCell = (row, column) => {
    const value = row[column.field];

    switch (column.type) {
      case 'status':
        return (
          <Chip
            label={value}
            size="small"
            sx={{
              bgcolor: column.statusColors?.[value] || 'grey.300',
              color: 'white',
              fontWeight: 600,
              textTransform: 'uppercase',
              fontSize: '0.7rem'
            }}
          />
        );

      case 'datetime':
        return value ? new Date(value).toLocaleString('cs-CZ') : '-';

      case 'progress':
        const percentage = row[column.totalField] > 0 
          ? Math.round((value / row[column.totalField]) * 100) 
          : 0;
        return `${value} / ${row[column.totalField]} (${percentage}%)`;

      case 'custom':
        return column.render ? column.render(value, row) : value;

      default:
        return value || '-';
    }
  };

  const displayData = totalCount !== undefined 
    ? data // Server-side pagination
    : data.slice(page * rowsPerPage, page * rowsPerPage + rowsPerPage); // Client-side

  return (
    <Paper
      elevation={0}
      sx={{
        background: theme => `linear-gradient(135deg, ${alpha(theme.palette.primary.main, 0.02)} 0%, ${alpha(theme.palette.secondary.main, 0.02)} 100%)`,
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 2,
        overflow: 'hidden'
      }}
    >
      {/* Header s refresh tlačítkem */}
      {onRefresh && (
        <Box sx={{ p: 2, display: 'flex', justifyContent: 'flex-end', borderBottom: '1px solid', borderColor: 'divider' }}>
          <Tooltip title="Obnovit">
            <IconButton onClick={onRefresh} size="small" disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
        </Box>
      )}

      <TableContainer>
        <Table>
          <TableHead>
            <TableRow>
              {columns.map((column) => (
                <TableCell
                  key={column.field}
                  sortDirection={orderBy === column.field ? order : false}
                  align={column.align || 'left'}
                  sx={{
                    fontWeight: 700,
                    bgcolor: 'background.paper',
                    borderBottom: '2px solid',
                    borderColor: 'primary.main'
                  }}
                >
                  {column.sortable !== false ? (
                    <TableSortLabel
                      active={orderBy === column.field}
                      direction={orderBy === column.field ? order : 'asc'}
                      onClick={() => handleSort(column.field)}
                    >
                      {column.label}
                    </TableSortLabel>
                  ) : (
                    column.label
                  )}
                </TableCell>
              ))}
              {(onRowClick || onDelete) && (
                <TableCell sx={{ fontWeight: 700, bgcolor: 'background.paper', borderBottom: '2px solid', borderColor: 'primary.main' }}>
                  Akce
                </TableCell>
              )}
            </TableRow>
          </TableHead>

          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">Načítání...</Typography>
                </TableCell>
              </TableRow>
            ) : displayData.length === 0 ? (
              <TableRow>
                <TableCell colSpan={columns.length + 1} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">Žádná data k zobrazení</Typography>
                </TableCell>
              </TableRow>
            ) : (
              displayData.map((row) => (
                <TableRow
                  key={row.id}
                  hover
                  onClick={() => onRowClick && onRowClick(row)}
                  sx={{
                    cursor: onRowClick ? 'pointer' : 'default',
                    '&:hover': {
                      bgcolor: onRowClick ? alpha('#1976d2', 0.04) : 'inherit'
                    }
                  }}
                >
                  {columns.map((column) => (
                    <TableCell key={column.field} align={column.align || 'left'}>
                      {renderCell(row, column)}
                    </TableCell>
                  ))}
                  
                  {(onRowClick || onDelete) && (
                    <TableCell>
                      <Box sx={{ display: 'flex', gap: 1 }}>
                        {onRowClick && (
                          <Tooltip title="Detail">
                            <IconButton size="small" onClick={() => onRowClick(row)}>
                              <ViewIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                        {onDelete && (
                          <Tooltip title="Smazat">
                            <IconButton size="small" color="error" onClick={() => onDelete(row.id)}>
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        )}
                      </Box>
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <TablePagination
        rowsPerPageOptions={[5, 10, 20, 50]}
        component="div"
        count={totalCount !== undefined ? totalCount : data.length}
        rowsPerPage={rowsPerPage}
        page={page}
        onPageChange={handleChangePage}
        onRowsPerPageChange={handleChangeRowsPerPage}
        labelRowsPerPage="Řádků na stránku:"
        labelDisplayedRows={({ from, to, count }) => `${from}–${to} z ${count}`}
      />
    </Paper>
  );
};
