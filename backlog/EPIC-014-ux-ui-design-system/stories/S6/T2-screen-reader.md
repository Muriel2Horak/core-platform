# T2: Screen Reader Support

**Parent Story:** S6 - Accessibility (WCAG 2.1 AA)  
**LOC:** ~200 | **Effort:** ~3h

## Objective
Add ARIA labels, roles, and live regions for screen reader compatibility.

## Implementation

```tsx
// frontend/src/components/accessibility/LiveRegion.tsx
export const LiveRegion: React.FC<{ message: string; politeness?: 'polite' | 'assertive' }> = ({
  message,
  politeness = 'polite',
}) => (
  <div
    role="status"
    aria-live={politeness}
    aria-atomic="true"
    style={{ position: 'absolute', left: '-9999px' }}
  >
    {message}
  </div>
);

// Usage in components
<Button
  aria-label="Delete user"
  aria-describedby="delete-description"
  onClick={handleDelete}
>
  <DeleteIcon />
</Button>
<span id="delete-description" hidden>
  This will permanently delete the user
</span>
```

## Acceptance Criteria
- [ ] All images have alt text
- [ ] Buttons have aria-label
- [ ] Form inputs have labels
- [ ] Live regions for notifications
- [ ] Landmarks (header, main, nav, footer)
- [ ] ARIA roles on custom widgets

## Files
- `frontend/src/components/accessibility/LiveRegion.tsx`
- `frontend/src/utils/ariaLabels.ts`
