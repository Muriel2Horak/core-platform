import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Alert,
  CircularProgress,
} from '@mui/material';
import {
  Business as BusinessIcon,
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';

function Tenants({ user }) {
  const [tenants, setTenants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Check if user has permission to view tenants (admin only)
  const hasPermission = user?.roles?.includes('CORE_ROLE_ADMIN');

  // Load tenants
  const loadTenants = async () => {
    if (!hasPermission) {
      setError('Nemáte oprávnění k zobrazení seznamu tenantů. Vyžaduje se role administrátora.');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      const tenantsData = await apiService.getTenants();
      setTenants(tenantsData || []);
      logger.info('Tenants loaded', { count: tenantsData?.length || 0 });
    } catch (error) {
      logger.error('Failed to load tenants', { error: error.message });
      setError('Nepodařilo se načíst seznam tenantů.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTenants();
  }, []);

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return 'Neuvedeno';
    try {
      return new Date(dateString).toLocaleDateString('cs-CZ');
    } catch {
      return 'Neplatné datum';
    }
  };

  if (!hasPermission) {
    return (
      <Box>
        <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600 }}>
          Tenanti
        </Typography>
        
        <Alert severity="error">
          Nemáte oprávnění k zobrazení seznamu tenantů. Vyžaduje se role administrátora.
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600 }}>
        Správa tenantů
      </Typography>
      
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Zde můžete spravovat tenenty v multi-tenant systému.
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress />
            </Box>
          ) : tenants.length === 0 ? (
            <Box sx={{ textAlign: 'center', p: 3 }}>
              <BusinessIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                Nebyli nalezeni žádní tenanti
              </Typography>
            </Box>
          ) : (
            <TableContainer component={Paper} elevation={0}>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Název</TableCell>
                    <TableCell>ID</TableCell>
                    <TableCell>Stav</TableCell>
                    <TableCell>Vytvořeno</TableCell>
                    <TableCell>Popis</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tenants.map((tenant, index) => (
                    <TableRow key={tenant.id || index} hover>
                      <TableCell>
                        <Typography variant="body1" sx={{ fontWeight: 500 }}>
                          {tenant?.name || 'Bez názvu'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                          {tenant?.id || 'Neuvedeno'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Chip
                          label={tenant?.enabled ? 'Aktivní' : 'Neaktivní'}
                          color={tenant?.enabled ? 'success' : 'default'}
                          size="small"
                        />
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2">
                          {formatDate(tenant?.createdAt)}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Typography variant="body2" color="text.secondary">
                          {tenant?.description || 'Bez popisu'}
                        </Typography>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </CardContent>
      </Card>
    </Box>
  );
}

export default Tenants;