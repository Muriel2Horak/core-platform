# T1: Canvas Component

**Story:** [S14: Miro-style Freeform Board](README.md)  
**Effort:** 20 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 TASK DESCRIPTION

Implementovat `BoardCanvas` komponent s react-konva - zoom, pan, grid, infinite canvas.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Infinite canvas** - scroll beyond initial viewport
2. **Zoom** - mouse wheel zoom (50%-200%)
3. **Pan** - drag canvas s middle mouse button
4. **Grid** - dotted grid 50x50px
5. **Toolbar** - zoom reset, fit to screen

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/board/BoardCanvas.tsx
import Konva from 'konva';
import { Stage, Layer, Rect, Circle } from 'react-konva';

interface BoardCanvasProps {
  width: number;
  height: number;
  children: React.ReactNode;
}

export const BoardCanvas: React.FC<BoardCanvasProps> = ({ width, height, children }) => {
  const [scale, setScale] = useState(1);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const stageRef = useRef<Konva.Stage>(null);

  // ✅ Zoom with mouse wheel
  const handleWheel = (e: Konva.KonvaEventObject<WheelEvent>) => {
    e.evt.preventDefault();
    
    const scaleBy = 1.1;
    const stage = e.target.getStage();
    const pointer = stage.getPointerPosition();
    
    const newScale = e.evt.deltaY > 0 ? scale / scaleBy : scale * scaleBy;
    const clampedScale = Math.max(0.5, Math.min(2, newScale));
    
    setScale(clampedScale);
    
    // Adjust position to zoom towards mouse
    const mousePointTo = {
      x: (pointer.x - position.x) / scale,
      y: (pointer.y - position.y) / scale
    };
    
    setPosition({
      x: pointer.x - mousePointTo.x * clampedScale,
      y: pointer.y - mousePointTo.y * clampedScale
    });
  };

  // ✅ Pan with drag
  const handleDragEnd = (e: Konva.KonvaEventObject<DragEvent>) => {
    setPosition({
      x: e.target.x(),
      y: e.target.y()
    });
  };

  // ✅ Grid rendering
  const renderGrid = () => {
    const gridSize = 50;
    const gridDots = [];
    
    for (let x = 0; x < width; x += gridSize) {
      for (let y = 0; y < height; y += gridSize) {
        gridDots.push(
          <Circle
            key={`${x}-${y}`}
            x={x}
            y={y}
            radius={1}
            fill="#ccc"
          />
        );
      }
    }
    
    return gridDots;
  };

  return (
    <Box sx={{ position: 'relative' }}>
      {/* Toolbar */}
      <Box sx={{ position: 'absolute', top: 10, right: 10, zIndex: 1 }}>
        <ButtonGroup variant="outlined" size="small">
          <Button onClick={() => setScale(1)}>100%</Button>
          <Button onClick={() => setScale(scale * 1.2)}>+</Button>
          <Button onClick={() => setScale(scale / 1.2)}>-</Button>
          <Button onClick={handleFitToScreen}>Fit</Button>
        </ButtonGroup>
        <Typography variant="caption" sx={{ ml: 1 }}>
          {Math.round(scale * 100)}%
        </Typography>
      </Box>

      {/* Canvas */}
      <Stage
        ref={stageRef}
        width={width}
        height={height}
        scaleX={scale}
        scaleY={scale}
        x={position.x}
        y={position.y}
        draggable
        onWheel={handleWheel}
        onDragEnd={handleDragEnd}
      >
        {/* Grid layer */}
        <Layer>
          {renderGrid()}
        </Layer>
        
        {/* Content layer */}
        <Layer>
          {children}
        </Layer>
      </Stage>
    </Box>
  );
};
```

---

## ✅ TESTING

```typescript
test('zooms in/out with mouse wheel', () => {
  render(<BoardCanvas width={800} height={600} />);
  
  const stage = screen.getByTestId('konva-stage');
  
  fireEvent.wheel(stage, { deltaY: -100 });
  expect(stage).toHaveStyle({ transform: 'scale(1.1)' });
  
  fireEvent.wheel(stage, { deltaY: 100 });
  expect(stage).toHaveStyle({ transform: 'scale(1)' });
});
```

---

## 📦 DELIVERABLES

- [ ] BoardCanvas component
- [ ] Zoom/pan functionality
- [ ] Grid rendering
- [ ] Toolbar UI
- [ ] Unit tests (50%+ coverage)

---

**Estimated:** 20 hours
