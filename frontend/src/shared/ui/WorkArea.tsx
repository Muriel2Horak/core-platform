/**
 * 🏢 WorkArea - Full-width Work Layout Container
 * 
 * Kontejner pro široké pracovní plochy jako tabulky, kanban boardy, atd.
 * Využívá plnou šířku bez max-width omezení.
 */

import React from 'react';
import { Box, Typography, Divider } from '@mui/material';
import { tokens } from '../theme/tokens';

export interface WorkSectionProps {
  /** Nadpis sekce */
  title?: string;
  /** Podnadpis sekce */
  subtitle?: string;
  /** Obsah sekce */
  children?: React.ReactNode;
  /** Akční tlačítka v hlavičce */
  actions?: React.ReactNode;
  /** Varianta zobrazení */
  variant?: 'default' | 'compact';
}

/**
 * WorkSection - sekce v rámci WorkArea
 */
export const WorkSection: React.FC<WorkSectionProps> = ({
  title,
  subtitle,
  children,
  actions,
  variant = 'default',
}) => {
  const isCompact = variant === 'compact';

  return (
    <Box sx={{ mb: isCompact ? 2 : 4 }}>
      {(title || subtitle || actions) && (
        <>
          <Box sx={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: isCompact ? 'center' : 'flex-start',
            mb: isCompact ? 1 : 2,
            flexWrap: 'wrap',
            gap: 2,
          }}>
            {(title || subtitle) && (
              <Box>
                {title && (
                  <Typography 
                    variant={isCompact ? "h5" : "h4"} 
                    sx={{ 
                      fontWeight: tokens.typography.fontWeight.bold,
                      mb: subtitle ? 0.5 : 0,
                    }}
                  >
                    {title}
                  </Typography>
                )}
                {subtitle && (
                  <Typography 
                    variant="body1" 
                    color="text.secondary"
                    sx={{ 
                      fontSize: isCompact ? '0.9rem' : '1rem',
                    }}
                  >
                    {subtitle}
                  </Typography>
                )}
              </Box>
            )}
            
            {actions && (
              <Box sx={{ 
                display: 'flex', 
                gap: 1, 
                alignItems: 'center',
                flexWrap: 'wrap',
              }}>
                {actions}
              </Box>
            )}
          </Box>
          
          {!isCompact && <Divider sx={{ mb: 3 }} />}
        </>
      )}
      
      {children}
    </Box>
  );
};

export interface FullBleedContainerProps {
  children: React.ReactNode;
  /** Výška kontejneru */
  height?: string | number;
}

/**
 * FullBleedContainer - kontejner pro obsah přes celou šířku bez paddingu
 */
export const FullBleedContainer: React.FC<FullBleedContainerProps> = ({
  children,
  height = 'auto',
}) => {
  return (
    <Box
      sx={{
        width: '100%',
        height,
        margin: `-${tokens.spacing.lg}`,
        marginBottom: tokens.spacing.lg,
        overflow: 'hidden',
      }}
    >
      {children}
    </Box>
  );
};

export interface WorkAreaProps {
  children: React.ReactNode;
  /** Padding kontejneru */
  padding?: string | number;
}

/**
 * WorkArea komponent pro široké pracovní plochy
 */
export const WorkArea: React.FC<WorkAreaProps> = ({
  children,
  padding = tokens.spacing.lg,
}) => {
  return (
    <Box
      sx={{
        width: '100%',
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
        p: padding,
        overflow: 'hidden', // Prevent content overflow
      }}
    >
      {children}
    </Box>
  );
};

export default WorkArea;