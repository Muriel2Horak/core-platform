import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ActionsBar } from '../ActionsBar';

/**
 * 🧪 W6: ActionsBar Unit Tests
 * 
 * Tests:
 * - Action buttons rendering with correct labels
 * - Read-only mode when workflow is locked
 * - Stale→Fresh refresh before action apply
 * - Disabled actions with "why not" tooltips
 * 
 * @since 2025-10-14
 */

const mockActions = [
  {
    code: 'approve',
    targetState: 'approved',
    label: 'Approve',
    enabled: true,
  },
  {
    code: 'reject',
    targetState: 'rejected',
    label: 'Reject',
    enabled: false,
    reason: 'Missing required documents',
  },
];

describe('ActionsBar', () => {
  it('renders action buttons with labels', () => {
    const onActionApply = vi.fn();
    const onRefresh = vi.fn();
    
    render(
      <ActionsBar
        entityType="order"
        entityId="123"
        currentState="submitted"
        allowedActions={mockActions}
        isLocked={false}
        onActionApply={onActionApply}
        onRefresh={onRefresh}
      />
    );
    
    expect(screen.getByTestId('action-approve')).toBeInTheDocument();
    expect(screen.getByTestId('action-reject')).toBeInTheDocument();
    
    expect(screen.getByText(/Approve → approved/)).toBeInTheDocument();
    expect(screen.getByText(/Reject → rejected/)).toBeInTheDocument();
  });

  it('shows current state chip', () => {
    const onActionApply = vi.fn();
    const onRefresh = vi.fn();
    
    render(
      <ActionsBar
        entityType="order"
        entityId="123"
        currentState="submitted"
        allowedActions={mockActions}
        isLocked={false}
        onActionApply={onActionApply}
        onRefresh={onRefresh}
      />
    );
    
    expect(screen.getByTestId('current-state')).toHaveTextContent('Current: submitted');
  });

  it('disables all actions when locked', () => {
    const onActionApply = vi.fn();
    const onRefresh = vi.fn();
    
    render(
      <ActionsBar
        entityType="order"
        entityId="123"
        currentState="submitted"
        allowedActions={mockActions}
        isLocked={true}
        lockedBy="jane.doe"
        onActionApply={onActionApply}
        onRefresh={onRefresh}
      />
    );
    
    expect(screen.getByTestId('lock-warning')).toBeInTheDocument();
    expect(screen.getByText(/locked by jane.doe/i)).toBeInTheDocument();
    
    expect(screen.getByTestId('action-approve')).toBeDisabled();
    expect(screen.getByTestId('action-reject')).toBeDisabled();
  });

  it('shows unlock icon when not locked', () => {
    const onActionApply = vi.fn();
    const onRefresh = vi.fn();
    
    render(
      <ActionsBar
        entityType="order"
        entityId="123"
        currentState="submitted"
        allowedActions={mockActions}
        isLocked={false}
        onActionApply={onActionApply}
        onRefresh={onRefresh}
      />
    );
    
    expect(screen.getByTestId('unlock-icon')).toBeInTheDocument();
  });

  it('shows lock icon when locked', () => {
    const onActionApply = vi.fn();
    const onRefresh = vi.fn();
    
    render(
      <ActionsBar
        entityType="order"
        entityId="123"
        currentState="submitted"
        allowedActions={mockActions}
        isLocked={true}
        lockedBy="admin"
        onActionApply={onActionApply}
        onRefresh={onRefresh}
      />
    );
    
    expect(screen.getByTestId('lock-icon')).toBeInTheDocument();
  });

  it('disables action with reason tooltip', () => {
    const onActionApply = vi.fn();
    const onRefresh = vi.fn();
    
    render(
      <ActionsBar
        entityType="order"
        entityId="123"
        currentState="submitted"
        allowedActions={mockActions}
        isLocked={false}
        onActionApply={onActionApply}
        onRefresh={onRefresh}
      />
    );
    
    const rejectButton = screen.getByTestId('action-reject');
    expect(rejectButton).toBeDisabled();
    expect(rejectButton).toHaveAttribute('data-reason', 'Missing required documents');
  });

  it('calls onActionApply when enabled button clicked', async () => {
    const onActionApply = vi.fn().mockResolvedValue(undefined);
    const onRefresh = vi.fn().mockResolvedValue(undefined);
    
    render(
      <ActionsBar
        entityType="order"
        entityId="123"
        currentState="submitted"
        allowedActions={mockActions}
        isLocked={false}
        onActionApply={onActionApply}
        onRefresh={onRefresh}
      />
    );
    
    const approveButton = screen.getByTestId('action-approve');
    fireEvent.click(approveButton);
    
    await waitFor(() => {
      expect(onActionApply).toHaveBeenCalledWith('approve');
    });
  });

  it('refreshes stale data before applying action', async () => {
    const onActionApply = vi.fn().mockResolvedValue(undefined);
    const onRefresh = vi.fn().mockResolvedValue(undefined);
    
    vi.useFakeTimers();
    
    render(
      <ActionsBar
        entityType="order"
        entityId="123"
        currentState="submitted"
        allowedActions={mockActions}
        isLocked={false}
        onActionApply={onActionApply}
        onRefresh={onRefresh}
      />
    );
    
    // Fast-forward 30s to trigger stale warning
    vi.advanceTimersByTime(30000);
    
    await waitFor(() => {
      expect(screen.getByTestId('stale-warning')).toBeInTheDocument();
    });
    
    const approveButton = screen.getByTestId('action-approve');
    fireEvent.click(approveButton);
    
    await waitFor(() => {
      expect(onRefresh).toHaveBeenCalled();
      expect(onActionApply).toHaveBeenCalledWith('approve');
    });
    
    vi.useRealTimers();
  });

  it('shows no actions message when empty', () => {
    const onActionApply = vi.fn();
    const onRefresh = vi.fn();
    
    render(
      <ActionsBar
        entityType="order"
        entityId="123"
        currentState="completed"
        allowedActions={[]}
        isLocked={false}
        onActionApply={onActionApply}
        onRefresh={onRefresh}
      />
    );
    
    expect(screen.getByTestId('no-actions')).toBeInTheDocument();
    expect(screen.getByText(/No actions available/)).toBeInTheDocument();
  });
});
