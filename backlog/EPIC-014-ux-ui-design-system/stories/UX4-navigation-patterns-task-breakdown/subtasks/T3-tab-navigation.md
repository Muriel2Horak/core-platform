# T3: Tab Navigation System

**Parent Story:** S4 - Navigation Patterns  
**LOC:** ~150 | **Effort:** ~2h

## Objective
Create tab navigation with URL synchronization and lazy loading.

## Implementation

```tsx
// frontend/src/components/navigation/TabNavigation.tsx
import { Tabs, Tab } from '@mui/material';
import { useNavigate, useLocation } from 'react-router-dom';

interface TabConfig {
  label: string;
  value: string;
  path: string;
  component: React.LazyExoticComponent<React.FC>;
}

export const TabNavigation: React.FC<{ tabs: TabConfig[] }> = ({ tabs }) => {
  const navigate = useNavigate();
  const location = useLocation();
  
  const currentTab = tabs.find(tab => 
    location.pathname.startsWith(tab.path)
  )?.value || tabs[0].value;
  
  return (
    <>
      <Tabs value={currentTab} onChange={(_, value) => {
        const tab = tabs.find(t => t.value === value);
        if (tab) navigate(tab.path);
      }}>
        {tabs.map(tab => (
          <Tab key={tab.value} label={tab.label} value={tab.value} />
        ))}
      </Tabs>
      
      <Suspense fallback={<CircularProgress />}>
        <Routes>
          {tabs.map(tab => (
            <Route key={tab.value} path={tab.path} element={<tab.component />} />
          ))}
        </Routes>
      </Suspense>
    </>
  );
};
```

## Acceptance Criteria
- [ ] Tabs sync with URL
- [ ] Active tab highlighted
- [ ] Tab change updates URL
- [ ] Lazy loading works
- [ ] Accessible (arrow key navigation)

## Files
- `frontend/src/components/navigation/TabNavigation.tsx`
