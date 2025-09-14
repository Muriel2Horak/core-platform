import React from 'react';
import {
  ThemeProvider,
  createTheme,
  CssBaseline,
  AppBar,
  Toolbar,
  Typography,
  Button,
  Container,
  Box,
  Paper,
} from '@mui/material';
import { BrowserRouter as Router, Routes, Route, Link, useLocation } from 'react-router-dom';
import { SWRConfig } from 'swr';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import HomeIcon from '@mui/icons-material/Home';

// Import pages
import HomePage from './pages/HomePage';
import AccountPage from './pages/AccountPage';

// Create modern Material Design 3 theme
const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#1976d2',
      light: '#42a5f5',
      dark: '#1565c0',
    },
    secondary: {
      main: '#9c27b0',
    },
    background: {
      default: '#fafafa',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    h4: {
      fontWeight: 600,
    },
    h6: {
      fontWeight: 500,
    },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          textTransform: 'none',
          fontWeight: 500,
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          borderRadius: 12,
        },
      },
    },
  },
});

function NavigationBar() {
  const location = useLocation();
  
  return (
    <AppBar position="static" elevation={1}>
      <Toolbar>
        <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
          Core Platform
        </Typography>
        <Button
          color="inherit"
          component={Link}
          to="/"
          startIcon={<HomeIcon />}
          sx={{ 
            mr: 1,
            backgroundColor: location.pathname === '/' ? 'rgba(255,255,255,0.1)' : 'transparent'
          }}
        >
          Domů
        </Button>
        <Button
          color="inherit"
          component={Link}
          to="/account"
          startIcon={<AccountCircleIcon />}
          sx={{
            backgroundColor: location.pathname === '/account' ? 'rgba(255,255,255,0.1)' : 'transparent'
          }}
        >
          Můj účet
        </Button>
      </Toolbar>
    </AppBar>
  );
}

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <SWRConfig
        value={{
          fetcher: (resource, init) => fetch(resource, init).then(res => res.json()),
          errorRetryCount: 1,
          revalidateOnFocus: false,
        }}
      >
        <Router>
          <Box sx={{ flexGrow: 1 }}>
            <NavigationBar />
            <Container maxWidth="lg" sx={{ mt: 4, mb: 4 }}>
              <Routes>
                <Route path="/" element={<HomePage />} />
                <Route path="/account" element={<AccountPage />} />
              </Routes>
            </Container>
          </Box>
        </Router>
      </SWRConfig>
    </ThemeProvider>
  );
}

export default App;
