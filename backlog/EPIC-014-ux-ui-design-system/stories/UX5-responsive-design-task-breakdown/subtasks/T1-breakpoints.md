# T1: Breakpoint System & Grid

**Parent Story:** S5 - Responsive Design  
**LOC:** ~200 | **Effort:** ~2h

## Objective
Define responsive breakpoints and implement mobile-first grid system.

## Implementation

```tsx
// frontend/src/theme/breakpoints.ts
export const breakpoints = {
  values: {
    xs: 0,      // Mobile portrait
    sm: 600,    // Mobile landscape
    md: 900,    // Tablet
    lg: 1200,   // Desktop
    xl: 1536,   // Large desktop
  },
};

// Usage in components
import { useMediaQuery, useTheme } from '@mui/material';

const ResponsiveComponent = () => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  
  return isMobile ? <MobileView /> : <DesktopView />;
};
```

## Acceptance Criteria
- [ ] Breakpoints defined in theme
- [ ] useMediaQuery hook documented
- [ ] Grid system responsive (12 columns)
- [ ] Components adapt to screen size
- [ ] Mobile-first CSS approach

## Files
- `frontend/src/theme/breakpoints.ts`
- `frontend/src/hooks/useResponsive.ts`
