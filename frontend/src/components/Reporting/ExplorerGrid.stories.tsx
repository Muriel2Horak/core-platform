import type { Meta, StoryObj } from '@storybook/react';
import { ExplorerGrid } from './ExplorerGrid';

/**
 * ExplorerGrid is an advanced data grid component that provides:
 * - Server-side pagination, sorting, and filtering
 * - Inline cell editing with optimistic locking
 * - Bulk selection and batch operations
 * - CSV export functionality
 * 
 * It automatically fetches entity metadata from the backend to configure columns,
 * validation rules, and editable fields.
 */
const meta: Meta<typeof ExplorerGrid> = {
  title: 'Reporting/ExplorerGrid',
  component: ExplorerGrid,
  tags: ['autodocs'],
  parameters: {
    layout: 'fullscreen',
    docs: {
      description: {
        component: `
ExplorerGrid is the primary component for tabular data exploration in the reporting module.
It integrates with AG Grid Community for high-performance rendering and server-side operations.

## Features

- **Server-Side Operations**: Pagination, sorting, and filtering are handled by the backend
- **Inline Editing**: Click any cell to edit (if editable), saves automatically with optimistic locking
- **Bulk Actions**: Select multiple rows to activate/deactivate in bulk
- **Export**: Download filtered data as CSV
- **Auto-Configuration**: Fetches entity spec to configure columns, types, and validation

## API Integration

- \`GET /api/reports/metadata/{entity}/spec\` - Fetch entity metadata
- \`POST /api/reports/query\` - Query data with pagination/sort/filter
- \`PATCH /api/entities/{entity}/{id}\` - Update single row (with If-Match header)
- \`POST /api/entities/{entity}/bulk-update\` - Bulk update selected rows
        `,
      },
    },
  },
  argTypes: {
    entity: {
      control: 'select',
      options: ['users_directory', 'tenants_registry', 'keycloak_groups'],
      description: 'Entity name to display',
    },
    initialFilters: {
      control: 'object',
      description: 'Initial filter conditions',
    },
    onRowClick: {
      action: 'rowClicked',
      description: 'Callback when row is clicked',
    },
    onDrillDown: {
      action: 'drillDown',
      description: 'Callback for drill-down navigation',
    },
  },
};

export default meta;
type Story = StoryObj<typeof ExplorerGrid>;

/**
 * Default view with users_directory entity
 */
export const Default: Story = {
  args: {
    entity: 'users_directory',
    initialFilters: {},
  },
};

/**
 * Pre-filtered view showing only active users
 */
export const FilteredByStatus: Story = {
  args: {
    entity: 'users_directory',
    initialFilters: {
      status: 'ACTIVE',
    },
  },
};

/**
 * Tenants registry view
 */
export const TenantsView: Story = {
  args: {
    entity: 'tenants_registry',
    initialFilters: {},
  },
};

/**
 * Groups hierarchy view
 */
export const GroupsView: Story = {
  args: {
    entity: 'keycloak_groups',
    initialFilters: {},
  },
};

/**
 * With drill-down handler
 */
export const WithDrillDown: Story = {
  args: {
    entity: 'users_directory',
    initialFilters: {},
    onDrillDown: (data) => {
      console.log('Drill down to:', data);
      alert(`Drill down to: ${JSON.stringify(data, null, 2)}`);
    },
  },
};
