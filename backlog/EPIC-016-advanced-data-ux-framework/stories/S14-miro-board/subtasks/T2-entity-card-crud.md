# T2: Entity Card CRUD

**Story:** [S14: Miro-style Freeform Board](README.md)  
**Effort:** 15 hours  
**Priority:** P0  
**Dependencies:** T1

---

## 📋 TASK DESCRIPTION

Drag entity instances (Workflow, User, Tenant, Ticket) z entity list na canvas, uložit XY pozice.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Entity sidebar** - list všech entit (filtrovatelný)
2. **Drag to canvas** - drag entity → vytvoří kartu s XY pozicí
3. **Persist position** - uložit do `BoardCard` entity
4. **Double-click** - edit entity modal

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/board/EntityCard.tsx
import { Rect, Text, Group } from 'react-konva';

interface EntityCardProps {
  entity: EntityInstance;  // Workflow, User, Tenant, Ticket
  x: number;
  y: number;
  onDragEnd: (x: number, y: number) => void;
  onDoubleClick: () => void;
}

export const EntityCard: React.FC<EntityCardProps> = ({
  entity,
  x,
  y,
  onDragEnd,
  onDoubleClick
}) => {
  return (
    <Group
      x={x}
      y={y}
      draggable
      onDragEnd={(e) => onDragEnd(e.target.x(), e.target.y())}
      onDblClick={onDoubleClick}
    >
      {/* Card background */}
      <Rect
        width={200}
        height={100}
        fill={getEntityColor(entity.type)}
        stroke="#333"
        strokeWidth={2}
        cornerRadius={8}
      />
      
      {/* Entity icon + name */}
      <Text
        x={10}
        y={10}
        text={`${getEntityIcon(entity.type)} ${entity.name}`}
        fontSize={14}
        fontStyle="bold"
      />
      
      {/* Entity details */}
      <Text
        x={10}
        y={30}
        text={`ID: ${entity.id}`}
        fontSize={12}
        fill="#666"
      />
    </Group>
  );
};

const getEntityColor = (type: EntityType) => {
  switch (type) {
    case 'WORKFLOW': return '#e3f2fd';
    case 'USER': return '#f3e5f5';
    case 'TENANT': return '#e8f5e9';
    case 'TICKET': return '#fff3e0';
    default: return '#f5f5f5';
  }
};
```

### Backend Entity

```java
// backend/src/main/java/cz/muriel/core/board/BoardCard.java
@Entity
public class BoardCard {
  @Id
  private UUID id;
  
  @ManyToOne
  private Board board;
  
  private String entityType;  // "WORKFLOW", "USER", etc.
  private UUID entityId;
  
  private Double x;
  private Double y;
  
  private Integer zIndex;
}
```

---

## 📦 DELIVERABLES

- [ ] EntityCard component
- [ ] Drag from sidebar to canvas
- [ ] BoardCard entity + API
- [ ] Position persistence

---

**Estimated:** 15 hours
