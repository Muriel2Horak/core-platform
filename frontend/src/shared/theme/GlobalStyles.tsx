/**
 * 🎨 Core Platform Global Styles  
 * 
 * CSS reset, focus ring a globální styly pro konzistentní vzhled.
 * Obsahuje WCAG AA compliant focus management.
 */

import { GlobalStyles as MuiGlobalStyles } from '@mui/material';
import { tokens } from './tokens';

export const GlobalStyles = () => (
  <MuiGlobalStyles
    styles={{
      // 🔄 CSS Reset
      '*': {
        boxSizing: 'border-box',
      },
      
      '*, *::before, *::after': {
        boxSizing: 'inherit',
      },

      // 📱 HTML & Body reset
      html: {
        height: '100%',
        fontSize: '16px', // Base font size
        lineHeight: 1.6,
        fontFamily: tokens.typography.fontFamily.primary,
        WebkitFontSmoothing: 'antialiased',
        MozOsxFontSmoothing: 'grayscale',
        WebkitTextSizeAdjust: '100%',
      },

      body: {
        height: '100%',
        margin: 0,
        padding: 0,
        backgroundColor: tokens.colors.grey[50],
        color: tokens.colors.grey[900],
        fontFamily: tokens.typography.fontFamily.primary,
        fontSize: tokens.typography.fontSize.base,
        lineHeight: tokens.typography.lineHeight.loose,
        
        // Smooth scrolling
        scrollBehavior: 'smooth',
        
        // Better text rendering
        textRendering: 'optimizeLegibility',
        fontFeatureSettings: '"kern" 1',
        fontKerning: 'normal',
      },

      '#root': {
        height: '100%',
        isolation: 'isolate', // Create new stacking context
      },

      // 🔍 Focus Ring - WCAG AA Compliant
      // NIKDY neodstraňovat focus ring!
      '*:focus': {
        // eslint-disable-next-line no-restricted-syntax
        outline: 'none', // Remove default outline - replaced by focus-visible
      },

      '*:focus-visible': {
        outline: `${tokens.a11y.focusRing.width} ${tokens.a11y.focusRing.style} ${tokens.a11y.focusRing.color}`,
        outlineOffset: tokens.a11y.focusRing.offset,
        borderRadius: tokens.radius.sm,
      },

      // Focus ring pro interaktivní prvky
      'button:focus-visible, a:focus-visible, input:focus-visible, textarea:focus-visible, select:focus-visible, [tabindex]:focus-visible': {
        outline: `${tokens.a11y.focusRing.width} ${tokens.a11y.focusRing.style} ${tokens.a11y.focusRing.color}`,
        outlineOffset: tokens.a11y.focusRing.offset,
      },

      // 🔗 Link styles  
      a: {
        color: tokens.colors.primary[600],
        textDecoration: 'none',
        transition: `color ${tokens.components.animation.fast} ${tokens.components.animation.easing}`,
        
        '&:hover': {
          color: tokens.colors.primary[700],
          textDecoration: 'underline',
          backgroundColor: `${tokens.colors.primary[500]}08`,
          transform: 'scale(1.02)',
        },
        
        '&:visited': {
          color: tokens.colors.secondary[600],
        },
      },

      // 📝 Form elements reset
      'input, textarea, select, button': {
        font: 'inherit',
        color: 'inherit',
      },

      'input[type="search"]': {
        WebkitAppearance: 'textfield',
        
        '&::-webkit-search-decoration': {
          WebkitAppearance: 'none',
        },
      },

      // 🔘 Button reset
      button: {
        border: 'none',
        background: 'none',
        cursor: 'pointer',
        padding: 0,
        font: 'inherit',

        '&:disabled': {
          cursor: 'not-allowed',
          opacity: 0.6,
        },

        '&:focus': {
          outline: `2px solid ${tokens.colors.primary[200]}`,
          outlineOffset: '2px',
          transform: 'scale(1.02)',
          transition: `all ${tokens.components.animation.fast} ${tokens.components.animation.easing}`,
        },
        '&:active': {
          transform: 'scale(0.98)',
          transition: `all ${tokens.components.animation.fast} ${tokens.components.animation.easing}`,
        },
      },

      // 📋 List reset
      'ul, ol': {
        listStyle: 'none',
        margin: 0,
        padding: 0,
      },

      // 🖼️ Image optimizations
      img: {
        maxWidth: '100%',
        height: 'auto',
        display: 'block',
      },

      // 📄 Typography improvements
      'h1, h2, h3, h4, h5, h6': {
        margin: 0,
        fontWeight: tokens.typography.fontWeight.semibold,
        lineHeight: tokens.typography.lineHeight.tight,
      },

      p: {
        margin: 0,
        marginBottom: tokens.spacing.md,
        
        '&:last-child': {
          marginBottom: 0,
        },
      },

      // 📊 Table improvements
      table: {
        borderCollapse: 'collapse',
        borderSpacing: 0,
        width: '100%',
      },

      // 🎯 Selection styling
      '::selection': {
        backgroundColor: tokens.colors.primary[200],
        color: tokens.colors.primary[800],
      },

      '::-moz-selection': {
        backgroundColor: tokens.colors.primary[200],
        color: tokens.colors.primary[800],
      },

      // 📱 Scrollbar styling (Webkit browsers)
      '::-webkit-scrollbar': {
        width: '8px',
        height: '8px',
      },

      '::-webkit-scrollbar-track': {
        backgroundColor: tokens.colors.grey[100],
        borderRadius: tokens.radius.sm,
      },

      '::-webkit-scrollbar-thumb': {
        backgroundColor: tokens.colors.grey[300],
        borderRadius: tokens.radius.sm,
        
        '&:hover': {
          backgroundColor: tokens.colors.grey[400],
        },
      },

      // 🔍 High contrast mode support
      '@media (prefers-contrast: high)': {
        '*:focus-visible': {
          outline: `3px solid ButtonText`,
          outlineOffset: '2px',
        },
      },

      // 🌙 Prefers reduced motion
      '@media (prefers-reduced-motion: reduce)': {
        '*, *::before, *::after': {
          animationDuration: '0.01ms !important',
          animationIterationCount: '1 !important',
          transitionDuration: '0.01ms !important',
          scrollBehavior: 'auto !important',
        },
      },

      // 📱 Touch device optimizations
      '@media (pointer: coarse)': {
        // Increase tap targets on touch devices
        'button, input, select, textarea, a[href]': {
          minHeight: tokens.a11y.touchTarget.minSize,
          minWidth: tokens.a11y.touchTarget.minSize,
        },
      },

      // 🌐 Print styles
      '@media print': {
        '*': {
          background: 'transparent !important',
          color: 'black !important',
          boxShadow: 'none !important',
          textShadow: 'none !important',
        },
        
        'a, a:visited': {
          textDecoration: 'underline',
        },
        
        'a[href]:after': {
          content: `" (" attr(href) ")"`,
        },
        
        'abbr[title]:after': {
          content: `" (" attr(title) ")"`,
        },
        
        'h2, h3': {
          pageBreakAfter: 'avoid',
        },
        
        'p, h2, h3': {
          orphans: 3,
          widows: 3,
        },
      },

      // 🎨 Utility classes
      '.sr-only': {
        position: 'absolute',
        width: '1px',
        height: '1px',
        padding: 0,
        margin: '-1px',
        overflow: 'hidden',
        clip: 'rect(0, 0, 0, 0)',
        whiteSpace: 'nowrap',
        border: 0,
      },

      '.skip-link': {
        position: 'absolute',
        top: '-40px',
        left: '6px',
        zIndex: tokens.zIndex.skipLink,
        color: tokens.colors.white,
        backgroundColor: tokens.colors.primary[600],
        padding: `${tokens.spacing.sm} ${tokens.spacing.md}`,
        borderRadius: tokens.radius.md,
        textDecoration: 'none',
        fontSize: tokens.typography.fontSize.sm,
        fontWeight: tokens.typography.fontWeight.medium,
        transition: `top ${tokens.components.animation.normal} ${tokens.components.animation.easing}`,
        
        '&:focus': {
          top: '6px',
        },
      },

      // 🔄 Loading animation keyframes
      '@keyframes spin': {
        '0%': {
          transform: 'rotate(0deg)',
        },
        '100%': {
          transform: 'rotate(360deg)',
        },
      },

      '@keyframes pulse': {
        '0%, 100%': {
          opacity: 1,
        },
        '50%': {
          opacity: 0.5,
        },
      },

      '@keyframes fadeIn': {
        '0%': {
          opacity: 0,
        },
        '100%': {
          opacity: 1,
        },
      },

      '@keyframes slideUp': {
        '0%': {
          transform: 'translateY(10px)',
          opacity: 0,
        },
        '100%': {
          transform: 'translateY(0)',
          opacity: 1,
        },
      },

      // 🎯 Animation utilities
      '.animate-spin': {
        animation: 'spin 1s linear infinite',
      },

      '.animate-pulse': {
        animation: 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },

      '.animate-fadeIn': {
        animation: `fadeIn ${tokens.components.animation.slow} ${tokens.components.animation.easing}`,
      },

      '.animate-slideUp': {
        animation: `slideUp ${tokens.components.animation.slow} ${tokens.components.animation.easing}`,
      },
    }}
  />
);

export default GlobalStyles;