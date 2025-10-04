import { createTheme } from '@mui/material/styles';
import type { ThemeOptions, Shadows } from '@mui/material/styles';

// 🎨 Typography podle našeho Design Systému - TypeScript compliant
const typography: ThemeOptions['typography'] = {
  fontFamily: "'Inter', 'Roboto', -apple-system, BlinkMacSystemFont, sans-serif", // Podle našich pravidel
  h1: {
    fontWeight: 600,
    fontSize: '2.5rem',      // Název stránky podle hierarchie
    lineHeight: 1.2,
    letterSpacing: '-0.02em',
  },
  h2: {
    fontWeight: 600,
    fontSize: '2rem',        // Hlavní sekce
    lineHeight: 1.3,
    letterSpacing: '-0.01em',
  },
  h3: {
    fontWeight: 600,
    fontSize: '1.5rem',      // Podsekce
    lineHeight: 1.4,
  },
  h4: {
    fontWeight: 600,
    fontSize: '1.25rem',
    lineHeight: 1.4,
  },
  h5: {
    fontWeight: 600,
    fontSize: '1.125rem',
    lineHeight: 1.4,
  },
  h6: {
    fontWeight: 600,
    fontSize: '1rem',
    lineHeight: 1.4,
  },
  button: {
    textTransform: 'none' as const, // ❌ Žádné CAPS LOCK podle UX pravidel!
    fontWeight: 500,
    fontSize: '0.875rem',
    lineHeight: 1.4,
    letterSpacing: '0.02em'
  },
  body1: {
    fontSize: '1rem',        // 16px - hlavní text podle pravidel
    fontWeight: 400,
    lineHeight: 1.6,         // 1.6× pro čitelnost
  },
  body2: {
    fontSize: '0.875rem',    // 14px - menší text
    fontWeight: 400,
    lineHeight: 1.5,
  },
  subtitle1: {
    fontSize: '0.875rem',
    fontWeight: 500,
    lineHeight: 1.4,
  },
  subtitle2: {
    fontSize: '0.75rem',     // 12px - labels/caption
    fontWeight: 400,
    lineHeight: 1.4,
  },
};

// Shadows array - správný počet pro MUI (25 shadows)
const shadows: Shadows = [
  'none',
  '0px 2px 1px -1px rgba(0,0,0,0.2),0px 1px 1px 0px rgba(0,0,0,0.14),0px 1px 3px 0px rgba(0,0,0,0.12)',
  '0px 3px 1px -2px rgba(0,0,0,0.2),0px 2px 2px 0px rgba(0,0,0,0.14),0px 1px 5px 0px rgba(0,0,0,0.12)',
  '0px 3px 3px -2px rgba(0,0,0,0.2),0px 3px 4px 0px rgba(0,0,0,0.14),0px 1px 8px 0px rgba(0,0,0,0.12)',
  '0px 2px 4px -1px rgba(0,0,0,0.2),0px 4px 5px 0px rgba(0,0,0,0.14),0px 1px 10px 0px rgba(0,0,0,0.12)',
  '0px 3px 5px -1px rgba(0,0,0,0.2),0px 5px 8px 0px rgba(0,0,0,0.14),0px 1px 14px 0px rgba(0,0,0,0.12)',
  '0px 3px 5px -1px rgba(0,0,0,0.2),0px 6px 10px 0px rgba(0,0,0,0.14),0px 1px 18px 0px rgba(0,0,0,0.12)',
  '0px 4px 5px -2px rgba(0,0,0,0.2),0px 7px 10px 1px rgba(0,0,0,0.14),0px 2px 16px 1px rgba(0,0,0,0.12)',
  '0px 5px 5px -3px rgba(0,0,0,0.2),0px 8px 10px 1px rgba(0,0,0,0.14),0px 3px 14px 2px rgba(0,0,0,0.12)',
  '0 9px 17.5px rgb(0,0,0,0.05)',
  '0px 6px 6px -3px rgba(0,0,0,0.2),0px 10px 14px 1px rgba(0,0,0,0.14),0px 4px 18px 3px rgba(0,0,0,0.12)',
  '0px 6px 7px -4px rgba(0,0,0,0.2),0px 11px 15px 1px rgba(0,0,0,0.14),0px 4px 20px 3px rgba(0,0,0,0.12)',
  '0px 7px 8px -4px rgba(0,0,0,0.2),0px 12px 17px 2px rgba(0,0,0,0.14),0px 5px 22px 4px rgba(0,0,0,0.12)',
  '0px 7px 8px -4px rgba(0,0,0,0.2),0px 13px 19px 2px rgba(0,0,0,0.14),0px 5px 24px 4px rgba(0,0,0,0.12)',
  '0px 7px 9px -4px rgba(0,0,0,0.2),0px 14px 21px 2px rgba(0,0,0,0.14),0px 5px 26px 4px rgba(0,0,0,0.12)',
  '0px 8px 9px -5px rgba(0,0,0,0.2),0px 15px 22px 2px rgba(0,0,0,0.14),0px 6px 28px 5px rgba(0,0,0,0.12)',
  '0px 8px 10px -5px rgba(0,0,0,0.2),0px 16px 24px 2px rgba(0,0,0,0.14),0px 6px 30px 5px rgba(0,0,0,0.12)',
  '0px 8px 11px -5px rgba(0,0,0,0.2),0px 17px 26px 2px rgba(0,0,0,0.14),0px 6px 32px 5px rgba(0,0,0,0.12)',
  '0px 9px 11px -5px rgba(0,0,0,0.2),0px 18px 28px 2px rgba(0,0,0,0.14),0px 7px 34px 6px rgba(0,0,0,0.12)',
  '0px 9px 12px -6px rgba(0,0,0,0.2),0px 19px 29px 2px rgba(0,0,0,0.14),0px 7px 36px 6px rgba(0,0,0,0.12)',
  '0px 10px 13px -6px rgba(0,0,0,0.2),0px 20px 31px 3px rgba(0,0,0,0.14),0px 8px 38px 7px rgba(0,0,0,0.12)',
  '0px 10px 13px -6px rgba(0,0,0,0.2),0px 21px 33px 3px rgba(0,0,0,0.14),0px 8px 40px 7px rgba(0,0,0,0.12)',
  '0px 10px 14px -6px rgba(0,0,0,0.2),0px 22px 35px 3px rgba(0,0,0,0.14),0px 8px 42px 7px rgba(0,0,0,0.12)',
  '0px 11px 14px -7px rgba(0,0,0,0.2),0px 23px 36px 3px rgba(0,0,0,0.14),0px 9px 44px 8px rgba(0,0,0,0.12)',
  '0px 11px 15px -7px rgba(0,0,0,0.2),0px 24px 38px 3px rgba(0,0,0,0.14),0px 9px 46px 8px rgba(0,0,0,0.12)',
];

// 🎨 Core Material theme - profesionální design podle našich principů
export const coreMaterialTheme = createTheme({
  direction: 'ltr',
  palette: {
    mode: 'light',
    
    // Primární brand barva - sjednocená s React aplikací
    primary: {
      main: '#667eea',      // ✅ NOVÁ barva podle design systému!
      light: '#8b95f8',
      dark: '#4d68d1',
      contrastText: '#ffffff',
    },
    
    // Sekundární barva - sjednocená s React aplikací  
    secondary: {
      main: '#764ba2',      // ✅ NOVÁ sekundární barva!
      light: '#c084fc',
      dark: '#5d3a82',
      contrastText: '#ffffff',
    },
    
    // 🔔 Feedback barvy - smysluplné použití
    success: {
      main: '#2e7d32',      // ✅ Zelená - úspěch
      light: '#E6FFFA',
      dark: '#1b5e20',
      contrastText: '#ffffff',
    },
    info: {
      main: '#1976d2',      // ℹ️ Modrá - informace
      light: '#EBF3FE',
      dark: '#0d47a1',
      contrastText: '#ffffff',
    },
    error: {
      main: '#d32f2f',      // ❌ Červená - chyba
      light: '#FDEDE8',
      dark: '#b71c1c',
      contrastText: '#ffffff',
    },
    warning: {
      main: '#f57c00',      // ⚠️ Oranžová - varování
      light: '#FEF5E5',
      dark: '#e65100',
      contrastText: '#ffffff',
    },
    
    // Neutralní paleta podle design systému
    grey: {
      50: '#fafafa',        // Pozadí
      100: '#f5f5f5',
      200: '#e0e0e0',       // Borders, dividers
      300: '#DFE5EF',
      400: '#bdbdbd',
      500: '#9e9e9e',
      600: '#757575',       // Sekundární text
      700: '#616161',
      800: '#424242',
      900: '#212121',       // Hlavní text
    },
    
    // Text barvy s WCAG AA kontrastem
    text: {
      primary: '#212121',    // 4.5:1 kontrast
      secondary: '#757575',  // 4.5:1 kontrast
    },
    
    action: {
      disabledBackground: 'rgba(0,0,0,0.12)',
      hoverOpacity: 0.04,
      hover: 'rgba(0, 0, 0, 0.04)',
    },
    
    divider: '#e0e0e0',
    
    background: {
      default: '#fafafa',   // Světlé pozadí
      paper: '#ffffff',
    },
  },
  
  typography,
  shadows,
  
  // Shape (border radius)
  shape: {
    borderRadius: 8
  },
  
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          fontFamily: typography.fontFamily,
          backgroundColor: '#fafafa',
          // Focus outline - nikdy neskrývat!
          '& *:focus-visible': {
            outline: '2px solid #1976d2',
            outlineOffset: '2px'
          }
        },
        a: {
          textDecoration: "none",
        },
      }
    },
    
    // 🔘 Tlačítka podle UX pravidel
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          boxShadow: 'none',
          textTransform: 'none', // Žádné CAPS LOCK!
          fontWeight: 500,
          padding: '8px 16px',   // Konzistentní spacing
          
          '&:hover': {
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            transform: 'translateY(-1px)',
            transition: 'all 0.2s ease'
          }
        },
        
        // Primární akce - výrazné
        contained: {
          backgroundColor: '#1976d2',
          color: '#ffffff',
          '&:hover': {
            backgroundColor: '#1565c0'
          }
        },
        
        // Sekundární akce - méně výrazné
        outlined: {
          borderColor: '#e0e0e0',
          color: '#212121',
          '&:hover': {
            borderColor: '#1976d2',
            backgroundColor: '#E3F2FD'
          }
        }
      },
    },
    
    // 📄 Karty - whitespace a konzistentní padding
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
          borderRadius: 12,
          border: '1px solid #e0e0e0',
          
          '&:hover': {
            boxShadow: '0 4px 20px rgba(0,0,0,0.12)',
            transition: 'box-shadow 0.2s ease'
          }
        },
      },
    },
    
    // 📝 Formuláře - podle UX pravidel
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            borderRadius: 8,
            
            '& fieldset': {
              borderColor: '#e0e0e0',
            },
            
            '&:hover fieldset': {
              borderColor: '#1976d2',
            },
            
            '&.Mui-focused fieldset': {
              borderColor: '#1976d2',
              borderWidth: 2
            },
          },
        },
      },
    },
  }
});

export default coreMaterialTheme;