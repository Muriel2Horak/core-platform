/**
 * 🔍 EmptyState - Core Platform Empty State Component
 * 
 * Komponenta pro zobrazení prázdných stavů s ikonou, textem a volitelnou akcí.
 * Používá se když nejsou data k zobrazení nebo když nastane chyba.
 */

import React from 'react';
import { Box, Typography, useTheme, useMediaQuery, SxProps, Theme } from '@mui/material';
import { tokens } from '../theme/tokens';

export interface EmptyStateProps {
  /** Ikona pro zobrazení */
  icon?: React.ReactNode;
  /** Hlavní text */
  title: string;
  /** Popisný text */
  description?: string;
  /** Akční tlačítko nebo element */
  action?: React.ReactNode;
  /** Velikost komponenty */
  size?: 'small' | 'medium' | 'large';
  /** Custom styling */
  sx?: SxProps<Theme>;
}

/**
 * EmptyState komponent s Core Platform design systémem
 */
export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  action,
  size = 'medium',
  sx,
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  // 📏 Size configurations
  const sizeConfig = {
    small: {
      iconSize: isMobile ? 48 : 56,
      titleSize: tokens.typography.fontSize.lg,
      descriptionSize: tokens.typography.fontSize.sm,
      spacing: tokens.spacing.lg,
      maxWidth: '320px',
    },
    medium: {
      iconSize: isMobile ? 64 : 80,
      titleSize: tokens.typography.fontSize.xl,
      descriptionSize: tokens.typography.fontSize.base,
      spacing: tokens.spacing.xl,
      maxWidth: '480px',
    },
    large: {
      iconSize: isMobile ? 80 : 120,
      titleSize: tokens.typography.fontSize['2xl'],
      descriptionSize: tokens.typography.fontSize.lg,
      spacing: tokens.spacing.xxl,
      maxWidth: '640px',
    },
  };

  const config = sizeConfig[size];

  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        padding: config.spacing,
        maxWidth: config.maxWidth,
        margin: '0 auto',
        minHeight: size === 'large' ? '400px' : size === 'medium' ? '300px' : '200px',
        ...sx,
      }}
      role="status"
      aria-live="polite"
    >
      {/* Icon */}
      {icon && (
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            width: config.iconSize,
            height: config.iconSize,
            mb: 3,
            color: tokens.colors.grey[400],
            
            // Icon styling
            '& > *': {
              fontSize: `${config.iconSize}px !important`,
              width: config.iconSize,
              height: config.iconSize,
            },
            
            // SVG styling
            '& svg': {
              width: '100%',
              height: '100%',
              opacity: 0.6,
            },
          }}
        >
          {icon}
        </Box>
      )}

      {/* Title */}
      <Typography
        variant="h3"
        component="h2"
        sx={{
          fontSize: config.titleSize,
          fontWeight: tokens.typography.fontWeight.semibold,
          color: tokens.colors.grey[700],
          mb: description ? 2 : action ? 3 : 0,
          lineHeight: tokens.typography.lineHeight.snug,
        }}
      >
        {title}
      </Typography>

      {/* Description */}
      {description && (
        <Typography
          variant="body1"
          sx={{
            fontSize: config.descriptionSize,
            color: tokens.colors.grey[500],
            lineHeight: tokens.typography.lineHeight.relaxed,
            mb: action ? 3 : 0,
            maxWidth: '100%',
            wordBreak: 'break-word',
          }}
        >
          {description}
        </Typography>
      )}

      {/* Action */}
      {action && (
        <Box
          sx={{
            mt: 1,
            display: 'flex',
            flexDirection: isMobile ? 'column' : 'row',
            gap: 2,
            alignItems: 'center',
            justifyContent: 'center',
            width: isMobile ? '100%' : 'auto',
          }}
        >
          {action}
        </Box>
      )}
    </Box>
  );
};

export default EmptyState;