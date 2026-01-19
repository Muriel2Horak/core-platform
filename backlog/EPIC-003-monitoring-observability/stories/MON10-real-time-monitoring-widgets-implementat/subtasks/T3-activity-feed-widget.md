# T3: Activity Feed Widget
**Effort:** ~2h | **LOC:** ~200

## Goal
Zobrazit live activity feed s WebSocket eventy.

## Files
- `frontend/src/components/monitoring/ActivityFeedWidget.tsx`
- `backend/src/main/java/cz/muriel/core/monitoring/ActivityEventPublisher.java`

## Tasks
- [ ] Implementovat event publisher (error, deploy, incident).
- [ ] Pridat widget s virtualizaci a max 50 items.
- [ ] Doplnit fade-in animaci a severity styling.
- [ ] Osetrit duplikaty a ordering.

## Output
- Activity feed widget s real-time eventy.

## Acceptance Criteria
- WebSocket live events fungují
- Fade-in animation
- Max 50 items (scroll virtualization)
- Events appear instantly
- Performance (no lag)
