import React from 'react';
import { Box, Card, CardContent, Typography, Grid, Chip } from '@mui/material';
import { Business as TenantIcon } from '@mui/icons-material';

/**
 * 🏢 Tenant Dashboard
 * Dashboard pro správce tenantu - přehled vlastního tenantu
 */
export const TenantDashboard = ({ user }) => {
  return (
    <Box sx={{ p: 3 }}>
      {/* Header */}
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" gutterBottom sx={{
          background: 'linear-gradient(135deg, #1976d2, #42a5f5)',
          WebkitBackgroundClip: 'text',
          WebkitTextFillColor: 'transparent',
          fontWeight: 700,
        }}>
          🏢 Tenant Dashboard
        </Typography>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mt: 2 }}>
          <Typography variant="body1" color="text.secondary">
            Přehled vašeho tenantu:
          </Typography>
          <Chip 
            icon={<TenantIcon />}
            label={user?.tenantKey || 'Unknown'} 
            color="primary" 
            sx={{ fontWeight: 600 }}
          />
        </Box>
      </Box>

      {/* Stats Grid */}
      <Grid container spacing={3}>
        <Grid item xs={12} md={6} lg={3}>
          <Card sx={{ 
            background: 'linear-gradient(135deg, rgba(25, 118, 210, 0.1), rgba(25, 118, 210, 0.05))',
            border: '1px solid rgba(25, 118, 210, 0.3)'
          }}>
            <CardContent>
              <Typography variant="h6" color="primary">Uživatelé</Typography>
              <Typography variant="h3" sx={{ mt: 2, fontWeight: 700 }}>-</Typography>
              <Typography variant="body2" color="text.secondary">Aktivní uživatelé</Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6} lg={3}>
          <Card sx={{ 
            background: 'linear-gradient(135deg, rgba(123, 31, 162, 0.1), rgba(123, 31, 162, 0.05))',
            border: '1px solid rgba(123, 31, 162, 0.3)'
          }}>
            <CardContent>
              <Typography variant="h6" sx={{ color: '#7b1fa2' }}>Role</Typography>
              <Typography variant="h3" sx={{ mt: 2, fontWeight: 700 }}>-</Typography>
              <Typography variant="body2" color="text.secondary">Definované role</Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6} lg={3}>
          <Card sx={{ 
            background: 'linear-gradient(135deg, rgba(56, 142, 60, 0.1), rgba(56, 142, 60, 0.05))',
            border: '1px solid rgba(56, 142, 60, 0.3)'
          }}>
            <CardContent>
              <Typography variant="h6" sx={{ color: '#388e3c' }}>Skupiny</Typography>
              <Typography variant="h3" sx={{ mt: 2, fontWeight: 700 }}>-</Typography>
              <Typography variant="body2" color="text.secondary">Aktivní skupiny</Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={6} lg={3}>
          <Card sx={{ 
            background: 'linear-gradient(135deg, rgba(245, 124, 0, 0.1), rgba(245, 124, 0, 0.05))',
            border: '1px solid rgba(245, 124, 0, 0.3)'
          }}>
            <CardContent>
              <Typography variant="h6" sx={{ color: '#f57c00' }}>Stav</Typography>
              <Chip label="Aktivní" color="success" sx={{ mt: 2 }} />
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>Tenant status</Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Info Card */}
      <Card sx={{ mt: 4, background: 'rgba(25, 118, 210, 0.02)' }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Správa tenantu
          </Typography>
          <Typography variant="body1" color="text.secondary">
            V této sekci můžete spravovat uživatele, role a skupiny pro váš tenant <strong>{user?.tenantKey}</strong>.
            Použijte menu vlevo pro navigaci mezi jednotlivými sekcemi.
          </Typography>
        </CardContent>
      </Card>
    </Box>
  );
};

export default TenantDashboard;
