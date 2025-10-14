import { useState, useEffect } from 'react';
import { Box, Button, Chip, Tooltip, Alert } from '@mui/material';
import { Lock as LockIcon, LockOpen as UnlockIcon } from '@mui/icons-material';

/**
 * 🔄 W6: Workflow Actions Bar Component
 * 
 * Features:
 * - Display allowed workflow actions from current state
 * - Read-only mode when workflow is locked by another user
 * - Stale→Fresh data refresh before applying action
 * - Disabled actions with tooltip showing "why not" reason
 * 
 * @since 2025-10-14
 */

interface ActionsBarProps {
  entityType: string;
  entityId: string;
  currentState: string;
  allowedActions: Array<{
    code: string;
    targetState: string;
    label: string;
    enabled: boolean;
    reason?: string; // "why not" - shown in tooltip when enabled=false
  }>;
  isLocked: boolean;
  lockedBy?: string;
  onActionApply: (actionCode: string) => Promise<void>;
  onRefresh: () => Promise<void>;
}

export const ActionsBar = ({
  entityType,
  entityId,
  currentState,
  allowedActions,
  isLocked,
  lockedBy,
  onActionApply,
  onRefresh,
}: ActionsBarProps) => {
  const [applying, setApplying] = useState<string | null>(null);
  const [staleWarning, setStaleWarning] = useState(false);

  // Simulate stale data detection (W6 requirement: stale→fresh before apply)
  useEffect(() => {
    const checkStale = async () => {
      // In real app: compare local timestamp with server timestamp
      // For W6 demo: mark as potentially stale after 30s
      const timer = setTimeout(() => setStaleWarning(true), 30000);
      return () => clearTimeout(timer);
    };
    checkStale();
  }, [currentState]);

  const handleAction = async (actionCode: string) => {
    try {
      setApplying(actionCode);
      
      // Stale→Fresh: refresh data before applying action
      if (staleWarning) {
        await onRefresh();
        setStaleWarning(false);
      }
      
      await onActionApply(actionCode);
    } catch (error) {
      console.error('Action failed:', error);
    } finally {
      setApplying(null);
    }
  };

  return (
    <Box sx={{ p: 2, borderTop: 1, borderColor: 'divider' }}>
      {/* Lock Warning */}
      {isLocked && (
        <Alert 
          severity="warning" 
          icon={<LockIcon />}
          sx={{ mb: 2 }}
          data-testid="lock-warning"
        >
          Workflow is locked by <strong>{lockedBy || 'another user'}</strong>. 
          Actions are read-only.
        </Alert>
      )}

      {/* Stale Data Warning */}
      {staleWarning && !isLocked && (
        <Alert 
          severity="info" 
          sx={{ mb: 2 }}
          data-testid="stale-warning"
        >
          Data may be stale. Will refresh before applying action.
        </Alert>
      )}

      {/* Current State */}
      <Box display="flex" alignItems="center" gap={2} mb={2}>
        <Chip 
          label={`Current: ${currentState}`}
          color="primary"
          size="small"
          data-testid="current-state"
        />
        {isLocked ? (
          <LockIcon fontSize="small" color="warning" data-testid="lock-icon" />
        ) : (
          <UnlockIcon fontSize="small" color="success" data-testid="unlock-icon" />
        )}
      </Box>

      {/* Action Buttons */}
      <Box display="flex" gap={1} flexWrap="wrap">
        {allowedActions.length === 0 ? (
          <Chip 
            label="No actions available" 
            variant="outlined" 
            size="small"
            data-testid="no-actions"
          />
        ) : (
          allowedActions.map((action) => {
            const isDisabled = isLocked || !action.enabled || applying !== null;
            const tooltipText = isLocked 
              ? `Locked by ${lockedBy || 'another user'}`
              : action.reason || '';

            return (
              <Tooltip 
                key={action.code} 
                title={isDisabled ? tooltipText : ''}
                arrow
              >
                <span> {/* Wrapper for disabled button tooltip */}
                  <Button
                    variant={action.enabled ? 'contained' : 'outlined'}
                    color="primary"
                    size="small"
                    disabled={isDisabled}
                    onClick={() => handleAction(action.code)}
                    data-testid={`action-${action.code}`}
                    data-reason={action.reason}
                  >
                    {action.label} → {action.targetState}
                    {applying === action.code && ' ...'}
                  </Button>
                </span>
              </Tooltip>
            );
          })
        )}
      </Box>

      {/* Debug Info */}
      <Box mt={2}>
        <Chip
          label={`Entity: ${entityType}/${entityId}`}
          size="small"
          variant="outlined"
          data-testid="entity-info"
        />
      </Box>
    </Box>
  );
};
