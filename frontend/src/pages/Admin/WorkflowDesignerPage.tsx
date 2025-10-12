import { useCallback, useState } from 'react';
import { Container, Typography, Box, Drawer } from '@mui/material';
import { AccountTree as WorkflowIcon } from '@mui/icons-material';
import { GlassPaper } from '../../shared/ui';
import ReactFlow, { 
  Background, 
  Controls, 
  MiniMap,
  Node,
  addEdge,
  Connection,
  useNodesState,
  useEdgesState,
} from 'reactflow';
import 'reactflow/dist/style.css';
import axios from 'axios';

import { WorkflowToolbar, ValidationPanel, SimulationPanel, nodeTypes } from '../../components/Workflow';
import { useElkLayout } from '../../lib/layout/useElkLayout';
import { useDagreLayout } from '../../lib/layout/useDagreLayout';

/**
 * W2: Workflow Designer Page
 * 
 * Phase W1: Interactive canvas with custom nodes, toolbar, auto-layout
 * Phase W2: Validation + Simulation
 */
export const WorkflowDesignerPage = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [validationResult, setValidationResult] = useState<any>(null);
  const [simulationResult, setSimulationResult] = useState<any>(null);
  const [showValidation, setShowValidation] = useState(false);
  const [showSimulation, setShowSimulation] = useState(false);
  
  const { getLayoutedElements: getElkLayout } = useElkLayout();
  const { getLayoutedElements: getDagreLayout } = useDagreLayout();

  // Add node handler
  const handleAddNode = useCallback((type: 'start' | 'task' | 'decision' | 'end') => {
    const id = `${type}-${Date.now()}`;
    const newNode: Node = {
      id,
      type,
      position: { x: Math.random() * 400, y: Math.random() * 400 },
      data: {
        label: `${type.toUpperCase()} ${nodes.length + 1}`,
        stateType: type.toUpperCase(),
        onEdit: (nodeId: string) => console.log('Edit node:', nodeId),
        onDelete: (nodeId: string) => setNodes((nds) => nds.filter((n) => n.id !== nodeId)),
      },
    };
    setNodes((nds) => [...nds, newNode]);
  }, [nodes.length, setNodes]);

  // Auto-layout handler
  const handleAutoLayout = useCallback(async (engine: 'elk' | 'dagre') => {
    if (engine === 'elk') {
      const { nodes: layoutedNodes, edges: layoutedEdges } = await getElkLayout(nodes, edges);
      setNodes(layoutedNodes);
      setEdges(layoutedEdges);
    } else {
      const { nodes: layoutedNodes, edges: layoutedEdges } = getDagreLayout(nodes, edges);
      setNodes(layoutedNodes);
      setEdges(layoutedEdges);
    }
  }, [nodes, edges, getElkLayout, getDagreLayout, setNodes, setEdges]);

  // Edge connection handler
  const onConnect = useCallback(
    (connection: Connection) => setEdges((eds) => addEdge(connection, eds)),
    [setEdges]
  );

  // Save/Load handlers (W2: wired to BE)
  const handleSave = useCallback(async () => {
    console.log('💾 Save draft:', { nodes, edges });
    try {
      await axios.put('/api/admin/workflows/User/draft', { nodes, edges });
      console.log('✅ Draft saved');
    } catch (error) {
      console.error('❌ Save failed:', error);
    }
  }, [nodes, edges]);

  const handleLoad = useCallback(async () => {
    console.log('📂 Load draft');
    try {
      const response = await axios.get('/api/admin/workflows/User/draft');
      console.log('✅ Draft loaded:', response.data);
    } catch (error) {
      console.error('❌ Load failed:', error);
    }
  }, []);

  // W2: Validation handler
  const handleValidate = useCallback(async () => {
    console.log('✅ Validating workflow...');
    try {
      const response = await axios.post('/api/admin/workflows/User/validate', { 
        nodes, 
        edges 
      });
      setValidationResult(response.data);
      setShowValidation(true);
      setShowSimulation(false);
    } catch (error) {
      console.error('❌ Validation failed:', error);
    }
  }, [nodes, edges]);

  // W2: Simulation handler
  const handleSimulate = useCallback(async () => {
    console.log('🎬 Simulating workflow...');
    try {
      const response = await axios.post('/api/admin/workflows/User/simulate', { 
        workflow: { nodes, edges },
        data: { userId: 1, status: 'pending' }, // Mock data
      });
      setSimulationResult(response.data);
      setShowSimulation(true);
      setShowValidation(false);
    } catch (error) {
      console.error('❌ Simulation failed:', error);
    }
  }, [nodes, edges]);

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

      <GlassPaper sx={{ height: 'calc(100% - 80px)', p: 0, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        {/* Toolbar */}
        <WorkflowToolbar
          onAddNode={handleAddNode}
          onAutoLayout={handleAutoLayout}
          onSave={handleSave}
          onLoad={handleLoad}
          onValidate={handleValidate}
          onSimulate={handleSimulate}
        />

        {/* Canvas */}
        <Box sx={{ flexGrow: 1, position: 'relative' }}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            nodeTypes={nodeTypes}
            fitView
          >
            <Background />
            <Controls />
            <MiniMap />
          </ReactFlow>
        </Box>
      </GlassPaper>

      {/* W2: Validation Drawer */}
      <Drawer
        anchor="right"
        open={showValidation}
        onClose={() => setShowValidation(false)}
        sx={{ '& .MuiDrawer-paper': { width: 400 } }}
      >
        <ValidationPanel 
          validationResult={validationResult}
          onClose={() => setShowValidation(false)}
        />
      </Drawer>

      {/* W2: Simulation Drawer */}
      <Drawer
        anchor="right"
        open={showSimulation}
        onClose={() => setShowSimulation(false)}
        sx={{ '& .MuiDrawer-paper': { width: 500 } }}
      >
        <SimulationPanel simulationResult={simulationResult} />
      </Drawer>
    </Container>
  );
};

export default WorkflowDesignerPage;
