import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ModelTree } from './ModelTree';

// Mock fetch
global.fetch = vi.fn();

describe('ModelTree - S10-B Read-only Viewer', () => {
  const mockEntities = {
    status: 'success',
    entitiesCount: 3,
    entities: [
      {
        name: 'User',
        entity: 'User',
        table: 'users_directory',
        fields: [
          { name: 'id', type: 'bigint' },
          { name: 'username', type: 'string' },
        ],
      },
      {
        name: 'Role',
        entity: 'Role',
        table: 'roles',
        fields: [{ name: 'id', type: 'bigint' }],
      },
      {
        name: 'Tenant',
        entity: 'Tenant',
        table: 'tenants',
        fields: [{ name: 'id', type: 'bigint' }],
      },
    ],
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should load and display entities from API', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockEntities,
    });

    const onSelectEntity = vi.fn();
    render(<ModelTree onSelectEntity={onSelectEntity} selectedEntity={null} />);

    // Should show loading initially
    expect(screen.getByRole('progressbar')).toBeInTheDocument();

    // Wait for entities to load
    await waitFor(() => {
      expect(screen.getByText('User')).toBeInTheDocument();
    });

    // Check all entities are displayed
    expect(screen.getByText('User')).toBeInTheDocument();
    expect(screen.getByText('Role')).toBeInTheDocument();
    expect(screen.getByText('Tenant')).toBeInTheDocument();

    // Check entity count
    expect(screen.getByText(/3 entit/)).toBeInTheDocument();
  });

  it('should filter entities by search term', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockEntities,
    });

    const onSelectEntity = vi.fn();
    render(<ModelTree onSelectEntity={onSelectEntity} selectedEntity={null} />);

    // Wait for entities to load
    await waitFor(() => {
      expect(screen.getByText('User')).toBeInTheDocument();
    });

    // Type in search box
    const searchBox = screen.getByPlaceholderText('Hledat entitu...');
    await userEvent.type(searchBox, 'User');

    // Should show only User entity
    await waitFor(() => {
      expect(screen.getByText('User')).toBeInTheDocument();
      expect(screen.queryByText('Role')).not.toBeInTheDocument();
      expect(screen.queryByText('Tenant')).not.toBeInTheDocument();
    });

    // Check filtered count
    expect(screen.getByText(/1 entit/)).toBeInTheDocument();
  });

  it('should call onSelectEntity when entity is clicked', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockEntities,
    });

    const onSelectEntity = vi.fn();
    render(<ModelTree onSelectEntity={onSelectEntity} selectedEntity={null} />);

    // Wait for entities to load
    await waitFor(() => {
      expect(screen.getByText('User')).toBeInTheDocument();
    });

    // Click on User entity
    await userEvent.click(screen.getByText('User'));

    // Should call onSelectEntity with User entity
    expect(onSelectEntity).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'User',
        entity: 'User',
        table: 'users_directory',
      })
    );
  });

  it('should show error message on API failure', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: false,
      status: 500,
      statusText: 'Internal Server Error',
    });

    const onSelectEntity = vi.fn();
    render(<ModelTree onSelectEntity={onSelectEntity} selectedEntity={null} />);

    // Wait for error message
    await waitFor(() => {
      expect(
        screen.getByText(/HTTP 500: Internal Server Error/)
      ).toBeInTheDocument();
    });
  });

  it('should show info message when no entities match search', async () => {
    (global.fetch as any).mockResolvedValueOnce({
      ok: true,
      json: async () => mockEntities,
    });

    const onSelectEntity = vi.fn();
    render(<ModelTree onSelectEntity={onSelectEntity} selectedEntity={null} />);

    // Wait for entities to load
    await waitFor(() => {
      expect(screen.getByText('User')).toBeInTheDocument();
    });

    // Search for non-existent entity
    const searchBox = screen.getByPlaceholderText('Hledat entitu...');
    await userEvent.type(searchBox, 'NonExistent');

    // Should show "no entities found"
    await waitFor(() => {
      expect(screen.getByText('Žádné entity nenalezeny')).toBeInTheDocument();
    });
  });
});
