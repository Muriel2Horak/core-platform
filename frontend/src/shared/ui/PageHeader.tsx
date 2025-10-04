/**
 * 📄 PageHeader - Core Platform Page Header Component
 * 
 * Konzistentní záhlaví pro všechny stránky s titulkem, podtitulkem a akcemi.
 * Podporuje breadcrumbs a responsive layout.
 */

import React from 'react';
import { Box, Typography, Breadcrumbs, Link, useTheme, useMediaQuery } from '@mui/material';
import { ChevronRight as ChevronRightIcon } from '@mui/icons-material';
import { tokens } from '../theme/tokens';

export interface BreadcrumbItem {
  /** Text breadcrumb položky */
  label: string;
  /** URL pro navigaci (optional pro poslední položku) */
  href?: string;
  /** Click handler jako alternativa k href */
  onClick?: () => void;
}

export interface PageHeaderProps {
  /** Hlavní titulek stránky */
  title: string;
  /** Volitelný podtitulek */
  subtitle?: string;
  /** Breadcrumb navigace */
  breadcrumbs?: BreadcrumbItem[];
  /** Akční tlačítka vpravo */
  actions?: React.ReactNode;
  /** Dodatečný obsah pod titulkem */
  children?: React.ReactNode;
}

/**
 * PageHeader komponent s Core Platform design systémem
 */
export const PageHeader: React.FC<PageHeaderProps> = ({
  title,
  subtitle,
  breadcrumbs = [],
  actions,
  children,
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));

  return (
    <Box
      component="header"
      sx={{
        mb: 4,
        pb: 3,
        borderBottom: `1px solid ${tokens.colors.grey[200]}`,
      }}
    >
      {/* Breadcrumbs */}
      {breadcrumbs.length > 0 && (
        <Breadcrumbs
          separator={<ChevronRightIcon fontSize="small" sx={{ color: tokens.colors.grey[400] }} />}
          sx={{ 
            mb: 2,
            '& .MuiBreadcrumbs-ol': {
              flexWrap: 'wrap',
            }
          }}
          aria-label="Navigační dráha"
        >
          {breadcrumbs.map((item, index) => {
            const isLast = index === breadcrumbs.length - 1;
            
            if (isLast || (!item.href && !item.onClick)) {
              return (
                <Typography
                  key={index}
                  color="text.primary"
                  sx={{
                    fontSize: tokens.typography.fontSize.sm,
                    fontWeight: isLast ? tokens.typography.fontWeight.semibold : tokens.typography.fontWeight.normal,
                  }}
                  aria-current={isLast ? 'page' : undefined}
                >
                  {item.label}
                </Typography>
              );
            }
            
            return (
              <Link
                key={index}
                href={item.href}
                onClick={item.onClick}
                underline="hover"
                color="inherit"
                sx={{
                  fontSize: tokens.typography.fontSize.sm,
                  color: tokens.colors.grey[600],
                  textDecoration: 'none',
                  cursor: 'pointer',
                  transition: `color ${tokens.components.animation.normal} ${tokens.components.animation.easing}`,
                  
                  '&:hover': {
                    color: tokens.colors.primary[600],
                  },
                  
                  '&:focus-visible': {
                    outline: `${tokens.a11y.focusRing.width} ${tokens.a11y.focusRing.style} ${tokens.a11y.focusRing.color}`,
                    outlineOffset: tokens.a11y.focusRing.offset,
                    borderRadius: tokens.radius.sm,
                  },
                }}
              >
                {item.label}
              </Link>
            );
          })}
        </Breadcrumbs>
      )}

      {/* Header Content */}
      <Box
        sx={{
          display: 'flex',
          flexDirection: isMobile ? 'column' : 'row',
          alignItems: isMobile ? 'flex-start' : 'flex-end',
          justifyContent: 'space-between',
          gap: isMobile ? 2 : 3,
        }}
      >
        {/* Title Section */}
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Typography
            variant="h1"
            component="h1"
            sx={{
              fontSize: isMobile ? tokens.typography.fontSize['3xl'] : tokens.typography.fontSize['4xl'],
              fontWeight: tokens.typography.fontWeight.bold,
              lineHeight: tokens.typography.lineHeight.tight,
              color: tokens.colors.grey[900],
              mb: subtitle ? 1 : 0,
              wordBreak: 'break-word', // Handle long titles
            }}
          >
            {title}
          </Typography>
          
          {subtitle && (
            <Typography
              variant="body1"
              sx={{
                fontSize: tokens.typography.fontSize.lg,
                color: tokens.colors.grey[600],
                lineHeight: tokens.typography.lineHeight.relaxed,
                maxWidth: tokens.components.layout.contentMaxWidth,
              }}
            >
              {subtitle}
            </Typography>
          )}
        </Box>

        {/* Actions Section */}
        {actions && (
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 2,
              flexWrap: 'wrap',
              flexShrink: 0,
              width: isMobile ? '100%' : 'auto',
              justifyContent: isMobile ? 'flex-start' : 'flex-end',
            }}
          >
            {actions}
          </Box>
        )}
      </Box>

      {/* Additional Content */}
      {children && (
        <Box sx={{ mt: 3 }}>
          {children}
        </Box>
      )}
    </Box>
  );
};

export default PageHeader;