# T2: View Switcher UI

**Story:** [S13: Saved Filters & Views](README.md)  
**Effort:** 10 hours  
**Priority:** P0  
**Dependencies:** T1

---

## 📋 TASK DESCRIPTION

Implementovat dropdown component pro rychlé přepínání mezi saved views.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Dropdown button** - "Current View: [name]" s šipkou
2. **Personal views** - sekce "My Views" (views owned by user)
3. **Shared views** - sekce "Shared with me" (views sdílené ostatními)
4. **Default view** - "Default (no filters)" vždy dostupný
5. **Quick switch** - kliknutí na view aplikuje filters okamžitě

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/views/ViewSwitcher.tsx
import { Button, Menu, MenuItem, ListSubheader, Divider } from '@mui/material';
import { KeyboardArrowDown, Star, People } from '@mui/icons-material';

interface ViewSwitcherProps {
  currentView: SavedView | null;
  personalViews: SavedView[];
  sharedViews: SavedView[];
  onViewSelect: (view: SavedView | null) => void;
  onCreateView: () => void;
}

export const ViewSwitcher: React.FC<ViewSwitcherProps> = ({
  currentView,
  personalViews,
  sharedViews,
  onViewSelect,
  onCreateView
}) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  
  const handleOpen = (event: React.MouseEvent<HTMLElement>) => {
    setAnchorEl(event.currentTarget);
  };
  
  const handleClose = () => {
    setAnchorEl(null);
  };
  
  const handleViewSelect = (view: SavedView | null) => {
    onViewSelect(view);
    handleClose();
  };

  return (
    <>
      <Button
        variant="outlined"
        endIcon={<KeyboardArrowDown />}
        onClick={handleOpen}
      >
        Current View: {currentView?.name || 'Default'}
      </Button>
      
      <Menu
        anchorEl={anchorEl}
        open={Boolean(anchorEl)}
        onClose={handleClose}
        PaperProps={{ sx: { minWidth: 250 } }}
      >
        {/* Default view */}
        <MenuItem 
          onClick={() => handleViewSelect(null)}
          selected={!currentView}
        >
          <Star fontSize="small" sx={{ mr: 1 }} />
          Default (no filters)
        </MenuItem>
        
        <Divider />
        
        {/* Personal views */}
        <ListSubheader>My Views</ListSubheader>
        {personalViews.length === 0 ? (
          <MenuItem disabled>
            <Typography variant="body2" color="text.secondary">
              No saved views yet
            </Typography>
          </MenuItem>
        ) : (
          personalViews.map(view => (
            <MenuItem
              key={view.id}
              onClick={() => handleViewSelect(view)}
              selected={currentView?.id === view.id}
            >
              {view.name}
            </MenuItem>
          ))
        )}
        
        <Divider />
        
        {/* Shared views */}
        <ListSubheader>Shared with me</ListSubheader>
        {sharedViews.length === 0 ? (
          <MenuItem disabled>
            <Typography variant="body2" color="text.secondary">
              No shared views
            </Typography>
          </MenuItem>
        ) : (
          sharedViews.map(view => (
            <MenuItem
              key={view.id}
              onClick={() => handleViewSelect(view)}
              selected={currentView?.id === view.id}
            >
              <People fontSize="small" sx={{ mr: 1 }} />
              {view.name}
              <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
                by {view.owner.name}
              </Typography>
            </MenuItem>
          ))
        )}
        
        <Divider />
        
        {/* Create new */}
        <MenuItem onClick={onCreateView}>
          + Create new view
        </MenuItem>
      </Menu>
    </>
  );
};
```

### Integration s Kanban Board

```typescript
// frontend/src/pages/KanbanBoardPage.tsx
export const KanbanBoardPage: React.FC = () => {
  const [currentView, setCurrentView] = useState<SavedView | null>(null);
  const [personalViews, setPersonalViews] = useState<SavedView[]>([]);
  const [sharedViews, setSharedViews] = useState<SavedView[]>([]);

  useEffect(() => {
    // Load saved views
    api.get<SavedView[]>('/api/saved-views').then(views => {
      setPersonalViews(views.filter(v => v.isOwner));
      setSharedViews(views.filter(v => !v.isOwner));
    });
  }, []);

  const handleViewSelect = (view: SavedView | null) => {
    setCurrentView(view);
    
    if (view) {
      // ✅ Apply filters from saved view
      setFilters(view.filters);
    } else {
      // ✅ Reset to default (no filters)
      setFilters({
        assignees: [],
        priorities: [],
        types: [],
        tags: [],
        groupBy: 'NONE'
      });
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 2 }}>
        <ViewSwitcher
          currentView={currentView}
          personalViews={personalViews}
          sharedViews={sharedViews}
          onViewSelect={handleViewSelect}
          onCreateView={() => setSaveDialogOpen(true)}
        />
        
        {/* Current filters display */}
        <KanbanFilters filters={filters} onFilterChange={setFilters} />
      </Box>
      
      <KanbanBoard filters={filters} />
    </Box>
  );
};
```

---

## ✅ TESTING

```typescript
test('renders personal and shared views', () => {
  const personalViews = [
    { id: '1', name: 'My View 1', isOwner: true }
  ];
  const sharedViews = [
    { id: '2', name: 'Shared View', isOwner: false, owner: { name: 'Jan' } }
  ];

  render(<ViewSwitcher personalViews={personalViews} sharedViews={sharedViews} />);
  
  fireEvent.click(screen.getByText('Current View: Default'));
  
  expect(screen.getByText('My View 1')).toBeInTheDocument();
  expect(screen.getByText('Shared View')).toBeInTheDocument();
  expect(screen.getByText('by Jan')).toBeInTheDocument();
});

test('applies filters when selecting view', () => {
  const view = {
    id: '1',
    name: 'High Priority',
    filters: { priorities: ['HIGH'] }
  };

  render(<ViewSwitcher personalViews={[view]} />);
  
  fireEvent.click(screen.getByText('Current View: Default'));
  fireEvent.click(screen.getByText('High Priority'));
  
  expect(onViewSelect).toHaveBeenCalledWith(view);
});
```

---

## 📦 DELIVERABLES

- [ ] ViewSwitcher component
- [ ] Integration with KanbanBoard
- [ ] Personal/Shared sections
- [ ] Unit tests (60%+ coverage)

---

**Estimated:** 10 hours
