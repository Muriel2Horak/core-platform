# T1: Popup Window System

**Story:** [S5: Multi-Window Editing](README.md)  
**Effort:** 25 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Draggable/resizable popup windows - základ pro multi-window.

---

## 🎯 ACCEPTANCE CRITERIA

1. Draggable popups (rnd library)
2. Resizable
3. Minimize/maximize/close
4. Bring to front on click

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/common/PopupWindow.tsx
import { Rnd } from 'react-rnd';

interface PopupWindowProps {
  id: string;
  title: string;
  children: ReactNode;
  onClose: () => void;
}

export const PopupWindow: React.FC<PopupWindowProps> = ({ id, title, children, onClose }) => {
  const [position, setPosition] = useState({ x: 100, y: 100 });
  const [size, setSize] = useState({ width: 600, height: 400 });
  
  return (
    <Rnd
      position={position}
      size={size}
      onDragStop={(e, d) => setPosition({ x: d.x, y: d.y })}
      onResizeStop={(e, dir, ref, delta, position) => {
        setSize({ width: ref.offsetWidth, height: ref.offsetHeight });
        setPosition(position);
      }}
      minWidth={300}
      minHeight={200}
    >
      <Paper elevation={8} sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <Box className="window-header" sx={{ cursor: 'move', bgcolor: 'primary.main', color: 'white', p: 1 }}>
          <Typography variant="h6">{title}</Typography>
          <IconButton onClick={onClose}><CloseIcon /></IconButton>
        </Box>
        <Box sx={{ flex: 1, overflow: 'auto', p: 2 }}>
          {children}
        </Box>
      </Paper>
    </Rnd>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] PopupWindow component
- [ ] Drag & drop
- [ ] Resize
- [ ] Unit tests

---

**Estimated:** 25 hours
