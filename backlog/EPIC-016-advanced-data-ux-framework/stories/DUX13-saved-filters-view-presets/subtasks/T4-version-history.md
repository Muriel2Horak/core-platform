# T4: Version History

**Story:** [S13: Saved Filters & Views](README.md)  
**Effort:** 6 hours  
**Priority:** P2  
**Dependencies:** T1, T3

---

## 📋 TASK DESCRIPTION

Zobrazit version history pro saved view - track změn, rollback na předchozí verzi.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Version list** - dialog zobrazí všechny verze (v1, v2, v3...)
2. **Version details** - timestamp, user who updated, změny v filters
3. **Rollback** - button "Restore this version"
4. **Current indicator** - highlight aktuální verze

---

## 🏗️ IMPLEMENTATION

### Backend - Version Tracking

```java
// backend/src/main/java/cz/muriel/core/views/SavedViewVersion.java
@Entity
@Table(name = "saved_view_versions")
public class SavedViewVersion {
  @Id
  private UUID id;
  
  @ManyToOne
  private SavedView view;
  
  private Integer version;
  
  private String name;
  
  @Column(columnDefinition = "jsonb")
  @Type(type = "jsonb")
  private FilterConfig filters;
  
  @ManyToOne
  private User updatedBy;
  
  @CreatedDate
  private Instant createdAt;
}

// Service update
@Service
public class SavedViewService {
  
  public SavedViewDTO updateView(UUID id, SavedViewUpdateDTO dto, User user) {
    SavedView view = repository.findById(id).orElseThrow();
    
    // ✅ Save current state as version
    SavedViewVersion version = new SavedViewVersion();
    version.setView(view);
    version.setVersion(view.getVersion());
    version.setName(view.getName());
    version.setFilters(view.getFilters());
    version.setUpdatedBy(user);
    versionRepository.save(version);
    
    // Update view
    view.setName(dto.getName());
    view.setFilters(dto.getFilters());
    view.setVersion(view.getVersion() + 1);
    
    return toDTO(repository.save(view));
  }
  
  public List<SavedViewVersionDTO> getVersionHistory(UUID viewId) {
    return versionRepository.findByViewIdOrderByVersionDesc(viewId)
      .stream()
      .map(this::toVersionDTO)
      .collect(Collectors.toList());
  }
  
  public SavedViewDTO rollbackToVersion(UUID viewId, Integer version, User user) {
    SavedView view = repository.findById(viewId).orElseThrow();
    SavedViewVersion targetVersion = versionRepository
      .findByViewIdAndVersion(viewId, version)
      .orElseThrow();
    
    // Save current as version before rollback
    saveCurrentAsVersion(view, user);
    
    // Restore from target version
    view.setName(targetVersion.getName());
    view.setFilters(targetVersion.getFilters());
    view.setVersion(view.getVersion() + 1);
    
    return toDTO(repository.save(view));
  }
}
```

### Frontend - Version History Dialog

```typescript
// frontend/src/components/views/VersionHistoryDialog.tsx
import { Dialog, DialogTitle, List, ListItem, ListItemText, Button, Chip } from '@mui/material';
import { History, Restore } from '@mui/icons-material';

interface VersionHistoryDialogProps {
  open: boolean;
  view: SavedView;
  onRollback: (version: number) => Promise<void>;
  onClose: () => void;
}

export const VersionHistoryDialog: React.FC<VersionHistoryDialogProps> = ({
  open,
  view,
  onRollback,
  onClose
}) => {
  const [versions, setVersions] = useState<SavedViewVersion[]>([]);

  useEffect(() => {
    if (open) {
      api.get<SavedViewVersion[]>(`/api/saved-views/${view.id}/versions`)
        .then(setVersions);
    }
  }, [open, view.id]);

  const handleRollback = async (version: number) => {
    await onRollback(version);
    onClose();
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <History sx={{ mr: 1 }} />
        Version History: {view.name}
      </DialogTitle>
      
      <List>
        {versions.map(v => (
          <ListItem
            key={v.id}
            secondaryAction={
              v.version !== view.version ? (
                <Button
                  size="small"
                  startIcon={<Restore />}
                  onClick={() => handleRollback(v.version)}
                >
                  Restore
                </Button>
              ) : (
                <Chip label="Current" color="primary" size="small" />
              )
            }
            sx={{
              bgcolor: v.version === view.version ? 'action.selected' : 'transparent'
            }}
          >
            <ListItemText
              primary={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <Typography variant="subtitle1">
                    Version {v.version}
                  </Typography>
                  {v.version === view.version && (
                    <Chip label="Current" color="primary" size="small" />
                  )}
                </Box>
              }
              secondary={
                <>
                  <Typography variant="body2" color="text.secondary">
                    Updated by {v.updatedBy.name} • {formatDate(v.createdAt)}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Filters: {JSON.stringify(v.filters, null, 2)}
                  </Typography>
                </>
              }
            />
          </ListItem>
        ))}
      </List>
    </Dialog>
  );
};
```

### Integration

```typescript
// frontend/src/components/views/ViewSwitcher.tsx (update)
const [versionHistoryOpen, setVersionHistoryOpen] = useState(false);

// Add menu item
<MenuItem onClick={() => setVersionHistoryOpen(true)}>
  <History fontSize="small" sx={{ mr: 1 }} />
  Version History
</MenuItem>

<VersionHistoryDialog
  open={versionHistoryOpen}
  view={currentView}
  onRollback={async (version) => {
    await api.post(`/api/saved-views/${currentView.id}/rollback`, { version });
    // Reload view
    const updated = await api.get(`/api/saved-views/${currentView.id}`);
    setCurrentView(updated);
  }}
  onClose={() => setVersionHistoryOpen(false)}
/>
```

---

## ✅ TESTING

```typescript
test('shows version history', async () => {
  const versions = [
    { version: 3, updatedBy: { name: 'Jan' }, createdAt: '2024-01-03' },
    { version: 2, updatedBy: { name: 'Petr' }, createdAt: '2024-01-02' },
    { version: 1, updatedBy: { name: 'Jan' }, createdAt: '2024-01-01' }
  ];

  render(<VersionHistoryDialog view={view} versions={versions} />);
  
  expect(screen.getByText('Version 3')).toBeInTheDocument();
  expect(screen.getByText('Version 2')).toBeInTheDocument();
  expect(screen.getByText('Version 1')).toBeInTheDocument();
});

test('restores old version', async () => {
  render(<VersionHistoryDialog view={view} />);
  
  fireEvent.click(screen.getByText('Restore', { selector: 'button' }));
  
  await waitFor(() => {
    expect(onRollback).toHaveBeenCalledWith(2);
  });
});
```

---

## 📦 DELIVERABLES

- [ ] SavedViewVersion entity
- [ ] Version history endpoint
- [ ] Rollback logic
- [ ] VersionHistoryDialog component
- [ ] Unit tests (50%+ coverage)

---

**Estimated:** 6 hours
