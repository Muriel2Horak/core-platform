import React from 'react';
import { Box, Typography, Chip, Alert } from '@mui/material';
import { Business as TenantIcon } from '@mui/icons-material';
import { KeycloakSyncPage } from '../Admin/KeycloakSyncPage';

/**
 * 🔄 Tenant Keycloak Sync Page
 * Synchronizace z Keycloak pouze pro svůj tenant
 */
export const TenantKeycloakSyncPage = ({ user }) => {
  return (
    <Box>
      {/* Header with Tenant Info */}
      <Box sx={{ mb: 3, p: 2, background: 'rgba(25, 118, 210, 0.05)', borderRadius: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="h6">
            Keycloak synchronizace pro tenant:
          </Typography>
          <Chip 
            icon={<TenantIcon />}
            label={user?.tenantKey || 'Unknown'} 
            color="primary" 
            sx={{ fontWeight: 600 }}
          />
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          Synchronizace uživatelů, rolí a skupin z Keycloak pro váš tenant
        </Typography>
      </Box>

      {/* Alert - Info */}
      <Alert severity="info" sx={{ mb: 3 }}>
        📌 Synchronizace probíhá pouze pro váš tenant: <strong>{user?.tenantKey}</strong>
      </Alert>

      {/* Keycloak Sync Component - locked na tenant */}
      <KeycloakSyncPage user={user} tenantLocked={true} />
    </Box>
  );
};

export default TenantKeycloakSyncPage;
