import React from 'react';
import { Box, Typography, Chip, Alert } from '@mui/material';
import { Business as TenantIcon } from '@mui/icons-material';
import { AdminRolesPage } from '../Admin/AdminRolesPage';

/**
 * 🔐 Tenant Roles Page
 * Správa rolí pro konkrétní tenant (pouze svůj tenant)
 */
export const TenantRolesPage = ({ user }) => {
  return (
    <Box>
      {/* Header with Tenant Info */}
      <Box sx={{ mb: 3, p: 2, background: 'rgba(123, 31, 162, 0.05)', borderRadius: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="h6">
            Správa rolí pro tenant:
          </Typography>
          <Chip 
            icon={<TenantIcon />}
            label={user?.tenantKey || 'Unknown'} 
            color="primary" 
            sx={{ fontWeight: 600 }}
          />
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          Zobrazení a správa rolí pouze pro váš tenant
        </Typography>
      </Box>

      {/* Alert - Info */}
      <Alert severity="info" sx={{ mb: 3 }}>
        📌 Vidíte pouze role z vašeho tenantu: <strong>{user?.tenantKey}</strong>
      </Alert>

      {/* Roles Component - filtrované na tenant */}
      <AdminRolesPage user={user} tenantFilter={user?.tenantKey} />
    </Box>
  );
};

export default TenantRolesPage;
