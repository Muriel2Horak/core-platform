# T3: Empty States

**Parent Story:** S8 - Error States & Feedback  
**LOC:** ~150 | **Effort:** ~2h

## Objective
Design helpful empty states with illustrations and clear CTAs.

## Implementation

```tsx
// frontend/src/components/empty/EmptyState.tsx
import { Box, Typography, Button } from '@mui/material';
import { Inbox } from '@mui/icons-material';

export const EmptyState: React.FC<{
  icon?: React.ReactNode;
  title: string;
  description: string;
  action?: { label: string; onClick: () => void };
}> = ({ icon = <Inbox />, title, description, action }) => (
  <Box
    sx={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: 300,
      textAlign: 'center',
      p: 4,
      color: 'text.secondary',
    }}
  >
    <Box sx={{ fontSize: 64, mb: 2, opacity: 0.5 }}>
      {icon}
    </Box>
    <Typography variant="h6" gutterBottom color="text.primary">
      {title}
    </Typography>
    <Typography variant="body2" sx={{ mb: 3, maxWidth: 400 }}>
      {description}
    </Typography>
    {action && (
      <Button variant="contained" onClick={action.onClick}>
        {action.label}
      </Button>
    )}
  </Box>
);

// Specific empty states
export const NoResults: React.FC<{ onClear?: () => void }> = ({ onClear }) => (
  <EmptyState
    icon={<SearchOff />}
    title="No results found"
    description="Try adjusting your search or filters to find what you're looking for."
    action={onClear ? { label: 'Clear Filters', onClick: onClear } : undefined}
  />
);

export const EmptyList: React.FC<{ onAdd: () => void; itemName: string }> = ({ onAdd, itemName }) => (
  <EmptyState
    icon={<Add />}
    title={`No ${itemName}s yet`}
    description={`Get started by creating your first ${itemName}.`}
    action={{ label: `Create ${itemName}`, onClick: onAdd }}
  />
);

export const FirstTimeUser: React.FC<{ onStart: () => void }> = ({ onStart }) => (
  <EmptyState
    icon={<Celebration />}
    title="Welcome!"
    description="Let's get you started with a quick tour of the platform."
    action={{ label: 'Start Tour', onClick: onStart }}
  />
);
```

## Acceptance Criteria
- [ ] Empty list states
- [ ] No search results
- [ ] First-time user experience
- [ ] Illustrations/icons consistent
- [ ] CTA buttons present
- [ ] Helpful, friendly copy
- [ ] Centered, good spacing

## Files
- `frontend/src/components/empty/EmptyState.tsx`
- `frontend/src/components/empty/NoResults.tsx`
- `frontend/src/components/empty/EmptyList.tsx`
