# T3: Connection Arrows

**Story:** [S14: Miro-style Freeform Board](README.md)  
**Effort:** 10 hours  
**Priority:** P1  
**Dependencies:** T2

---

## 📋 TASK DESCRIPTION

Kreslit šipky mezi entity cards - click card 1 → click card 2 → arrow.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Arrow tool** - toolbar button "Add Arrow"
2. **Two-click flow** - click source → click target → creates arrow
3. **Persist** - uložit do `BoardConnection` entity
4. **Delete** - click arrow → delete button

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/board/ConnectionArrow.tsx
import { Arrow } from 'react-konva';

interface ConnectionArrowProps {
  from: { x: number; y: number };
  to: { x: number; y: number };
  onClick: () => void;
}

export const ConnectionArrow: React.FC<ConnectionArrowProps> = ({ from, to, onClick }) => {
  return (
    <Arrow
      points={[from.x, from.y, to.x, to.y]}
      stroke="#333"
      strokeWidth={2}
      fill="#333"
      pointerLength={10}
      pointerWidth={10}
      onClick={onClick}
    />
  );
};
```

### Backend Entity

```java
@Entity
public class BoardConnection {
  @Id
  private UUID id;
  
  @ManyToOne
  private BoardCard sourceCard;
  
  @ManyToOne
  private BoardCard targetCard;
  
  private String label;
}
```

---

## 📦 DELIVERABLES

- [ ] ConnectionArrow component
- [ ] Two-click creation flow
- [ ] BoardConnection entity + API
- [ ] Delete functionality

---

**Estimated:** 10 hours
