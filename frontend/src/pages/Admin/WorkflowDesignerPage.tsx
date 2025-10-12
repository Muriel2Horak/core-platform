import { Container, Typography, Box } from '@mui/material';
import { AccountTree as WorkflowIcon } from '@mui/icons-material';
import { GlassPaper } from '../../shared/ui';
import ReactFlow, { Background, Controls, MiniMap } from 'reactflow';
import 'reactflow/dist/style.css';

/**
 * W0: Workflow Designer Page (placeholder)
 * 
 * Phase W0: Foundation - empty canvas with React Flow mounted
 */
export const WorkflowDesignerPage = () => {
  return (
    <Container maxWidth="xl" sx={{ py: 4, height: 'calc(100vh - 100px)' }}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <WorkflowIcon fontSize="large" color="primary" />
        <Box>
          <Typography variant="h4">Workflow Designer</Typography>
          <Typography variant="body2" color="text.secondary">
            Vizuální editor stavových automatů s auto-layoutem
          </Typography>
        </Box>
      </Box>

      <GlassPaper sx={{ height: 'calc(100% - 80px)', p: 0, overflow: 'hidden' }}>
        <ReactFlow
          nodes={[]}
          edges={[]}
          fitView
        >
          <Background />
          <Controls />
          <MiniMap />
        </ReactFlow>
      </GlassPaper>
    </Container>
  );
};

export default WorkflowDesignerPage;
