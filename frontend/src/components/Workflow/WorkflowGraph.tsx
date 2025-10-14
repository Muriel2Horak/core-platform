import { useMemo, useCallback } from 'react';
import { Box, Paper, Typography, Tooltip, Chip, ToggleButton, ToggleButtonGroup } from '@mui/material';
import ReactFlow, {
  Background,
  Controls,
  MiniMap,
  Node,
  Edge,
  MarkerType,
  Position,
} from 'reactflow';
import 'reactflow/dist/style.css';
import { useElkLayout } from '../../lib/layout/useElkLayout';
import { useDagreLayout } from '../../lib/layout/useDagreLayout';

/**
 * 🔄 W6: Workflow Runtime Graph Component
 * 
 * Features:
 * - Current state highlight
 * - Allowed edges in green, blocked edges in gray with tooltip
 * - Auto-layout toggle (elkjs / dagre)
 * - "Why not" tooltips for disabled transitions
 * 
 * @since 2025-10-14
 */

interface WorkflowGraphProps {
  graph: {
    entityType: string;
    entityId: string;
    currentState: string | null;
    nodes: Array<{
      id: string;
      code: string;
      label: string;
      type: string;
      current: boolean;
      metadata?: Record<string, any>;
    }>;
    edges: Array<{
      id: string;
      source: string;
      target: string;
      label: string;
      transitionCode: string;
      allowed: boolean;
      whyNot?: string;
      slaMinutes?: number;
    }>;
  };
}

type LayoutEngine = 'elk' | 'dagre';

export const WorkflowGraph = ({ graph }: WorkflowGraphProps) => {
  const [layoutEngine, setLayoutEngine] = useState<LayoutEngine>('elk');
  const { getLayoutedElements: getElkLayout } = useElkLayout();
  const { getLayoutedElements: getDagreLayout } = useDagreLayout();

  // Convert graph nodes to React Flow nodes
  const flowNodes: Node[] = useMemo(() => {
    return graph.nodes.map(node => ({
      id: node.id,
      type: 'default',
      data: { 
        label: (
          <Box>
            <Typography variant="body2" fontWeight={node.current ? 'bold' : 'normal'}>
              {node.label}
            </Typography>
            {node.current && (
              <Chip label="CURRENT" size="small" color="primary" sx={{ mt: 0.5 }} />
            )}
          </Box>
        )
      },
      position: { x: 0, y: 0 }, // Will be set by layout
      style: {
        background: node.current ? '#e3f2fd' : '#ffffff',
        border: node.current ? '2px solid #1976d2' : '1px solid #e0e0e0',
        borderRadius: 8,
        padding: 10,
        minWidth: 120,
      },
    }));
  }, [graph.nodes]);

  // Convert graph edges to React Flow edges
  const flowEdges: Edge[] = useMemo(() => {
    return graph.edges.map(edge => ({
      id: edge.id,
      source: edge.source,
      target: edge.target,
      label: (
        <Tooltip 
          title={edge.whyNot || (edge.allowed ? 'Allowed transition' : 'Blocked')}
          arrow
          placement="top"
        >
          <Box sx={{ 
            background: 'white', 
            padding: '2px 8px', 
            borderRadius: 1,
            border: '1px solid #e0e0e0',
            cursor: 'help',
          }}>
            <Typography variant="caption">
              {edge.label}
              {edge.slaMinutes && ` (SLA: ${edge.slaMinutes}m)`}
            </Typography>
          </Box>
        </Tooltip>
      ),
      type: 'smoothstep',
      animated: edge.allowed,
      style: {
        stroke: edge.allowed ? '#4caf50' : '#bdbdbd',
        strokeWidth: edge.allowed ? 2 : 1,
        opacity: edge.allowed ? 1 : 0.5,
      },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: edge.allowed ? '#4caf50' : '#bdbdbd',
      },
    }));
  }, [graph.edges]);

  // Apply layout
  const { nodes: layoutedNodes, edges: layoutedEdges } = useMemo(() => {
    const layoutFn = layoutEngine === 'elk' ? getElkLayout : getDagreLayout;
    return layoutFn(flowNodes, flowEdges);
  }, [flowNodes, flowEdges, layoutEngine, getElkLayout, getDagreLayout]);

  const handleLayoutChange = useCallback((_event: React.MouseEvent<HTMLElement>, newLayout: LayoutEngine | null) => {
    if (newLayout !== null) {
      setLayoutEngine(newLayout);
    }
  }, []);

  return (
    <Paper elevation={2} sx={{ height: 500, position: 'relative' }}>
      {/* Layout Toggle */}
      <Box sx={{ position: 'absolute', top: 16, left: 16, zIndex: 10 }}>
        <ToggleButtonGroup
          value={layoutEngine}
          exclusive
          onChange={handleLayoutChange}
          size="small"
        >
          <ToggleButton value="elk">
            ELK Layout
          </ToggleButton>
          <ToggleButton value="dagre">
            Dagre Layout
          </ToggleButton>
        </ToggleButtonGroup>
      </Box>

      {/* Graph */}
      <ReactFlow
        nodes={layoutedNodes}
        edges={layoutedEdges}
        fitView
        attributionPosition="bottom-right"
        nodesDraggable={false}
        nodesConnectable={false}
        elementsSelectable={false}
      >
        <Background />
        <Controls />
        <MiniMap 
          nodeColor={(node) => {
            const graphNode = graph.nodes.find(n => n.id === node.id);
            return graphNode?.current ? '#1976d2' : '#e0e0e0';
          }}
        />
      </ReactFlow>

      {/* Legend */}
      <Box sx={{ 
        position: 'absolute', 
        bottom: 40, 
        right: 200, 
        background: 'white', 
        p: 1.5, 
        borderRadius: 1,
        border: '1px solid #e0e0e0',
        zIndex: 10,
      }}>
        <Typography variant="caption" fontWeight="bold" display="block" mb={0.5}>
          Legend:
        </Typography>
        <Box display="flex" gap={2}>
          <Box display="flex" alignItems="center" gap={0.5}>
            <Box sx={{ width: 20, height: 3, background: '#4caf50' }} />
            <Typography variant="caption">Allowed</Typography>
          </Box>
          <Box display="flex" alignItems="center" gap={0.5}>
            <Box sx={{ width: 20, height: 3, background: '#bdbdbd' }} />
            <Typography variant="caption">Blocked</Typography>
          </Box>
          <Box display="flex" alignItems="center" gap={0.5}>
            <Box sx={{ width: 20, height: 20, background: '#e3f2fd', border: '2px solid #1976d2', borderRadius: 0.5 }} />
            <Typography variant="caption">Current</Typography>
          </Box>
        </Box>
      </Box>
    </Paper>
  );
};
