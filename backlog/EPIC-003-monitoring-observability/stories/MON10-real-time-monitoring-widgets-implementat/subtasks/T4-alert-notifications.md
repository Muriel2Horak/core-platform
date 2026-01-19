# T4: Alert Notifications
**Effort:** ~2h | **LOC:** ~300

## Goal
Zajistit desktop notifikace pro alerty a thresholdy.

## Files
- `frontend/src/utils/notifications.ts`
- `frontend/src/hooks/useMetricAlerts.ts`

## Tasks
- [ ] Implementovat Notification API s permission flow.
- [ ] Pridat sound/vibration toggle (optional).
- [ ] Vytvorit UI pro thresholdy a jejich toggling.
- [ ] Osetrit fallback (in-app toast) pri blokaci.

## Output
- Notifikace pro alerty v realnem case.

## Acceptance Criteria
- Desktop notifications (Notification API)
- Permission request on load
- Sound alerts (optional)
- Threshold configuration UI
- Notifications work cross-browser
