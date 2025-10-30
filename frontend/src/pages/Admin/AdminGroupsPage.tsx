import { useState, useEffect } from 'react';
import { Box, Typography, FormControl, InputLabel, Select, MenuItem, Chip } from '@mui/material';
import { Business as TenantIcon } from '@mui/icons-material';
import axios from 'axios';
import Groups from '../../components/Groups';
import { AiHelpWidget } from '../../components/AiHelpWidget';

export const AdminGroupsPage = ({ user }: { user: any }) => {
  const [selectedTenant, setSelectedTenant] = useState<string>('all');
  const [tenants, setTenants] = useState<any[]>([]);
  
  const routeId = 'admin.groups.list';

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

  return (
    <Box data-route-id={routeId}>
      {isCoreAdmin && (
        <Box sx={{ mb: 3, p: 2, background: 'rgba(25, 118, 210, 0.05)', borderRadius: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, flexWrap: 'wrap', justifyContent: 'space-between' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 3, flexWrap: 'wrap' }}>
              <Typography variant="h6">Správa skupin</Typography>

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
                        {tenant.displayName || tenant.key}
                      </Box>
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>

              <Chip 
                label={selectedTenant === 'all' ? 'Všechny tenanty' : selectedTenant} 
                color={selectedTenant === 'all' ? 'default' : 'primary'}
                size="small"
              />
            </Box>

            <AiHelpWidget 
              context={{
                routeId,
                feature: 'groups-management',
                filters: { tenant: selectedTenant }
              }}
            />
          </Box>
        </Box>
      )}

      <Groups user={user} tenantFilter={selectedTenant} />
    </Box>
  );
};
