import { Box, Tooltip } from '@mui/material';
import { Lock } from '@mui/icons-material';

export interface FieldLockIndicatorProps {
  fieldName: string;
  isLocked: boolean;
  lockedBy?: string;
  currentUserId: string;
  getUserDisplayName?: (userId: string) => string;
}

/**
 * Field-level lock indicator
 * 
 * Shows a lock icon when:
 * - This user has the lock (green)
 * - Another user has the lock (red)
 */
export function FieldLockIndicator({
  fieldName,
  isLocked,
  lockedBy,
  currentUserId,
  getUserDisplayName = (userId) => userId,
}: FieldLockIndicatorProps) {
  if (!isLocked || !lockedBy) {
    return null;
  }

  const isOwnLock = lockedBy === currentUserId;
  const displayName = getUserDisplayName(lockedBy);

  return (
    <Tooltip
      title={
        isOwnLock
          ? `You are editing ${fieldName}`
          : `${displayName} is editing ${fieldName}`
      }
    >
      <Box
        sx={{
          display: 'inline-flex',
          alignItems: 'center',
          justifyContent: 'center',
          width: 20,
          height: 20,
        }}
      >
        {isOwnLock ? (
          <Lock sx={{ fontSize: 16, color: 'success.main' }} />
        ) : (
          <Lock sx={{ fontSize: 16, color: 'error.main' }} />
        )}
      </Box>
    </Tooltip>
  );
}
