# T4: Drag & Drop Reparenting

**Story:** [S15: Task Breakdown](README.md)  
**Effort:** 6 hours  
**Priority:** P2  
**Dependencies:** T1

---

## 📋 TASK DESCRIPTION

Drag task na jiný parent → změna hierarchie.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Drag to reparent** - drag task na new parent
2. **Server sync** - PATCH `/api/tasks/{id}` s novým parentId
3. **Validation** - prevent circular dependencies
4. **Visual feedback** - highlight valid drop targets

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/breakdown/TreeView.tsx
import { DndProvider, useDrag, useDrop } from 'react-dnd';

const TreeNode: React.FC<TreeNodeProps> = ({ node }) => {
  const [{ isDragging }, drag] = useDrag(() => ({
    type: 'TASK',
    item: { id: node.id },
    collect: (monitor) => ({
      isDragging: monitor.isDragging()
    })
  }));

  const [{ isOver }, drop] = useDrop(() => ({
    accept: 'TASK',
    drop: (item: { id: string }) => {
      handleReparent(item.id, node.id);
    },
    collect: (monitor) => ({
      isOver: monitor.isOver()
    })
  }));

  const handleReparent = async (taskId: string, newParentId: string) => {
    // Prevent circular dependency
    if (isDescendant(newParentId, taskId)) {
      alert('Cannot move parent into its own child');
      return;
    }

    await api.patch(`/api/tasks/${taskId}`, { parentId: newParentId });
  };

  return (
    <Box ref={(el) => drag(drop(el))} sx={{ bgcolor: isOver ? 'action.hover' : 'transparent' }}>
      <TreeItem {...props} />
    </Box>
  );
};
```

---

## 📦 DELIVERABLES

- [ ] Drag & drop reparenting
- [ ] Circular dependency validation
- [ ] Server sync
- [ ] Unit tests (50%+ coverage)

---

**Estimated:** 6 hours
