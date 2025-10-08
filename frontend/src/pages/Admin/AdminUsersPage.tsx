import { useState, useEffect } from 'react';
import { Box, Typography, FormControl, InputLabel, Select, MenuItem, Chip, Alert } from '@mui/material';
import { Business as TenantIcon } from '@mui/icons-material';
import axios from 'axios';
import Users from '../../components/Users';

export const AdminUsersPage = ({ user }: { user: any }) => {
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

  return (
    <Box>
      {isCoreAdmin && (
        <Box sx={{ mb: 3, p: 2, background: 'rgba(25, 118, 210, 0.05)', borderRadius: 2 }}>
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

          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            {selectedTenant === 'all'
              ? 'Zobrazení uživatelů ze všech tenantů'
              : `Zobrazení uživatelů pouze z tenantu: ${selectedTenant}`
            }
          </Typography>
        </Box>
      )}

      {!isCoreAdmin && (
        <Alert severity="info" sx={{ mb: 3 }}>
          📌 Vidíte uživatele z vašeho tenantu: <strong>{user?.tenantKey}</strong>
        </Alert>
      )}

      <Users user={user} />
    </Box>
  );
};

export default AdminUsersPage;
