import React from 'react';
import { Box, Typography, Chip, Alert } from '@mui/material';
import { Business as TenantIcon } from '@mui/icons-material';
import Users from '../../components/Users.jsx';

/**
 * 👥 Tenant Users Page
 * Správa uživatelů pro konkrétní tenant (pouze svůj tenant)
 */
export const TenantUsersPage = ({ user }) => {
  return (
    <Box>
      {/* Header with Tenant Info */}
      <Box sx={{ mb: 3, p: 2, background: 'rgba(25, 118, 210, 0.05)', borderRadius: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="h6">
            Správa uživatelů pro tenant:
          </Typography>
          <Chip 
            icon={<TenantIcon />}
            label={user?.tenantKey || 'Unknown'} 
            color="primary" 
            sx={{ fontWeight: 600 }}
          />
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          Zobrazení a správa uživatelů pouze pro váš tenant
        </Typography>
      </Box>

      {/* Alert - Info */}
      <Alert severity="info" sx={{ mb: 3 }}>
        📌 Vidíte pouze uživatele z vašeho tenantu: <strong>{user?.tenantKey}</strong>
      </Alert>

      {/* Users Component - filtrované na tenant */}
      <Users user={user} tenantFilter={user?.tenantKey} />
    </Box>
  );
};

export default TenantUsersPage;
