import { Box, Skeleton } from '@mui/material';

/**
 * Loading skeleton for presence indicator
 * 
 * Shows while WebSocket is connecting
 */
export function PresenceLoadingSkeleton() {
  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
      {/* Connection status skeleton */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        <Skeleton variant="circular" width={8} height={8} />
        <Skeleton variant="text" width={40} />
      </Box>

      {/* User count skeleton */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        <Skeleton variant="circular" width={16} height={16} />
        <Skeleton variant="text" width={20} />
      </Box>

      {/* Avatar stack skeleton */}
      <Box sx={{ display: 'flex', ml: 1 }}>
        {[0, 1, 2].map((idx) => (
          <Skeleton
            key={idx}
            variant="circular"
            width={32}
            height={32}
            sx={{ ml: idx > 0 ? -1 : 0 }}
          />
        ))}
      </Box>

      {/* Version badge skeleton */}
      <Skeleton variant="rounded" width={40} height={24} />
    </Box>
  );
}

/**
 * Loading skeleton for form with presence
 */
export function FormLoadingSkeleton() {
  return (
    <Box sx={{ p: 3 }}>
      <Skeleton variant="text" width="60%" height={40} sx={{ mb: 2 }} />
      <Skeleton variant="rectangular" width="100%" height={56} sx={{ mb: 2 }} />
      <Skeleton variant="rectangular" width="100%" height={56} sx={{ mb: 2 }} />
      <Skeleton variant="rectangular" width="100%" height={56} sx={{ mb: 2 }} />
      <Box sx={{ display: 'flex', gap: 2, mt: 3 }}>
        <Skeleton variant="rectangular" width={100} height={36} />
        <Skeleton variant="rectangular" width={100} height={36} />
      </Box>
    </Box>
  );
}
