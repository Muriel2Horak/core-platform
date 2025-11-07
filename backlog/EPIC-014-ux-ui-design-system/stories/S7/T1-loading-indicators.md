# T1: Loading Indicators

**Parent Story:** S7 - Loading States & Animations  
**LOC:** ~150 | **Effort:** ~2h

## Objective
Create consistent loading indicators (spinners, progress bars, skeleton screens).

## Implementation

```tsx
// frontend/src/components/loading/LoadingButton.tsx
import { Button, CircularProgress } from '@mui/material';

export const LoadingButton: React.FC<{
  loading: boolean;
  children: React.ReactNode;
  onClick: () => void;
  disabled?: boolean;
}> = ({ loading, children, onClick, disabled }) => (
  <Button
    onClick={onClick}
    disabled={loading || disabled}
    startIcon={loading ? <CircularProgress size={20} /> : undefined}
  >
    {children}
  </Button>
);

// frontend/src/components/loading/Skeleton.tsx
import { Skeleton, Stack } from '@mui/material';

export const TableSkeleton: React.FC<{ rows?: number }> = ({ rows = 5 }) => (
  <Stack spacing={1}>
    {[...Array(rows)].map((_, i) => (
      <Skeleton key={i} variant="rectangular" height={60} />
    ))}
  </Stack>
);

export const CardSkeleton: React.FC = () => (
  <Card>
    <Skeleton variant="rectangular" height={200} />
    <CardContent>
      <Skeleton variant="text" width="60%" />
      <Skeleton variant="text" width="80%" />
    </CardContent>
  </Card>
);

// frontend/src/components/loading/ProgressBar.tsx
export const UploadProgress: React.FC<{ progress: number }> = ({ progress }) => (
  <Box sx={{ width: '100%' }}>
    <LinearProgress variant="determinate" value={progress} />
    <Typography variant="caption" color="text.secondary">
      {progress}% uploaded
    </Typography>
  </Box>
);
```

## Acceptance Criteria
- [ ] Spinner for async operations (<CircularProgress />)
- [ ] Progress bar for file uploads (LinearProgress)
- [ ] Skeleton screens for initial load
- [ ] Loading states disable interactions
- [ ] Accessible (aria-busy, role="status")
- [ ] Minimum display time (300ms to avoid flicker)

## Files
- `frontend/src/components/loading/LoadingButton.tsx`
- `frontend/src/components/loading/Skeleton.tsx`
- `frontend/src/components/loading/ProgressBar.tsx`
- `frontend/src/components/loading/LoadingOverlay.tsx`
