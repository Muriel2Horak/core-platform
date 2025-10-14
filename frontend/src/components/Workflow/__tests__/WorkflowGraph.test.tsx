import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { WorkflowGraph } from '../WorkflowGraph';

/**
 * 🧪 W6: WorkflowGraph Unit Tests
 * 
 * Tests:
 * - Current state highlighting (blue border/background)
 * - Allowed edges (green, animated) vs blocked (gray, static)
 * - Tooltip rendering for "why not" reasons
 * - Layout toggle (elk vs dagre)
 * 
 * @since 2025-10-14
 */

// Mock React Flow
vi.mock('reactflow', () => ({
  default: ({ nodes, edges }: any) => (
    <div data-testid="react-flow">
      {nodes.map((node: any) => (
        <div key={node.id} data-testid={`node-${node.id}`} data-current={node.data.isCurrent}>
          {node.data.label}
        </div>
      ))}
      {edges.map((edge: any) => (
        <div 
          key={edge.id} 
          data-testid={`edge-${edge.id}`}
          data-animated={edge.animated}
          data-style={edge.style?.stroke}
        />
      ))}
    </div>
  ),
  Background: () => <div data-testid="background" />,
  Controls: () => <div data-testid="controls" />,
  MiniMap: () => <div data-testid="minimap" />,
  MarkerType: { ArrowClosed: 'arrowclosed' },
  Position: { Top: 'top', Bottom: 'bottom' },
}));

// Mock layout hooks
vi.mock('../../../hooks', () => ({
  useElkLayout: () => vi.fn((nodes) => Promise.resolve(nodes)),
  useDagreLayout: () => vi.fn((nodes) => nodes),
}));

const mockGraph = {
  entityType: 'order',
  entityId: '123',
  currentState: 'submitted',
  nodes: [
    { id: 'draft', code: 'draft', label: 'Draft', type: 'state', current: false },
    { id: 'submitted', code: 'submitted', label: 'Submitted', type: 'state', current: true },
    { id: 'approved', code: 'approved', label: 'Approved', type: 'state', current: false },
  ],
  edges: [
    { 
      id: 'submitted-approved',
      source: 'submitted', 
      target: 'approved', 
      label: 'Approve',
      transitionCode: 'approve', 
      allowed: true,
      whyNot: undefined,
    },
    {
      id: 'submitted-draft',
      source: 'submitted',
      target: 'draft',
      label: 'Reject',
      transitionCode: 'reject',
      allowed: false,
      whyNot: 'Missing manager approval',
    },
  ],
};

describe('WorkflowGraph', () => {
  it('renders workflow graph with nodes', () => {
    render(<WorkflowGraph graph={mockGraph} />);
    
    expect(screen.getByTestId('react-flow')).toBeInTheDocument();
    expect(screen.getByTestId('node-draft')).toBeInTheDocument();
    expect(screen.getByTestId('node-submitted')).toBeInTheDocument();
    expect(screen.getByTestId('node-approved')).toBeInTheDocument();
  });

  it('highlights current state node', () => {
    render(<WorkflowGraph graph={mockGraph} />);
    
    const currentNode = screen.getByTestId('node-submitted');
    expect(currentNode).toHaveAttribute('data-current', 'true');
    
    const nonCurrentNode = screen.getByTestId('node-draft');
    expect(nonCurrentNode).toHaveAttribute('data-current', 'false');
  });

  it('renders allowed edge as green and animated', () => {
    render(<WorkflowGraph graph={mockGraph} />);
    
    const allowedEdge = screen.getByTestId('edge-submitted-approved');
    expect(allowedEdge).toHaveAttribute('data-animated', 'true');
    expect(allowedEdge).toHaveAttribute('data-style', '#4caf50'); // green
  });

  it('renders blocked edge as gray and static', () => {
    render(<WorkflowGraph graph={mockGraph} />);
    
    const blockedEdge = screen.getByTestId('edge-submitted-draft');
    expect(blockedEdge).toHaveAttribute('data-animated', 'false');
    expect(blockedEdge).toHaveAttribute('data-style', '#9e9e9e'); // gray
  });

  it('shows "why not" reason in tooltip', async () => {
    render(<WorkflowGraph graph={mockGraph} />);
    
    // MUI Tooltip requires hover - check tooltip title attribute
    const blockedEdge = screen.getByTestId('edge-submitted-draft');
    expect(blockedEdge.closest('[title]')).toHaveAttribute('title', 'Missing manager approval');
  });

  it('renders legend with visual indicators', () => {
    render(<WorkflowGraph graph={mockGraph} />);
    
    expect(screen.getByText(/Current State/)).toBeInTheDocument();
    expect(screen.getByText(/Allowed Transition/)).toBeInTheDocument();
    expect(screen.getByText(/Blocked Transition/)).toBeInTheDocument();
  });

  it('toggles layout algorithm', async () => {
    render(<WorkflowGraph graph={mockGraph} />);
    
    const elkButton = screen.getByRole('button', { name: /elk/i });
    const dagreButton = screen.getByRole('button', { name: /dagre/i });
    
    expect(elkButton).toBeInTheDocument();
    expect(dagreButton).toBeInTheDocument();
    
    // Default is elk
    expect(elkButton).toHaveAttribute('aria-pressed', 'true');
  });
});
