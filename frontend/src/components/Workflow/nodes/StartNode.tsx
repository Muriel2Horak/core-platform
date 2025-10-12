import { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { Box, Typography } from '@mui/material';
import { PlayCircle as StartIcon } from '@mui/icons-material';
import { tokens } from '../../../shared/theme/tokens';

/**
 * W1: StartNode - Vizuální reprezentace START state
 * 
 * Zelený kruh, pouze output handle
 */
export interface StartNodeData {
  label: string;
  stateType: 'START';
}

const StartNode = memo(({ data, selected }: NodeProps<StartNodeData>) => {
  return (
    <Box
      sx={{
        width: 80,
        height: 80,
        borderRadius: '50%',
        backgroundColor: tokens.colors.success[100],
        border: `3px solid ${selected ? tokens.colors.success[700] : tokens.colors.success[500]}`,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 0.5,
        boxShadow: selected ? tokens.shadows.lg : tokens.shadows.md,
        transition: 'all 0.2s ease',
        '&:hover': {
          boxShadow: tokens.shadows.lg,
          transform: 'scale(1.05)',
        },
      }}
    >
      <StartIcon sx={{ fontSize: 28, color: tokens.colors.success[700] }} />
      <Typography 
        variant="caption" 
        fontWeight={700} 
        color={tokens.colors.success[900]}
        sx={{ fontSize: 10 }}
      >
        {data.label}
      </Typography>

      {/* Output Handle (bottom) */}
      <Handle
        type="source"
        position={Position.Bottom}
        style={{
          background: tokens.colors.success[600],
          width: 12,
          height: 12,
          border: '2px solid white',
        }}
      />
    </Box>
  );
});

StartNode.displayName = 'StartNode';

export default StartNode;
