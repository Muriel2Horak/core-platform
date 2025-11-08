# T5: Column Customization

**Story:** [S12: Kanban Board View](README.md)  
**Effort:** 15 hours  
**Priority:** P2  
**Dependencies:** T3

---

## 📋 TASK DESCRIPTION

Implementovat customization dialog pro kanban sloupce - rename, reorder, status mapping, WIP limits.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Rename column** - změna názvu sloupce
2. **Reorder columns** - drag & drop sloupců
3. **Status mapping** - multi-select dropdown pro přiřazení statusů
4. **WIP limit** - nastavení max počtu karet v sloupci
5. **Warning indicator** - červená ikonka když WIP limit překročen

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/kanban/ColumnCustomizationDialog.tsx
import { Dialog, DialogTitle, DialogContent, TextField, Select, MenuItem, Checkbox } from '@mui/material';

interface ColumnCustomizationDialogProps {
  column: KanbanColumn;
  availableStatuses: WorkflowStatus[];
  onSave: (updatedColumn: KanbanColumn) => void;
  onClose: () => void;
}

export const ColumnCustomizationDialog: React.FC<ColumnCustomizationDialogProps> = ({
  column,
  availableStatuses,
  onSave,
  onClose
}) => {
  const [name, setName] = useState(column.name);
  const [selectedStatuses, setSelectedStatuses] = useState<string[]>(
    column.statuses.map(s => s.id)
  );
  const [wipLimit, setWipLimit] = useState(column.wipLimit || 0);

  const handleSave = () => {
    onSave({
      ...column,
      name,
      statuses: availableStatuses.filter(s => selectedStatuses.includes(s.id)),
      wipLimit
    });
    onClose();
  };

  return (
    <Dialog open onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>Customize Column</DialogTitle>
      <DialogContent>
        {/* Column name */}
        <TextField
          label="Column Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          fullWidth
          sx={{ mb: 2 }}
        />

        {/* Status mapping */}
        <FormControl fullWidth sx={{ mb: 2 }}>
          <InputLabel>Mapped Statuses</InputLabel>
          <Select
            multiple
            value={selectedStatuses}
            onChange={(e) => setSelectedStatuses(e.target.value as string[])}
            renderValue={(selected) => (
              <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                {selected.map(id => (
                  <Chip key={id} label={availableStatuses.find(s => s.id === id)?.name} size="small" />
                ))}
              </Box>
            )}
          >
            {availableStatuses.map(status => (
              <MenuItem key={status.id} value={status.id}>
                <Checkbox checked={selectedStatuses.includes(status.id)} />
                <ListItemText primary={status.name} />
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        {/* WIP limit */}
        <TextField
          label="WIP Limit (0 = no limit)"
          type="number"
          value={wipLimit}
          onChange={(e) => setWipLimit(parseInt(e.target.value))}
          fullWidth
        />

        <Button onClick={handleSave} variant="contained" sx={{ mt: 2 }}>
          Save
        </Button>
      </DialogContent>
    </Dialog>
  );
};
```

### WIP Limit Warning

```typescript
// frontend/src/components/kanban/KanbanColumn.tsx (update)
const isWipLimitExceeded = column.wipLimit > 0 && cards.length > column.wipLimit;

return (
  <Box>
    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <Typography variant="h6">
        {column.name} ({cards.length})
        {isWipLimitExceeded && (
          <WarningIcon color="error" sx={{ ml: 1 }} />
        )}
      </Typography>
      
      <IconButton onClick={() => setCustomizeOpen(true)}>
        <SettingsIcon />
      </IconButton>
    </Box>
    
    {isWipLimitExceeded && (
      <Alert severity="warning" sx={{ mt: 1 }}>
        WIP limit exceeded! ({cards.length}/{column.wipLimit})
      </Alert>
    )}
    
    {/* Cards... */}
  </Box>
);
```

### Backend Entity

```java
// backend/src/main/java/cz/muriel/core/kanban/KanbanColumn.java
@Entity
public class KanbanColumn {
  @Id
  private UUID id;
  
  private String name;
  
  @ManyToMany
  private Set<WorkflowStatus> statuses;
  
  private Integer wipLimit;
  
  @Column(name = "column_order")
  private Integer order;  // For reordering
  
  // ✅ Check WIP limit
  public boolean isWipLimitExceeded(int cardCount) {
    return wipLimit != null && wipLimit > 0 && cardCount > wipLimit;
  }
}
```

---

## ✅ TESTING

```typescript
test('shows WIP limit warning when exceeded', () => {
  const column = { id: '1', name: 'In Progress', wipLimit: 3 };
  const cards = [{ id: '1' }, { id: '2' }, { id: '3' }, { id: '4' }];

  render(<KanbanColumn column={column} cards={cards} />);
  
  expect(screen.getByText('WIP limit exceeded! (4/3)')).toBeInTheDocument();
  expect(screen.getByTestId('WarningIcon')).toBeInTheDocument();
});

test('saves column customization', async () => {
  render(<ColumnCustomizationDialog column={column} />);
  
  fireEvent.change(screen.getByLabelText('Column Name'), { target: { value: 'Doing' } });
  fireEvent.change(screen.getByLabelText('WIP Limit'), { target: { value: '5' } });
  fireEvent.click(screen.getByText('Save'));
  
  await waitFor(() => {
    expect(onSave).toHaveBeenCalledWith({
      ...column,
      name: 'Doing',
      wipLimit: 5
    });
  });
});
```

---

## 📦 DELIVERABLES

- [ ] Customization dialog component
- [ ] WIP limit warning UI
- [ ] Backend API for column updates
- [ ] Unit tests (50%+ coverage)

---

**Estimated:** 15 hours
