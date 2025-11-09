# ADM-001: Global Module Catalog

**Status:** ⏳ **PENDING**  
**Effort:** 3 dny  
**Priority:** 🔥 HIGH  
**Dependencies:** MOD-002  
**Category:** Admin UI

---

## 📖 User Story

**As a platform admin**,  
I want to see all available modules in a catalog,  
So that I can install and manage modules.

---

## 🎯 Acceptance Criteria

- ⏳ Module catalog UI (Material Design table)
- ⏳ Columns: Name, Version, Status, Dependencies, Actions
- ⏳ Status indicators: Available, Installed, Enabled, Disabled
- ⏳ Actions: Install, Enable, Disable, Uninstall
- ⏳ Dependency graph visualization (D3.js)
- ⏳ Filter by status, search by name

---

## 💻 Implementation

### React Component

```tsx
// frontend/src/pages/admin/ModuleCatalog.tsx

import React, { useState, useEffect } from 'react';
import {
  Table, TableBody, TableCell, TableHead, TableRow,
  Button, Chip, IconButton, Tooltip
} from '@mui/material';
import { Check, Close, Power, PowerOff, Delete } from '@mui/icons-material';
import { ModuleInfo, ModuleStatus } from '@/types/modules';
import { moduleApi } from '@/api/modules';

export const ModuleCatalog: React.FC = () => {
  const [modules, setModules] = useState<ModuleInfo[]>([]);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    loadModules();
  }, []);
  
  const loadModules = async () => {
    setLoading(true);
    const data = await moduleApi.list();
    setModules(data);
    setLoading(false);
  };
  
  const handleInstall = async (moduleId: string) => {
    await moduleApi.install(moduleId);
    await loadModules();
  };
  
  const handleEnable = async (moduleId: string) => {
    await moduleApi.enable(moduleId);
    await loadModules();
  };
  
  const handleDisable = async (moduleId: string) => {
    if (confirm('Disable module? This will hide it from all tenants.')) {
      await moduleApi.disable(moduleId);
      await loadModules();
    }
  };
  
  const handleUninstall = async (moduleId: string) => {
    if (confirm('Uninstall module? Data will be retained but module removed.')) {
      await moduleApi.uninstall(moduleId);
      await loadModules();
    }
  };
  
  const getStatusChip = (status: ModuleStatus) => {
    const colors = {
      AVAILABLE: 'default',
      INSTALLED: 'info',
      ENABLED: 'success',
      DISABLED: 'warning',
      ERROR: 'error'
    };
    
    return (
      <Chip 
        label={status} 
        color={colors[status] as any} 
        size="small"
      />
    );
  };
  
  return (
    <div>
      <h1>Module Catalog</h1>
      
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Module</TableCell>
            <TableCell>Version</TableCell>
            <TableCell>Status</TableCell>
            <TableCell>Dependencies</TableCell>
            <TableCell>Actions</TableCell>
          </TableRow>
        </TableHead>
        
        <TableBody>
          {modules.map(module => (
            <TableRow key={module.id}>
              <TableCell>
                <strong>{module.name}</strong>
                <div style={{ fontSize: '0.85em', color: '#666' }}>
                  {module.description}
                </div>
              </TableCell>
              
              <TableCell>{module.version}</TableCell>
              
              <TableCell>
                {getStatusChip(module.status)}
              </TableCell>
              
              <TableCell>
                {module.dependencies?.map(dep => (
                  <Chip 
                    key={dep} 
                    label={dep} 
                    size="small" 
                    variant="outlined"
                    sx={{ mr: 0.5 }}
                  />
                ))}
              </TableCell>
              
              <TableCell>
                {module.status === 'AVAILABLE' && (
                  <Button 
                    size="small" 
                    onClick={() => handleInstall(module.id)}
                  >
                    Install
                  </Button>
                )}
                
                {module.status === 'INSTALLED' && (
                  <Button 
                    size="small" 
                    startIcon={<Power />}
                    onClick={() => handleEnable(module.id)}
                  >
                    Enable
                  </Button>
                )}
                
                {module.status === 'ENABLED' && (
                  <>
                    <Tooltip title="Disable">
                      <IconButton 
                        size="small"
                        onClick={() => handleDisable(module.id)}
                      >
                        <PowerOff />
                      </IconButton>
                    </Tooltip>
                  </>
                )}
                
                {(module.status === 'DISABLED' || module.status === 'INSTALLED') && (
                  <Tooltip title="Uninstall">
                    <IconButton 
                      size="small"
                      onClick={() => handleUninstall(module.id)}
                    >
                      <Delete />
                    </IconButton>
                  </Tooltip>
                )}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
};
```

### Dependency Graph

```tsx
// DependencyGraph.tsx

import React from 'react';
import ReactFlow, { Node, Edge } from 'reactflow';
import { ModuleInfo } from '@/types/modules';

interface Props {
  modules: ModuleInfo[];
}

export const DependencyGraph: React.FC<Props> = ({ modules }) => {
  const nodes: Node[] = modules.map((module, index) => ({
    id: module.id,
    data: { label: module.name },
    position: { x: index * 200, y: 100 }
  }));
  
  const edges: Edge[] = modules.flatMap(module =>
    (module.dependencies || []).map(depId => ({
      id: `${module.id}-${depId}`,
      source: depId,
      target: module.id,
      animated: true
    }))
  );
  
  return (
    <div style={{ height: 500 }}>
      <ReactFlow nodes={nodes} edges={edges} />
    </div>
  );
};
```

---

## 🧪 Testing

```typescript
// ModuleCatalog.test.tsx

import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ModuleCatalog } from './ModuleCatalog';
import { moduleApi } from '@/api/modules';

jest.mock('@/api/modules');

describe('ModuleCatalog', () => {
  it('should display modules', async () => {
    (moduleApi.list as jest.Mock).mockResolvedValue([
      { id: 'helpdesk', name: 'Helpdesk', status: 'ENABLED' }
    ]);
    
    render(<ModuleCatalog />);
    
    await waitFor(() => {
      expect(screen.getByText('Helpdesk')).toBeInTheDocument();
      expect(screen.getByText('ENABLED')).toBeInTheDocument();
    });
  });
  
  it('should install module', async () => {
    (moduleApi.list as jest.Mock).mockResolvedValue([
      { id: 'crm', name: 'CRM', status: 'AVAILABLE' }
    ]);
    
    (moduleApi.install as jest.Mock).mockResolvedValue({});
    
    render(<ModuleCatalog />);
    
    const installBtn = await screen.findByText('Install');
    fireEvent.click(installBtn);
    
    await waitFor(() => {
      expect(moduleApi.install).toHaveBeenCalledWith('crm');
    });
  });
});
```

---

**Last Updated:** 9. listopadu 2025
