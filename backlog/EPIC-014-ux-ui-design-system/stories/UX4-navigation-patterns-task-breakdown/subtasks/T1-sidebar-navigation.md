# T1: Sidebar Navigation Component

**Parent Story:** S4 - Navigation Patterns  
**LOC:** ~250 | **Effort:** ~3h

## Objective
Create collapsible sidebar navigation with sections, icons, and active state highlighting.

## Implementation

```tsx
// frontend/src/components/navigation/Sidebar.tsx
import { Drawer, List, ListItemButton, ListItemIcon, ListItemText, Collapse } from '@mui/material';
import { useLocation, useNavigate } from 'react-router-dom';
import { ExpandLess, ExpandMore } from '@mui/icons-material';

interface NavItem {
  id: string;
  label: string;
  icon: React.ReactNode;
  path?: string;
  children?: NavItem[];
}

export const Sidebar: React.FC<{ items: NavItem[] }> = ({ items }) => {
  const location = useLocation();
  const navigate = useNavigate();
  const [expanded, setExpanded] = useState<Set<string>>(new Set());

  const isActive = (path: string) => location.pathname === path;
  
  const toggleExpand = (id: string) => {
    setExpanded(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  return (
    <Drawer variant="permanent">
      <List>
        {items.map(item => (
          <NavSection
            key={item.id}
            item={item}
            isActive={isActive}
            expanded={expanded.has(item.id)}
            onToggle={() => toggleExpand(item.id)}
            onNavigate={navigate}
          />
        ))}
      </List>
    </Drawer>
  );
};
```

## Acceptance Criteria
- [ ] Sidebar renders with navigation items
- [ ] Active route highlighted
- [ ] Sections collapsible/expandable
- [ ] Icons display correctly
- [ ] Click navigates to route
- [ ] Hover states styled
- [ ] Mobile: Drawer overlay mode

## Files
- `frontend/src/components/navigation/Sidebar.tsx`
- `frontend/src/components/navigation/NavSection.tsx`
- `frontend/src/components/navigation/NavItem.tsx`
