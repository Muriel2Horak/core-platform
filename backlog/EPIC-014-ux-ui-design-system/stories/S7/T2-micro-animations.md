# T2: Micro-Animations

**Parent Story:** S7 - Loading States & Animations  
**LOC:** ~150 | **Effort:** ~2h

## Objective
Add subtle micro-animations for better UX (ripple, elevation, expand).

## Implementation

```tsx
// frontend/src/theme/animations.ts
export const animations = {
  ripple: {
    '@keyframes ripple': {
      '0%': { transform: 'scale(0)', opacity: 1 },
      '100%': { transform: 'scale(4)', opacity: 0 },
    },
  },
  fadeIn: {
    '@keyframes fadeIn': {
      from: { opacity: 0, transform: 'translateY(10px)' },
      to: { opacity: 1, transform: 'translateY(0)' },
    },
  },
  slideIn: {
    '@keyframes slideIn': {
      from: { transform: 'translateX(-100%)' },
      to: { transform: 'translateX(0)' },
    },
  },
  pulse: {
    '@keyframes pulse': {
      '0%, 100%': { opacity: 1 },
      '50%': { opacity: 0.5 },
    },
  },
};

// Usage in components
<Box sx={{ 
  animation: 'fadeIn 200ms ease-in',
  '@keyframes fadeIn': animations.fadeIn['@keyframes fadeIn']
}}>
  Content
</Box>

// Animated Card
export const AnimatedCard: React.FC = ({ children }) => (
  <Card
    sx={{
      transition: 'all 0.3s ease',
      '&:hover': {
        transform: 'translateY(-4px)',
        boxShadow: 6,
      },
    }}
  >
    {children}
  </Card>
);

// Expand/Collapse Animation
export const ExpandableSection: React.FC<{ expanded: boolean }> = ({ expanded, children }) => (
  <Collapse in={expanded} timeout={250}>
    {children}
  </Collapse>
);
```

## Acceptance Criteria
- [ ] Button ripple effect (200ms) - MUI default
- [ ] Card elevation on hover (300ms)
- [ ] Expand/collapse animations (250ms)
- [ ] All animations <300ms
- [ ] Respects prefers-reduced-motion
- [ ] Smooth easing functions (ease-in-out)

## Files
- `frontend/src/theme/animations.ts`
- `frontend/src/components/AnimatedCard.tsx`
- `frontend/src/components/ExpandableSection.tsx`
