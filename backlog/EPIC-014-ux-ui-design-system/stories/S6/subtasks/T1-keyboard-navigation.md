# T1: Keyboard Navigation

**Parent Story:** S6 - Accessibility (WCAG 2.1 AA)  
**LOC:** ~200 | **Effort:** ~3h

## Objective
Implement full keyboard navigation support with logical tab order.

## Implementation

```tsx
// frontend/src/components/accessibility/SkipLinks.tsx
export const SkipLinks: React.FC = () => (
  <Box
    sx={{
      position: 'absolute',
      left: '-9999px',
      '&:focus': {
        left: 0,
        top: 0,
        zIndex: 9999,
        padding: 2,
        backgroundColor: 'background.paper',
      },
    }}
  >
    <Link href="#main-content">Skip to main content</Link>
    <Link href="#navigation">Skip to navigation</Link>
  </Box>
);

// Keyboard event handler
const handleKeyDown = (e: React.KeyboardEvent) => {
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault();
    onClick();
  }
};
```

## Acceptance Criteria
- [ ] Tab order logical (left-to-right, top-to-bottom)
- [ ] Skip links for main content
- [ ] All interactive elements keyboard-accessible
- [ ] Enter/Space activate buttons
- [ ] Escape closes dialogs/menus
- [ ] Arrow keys navigate lists/menus

## Files
- `frontend/src/components/accessibility/SkipLinks.tsx`
- `frontend/src/hooks/useKeyboardNav.ts`
