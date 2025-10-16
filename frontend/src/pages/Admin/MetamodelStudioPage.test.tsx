import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MetamodelStudioPage } from './MetamodelStudioPage';
import * as AuthProvider from '../../components/AuthProvider.jsx';

// Mock AuthProvider
vi.mock('../../components/AuthProvider.jsx', () => ({
  useAuth: vi.fn(),
}));

describe('MetamodelStudioPage - S10-A RBAC & Layout', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should show access denied for user without CORE_ROLE_STUDIO role', () => {
    // Mock user without CORE_ROLE_STUDIO
    // @ts-expect-error - Mock return value for testing
    vi.mocked(AuthProvider.useAuth).mockReturnValue({
      user: { username: 'regular_user', roles: ['CORE_ROLE_USER'] },
      isAuthenticated: true,
      loading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    render(<MetamodelStudioPage />);

    // Check access denied message
    expect(screen.getByText('Přístup odepřen')).toBeInTheDocument();
    expect(screen.getByText(/CORE_ROLE_STUDIO/)).toBeInTheDocument();
  });

  it('should render Studio layout for user with CORE_ROLE_STUDIO role', () => {
    // Mock user with CORE_ROLE_STUDIO
    // @ts-expect-error - Mock return value for testing
    vi.mocked(AuthProvider.useAuth).mockReturnValue({
      user: { username: 'studio_admin', roles: ['CORE_ROLE_STUDIO'] },
      isAuthenticated: true,
      loading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    render(<MetamodelStudioPage />);

    // Check header
    expect(screen.getByText('🎨 Metamodel Studio')).toBeInTheDocument();
    expect(
      screen.getByText(/Admin GUI pro správu metamodelu/)
    ).toBeInTheDocument();

    // Check navigation tabs
    expect(screen.getByText('📦 Entities')).toBeInTheDocument();
    expect(screen.getByText('🔗 Relations')).toBeInTheDocument();
    expect(screen.getByText('✓ Validations')).toBeInTheDocument();
    expect(screen.getByText('⚡ Workflow Steps')).toBeInTheDocument();

    // Check 3-column layout panels
    expect(screen.getByText('📂 Model Tree')).toBeInTheDocument();
    expect(screen.getByText('✏️ Editor')).toBeInTheDocument();
    expect(screen.getByText('🔍 Diff & Validation')).toBeInTheDocument();
  });

  it('should show S10-B placeholder in ModelTree', () => {
    // @ts-expect-error - Mock return value for testing
    vi.mocked(AuthProvider.useAuth).mockReturnValue({
      user: { username: 'studio_admin', roles: ['CORE_ADMIN_STUDIO'] },
      isAuthenticated: true,
      loading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    render(<MetamodelStudioPage />);

    expect(
      screen.getByText(/S10-B: Tree view bude načítat entity z BE/)
    ).toBeInTheDocument();
  });

  it('should show S10-C placeholder in Editor', () => {
    // @ts-expect-error - Mock return value for testing
    vi.mocked(AuthProvider.useAuth).mockReturnValue({
      user: { username: 'studio_admin', roles: ['CORE_ADMIN_STUDIO'] },
      isAuthenticated: true,
      loading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    render(<MetamodelStudioPage />);

    expect(
      screen.getByText(
        /S10-C: EntityEditor, FieldEditor, RelationEditor, ValidationEditor/
      )
    ).toBeInTheDocument();
  });

  it('should show S10-D placeholder in Diff panel', () => {
    // @ts-expect-error - Mock return value for testing
    vi.mocked(AuthProvider.useAuth).mockReturnValue({
      user: { username: 'studio_admin', roles: ['CORE_ADMIN_STUDIO'] },
      isAuthenticated: true,
      loading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    render(<MetamodelStudioPage />);

    expect(
      screen.getByText(/S10-D: Diff viewer, Validate\/Propose\/Approve buttons/)
    ).toBeInTheDocument();
  });

  it('should disable Workflow Steps tab (S10-E placeholder)', () => {
    // @ts-expect-error - Mock return value for testing
    vi.mocked(AuthProvider.useAuth).mockReturnValue({
      user: { username: 'studio_admin', roles: ['CORE_ADMIN_STUDIO'] },
      isAuthenticated: true,
      loading: false,
      login: vi.fn(),
      logout: vi.fn(),
    });

    render(<MetamodelStudioPage />);

    // Workflow Steps tab should be visible but disabled
    const workflowTab = screen.getByText('⚡ Workflow Steps').closest('button');
    expect(workflowTab).toBeInTheDocument();
    expect(workflowTab).toBeDisabled();
  });
});
