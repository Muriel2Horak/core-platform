import { Box, Avatar, Chip, Tooltip, Typography } from '@mui/material';
import { Visibility, Edit, People, Refresh } from '@mui/icons-material';
import type { PresenceState } from '../../lib/presence/usePresence';
import { PresenceLoadingSkeleton } from './PresenceLoadingSkeleton';

export interface PresenceIndicatorProps {
  presence: PresenceState;
  currentUserId: string;
  getUserDisplayName?: (userId: string) => string;
  getUserAvatar?: (userId: string) => string | undefined;
}

/**
 * Presence indicator showing who is viewing/editing the entity
 * 
 * Features:
 * - Shows avatars of all active users
 * - "Stale" badge when entity is being modified (WRITE pipeline active)
 * - Busy user highlighted (who initiated the write)
 * - Version number display
 */
export function PresenceIndicator({
  presence,
  currentUserId,
  getUserDisplayName = (userId) => userId,
  getUserAvatar,
}: PresenceIndicatorProps) {
  const { users, stale, busyBy, version, connected, loading, reconnecting, reconnectAttempt } = presence;

  // Show loading skeleton while initial connection
  if (loading) {
    return <PresenceLoadingSkeleton />;
  }

  // Show reconnecting state
  if (reconnecting) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
        <Chip
          icon={<Refresh sx={{ fontSize: 16, animation: 'spin 1s linear infinite', '@keyframes spin': { from: { transform: 'rotate(0deg)' }, to: { transform: 'rotate(360deg)' } } }} />}
          label={`Reconnecting (${reconnectAttempt}/5)...`}
          color="warning"
          size="small"
        />
      </Box>
    );
  }

  // Show offline state
  if (!connected) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, color: 'text.secondary', fontSize: '0.875rem' }}>
        <Box sx={{ height: 8, width: 8, borderRadius: '50%', bgcolor: 'grey.400' }} />
        <Typography variant="body2" color="text.secondary">Offline</Typography>
      </Box>
    );
  }

  const viewingUsers = users.filter((u): u is string => typeof u === 'string');
  const otherUsers = viewingUsers.filter((u) => u !== currentUserId);

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
      {/* Connection Status */}
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
        <Box
          sx={{
            height: 8,
            width: 8,
            borderRadius: '50%',
            bgcolor: 'success.main',
            animation: 'pulse 2s ease-in-out infinite',
            '@keyframes pulse': {
              '0%, 100%': { opacity: 1 },
              '50%': { opacity: 0.5 },
            },
          }}
        />
        <Typography variant="caption" color="text.secondary">Live</Typography>
      </Box>

      {/* Stale Badge */}
      {stale && busyBy && (
        <Tooltip title={`${getUserDisplayName(busyBy)} is modifying this record`}>
          <Chip
            icon={<Edit sx={{ fontSize: 16 }} />}
            label="Editing"
            color="error"
            size="small"
          />
        </Tooltip>
      )}

      {/* User Avatars */}
      {viewingUsers.length > 0 && (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
          <Tooltip
            title={
              <Box>
                {viewingUsers.map((userId) => (
                  <Box key={userId} sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                    <Visibility sx={{ fontSize: 12 }} />
                    <Typography variant="caption">
                      {getUserDisplayName(userId)}
                      {userId === currentUserId && ' (you)'}
                      {userId === busyBy && ' (editing)'}
                    </Typography>
                  </Box>
                ))}
              </Box>
            }
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <People fontSize="small" color="action" />
              <Typography variant="body2" fontWeight="medium">{viewingUsers.length}</Typography>
            </Box>
          </Tooltip>

          {/* Avatar Stack */}
          <Box sx={{ display: 'flex', ml: 1 }}>
            {otherUsers.slice(0, 3).map((userId, idx) => (
              <Tooltip key={userId} title={getUserDisplayName(userId)}>
                <Avatar
                  src={getUserAvatar?.(userId)}
                  sx={{
                    width: 32,
                    height: 32,
                    ml: idx > 0 ? -1 : 0,
                    border: '2px solid white',
                    fontSize: '0.75rem',
                    bgcolor: userId === busyBy ? 'warning.main' : 'primary.main',
                  }}
                >
                  {getUserDisplayName(userId).slice(0, 2).toUpperCase()}
                </Avatar>
              </Tooltip>
            ))}
            {otherUsers.length > 3 && (
              <Tooltip title={`+${otherUsers.length - 3} more users`}>
                <Avatar
                  sx={{
                    width: 32,
                    height: 32,
                    ml: -1,
                    border: '2px solid white',
                    fontSize: '0.75rem',
                    bgcolor: 'grey.400',
                  }}
                >
                  +{otherUsers.length - 3}
                </Avatar>
              </Tooltip>
            )}
          </Box>
        </Box>
      )}

      {/* Version Badge */}
      {version !== undefined && (
        <Tooltip title="Entity version">
          <Chip label={`v${version}`} size="small" variant="outlined" />
        </Tooltip>
      )}
    </Box>
  );
}
