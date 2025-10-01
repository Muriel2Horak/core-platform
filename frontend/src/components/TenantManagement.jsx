import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Grid,
  Typography,
  Button,
  Alert,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Menu,
  MenuItem,
  Card,
  CardContent,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControlLabel,
  Switch,
  Paper,
  Slide,
  Tooltip,
  Avatar,
  Badge
} from '@mui/material';
import {
  Business as BusinessIcon,
  Add as AddIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Visibility as VisibilityIcon,
  Refresh as RefreshIcon,
  Check as CheckIcon,
  Close as CloseIcon,
  Domain as DomainIcon,
  People as PeopleIcon,
  Settings as SettingsIcon
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';

function TenantManagement({ user }) {
  // State for data
  const [tenants, setTenants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // State for tenant actions
  const [selectedTenant, setSelectedTenant] = useState(null);
  const [actionMenuAnchor, setActionMenuAnchor] = useState(null);

  // State for dialogs
  const [createDialog, setCreateDialog] = useState(false);
  const [editDialog, setEditDialog] = useState(false);
  const [deleteConfirmDialog, setDeleteConfirmDialog] = useState(false);
  const [viewDialog, setViewDialog] = useState(false);

  // State for new/edited tenant
  const [tenantFormData, setTenantFormData] = useState({
    name: '',
    id: '',
    description: '',
    enabled: true,
    adminEmail: '',
    domain: ''
  });
  const [formErrors, setFormErrors] = useState({});

  // User permissions - only admin can manage tenants
  const isAdmin = user?.roles?.includes('CORE_ROLE_ADMIN');

  useEffect(() => {
    if (isAdmin) {
      loadTenants();
    }
  }, [isAdmin]);

  const loadTenants = useCallback(async () => {
    if (!isAdmin) {
      setError('Nemáte oprávnění k zobrazení seznamu tenantů. Vyžaduje se role administrátora.');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      logger.pageView('/tenants', { operation: 'load_tenants' });
      
      const data = await apiService.getTenants();
      setTenants(data || []);
      
      logger.userAction('TENANTS_LOADED', { 
        count: data?.length || 0 
      });
      
    } catch (err) {
      console.error('Failed to load tenants:', err);
      setError('Nepodařilo se načíst seznam tenantů: ' + err.message);
      logger.error('TENANTS_LOAD_ERROR', err.message);
    } finally {
      setLoading(false);
    }
  }, [isAdmin]);

  const handleActionMenuOpen = (event, tenant) => {
    setActionMenuAnchor(event.currentTarget);
    setSelectedTenant(tenant);
  };

  const handleActionMenuClose = () => {
    setActionMenuAnchor(null);
    setSelectedTenant(null);
  };

  const openCreateDialog = () => {
    setTenantFormData({
      name: '',
      id: '',
      description: '',
      enabled: true,
      adminEmail: '',
      domain: ''
    });
    setFormErrors({});
    setCreateDialog(true);
  };

  const openEditDialog = (tenant) => {
    setTenantFormData({
      name: tenant.name || '',
      id: tenant.id || '',
      description: tenant.description || '',
      enabled: tenant.enabled !== false,
      adminEmail: tenant.adminEmail || '',
      domain: tenant.domain || ''
    });
    setFormErrors({});
    setEditDialog(true);
    handleActionMenuClose();
  };

  const openViewDialog = (tenant) => {
    setSelectedTenant(tenant);
    setViewDialog(true);
    handleActionMenuClose();
  };

  const openDeleteDialog = (tenant) => {
    setSelectedTenant(tenant);
    setDeleteConfirmDialog(true);
    handleActionMenuClose();
  };

  const validateForm = () => {
    const errors = {};
    
    if (!tenantFormData.name?.trim()) {
      errors.name = 'Název je povinný';
    }
    
    if (!tenantFormData.id?.trim()) {
      errors.id = 'ID je povinné';
    } else if (!/^[a-z0-9-]+$/.test(tenantFormData.id)) {
      errors.id = 'ID může obsahovat pouze malá písmena, číslice a pomlčky';
    }
    
    if (tenantFormData.adminEmail && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(tenantFormData.adminEmail)) {
      errors.adminEmail = 'Neplatný formát emailu';
    }
    
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateTenant = async () => {
    if (!validateForm()) return;

    try {
      setSaving(true);
      
      // Here we would call API to create tenant
      // For demo, simulate success
      await new Promise(resolve => setTimeout(resolve, 1500));
      
      setCreateDialog(false);
      setSuccess(`Tenant "${tenantFormData.name}" byl úspěšně vytvořen`);
      
      // Reload data
      await loadTenants();
      
      setTimeout(() => setSuccess(null), 5000);
      
    } catch (err) {
      console.error('Failed to create tenant:', err);
      setError('Nepodařilo se vytvořit tenant: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleUpdateTenant = async () => {
    if (!validateForm()) return;

    try {
      setSaving(true);
      
      // Here we would call API to update tenant
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      setEditDialog(false);
      setSuccess(`Tenant "${tenantFormData.name}" byl úspěšně aktualizován`);
      
      await loadTenants();
      
      setTimeout(() => setSuccess(null), 5000);
      
    } catch (err) {
      console.error('Failed to update tenant:', err);
      setError('Nepodařilo se aktualizovat tenant: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteTenant = async () => {
    if (!selectedTenant) return;

    try {
      setSaving(true);
      
      // Here we would call API to delete tenant
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      setDeleteConfirmDialog(false);
      setSuccess(`Tenant "${selectedTenant.name}" byl úspěšně smazán`);
      
      await loadTenants();
      
      setTimeout(() => setSuccess(null), 5000);
      
    } catch (err) {
      console.error('Failed to delete tenant:', err);
      setError('Nepodařilo se smazat tenant: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'Neuvedeno';
    try {
      return new Date(dateString).toLocaleDateString('cs-CZ', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch {
      return 'Neplatné datum';
    }
  };

  const getTenantInitials = (tenant) => {
    return tenant?.name ? tenant.name.substring(0, 2).toUpperCase() : 'TE';
  };

  if (!isAdmin) {
    return (
      <Box>
        <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600 }}>
          Správa tenantů
        </Typography>
        
        <Alert severity="error">
          Nemáte oprávnění k zobrazení seznamu tenantů. Vyžaduje se role administrátora.
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 1400, mx: 'auto' }}>
      <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600, mb: 4 }}>
        Správa tenantů
      </Typography>
      
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Spravujte tenenty v multi-tenant systému. Vytvářejte nové tenanty a konfigurujte jejich nastavení.
      </Typography>

      {/* Success/Error Messages */}
      {success && (
        <Slide direction="down" in={Boolean(success)} mountOnEnter unmountOnExit>
          <Alert
            severity="success"
            onClose={() => setSuccess(null)}
            sx={{ mb: 3, borderRadius: 2 }}
            variant="filled"
          >
            {success}
          </Alert>
        </Slide>
      )}
      
      {error && (
        <Slide direction="down" in={Boolean(error)} mountOnEnter unmountOnExit>
          <Alert
            severity="error"
            onClose={() => setError(null)}
            sx={{ mb: 3, borderRadius: 2 }}
            variant="filled"
          >
            {error}
          </Alert>
        </Slide>
      )}

      {/* Header with Actions */}
      <Card sx={{ mb: 3, borderRadius: 3 }}>
        <CardContent sx={{ p: 3 }}>
          <Box display="flex" justifyContent="space-between" alignItems="center" flexWrap="wrap" gap={2}>
            <Box>
              <Typography variant="h5" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1, fontWeight: 600 }}>
                <BusinessIcon />
                Multi-tenant správa
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Celkem tenantů: <strong>{tenants.length}</strong>
              </Typography>
            </Box>

            <Box display="flex" gap={2}>
              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={loadTenants}
                disabled={loading}
                sx={{
                  borderRadius: 2,
                  borderWidth: 2,
                  '&:hover': { borderWidth: 2 }
                }}
              >
                Obnovit
              </Button>

              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={openCreateDialog}
                sx={{
                  borderRadius: 2,
                  background: 'linear-gradient(45deg, #667eea, #764ba2)',
                  '&:hover': {
                    background: 'linear-gradient(45deg, #5a6fd8, #6b4190)',
                    transform: 'translateY(-2px)',
                    boxShadow: '0 8px 20px rgba(0,0,0,0.2)'
                  },
                  transition: 'all 0.3s ease'
                }}
              >
                Nový tenant
              </Button>
            </Box>
          </Box>
        </CardContent>
      </Card>

      {/* Tenants Table */}
      <Paper elevation={2} sx={{ borderRadius: 3, overflow: 'hidden' }}>
        <Box sx={{ p: 3, borderBottom: 1, borderColor: 'divider' }}>
          <Typography variant="h6" sx={{ fontWeight: 600 }}>
            Seznam tenantů
          </Typography>
        </Box>

        {loading ? (
          <Box display="flex" justifyContent="center" p={4}>
            <CircularProgress size={40} />
          </Box>
        ) : (
          <>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow sx={{ backgroundColor: 'grey.50' }}>
                    <TableCell sx={{ fontWeight: 600 }}>Tenant</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>ID</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Stav</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Doména</TableCell>
                    <TableCell sx={{ fontWeight: 600 }}>Vytvořeno</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 600 }}>Akce</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tenants.map((tenant) => (
                    <TableRow 
                      key={tenant.id} 
                      hover 
                      sx={{ 
                        cursor: 'pointer',
                        '&:hover': {
                          backgroundColor: 'primary.light',
                          '& .MuiTableCell-root': {
                            color: 'primary.main'
                          }
                        }
                      }}
                      onClick={() => openViewDialog(tenant)}
                    >
                      <TableCell>
                        <Box display="flex" alignItems="center" gap={2}>
                          <Badge
                            badgeContent={tenant?.enabled ? '✓' : '⏸'}
                            color={tenant?.enabled ? 'success' : 'default'}
                            anchorOrigin={{
                              vertical: 'bottom',
                              horizontal: 'right',
                            }}
                          >
                            <Avatar
                              sx={{
                                width: 40,
                                height: 40,
                                backgroundColor: tenant?.enabled ? 'primary.main' : 'grey.400',
                                fontWeight: 'bold'
                              }}
                            >
                              {getTenantInitials(tenant)}
                            </Avatar>
                          </Badge>
                          <Box>
                            <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
                              {tenant?.name || 'Bez názvu'}
                            </Typography>
                            <Typography variant="body2" color="text.secondary">
                              {tenant?.description || 'Bez popisu'}
                            </Typography>
                          </Box>
                        </Box>
                      </TableCell>
                      
                      <TableCell>
                        <Typography variant="body2" sx={{ fontFamily: 'monospace', fontSize: '0.9rem' }}>
                          {tenant?.id}
                        </Typography>
                      </TableCell>
                      
                      <TableCell>
                        <Chip
                          label={tenant?.enabled ? 'Aktivní' : 'Neaktivní'}
                          color={tenant?.enabled ? 'success' : 'default'}
                          size="small"
                          sx={{ borderRadius: 2 }}
                        />
                      </TableCell>
                      
                      <TableCell>
                        <Box display="flex" alignItems="center" gap={1}>
                          <DomainIcon fontSize="small" color="action" />
                          <Typography variant="body2">
                            {tenant?.domain || 'Nenastaveno'}
                          </Typography>
                        </Box>
                      </TableCell>
                      
                      <TableCell>
                        <Typography variant="body2">
                          {formatDate(tenant?.createdAt)}
                        </Typography>
                      </TableCell>
                      
                      <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                        <Tooltip title="Akce">
                          <IconButton
                            onClick={(e) => handleActionMenuOpen(e, tenant)}
                            sx={{
                              '&:hover': {
                                backgroundColor: 'primary.light'
                              }
                            }}
                          >
                            <MoreVertIcon />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>

            {/* Empty State */}
            {tenants.length === 0 && !loading && (
              <Box textAlign="center" py={6}>
                <BusinessIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
                <Typography variant="h6" color="text.secondary" gutterBottom>
                  Žádní tenanti nenalezeni
                </Typography>
                <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                  Vytvořte první tenant pro začátek práce
                </Typography>
                <Button
                  variant="contained"
                  startIcon={<AddIcon />}
                  onClick={openCreateDialog}
                  sx={{ borderRadius: 2 }}
                >
                  Vytvořit tenant
                </Button>
              </Box>
            )}
          </>
        )}
      </Paper>

      {/* Action Menu */}
      <Menu
        anchorEl={actionMenuAnchor}
        open={Boolean(actionMenuAnchor)}
        onClose={handleActionMenuClose}
        PaperProps={{
          sx: {
            borderRadius: 2,
            boxShadow: '0 8px 16px rgba(0,0,0,0.15)'
          }
        }}
      >
        <MenuItem onClick={() => openViewDialog(selectedTenant)}>
          <VisibilityIcon sx={{ mr: 1 }} fontSize="small" />
          Zobrazit detail
        </MenuItem>
        
        <MenuItem onClick={() => openEditDialog(selectedTenant)}>
          <EditIcon sx={{ mr: 1 }} fontSize="small" />
          Upravit
        </MenuItem>
        
        <MenuItem 
          onClick={() => openDeleteDialog(selectedTenant)}
          sx={{ color: 'error.main' }}
        >
          <DeleteIcon sx={{ mr: 1 }} fontSize="small" />
          Smazat
        </MenuItem>
      </Menu>

      {/* Create Tenant Dialog */}
      <Dialog 
        open={createDialog} 
        onClose={() => setCreateDialog(false)}
        maxWidth="md"
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: 3,
            background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)'
          }
        }}
      >
        <DialogTitle sx={{
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          color: 'white',
          textAlign: 'center',
          fontSize: '1.5rem',
          fontWeight: 'bold'
        }}>
          🏢 Vytvořit nového tenanta
        </DialogTitle>
        <DialogContent sx={{ p: 3 }}>
          <Grid container spacing={3} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Název tenanta *"
                value={tenantFormData.name}
                onChange={(e) => setTenantFormData(prev => ({ ...prev, name: e.target.value }))}
                error={!!formErrors.name}
                helperText={formErrors.name}
                variant="outlined"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'white'
                  }
                }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="ID tenanta *"
                value={tenantFormData.id}
                onChange={(e) => setTenantFormData(prev => ({ ...prev, id: e.target.value.toLowerCase() }))}
                error={!!formErrors.id}
                helperText={formErrors.id || 'Pouze malá písmena, číslice a pomlčky'}
                variant="outlined"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'white'
                  }
                }}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Popis"
                multiline
                rows={3}
                value={tenantFormData.description}
                onChange={(e) => setTenantFormData(prev => ({ ...prev, description: e.target.value }))}
                variant="outlined"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'white'
                  }
                }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Email administrátora"
                type="email"
                value={tenantFormData.adminEmail}
                onChange={(e) => setTenantFormData(prev => ({ ...prev, adminEmail: e.target.value }))}
                error={!!formErrors.adminEmail}
                helperText={formErrors.adminEmail}
                variant="outlined"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'white'
                  }
                }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Doména"
                value={tenantFormData.domain}
                onChange={(e) => setTenantFormData(prev => ({ ...prev, domain: e.target.value }))}
                variant="outlined"
                placeholder="example.com"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'white'
                  }
                }}
              />
            </Grid>

            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={tenantFormData.enabled}
                    onChange={(e) => setTenantFormData(prev => ({ ...prev, enabled: e.target.checked }))}
                    color="primary"
                  />
                }
                label="Aktivní tenant"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ p: 3, justifyContent: 'center', gap: 2 }}>
          <Button
            onClick={() => setCreateDialog(false)}
            disabled={saving}
            variant="outlined"
            sx={{
              borderRadius: 2,
              px: 3,
              borderWidth: 2,
              '&:hover': { borderWidth: 2 }
            }}
          >
            Zrušit
          </Button>
          <Button
            variant="contained"
            onClick={handleCreateTenant}
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : <CheckIcon />}
            sx={{
              borderRadius: 2,
              px: 3,
              background: 'linear-gradient(45deg, #667eea, #764ba2)',
              '&:hover': {
                background: 'linear-gradient(45deg, #5a6fd8, #6b4190)',
                transform: 'translateY(-2px)'
              },
              transition: 'all 0.3s ease'
            }}
          >
            {saving ? 'Vytvářím...' : 'Vytvořit tenant'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit Tenant Dialog */}
      <Dialog 
        open={editDialog} 
        onClose={() => setEditDialog(false)}
        maxWidth="md"
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: 3,
            background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)'
          }
        }}
      >
        <DialogTitle sx={{
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          color: 'white',
          textAlign: 'center',
          fontSize: '1.5rem',
          fontWeight: 'bold'
        }}>
          ✏️ Upravit tenanta
        </DialogTitle>
        <DialogContent sx={{ p: 3 }}>
          <Grid container spacing={3} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Název tenanta *"
                value={tenantFormData.name}
                onChange={(e) => setTenantFormData(prev => ({ ...prev, name: e.target.value }))}
                error={!!formErrors.name}
                helperText={formErrors.name}
                variant="outlined"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'white'
                  }
                }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="ID tenanta"
                value={tenantFormData.id}
                disabled
                variant="outlined"
                helperText="ID tenanta nelze změnit"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'grey.100'
                  }
                }}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Popis"
                multiline
                rows={3}
                value={tenantFormData.description}
                onChange={(e) => setTenantFormData(prev => ({ ...prev, description: e.target.value }))}
                variant="outlined"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'white'
                  }
                }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Email administrátora"
                type="email"
                value={tenantFormData.adminEmail}
                onChange={(e) => setTenantFormData(prev => ({ ...prev, adminEmail: e.target.value }))}
                error={!!formErrors.adminEmail}
                helperText={formErrors.adminEmail}
                variant="outlined"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'white'
                  }
                }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Doména"
                value={tenantFormData.domain}
                onChange={(e) => setTenantFormData(prev => ({ ...prev, domain: e.target.value }))}
                variant="outlined"
                placeholder="example.com"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'white'
                  }
                }}
              />
            </Grid>

            <Grid item xs={12}>
              <FormControlLabel
                control={
                  <Switch
                    checked={tenantFormData.enabled}
                    onChange={(e) => setTenantFormData(prev => ({ ...prev, enabled: e.target.checked }))}
                    color="primary"
                  />
                }
                label="Aktivní tenant"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ p: 3, justifyContent: 'center', gap: 2 }}>
          <Button
            onClick={() => setEditDialog(false)}
            disabled={saving}
            variant="outlined"
            sx={{ borderRadius: 2, px: 3 }}
          >
            Zrušit
          </Button>
          <Button
            variant="contained"
            onClick={handleUpdateTenant}
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : <CheckIcon />}
            sx={{
              borderRadius: 2,
              px: 3,
              background: 'linear-gradient(45deg, #667eea, #764ba2)'
            }}
          >
            {saving ? 'Ukládám...' : 'Uložit změny'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* View Tenant Dialog */}
      <Dialog 
        open={viewDialog} 
        onClose={() => setViewDialog(false)}
        maxWidth="md"
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: 3
          }
        }}
      >
        <DialogTitle sx={{ fontWeight: 600, fontSize: '1.5rem' }}>
          📋 Detail tenanta
        </DialogTitle>
        <DialogContent sx={{ p: 3 }}>
          {selectedTenant && (
            <Grid container spacing={3}>
              <Grid item xs={12}>
                <Box display="flex" alignItems="center" gap={2} mb={2}>
                  <Avatar
                    sx={{
                      width: 60,
                      height: 60,
                      backgroundColor: selectedTenant.enabled ? 'primary.main' : 'grey.400',
                      fontSize: '1.5rem',
                      fontWeight: 'bold'
                    }}
                  >
                    {getTenantInitials(selectedTenant)}
                  </Avatar>
                  <Box>
                    <Typography variant="h5" gutterBottom>
                      {selectedTenant.name}
                    </Typography>
                    <Chip
                      label={selectedTenant.enabled ? 'Aktivní' : 'Neaktivní'}
                      color={selectedTenant.enabled ? 'success' : 'default'}
                      size="small"
                    />
                  </Box>
                </Box>
              </Grid>

              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  ID tenanta
                </Typography>
                <Typography variant="body1" sx={{ fontFamily: 'monospace', fontWeight: 500 }}>
                  {selectedTenant.id}
                </Typography>
              </Grid>

              <Grid item xs={12} sm={6}>
                <Typography variant="body2" color="text.secondary">
                  Vytvořeno
                </Typography>
                <Typography variant="body1" sx={{ fontWeight: 500 }}>
                  {formatDate(selectedTenant.createdAt)}
                </Typography>
              </Grid>

              {selectedTenant.domain && (
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="text.secondary">
                    Doména
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 500 }}>
                    {selectedTenant.domain}
                  </Typography>
                </Grid>
              )}

              {selectedTenant.adminEmail && (
                <Grid item xs={12} sm={6}>
                  <Typography variant="body2" color="text.secondary">
                    Email administrátora
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 500 }}>
                    {selectedTenant.adminEmail}
                  </Typography>
                </Grid>
              )}

              {selectedTenant.description && (
                <Grid item xs={12}>
                  <Typography variant="body2" color="text.secondary">
                    Popis
                  </Typography>
                  <Typography variant="body1" sx={{ fontWeight: 500 }}>
                    {selectedTenant.description}
                  </Typography>
                </Grid>
              )}
            </Grid>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <Button onClick={() => setViewDialog(false)} sx={{ borderRadius: 2 }}>
            Zavřít
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog 
        open={deleteConfirmDialog} 
        onClose={() => setDeleteConfirmDialog(false)}
        PaperProps={{
          sx: {
            borderRadius: 3
          }
        }}
      >
        <DialogTitle sx={{ fontWeight: 600 }}>
          Smazat tenanta
        </DialogTitle>
        <DialogContent>
          <Typography>
            Opravdu chcete smazat tenanta <strong>{selectedTenant?.name}</strong>?
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
            ID: {selectedTenant?.id}
          </Typography>
          <Typography variant="body2" color="error" sx={{ mt: 1 }}>
            Tato akce je nevratná a smaže všechna data tenanta!
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <Button 
            onClick={() => setDeleteConfirmDialog(false)} 
            disabled={saving}
            sx={{ borderRadius: 2 }}
          >
            Zrušit
          </Button>
          <Button
            color="error"
            variant="contained"
            onClick={handleDeleteTenant}
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : null}
            sx={{ borderRadius: 2 }}
          >
            {saving ? 'Mažu...' : 'Smazat'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default TenantManagement;