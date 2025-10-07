/**
 * 🎨 Core Platform MUI Theme - Glassmorphic Edition
 * 
 * Minimalistický design systém s podporou dark/light módu.
 * Inspirovaný moderním Keycloak Glass designem.
 */

import { createTheme, ThemeOptions, alpha } from '@mui/material/styles';
import { tokens } from './tokens';

// 🌓 Detekce systémového dark módu
const prefersDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches;

// 🎨 Glassmorphic color palette pro light mode
const lightPalette = {
  mode: 'light' as const,
  primary: {
    main: '#1976d2',      // Modrá - méně výrazná než fialová
    light: '#42a5f5',
    dark: '#1565c0',
    contrastText: '#ffffff',
  },
  secondary: {
    main: '#455a64',      // Šedá - neutrální
    light: '#78909c',
    dark: '#37474f',
    contrastText: '#ffffff',
  },
  background: {
    default: '#f5f5f7',   // Světle šedá - jako macOS
    paper: 'rgba(255, 255, 255, 0.8)',  // Glassmorphic bílá
  },
  text: {
    primary: '#1a1a1a',
    secondary: '#6b6b6b',
    disabled: '#9e9e9e',
  },
  divider: 'rgba(0, 0, 0, 0.08)',
};

// 🎨 Glassmorphic color palette pro dark mode
const darkPalette = {
  mode: 'dark' as const,
  primary: {
    main: '#64b5f6',      // Světlejší modrá pro dark mode
    light: '#90caf9',
    dark: '#42a5f5',
    contrastText: '#000000',
  },
  secondary: {
    main: '#90a4ae',      // Světlejší šedá
    light: '#b0bec5',
    dark: '#78909c',
    contrastText: '#000000',
  },
  background: {
    default: '#121212',   // Tmavá - Material Design standard
    paper: 'rgba(30, 30, 30, 0.8)',  // Glassmorphic tmavá
  },
  text: {
    primary: '#ffffff',
    secondary: '#b0b0b0',
    disabled: '#757575',
  },
  divider: 'rgba(255, 255, 255, 0.12)',
};

// 🔧 Base theme options
const baseThemeOptions: ThemeOptions = {
  palette: prefersDarkMode ? darkPalette : lightPalette,
  
  typography: {
    fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
    h1: {
      fontSize: '2.5rem',
      fontWeight: 600,
      letterSpacing: '-0.02em',
    },
    h2: {
      fontSize: '2rem',
      fontWeight: 600,
      letterSpacing: '-0.01em',
    },
    h3: {
      fontSize: '1.75rem',
      fontWeight: 600,
    },
    h4: {
      fontSize: '1.5rem',
      fontWeight: 600,
    },
    h5: {
      fontSize: '1.25rem',
      fontWeight: 600,
    },
    h6: {
      fontSize: '1rem',
      fontWeight: 600,
    },
    body1: {
      fontSize: '1rem',
      lineHeight: 1.7,
    },
    body2: {
      fontSize: '0.875rem',
      lineHeight: 1.6,
    },
    button: {
      textTransform: 'none',
      fontWeight: 500,
    },
  },

  shape: {
    borderRadius: 12,
  },

  shadows: [
    'none',
    '0 2px 4px rgba(0,0,0,0.05)',
    '0 4px 8px rgba(0,0,0,0.08)',
    '0 8px 16px rgba(0,0,0,0.1)',
    '0 12px 24px rgba(0,0,0,0.12)',
    '0 16px 32px rgba(0,0,0,0.15)',
    ...Array(19).fill('0 20px 40px rgba(0,0,0,0.2)'),
  ],
};

// 🎨 Create base theme
const baseTheme = createTheme(baseThemeOptions);

// 🔧 Glassmorphic component overrides
const glassTheme = createTheme({
  ...baseTheme,
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          // Glassmorphic gradient pozadí
          background: prefersDarkMode
            ? 'linear-gradient(135deg, #1a1a1a 0%, #2d2d2d 100%)'
            : 'linear-gradient(135deg, #f5f5f7 0%, #e8e8eb 100%)',
          backgroundAttachment: 'fixed',
        },
      },
    },

    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
          backgroundColor: prefersDarkMode 
            ? 'rgba(30, 30, 30, 0.8)' 
            : 'rgba(255, 255, 255, 0.8)',
          backdropFilter: 'blur(20px) saturate(180%)',
          border: prefersDarkMode
            ? '1px solid rgba(255, 255, 255, 0.1)'
            : '1px solid rgba(0, 0, 0, 0.08)',
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        },
        elevation1: {
          boxShadow: prefersDarkMode
            ? '0 4px 16px rgba(0, 0, 0, 0.4)'
            : '0 4px 16px rgba(0, 0, 0, 0.1)',
        },
      },
    },

    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: prefersDarkMode 
            ? 'rgba(30, 30, 30, 0.8)' 
            : 'rgba(255, 255, 255, 0.8)',
          backdropFilter: 'blur(20px) saturate(180%)',
          borderBottom: prefersDarkMode
            ? '1px solid rgba(255, 255, 255, 0.1)'
            : '1px solid rgba(0, 0, 0, 0.08)',
          boxShadow: 'none',
        },
      },
    },

    MuiDrawer: {
      styleOverrides: {
        paper: {
          backgroundColor: prefersDarkMode 
            ? 'rgba(20, 20, 20, 0.95)' 
            : 'rgba(255, 255, 255, 0.95)',
          backdropFilter: 'blur(20px) saturate(180%)',
          borderRight: prefersDarkMode
            ? '1px solid rgba(255, 255, 255, 0.1)'
            : '1px solid rgba(0, 0, 0, 0.08)',
        },
      },
    },

    MuiCard: {
      styleOverrides: {
        root: {
          backgroundColor: prefersDarkMode 
            ? 'rgba(30, 30, 30, 0.8)' 
            : 'rgba(255, 255, 255, 0.8)',
          backdropFilter: 'blur(20px) saturate(180%)',
          border: prefersDarkMode
            ? '1px solid rgba(255, 255, 255, 0.1)'
            : '1px solid rgba(0, 0, 0, 0.08)',
          borderRadius: 16,
          boxShadow: prefersDarkMode
            ? '0 8px 24px rgba(0, 0, 0, 0.4)'
            : '0 8px 24px rgba(0, 0, 0, 0.1)',
          transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            transform: 'translateY(-4px)',
            boxShadow: prefersDarkMode
              ? '0 12px 32px rgba(0, 0, 0, 0.5)'
              : '0 12px 32px rgba(0, 0, 0, 0.15)',
          },
        },
      },
    },

    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          padding: '10px 24px',
          fontWeight: 500,
          textTransform: 'none',
          transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
          '&:hover': {
            transform: 'translateY(-1px)',
          },
        },
        contained: {
          boxShadow: 'none',
          '&:hover': {
            boxShadow: prefersDarkMode
              ? '0 8px 16px rgba(100, 181, 246, 0.3)'
              : '0 8px 16px rgba(25, 118, 210, 0.3)',
          },
        },
        outlined: {
          borderWidth: '1.5px',
          '&:hover': {
            borderWidth: '1.5px',
            backgroundColor: prefersDarkMode
              ? 'rgba(100, 181, 246, 0.08)'
              : 'rgba(25, 118, 210, 0.08)',
          },
        },
      },
    },

    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            backgroundColor: prefersDarkMode 
              ? 'rgba(255, 255, 255, 0.05)' 
              : 'rgba(0, 0, 0, 0.02)',
            backdropFilter: 'blur(10px)',
            borderRadius: 10,
            transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
            '&:hover': {
              backgroundColor: prefersDarkMode 
                ? 'rgba(255, 255, 255, 0.08)' 
                : 'rgba(0, 0, 0, 0.04)',
            },
            '&.Mui-focused': {
              backgroundColor: prefersDarkMode 
                ? 'rgba(255, 255, 255, 0.1)' 
                : 'rgba(0, 0, 0, 0.03)',
            },
          },
        },
      },
    },

    MuiDialog: {
      styleOverrides: {
        paper: {
          backgroundColor: prefersDarkMode 
            ? 'rgba(30, 30, 30, 0.95)' 
            : 'rgba(255, 255, 255, 0.95)',
          backdropFilter: 'blur(20px) saturate(180%)',
          borderRadius: 20,
          border: prefersDarkMode
            ? '1px solid rgba(255, 255, 255, 0.1)'
            : '1px solid rgba(0, 0, 0, 0.08)',
        },
      },
    },

    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          backdropFilter: 'blur(10px)',
        },
        filled: {
          backgroundColor: prefersDarkMode 
            ? 'rgba(100, 181, 246, 0.2)' 
            : 'rgba(25, 118, 210, 0.1)',
        },
      },
    },
  },
});

export const corePlatformTheme = glassTheme;
export const theme = glassTheme;
export default glassTheme;