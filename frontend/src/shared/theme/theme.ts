/**
 * 🎨 Core Platform MUI Theme
 * 
 * Material-UI theme vytvořený z našich design tokens.
 * Obsahuje kompletní overrides pro konzistentní vzhled.
 */

import { createTheme, ThemeOptions } from '@mui/material/styles';
import { tokens } from './tokens';

// 🔧 Base theme options z našich tokens
const baseThemeOptions: ThemeOptions = {
  // 🎨 Palette z tokens
  palette: {
    mode: 'light',
    primary: {
      main: tokens.colors.primary[500],
      light: tokens.colors.primary[400],
      dark: tokens.colors.primary[600],
      contrastText: tokens.colors.white,
    },
    secondary: {
      main: tokens.colors.secondary[500],
      light: tokens.colors.secondary[400], 
      dark: tokens.colors.secondary[600],
      contrastText: tokens.colors.white,
    },
    error: {
      main: tokens.colors.error[500],
      light: tokens.colors.error[200],
      dark: tokens.colors.error[700],
      contrastText: tokens.colors.white,
    },
    warning: {
      main: tokens.colors.warning[500],
      light: tokens.colors.warning[200],
      dark: tokens.colors.warning[700],
      contrastText: tokens.colors.white,
    },
    info: {
      main: tokens.colors.info[500],
      light: tokens.colors.info[200],
      dark: tokens.colors.info[700],
      contrastText: tokens.colors.white,
    },
    success: {
      main: tokens.colors.success[500],
      light: tokens.colors.success[200],
      dark: tokens.colors.success[700],
      contrastText: tokens.colors.white,
    },
    grey: tokens.colors.grey,
    background: {
      default: tokens.colors.grey[50],
      paper: tokens.colors.white,
    },
    text: {
      primary: tokens.colors.grey[900],
      secondary: tokens.colors.grey[600],
      disabled: tokens.colors.grey[400],
    },
    divider: tokens.colors.grey[200],
  },

  // ✍️ Typography z tokens
  typography: {
    fontFamily: tokens.typography.fontFamily.primary,
    h1: {
      fontSize: tokens.typography.fontSize['5xl'],
      fontWeight: tokens.typography.fontWeight.bold,
      lineHeight: tokens.typography.lineHeight.tight,
      letterSpacing: '-0.025em',
    },
    h2: {
      fontSize: tokens.typography.fontSize['4xl'],
      fontWeight: tokens.typography.fontWeight.semibold,
      lineHeight: tokens.typography.lineHeight.snug,
      letterSpacing: '-0.025em',
    },
    h3: {
      fontSize: tokens.typography.fontSize['3xl'],
      fontWeight: tokens.typography.fontWeight.semibold,
      lineHeight: tokens.typography.lineHeight.normal,
    },
    h4: {
      fontSize: tokens.typography.fontSize['2xl'],
      fontWeight: tokens.typography.fontWeight.semibold,
      lineHeight: tokens.typography.lineHeight.normal,
    },
    h5: {
      fontSize: tokens.typography.fontSize.xl,
      fontWeight: tokens.typography.fontWeight.semibold,
      lineHeight: tokens.typography.lineHeight.relaxed,
    },
    h6: {
      fontSize: tokens.typography.fontSize.base,
      fontWeight: tokens.typography.fontWeight.semibold,
      lineHeight: tokens.typography.lineHeight.relaxed,
    },
    body1: {
      fontSize: tokens.typography.fontSize.base,
      fontWeight: tokens.typography.fontWeight.normal,
      lineHeight: tokens.typography.lineHeight.loose,
    },
    body2: {
      fontSize: tokens.typography.fontSize.sm,
      fontWeight: tokens.typography.fontWeight.normal,
      lineHeight: tokens.typography.lineHeight.loose,
    },
    caption: {
      fontSize: tokens.typography.fontSize.xs,
      fontWeight: tokens.typography.fontWeight.normal,
      lineHeight: tokens.typography.lineHeight.normal,
    },
    button: {
      fontSize: tokens.typography.fontSize.sm,
      fontWeight: tokens.typography.fontWeight.medium,
      textTransform: 'none' as const, // Disable uppercase
      letterSpacing: '0.02em',
    },
  },

  // 📐 Shape & spacing
  shape: {
    borderRadius: parseInt(tokens.radius.lg, 10), // 12px default
  },

  spacing: 8, // 8px base unit

  // 📱 Breakpoints z tokens
  breakpoints: {
    values: {
      xs: parseInt(tokens.breakpoints.xs, 10),
      sm: parseInt(tokens.breakpoints.sm, 10), 
      md: parseInt(tokens.breakpoints.md, 10),
      lg: parseInt(tokens.breakpoints.lg, 10),
      xl: parseInt(tokens.breakpoints.xl, 10),
    },
  },

  // 🌟 Shadows z tokens
  shadows: [
    'none',
    tokens.shadows.sm,
    tokens.shadows.sm,
    tokens.shadows.md,
    tokens.shadows.md,
    tokens.shadows.lg,
    tokens.shadows.lg,
    tokens.shadows.xl,
    tokens.shadows.xl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
    tokens.shadows.xxl,
  ],

  // 🎞️ Transitions
  transitions: {
    duration: {
      shortest: 150,
      shorter: 200,
      short: 250,
      standard: 300,
      complex: 375,
      enteringScreen: 225,
      leavingScreen: 195,
    },
    easing: {
      easeInOut: tokens.components.animation.easing,
      easeOut: 'cubic-bezier(0.0, 0, 0.2, 1)',
      easeIn: 'cubic-bezier(0.4, 0, 1, 1)',
      sharp: 'cubic-bezier(0.4, 0, 0.6, 1)',
    },
  },

  // 🎯 Z-index z tokens  
  zIndex: {
    mobileStepper: tokens.zIndex.base,
    speedDial: tokens.zIndex.docked,
    appBar: tokens.zIndex.sticky,
    drawer: tokens.zIndex.overlay,
    modal: tokens.zIndex.modal,
    snackbar: tokens.zIndex.toast,
    tooltip: tokens.zIndex.tooltip,
  },
};

// 🎨 Vytvoříme base theme
const baseTheme = createTheme(baseThemeOptions);

// 🔧 Component overrides - aplikujeme naše design tokeny
const themeWithComponents = createTheme({
  ...baseTheme,
  components: {
    // 🔘 Button overrides
    MuiButton: {
      styleOverrides: {
        root: {
          borderRadius: tokens.radius.lg,
          fontWeight: tokens.typography.fontWeight.medium,
          textTransform: 'none',
          boxShadow: 'none',
          transition: `all ${tokens.components.animation.normal} ${tokens.components.animation.easing}`,
          
          '&:hover': {
            boxShadow: tokens.shadows.md,
            transform: 'translateY(-1px)',
          },
          
          '&:active': {
            transform: 'translateY(0)',
          },
          
          '&:focus-visible': {
            outline: `${tokens.a11y.focusRing.width} ${tokens.a11y.focusRing.style} ${tokens.a11y.focusRing.color}`,
            outlineOffset: tokens.a11y.focusRing.offset,
          },
        },
        
        sizeLarge: {
          height: tokens.components.button.height.large,
          padding: tokens.components.button.padding.large,
          fontSize: tokens.components.button.fontSize.large,
        },
        
        sizeMedium: {
          height: tokens.components.button.height.medium,
          padding: tokens.components.button.padding.medium,
          fontSize: tokens.components.button.fontSize.medium,
        },
        
        sizeSmall: {
          height: tokens.components.button.height.small,
          padding: tokens.components.button.padding.small,
          fontSize: tokens.components.button.fontSize.small,
        },
        
        containedPrimary: {
          background: tokens.colors.gradients.primary,
          '&:hover': {
            background: tokens.colors.gradients.primaryLight,
            boxShadow: tokens.shadows.glassHover,
          },
        },
      },
    },

    // 📝 TextField overrides
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            borderRadius: tokens.radius.md,
            backgroundColor: tokens.colors.white,
            transition: `all ${tokens.components.animation.normal} ${tokens.components.animation.easing}`,
            
            '&:hover .MuiOutlinedInput-notchedOutline': {
              borderColor: tokens.colors.primary[400],
            },
            
            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
              borderColor: tokens.colors.primary[500],
              borderWidth: '2px',
            },
            
            '&.Mui-error .MuiOutlinedInput-notchedOutline': {
              borderColor: tokens.colors.error[500],
            },
          },

          // Readonly styling
          '& .MuiOutlinedInput-root.Mui-readOnly, & .MuiOutlinedInput-root[readonly]': {
            backgroundColor: tokens.colors.grey[50],
            cursor: 'not-allowed',
            
            '& .MuiOutlinedInput-input': {
              cursor: 'not-allowed',
            },
          },
        },
      },
    },

    // 📊 Card overrides
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: tokens.radius.xl,
          boxShadow: tokens.shadows.md,
          border: `1px solid ${tokens.colors.grey[200]}`,
          transition: `all ${tokens.components.animation.normal} ${tokens.components.animation.easing}`,
          
          '&:hover': {
            boxShadow: tokens.shadows.lg,
            transform: 'translateY(-2px)',
          },
        },
      },
    },

    // 🗂️ Tabs overrides
    MuiTabs: {
      styleOverrides: {
        root: {
          minHeight: 48,
        },
        indicator: {
          height: 3,
          borderRadius: '3px 3px 0 0',
          background: tokens.colors.gradients.primary,
        },
      },
    },

    MuiTab: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          fontWeight: tokens.typography.fontWeight.medium,
          fontSize: tokens.typography.fontSize.base,
          minHeight: 48,
          color: tokens.colors.grey[600],
          transition: `all ${tokens.components.animation.normal} ${tokens.components.animation.easing}`,
          
          '&:hover': {
            color: tokens.colors.primary[600],
            backgroundColor: `${tokens.colors.primary[50]}`,
          },
          
          '&.Mui-selected': {
            color: tokens.colors.primary[600],
            fontWeight: tokens.typography.fontWeight.semibold,
          },
          
          '&:focus-visible': {
            outline: `${tokens.a11y.focusRing.width} ${tokens.a11y.focusRing.style} ${tokens.a11y.focusRing.color}`,
            outlineOffset: tokens.a11y.focusRing.offset,
          },
        },
      },
    },

    // 💬 Tooltip overrides
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          backgroundColor: tokens.colors.grey[800],
          color: tokens.colors.white,
          fontSize: tokens.typography.fontSize.sm,
          borderRadius: tokens.radius.md,
          padding: `${tokens.spacing.sm} ${tokens.spacing.md}`,
          boxShadow: tokens.shadows.xl,
        },
        arrow: {
          color: tokens.colors.grey[800],
        },
      },
      defaultProps: {
        enterDelay: 500,
        leaveDelay: 200,
        arrow: true,
      },
    },

    // 🔔 Alert overrides
    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: tokens.radius.lg,
          fontWeight: tokens.typography.fontWeight.medium,
        },
        filledSuccess: {
          backgroundColor: tokens.colors.success[600],
        },
        filledError: {
          backgroundColor: tokens.colors.error[600],
        },
        filledWarning: {
          backgroundColor: tokens.colors.warning[600],
        },
        filledInfo: {
          backgroundColor: tokens.colors.info[600],
        },
      },
    },

    // 🎯 Chip overrides
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: tokens.radius.md,
          fontWeight: tokens.typography.fontWeight.medium,
        },
      },
    },

    // 📄 Paper overrides pro glassmorphism
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none', // Remove MUI default gradient
        },
      },
    },

    // 🎭 Dialog overrides
    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: tokens.radius.xl,
          boxShadow: tokens.shadows.xxl,
        },
      },
    },

    // 📱 Drawer overrides
    MuiDrawer: {
      styleOverrides: {
        paper: {
          borderRadius: 0, // Keep square for sidebars
          border: 'none',
        },
      },
    },
  },
});

// 🎨 Export finálního theme
export const corePlatformTheme = themeWithComponents;

// Také exportujeme původní theme pro Keycloak kompatibilitu
export const coreMaterialTheme = themeWithComponents;

export const theme = createTheme({
  ...baseTheme,
  palette: {
    ...baseTheme.palette,
    primary: {
      main: tokens.colors.primary.main,
      light: tokens.colors.primary.light,
      dark: tokens.colors.primary.dark,
      contrastText: tokens.colors.primary.contrastText,
    },
    secondary: {
      main: tokens.colors.secondary.main,
      light: tokens.colors.secondary.light,
      dark: tokens.colors.secondary.dark,
      contrastText: tokens.colors.secondary.contrastText,
    },
    error: {
      main: tokens.colors.error.main,
      light: tokens.colors.error.light,
      dark: tokens.colors.error.dark,
      contrastText: tokens.colors.error.contrastText,
    },
    warning: {
      main: tokens.colors.warning.main,
      light: tokens.colors.warning.light,
      dark: tokens.colors.warning.dark,
      contrastText: tokens.colors.warning.contrastText,
    },
    info: {
      main: tokens.colors.info.main,
      light: tokens.colors.info.light,
      dark: tokens.colors.info.dark,
      contrastText: tokens.colors.info.contrastText,
    },
    success: {
      main: tokens.colors.success.main,
      light: tokens.colors.success.light,
      dark: tokens.colors.success.dark,
      contrastText: tokens.colors.success.contrastText,
    },
    background: {
      default: tokens.colors.background.default,
      paper: tokens.colors.background.paper,
    },
    text: {
      primary: tokens.colors.text.primary,
      secondary: tokens.colors.text.secondary,
      disabled: tokens.colors.text.disabled,
    },
  },
  typography: {
    fontFamily: tokens.typography.fontFamily.primary,
    h1: {
      fontSize: tokens.typography.heading.h1.fontSize,
      fontWeight: tokens.typography.heading.h1.fontWeight,
      lineHeight: tokens.typography.heading.h1.lineHeight,
    },
    h2: {
      fontSize: tokens.typography.heading.h2.fontSize,
      fontWeight: tokens.typography.heading.h2.fontWeight,
      lineHeight: tokens.typography.heading.h2.lineHeight,
    },
    h3: {
      fontSize: tokens.typography.heading.h3.fontSize,
      fontWeight: tokens.typography.heading.h3.fontWeight,
      lineHeight: tokens.typography.heading.h3.lineHeight,
    },
    h4: {
      fontSize: tokens.typography.heading.h4.fontSize,
      fontWeight: tokens.typography.heading.h4.fontWeight,
      lineHeight: tokens.typography.heading.h4.lineHeight,
    },
    h5: {
      fontSize: tokens.typography.heading.h5.fontSize,
      fontWeight: tokens.typography.heading.h5.fontWeight,
      lineHeight: tokens.typography.heading.h5.lineHeight,
    },
    h6: {
      fontSize: tokens.typography.heading.h6.fontSize,
      fontWeight: tokens.typography.heading.h6.fontWeight,
      lineHeight: tokens.typography.heading.h6.lineHeight,
    },
    body1: {
      fontSize: tokens.typography.body.large.fontSize,
      fontWeight: tokens.typography.body.large.fontWeight,
      lineHeight: tokens.typography.body.large.lineHeight,
    },
    body2: {
      fontSize: tokens.typography.body.medium.fontSize,
      fontWeight: tokens.typography.body.medium.fontWeight,
      lineHeight: tokens.typography.body.medium.lineHeight,
    },
    caption: {
      fontSize: tokens.typography.body.small.fontSize,
      fontWeight: tokens.typography.body.small.fontWeight,
      lineHeight: tokens.typography.body.small.lineHeight,
    },
  },
  spacing: 8,
  shape: {
    borderRadius: parseInt(tokens.borderRadius.medium, 10), // Convert string to number
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: tokens.borderRadius.small,
          fontWeight: tokens.typography.fontWeight.medium,
          transition: tokens.animation.transition.all,
          '&:hover': {
            transform: tokens.animation.transform.scaleHover,
          },
        },
        containedPrimary: {
          backgroundColor: tokens.colors.primary.main,
          color: tokens.colors.primary.contrastText,
          boxShadow: tokens.elevation.medium,
          '&:hover': {
            backgroundColor: tokens.colors.primary.dark,
            boxShadow: tokens.elevation.high,
          },
        },
        outlinedPrimary: {
          borderColor: tokens.colors.primary.main,
          color: tokens.colors.primary.main,
          '&:hover': {
            borderColor: tokens.colors.primary.dark,
            backgroundColor: `${tokens.colors.primary.main}08`,
          },
        },
        textPrimary: {
          color: tokens.colors.primary.main,
          '&:hover': {
            backgroundColor: `${tokens.colors.primary.main}08`,
          },
        },
      },
    },
    MuiCard: {
      styleOverrides: {
        root: {
          borderRadius: tokens.borderRadius.medium,
          boxShadow: tokens.elevation.low,
          border: `1px solid ${tokens.colors.border.light}`,
          backgroundColor: tokens.colors.background.paper,
          transition: tokens.animation.transition.all,
          '&:hover': {
            boxShadow: tokens.elevation.medium,
            transform: tokens.animation.transform.translateY,
          },
        },
      },
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            borderRadius: tokens.borderRadius.small,
            transition: tokens.animation.transition.all,
            '&:hover .MuiOutlinedInput-notchedOutline': {
              borderColor: tokens.colors.primary.light,
            },
            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
              borderColor: tokens.colors.primary.main,
              borderWidth: '2px',
            },
          },
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundColor: tokens.colors.background.paper,
          color: tokens.colors.text.primary,
          boxShadow: tokens.elevation.low,
          borderBottom: `1px solid ${tokens.colors.border.light}`,
          transition: tokens.animation.transition.all,
        },
      },
    },
  },
} as const);

export default corePlatformTheme;