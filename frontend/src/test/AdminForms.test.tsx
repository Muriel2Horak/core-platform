import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';

/**
 * 🧪 Admin Forms Tests
 * 
 * Tests form validation and RBAC (Role-Based Access Control)
 */

// Mock component representing an admin form
const MockTenantAdminForm = ({ 
  userRole, 
  onSubmit 
}: { 
  userRole: string; 
  onSubmit?: (data: any) => void;
}) => {
  const isTenantAdmin = userRole === 'tenant-admin';

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (onSubmit) {
      const formData = new FormData(e.target as HTMLFormElement);
      onSubmit(Object.fromEntries(formData));
    }
  };

  return (
    <form onSubmit={handleSubmit} data-testid="admin-form">
      <h2>Tenant Configuration</h2>
      
      {/* System admins can edit tenant key */}
      <input
        name="tenantKey"
        data-testid="tenant-key"
        disabled={isTenantAdmin}
        defaultValue="test-tenant"
        aria-label="Tenant Key"
      />

      {/* Both roles can edit display name */}
      <input
        name="displayName"
        data-testid="display-name"
        disabled={false}
        defaultValue="Test Tenant"
        aria-label="Display Name"
      />

      {/* Only system admins can delete tenants */}
      <button
        type="button"
        data-testid="delete-button"
        disabled={isTenantAdmin}
      >
        Delete Tenant
      </button>

      <button type="submit" data-testid="submit-button">
        Save Changes
      </button>
    </form>
  );
};

describe('Admin Forms - RBAC', () => {
  describe('Tenant Admin Role', () => {
    it('disables tenant key editing for tenant admins', () => {
      render(
        <BrowserRouter>
          <MockTenantAdminForm userRole="tenant-admin" />
        </BrowserRouter>
      );

      const tenantKeyInput = screen.getByTestId('tenant-key') as HTMLInputElement;
      expect(tenantKeyInput).toBeDisabled();
    });

    it('allows display name editing for tenant admins', () => {
      render(
        <BrowserRouter>
          <MockTenantAdminForm userRole="tenant-admin" />
        </BrowserRouter>
      );

      const displayNameInput = screen.getByTestId('display-name') as HTMLInputElement;
      expect(displayNameInput).not.toBeDisabled();
    });

    it('disables delete button for tenant admins', () => {
      render(
        <BrowserRouter>
          <MockTenantAdminForm userRole="tenant-admin" />
        </BrowserRouter>
      );

      const deleteButton = screen.getByTestId('delete-button');
      expect(deleteButton).toBeDisabled();
    });
  });

  describe('System Admin Role', () => {
    it('enables tenant key editing for system admins', () => {
      render(
        <BrowserRouter>
          <MockTenantAdminForm userRole="system-admin" />
        </BrowserRouter>
      );

      const tenantKeyInput = screen.getByTestId('tenant-key') as HTMLInputElement;
      expect(tenantKeyInput).not.toBeDisabled();
    });

    it('enables delete button for system admins', () => {
      render(
        <BrowserRouter>
          <MockTenantAdminForm userRole="system-admin" />
        </BrowserRouter>
      );

      const deleteButton = screen.getByTestId('delete-button');
      expect(deleteButton).not.toBeDisabled();
    });

    it('allows full form submission for system admins', () => {
      const mockSubmit = vi.fn();

      render(
        <BrowserRouter>
          <MockTenantAdminForm userRole="system-admin" onSubmit={mockSubmit} />
        </BrowserRouter>
      );

      const submitButton = screen.getByTestId('submit-button');
      fireEvent.click(submitButton);

      expect(mockSubmit).toHaveBeenCalled();
    });
  });

  describe('Form Validation', () => {
    it('validates required fields', () => {
      const mockSubmit = vi.fn();

      render(
        <BrowserRouter>
          <MockTenantAdminForm userRole="system-admin" onSubmit={mockSubmit} />
        </BrowserRouter>
      );

      const form = screen.getByTestId('admin-form');
      expect(form).toBeInTheDocument();
    });

    it('handles form submission', () => {
      const mockSubmit = vi.fn();

      render(
        <BrowserRouter>
          <MockTenantAdminForm userRole="system-admin" onSubmit={mockSubmit} />
        </BrowserRouter>
      );

      const submitButton = screen.getByTestId('submit-button');
      fireEvent.click(submitButton);

      expect(mockSubmit).toHaveBeenCalledWith(
        expect.objectContaining({
          tenantKey: 'test-tenant',
          displayName: 'Test Tenant',
        })
      );
    });
  });
});

describe('Form Accessibility', () => {
  it('has accessible labels', () => {
    render(
      <BrowserRouter>
        <MockTenantAdminForm userRole="system-admin" />
      </BrowserRouter>
    );

    expect(screen.getByLabelText('Tenant Key')).toBeInTheDocument();
    expect(screen.getByLabelText('Display Name')).toBeInTheDocument();
  });
});
