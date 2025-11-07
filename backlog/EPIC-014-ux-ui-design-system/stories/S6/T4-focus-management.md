# T4: Focus Management

**Parent Story:** S6 - Accessibility (WCAG 2.1 AA)  
**LOC:** ~150 | **Effort:** ~2h

## Objective
Implement proper focus management for dialogs, modals, and dynamic content.

## Implementation

```tsx
// frontend/src/hooks/useFocusTrap.ts
export const useFocusTrap = (ref: React.RefObject<HTMLElement>) => {
  useEffect(() => {
    if (!ref.current) return;
    
    const focusableElements = ref.current.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );
    
    const firstElement = focusableElements[0] as HTMLElement;
    const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;
    
    const handleTab = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;
      
      if (e.shiftKey && document.activeElement === firstElement) {
        e.preventDefault();
        lastElement.focus();
      } else if (!e.shiftKey && document.activeElement === lastElement) {
        e.preventDefault();
        firstElement.focus();
      }
    };
    
    ref.current.addEventListener('keydown', handleTab);
    firstElement?.focus();
    
    return () => ref.current?.removeEventListener('keydown', handleTab);
  }, [ref]);
};
```

## Acceptance Criteria
- [ ] Focus trapped in dialogs
- [ ] Focus restored after dialog close
- [ ] First element focused on dialog open
- [ ] Tab cycles within dialog
- [ ] Visible focus indicators (2px outline)

## Files
- `frontend/src/hooks/useFocusTrap.ts`
- `frontend/src/hooks/useFocusRestore.ts`
