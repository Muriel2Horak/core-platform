# T2: Breadcrumbs Component

**Parent Story:** S4 - Navigation Patterns  
**LOC:** ~150 | **Effort:** ~2h

## Objective
Create dynamic breadcrumbs component that shows current page hierarchy.

## Implementation

```tsx
// frontend/src/components/navigation/Breadcrumbs.tsx
import { Breadcrumbs as MuiBreadcrumbs, Link, Typography } from '@mui/material';
import { NavigateNext } from '@mui/icons-material';
import { useLocation, Link as RouterLink } from 'react-router-dom';

export const Breadcrumbs: React.FC = () => {
  const location = useLocation();
  
  const pathnames = location.pathname.split('/').filter(x => x);
  
  return (
    <MuiBreadcrumbs separator={<NavigateNext fontSize="small" />}>
      <Link component={RouterLink} to="/" underline="hover">
        Home
      </Link>
      {pathnames.map((value, index) => {
        const to = `/${pathnames.slice(0, index + 1).join('/')}`;
        const isLast = index === pathnames.length - 1;
        
        return isLast ? (
          <Typography key={to} color="text.primary">
            {formatBreadcrumb(value)}
          </Typography>
        ) : (
          <Link key={to} component={RouterLink} to={to} underline="hover">
            {formatBreadcrumb(value)}
          </Link>
        );
      })}
    </MuiBreadcrumbs>
  );
};
```

## Acceptance Criteria
- [ ] Breadcrumbs auto-generated from route
- [ ] All items clickable except current
- [ ] Proper separators (/)
- [ ] Current page not a link
- [ ] Truncation for long paths
- [ ] Custom labels via route config

## Files
- `frontend/src/components/navigation/Breadcrumbs.tsx`
- `frontend/src/utils/breadcrumbFormatter.ts`
