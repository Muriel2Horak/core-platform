# T4: Context Menus

**Story:** [S9: Enhanced Tile Actions & Workflows](README.md)  
**Effort:** 15 hours  
**Priority:** P1  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Right-click context menu na tiles.

---

## 🎯 ACCEPTANCE CRITERIA

1. Right-click opens menu
2. Dynamic actions
3. Keyboard navigation
4. Close on outside click

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/context-menu/TileContextMenu.tsx
export const TileContextMenu: React.FC<{ entity: any; actions: Action[] }> = ({ entity, actions }) => {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  
  const handleContextMenu = (e: React.MouseEvent) => {
    e.preventDefault();
    setAnchorEl(e.currentTarget as HTMLElement);
  };
  
  return (
    <div onContextMenu={handleContextMenu}>
      <Tile entity={entity} />
      
      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={() => setAnchorEl(null)}>
        {actions.map(action => (
          <MenuItem key={action.id} onClick={() => {
            action.handler(entity);
            setAnchorEl(null);
          }}>
            <ListItemIcon>{action.icon}</ListItemIcon>
            <ListItemText>{action.label}</ListItemText>
          </MenuItem>
        ))}
      </Menu>
    </div>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] Context menu
- [ ] Dynamic actions
- [ ] Keyboard support

---

**Estimated:** 15 hours
