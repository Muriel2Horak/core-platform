# T4: Mobile Navigation Drawer

**Parent Story:** S4 - Navigation Patterns  
**LOC:** ~50 | **Effort:** ~1h

## Objective
Adapt sidebar navigation for mobile with hamburger menu and drawer overlay.

## Implementation

```tsx
// frontend/src/components/navigation/MobileNav.tsx
import { IconButton, SwipeableDrawer } from '@mui/material';
import { Menu as MenuIcon } from '@mui/icons-material';

export const MobileNav: React.FC<{ items: NavItem[] }> = ({ items }) => {
  const [open, setOpen] = useState(false);
  
  return (
    <>
      <IconButton
        edge="start"
        color="inherit"
        aria-label="menu"
        onClick={() => setOpen(true)}
      >
        <MenuIcon />
      </IconButton>
      
      <SwipeableDrawer
        anchor="left"
        open={open}
        onClose={() => setOpen(false)}
        onOpen={() => setOpen(true)}
      >
        <Sidebar items={items} onNavigate={() => setOpen(false)} />
      </SwipeableDrawer>
    </>
  );
};
```

## Acceptance Criteria
- [ ] Hamburger icon in AppBar (mobile only)
- [ ] Drawer opens on click
- [ ] Swipe to open/close
- [ ] Closes after navigation
- [ ] Backdrop click closes

## Files
- `frontend/src/components/navigation/MobileNav.tsx`
