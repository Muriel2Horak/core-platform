/**
 * 🎨 Core Platform Design Tokens
 * 
 * Centrální definice všech design hodnot pro konzistentní UI.
 * WCAG AA compliant barvy s minimálním kontrastem 4.5:1.
 */

// 🎨 COLOR PALETTE
export const colors = {
  // Primary palette
  primary: {
    50: '#f0f4ff',
    100: '#e0e7ff', 
    200: '#c7d2fe',
    300: '#a5b4fc',
    400: '#8b95f8',
    500: '#667eea', // Main primary color
    600: '#4d68d1',
    700: '#4338ca',
    800: '#3730a3',
    900: '#312e81',
    
    // Theme compatibility - aliases for MUI theme
    main: '#667eea',
    light: '#8b95f8',
    dark: '#4d68d1',
    contrastText: '#ffffff',
  },

  // Secondary palette  
  secondary: {
    50: '#faf5ff',
    100: '#f3e8ff',
    200: '#e9d5ff', 
    300: '#d8b4fe',
    400: '#c084fc',
    500: '#764ba2', // Main secondary color
    600: '#5d3a82',
    700: '#7c3aed',
    800: '#6b21a8',
    900: '#581c87',
    
    // Theme compatibility
    main: '#764ba2',
    light: '#c084fc',
    dark: '#5d3a82',
    contrastText: '#ffffff',
  },

  // Neutral greys
  grey: {
    50: '#fafafa',
    100: '#f5f5f5',
    200: '#eeeeee',
    300: '#e0e0e0',
    400: '#bdbdbd',
    500: '#9e9e9e', 
    600: '#757575',
    700: '#616161',
    800: '#424242',
    900: '#212121',
  },

  // Semantic colors
  success: {
    50: '#f0f9f0',
    100: '#dcf4dc',
    200: '#bae7ba',
    500: '#4caf50', // Main success
    600: '#43a047',
    700: '#388e3c',
    900: '#1b5e20',
    
    // Theme compatibility
    main: '#4caf50',
    light: '#bae7ba',
    dark: '#388e3c',
    contrastText: '#ffffff',
  },

  warning: {
    50: '#fff8e1',
    100: '#ffecb3',
    200: '#ffe082',
    500: '#ff9800', // Main warning
    600: '#fb8c00',
    700: '#f57c00',
    900: '#e65100',
    
    // Theme compatibility
    main: '#ff9800',
    light: '#ffe082',
    dark: '#f57c00',
    contrastText: '#ffffff',
  },

  error: {
    50: '#ffebee',
    100: '#ffcdd2',
    200: '#ef9a9a',
    500: '#f44336', // Main error
    600: '#e53935',
    700: '#d32f2f',
    900: '#b71c1c',
    
    // Theme compatibility
    main: '#f44336',
    light: '#ef9a9a',
    dark: '#d32f2f',
    contrastText: '#ffffff',
  },

  info: {
    50: '#e3f2fd',
    100: '#bbdefb',
    200: '#90caf9',
    500: '#2196f3', // Main info
    600: '#1e88e5',
    700: '#1976d2',
    900: '#0d47a1',
    
    // Theme compatibility
    main: '#2196f3',
    light: '#90caf9',
    dark: '#1976d2',
    contrastText: '#ffffff',
  },

  // Background colors for theme compatibility
  background: {
    default: '#fafafa', // grey[50]
    paper: '#ffffff',
    neutral: '#f5f5f5', // grey[100]
  },

  // Text colors for theme compatibility
  text: {
    primary: '#212121', // grey[900]
    secondary: '#757575', // grey[600]
    disabled: '#bdbdbd', // grey[400]
  },

  // Border colors
  border: {
    light: '#eeeeee', // grey[200]
    medium: '#e0e0e0', // grey[300]
    dark: '#bdbdbd', // grey[400]
  },

  // Special colors
  white: '#ffffff',
  black: '#000000',
  
  // Glassmorphism gradients
  gradients: {
    primary: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
    primaryLight: 'linear-gradient(135deg, #8fa3f3 0%, #9575b8 100%)',
    glass: 'linear-gradient(135deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0.05) 100%)',
    glassCard: 'linear-gradient(135deg, rgba(255,255,255,0.7) 0%, rgba(255,255,255,0.3) 100%)',
  },

  // 🗂️ SIDEBAR COLORS - Čistý profesionální design
  sidebar: {
    // Čistý tmavě šedý gradient - profesionální a časeless
    bg: 'linear-gradient(180deg, #374151 0%, #1f2937 100%)', // Tmavě šedá → velmi tmavě šedá
    bgSolid: '#374151', // Fallback tmavě šedá
    
    // Text colors - maximální kontrast pro dokonalou čitelnost
    text: '#ffffff', // Čistě bílý text pro maximální kontrast
    textMuted: 'rgba(255, 255, 255, 0.80)', // 80% opacity pro ikony - stále dobře viditelné
    
    // Interactive states - jemné ale viditelné
    activeBg: 'rgba(255, 255, 255, 0.12)', // Jemný bílý overlay pro aktivní položky
    hoverBg: 'rgba(255, 255, 255, 0.08)',  // Jemný hover efekt
    
    // Active accent - moderní modrá pro accent
    activePill: '#3b82f6', // Čistě modrá pro aktivní indikátor (místo fialové)
  },

  // 🎯 FOCUS RING - Enhanced for better visibility
  focusRing: '#6aa9ff', // Brighter blue for better focus visibility
} as const;

// 📏 SPACING SCALE (8px base)
export const spacing = {
  xs: '4px',   // 0.25rem
  sm: '8px',   // 0.5rem  
  md: '16px',  // 1rem
  lg: '24px',  // 1.5rem
  xl: '32px',  // 2rem
  xxl: '48px', // 3rem
  xxxl: '64px', // 4rem
} as const;

// 📐 BORDER RADIUS
export const radius = {
  none: '0px',
  sm: '4px',
  md: '8px',   // Standard radius
  lg: '12px',  // Cards, buttons
  xl: '16px',  // Modal dialogs
  xxl: '24px', // Hero sections
  round: '50%', // Circular elements
} as const;

// 📐 BORDER RADIUS - Theme compatibility aliases
export const borderRadius = {
  small: '4px',
  medium: '8px',
  large: '12px',
  ...radius,
} as const;

// 🌟 SHADOWS
export const shadows = {
  none: 'none',
  sm: '0 1px 2px 0 rgba(0, 0, 0, 0.05)',
  md: '0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06)',
  lg: '0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05)',
  xl: '0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04)',
  xxl: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
  
  // Special glassmorphism shadows
  glass: '0 8px 32px rgba(0, 0, 0, 0.12), 0 0 0 1px rgba(255, 255, 255, 0.05) inset',
  glassHover: '0 20px 40px rgba(102, 126, 234, 0.3), 0 0 0 1px rgba(255, 255, 255, 0.1) inset',
} as const;

// 🌟 ELEVATION - Theme compatibility aliases
export const elevation = {
  none: shadows.none,
  low: shadows.sm,
  medium: shadows.md,
  high: shadows.lg,
  highest: shadows.xl,
} as const;

// ✍️ TYPOGRAPHY
export const typography = {
  fontFamily: {
    primary: '"Plus Jakarta Sans", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
    mono: '"Fira Code", "Monaco", "Cascadia Code", "Segoe UI Mono", "Roboto Mono", monospace',
  },
  
  fontSize: {
    xs: '12px',   // 0.75rem
    sm: '14px',   // 0.875rem  
    base: '16px', // 1rem
    lg: '18px',   // 1.125rem
    xl: '20px',   // 1.25rem
    '2xl': '24px', // 1.5rem
    '3xl': '30px', // 1.875rem
    '4xl': '36px', // 2.25rem
    '5xl': '48px', // 3rem
  },
  
  fontWeight: {
    light: 300,
    normal: 400,
    medium: 500,
    semibold: 600,
    bold: 700,
  },
  
  lineHeight: {
    tight: 1.2,
    snug: 1.3,
    normal: 1.4,
    relaxed: 1.5,
    loose: 1.6,
  },

  // Theme compatibility - heading definitions
  heading: {
    h1: {
      fontSize: '48px', // 5xl
      fontWeight: 700,  // bold
      lineHeight: 1.2,  // tight
    },
    h2: {
      fontSize: '36px', // 4xl
      fontWeight: 600,  // semibold
      lineHeight: 1.3,  // snug
    },
    h3: {
      fontSize: '30px', // 3xl
      fontWeight: 600,  // semibold
      lineHeight: 1.4,  // normal
    },
    h4: {
      fontSize: '24px', // 2xl
      fontWeight: 600,  // semibold
      lineHeight: 1.4,  // normal
    },
    h5: {
      fontSize: '20px', // xl
      fontWeight: 600,  // semibold
      lineHeight: 1.5,  // relaxed
    },
    h6: {
      fontSize: '16px', // base
      fontWeight: 600,  // semibold
      lineHeight: 1.5,  // relaxed
    },
  },

  // Theme compatibility - body text definitions
  body: {
    large: {
      fontSize: '16px', // base
      fontWeight: 400,  // normal
      lineHeight: 1.6,  // loose
    },
    medium: {
      fontSize: '14px', // sm
      fontWeight: 400,  // normal
      lineHeight: 1.6,  // loose
    },
    small: {
      fontSize: '12px', // xs
      fontWeight: 400,  // normal
      lineHeight: 1.4,  // normal
    },
  },
} as const;

// 📱 BREAKPOINTS
export const breakpoints = {
  xs: '0px',
  sm: '600px',
  md: '900px', 
  lg: '1200px',
  xl: '1536px',
} as const;

// 🎭 ANIMATION
export const animation = {
  transition: {
    all: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
    fast: 'all 0.15s cubic-bezier(0.4, 0, 0.2, 1)',
    slow: 'all 0.35s cubic-bezier(0.4, 0, 0.2, 1)',
  },
  transform: {
    scaleHover: 'scale(1.02)',
    translateY: 'translateY(-2px)',
  },
  duration: {
    fast: '150ms',
    normal: '250ms',
    slow: '350ms',
  },
  easing: 'cubic-bezier(0.4, 0, 0.2, 1)',
  easingBounce: 'cubic-bezier(0.68, -0.55, 0.265, 1.55)',
} as const;

// 🎛️ COMPONENT TOKENS
export const components = {
  // Button specifications
  button: {
    height: {
      small: '32px',
      medium: '40px',
      large: '48px',
    },
    padding: {
      small: '6px 12px',
      medium: '8px 16px', 
      large: '12px 24px',
    },
    fontSize: {
      small: typography.fontSize.sm,
      medium: typography.fontSize.base,
      large: typography.fontSize.lg,
    }
  },
  
  // Form field specifications
  formField: {
    height: '56px', // Material Design 3 standard
    borderRadius: radius.md,
    fontSize: typography.fontSize.base,
  },
  
  // Layout specifications
  layout: {
    sidebarWidth: '264px',      // Desktop expanded
    sidebarCollapsed: '72px',   // Collapsed/mobile
    
    // Header
    headerHeight: '64px',
    
    // Content containers
    containerMaxWidth: '1200px',   // PageContainer xl
    containerLgWidth: '1024px',    // PageContainer lg  
    container2xlWidth: '1400px',   // PageContainer 2xl
    contentMaxWidth: '800px',      // Narrow content (forms, etc.)
    
    // Breakpoints for layout switching
    breakpoints: {
      sidebarCollapse: '1280px',  // Below this = collapsed sidebar
      fullWidth: '1440px',       // Above this = full WorkArea
      mobile: '1024px',          // Below this = mobile adjustments
    },
  },
  
  // Sidebar specific tokens
  sidebar: {
    width: {
      expanded: '264px',
      collapsed: '72px',
    },
    itemHeight: '44px',        // WCAG AAA touch target
    itemPadding: '10px 16px',  // Vertical and horizontal padding
    itemBorderRadius: '12px',  // Modern rounded corners
    iconSize: '20px',          // Icon dimensions
    fontSize: '0.95rem',       // Label text size
    fontWeight: 600,           // Semi-bold for readability
    gap: '4px',               // Spacing between items
  },

  // WorkArea tokens
  workArea: {
    gap: '24px',              // Default gap between sections
    sectionPadding: '24px',   // Padding inside work sections
    fullBleedGutter: '0px',   // No gutter for full-bleed content
  },

  // 🎭 Animations & Transitions - moved here to match usage in Layout.jsx
  animation: {
    /** Rychlá animace - hover efekty */
    fast: '150ms',
    /** Normální animace - modaly, dropdowny */
    normal: '250ms',
    /** Pomalá animace - layoutové změny */
    slow: '350ms',
    /** Easing funkce pro smooth přechody */
    easing: 'cubic-bezier(0.4, 0, 0.2, 1)',
    /** Bounce easing pro playful efekty */
    easingBounce: 'cubic-bezier(0.68, -0.55, 0.265, 1.55)',
  },
} as const;

// 🎯 Z-INDEX SCALE
export const zIndex = {
  hide: -1,
  auto: 'auto',
  base: 0,
  docked: 10,
  dropdown: 1000,
  sticky: 1100,
  banner: 1200, 
  overlay: 1300,
  modal: 1400,
  popover: 1500,
  skipLink: 1600,
  toast: 1700,
  tooltip: 1800,
} as const;

// 🔍 ACCESSIBILITY
export const a11y = {
  // Minimum contrast ratios (WCAG AA)
  contrast: {
    normal: 4.5, // 4.5:1 for normal text
    large: 3.0,  // 3:1 for large text (18pt+)
  },
  
  // Focus ring specifications
  focusRing: {
    width: '2px',
    color: colors.focusRing,  // Use new focus ring color
    offset: '2px',
    style: 'solid',
  },
  
  // Touch target minimums
  touchTarget: {
    minSize: '44px', // WCAG AAA recommendation
  },

  // Sidebar accessibility
  sidebar: {
    minTextContrast: 4.5,     // WCAG AA requirement
    minIconContrast: 3.0,     // WCAG AA for large elements
    focusRingColor: colors.focusRing,
  },
} as const;

// 🔗 EXPORT ALL TOKENS
export const tokens = {
  colors,
  spacing,
  radius,
  borderRadius,
  shadows,
  elevation,
  typography,
  breakpoints,
  animation,
  components,
  zIndex,
  a11y,
} as const;

export default tokens;