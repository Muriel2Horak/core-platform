/**
 * Example: Integration of AiHelpWidget into AdminUsersPage
 * 
 * This file shows how to integrate the AI Help Widget into an existing page.
 * 
 * Steps to integrate:
 * 1. Import AiHelpWidget component
 * 2. Add data-route-id attribute to page container
 * 3. Add AiHelpWidget component with appropriate routeId
 * 4. Optionally add to page header/toolbar
 */

// Example modified AdminUsersPage.tsx:

import { useState, useEffect } from 'react';
import { 
  Box, 
  Typography, 
  FormControl, 
  InputLabel, 
  Select, 
  MenuItem, 
  Chip, 
  Alert,
  Stack,
} from '@mui/material';
import { Business as TenantIcon } from '@mui/icons-material';
import axios from 'axios';
import Users from '../../components/Users';
import { AiHelpWidget } from '../../components/AiHelpWidget'; // 1. Import

export const AdminUsersPageWithAiHelp = ({ user }: { user: any }) => {
  const [selectedTenant, setSelectedTenant] = useState<string>('all');
  const [tenants, setTenants] = useState<any[]>([]);

  useEffect(() => {
    const fetchTenants = async () => {
      try {
        const response = await axios.get('/api/tenants');
        setTenants(response.data || []);
      } catch (error) {
        console.error('Failed to fetch tenants:', error);
      }
    };

    if (user?.roles?.includes('CORE_ROLE_ADMIN')) {
      fetchTenants();
    }
  }, [user]);

  const isCoreAdmin = user?.roles?.includes('CORE_ROLE_ADMIN');
  const routeId = 'admin.users.list'; // 2. Define routeId

  return (
    <Box data-route-id={routeId}> {/* 2. Add data-route-id attribute */}
      {isCoreAdmin && (
        <Box sx={{ mb: 3, p: 2, background: 'rgba(25, 118, 210, 0.05)', borderRadius: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, flexWrap: 'wrap', justifyContent: 'space-between' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, flexWrap: 'wrap' }}>
              <Typography variant="h6">Správa uživatelů</Typography>

              <FormControl size="small" sx={{ minWidth: 250 }}>
                <InputLabel>Filtr tenantu</InputLabel>
                <Select
                  value={selectedTenant}
                  label="Filtr tenantu"
                  onChange={(e) => setSelectedTenant(e.target.value)}
                >
                  <MenuItem value="all"><em>🌐 Všechny tenanty</em></MenuItem>
                  {tenants.map((tenant: any) => (
                    <MenuItem key={tenant.key} value={tenant.key}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <TenantIcon fontSize="small" />
                        {tenant.key}
                      </Box>
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              {selectedTenant !== 'all' && (
                <Chip
                  label={`Filtrováno: ${selectedTenant}`}
                  color="primary"
                  onDelete={() => setSelectedTenant('all')}
                />
              )}
            </Box>

            {/* 3. Add AiHelpWidget to header */}
            <AiHelpWidget routeId={routeId} />
          </Box>

          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            {selectedTenant === 'all'
              ? 'Zobrazení uživatelů ze všech tenantů'
              : `Zobrazení uživatelů pouze z tenantu: ${selectedTenant}`
            }
          </Typography>
        </Box>
      )}

      {!isCoreAdmin && (
        <Stack direction="row" spacing={1} sx={{ mb: 3 }}>
          <Alert severity="info" sx={{ flex: 1 }}>
            📌 Vidíte uživatele z vašeho tenantu: <strong>{user?.tenantKey}</strong>
          </Alert>
          {/* 3. Add AiHelpWidget for non-admin view */}
          <AiHelpWidget routeId={routeId} />
        </Stack>
      )}

      <Users user={user} />
    </Box>
  );
};

/**
 * Integration Checklist:
 * 
 * ✅ 1. Import AiHelpWidget
 * ✅ 2. Define routeId (format: entity.viewKind, e.g., "users.list", "proposals.edit")
 * ✅ 3. Add data-route-id attribute to page container
 * ✅ 4. Add <AiHelpWidget routeId={routeId} /> to page header/toolbar
 * 
 * Optional:
 * - Set visible={false} to hide widget conditionally
 * - Widget automatically hides when AI_ENABLED=false
 * - Widget checks /api/admin/ai/status on mount
 * 
 * Metamodel Requirements:
 * - Entity must have AI configuration in metamodel YAML
 * - Fields should have pii, helpSafe, mask annotations
 * - Workflow actions should have howto, preconditions, postconditions
 * 
 * Testing:
 * 1. Enable AI in Admin > Metamodel Studio > AI Config tab
 * 2. Navigate to the page
 * 3. Click "Nápověda" button
 * 4. Verify structured help is displayed (META_ONLY mode)
 * 5. Check that no data values are shown (only metadata)
 */
