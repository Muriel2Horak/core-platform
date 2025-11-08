# T3: Save View Dialog

**Story:** [S13: Saved Filters & Views](README.md)  
**Effort:** 8 hours  
**Priority:** P1  
**Dependencies:** T1, T2

---

## 📋 TASK DESCRIPTION

Implementovat dialog pro uložení aktuálních filtrů jako nový saved view nebo update existujícího.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Name input** - required field
2. **Save mode** - "Save as new" vs "Update existing"
3. **Share toggle** - checkbox "Share with others"
4. **User picker** - multi-select dropdown (pokud shared)
5. **Preview** - zobrazit current filters

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/views/SaveViewDialog.tsx
import { Dialog, DialogTitle, DialogContent, TextField, Checkbox, FormControlLabel, Button } from '@mui/material';

interface SaveViewDialogProps {
  open: boolean;
  currentFilters: FilterConfig;
  existingView?: SavedView | null;
  onSave: (dto: SaveViewCreateDTO) => Promise<void>;
  onClose: () => void;
}

export const SaveViewDialog: React.FC<SaveViewDialogProps> = ({
  open,
  currentFilters,
  existingView,
  onSave,
  onClose
}) => {
  const [name, setName] = useState(existingView?.name || '');
  const [isShared, setIsShared] = useState(existingView?.isShared || false);
  const [selectedUsers, setSelectedUsers] = useState<UUID[]>([]);
  const [availableUsers, setAvailableUsers] = useState<User[]>([]);

  useEffect(() => {
    if (isShared) {
      // Load users for sharing
      api.get<User[]>('/api/users').then(setAvailableUsers);
    }
  }, [isShared]);

  const handleSave = async () => {
    const dto: SaveViewCreateDTO = {
      name,
      filters: currentFilters,
      isShared,
      sharedWith: isShared ? selectedUsers : []
    };

    await onSave(dto);
    onClose();
  };

  const filterSummary = useMemo(() => {
    const parts = [];
    if (currentFilters.assignees.length > 0) {
      parts.push(`Assignees: ${currentFilters.assignees.length}`);
    }
    if (currentFilters.priorities.length > 0) {
      parts.push(`Priorities: ${currentFilters.priorities.join(', ')}`);
    }
    if (currentFilters.groupBy !== 'NONE') {
      parts.push(`Grouped by: ${currentFilters.groupBy}`);
    }
    return parts.join(' | ');
  }, [currentFilters]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        {existingView ? 'Update View' : 'Save View'}
      </DialogTitle>
      
      <DialogContent>
        {/* Name */}
        <TextField
          label="View Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          fullWidth
          required
          sx={{ mb: 2 }}
          error={!name}
          helperText={!name ? 'Name is required' : ''}
        />

        {/* Filter preview */}
        <Paper variant="outlined" sx={{ p: 2, mb: 2, bgcolor: 'grey.50' }}>
          <Typography variant="caption" color="text.secondary">
            Current filters:
          </Typography>
          <Typography variant="body2">
            {filterSummary || 'No filters applied'}
          </Typography>
        </Paper>

        {/* Share toggle */}
        <FormControlLabel
          control={
            <Checkbox
              checked={isShared}
              onChange={(e) => setIsShared(e.target.checked)}
            />
          }
          label="Share this view with others"
        />

        {/* User picker (pokud shared) */}
        {isShared && (
          <FormControl fullWidth sx={{ mt: 2 }}>
            <InputLabel>Share with users</InputLabel>
            <Select
              multiple
              value={selectedUsers}
              onChange={(e) => setSelectedUsers(e.target.value as UUID[])}
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                  {selected.map(id => (
                    <Chip
                      key={id}
                      label={availableUsers.find(u => u.id === id)?.name}
                      size="small"
                    />
                  ))}
                </Box>
              )}
            >
              {availableUsers.map(user => (
                <MenuItem key={user.id} value={user.id}>
                  <Checkbox checked={selectedUsers.includes(user.id)} />
                  <ListItemText primary={user.name} secondary={user.email} />
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        )}

        {/* Actions */}
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1, mt: 3 }}>
          <Button onClick={onClose}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleSave}
            disabled={!name}
          >
            {existingView ? 'Update' : 'Save'}
          </Button>
        </Box>
      </DialogContent>
    </Dialog>
  );
};
```

### Integration

```typescript
// frontend/src/pages/KanbanBoardPage.tsx (update)
const [saveDialogOpen, setSaveDialogOpen] = useState(false);

const handleSaveView = async (dto: SaveViewCreateDTO) => {
  if (currentView) {
    // Update existing
    await api.patch(`/api/saved-views/${currentView.id}`, dto);
  } else {
    // Create new
    const newView = await api.post<SavedView>('/api/saved-views', dto);
    setCurrentView(newView);
  }
  
  // Reload views
  const views = await api.get<SavedView[]>('/api/saved-views');
  setPersonalViews(views.filter(v => v.isOwner));
  setSharedViews(views.filter(v => !v.isOwner));
};

return (
  <>
    <KanbanBoard />
    
    <SaveViewDialog
      open={saveDialogOpen}
      currentFilters={filters}
      existingView={currentView}
      onSave={handleSaveView}
      onClose={() => setSaveDialogOpen(false)}
    />
  </>
);
```

---

## ✅ TESTING

```typescript
test('saves new view', async () => {
  render(<SaveViewDialog open currentFilters={filters} />);
  
  fireEvent.change(screen.getByLabelText('View Name'), {
    target: { value: 'My Custom View' }
  });
  
  fireEvent.click(screen.getByText('Save'));
  
  await waitFor(() => {
    expect(onSave).toHaveBeenCalledWith({
      name: 'My Custom View',
      filters: filters,
      isShared: false,
      sharedWith: []
    });
  });
});

test('shows user picker when shared enabled', () => {
  render(<SaveViewDialog open currentFilters={filters} />);
  
  fireEvent.click(screen.getByLabelText('Share this view with others'));
  
  expect(screen.getByLabelText('Share with users')).toBeInTheDocument();
});
```

---

## 📦 DELIVERABLES

- [ ] SaveViewDialog component
- [ ] Name validation
- [ ] Share toggle + user picker
- [ ] Filter preview
- [ ] Unit tests (60%+ coverage)

---

**Estimated:** 8 hours
