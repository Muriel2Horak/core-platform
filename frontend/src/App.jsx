import './App.css'
import { CssBaseline, ThemeProvider } from '@mui/material';
import { baselightTheme } from "./theme/DefaultColors";
import { RouterProvider } from 'react-router';
import router from "./routes/Router.js"
import AuthGuard from './components/AuthGuard/AuthGuard';

function App() {
  const theme = baselightTheme;

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <AuthGuard>
        <RouterProvider router={router} />
      </AuthGuard>
    </ThemeProvider>
  );
}

export default App