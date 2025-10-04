import { createTheme } from '@mui/material/styles';

// 🎨 1. BRAND DESIGN TOKENS - podle vašich principů
const designTokens = {
  // Primární paleta - profesionální modrá (WCAG AA compliant)
  primary: {
    50: '#e3f2fd',
    100: '#bbdefb', 
    500: '#1976d2', // Hlavní brand barva
    600: '#1565c0',
    900: '#0d47a1'
  },
  
  // Sekundární paleta - doplňková
  secondary: {
    50: '#f3e5f5',
    100: '#e1bee7',
    500: '#9c27b0',
    600: '#8e24aa', 
    900: '#4a148c'
  },

  // Neutralní paleta - 4 odstíny jak požadujete
  neutral: {
    50: '#fafafa',   // Nejsvětlejší - pozadí
    200: '#e0e0e0',  // Borders, dividers
    600: '#757575',  // Sekundární text
    900: '#212121'   // Hlavní text
  },

  // 4 Feedback barvy - podle UX best practices
  feedback: {
    success: '#2e7d32',   // ✅ Zelená - úspěch
    warning: '#f57c00',   // ⚠️ Oranžová - varování  
    error: '#d32f2f',     // ❌ Červená - chyba
    info: '#1976d2'       // ℹ️ Modrá - informace
  },

  // Spacing scale (4/8/16px jak požadujete)
  spacing: {
    xs: 4,
    sm: 8, 
    md: 16,
    lg: 24,
    xl: 32,
    xxl: 48
  }
};

// 🔤 2. TYPOGRAFIE - max 2 fonty jak požadujete
const typography = {
  // Hlavní font - Inter pro čitelnost
  fontFamily: "'Inter', 'Roboto', -apple-system, BlinkMacSystemFont, sans-serif",
  
  // Hierarchie podle vašich pravidel
  h1: {
    fontSize: '2.5rem',      // Název stránky
    fontWeight: 600,
    lineHeight: 1.2,
    letterSpacing: '-0.02em',
    color: designTokens.neutral[900]
  },
  h2: {
    fontSize: '2rem',        // Hlavní sekce  
    fontWeight: 600,
    lineHeight: 1.3,
    letterSpacing: '-0.01em',
    color: designTokens.neutral[900]
  },
  h3: {
    fontSize: '1.5rem',      // Podsekce
    fontWeight: 600, 
    lineHeight: 1.4,
    color: designTokens.neutral[900]
  },
  h4: {
    fontSize: '1.25rem',
    fontWeight: 600,
    lineHeight: 1.4,
    color: designTokens.neutral[900]
  },
  h5: {
    fontSize: '1.125rem',
    fontWeight: 600,
    lineHeight: 1.4,
    color: designTokens.neutral[900]
  },
  h6: {
    fontSize: '1rem',
    fontWeight: 600,
    lineHeight: 1.4,
    color: designTokens.neutral[900]
  },
  
  // Body text - 14-16px jak požadujete
  body1: {
    fontSize: '1rem',        // 16px - hlavní text
    fontWeight: 400,
    lineHeight: 1.6,         // 1.6× pro čitelnost
    color: designTokens.neutral[900]
  },
  body2: {
    fontSize: '0.875rem',    // 14px - menší text
    fontWeight: 400,
    lineHeight: 1.5,
    color: designTokens.neutral[600]
  },
  
  // Labels/Caption - 12-14px
  caption: {
    fontSize: '0.75rem',     // 12px
    fontWeight: 400,
    lineHeight: 1.4,
    color: designTokens.neutral[600]
  },
  
  // Tlačítka - bez CAPS LOCK jak požadujete
  button: {
    fontSize: '0.875rem',
    fontWeight: 500,
    lineHeight: 1.4,
    textTransform: 'none',   // Žádné caps lock!
    letterSpacing: '0.02em'
  },

  // Monospace pro logy/kód
  code: {
    fontFamily: "'JetBrains Mono', 'Fira Code', 'Consolas', monospace",
    fontSize: '0.875rem'
  }
};

// 🖼️ 3. LAYOUT SYSTEM
const layoutTokens = {
  // Grid systém - 12 columns jak požadujete
  gridColumns: 12,
  
  // Breakpoints pro responsivitu
  breakpoints: {
    mobile: 0,      // <768px
    tablet: 768,    // 768-1279px  
    desktop: 1280   // ≥1280px
  },
  
  // Container max widths
  container: {
    mobile: '100%',
    tablet: '768px', 
    desktop: '1200px'
  }
};

// 🎨 MATERIAL-UI THEME s vaším design systémem
export const coreMaterialTheme = createTheme({
  // Paleta podle design tokens
  palette: {
    mode: 'light',
    
    // Primární brand barva
    primary: {
      main: designTokens.primary[500],
      light: designTokens.primary[100],
      dark: designTokens.primary[600],
      contrastText: '#ffffff'
    },
    
    // Sekundární barva
    secondary: {
      main: designTokens.secondary[500], 
      light: designTokens.secondary[100],
      dark: designTokens.secondary[600],
      contrastText: '#ffffff'
    },
    
    // Feedback barvy - smysluplné použití
    success: {
      main: designTokens.feedback.success,
      light: '#4caf50',
      dark: '#1b5e20',
      contrastText: '#ffffff'
    },
    warning: {
      main: designTokens.feedback.warning,
      light: '#ff9800', 
      dark: '#e65100',
      contrastText: '#ffffff'
    },
    error: {
      main: designTokens.feedback.error,
      light: '#f44336',
      dark: '#b71c1c', 
      contrastText: '#ffffff'
    },
    info: {
      main: designTokens.feedback.info,
      light: '#2196f3',
      dark: '#0d47a1',
      contrastText: '#ffffff' 
    },
    
    // Neutralní paleta
    grey: {
      50: designTokens.neutral[50],
      200: designTokens.neutral[200], 
      600: designTokens.neutral[600],
      900: designTokens.neutral[900]
    },
    
    // Text barvy s WCAG AA kontrastem
    text: {
      primary: designTokens.neutral[900],    // 4.5:1 kontrast
      secondary: designTokens.neutral[600],  // 4.5:1 kontrast
    },
    
    // Pozadí
    background: {
      default: designTokens.neutral[50],
      paper: '#ffffff'
    },
    
    // Akce
    action: {
      hover: 'rgba(0, 0, 0, 0.04)',
      selected: 'rgba(25, 118, 210, 0.08)',
      disabled: 'rgba(0, 0, 0, 0.26)',
      disabledBackground: 'rgba(0, 0, 0, 0.12)'
    },
    
    divider: designTokens.neutral[200]
  },
  
  // Typografie
  typography,
  
  // Spacing podle 4/8/16 scale
  spacing: designTokens.spacing.xs,
  
  // Breakpoints pro responsivitu
  breakpoints: {
    values: layoutTokens.breakpoints
  },
  
  // Shape (border radius)
  shape: {
    borderRadius: 8
  },

  // 🖱️ 4. KOMPONENTY - podle UX pravidel
  components: {
    // Global styles
    MuiCssBaseline: {
      styleOverrides: {
        body: {
          fontFamily: typography.fontFamily,
          backgroundColor: designTokens.neutral[50],
          color: designTokens.neutral[900]
        },
        // Focus outline - nikdy neskrývat!
        '*:focus-visible': {
          outline: `2px solid ${designTokens.primary[500]}`,
          outlineOffset: '2px'
        }
      }
    },
    
    // 🔘 TLAČÍTKA - konzistentní podle typu akce
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          padding: `${designTokens.spacing.sm}px ${designTokens.spacing.md}px`,
          fontSize: typography.button.fontSize,
          fontWeight: typography.button.fontWeight,
          textTransform: typography.button.textTransform,
          
          // Hover efekty
          '&:hover': {
            transform: 'translateY(-1px)',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            transition: 'all 0.2s ease'
          }
        },
        
        // Primární akce - výrazné
        contained: {
          backgroundColor: designTokens.primary[500],
          color: '#ffffff',
          boxShadow: '0 2px 8px rgba(25,118,210,0.3)',
          
          '&:hover': {
            backgroundColor: designTokens.primary[600]
          }
        },
        
        // Sekundární akce - méně výrazné  
        outlined: {
          borderColor: designTokens.neutral[200],
          color: designTokens.neutral[900],
          
          '&:hover': {
            borderColor: designTokens.primary[500],
            backgroundColor: designTokens.primary[50]
          }
        }
      },
      
      // Varianty pro různé typy akcí
      variants: [
        // Destruktivní akce - červené
        {
          props: { variant: 'destructive' },
          style: {
            backgroundColor: designTokens.feedback.error,
            color: '#ffffff',
            
            '&:hover': {
              backgroundColor: '#b71c1c'
            }
          }
        }
      ]
    },
    
    // 📝 FORMULÁŘE - podle UX pravidel
    MuiTextField: {
      styleOverrides: {
        root: {
          marginBottom: designTokens.spacing.md,
          
          // Label vždy nahoře, aligned left
          '& .MuiInputLabel-root': {
            position: 'static',
            transform: 'none',
            marginBottom: designTokens.spacing.xs,
            color: designTokens.neutral[900],
            fontSize: typography.body2.fontSize,
            fontWeight: 500
          },
          
          '& .MuiOutlinedInput-root': {
            borderRadius: 8,
            
            '& fieldset': {
              borderColor: designTokens.neutral[200]
            },
            
            '&:hover fieldset': {
              borderColor: designTokens.primary[500] 
            },
            
            '&.Mui-focused fieldset': {
              borderColor: designTokens.primary[500],
              borderWidth: 2
            }
          },
          
          // Error states - pod polem, červeně
          '& .MuiFormHelperText-root.Mui-error': {
            color: designTokens.feedback.error,
            marginTop: designTokens.spacing.xs
          }
        }
      }
    },
    
    // 📄 KARTY - whitespace a konzistentní padding
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: 12,
          padding: designTokens.spacing.lg,
          boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
          border: `1px solid ${designTokens.neutral[200]}`,
          
          '&:hover': {
            boxShadow: '0 4px 20px rgba(0,0,0,0.12)',
            transition: 'box-shadow 0.2s ease'
          }
        }
      }
    },
    
    // 📊 DATA GRID/TABLE
    MuiTableHead: {
      styleOverrides: {
        root: {
          backgroundColor: designTokens.neutral[50],
          
          '& .MuiTableCell-head': {
            fontWeight: 600,
            color: designTokens.neutral[900]
          }
        }
      }
    },
    
    // 🔔 FEEDBACK komponenty 
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          padding: designTokens.spacing.md
        },
        
        standardSuccess: {
          backgroundColor: '#e8f5e8',
          color: designTokens.feedback.success
        },
        
        standardWarning: {
          backgroundColor: '#fff3e0', 
          color: designTokens.feedback.warning
        },
        
        standardError: {
          backgroundColor: '#ffebee',
          color: designTokens.feedback.error
        },
        
        standardInfo: {
          backgroundColor: designTokens.primary[50],
          color: designTokens.feedback.info
        }
      }
    }
  }
});

export default coreMaterialTheme;