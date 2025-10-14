import { useCallback } from 'react';
import dagre from 'dagre';
import { Node, Edge } from 'reactflow';

/**
 * 🔄 W6: Dagre Layout Hook
 * 
 * Provides compact graph layout using Dagre.
 * Good for simple workflows with few nodes.
 * 
 * @since 2025-10-14
 */

const dagreGraph = new dagre.graphlib.Graph();
dagreGraph.setDefaultEdgeLabel(() => ({}));

const nodeWidth = 200;
const nodeHeight = 80;

export const useDagreLayout = () => {
  return useCallback((nodes: Node[], edges: Edge[]): Node[] => {
    dagreGraph.setGraph({ rankdir: 'TB', ranksep: 100, nodesep: 80 });

    nodes.forEach((node) => {
      dagreGraph.setNode(node.id, { width: nodeWidth, height: nodeHeight });
    });

    edges.forEach((edge) => {
      dagreGraph.setEdge(edge.source, edge.target);
    });

    dagre.layout(dagreGraph);

    return nodes.map((node) => {
      const nodeWithPosition = dagreGraph.node(node.id);
      return {
        ...node,
        position: {
          x: nodeWithPosition.x - nodeWidth / 2,
          y: nodeWithPosition.y - nodeHeight / 2,
        },
      };
    });
  }, []);
};
