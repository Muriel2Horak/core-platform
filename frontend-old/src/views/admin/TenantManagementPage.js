import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
  CircularProgress,
  IconButton,
  Tooltip,
  Grid
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Visibility as ViewIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Refresh as RefreshIcon,
  Business as BusinessIcon
} from '@mui/icons-material';
import PageContainer from '../../components/container/PageContainer';
import authService from '../../services/auth';
import logger from '../../services/logger';

const TenantManagementPage = () => {
  const [tenants, setTenants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [statsDialogOpen, setStatsDialogOpen] = useState(false);
  const [selectedTenant, setSelectedTenant] = useState(null);
  const [tenantStats, setTenantStats] = useState(null);
  const [statsLoading, setStatsLoading] = useState(false);

  // Create tenant form state
  const [newTenant, setNewTenant] = useState({
    key: '',
    displayName: ''
  });
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState(null);

  useEffect(() => {
    loadTenants();
  }, []);

  /**
   * 📋 LOAD TENANTS: Načte seznam všech tenantů
   */
  const loadTenants = async () => {
    try {
      setLoading(true);
      setError(null);

      logger.pageView('/admin/tenants', { operation: 'load_tenants' });

      const response = await authService.apiCall('/admin/tenants');

      if (!response || !response.ok) {
        throw new Error(`HTTP ${response?.status || 'Unknown'}`);
      }

      const data = await response.json();
      
      // Handle new API response format
      const tenantsList = data.tenants || data;
      setTenants(tenantsList);

      logger.userAction('TENANTS_LOADED', { count: tenantsList.length });

    } catch (err) {
      console.error('Failed to load tenants:', err);
      setError(`Nepodařilo se načíst seznam tenantů: ${err.message}`);
      logger.error('TENANTS_LOAD_ERROR', err.message);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 🆕 CREATE TENANT: Vytvoří nový tenant
   */
  const handleCreateTenant = async () => {
    try {
      setCreateLoading(true);
      setCreateError(null);

      // Validate input
      if (!newTenant.key.trim()) {
        setCreateError('Tenant key je povinný');
        return;
      }

      if (!newTenant.displayName.trim()) {
        setCreateError('Tenant name je povinný');
        return;
      }

      // Validate tenant key format (lowercase, alphanumeric + hyphens)
      const keyRegex = /^[a-z0-9]([a-z0-9-]*[a-z0-9])?$/;
      if (!keyRegex.test(newTenant.key)) {
        setCreateError('Tenant key může obsahovat pouze malá písmena, číslice a pomlčky. Musí začínat a končit alfanumerickým znakem.');
        return;
      }

      logger.userAction('TENANT_CREATE_ATTEMPT', { 
        tenantKey: newTenant.key, 
        displayName: newTenant.displayName 
      });

      const response = await authService.apiCall('/admin/tenants', {
        method: 'POST',
        body: JSON.stringify({
          key: newTenant.key.trim(),
          displayName: newTenant.displayName.trim(),
          autoCreate: true
        })
      });

      if (!response || !response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP ${response.status}`);
      }

      const result = await response.json();

      // Close dialog and refresh list
      setCreateDialogOpen(false);
      setNewTenant({ key: '', displayName: '' });
      await loadTenants();

      logger.userAction('TENANT_CREATED', {
        tenantKey: result.tenant?.key || newTenant.key,
        tenantName: result.tenant?.displayName || newTenant.displayName
      });

    } catch (err) {
      console.error('Failed to create tenant:', err);
      setCreateError(`Nepodařilo se vytvořit tenant: ${err.message}`);
      logger.error('TENANT_CREATE_ERROR', err.message, { 
        tenantKey: newTenant.key,
        error: err.stack 
      });
    } finally {
      setCreateLoading(false);
    }
  };

  /**
   * 📊 LOAD TENANT STATS: Načte statistiky tenantu
   */
  const handleViewStats = async (tenant) => {
    try {
      setSelectedTenant(tenant);
      setStatsDialogOpen(true);
      setStatsLoading(true);
      setTenantStats(null);

      logger.userAction('TENANT_STATS_REQUEST', { tenantKey: tenant.key });

      const response = await authService.apiCall(`/admin/tenants/${tenant.key}/stats`);

      if (!response || !response.ok) {
        throw new Error(`HTTP ${response?.status || 'Unknown'}`);
      }

      const stats = await response.json();
      setTenantStats(stats);

      logger.userAction('TENANT_STATS_VIEWED', { tenantKey: tenant.key });

    } catch (err) {
      console.error('Failed to load tenant stats:', err);
      setTenantStats({ error: `Nepodařilo se načíst statistiky: ${err.message}` });
      logger.error('TENANT_STATS_ERROR', err.message, { tenantKey: tenant.key });
    } finally {
      setStatsLoading(false);
    }
  };

  /**
   * 🗑️ DELETE TENANT: Smaže tenant
   */
  const handleDeleteTenant = async (tenant) => {
    if (!window.confirm(`Opravdu chcete smazat tenant "${tenant.displayName || tenant.name}"?\n\nTato akce je NEVRATNÁ a smaže:\n- Keycloak realm\n- Všechny uživatele\n- Všechna data`)) {
      return;
    }

    try {
      logger.userAction('TENANT_DELETE_ATTEMPT', { 
        tenantKey: tenant.key,
        tenantName: tenant.displayName || tenant.name 
      });

      const response = await authService.apiCall(`/admin/tenants/${tenant.key}`, {
        method: 'DELETE'
      });

      if (!response || !response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || `HTTP ${response.status}`);
      }

      await loadTenants();

      logger.userAction('TENANT_DELETED', {
        tenantKey: tenant.key,
        tenantName: tenant.displayName || tenant.name
      });

    } catch (err) {
      console.error('Failed to delete tenant:', err);
      setError(`Nepodařilo se smazat tenant: ${err.message}`);
      logger.error('TENANT_DELETE_ERROR', err.message, { 
        tenantKey: tenant.key,
        error: err.stack 
      });
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleDateString('cs-CZ', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <PageContainer title="Správa Tenantů" description="Centrální správa tenantů a Keycloak realms">
      <Box sx={{ p: 3 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
          <Box>
            <Typography variant="h4" gutterBottom>
              <BusinessIcon sx={{ mr: 1, verticalAlign: 'middle' }} />
              Správa Tenantů
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Centrální správa tenantů a Keycloak realms
            </Typography>
          </Box>

          <Box>
            <Button
              variant="outlined"
              startIcon={<RefreshIcon />}
              onClick={loadTenants}
              sx={{ mr: 2 }}
              disabled={loading}
            >
              Obnovit
            </Button>

            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => setCreateDialogOpen(true)}
            >
              Nový Tenant
            </Button>
          </Box>
        </Box>

        {/* Error Alert */}
        {error && (
          <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
            {error}
          </Alert>
        )}

        {/* Tenants Table */}
        <Card>
          <CardContent>
            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                <CircularProgress />
              </Box>
            ) : (
              <TableContainer component={Paper}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell><strong>Tenant Key</strong></TableCell>
                      <TableCell><strong>Název</strong></TableCell>
                      <TableCell><strong>Subdoména</strong></TableCell>
                      <TableCell><strong>Akce</strong></TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {tenants.map((tenant) => (
                      <TableRow key={tenant.id}>
                        <TableCell>
                          <Typography variant="body2" fontFamily="monospace">
                            {tenant.key}
                          </Typography>
                        </TableCell>

                        <TableCell>
                          <Typography variant="body1">
                            {tenant.displayName || tenant.name}
                          </Typography>
                        </TableCell>

                        <TableCell>
                          <Typography variant="body2" color="text.secondary">
                            {tenant.subdomain || `${tenant.key}.core-platform.local`}
                          </Typography>
                        </TableCell>

                        <TableCell>
                          <Box sx={{ display: 'flex', gap: 1 }}>
                            <Tooltip title="Zobrazit statistiky">
                              <IconButton
                                size="small"
                                onClick={() => handleViewStats(tenant)}
                              >
                                <ViewIcon />
                              </IconButton>
                            </Tooltip>

                            {tenant.key !== 'core-platform' && (
                              <Tooltip title="Smazat tenant">
                                <IconButton
                                  size="small"
                                  color="error"
                                  onClick={() => handleDeleteTenant(tenant)}
                                >
                                  <DeleteIcon />
                                </IconButton>
                              </Tooltip>
                            )}
                          </Box>
                        </TableCell>
                      </TableRow>
                    ))}

                    {tenants.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4} align="center">
                          <Typography color="text.secondary">
                            Žádné tenanty nenalezeny
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </CardContent>
        </Card>

        {/* Create Tenant Dialog */}
        <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="sm" fullWidth>
          <DialogTitle>Vytvořit Nový Tenant</DialogTitle>
          <DialogContent>
            <Box sx={{ pt: 1 }}>
              {createError && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {createError}
                </Alert>
              )}

              <TextField
                fullWidth
                label="Tenant Key"
                value={newTenant.key}
                onChange={(e) => setNewTenant({ ...newTenant, key: e.target.value.toLowerCase() })}
                margin="normal"
                placeholder="acme-corp"
                helperText="Pouze malá písmena, číslice a pomlčky. Bude použito jako subdoména."
                disabled={createLoading}
              />

              <TextField
                fullWidth
                label="Display Name"
                value={newTenant.displayName}
                onChange={(e) => setNewTenant({ ...newTenant, displayName: e.target.value })}
                margin="normal"
                placeholder="ACME Corporation"
                helperText="Zobrazovaný název tenantu"
                disabled={createLoading}
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => setCreateDialogOpen(false)}
              disabled={createLoading}
            >
              Zrušit
            </Button>
            <Button
              onClick={handleCreateTenant}
              variant="contained"
              disabled={createLoading || !newTenant.key.trim() || !newTenant.displayName.trim()}
              startIcon={createLoading ? <CircularProgress size={20} /> : null}
            >
              {createLoading ? 'Vytváření...' : 'Vytvořit'}
            </Button>
          </DialogActions>
        </Dialog>

        {/* Tenant Stats Dialog */}
        <Dialog open={statsDialogOpen} onClose={() => setStatsDialogOpen(false)} maxWidth="md" fullWidth>
          <DialogTitle>
            Statistiky Tenantu: {selectedTenant?.displayName || selectedTenant?.name}
          </DialogTitle>
          <DialogContent>
            {statsLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                <CircularProgress />
              </Box>
            ) : tenantStats?.error ? (
              <Alert severity="error">
                {tenantStats.error}
              </Alert>
            ) : tenantStats ? (
              <Grid container spacing={2}>
                <Grid item xs={12} md={6}>
                  <Card variant="outlined">
                    <CardContent>
                      <Typography variant="h6" gutterBottom>
                        👥 Uživatelé
                      </Typography>
                      <Typography variant="h4" color="primary">
                        {tenantStats.userCount}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        Celkem uživatelů v databázi
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>

                <Grid item xs={12} md={6}>
                  <Card variant="outlined">
                    <CardContent>
                      <Typography variant="h6" gutterBottom>
                        🔐 Keycloak Status
                      </Typography>
                      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
                        <Chip
                          icon={tenantStats.realmExists ? <CheckCircleIcon /> : <ErrorIcon />}
                          label={tenantStats.realmExists ? 'Realm existuje' : 'Realm neexistuje'}
                          color={tenantStats.realmExists ? 'success' : 'error'}
                          size="small"
                        />

                        {tenantStats.realmExists && (
                          <Chip
                            label={tenantStats.realmEnabled ? 'Aktivní' : 'Neaktivní'}
                            color={tenantStats.realmEnabled ? 'success' : 'warning'}
                            size="small"
                          />
                        )}
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>

                <Grid item xs={12}>
                  <Card variant="outlined">
                    <CardContent>
                      <Typography variant="h6" gutterBottom>
                        ℹ️ Detaily
                      </Typography>
                      <Typography variant="body2" sx={{ mb: 1 }}>
                        <strong>Tenant Key:</strong> {tenantStats.tenantKey}
                      </Typography>
                      <Typography variant="body2" sx={{ mb: 1 }}>
                        <strong>Display Name:</strong> {tenantStats.displayName}
                      </Typography>
                      <Typography variant="body2" sx={{ mb: 1 }}>
                        <strong>Subdoména:</strong> {tenantStats.tenantKey}.core-platform.local
                      </Typography>
                      <Typography variant="body2" sx={{ mb: 1 }}>
                        <strong>Vytvořeno:</strong> {formatDate(tenantStats.createdAt)}
                      </Typography>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
            ) : null}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setStatsDialogOpen(false)}>
              Zavřít
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    </PageContainer>
  );
};

export default TenantManagementPage;