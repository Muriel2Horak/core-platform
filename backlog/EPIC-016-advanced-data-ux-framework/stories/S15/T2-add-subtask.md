# T2: Add Subtask Inline

**Story:** [S15: Task Breakdown](README.md)  
**Effort:** 8 hours  
**Priority:** P0  
**Dependencies:** T1

---

## 📋 TASK DESCRIPTION

Inline form pro přidání subtasku přímo v tree view.

---

## 🎯 ACCEPTANCE CRITERIA

1. **"+ Add subtask" button** - pod každým node
2. **Inline form** - title input + save/cancel
3. **Parent assignment** - automaticky přiřadit parent
4. **Server sync** - POST `/api/tasks` s parentId

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/breakdown/AddSubtaskForm.tsx
interface AddSubtaskFormProps {
  parentId: string;
  onSave: (title: string) => Promise<void>;
  onCancel: () => void;
}

export const AddSubtaskForm: React.FC<AddSubtaskFormProps> = ({ parentId, onSave, onCancel }) => {
  const [title, setTitle] = useState('');

  const handleSave = async () => {
    await onSave(title);
    setTitle('');
  };

  return (
    <Box sx={{ display: 'flex', gap: 1, ml: 4, mt: 1 }}>
      <TextField
        size="small"
        placeholder="Subtask title..."
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        autoFocus
      />
      <IconButton size="small" onClick={handleSave}>
        <Check />
      </IconButton>
      <IconButton size="small" onClick={onCancel}>
        <Close />
      </IconButton>
    </Box>
  );
};
```

### Integration

```typescript
// TreeNode.tsx (update)
const [showAddForm, setShowAddForm] = useState(false);

<Box>
  <TreeItem {...props} />
  
  {expanded && (
    <Button size="small" onClick={() => setShowAddForm(true)}>
      + Add subtask
    </Button>
  )}
  
  {showAddForm && (
    <AddSubtaskForm
      parentId={node.id}
      onSave={async (title) => {
        await api.post('/api/tasks', { title, parentId: node.id });
        setShowAddForm(false);
      }}
      onCancel={() => setShowAddForm(false)}
    />
  )}
</Box>
```

---

## 📦 DELIVERABLES

- [ ] AddSubtaskForm component
- [ ] Inline add functionality
- [ ] Parent assignment
- [ ] Unit tests (50%+ coverage)

---

**Estimated:** 8 hours
