/**
 * 📄 PageContainer - Content Layout Container
 * 
 * Kontejner pro centrálně zarovnaný obsah s max-width constraints.
 * Použití pro standardní stránky jako profily, formuláře, dashboardy.
 */

import React from 'react';
import { Container, Box } from '@mui/material';
import { tokens } from '../theme/tokens';

export interface PageContainerProps {
  children: React.ReactNode;
  /** Maximum width kontejneru */
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | false;
  /** Disable gutters (padding) */
  disableGutters?: boolean;
}

/**
 * PageContainer komponent pro centrálně zarovnaný obsah
 */
export const PageContainer: React.FC<PageContainerProps> = ({
  children,
  maxWidth = 'xl',
  disableGutters = false,
}) => {
  return (
    <Container
      maxWidth={maxWidth}
      disableGutters={disableGutters}
      sx={{
        py: tokens.spacing.lg,
        px: disableGutters ? 0 : tokens.spacing.md,
        flex: 1,
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      <Box sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
        {children}
      </Box>
    </Container>
  );
};

export default PageContainer;