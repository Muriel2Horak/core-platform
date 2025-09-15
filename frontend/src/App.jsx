import './App.css'
import { CssBaseline, ThemeProvider, Box, Button, Typography } from '@mui/material';
import { baselightTheme } from "./theme/DefaultColors";
import { RouterProvider } from 'react-router';
import router from "./routes/Router.js"
import AuthGuard from './components/AuthGuard/AuthGuard';
import { logger } from './services/logger.js';

function App() {
  const theme = baselightTheme;

  // Testovací funkce pro různé typy logů
  const testLogs = {
    info: () => {
      logger.info('TEST_INFO', 'Testovací INFO log z frontendu', { 
        testData: 'nějaká data', 
        timestamp: Date.now() 
      });
    },
    
    error: () => {
      logger.error('TEST_ERROR', 'Testovací ERROR log z frontendu', { 
        errorType: 'test', 
        component: 'App.jsx',
        stack: 'Simulovaný stack trace'
      });
    },
    
    warn: () => {
      logger.warn('TEST_WARN', 'Testovací WARNING log z frontendu', { 
        warningType: 'test',
        action: 'user_test'
      });
    },
    
    security: () => {
      logger.security('TEST_SECURITY', 'Testovací SECURITY log z frontendu', { 
        threatLevel: 'low',
        action: 'manual_test'
      });
    },
    
    apiCall: () => {
      logger.apiCall('GET', '/api/test', 404, 150, { 
        description: 'Simulovaný API call test'
      });
    }
  };

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      
      {/* Testovací panel pro logování - pouze pro development */}
      {import.meta.env.DEV && (
        <Box 
          sx={{ 
            position: 'fixed', 
            top: 10, 
            right: 10, 
            zIndex: 9999, 
            bgcolor: 'background.paper',
            p: 2,
            border: '1px solid #ccc',
            borderRadius: 1,
            boxShadow: 2
          }}
        >
          <Typography variant="h6" gutterBottom>🧪 Log Tester</Typography>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            <Button size="small" variant="outlined" onClick={testLogs.info} color="primary">
              📝 Test INFO
            </Button>
            <Button size="small" variant="outlined" onClick={testLogs.error} color="error">
              ❌ Test ERROR  
            </Button>
            <Button size="small" variant="outlined" onClick={testLogs.warn} color="warning">
              ⚠️ Test WARN
            </Button>
            <Button size="small" variant="outlined" onClick={testLogs.security} sx={{color: 'purple'}}>
              🔒 Test SECURITY
            </Button>
            <Button size="small" variant="outlined" onClick={testLogs.apiCall} color="secondary">
              📡 Test API Call
            </Button>
          </Box>
        </Box>
      )}

      <AuthGuard>
        <RouterProvider router={router} />
      </AuthGuard>
    </ThemeProvider>
  );
}

export default App