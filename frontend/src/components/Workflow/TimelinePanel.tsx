import { Box, Paper, Typography, Chip } from '@mui/material';
import { Timeline, TimelineItem, TimelineSeparator, TimelineConnector, TimelineContent, TimelineDot } from '@mui/lab';
import { CheckCircle as CheckIcon, Error as ErrorIcon, Warning as WarningIcon } from '@mui/icons-material';
import { formatDistanceToNow } from 'date-fns';

/**
 * 🔄 W6: Workflow Timeline Panel Component
 * 
 * Features:
 * - State transition history with timestamps
 * - Duration indicators for each state
 * - SLA status badges (OK/WARN/BREACH)
 * - Total workflow duration
 * 
 * @since 2025-10-14
 */

interface TimelinePanelProps {
  history: {
    entityType: string;
    entityId: string;
    entries: Array<{
      eventType: string;
      fromState: string | null;
      toState: string;
      transitionCode: string;
      timestamp: string;
      durationMs: number;
      actor: string;
      slaStatus: 'NONE' | 'OK' | 'WARN' | 'BREACH';
    }>;
    totalDurationMs: number;
  };
}

export const TimelinePanel = ({ history }: TimelinePanelProps) => {
  const formatDuration = (ms: number): string => {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (days > 0) return `${days}d ${hours % 24}h`;
    if (hours > 0) return `${hours}h ${minutes % 60}m`;
    if (minutes > 0) return `${minutes}m ${seconds % 60}s`;
    return `${seconds}s`;
  };

  const getSlaColor = (status: string) => {
    switch (status) {
      case 'OK': return 'success';
      case 'WARN': return 'warning';
      case 'BREACH': return 'error';
      default: return 'default';
    }
  };

  const getSlaIcon = (status: string): JSX.Element | undefined => {
    switch (status) {
      case 'OK': return <CheckIcon fontSize="small" />;
      case 'WARN': return <WarningIcon fontSize="small" />;
      case 'BREACH': return <ErrorIcon fontSize="small" />;
      default: return undefined;
    }
  };

  return (
    <Paper elevation={2} sx={{ p: 2, maxHeight: 500, overflow: 'auto' }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">Workflow Timeline</Typography>
        <Chip 
          label={`Total: ${formatDuration(history.totalDurationMs)}`}
          size="small"
          color="primary"
          variant="outlined"
        />
      </Box>

      {/* Timeline */}
      {history.entries.length === 0 ? (
        <Typography variant="body2" color="text.secondary" align="center" py={4}>
          No workflow history yet
        </Typography>
      ) : (
        <Timeline position="right">
          {history.entries.map((entry, index) => (
            <TimelineItem key={index}>
              <TimelineSeparator>
                <TimelineDot 
                  color={index === 0 ? 'primary' : 'grey'}
                  variant={index === 0 ? 'filled' : 'outlined'}
                />
                {index < history.entries.length - 1 && <TimelineConnector />}
              </TimelineSeparator>

              <TimelineContent>
                <Box mb={2}>
                  {/* Transition Info */}
                  <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                    <Typography variant="body2" fontWeight="bold">
                      {entry.fromState ? `${entry.fromState} → ${entry.toState}` : entry.toState}
                    </Typography>
                    <Typography variant="caption" color="text.secondary">
                      ({entry.transitionCode})
                    </Typography>
                  </Box>

                  {/* Duration + SLA */}
                  <Box display="flex" alignItems="center" gap={1} mb={0.5}>
                    <Chip
                      label={formatDuration(entry.durationMs)}
                      size="small"
                      variant="outlined"
                      data-testid="duration"
                    />
                    {entry.slaStatus !== 'NONE' && (
                      <Chip
                        label={entry.slaStatus}
                        size="small"
                        color={getSlaColor(entry.slaStatus) as any}
                        icon={getSlaIcon(entry.slaStatus)}
                        data-testid="sla-badge"
                      />
                    )}
                  </Box>

                  {/* Timestamp + Actor */}
                  <Typography variant="caption" color="text.secondary" data-testid="timestamp">
                    {formatDistanceToNow(new Date(entry.timestamp), { addSuffix: true })} by {entry.actor}
                  </Typography>
                </Box>
              </TimelineContent>
            </TimelineItem>
          ))}
        </Timeline>
      )}
    </Paper>
  );
};
