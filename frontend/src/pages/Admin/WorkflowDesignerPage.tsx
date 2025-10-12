import { useCallback } from 'react';
import { Container, Typography, Box } from '@mui/material';
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

import { WorkflowToolbar, nodeTypes } from '../../components/Workflow';
import { useElkLayout } from '../../lib/layout/useElkLayout';
import { useDagreLayout } from '../../lib/layout/useDagreLayout';

/**
 * W1: Workflow Designer Page
 * 
 * Phase W1: Interactive canvas with custom nodes, toolbar, auto-layout
 */
export const WorkflowDesignerPage = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  
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

  // Save/Load handlers (TODO W1: wire to BE)
  const handleSave = useCallback(() => {
    console.log('💾 Save draft:', { nodes, edges });
    // TODO W1: POST /api/admin/workflows/{entity}/draft
  }, [nodes, edges]);

  const handleLoad = useCallback(() => {
    console.log('📂 Load draft');
    // TODO W1: GET /api/admin/workflows/{entity}/draft
  }, []);

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
    </Container>
  );
};

export default WorkflowDesignerPage;
