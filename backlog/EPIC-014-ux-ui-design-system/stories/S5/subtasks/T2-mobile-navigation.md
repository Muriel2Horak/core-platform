# T2: Mobile Navigation Patterns

**Parent Story:** S5 - Responsive Design  
**LOC:** ~200 | **Effort:** ~2h

## Objective
Implement mobile-specific navigation patterns (hamburger menu, bottom tabs).

## Implementation

```tsx
// frontend/src/components/mobile/BottomNavigation.tsx
import { BottomNavigation, BottomNavigationAction } from '@mui/material';
import { Home, Search, Settings } from '@mui/icons-material';

export const MobileBottomNav: React.FC = () => {
  const [value, setValue] = useState(0);
  
  return (
    <BottomNavigation
      value={value}
      onChange={(_, newValue) => setValue(newValue)}
      sx={{ position: 'fixed', bottom: 0, left: 0, right: 0 }}
    >
      <BottomNavigationAction label="Home" icon={<Home />} />
      <BottomNavigationAction label="Search" icon={<Search />} />
      <BottomNavigationAction label="Settings" icon={<Settings />} />
    </BottomNavigation>
  );
};
```

## Acceptance Criteria
- [ ] Bottom navigation on mobile
- [ ] Hamburger menu for secondary nav
- [ ] Swipe gestures supported
- [ ] Active tab highlighted

## Files
- `frontend/src/components/mobile/BottomNavigation.tsx`
- `frontend/src/components/mobile/MobileMenu.tsx`
