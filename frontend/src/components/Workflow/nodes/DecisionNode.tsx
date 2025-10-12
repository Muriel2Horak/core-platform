import { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { Box, Typography, IconButton, Tooltip } from '@mui/material';
import { 
  CallSplit as DecisionIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import { tokens } from '../../../shared/theme/tokens';

/**
 * W1: DecisionNode - Vizuální reprezentace DECISION state
 * 
 * Žlutý diamant s multiple output handles
 */
export interface DecisionNodeData {
  label: string;
  stateType: 'DECISION';
  description?: string;
  branches?: string[]; // ['YES', 'NO'] nebo ['A', 'B', 'C']
  onEdit?: (id: string) => void;
  onDelete?: (id: string) => void;
}

const DecisionNode = memo(({ id, data, selected }: NodeProps<DecisionNodeData>) => {
  const branches = data.branches || ['YES', 'NO'];

  return (
    <Box
      sx={{
        minWidth: 200,
        position: 'relative',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
      }}
    >
      {/* Diamond Shape */}
      <Box
        sx={{
          width: 140,
          height: 140,
          backgroundColor: tokens.colors.warning[50],
          border: `2px solid ${selected ? tokens.colors.warning[600] : tokens.colors.warning[500]}`,
          transform: 'rotate(45deg)',
          borderRadius: tokens.borderRadius.sm,
          boxShadow: selected ? tokens.shadows.lg : tokens.shadows.sm,
          transition: 'all 0.2s ease',
          position: 'absolute',
          '&:hover': {
            boxShadow: tokens.shadows.md,
            borderColor: tokens.colors.warning[500],
          },
        }}
      />

      {/* Content Container (rotated back) */}
      <Box
        sx={{
          position: 'relative',
          zIndex: 1,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: 1,
          maxWidth: 100,
          textAlign: 'center',
        }}
      >
        {/* Icon & Type */}
        <Box display="flex" alignItems="center" gap={0.5}>
          <DecisionIcon sx={{ fontSize: 16, color: tokens.colors.warning[700] }} />
          <Typography variant="caption" fontWeight={600} color={tokens.colors.warning[900]}>
            DECISION
          </Typography>
        </Box>

        {/* Label */}
        <Typography variant="body2" fontWeight={600} sx={{ lineHeight: 1.2 }}>
          {data.label}
        </Typography>

        {/* Actions */}
        <Box display="flex" gap={0.5}>
          {data.onEdit && (
            <Tooltip title="Upravit">
              <IconButton
                size="small"
                onClick={() => data.onEdit?.(id)}
                sx={{ 
                  p: 0.5,
                  backgroundColor: 'white',
                  '&:hover': { backgroundColor: tokens.colors.warning[100] },
                }}
              >
                <EditIcon sx={{ fontSize: 12 }} />
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
                  backgroundColor: 'white',
                  '&:hover': { backgroundColor: tokens.colors.error[100] },
                }}
              >
                <DeleteIcon sx={{ fontSize: 12, color: tokens.colors.error[600] }} />
              </IconButton>
            </Tooltip>
          )}
        </Box>
      </Box>

      {/* Input Handle (top) */}
      <Handle
        type="target"
        position={Position.Top}
        style={{
          background: tokens.colors.warning[500],
          width: 10,
          height: 10,
          border: '2px solid white',
          top: -5,
        }}
      />

      {/* Output Handles (multiple branches) */}
      {branches.map((branch, index) => {
        const totalBranches = branches.length;
        const angle = (index * 360) / totalBranches;
        const isBottom = angle >= 135 && angle <= 225;
        
        return (
          <Handle
            key={branch}
            type="source"
            position={Position.Bottom}
            id={branch}
            style={{
              background: tokens.colors.warning[500],
              width: 10,
              height: 10,
              border: '2px solid white',
              bottom: isBottom ? -5 : 'auto',
              left: `${30 + index * 20}%`,
            }}
          />
        );
      })}
    </Box>
  );
});

DecisionNode.displayName = 'DecisionNode';

export default DecisionNode;
