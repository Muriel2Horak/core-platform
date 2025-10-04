/**
 * ⏳ Loader - Core Platform Loading Component
 * 
 * Komponenta pro zobrazení loading stavů - page, inline, skeleton.
 * Podporuje různé velikosti a typy loading indikátorů.
 */

import React from 'react';
import { Box, CircularProgress, Skeleton, Typography, SxProps, Theme } from '@mui/material';
import { tokens } from '../theme/tokens';

export interface LoaderProps {
  /** Typ loading indikátoru */
  variant?: 'page' | 'inline' | 'skeleton' | 'spinner';
  /** Velikost loaderu */
  size?: 'small' | 'medium' | 'large';
  /** Loading text */
  text?: string;
  /** Počet skeleton řádků (pro skeleton variant) */
  rows?: number;
  /** Výška skeleton řádků */
  height?: number | string;
  /** Custom styling */
  sx?: SxProps<Theme>;
}

/**
 * Loader komponent s Core Platform design systémem
 */
export const Loader: React.FC<LoaderProps> = ({
  variant = 'spinner',
  size = 'medium',
  text,
  rows = 3,
  height = 20,
  sx,
}) => {
  // 📏 Size configurations
  const sizeConfig = {
    small: {
      spinner: 20,
      text: tokens.typography.fontSize.sm,
      spacing: tokens.spacing.sm,
    },
    medium: {
      spinner: 40,
      text: tokens.typography.fontSize.base,
      spacing: tokens.spacing.md,
    },
    large: {
      spinner: 60,
      text: tokens.typography.fontSize.lg,
      spacing: tokens.spacing.lg,
    },
  };

  const config = sizeConfig[size];

  // 📄 Page loader - full screen loading
  if (variant === 'page') {
    return (
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '60vh',
          textAlign: 'center',
          padding: tokens.spacing.xxl,
          ...sx,
        }}
        role="status"
        aria-live="polite"
        aria-label={text || 'Načítá se...'}
      >
        <CircularProgress
          size={config.spinner}
          thickness={4}
          sx={{
            color: tokens.colors.primary[500],
            mb: text ? 3 : 0,
            // Smooth animation
            animation: 'spin 1s linear infinite',
          }}
        />
        
        {text && (
          <Typography
            variant="body1"
            sx={{
              fontSize: config.text,
              color: tokens.colors.grey[600],
              fontWeight: tokens.typography.fontWeight.medium,
            }}
          >
            {text}
          </Typography>
        )}
      </Box>
    );
  }

  // 🔄 Inline loader - malý spinner pro inline použití
  if (variant === 'inline') {
    return (
      <Box
        sx={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: config.spacing,
          ...sx,
        }}
        role="status"
        aria-live="polite"
        aria-label={text || 'Načítá se...'}
      >
        <CircularProgress
          size={config.spinner}
          thickness={4}
          sx={{
            color: tokens.colors.primary[500],
          }}
        />
        
        {text && (
          <Typography
            variant="body2"
            sx={{
              fontSize: config.text,
              color: tokens.colors.grey[600],
            }}
          >
            {text}
          </Typography>
        )}
      </Box>
    );
  }

  // 🦴 Skeleton loader - pro content placeholders
  if (variant === 'skeleton') {
    return (
      <Box
        sx={{
          width: '100%',
          ...sx,
        }}
        role="status"
        aria-live="polite"
        aria-label="Načítá se obsah..."
      >
        {Array.from({ length: rows }).map((_, index) => (
          <Skeleton
            key={index}
            variant="rectangular"
            height={height}
            sx={{
              mb: 1,
              borderRadius: tokens.radius.sm,
              backgroundColor: tokens.colors.grey[100],
              
              // Posledním řádek může být kratší
              ...(index === rows - 1 && rows > 1 && {
                width: '60%',
              }),
              
              // Custom skeleton animation
              '&::after': {
                background: `linear-gradient(90deg, transparent, ${tokens.colors.grey[200]}, transparent)`,
              },
            }}
          />
        ))}
      </Box>
    );
  }

  // 🌀 Default spinner loader
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: config.spacing,
        ...sx,
      }}
      role="status"
      aria-live="polite"
      aria-label={text || 'Načítá se...'}
    >
      <CircularProgress
        size={config.spinner}
        thickness={4}
        sx={{
          color: tokens.colors.primary[500],
          mb: text ? 2 : 0,
        }}
      />
      
      {text && (
        <Typography
          variant="body2"
          sx={{
            fontSize: config.text,
            color: tokens.colors.grey[600],
            textAlign: 'center',
          }}
        >
          {text}
        </Typography>
      )}
    </Box>
  );
};

export default Loader;