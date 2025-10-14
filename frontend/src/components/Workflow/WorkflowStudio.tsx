import { useState, useCallback } from 'react';
import { 
  Box, Paper, Typography, Button, Toolbar, Divider,
  Dialog, DialogTitle, DialogContent, DialogActions,
  TextField, Chip, IconButton, Stack, Alert
} from '@mui/material';
import { 
  Save as SaveIcon, 
  Settings as SettingsIcon,
  Add as AddIcon,
  Delete as DeleteIcon,
  CheckCircle as CheckIcon,
  Edit as EditIcon
} from '@mui/icons-material';
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  addEdge,
  useNodesState,
  useEdgesState,
  Connection,
  Edge,
  Node,
} from 'reactflow';
import 'reactflow/dist/style.css';

/**
 * 🔄 W10: Workflow Studio Editor
 * 
 * Drag-and-drop workflow editor with:
 * - Visual state machine builder
 * - Node palette (start, task, decision, end)
 * - Guard and action configuration
 * - Real-time validation
 * 
 * @since 2025-01-14
 */

interface WorkflowStudioProps {
  entityType: string;
  initialDefinition?: any;
  onSave: (definition: any) => Promise<void>;
  onValidate?: (definition: any) => Promise<ValidationResult>;
}

interface ValidationResult {
  valid: boolean;
  errors: string[];
  warnings: string[];
}

interface NodeData {
  label: string;
  type: 'state' | 'decision' | 'end';
  actions?: string[];
  guards?: string[];
  metadata?: Record<string, any>;
}

interface EdgeData {
  label?: string;
  condition?: string;
  guards?: string[];
}

const initialNodes: Node[] = [
  {
    id: 'start',
    type: 'input',
    data: { label: 'Start', type: 'state', actions: [], guards: [] },
    position: { x: 250, y: 0 },
  },
];

const initialEdges: Edge[] = [];

export const WorkflowStudio = ({ entityType, initialDefinition, onSave, onValidate }: WorkflowStudioProps) => {
  const [nodes, setNodes, onNodesChange] = useNodesState(
    initialDefinition?.nodes || initialNodes
  );
  const [edges, setEdges, onEdgesChange] = useEdgesState(
    initialDefinition?.edges || initialEdges
  );
  const [saving, setSaving] = useState(false);
  const [validationResult, setValidationResult] = useState<ValidationResult | null>(null);
  
  // Node configuration dialog
  const [nodeDialogOpen, setNodeDialogOpen] = useState(false);
  const [selectedNode, setSelectedNode] = useState<Node<NodeData> | null>(null);
  const [nodeLabel, setNodeLabel] = useState('');
  const [nodeActions, setNodeActions] = useState<string[]>([]);
  const [nodeGuards, setNodeGuards] = useState<string[]>([]);
  const [newAction, setNewAction] = useState('');
  const [newGuard, setNewGuard] = useState('');

  // Edge configuration dialog
  const [edgeDialogOpen, setEdgeDialogOpen] = useState(false);
  const [selectedEdge, setSelectedEdge] = useState<Edge<EdgeData> | null>(null);
  const [edgeLabel, setEdgeLabel] = useState('');
  const [edgeCondition, setEdgeCondition] = useState('');

  const onConnect = useCallback(
    (params: Connection) => setEdges((eds) => addEdge(params, eds)),
    [setEdges]
  );

  const handleNodeDoubleClick = useCallback((_event: React.MouseEvent, node: Node) => {
    setSelectedNode(node as Node<NodeData>);
    setNodeLabel(node.data.label || '');
    setNodeActions((node.data as NodeData).actions || []);
    setNodeGuards((node.data as NodeData).guards || []);
    setNodeDialogOpen(true);
  }, []);

  const handleEdgeDoubleClick = useCallback((_event: React.MouseEvent, edge: Edge) => {
    setSelectedEdge(edge as Edge<EdgeData>);
    setEdgeLabel(edge.label as string || '');
    setEdgeCondition((edge.data as EdgeData)?.condition || '');
    setEdgeDialogOpen(true);
  }, []);

  const handleSaveNodeConfig = () => {
    if (!selectedNode) return;

    setNodes((nds) =>
      nds.map((n) =>
        n.id === selectedNode.id
          ? {
              ...n,
              data: {
                ...n.data,
                label: nodeLabel,
                actions: nodeActions,
                guards: nodeGuards,
              },
            }
          : n
      )
    );

    setNodeDialogOpen(false);
    setSelectedNode(null);
  };

  const handleSaveEdgeConfig = () => {
    if (!selectedEdge) return;

    setEdges((eds) =>
      eds.map((e) =>
        e.id === selectedEdge.id
          ? {
              ...e,
              label: edgeLabel,
              data: {
                ...e.data,
                condition: edgeCondition,
              },
            }
          : e
      )
    );

    setEdgeDialogOpen(false);
    setSelectedEdge(null);
  };

  const handleAddAction = () => {
    if (newAction.trim()) {
      setNodeActions([...nodeActions, newAction.trim()]);
      setNewAction('');
    }
  };

  const handleRemoveAction = (index: number) => {
    setNodeActions(nodeActions.filter((_, i) => i !== index));
  };

  const handleAddGuard = () => {
    if (newGuard.trim()) {
      setNodeGuards([...nodeGuards, newGuard.trim()]);
      setNewGuard('');
    }
  };

  const handleRemoveGuard = (index: number) => {
    setNodeGuards(nodeGuards.filter((_, i) => i !== index));
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const definition = {
        entityType,
        nodes: nodes.map(node => ({
          id: node.id,
          type: (node.data as NodeData).type || 'state',
          label: node.data.label,
          position: node.position,
          actions: (node.data as NodeData).actions || [],
          guards: (node.data as NodeData).guards || [],
        })),
        edges: edges.map(edge => ({
          id: edge.id,
          source: edge.source,
          target: edge.target,
          label: edge.label,
          condition: (edge.data as EdgeData)?.condition,
        })),
      };

      await onSave(definition);
    } finally {
      setSaving(false);
    }
  };

  const handleValidate = async () => {
    if (!onValidate) return;

    const definition = {
      entityType,
      nodes,
      edges,
    };

    const result = await onValidate(definition);
    setValidationResult(result);
  };

  const addStateNode = () => {
    const newNode: Node<NodeData> = {
      id: `state-${Date.now()}`,
      type: 'default',
      data: { 
        label: `State ${nodes.length}`,
        type: 'state',
        actions: [],
        guards: []
      },
      position: { x: 250 + Math.random() * 200, y: 100 + Math.random() * 200 },
    };
    setNodes((nds) => [...nds, newNode]);
  };

  const addDecisionNode = () => {
    const newNode: Node<NodeData> = {
      id: `decision-${Date.now()}`,
      type: 'default',
      data: { 
        label: `Decision ${nodes.length}`,
        type: 'decision',
        guards: []
      },
      position: { x: 250 + Math.random() * 200, y: 100 + Math.random() * 200 },
      style: { borderColor: '#ff9800', borderWidth: 2 },
    };
    setNodes((nds) => [...nds, newNode]);
  };

  const addEndNode = () => {
    const newNode: Node<NodeData> = {
      id: `end-${Date.now()}`,
      type: 'output',
      data: { 
        label: 'End',
        type: 'end'
      },
      position: { x: 250 + Math.random() * 200, y: 100 + Math.random() * 200 },
    };
    setNodes((nds) => [...nds, newNode]);
  };

  const handleExport = () => {
    const definition = {
      entityType,
      nodes: nodes.map(node => ({
        id: node.id,
        type: (node.data as NodeData).type || 'state',
        label: node.data.label,
        position: node.position,
        actions: (node.data as NodeData).actions || [],
        guards: (node.data as NodeData).guards || [],
      })),
      edges: edges.map(edge => ({
        id: edge.id,
        source: edge.source,
        target: edge.target,
        label: edge.label,
        condition: (edge.data as EdgeData)?.condition,
      })),
    };

    const blob = new Blob([JSON.stringify(definition, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `workflow-${entityType}-${Date.now()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <Paper elevation={3} sx={{ height: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Toolbar */}
      <Toolbar sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Typography variant="h6" sx={{ flexGrow: 1 }}>
          Workflow Studio - {entityType}
        </Typography>

        <Button
          startIcon={<SettingsIcon />}
          onClick={handleValidate}
          sx={{ mr: 1 }}
          disabled={!onValidate}
        >
          Validate
        </Button>

        <Button
          onClick={handleExport}
          sx={{ mr: 1 }}
        >
          Export
        </Button>

        <Button
          startIcon={<SaveIcon />}
          variant="contained"
          onClick={handleSave}
          disabled={saving}
        >
          {saving ? 'Saving...' : 'Save'}
        </Button>
      </Toolbar>

      {/* Main editor area */}
      <Box display="flex" flexGrow={1}>
        {/* Node palette */}
        <Paper
          elevation={0}
          sx={{
            width: 200,
            borderRight: 1,
            borderColor: 'divider',
            p: 2,
            display: 'flex',
            flexDirection: 'column',
            gap: 1,
          }}
        >
          <Typography variant="subtitle2" gutterBottom>
            Node Palette
          </Typography>
          
          <Button
            variant="outlined"
            size="small"
            onClick={addStateNode}
            fullWidth
            startIcon={<AddIcon />}
          >
            State
          </Button>
          
          <Button
            variant="outlined"
            size="small"
            onClick={addDecisionNode}
            fullWidth
            color="warning"
            startIcon={<AddIcon />}
          >
            Decision
          </Button>
          
          <Button
            variant="outlined"
            size="small"
            onClick={addEndNode}
            fullWidth
            color="error"
            startIcon={<AddIcon />}
          >
            End
          </Button>

          <Divider sx={{ my: 2 }} />

          {validationResult && (
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Validation Result
              </Typography>
              
              {validationResult.valid ? (
                <Alert severity="success" icon={<CheckIcon />}>
                  Valid workflow
                </Alert>
              ) : (
                <>
                  {validationResult.errors.map((err, i) => (
                    <Alert key={i} severity="error" sx={{ mb: 1 }}>
                      {err}
                    </Alert>
                  ))}
                  {validationResult.warnings.map((warn, i) => (
                    <Alert key={i} severity="warning" sx={{ mb: 1 }}>
                      {warn}
                    </Alert>
                  ))}
                </>
              )}
            </Box>
          )}

          <Box mt="auto">
            <Typography variant="caption" color="text.secondary">
              Double-click nodes/edges to configure
            </Typography>
          </Box>
        </Paper>

        {/* React Flow canvas */}
        <Box flexGrow={1}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeDoubleClick={handleNodeDoubleClick}
            onEdgeDoubleClick={handleEdgeDoubleClick}
            fitView
          >
            <Background />
            <Controls />
            <MiniMap />
          </ReactFlow>
        </Box>
      </Box>

      {/* Node Configuration Dialog */}
      <Dialog open={nodeDialogOpen} onClose={() => setNodeDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          Configure Node
          <IconButton
            edge="end"
            onClick={() => setNodeDialogOpen(false)}
            sx={{ position: 'absolute', right: 8, top: 8 }}
          >
            ×
          </IconButton>
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Label"
              value={nodeLabel}
              onChange={(e) => setNodeLabel(e.target.value)}
              fullWidth
            />

            {selectedNode?.data.type === 'state' && (
              <>
                <Box>
                  <Typography variant="subtitle2" gutterBottom>
                    Actions
                  </Typography>
                  <Stack direction="row" spacing={1} mb={1}>
                    <TextField
                      size="small"
                      placeholder="Add action..."
                      value={newAction}
                      onChange={(e) => setNewAction(e.target.value)}
                      onKeyPress={(e) => e.key === 'Enter' && handleAddAction()}
                      fullWidth
                    />
                    <IconButton onClick={handleAddAction} color="primary">
                      <AddIcon />
                    </IconButton>
                  </Stack>
                  <Stack direction="row" spacing={1} flexWrap="wrap">
                    {nodeActions.map((action, i) => (
                      <Chip
                        key={i}
                        label={action}
                        onDelete={() => handleRemoveAction(i)}
                        deleteIcon={<DeleteIcon />}
                      />
                    ))}
                  </Stack>
                </Box>

                <Box>
                  <Typography variant="subtitle2" gutterBottom>
                    Guards
                  </Typography>
                  <Stack direction="row" spacing={1} mb={1}>
                    <TextField
                      size="small"
                      placeholder="Add guard..."
                      value={newGuard}
                      onChange={(e) => setNewGuard(e.target.value)}
                      onKeyPress={(e) => e.key === 'Enter' && handleAddGuard()}
                      fullWidth
                    />
                    <IconButton onClick={handleAddGuard} color="primary">
                      <AddIcon />
                    </IconButton>
                  </Stack>
                  <Stack direction="row" spacing={1} flexWrap="wrap">
                    {nodeGuards.map((guard, i) => (
                      <Chip
                        key={i}
                        label={guard}
                        onDelete={() => handleRemoveGuard(i)}
                        deleteIcon={<DeleteIcon />}
                        color="secondary"
                      />
                    ))}
                  </Stack>
                </Box>
              </>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setNodeDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleSaveNodeConfig} variant="contained" startIcon={<EditIcon />}>
            Save
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edge Configuration Dialog */}
      <Dialog open={edgeDialogOpen} onClose={() => setEdgeDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>
          Configure Transition
          <IconButton
            edge="end"
            onClick={() => setEdgeDialogOpen(false)}
            sx={{ position: 'absolute', right: 8, top: 8 }}
          >
            ×
          </IconButton>
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Label"
              value={edgeLabel}
              onChange={(e) => setEdgeLabel(e.target.value)}
              fullWidth
              placeholder="e.g., 'on_submit', 'approved'"
            />

            <TextField
              label="Condition"
              value={edgeCondition}
              onChange={(e) => setEdgeCondition(e.target.value)}
              fullWidth
              multiline
              rows={3}
              placeholder="e.g., status == 'APPROVED'"
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEdgeDialogOpen(false)}>Cancel</Button>
          <Button onClick={handleSaveEdgeConfig} variant="contained" startIcon={<EditIcon />}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
};
