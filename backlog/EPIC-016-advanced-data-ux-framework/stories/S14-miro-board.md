# S14: Miro-style Freeform Board

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🟡 **P2 - MEDIUM**  
**Effort:** ~80 hours  
**Sprint:** 4-5  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Creative Project Manager / Designer,  
**chci** freeform canvas (jako Miro/FigJam), kde můžu umístit sticky notes kamkoli,  
**abych**:
- Vytvořil **brainstorming board** s lístečky umístěnými volně
- Spojil poznámky **šipkami** (connections)
- **Zoomoval a panoval** po nekonečném canvasu
- Viděl **real-time cursory** spolupracovníků (collaborative editing)
- Exportoval board jako **obrázek nebo PDF**

**Use cases:**
- Brainstorming session (sticky notes s nápady)
- Sprint retrospective (What went well / What to improve)
- Mind mapping (centrální nápad → větvení)
- User journey mapping (user actions → touchpoints)

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

### AC2: Sticky Notes (CRUD)

**GIVEN** canvas otevřený  
**WHEN** kliknu "Add Sticky Note"  
**THEN** vytvoří se sticky note:

**Sticky note UI:**

```
┌─────────────────────────────────┐
│ 📝 Sticky Note       [Color ▼] │  ← Title bar (draggable)
├─────────────────────────────────┤
│                                 │
│ User needs better onboarding   │  ← Editable text
│                                 │
│                                 │
├─────────────────────────────────┤
│ [👤 Alice]  [⋮ More]           │  ← Footer (author, actions)
└─────────────────────────────────┘
```

**Sticky note variants:**

```typescript
interface StickyNote {
  id: string;
  x: number;
  y: number;
  width: number;
  height: number;
  color: 'yellow' | 'pink' | 'blue' | 'green' | 'purple'; // Like Miro
  text: string;
  authorId: string;
  rotation: number; // Degrees (-15 to +15 for organic look)
  zIndex: number; // Layering
  createdAt: string;
  updatedAt: string;
}
```

**Color palette:**

| Color  | Hex     | Use Case |
|--------|---------|----------|
| Yellow | #FFF9C4 | Ideas, general notes |
| Pink   | #F8BBD0 | Issues, blockers |
| Blue   | #BBDEFB | Actions, tasks |
| Green  | #C8E6C9 | Wins, positives |
| Purple | #E1BEE7 | Questions |

**Interactions:**
- **Double-click canvas:** Create new sticky at cursor position
- **Click sticky:** Select (show border + resize handles)
- **Double-click sticky:** Edit text (contenteditable)
- **Drag sticky:** Move freely on canvas
- **Rotate:** Small rotate handle at top-right corner
- **Resize:** Drag bottom-right corner (maintain aspect ratio)
- **Delete:** Click ⋮ More → Delete

**Implementation:**

```typescript
// frontend/src/components/miro-board/StickyNote.tsx
import React, { useState, useRef } from 'react';
import { Group, Rect, Text, Transformer } from 'react-konva';
import Konva from 'konva';

interface StickyNoteProps {
  note: StickyNote;
  isSelected: boolean;
  onSelect: () => void;
  onChange: (note: Partial<StickyNote>) => void;
}

export const StickyNote: React.FC<StickyNoteProps> = ({
  note,
  isSelected,
  onSelect,
  onChange
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
      width: Math.max(5, node.width() * scaleX),
      height: Math.max(5, node.height() * scaleY),
      rotation: node.rotation(),
    });
  };

  const colorMap = {
    yellow: '#FFF9C4',
    pink: '#F8BBD0',
    blue: '#BBDEFB',
    green: '#C8E6C9',
    purple: '#E1BEE7',
  };

  return (
    <>
      <Group
        ref={shapeRef}
        x={note.x}
        y={note.y}
        rotation={note.rotation}
        draggable
        onClick={onSelect}
        onTap={onSelect}
        onDragEnd={handleDragEnd}
        onTransformEnd={handleTransformEnd}
      >
        {/* Background rect */}
        <Rect
          width={note.width}
          height={note.height}
          fill={colorMap[note.color]}
          stroke={isSelected ? '#2196F3' : '#000'}
          strokeWidth={isSelected ? 2 : 1}
          shadowBlur={5}
          shadowOpacity={0.3}
        />

        {/* Text */}
        <Text
          text={note.text}
          fontSize={14}
          fontFamily="Arial"
          fill="#000"
          width={note.width - 20}
          height={note.height - 20}
          padding={10}
          align="left"
          verticalAlign="top"
        />
      </Group>

      {isSelected && (
        <Transformer
          ref={trRef}
          boundBoxFunc={(oldBox, newBox) => {
            // Min size
            if (newBox.width < 100 || newBox.height < 80) {
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

### AC3: Connections (Arrows Between Notes)

**GIVEN** dvě sticky notes na canvasu  
**WHEN** vyberu "Draw Connection"  
**THEN** můžu nakreslit šipku mezi nimi:

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
│ ☑ Sticky notes                         │
│ ☑ Connections                          │
│ ☑ Shapes                               │
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
  link.download = 'miro-board.png';
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
  pdf.save('miro-board.pdf');
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

#### **T2: Sticky Note CRUD** (15h)

**Deliverable:**
- StickyNote component (draggable, resizable, rotatable)
- Create sticky on double-click
- Edit text inline (contenteditable)
- Color picker (5 colors)
- Delete sticky
- Backend API for persistence

**Backend:**

```java
// backend/src/main/java/cz/muriel/core/miro/model/StickyNote.java
@Entity
@Table(name = "miro_sticky_notes")
public class StickyNote {
    @Id
    @GeneratedValue
    private Long id;

    private String boardId;
    private Double x;
    private Double y;
    private Integer width;
    private Integer height;
    private String color;
    private String text;
    private Double rotation;
    private Long authorId;
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
- Broadcast note updates to all users
- Live cursors with user names
- Presence indicators (who's online)

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
test('Create sticky note on canvas', async ({ page }) => {
  await page.goto('/miro-board/123');

  // Double-click canvas
  await page.dblclick('canvas', { position: { x: 200, y: 300 } });

  // Verify sticky created
  await expect(page.locator('text=New Note')).toBeVisible();
});

test('Draw arrow between sticky notes', async ({ page }) => {
  // Create two stickies first
  // ...

  // Click arrow tool
  await page.click('button:has-text("Arrow")');

  // Click source sticky
  await page.click('text=Note A');

  // Click target sticky
  await page.click('text=Note B');

  // Verify arrow drawn
  await expect(page.locator('svg line')).toBeVisible();
});

test('Real-time collaboration', async ({ page, context }) => {
  // User A
  await page.goto('/miro-board/123');
  await page.dblclick('canvas');

  // User B (new tab)
  const page2 = await context.newPage();
  await page2.goto('/miro-board/123');

  // Verify User B sees sticky note created by User A
  await expect(page2.locator('text=New Note')).toBeVisible();
});
```

---

## 📊 SUCCESS METRICS

- ✅ Canvas render < 500ms (100 stickies)
- ✅ Zoom/pan latency < 50ms
- ✅ Real-time sync latency < 200ms
- ✅ Export to PNG < 2s
- ✅ 40%+ users create at least 1 Miro board

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
- **Miro** (https://miro.com) - Infinite canvas, sticky notes, connections
- **FigJam** (https://www.figma.com/figjam/) - Collaborative whiteboard
- **Excalidraw** (https://excalidraw.com) - Simple drawing tool

**Key UX patterns:**
- Infinite canvas (no boundaries)
- Zoom to cursor (like Google Maps)
- Sticky notes feel "physical" (rotation, shadows)
- Real-time cursors with names (like Figma)

---

**Status:** 📋 TODO  
**Effort:** ~80 hours (~2 sprints)  
**Next:** S15 (Task Breakdown)
