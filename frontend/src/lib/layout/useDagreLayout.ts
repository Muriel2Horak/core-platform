import dagre from 'dagre';
import { Node, Edge } from 'reactflow';

/**
 * W0: Dagre auto-layout hook (foundation)
 * 
 * Alternative to ELK, simpler but less features
 */
export const useDagreLayout = () => {
  const getLayoutedElements = (
    nodes: Node[],
    edges: Edge[],
    direction = 'TB'
  ) => {
    const dagreGraph = new dagre.graphlib.Graph();
    dagreGraph.setDefaultEdgeLabel(() => ({}));
    dagreGraph.setGraph({ rankdir: direction, nodesep: 80, ranksep: 100 });

    nodes.forEach((node) => {
      dagreGraph.setNode(node.id, { width: node.width ?? 150, height: node.height ?? 50 });
    });

    edges.forEach((edge) => {
      dagreGraph.setEdge(edge.source, edge.target);
    });

    dagre.layout(dagreGraph);

    const layoutedNodes = nodes.map((node) => {
      const nodeWithPosition = dagreGraph.node(node.id);
      return {
        ...node,
        position: {
          x: nodeWithPosition.x - (node.width ?? 150) / 2,
          y: nodeWithPosition.y - (node.height ?? 50) / 2,
        },
      };
    });

    return { nodes: layoutedNodes, edges };
  };

  return { getLayoutedElements };
};
