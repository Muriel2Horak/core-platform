import { memo } from 'react';
import { Handle, Position, NodeProps } from 'reactflow';
import { Box, Typography } from '@mui/material';
import { CheckCircle as EndIcon } from '@mui/icons-material';
import { tokens } from '../../../shared/theme/tokens';

/**
 * W1: EndNode - Vizuální reprezentace END state
 * 
 * Červený kruh, pouze input handle
 */
export interface EndNodeData {
  label: string;
  stateType: 'END';
}

const EndNode = memo(({ data, selected }: NodeProps<EndNodeData>) => {
  return (
    <Box
      sx={{
        width: 80,
        height: 80,
        borderRadius: '50%',
        backgroundColor: tokens.colors.error[50],
        border: `3px solid ${selected ? tokens.colors.error[700] : tokens.colors.error[500]}`,
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
      <EndIcon sx={{ fontSize: 28, color: tokens.colors.error[700] }} />
      <Typography 
        variant="caption" 
        fontWeight={700} 
        color={tokens.colors.error[900]}
        sx={{ fontSize: 10 }}
      >
        {data.label}
      </Typography>

      {/* Input Handle (top) */}
      <Handle
        type="target"
        position={Position.Top}
        style={{
          background: tokens.colors.error[600],
          width: 12,
          height: 12,
          border: '2px solid white',
        }}
      />
    </Box>
  );
});

EndNode.displayName = 'EndNode';

export default EndNode;
