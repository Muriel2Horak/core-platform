import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TimelinePanel } from '../TimelinePanel';

/**
 * 🧪 W6: TimelinePanel Unit Tests
 * 
 * Tests:
 * - Duration formatting (ms→human readable)
 * - SLA badge rendering (OK/WARN/BREACH with icons)
 * - Total workflow duration display
 * - Empty state rendering
 * 
 * @since 2025-10-14
 */

const mockHistory = {
  entityType: 'order',
  entityId: '123',
  entries: [
    {
      eventType: 'STATE_ENTER',
      fromState: 'draft',
      toState: 'submitted',
      transitionCode: 'submit',
      timestamp: new Date(Date.now() - 3600000).toISOString(), // 1 hour ago
      durationMs: 300000, // 5 minutes
      actor: 'john.doe',
      slaStatus: 'OK' as const,
    },
    {
      eventType: 'STATE_ENTER',
      fromState: 'submitted',
      toState: 'approved',
      transitionCode: 'approve',
      timestamp: new Date(Date.now() - 7200000).toISOString(), // 2 hours ago
      durationMs: 900000, // 15 minutes
      actor: 'manager',
      slaStatus: 'WARN' as const,
    },
    {
      eventType: 'STATE_ENTER',
      fromState: 'approved',
      toState: 'completed',
      transitionCode: 'complete',
      timestamp: new Date(Date.now() - 86400000).toISOString(), // 1 day ago
      durationMs: 7200000, // 2 hours
      actor: 'system',
      slaStatus: 'BREACH' as const,
    },
  ],
  totalDurationMs: 8400000, // 2h 20m
};

const emptyHistory = {
  entityType: 'order',
  entityId: '456',
  entries: [],
  totalDurationMs: 0,
};

describe('TimelinePanel', () => {
  it('renders workflow history with all entries', () => {
    render(<TimelinePanel history={mockHistory} />);
    
    expect(screen.getByText('Workflow Timeline')).toBeInTheDocument();
    expect(screen.getByText(/draft → submitted/)).toBeInTheDocument();
    expect(screen.getByText(/submitted → approved/)).toBeInTheDocument();
    expect(screen.getByText(/approved → completed/)).toBeInTheDocument();
  });

  it('displays total duration in header', () => {
    render(<TimelinePanel history={mockHistory} />);
    
    expect(screen.getByText(/Total: 2h 20m/)).toBeInTheDocument();
  });

  it('formats durations correctly', () => {
    render(<TimelinePanel history={mockHistory} />);
    
    const durations = screen.getAllByTestId('duration');
    expect(durations[0]).toHaveTextContent('5m 0s');
    expect(durations[1]).toHaveTextContent('15m 0s');
    expect(durations[2]).toHaveTextContent('2h 0m');
  });

  it('renders SLA badges with correct colors', () => {
    render(<TimelinePanel history={mockHistory} />);
    
    const slaBadges = screen.getAllByTestId('sla-badge');
    expect(slaBadges).toHaveLength(3);
    
    expect(slaBadges[0]).toHaveTextContent('OK');
    expect(slaBadges[1]).toHaveTextContent('WARN');
    expect(slaBadges[2]).toHaveTextContent('BREACH');
  });

  it('shows actor names in timestamps', () => {
    render(<TimelinePanel history={mockHistory} />);
    
    expect(screen.getByText(/by john.doe/)).toBeInTheDocument();
    expect(screen.getByText(/by manager/)).toBeInTheDocument();
    expect(screen.getByText(/by system/)).toBeInTheDocument();
  });

  it('displays relative timestamps', () => {
    render(<TimelinePanel history={mockHistory} />);
    
    const timestamps = screen.getAllByTestId('timestamp');
    expect(timestamps).toHaveLength(3);
    
    // date-fns formatDistanceToNow
    expect(timestamps[0]).toHaveTextContent(/ago/);
  });

  it('handles empty history gracefully', () => {
    render(<TimelinePanel history={emptyHistory} />);
    
    expect(screen.getByText(/No workflow history yet/)).toBeInTheDocument();
    expect(screen.getByText(/Total: 0s/)).toBeInTheDocument();
  });

  it('hides SLA badge when status is NONE', () => {
    const historyWithNone = {
      ...mockHistory,
      entries: [
        {
          ...mockHistory.entries[0],
          slaStatus: 'NONE' as const,
        },
      ],
    };
    
    render(<TimelinePanel history={historyWithNone} />);
    
    expect(screen.queryByTestId('sla-badge')).not.toBeInTheDocument();
  });
});
