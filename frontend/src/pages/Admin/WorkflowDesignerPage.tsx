import { useCallback, useState } from 'react';
import { Container, Typography, Box, Drawer, Tabs, Tab } from '@mui/material';
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

import { 
  WorkflowToolbar, 
  ValidationPanel, 
  SimulationPanel, 
  ProposalDialog,
  ProposalListPanel,
  ProposalReviewDialog,
  VersionHistoryPanel,
  nodeTypes,
} from '../../components/Workflow';
import { useElkLayout } from '../../lib/layout/useElkLayout';
import { useDagreLayout } from '../../lib/layout/useDagreLayout';

/**
 * W3: Workflow Designer Page
 * 
 * Phase W1: Interactive canvas with custom nodes, toolbar, auto-layout
 * Phase W2: Validation + Simulation
 * Phase W3: Proposals & Approvals + Version history
 */
export const WorkflowDesignerPage = () => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const [validationResult, setValidationResult] = useState<any>(null);
  const [simulationResult, setSimulationResult] = useState<any>(null);
  const [showValidation, setShowValidation] = useState(false);
  const [showSimulation, setShowSimulation] = useState(false);

  // W3: Proposal state
  const [showProposalDialog, setShowProposalDialog] = useState(false);
  const [showProposalDrawer, setShowProposalDrawer] = useState(false);
  const [selectedProposal, setSelectedProposal] = useState<any>(null);
  const [showReviewDialog, setShowReviewDialog] = useState(false);
  const [proposalRefreshTrigger, setProposalRefreshTrigger] = useState(0);

  // W3: Version history state
  const [showVersionDrawer, setShowVersionDrawer] = useState(false);
  const [versionRefreshTrigger, setVersionRefreshTrigger] = useState(0);

  // W3: Right drawer tab state
  const [rightDrawerTab, setRightDrawerTab] = useState<'validation' | 'simulation' | 'proposals' | 'versions'>('validation');
  
  const { getLayoutedElements: getElkLayout } = useElkLayout();
  const { getLayoutedElements: getDagreLayout } = useDagreLayout();

  const entity = 'customer-onboarding'; // TODO: make dynamic

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

  // W3: Proposal handlers
  const handleCreateProposal = useCallback(() => {
    console.log('📝 Create proposal');
    setShowProposalDialog(true);
  }, []);

  const handleProposalCreated = useCallback((proposalId: number) => {
    console.log('✅ Proposal created:', proposalId);
    setProposalRefreshTrigger(prev => prev + 1);
    setVersionRefreshTrigger(prev => prev + 1);
  }, []);

  const handleViewProposals = useCallback(() => {
    console.log('📋 View proposals');
    setShowProposalDrawer(true);
    setRightDrawerTab('proposals');
  }, []);

  const handleVersionHistory = useCallback(() => {
    console.log('📦 View version history');
    setShowVersionDrawer(true);
    setRightDrawerTab('versions');
  }, []);

  const handleProposalReviewed = useCallback(() => {
    console.log('✅ Proposal reviewed');
    setProposalRefreshTrigger(prev => prev + 1);
    setVersionRefreshTrigger(prev => prev + 1);
  }, []);

  const handleSelectProposal = useCallback((proposal: any) => {
    console.log('📄 Selected proposal:', proposal);
    setSelectedProposal(proposal);
    setShowReviewDialog(true);
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
          onValidate={handleValidate}
          onSimulate={handleSimulate}
          onCreateProposal={handleCreateProposal}
          onViewProposals={handleViewProposals}
          onVersionHistory={handleVersionHistory}
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

      {/* W3: Proposal Creation Dialog */}
      <ProposalDialog
        open={showProposalDialog}
        onClose={() => setShowProposalDialog(false)}
        entity={entity}
        nodes={nodes}
        edges={edges}
        onProposalCreated={handleProposalCreated}
      />

      {/* W3: Proposal List Drawer */}
      <Drawer
        anchor="right"
        open={showProposalDrawer && rightDrawerTab === 'proposals'}
        onClose={() => setShowProposalDrawer(false)}
        sx={{ '& .MuiDrawer-paper': { width: 500 } }}
      >
        <ProposalListPanel
          entity={entity}
          status="PENDING"
          onSelectProposal={handleSelectProposal}
          refreshTrigger={proposalRefreshTrigger}
        />
      </Drawer>

      {/* W3: Proposal Review Dialog */}
      <ProposalReviewDialog
        open={showReviewDialog}
        onClose={() => setShowReviewDialog(false)}
        proposal={selectedProposal}
        onReviewed={handleProposalReviewed}
      />

      {/* W3: Version History Drawer */}
      <Drawer
        anchor="right"
        open={showVersionDrawer && rightDrawerTab === 'versions'}
        onClose={() => setShowVersionDrawer(false)}
        sx={{ '& .MuiDrawer-paper': { width: 500 } }}
      >
        <VersionHistoryPanel
          entity={entity}
          refreshTrigger={versionRefreshTrigger}
        />
      </Drawer>
    </Container>
  );
};

export default WorkflowDesignerPage;
