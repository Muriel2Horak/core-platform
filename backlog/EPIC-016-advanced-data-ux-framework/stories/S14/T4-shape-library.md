# T4: Shape Library

**Story:** [S14: Miro-style Freeform Board](README.md)  
**Effort:** 10 hours  
**Priority:** P2  
**Dependencies:** T1

---

## 📋 TASK DESCRIPTION

Toolbar s geometrickými tvary (rectangle, circle, line, text).

---

## 🎯 ACCEPTANCE CRITERIA

1. **Shape toolbar** - rect, circle, line, text buttons
2. **Click to add** - click canvas → adds shape
3. **Persist** - uložit do `BoardShape` entity
4. **Edit** - resize, rotate, color picker

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/board/ShapeToolbar.tsx
import { Rect, Circle, Line, Text } from 'react-konva';

const shapes = ['RECT', 'CIRCLE', 'LINE', 'TEXT'];

export const ShapeToolbar: React.FC = ({ onShapeSelect }) => {
  return (
    <Box>
      {shapes.map(shape => (
        <IconButton key={shape} onClick={() => onShapeSelect(shape)}>
          {getShapeIcon(shape)}
        </IconButton>
      ))}
    </Box>
  );
};
```

### Backend Entity

```java
@Entity
public class BoardShape {
  @Id
  private UUID id;
  
  @ManyToOne
  private Board board;
  
  private String type;  // "RECT", "CIRCLE", etc.
  private Double x, y, width, height, radius;
  private String color;
}
```

---

## 📦 DELIVERABLES

- [ ] Shape toolbar
- [ ] Shape rendering
- [ ] BoardShape entity + API
- [ ] Edit/resize logic

---

**Estimated:** 10 hours
