# T1: Tree View Component

**Story:** [S15: Task Breakdown](README.md)  
**Effort:** 12 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 TASK DESCRIPTION

Implementovat rekurzivní TreeNode component pro hierarchii (Epic → Story → Task → Subtask).

---

## 🎯 ACCEPTANCE CRITERIA

1. **Recursive rendering** - TreeNode obsahuje children TreeNodes
2. **Expand/collapse** - ▶/▼ button
3. **Indentation** - visual hierarchy
4. **Max 4 levels** - Epic → Story → Task → Subtask

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/breakdown/TreeNode.tsx
import { TreeItem } from '@mui/x-tree-view';

interface TreeNodeProps {
  node: HierarchicalTask;
  level: number;
  onExpand: (id: string) => void;
  onCollapse: (id: string) => void;
}

export const TreeNode: React.FC<TreeNodeProps> = ({ node, level, onExpand, onCollapse }) => {
  const [expanded, setExpanded] = useState(false);

  const handleToggle = () => {
    if (expanded) {
      onCollapse(node.id);
    } else {
      onExpand(node.id);
    }
    setExpanded(!expanded);
  };

  return (
    <TreeItem
      nodeId={node.id}
      label={
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          {node.children?.length > 0 && (
            <IconButton size="small" onClick={handleToggle}>
              {expanded ? <ExpandMore /> : <ChevronRight />}
            </IconButton>
          )}
          
          {getTypeIcon(node.type)}
          
          <Typography variant="body2">
            {node.id}: {node.title}
          </Typography>
          
          <Chip label={node.status} size="small" />
        </Box>
      }
      sx={{ ml: level * 3 }}
    >
      {expanded && node.children?.map(child => (
        <TreeNode key={child.id} node={child} level={level + 1} />
      ))}
    </TreeItem>
  );
};

const getTypeIcon = (type: TaskType) => {
  switch (type) {
    case 'EPIC': return <Folder />;
    case 'STORY': return <Description />;
    case 'TASK': return <CheckBox />;
    case 'SUBTASK': return <SubdirectoryArrowRight />;
  }
};
```

---

## 📦 DELIVERABLES

- [ ] TreeNode component
- [ ] Recursive rendering
- [ ] Expand/collapse logic
- [ ] Unit tests (60%+ coverage)

---

**Estimated:** 12 hours
