// Utility pro lazy loading MUI komponent
import { lazy } from 'react';

// ⚠️ POZOR: Načítáme jen ty komponenty, které NEJSOU kritické pro první načtení
export const LazyMuiComponents = {
  // Date pickers - používané jen v některých formách
  DatePicker: lazy(() => import('@mui/x-date-pickers/DatePicker').then(module => ({ default: module.DatePicker }))),
  LocalizationProvider: lazy(() => import('@mui/x-date-pickers/LocalizationProvider').then(module => ({ default: module.LocalizationProvider }))),
  AdapterDayjs: lazy(() => import('@mui/x-date-pickers/AdapterDayjs').then(module => ({ default: module.AdapterDayjs }))),
  
  // Komplexní komponenty používané jen na specifických stránkách
  Autocomplete: lazy(() => import('@mui/material/Autocomplete').then(module => ({ default: module.default }))),
  Rating: lazy(() => import('@mui/material/Rating').then(module => ({ default: module.default }))),
  Slider: lazy(() => import('@mui/material/Slider').then(module => ({ default: module.default }))),
  Switch: lazy(() => import('@mui/material/Switch').then(module => ({ default: module.default }))),
  
  // Fab buttons - používané jen občas
  Fab: lazy(() => import('@mui/material/Fab').then(module => ({ default: module.default }))),
  
  // Table komponenty - jen pro data stránky
  Table: lazy(() => import('@mui/material/Table').then(module => ({ default: module.default }))),
  TableBody: lazy(() => import('@mui/material/TableBody').then(module => ({ default: module.default }))),
  TableCell: lazy(() => import('@mui/material/TableCell').then(module => ({ default: module.default }))),
  TableContainer: lazy(() => import('@mui/material/TableContainer').then(module => ({ default: module.default }))),
  TableHead: lazy(() => import('@mui/material/TableHead').then(module => ({ default: module.default }))),
  TableRow: lazy(() => import('@mui/material/TableRow').then(module => ({ default: module.default }))),
  
  // Pagination - jen pro stránkování
  Pagination: lazy(() => import('@mui/material/Pagination').then(module => ({ default: module.default }))),
};

export default LazyMuiComponents;