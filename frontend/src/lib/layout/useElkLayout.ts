import ELK from 'elkjs/lib/elk.bundled.js';
import { Node, Edge } from 'reactflow';

const elk = new ELK();

/**
 * W0: ELK.js auto-layout hook (foundation)
 * 
 * Hierarchical layout for workflow graphs
 */
export const useElkLayout = () => {
  const getLayoutedElements = async (
    nodes: Node[],
    edges: Edge[],
    options = {}
  ) => {
    const graph = {
      id: 'root',
      layoutOptions: {
        'elk.algorithm': 'layered',
        'elk.direction': 'DOWN',
        'elk.spacing.nodeNode': '80',
        'elk.layered.spacing.nodeNodeBetweenLayers': '100',
        ...options,
      },
      children: nodes.map((node) => ({
        id: node.id,
        width: node.width ?? 150,
        height: node.height ?? 50,
      })),
      edges: edges.map((edge) => ({
        id: edge.id,
        sources: [edge.source],
        targets: [edge.target],
      })),
    };

    const layoutedGraph = await elk.layout(graph);

    const layoutedNodes = nodes.map((node) => {
      const layoutedNode = layoutedGraph.children?.find((n) => n.id === node.id);
      return {
        ...node,
        position: {
          x: layoutedNode?.x ?? 0,
          y: layoutedNode?.y ?? 0,
        },
      };
    });

    return { nodes: layoutedNodes, edges };
  };

  return { getLayoutedElements };
};
