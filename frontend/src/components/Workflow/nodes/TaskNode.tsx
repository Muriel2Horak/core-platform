import { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { Box, Typography, IconButton, Tooltip } from '@mui/material';
import { 
  PlayArrow as StartIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import { tokens } from '../../../shared/theme/tokens';

/**
 * W1: TaskNode - Vizuální reprezentace TASK state
 * 
 * Modrý box s handle porty, edit/delete akce
 */
export interface TaskNodeData {
  label: string;
  stateType: 'TASK';
  description?: string;
  onEdit?: (id: string) => void;
  onDelete?: (id: string) => void;
}

const TaskNode = memo(({ id, data, selected }: NodeProps<TaskNodeData>) => {
  return (
    <Box
      sx={{
        minWidth: 180,
        backgroundColor: tokens.colors.primary[50],
        border: `2px solid ${selected ? tokens.colors.primary[500] : tokens.colors.primary[300]}`,
        borderRadius: tokens.borderRadius.md,
        boxShadow: selected ? tokens.shadows.lg : tokens.shadows.sm,
        transition: 'all 0.2s ease',
        '&:hover': {
          boxShadow: tokens.shadows.md,
          borderColor: tokens.colors.primary[400],
        },
      }}
    >
      {/* Input Handle */}
      <Handle
        type="target"
        position={Position.Top}
        style={{
          background: tokens.colors.primary[500],
          width: 10,
          height: 10,
          border: '2px solid white',
        }}
      />

      {/* Header */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          p: 1.5,
          backgroundColor: tokens.colors.primary[100],
          borderBottom: `1px solid ${tokens.colors.primary[200]}`,
        }}
      >
        <Box display="flex" alignItems="center" gap={1}>
          <StartIcon sx={{ fontSize: 18, color: tokens.colors.primary[600] }} />
          <Typography variant="caption" fontWeight={600} color={tokens.colors.primary[900]}>
            TASK
          </Typography>
        </Box>

        <Box display="flex" gap={0.5}>
          {data.onEdit && (
            <Tooltip title="Upravit">
              <IconButton
                size="small"
                onClick={() => data.onEdit?.(id)}
                sx={{ 
                  p: 0.5,
                  '&:hover': { backgroundColor: tokens.colors.primary[200] },
                }}
              >
                <EditIcon sx={{ fontSize: 14 }} />
              </IconButton>
            </Tooltip>
          )}
          {data.onDelete && (
            <Tooltip title="Smazat">
              <IconButton
                size="small"
                onClick={() => data.onDelete?.(id)}
                sx={{ 
                  p: 0.5,
                  '&:hover': { backgroundColor: tokens.colors.error[100] },
                }}
              >
                <DeleteIcon sx={{ fontSize: 14, color: tokens.colors.error[600] }} />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      </Box>

      {/* Body */}
      <Box sx={{ p: 1.5 }}>
        <Typography 
          variant="body2" 
          fontWeight={500}
          sx={{ mb: data.description ? 0.5 : 0 }}
        >
          {data.label}
        </Typography>
        {data.description && (
          <Typography variant="caption" color="text.secondary">
            {data.description}
          </Typography>
        )}
      </Box>

      {/* Output Handle */}
      <Handle
        type="source"
        position={Position.Bottom}
        style={{
          background: tokens.colors.primary[500],
          width: 10,
          height: 10,
          border: '2px solid white',
        }}
      />
    </Box>
  );
});

TaskNode.displayName = 'TaskNode';

export default TaskNode;
