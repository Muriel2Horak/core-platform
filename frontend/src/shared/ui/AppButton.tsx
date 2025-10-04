/**
 * 🔘 AppButton - Core Platform Button Component
 * 
 * Wrapper kolem MUI Button s našimi design variantami.
 * Podporuje primary, secondary, danger stavy s loading funkcionalitou.
 */

import React from 'react';
import { Button, ButtonProps, CircularProgress, Box } from '@mui/material';
import { tokens } from '../theme/tokens';

export interface AppButtonProps extends Omit<ButtonProps, 'variant' | 'color'> {
  /** Button variant - ovlivňuje barvy a styl */
  variant?: 'primary' | 'secondary' | 'danger' | 'outlined' | 'text';
  /** Loading state - zobrazí spinner a disabluje button */
  loading?: boolean;
  /** Button size */
  size?: 'small' | 'medium' | 'large';
  /** Full width button */
  fullWidth?: boolean;
  /** Children content */
  children: React.ReactNode;
}

/**
 * AppButton komponent s Core Platform design systémem
 */
export const AppButton: React.FC<AppButtonProps> = ({
  variant = 'primary',
  loading = false,
  size = 'medium',
  disabled,
  startIcon,
  endIcon,
  children,
  sx,
  ...props
}) => {
  // 🎨 Variant styling
  const getVariantStyles = () => {
    const baseStyles = {
      borderRadius: tokens.radius.lg,
      fontWeight: tokens.typography.fontWeight.medium,
      textTransform: 'none' as const,
      transition: `all ${tokens.components.animation.normal} ${tokens.components.animation.easing}`,
      minHeight: tokens.a11y.touchTarget.minSize, // WCAG AAA
      
      '&:focus-visible': {
        outline: `${tokens.a11y.focusRing.width} ${tokens.a11y.focusRing.style} ${tokens.a11y.focusRing.color}`,
        outlineOffset: tokens.a11y.focusRing.offset,
      },
    };

    switch (variant) {
      case 'primary':
        return {
          ...baseStyles,
          background: tokens.colors.gradients.primary,
          color: tokens.colors.white,
          border: 'none',
          boxShadow: tokens.shadows.sm,
          
          '&:hover': {
            background: tokens.colors.gradients.primaryLight,
            boxShadow: tokens.shadows.md,
            transform: 'translateY(-1px)',
          },
          
          '&:active': {
            transform: 'translateY(0)',
            boxShadow: tokens.shadows.sm,
          },
        };

      case 'secondary':
        return {
          ...baseStyles,
          backgroundColor: tokens.colors.grey[100],
          color: tokens.colors.grey[700],
          border: `1px solid ${tokens.colors.grey[300]}`,
          
          '&:hover': {
            backgroundColor: tokens.colors.grey[200],
            borderColor: tokens.colors.grey[400],
            transform: 'translateY(-1px)',
            boxShadow: tokens.shadows.sm,
          },
        };

      case 'danger':
        return {
          ...baseStyles,
          backgroundColor: tokens.colors.error[500],
          color: tokens.colors.white,
          border: 'none',
          
          '&:hover': {
            backgroundColor: tokens.colors.error[600],
            transform: 'translateY(-1px)',
            boxShadow: tokens.shadows.md,
          },
        };

      case 'outlined':
        return {
          ...baseStyles,
          backgroundColor: 'transparent',
          color: tokens.colors.primary[600],
          border: `2px solid ${tokens.colors.primary[500]}`,
          
          '&:hover': {
            backgroundColor: tokens.colors.primary[50],
            borderColor: tokens.colors.primary[600],
            transform: 'translateY(-1px)',
          },
        };

      case 'text':
        return {
          ...baseStyles,
          backgroundColor: 'transparent',
          color: tokens.colors.primary[600],
          border: 'none',
          
          '&:hover': {
            backgroundColor: tokens.colors.primary[50],
          },
        };

      default:
        return baseStyles;
    }
  };

  // 📏 Size styles
  const getSizeStyles = () => {
    switch (size) {
      case 'small':
        return {
          height: tokens.components.button.height.small,
          padding: tokens.components.button.padding.small,
          fontSize: tokens.components.button.fontSize.small,
        };
      case 'large':
        return {
          height: tokens.components.button.height.large,
          padding: tokens.components.button.padding.large,
          fontSize: tokens.components.button.fontSize.large,
        };
      default: // medium
        return {
          height: tokens.components.button.height.medium,
          padding: tokens.components.button.padding.medium,
          fontSize: tokens.components.button.fontSize.medium,
        };
    }
  };

  // 🔄 Loading spinner
  const LoadingSpinner = () => (
    <CircularProgress
      size={size === 'small' ? 16 : size === 'large' ? 24 : 20}
      sx={{
        color: 'inherit',
        marginRight: children ? 1 : 0,
      }}
    />
  );

  return (
    <Button
      disabled={disabled || loading}
      startIcon={loading ? <LoadingSpinner /> : startIcon}
      endIcon={!loading ? endIcon : undefined}
      sx={{
        ...getVariantStyles(),
        ...getSizeStyles(),
        // Disabled state
        '&.Mui-disabled': {
          opacity: 0.6,
          cursor: 'not-allowed',
          transform: 'none',
          boxShadow: 'none',
        },
        ...sx,
      }}
      {...props}
    >
      {loading ? (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {typeof children === 'string' && children.includes('...') 
            ? children 
            : children
          }
        </Box>
      ) : (
        children
      )}
    </Button>
  );
};

export default AppButton;