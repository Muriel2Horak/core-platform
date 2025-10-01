import React from 'react';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { RouterProvider } from 'react-router';
import router from '../routes/Router.js';

// 🚀 LAZY MUI PROVIDER - odděluje těžké MUI komponenty od základního načítání
const LazyMuiProvider = () => {
  return (
    <LocalizationProvider dateAdapter={AdapterDayjs}>
      <RouterProvider router={router} />
    </LocalizationProvider>
  );
};

export default LazyMuiProvider;