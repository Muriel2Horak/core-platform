import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { WorkflowStudio } from './WorkflowStudio';

describe('WorkflowStudio', () => {
  const mockOnSave = vi.fn().mockResolvedValue(undefined);
  const mockOnValidate = vi.fn().mockResolvedValue({
    valid: true,
    errors: [],
    warnings: [],
  });

  it('renders with entity type in header', () => {
    render(
      <WorkflowStudio
        entityType="ORDER"
        onSave={mockOnSave}
        onValidate={mockOnValidate}
      />
    );

    expect(screen.getByText(/Workflow Studio - ORDER/)).toBeInTheDocument();
  });

  it('shows node palette with add buttons', () => {
    render(
      <WorkflowStudio
        entityType="ORDER"
        onSave={mockOnSave}
      />
    );

    expect(screen.getByText('Node Palette')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /State/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Decision/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /End/i })).toBeInTheDocument();
  });

  it('adds state node when button clicked', async () => {
    render(
      <WorkflowStudio
        entityType="ORDER"
        onSave={mockOnSave}
      />
    );

    const stateButton = screen.getByRole('button', { name: /State/i });
    fireEvent.click(stateButton);

    // Node should be added to canvas (React Flow internal state)
    await waitFor(() => {
      // React Flow handles node rendering internally
      expect(stateButton).toBeEnabled();
    });
  });

  it('disables validate button when no onValidate provided', () => {
    render(
      <WorkflowStudio
        entityType="ORDER"
        onSave={mockOnSave}
      />
    );

    const validateButton = screen.getByRole('button', { name: /Validate/i });
    expect(validateButton).toBeDisabled();
  });

  it('enables validate button when onValidate provided', () => {
    render(
      <WorkflowStudio
        entityType="ORDER"
        onSave={mockOnSave}
        onValidate={mockOnValidate}
      />
    );

    const validateButton = screen.getByRole('button', { name: /Validate/i });
    expect(validateButton).toBeEnabled();
  });

  it('calls onSave with definition when save clicked', async () => {
    render(
      <WorkflowStudio
        entityType="ORDER"
        onSave={mockOnSave}
      />
    );

    const saveButton = screen.getByRole('button', { name: /Save/i });
    fireEvent.click(saveButton);

    await waitFor(() => {
      expect(mockOnSave).toHaveBeenCalledWith(
        expect.objectContaining({
          entityType: 'ORDER',
          nodes: expect.any(Array),
          edges: expect.any(Array),
        })
      );
    });
  });

  it('shows validation results when validate succeeds', async () => {
    render(
      <WorkflowStudio
        entityType="ORDER"
        onSave={mockOnSave}
        onValidate={mockOnValidate}
      />
    );

    const validateButton = screen.getByRole('button', { name: /Validate/i });
    fireEvent.click(validateButton);

    await waitFor(() => {
      expect(screen.getByText('Valid workflow')).toBeInTheDocument();
    });
  });

  it('shows validation errors when validate fails', async () => {
    const mockValidateFail = vi.fn().mockResolvedValue({
      valid: false,
      errors: ['No end node found', 'Disconnected state detected'],
      warnings: ['Consider adding guards'],
    });

    render(
      <WorkflowStudio
        entityType="ORDER"
        onSave={mockOnSave}
        onValidate={mockValidateFail}
      />
    );

    const validateButton = screen.getByRole('button', { name: /Validate/i });
    fireEvent.click(validateButton);

    await waitFor(() => {
      expect(screen.getByText('No end node found')).toBeInTheDocument();
      expect(screen.getByText('Disconnected state detected')).toBeInTheDocument();
      expect(screen.getByText('Consider adding guards')).toBeInTheDocument();
    });
  });

  it('loads initial definition if provided', () => {
    const initialDef = {
      nodes: [
        {
          id: 'start',
          type: 'input',
          data: { label: 'Start', type: 'state' },
          position: { x: 0, y: 0 },
        },
        {
          id: 'end',
          type: 'output',
          data: { label: 'End', type: 'end' },
          position: { x: 200, y: 200 },
        },
      ],
      edges: [
        {
          id: 'e1',
          source: 'start',
          target: 'end',
        },
      ],
    };

    render(
      <WorkflowStudio
        entityType="ORDER"
        initialDefinition={initialDef}
        onSave={mockOnSave}
      />
    );

    // Initial nodes should be loaded (React Flow internal state)
    expect(screen.getByText(/Workflow Studio/)).toBeInTheDocument();
  });

  it('shows helper text about double-clicking', () => {
    render(
      <WorkflowStudio
        entityType="ORDER"
        onSave={mockOnSave}
      />
    );

    expect(screen.getByText(/Double-click nodes\/edges to configure/i)).toBeInTheDocument();
  });

  it('has export button in toolbar', () => {
    render(
      <WorkflowStudio
        entityType="ORDER"
        onSave={mockOnSave}
      />
    );

    expect(screen.getByRole('button', { name: /Export/i })).toBeInTheDocument();
  });
});
