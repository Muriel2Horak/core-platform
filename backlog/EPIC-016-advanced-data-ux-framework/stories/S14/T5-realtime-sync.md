# T5: Real-time Sync

**Story:** [S14: Miro-style Freeform Board](README.md)  
**Effort:** 15 hours  
**Priority:** P1  
**Dependencies:** T2

---

## 📋 TASK DESCRIPTION

WebSocket real-time updates - entity instance změny propagovat na board.

---

## 🎯 ACCEPTANCE CRITERIA

1. **WebSocket connection** - `/ws/board/{boardId}`
2. **Entity updates** - když se změní Workflow → update card
3. **Presence indicators** - show other users on board
4. **Conflict resolution** - optimistic updates

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/hooks/useBoardSync.ts
import { io } from 'socket.io-client';

export const useBoardSync = (boardId: string) => {
  const [socket, setSocket] = useState<Socket | null>(null);
  const [cards, setCards] = useState<BoardCard[]>([]);

  useEffect(() => {
    const newSocket = io(`/ws/board/${boardId}`);
    
    newSocket.on('entity:updated', (update: EntityUpdate) => {
      setCards(prev => prev.map(card =>
        card.entityId === update.entityId
          ? { ...card, entity: update.entity }
          : card
      ));
    });
    
    newSocket.on('card:moved', (move: CardMove) => {
      setCards(prev => prev.map(card =>
        card.id === move.cardId
          ? { ...card, x: move.x, y: move.y }
          : card
      ));
    });
    
    setSocket(newSocket);
    
    return () => {
      newSocket.disconnect();
    };
  }, [boardId]);

  return { socket, cards };
};
```

### Backend WebSocket

```java
// backend/src/main/java/cz/muriel/core/board/BoardWebSocketController.java
@Controller
public class BoardWebSocketController {
  
  @MessageMapping("/board/{boardId}/move")
  @SendTo("/topic/board/{boardId}")
  public CardMove handleCardMove(@DestinationVariable String boardId, CardMove move) {
    boardService.updateCardPosition(move.cardId, move.x, move.y);
    return move;
  }
}
```

---

## 📦 DELIVERABLES

- [ ] WebSocket integration
- [ ] Entity update events
- [ ] Presence indicators
- [ ] Conflict resolution

---

**Estimated:** 15 hours
