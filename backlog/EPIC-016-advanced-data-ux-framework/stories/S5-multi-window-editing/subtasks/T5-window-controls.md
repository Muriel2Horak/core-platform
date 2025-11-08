# T5: Window Controls

**Story:** [S5: Multi-Window Editing](README.md)  
**Effort:** 10 hours  
**Priority:** P2  
**Dependencies:** T1, T2

---

## 📋 OBJECTIVE

Minimize, maximize, close buttons + keyboard shortcuts.

---

## 🎯 ACCEPTANCE CRITERIA

1. Minimize button (hide, show in taskbar)
2. Maximize button (fullscreen toggle)
3. Close button
4. Keyboard shortcuts (Ctrl+W close, Ctrl+M minimize)

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/common/PopupWindow.tsx (extension)
export const PopupWindow: React.FC<PopupWindowProps> = ({ id, title, children, onClose }) => {
  const [isMinimized, setIsMinimized] = useState(false);
  const [isMaximized, setIsMaximized] = useState(false);
  
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.key === 'w') {
        e.preventDefault();
        onClose();
      }
      if (e.ctrlKey && e.key === 'm') {
        e.preventDefault();
        setIsMinimized(!isMinimized);
      }
    };
    
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isMinimized]);
  
  if (isMinimized) {
    return <WindowTaskbarItem id={id} title={title} onClick={() => setIsMinimized(false)} />;
  }
  
  return (
    <Rnd {...rndProps} size={isMaximized ? { width: '100vw', height: '100vh' } : size}>
      <Paper>
        <Box className="window-header">
          <Typography>{title}</Typography>
          <IconButton onClick={() => setIsMinimized(true)}><MinimizeIcon /></IconButton>
          <IconButton onClick={() => setIsMaximized(!isMaximized)}>
            {isMaximized ? <RestoreIcon /> : <MaximizeIcon />}
          </IconButton>
          <IconButton onClick={onClose}><CloseIcon /></IconButton>
        </Box>
        {/* ... */}
      </Paper>
    </Rnd>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] Minimize/maximize/close
- [ ] Keyboard shortcuts
- [ ] Taskbar for minimized
- [ ] Unit tests

---

**Estimated:** 10 hours
