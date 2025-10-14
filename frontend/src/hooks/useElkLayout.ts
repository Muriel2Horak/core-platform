import { useCallback } from 'react';
import ELK, { ElkNode } from 'elkjs/lib/elk.bundled.js';
import { Node, Edge } from 'reactflow';

/**
 * 🔄 W6: ELK Layout Hook
 * 
 * Provides hierarchical graph layout using ELK.js
 * Good for complex workflows with clear hierarchy.
 * 
 * @since 2025-10-14
 */

const elk = new ELK();

const elkOptions = {
  'elk.algorithm': 'layered',
  'elk.layered.spacing.nodeNodeBetweenLayers': '100',
  'elk.spacing.nodeNode': '80',
  'elk.direction': 'DOWN',
};

export const useElkLayout = () => {
  return useCallback(async (nodes: Node[], edges: Edge[]): Promise<Node[]> => {
    const graph: ElkNode = {
      id: 'root',
      layoutOptions: elkOptions,
      children: nodes.map((node) => ({
        id: node.id,
        width: 200,
        height: 80,
      })),
      edges: edges.map((edge) => ({
        id: edge.id,
        sources: [edge.source],
        targets: [edge.target],
      })),
    };

    const layouted = await elk.layout(graph);

    return nodes.map((node) => {
      const layoutedNode = layouted.children?.find((n) => n.id === node.id);
      return {
        ...node,
        position: {
          x: layoutedNode?.x ?? 0,
          y: layoutedNode?.y ?? 0,
        },
      };
    });
  }, []);
};
