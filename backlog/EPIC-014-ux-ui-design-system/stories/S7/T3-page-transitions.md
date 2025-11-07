# T3: Page Transitions

**Parent Story:** S7 - Loading States & Animations  
**LOC:** ~100 | **Effort:** ~1h

## Objective
Implement smooth page transitions for route changes.

## Implementation

```tsx
// frontend/src/components/layout/PageTransition.tsx
import { motion } from 'framer-motion';

const pageVariants = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 },
};

export const PageTransition: React.FC<{ children: React.ReactNode }> = ({ children }) => (
  <motion.div
    initial="initial"
    animate="animate"
    exit="exit"
    variants={pageVariants}
    transition={{ duration: 0.2 }}
  >
    {children}
  </motion.div>
);

// Usage in router
import { AnimatePresence } from 'framer-motion';
import { useLocation } from 'react-router-dom';

function App() {
  const location = useLocation();
  
  return (
    <AnimatePresence mode="wait">
      <Routes location={location} key={location.pathname}>
        <Route path="*" element={
          <PageTransition>
            <YourPage />
          </PageTransition>
        } />
      </Routes>
    </AnimatePresence>
  );
}

// frontend/src/components/layout/LoadingBar.tsx
import { LinearProgress } from '@mui/material';
import { useNavigation } from 'react-router-dom';

export const LoadingBar: React.FC = () => {
  const navigation = useNavigation();
  const isLoading = navigation.state === 'loading';
  
  return isLoading ? (
    <LinearProgress sx={{ position: 'fixed', top: 0, left: 0, right: 0, zIndex: 9999 }} />
  ) : null;
};
```

## Acceptance Criteria
- [ ] Fade in/out on route change (200ms)
- [ ] Loading bar at top during navigation
- [ ] Smooth slide transitions
- [ ] No layout shift
- [ ] AnimatePresence mode="wait" (no overlap)

## Files
- `frontend/src/components/layout/PageTransition.tsx`
- `frontend/src/components/layout/LoadingBar.tsx`
- `package.json` (add framer-motion dependency)
