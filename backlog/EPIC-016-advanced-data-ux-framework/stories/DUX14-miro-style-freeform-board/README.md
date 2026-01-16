---
id: S14
epic: EPIC-016-advanced-data-ux-framework
title: "Miro-style Freeform Board"
priority: P2
status: ready
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "80 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-016-advanced-data-ux-framework/stories/DUX14-miro-style-freeform-board/README.md
    - backlog/EPIC-016-advanced-data-ux-framework/README.md
---


# S14: Miro-style Freeform Board

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 🟡 **READY**
**Priority:** 🟡 **P2 - MEDIUM**  
**Effort:** ~80 hours  
**Sprint:** 4-5  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Business Analyst / Process Designer,  
**chci** freeform canvas (jako Miro/FigJam), kde můžu umístit **entity instances** (Users, Workflows, Tenants) kamkoli,  
**abych**:
- Vytvořil **process flow diagram** s User cards a Workflow cards volně rozmístěnými
- Spojil entity **šipkami** (např. User → executes → Workflow → updates → Tenant)
- **Zoomoval a panoval** po nekonečném canvasu s desítkami entit
- Viděl **real-time cursory** spolupracovníků (collaborative editing)
- Exportoval board jako **obrázek nebo PDF** pro dokumentaci

**Use cases:**
- **Workflow mapping**: Rozmístit Workflow cards volně, spojit šipkami (dependencies)
- **Organization chart**: User cards umístěné podle hierarchie, connections = reporting lines
- **Tenant ecosystem**: Tenant cards + jejich Users/Workflows, connections = relationships
- **Process design**: Drag User → Workflow → Tenant cards, design flow
- **System architecture**: Service cards (backend entities) + connections (API calls)

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Infinite Canvas with Zoom & Pan

**GIVEN** otevřu Miro Board view  
**WHEN** interaguji s canvasem  
**THEN** můžu:

**Zoom controls:**

```
Canvas toolbar (top-right):
┌────────────────────────────────────────┐
│ [−] 75% [+]  [Fit to Screen] [100%]   │
└────────────────────────────────────────┘
```

**Interactions:**
- **Mouse wheel:** Scroll up = zoom in, scroll down = zoom out
- **Pinch:** Two-finger pinch on trackpad
- **Pan:** Click and drag empty canvas area (cursor: grab/grabbing)
- **Zoom levels:** 25%, 50%, 75%, 100%, 150%, 200%, 400%

**Implementation:**

```typescript
// frontend/src/components/miro-board/MiroCanvas.tsx
import React, { useRef, useState } from 'react';
import { Stage, Layer } from 'react-konva';
import Konva from 'konva';

export const MiroCanvas: React.FC = () => {
  const stageRef = useRef<Konva.Stage>(null);
  const [stagePos, setStagePos] = useState({ x: 0, y: 0 });
  const [scale, setScale] = useState(1);

  const handleWheel = (e: Konva.KonvaEventObject<WheelEvent>) => {
    e.evt.preventDefault();

    const stage = stageRef.current;
    if (!stage) return;

    const oldScale = stage.scaleX();
    const pointer = stage.getPointerPosition();
    if (!pointer) return;

    // Zoom delta
    const scaleBy = 1.1;
    const newScale = e.evt.deltaY < 0 ? oldScale * scaleBy : oldScale / scaleBy;

    // Clamp zoom
    const clampedScale = Math.max(0.25, Math.min(4, newScale));

    // Zoom to cursor position
    const mousePointTo = {
      x: (pointer.x - stage.x()) / oldScale,
      y: (pointer.y - stage.y()) / oldScale,
    };

    const newPos = {
      x: pointer.x - mousePointTo.x * clampedScale,
      y: pointer.y - mousePointTo.y * clampedScale,
    };

    setScale(clampedScale);
    setStagePos(newPos);
  };

  const handleDragEnd = (e: Konva.KonvaEventObject<DragEvent>) => {
    setStagePos({ x: e.target.x(), y: e.target.y() });
  };

  return (
    <Stage
      ref={stageRef}
      width={window.innerWidth}
      height={window.innerHeight - 64} // Minus toolbar
      scaleX={scale}
      scaleY={scale}
      x={stagePos.x}
      y={stagePos.y}
      draggable
      onWheel={handleWheel}
      onDragEnd={handleDragEnd}
    >
      <Layer>
        {/* Sticky notes render here */}
      </Layer>
    </Stage>
  );
};
```

### AC2: Entity Cards (CRUD)

**GIVEN** canvas otevřený  
**WHEN** kliknu "Add Entity"  
**THEN** otevře se entity picker:

```
┌────────────────────────────────────────┐
│ Add Entity to Board                    │
├────────────────────────────────────────┤
│ Entity Type:                           │
│ ○ User                                 │
│ ● Workflow                             │
│ ○ Tenant                               │
│ ○ Custom Entity                        │
│                                        │
│ Search: [Type to search...]            │
│                                        │
│ Results:                               │
│ ☐ WF-001: Customer Onboarding         │
│ ☐ WF-002: Invoice Processing          │
│ ☑ WF-003: Employee Onboarding         │  ← Selected
│ ☐ WF-004: Approval Workflow           │
│                                        │
│ [Cancel]  [Add to Board]               │
└────────────────────────────────────────┘
```

**Entity card UI (Workflow example):**

```
┌─────────────────────────────────────────┐
│ � WF-003: Employee Onboarding         │  ← Header (entity icon + title)
├─────────────────────────────────────────┤
│ Status: ⚡ Active                       │
│ Owner:  👤 Alice Johnson               │
│ Steps:  5 steps                        │
│ Avg time: 2.5 hours                    │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ [████████░░░░░░░] 60% completed     │ │  ← Mini chart
│ └─────────────────────────────────────┘ │
├─────────────────────────────────────────┤
│ [View Details] [⋮ More]                 │  ← Actions
└─────────────────────────────────────────┘
```

**Entity card UI (User example):**

```
┌─────────────────────────────────────────┐
│ 👤 Alice Johnson                        │
│    alice.johnson@company.com            │
├─────────────────────────────────────────┤
│ Role:     Admin                         │
│ Tenant:   Company A                     │
│ Status:   🟢 Active                     │
│ Last login: 2 hours ago                 │
│                                         │
│ Active workflows: 3                     │
│ Completed tasks: 127                    │
├─────────────────────────────────────────┤
│ [View Profile] [⋮ More]                 │
└─────────────────────────────────────────┘
```

**Entity card data model:**

```typescript
interface EntityCard {
  id: string;
  boardId: string;
  
  // Canvas position
  x: number;
  y: number;
  width: number;
  height: number;
  rotation: number; // Optional slight tilt
  zIndex: number;
  
  // Entity reference
  entityType: 'USER' | 'WORKFLOW' | 'TENANT' | 'CUSTOM';
  entityId: string; // Reference to actual entity
  entityData: {
    // Denormalized data for quick render (synced from entity)
    title: string;
    subtitle?: string;
    status?: string;
    icon?: string;
    metadata?: Record<string, any>; // Entity-specific fields
  };
  
  // Visual customization
  cardStyle: 'compact' | 'detailed' | 'mini'; // Display mode
  showMiniChart?: boolean; // Show embedded chart
  
  createdAt: string;
  updatedAt: string;
}
```

**Entity-specific card variants:**

| Entity Type | Icon | Key Fields | Mini Chart |
|-------------|------|------------|------------|
| **User** | 👤 | Name, Email, Role, Status, Last Login | Activity timeline |
| **Workflow** | 🔄 | Name, Status, Owner, Steps, Avg Time | Completion % |
| **Tenant** | 🏢 | Name, Status, Users Count, Workflows | Usage stats |
| **Custom** | 📦 | Custom fields based on metamodel | Configurable |

**Interactions:**
- **Double-click canvas:** Open entity picker → select entity → place on canvas
- **Drag entity library:** Drag User/Workflow from sidebar → drop on canvas
- **Click card:** Select (show border + resize handles)
- **Double-click card:** Open entity detail popup (S5 Multi-Window)
- **Drag card:** Move freely on canvas
- **Resize:** Drag bottom-right corner (compact ↔ detailed view)
- **Delete:** Click ⋮ More → Remove from Board (entity itself NOT deleted)

**Implementation:**

```typescript
// frontend/src/components/miro-board/EntityCard.tsx
import React, { useState, useRef } from 'react';
import { Group, Rect, Text, Transformer, Image } from 'react-konva';
import Konva from 'konva';

interface EntityCardProps {
  card: EntityCard;
  isSelected: boolean;
  onSelect: () => void;
  onChange: (card: Partial<EntityCard>) => void;
  onDoubleClick: () => void; // Open entity detail popup
}

export const EntityCard: React.FC<EntityCardProps> = ({
  card,
  isSelected,
  onSelect,
  onChange,
  onDoubleClick
}) => {
  const shapeRef = useRef<Konva.Group>(null);
  const trRef = useRef<Konva.Transformer>(null);

  React.useEffect(() => {
    if (isSelected && trRef.current && shapeRef.current) {
      trRef.current.nodes([shapeRef.current]);
      trRef.current.getLayer()?.batchDraw();
    }
  }, [isSelected]);

  const handleDragEnd = (e: Konva.KonvaEventObject<DragEvent>) => {
    onChange({
      x: e.target.x(),
      y: e.target.y(),
    });
  };

  const handleTransformEnd = () => {
    const node = shapeRef.current;
    if (!node) return;

    const scaleX = node.scaleX();
    const scaleY = node.scaleY();

    // Reset scale
    node.scaleX(1);
    node.scaleY(1);

    onChange({
      width: Math.max(150, node.width() * scaleX), // Min 150px
      height: Math.max(100, node.height() * scaleY), // Min 100px
      rotation: node.rotation(),
    });
  };

  // Entity-specific colors
  const colorMap = {
    USER: '#E3F2FD',      // Light blue
    WORKFLOW: '#FFF9C4',  // Light yellow
    TENANT: '#C8E6C9',    // Light green
    CUSTOM: '#F3E5F5',    // Light purple
  };

  return (
    <>
      <Group
        ref={shapeRef}
        x={card.x}
        y={card.y}
        rotation={card.rotation}
        draggable
        onClick={onSelect}
        onDblClick={onDoubleClick} // Open detail popup
        onTap={onSelect}
        onDragEnd={handleDragEnd}
        onTransformEnd={handleTransformEnd}
      >
        {/* Background card */}
        <Rect
          width={card.width}
          height={card.height}
          fill={colorMap[card.entityType]}
          stroke={isSelected ? '#2196F3' : '#BDBDBD'}
          strokeWidth={isSelected ? 3 : 1}
          cornerRadius={8}
          shadowBlur={5}
          shadowOpacity={0.2}
        />

        {/* Entity icon (top-left) */}
        <Text
          text={card.entityData.icon || '📦'}
          fontSize={24}
          x={10}
          y={10}
        />

        {/* Entity title */}
        <Text
          text={card.entityData.title}
          fontSize={16}
          fontStyle="bold"
          fill="#000"
          x={45}
          y={12}
          width={card.width - 55}
        />

        {/* Subtitle (if exists) */}
        {card.entityData.subtitle && (
          <Text
            text={card.entityData.subtitle}
            fontSize={12}
            fill="#666"
            x={45}
            y={35}
            width={card.width - 55}
          />
        )}

        {/* Status badge */}
        {card.entityData.status && (
          <Rect
            x={10}
            y={card.height - 35}
            width={80}
            height={25}
            fill="#4CAF50"
            cornerRadius={4}
          />
        )}
        {card.entityData.status && (
          <Text
            text={card.entityData.status}
            fontSize={12}
            fill="#FFF"
            x={15}
            y={card.height - 30}
          />
        )}
      </Group>

      {isSelected && (
        <Transformer
          ref={trRef}
          boundBoxFunc={(oldBox, newBox) => {
            // Min size
            if (newBox.width < 150 || newBox.height < 100) {
              return oldBox;
            }
            return newBox;
          }}
        />
      )}
    </>
  );
};
```

### AC3: Connections Between Entities

**GIVEN** dvě entity cards na canvasu  
**WHEN** vyberu "Draw Connection"  
**THEN** můžu nakreslit šipku mezi nimi:

**Connection example - Workflow dependencies:**

```
┌──────────────────┐
│ 🔄 WF-001:       │
│ Onboarding       │─────➜ "depends on" ────➜ ┌──────────────────┐
└──────────────────┘                           │ 🔄 WF-002:       │
                                               │ Setup Account    │
                                               └──────────────────┘
```

**Connection example - User to Workflow:**

```
┌──────────────────┐
│ 👤 Alice Johnson │
└──────────────────┘
         │
         ├────➜ "owns" ────➜ 🔄 WF-001: Onboarding
         │
         └────➜ "executes" ─➜ 🔄 WF-003: Approval
```

**Connection types:**

| Connection | Label Example | Use Case |
|------------|---------------|----------|
| **Dependency** | "depends on", "requires" | Workflow → Workflow (prerequisites) |
| **Ownership** | "owns", "manages" | User → Workflow (owner relationship) |
| **Execution** | "executes", "runs" | User → Workflow (executor) |
| **Parent-Child** | "contains", "has" | Tenant → Users (organization) |
| **Data Flow** | "sends data to", "updates" | Workflow → Tenant (data operations) |

**Connection modes:**

```
Toolbar:
┌────────────────────────────────────────┐
│ [➜ Arrow] [➝ Line] [⤴ Curved Arrow]   │
└────────────────────────────────────────┘
```

**Drawing flow:**
1. Click source sticky note
2. Drag to target sticky note
3. Release → arrow drawn

**Arrow types:**

```typescript
interface Connection {
  id: string;
  sourceNoteId: string;
  targetNoteId: string;
  type: 'arrow' | 'line' | 'curved';
  color: string;
  label?: string; // Optional label in middle
  zIndex: number; // Below sticky notes
}
```

**Visual:**

```
┌─────────┐
│ Note A  │─────➜ "needs to" ────➜ ┌─────────┐
└─────────┘                         │ Note B  │
                                    └─────────┘
```

**Implementation:**

```typescript
// frontend/src/components/miro-board/ConnectionArrow.tsx
import React from 'react';
import { Arrow, Text } from 'react-konva';

interface ConnectionArrowProps {
  connection: Connection;
  sourceNote: StickyNote;
  targetNote: StickyNote;
}

export const ConnectionArrow: React.FC<ConnectionArrowProps> = ({
  connection,
  sourceNote,
  targetNote
}) => {
  // Calculate arrow endpoints (center of sticky notes)
  const points = [
    sourceNote.x + sourceNote.width / 2,
    sourceNote.y + sourceNote.height / 2,
    targetNote.x + targetNote.width / 2,
    targetNote.y + targetNote.height / 2,
  ];

  return (
    <>
      <Arrow
        points={points}
        stroke={connection.color || '#000'}
        fill={connection.color || '#000'}
        strokeWidth={2}
        pointerLength={10}
        pointerWidth={10}
      />

      {connection.label && (
        <Text
          text={connection.label}
          x={(points[0] + points[2]) / 2 - 30}
          y={(points[1] + points[3]) / 2 - 10}
          fontSize={12}
          fill="#555"
        />
      )}
    </>
  );
};
```

### AC4: Toolbar & Shape Library

**GIVEN** canvas toolbar  
**WHEN** vyberu tool  
**THEN** můžu přidat různé shapes:

**Toolbar layout:**

```
┌──────────────────────────────────────────────────────────────────────┐
│ [🖐️ Select] [📝 Sticky] [➜ Arrow] [🔲 Rect] [⭕ Circle] [✏️ Draw]  │
│                                                                      │
│ [Yellow ▼] [12pt ▼] [Bold] [Italic] [⋮ More]                       │
└──────────────────────────────────────────────────────────────────────┘
```

**Tools:**

| Tool    | Icon | Action |
|---------|------|--------|
| Select  | 🖐️   | Default mode (select, move, resize) |
| Sticky  | 📝   | Click canvas → create sticky note |
| Arrow   | ➜    | Click source → click target → draw arrow |
| Rectangle | 🔲 | Drag to create rectangle shape |
| Circle  | ⭕   | Drag to create circle shape |
| Draw    | ✏️   | Freehand drawing (pen tool) |

### AC5: Real-time Collaboration

**GIVEN** dva users na stejném boardu  
**WHEN** Alice přesune sticky note  
**THEN** Bob vidí změnu real-time:

**Features:**
- **Live cursors:** Vidím cursory ostatních users (s jménem)
- **Live edits:** Sticky notes se updateují instantly
- **Presence indicators:** Kdo je online (avatars v top-right)

**Implementation (WebSockets):**

```typescript
// frontend/src/hooks/useMiroBoardSync.ts
import { useEffect } from 'react';
import { io, Socket } from 'socket.io-client';

export const useMiroBoardSync = (boardId: string) => {
  const [socket, setSocket] = useState<Socket | null>(null);

  useEffect(() => {
    const newSocket = io('wss://api.example.com', {
      query: { boardId }
    });

    newSocket.on('note:updated', (note: StickyNote) => {
      // Update local state
      updateNote(note);
    });

    newSocket.on('note:created', (note: StickyNote) => {
      addNote(note);
    });

    newSocket.on('note:deleted', (noteId: string) => {
      removeNote(noteId);
    });

    newSocket.on('cursor:moved', (data: { userId: string; x: number; y: number }) => {
      updateUserCursor(data);
    });

    setSocket(newSocket);

    return () => {
      newSocket.close();
    };
  }, [boardId]);

  const emitNoteUpdate = (note: StickyNote) => {
    socket?.emit('note:update', note);
  };

  return { emitNoteUpdate };
};
```

**Backend WebSocket:**

```java
// backend/src/main/java/cz/muriel/core/miro/websocket/MiroBoardWebSocket.java
@ServerEndpoint("/ws/miro-board/{boardId}")
public class MiroBoardWebSocket {

    private static final Map<String, Set<Session>> boardSessions = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session, @PathParam("boardId") String boardId) {
        boardSessions.computeIfAbsent(boardId, k -> new HashSet<>()).add(session);
        logger.info("User joined board: {}", boardId);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("boardId") String boardId) {
        // Broadcast to all other users on this board
        var sessions = boardSessions.get(boardId);
        if (sessions != null) {
            sessions.stream()
                .filter(s -> !s.equals(session))
                .forEach(s -> s.getAsyncRemote().sendText(message));
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("boardId") String boardId) {
        var sessions = boardSessions.get(boardId);
        if (sessions != null) {
            sessions.remove(session);
        }
    }
}
```

### AC6: Export to Image/PDF

**GIVEN** board s obsahem  
**WHEN** kliknu "Export"  
**THEN** můžu stáhnout jako PNG nebo PDF:

**Export dialog:**

```
┌────────────────────────────────────────┐
│ Export Board                           │
├────────────────────────────────────────┤
│ Format:                                │
│ ○ PNG Image (transparent background)  │
│ ● PDF Document                         │
│                                        │
│ Quality:                               │
│ ○ Low (1x)                             │
│ ● Medium (2x)                          │
│ ○ High (4x)                            │
│                                        │
│ Include:                               │
│ ☑ Entity cards                         │
│ ☑ Connections                          │
│ ☑ Labels                               │
│                                        │
│ [Cancel]  [Export]                     │
└────────────────────────────────────────┘
```

**Implementation:**

```typescript
// frontend/src/utils/exportBoard.ts
import { Stage } from 'konva/lib/Stage';

export const exportBoardToPNG = (stage: Stage) => {
  const uri = stage.toDataURL({
    pixelRatio: 2, // 2x quality
  });

  // Download
  const link = document.createElement('a');
  link.download = 'entity-board.png'; // Changed from 'miro-board.png'
  link.href = uri;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

export const exportBoardToPDF = async (stage: Stage) => {
  const { jsPDF } = await import('jspdf');
  const uri = stage.toDataURL({ pixelRatio: 2 });

  const pdf = new jsPDF('landscape', 'mm', 'a4');
  pdf.addImage(uri, 'PNG', 10, 10, 277, 190); // A4 landscape size
  pdf.save('entity-board.pdf'); // Changed from 'miro-board.pdf'
};
```

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Canvas Component with Zoom/Pan** (20h)

**Deliverable:**
- react-konva Stage with infinite canvas
- Mouse wheel zoom
- Drag to pan
- Zoom controls (toolbar)
- Fit to screen button

**Tech stack:**
- `react-konva` - Canvas rendering
- `konva` - Core canvas library

---

#### **T2: Entity Card CRUD** (15h)

**Deliverable:**
- EntityCard component (draggable, resizable)
- Entity picker dialog (search Users, Workflows, Tenants)
- Entity-specific card rendering (User card, Workflow card, Tenant card)
- Double-click to open detail popup (S5 integration)
- Remove from board (entity NOT deleted)
- Backend API for board persistence

**Backend:**

```java
// backend/src/main/java/cz/muriel/core/miro/model/EntityCard.java
@Entity
@Table(name = "miro_entity_cards")
public class EntityCard {
    @Id
    @GeneratedValue
    private Long id;

    private String boardId;
    
    // Canvas position
    private Double x;
    private Double y;
    private Integer width;
    private Integer height;
    private Double rotation;
    
    // Entity reference
    @Enumerated(EnumType.STRING)
    private EntityType entityType; // USER, WORKFLOW, TENANT, CUSTOM
    
    private String entityId; // Reference to actual entity
    
    // Denormalized data (synced from entity for quick render)
    @Column(columnDefinition = "jsonb")
    private String entityDataJson;
    
    private Long createdById;
    private LocalDateTime createdAt;
}
```

---

#### **T3: Connection Arrows** (10h)

**Deliverable:**
- Draw arrow between sticky notes
- Arrow types: straight, curved
- Optional label on arrow
- Delete connection

---

#### **T4: Shape Library** (10h)

**Deliverable:**
- Toolbar with tools (select, sticky, arrow, rect, circle, draw)
- Rectangle and circle shapes
- Freehand drawing tool (pen)

---

#### **T5: Real-time Sync (WebSockets)** (15h)

**Deliverable:**
- WebSocket connection per board
- Broadcast entity card updates to all users
- Live cursors with user names
- Presence indicators (who's online)
- Real-time sync when entity data changes (e.g., Workflow status updated → card refreshes)

---

#### **T6: Export to Image/PDF** (6h)

**Deliverable:**
- Export canvas to PNG (Konva `toDataURL`)
- Export to PDF (jsPDF)
- Export dialog with options

---

#### **T7: Testing** (4h)

**E2E tests:**

```typescript
// e2e/specs/miro-board/canvas.spec.ts
test('Add entity card to canvas', async ({ page }) => {
  await page.goto('/miro-board/123');

  // Open entity picker
  await page.click('button:has-text("Add Entity")');
  
  // Select Workflow
  await page.click('input[value="WORKFLOW"]');
  await page.fill('input[placeholder="Type to search..."]', 'Onboarding');
  await page.click('text=WF-001: Customer Onboarding');
  await page.click('button:has-text("Add to Board")');

  // Verify entity card created
  await expect(page.locator('text=WF-001: Customer Onboarding')).toBeVisible();
});

test('Draw arrow between entity cards', async ({ page }) => {
  // Add two entity cards first
  // ...

  // Click arrow tool
  await page.click('button:has-text("Arrow")');

  // Click source entity
  await page.click('text=WF-001');

  // Click target entity
  await page.click('text=WF-002');

  // Verify arrow drawn with label
  await expect(page.locator('text=depends on')).toBeVisible();
});
  await expect(page.locator('svg line')).toBeVisible();
});

test('Real-time collaboration', async ({ page, context }) => {
  // User A
  await page.goto('/miro-board/123');
  await page.click('button:has-text("Add Entity")');
  // ... add entity card

  // User B (new tab)
  const page2 = await context.newPage();
  await page2.goto('/miro-board/123');

  // Verify User B sees entity card created by User A
  await expect(page2.locator('text=WF-001')).toBeVisible();
});

test('Double-click entity card opens detail popup', async ({ page }) => {
  await page.goto('/miro-board/123');
  
  // Assume entity card already exists
  await page.dblclick('text=WF-001: Customer Onboarding');
  
  // Verify detail popup opened (S5 Multi-Window integration)
  await expect(page.locator('[role="dialog"] >> text=Customer Onboarding')).toBeVisible();
});
```

---

## 📊 SUCCESS METRICS

- ✅ Canvas render < 500ms (100 entity cards)
- ✅ Zoom/pan latency < 50ms
- ✅ Real-time sync latency < 200ms
- ✅ Export to PNG < 2s
- ✅ 40%+ users create at least 1 entity board
- ✅ **Entity card load time < 300ms** (fetch + render entity data)
- ✅ **Double-click to detail popup < 500ms** (S5 integration)

---

## 🔗 DEPENDENCIES

- **S1:** DataView (entities to visualize)
- **EPIC-003:** RBAC (board permissions)
- **Libraries:**
  - `react-konva` (canvas rendering)
  - `konva` (core library)
  - `socket.io` (WebSockets)
  - `jspdf` (PDF export)

---

## 🎨 DESIGN INSPIRATION

**Reference apps:**
- **Miro** (https://miro.com) - Infinite canvas, collaboration
- **FigJam** (https://www.figma.com/figjam/) - Collaborative whiteboard
- **Lucidchart** (https://www.lucidchart.com) - Entity relationship diagrams
- **Draw.io** (https://www.drawio.com) - Flowchart tool

**Key UX patterns:**
- Infinite canvas (no boundaries)
- Zoom to cursor (like Google Maps)
- Entity cards feel "physical" (shadows, slight rotation)
- Real-time cursors with names (like Figma)
- **Entity-specific rendering** (User card ≠ Workflow card ≠ Tenant card)

---

**Status:** 📋 TODO  
**Effort:** ~80 hours (~2 sprints)  
**Next:** S15 (Task Breakdown)
