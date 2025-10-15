import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { AiHelpWidget } from './AiHelpWidget';

// Mock fetch
global.fetch = vi.fn();

describe('AiHelpWidget', () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it('should not render when AI is disabled', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ enabled: false }),
    });

    render(<AiHelpWidget routeId="test.route" />);

    await waitFor(() => {
      expect(screen.queryByTestId('ai-help-button')).not.toBeInTheDocument();
    });
  });

  it('should render help button when AI is enabled', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ enabled: true }),
    });

    render(<AiHelpWidget routeId="test.route" />);

    await waitFor(() => {
      expect(screen.getByTestId('ai-help-button')).toBeInTheDocument();
    });
  });

  it('should fetch AI context when help button is clicked', async () => {
    // Mock AI status check
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ enabled: true }),
    });

    // Mock AI context fetch
    const mockContext = {
      route: {
        routeId: 'test.route',
        viewKind: 'list',
        entity: 'TestEntity',
        title: 'Test Page',
      },
      fields: [
        {
          name: 'testField',
          type: 'string',
          label: 'Test Field',
          required: true,
        },
      ],
      actions: [
        {
          code: 'test_action',
          label: 'Test Action',
          howto: ['Step 1', 'Step 2'],
        },
      ],
    };

    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockContext,
    });

    render(<AiHelpWidget routeId="test.route" />);

    await waitFor(() => {
      expect(screen.getByTestId('ai-help-button')).toBeInTheDocument();
    });

    // Click help button
    fireEvent.click(screen.getByTestId('ai-help-button'));

    // Wait for dialog to open
    await waitFor(() => {
      expect(screen.getByTestId('ai-help-dialog')).toBeInTheDocument();
    });

    // Verify context is fetched with correct routeId
    expect(global.fetch).toHaveBeenCalledWith(
      expect.stringContaining('/api/ai/context?routeId=test.route')
    );
  });

  it('should display error when AI context fetch fails', async () => {
    // Mock AI status check
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ enabled: true }),
    });

    // Mock failed context fetch
    (global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 500,
    });

    render(<AiHelpWidget routeId="test.route" />);

    await waitFor(() => {
      expect(screen.getByTestId('ai-help-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-help-button'));

    await waitFor(() => {
      expect(screen.getByText(/Chyba při načítání nápovědy/)).toBeInTheDocument();
    });
  });

  it('should display specific error for 404 response', async () => {
    // Mock AI status check
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ enabled: true }),
    });

    // Mock 404 response (AI disabled)
    (global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 404,
    });

    render(<AiHelpWidget routeId="test.route" />);

    await waitFor(() => {
      expect(screen.getByTestId('ai-help-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-help-button'));

    await waitFor(() => {
      expect(screen.getByText(/AI funkce nejsou dostupné/)).toBeInTheDocument();
    });
  });

  it('should display locked warning for 423 response', async () => {
    // Mock AI status check
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ enabled: true }),
    });

    // Mock 423 response (locked)
    (global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 423,
    });

    render(<AiHelpWidget routeId="test.route" />);

    await waitFor(() => {
      expect(screen.getByTestId('ai-help-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-help-button'));

    await waitFor(() => {
      expect(screen.getByText(/uzamčena pro úpravy/)).toBeInTheDocument();
    });
  });

  it('should display updating warning when state.updating is true', async () => {
    // Mock AI status check
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ enabled: true }),
    });

    // Mock context with updating state
    const mockContext = {
      route: {
        routeId: 'test.route',
        viewKind: 'edit',
        entity: 'TestEntity',
        title: 'Test Page',
      },
      state: {
        current: 'draft',
        updating: true,
      },
    };

    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockContext,
    });

    render(<AiHelpWidget routeId="test.route" />);

    await waitFor(() => {
      expect(screen.getByTestId('ai-help-button')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('ai-help-button'));

    await waitFor(() => {
      expect(screen.getByText(/Probíhá aktualizace/)).toBeInTheDocument();
    });
  });

  it('should not render when visible prop is false', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => ({ enabled: true }),
    });

    render(<AiHelpWidget routeId="test.route" visible={false} />);

    await waitFor(() => {
      expect(screen.queryByTestId('ai-help-button')).not.toBeInTheDocument();
    });
  });
});
