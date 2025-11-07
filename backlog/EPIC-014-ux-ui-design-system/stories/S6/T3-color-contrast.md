# T3: Color Contrast & Accessible Themes

**Parent Story:** S6 - Accessibility (WCAG 2.1 AA)  
**LOC:** ~150 | **Effort:** ~2h

## Objective
Ensure all text and UI elements meet WCAG 2.1 AA color contrast ratios.

## Implementation

```tsx
// frontend/src/theme/palette.ts
export const palette = {
  primary: {
    main: '#1976d2',     // Contrast ratio 4.5:1 on white
    contrastText: '#fff',
  },
  text: {
    primary: 'rgba(0, 0, 0, 0.87)',    // 15.8:1 on white
    secondary: 'rgba(0, 0, 0, 0.60)',  // 7.0:1 on white
  },
  error: {
    main: '#d32f2f',     // 4.5:1 on white
  },
};

// Contrast checker utility
const getContrastRatio = (foreground: string, background: string): number => {
  // WCAG formula implementation
};
```

## Acceptance Criteria
- [ ] Text contrast ≥4.5:1 (normal text)
- [ ] Large text contrast ≥3:1
- [ ] Link color distinguishable
- [ ] Focus indicators visible
- [ ] Dark mode also compliant
- [ ] Automated contrast testing

## Files
- `frontend/src/theme/palette.ts`
- `frontend/src/utils/contrastChecker.ts`
