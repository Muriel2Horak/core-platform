import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Button,
  LinearProgress,
  Alert,
  Stack,
  Chip,
  IconButton,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  List,
  ListItem,
  ListItemText,
  Paper,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
} from '@mui/material';
import {
  Sync as SyncIcon,
  ExpandMore as ExpandMoreIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Refresh as RefreshIcon,
  People as PeopleIcon,
  Security as SecurityIcon,
  Group as GroupIcon,
  Business as TenantIcon,
} from '@mui/icons-material';
import axios from 'axios';

/**
 * 🔄 Keycloak Sync Admin Page
 * 
 * Umožňuje manuální synchronizaci uživatelů, rolí a skupin z Keycloak
 * s real-time progress tracking.
 */
export const KeycloakSyncPage = ({ user }) => {
  const [syncs, setSyncs] = useState([]);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [selectedTenants, setSelectedTenants] = useState(['test-tenant']); // Multi-select pro CORE_ADMIN
  const [tenants, setTenants] = useState([]);

  // Fetch tenants for selector
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

  // Fetch active syncs and stats
  const fetchSyncsAndStats = async () => {
    try {
      const [syncsRes, statsRes] = await Promise.all([
        axios.get('/api/admin/keycloak-sync/active'),
        axios.get('/api/admin/keycloak-sync/stats'),
      ]);
      setSyncs(syncsRes.data);
      setStats(statsRes.data);
    } catch (err) {
      console.error('Failed to fetch syncs:', err);
    }
  };

  // Poll for updates every 2 seconds when there are running syncs
  useEffect(() => {
    fetchSyncsAndStats();
    
    const interval = setInterval(() => {
      if (syncs.some(s => s.status === 'running')) {
        fetchSyncsAndStats();
      }
    }, 2000);

    return () => clearInterval(interval);
  }, [syncs.length]);

  // Start sync for multiple tenants
  const startSync = async (type) => {
    if (selectedTenants.length === 0) {
      setError('Vyberte alespoň jeden tenant pro synchronizaci');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      // Start sync for all selected tenants
      const promises = selectedTenants.map(tenantKey => {
        const endpoint = `/api/admin/keycloak-sync/${type}/${tenantKey}`;
        return axios.post(endpoint);
      });

      await Promise.all(promises);
      
      // Immediately fetch updated syncs
      await fetchSyncsAndStats();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to start synchronization');
    } finally {
      setLoading(false);
    }
  };

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
          🔄 Keycloak Synchronization
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Manuální synchronizace uživatelů, rolí a skupin z Keycloak do aplikace
        </Typography>
      </Box>

      {/* Tenant Selector for CORE_ADMIN */}
      {user?.roles?.includes('CORE_ROLE_ADMIN') && (
        <Box sx={{ mb: 3, p: 3, background: 'linear-gradient(135deg, rgba(25, 118, 210, 0.08), rgba(66, 165, 245, 0.08))', borderRadius: 2, border: '1px solid rgba(25, 118, 210, 0.2)' }}>
          <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <TenantIcon /> Výběr tenantů pro synchronizaci
          </Typography>
          
          <FormControl fullWidth size="small" sx={{ mt: 2 }}>
            <InputLabel>Vyberte tenanty</InputLabel>
            <Select
              multiple
              value={selectedTenants}
              label="Vyberte tenanty"
              onChange={(e) => setSelectedTenants(e.target.value)}
              renderValue={(selected) => (
                <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                  {selected.map((value) => (
                    <Chip 
                      key={value} 
                      label={value} 
                      size="small" 
                      color="primary"
                      sx={{ 
                        fontWeight: 600,
                        bgcolor: 'primary.main',
                        color: 'white'
                      }}
                    />
                  ))}
                </Box>
              )}
              sx={{
                '& .MuiSelect-select': {
                  py: 1.5
                }
              }}
            >
              {tenants.map((tenant) => (
                <MenuItem key={tenant.key} value={tenant.key}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <TenantIcon fontSize="small" color={selectedTenants.includes(tenant.key) ? 'primary' : 'inherit'} />
                    <Typography sx={{ fontWeight: selectedTenants.includes(tenant.key) ? 600 : 400 }}>
                      {tenant.key}
                    </Typography>
                  </Box>
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
            {selectedTenants.length === 0 && '⚠️ Vyberte alespoň jeden tenant'}
            {selectedTenants.length === 1 && `📌 Vybraný tenant: ${selectedTenants[0]}`}
            {selectedTenants.length > 1 && `📌 Vybrané tenanty: ${selectedTenants.length} (${selectedTenants.join(', ')})`}
          </Typography>
        </Box>
      )}

      {/* Error Alert */}
      {error && (
        <Alert severity="error" onClose={() => setError(null)} sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Stats Cards */}
      {stats && (
        <Box sx={{ mb: 4, display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 2 }}>
          <StatCard label="Total Syncs" value={stats.total} color="#1976d2" />
          <StatCard label="Running" value={stats.running} color="#ff9800" />
          <StatCard label="Completed" value={stats.completed} color="#4caf50" />
          <StatCard label="Failed" value={stats.failed} color="#f44336" />
        </Box>
      )}

      {/* Sync Actions */}
      <Card sx={{ mb: 4, background: 'linear-gradient(135deg, rgba(25, 118, 210, 0.05), rgba(66, 165, 245, 0.05))' }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            Quick Actions
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
            {selectedTenants.length === 0 && '⚠️ Vyberte tenant pro synchronizaci'}
            {selectedTenants.length === 1 && (
              <>Synchronizace pro: <Chip label={selectedTenants[0]} size="small" color="primary" sx={{ fontWeight: 600 }} /></>
            )}
            {selectedTenants.length > 1 && (
              <>Synchronizace pro: <Chip label={`${selectedTenants.length} tenantů`} size="small" color="primary" sx={{ fontWeight: 600 }} /></>
            )}
          </Typography>

          <Stack direction="row" spacing={2} flexWrap="wrap" useFlexGap>
            <Button
              variant="contained"
              startIcon={<PeopleIcon />}
              onClick={() => startSync('users')}
              disabled={loading || selectedTenants.length === 0}
              sx={{
                background: 'linear-gradient(135deg, #1976d2, #1565c0)',
                '&:hover': { background: 'linear-gradient(135deg, #1565c0, #0d47a1)' },
                '&.Mui-disabled': {
                  background: 'rgba(0, 0, 0, 0.26)',
                  color: 'rgba(0, 0, 0, 0.5)',
                  fontWeight: 600
                }
              }}
            >
              Sync Users
            </Button>

            <Button
              variant="contained"
              startIcon={<SecurityIcon />}
              onClick={() => startSync('roles')}
              disabled={loading || selectedTenants.length === 0}
              sx={{
                background: 'linear-gradient(135deg, #7b1fa2, #6a1b9a)',
                '&:hover': { background: 'linear-gradient(135deg, #6a1b9a, #4a148c)' },
                '&.Mui-disabled': {
                  background: 'rgba(0, 0, 0, 0.26)',
                  color: 'rgba(0, 0, 0, 0.5)',
                  fontWeight: 600
                }
              }}
            >
              Sync Roles
            </Button>

            <Button
              variant="contained"
              startIcon={<GroupIcon />}
              onClick={() => startSync('groups')}
              disabled={loading || selectedTenants.length === 0}
              sx={{
                background: 'linear-gradient(135deg, #388e3c, #2e7d32)',
                '&:hover': { background: 'linear-gradient(135deg, #2e7d32, #1b5e20)' },
                '&.Mui-disabled': {
                  background: 'rgba(0, 0, 0, 0.26)',
                  color: 'rgba(0, 0, 0, 0.5)',
                  fontWeight: 600
                }
              }}
            >
              Sync Groups
            </Button>

            <Button
              variant="contained"
              startIcon={<SyncIcon />}
              onClick={() => startSync('all')}
              disabled={loading || selectedTenants.length === 0}
              sx={{
                background: 'linear-gradient(135deg, #f57c00, #ef6c00)',
                '&:hover': { background: 'linear-gradient(135deg, #ef6c00, #e65100)' },
                '&.Mui-disabled': {
                  background: 'rgba(0, 0, 0, 0.26)',
                  color: 'rgba(0, 0, 0, 0.5)',
                  fontWeight: 600
                }
              }}
            >
              Sync All
            </Button>

            <IconButton onClick={fetchSyncsAndStats} color="primary">
              <RefreshIcon />
            </IconButton>
          </Stack>
        </CardContent>
      </Card>

      {/* Active Syncs */}
      <Typography variant="h6" gutterBottom>
        Active Synchronizations
      </Typography>

      {syncs.length === 0 ? (
        <Paper sx={{ p: 4, textAlign: 'center', background: 'rgba(25, 118, 210, 0.02)' }}>
          <Typography color="text.secondary">
            No active synchronizations. Start one using the buttons above.
          </Typography>
        </Paper>
      ) : (
        <Stack spacing={2}>
          {syncs.map((sync) => (
            <SyncCard key={sync.syncId} sync={sync} />
          ))}
        </Stack>
      )}
    </Box>
  );
};

/**
 * 📊 Stat Card Component
 */
const StatCard = ({ label, value, color }) => (
  <Card sx={{
    background: `linear-gradient(135deg, ${color}15, ${color}05)`,
    border: `1px solid ${color}30`,
  }}>
    <CardContent>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography variant="h3" sx={{ color, fontWeight: 700 }}>
        {value}
      </Typography>
    </CardContent>
  </Card>
);

/**
 * 🔄 Sync Card Component with Progress
 */
const SyncCard = ({ sync }) => {
  const getStatusColor = (status) => {
    switch (status) {
      case 'running': return '#ff9800';
      case 'completed': return '#4caf50';
      case 'failed': return '#f44336';
      default: return '#9e9e9e';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'running': return <SyncIcon sx={{ animation: 'spin 2s linear infinite' }} />;
      case 'completed': return <CheckCircleIcon />;
      case 'failed': return <ErrorIcon />;
      default: return <SyncIcon />;
    }
  };

  const getTypeIcon = (type) => {
    switch (type) {
      case 'users': return <PeopleIcon />;
      case 'roles': return <SecurityIcon />;
      case 'groups': return <GroupIcon />;
      case 'all': return <SyncIcon />;
      default: return <SyncIcon />;
    }
  };

  return (
    <Accordion
      defaultExpanded={sync.status === 'running'}
      sx={{
        border: `2px solid ${getStatusColor(sync.status)}40`,
        background: `linear-gradient(135deg, ${getStatusColor(sync.status)}08, ${getStatusColor(sync.status)}03)`,
      }}
    >
      <AccordionSummary expandIcon={<ExpandMoreIcon />}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, width: '100%' }}>
          <Box sx={{ color: getStatusColor(sync.status) }}>
            {getTypeIcon(sync.type)}
          </Box>

          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="h6">
              {sync.type.charAt(0).toUpperCase() + sync.type.slice(1)} Sync
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Tenant: {sync.tenantKey} • Started: {new Date(sync.startTime).toLocaleString()}
            </Typography>
          </Box>

          <Chip
            icon={getStatusIcon(sync.status)}
            label={sync.status.toUpperCase()}
            sx={{
              background: getStatusColor(sync.status),
              color: 'white',
              fontWeight: 600,
            }}
          />
        </Box>
      </AccordionSummary>

      <AccordionDetails>
        {/* Progress Bar */}
        {sync.status === 'running' && (
          <Box sx={{ mb: 3 }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 1 }}>
              <Typography variant="body2">
                Progress: {sync.processed} / {sync.total}
              </Typography>
              <Typography variant="body2" fontWeight={600}>
                {sync.progress}%
              </Typography>
            </Box>
            <LinearProgress
              variant="determinate"
              value={sync.progress}
              sx={{
                height: 8,
                borderRadius: 4,
                background: `${getStatusColor(sync.status)}20`,
                '& .MuiLinearProgress-bar': {
                  background: `linear-gradient(90deg, ${getStatusColor(sync.status)}, ${getStatusColor(sync.status)}cc)`,
                },
              }}
            />
          </Box>
        )}

        {/* Completed Info */}
        {sync.status === 'completed' && (
          <Alert severity="success" sx={{ mb: 2 }}>
            ✅ Synchronization completed successfully!
            <br />
            Processed: {sync.processed} / {sync.total}
            <br />
            Finished: {sync.endTime && new Date(sync.endTime).toLocaleString()}
          </Alert>
        )}

        {/* Failed Info */}
        {sync.status === 'failed' && (
          <Alert severity="error" sx={{ mb: 2 }}>
            ❌ Synchronization failed
            <br />
            Processed: {sync.processed} / {sync.total}
          </Alert>
        )}

        {/* Errors */}
        {sync.errors && sync.errors.length > 0 && (
          <Box>
            <Typography variant="subtitle2" color="error" gutterBottom>
              Errors ({sync.errors.length}):
            </Typography>
            <List dense>
              {sync.errors.map((error, index) => (
                <ListItem key={index}>
                  <ListItemText
                    primary={error}
                    primaryTypographyProps={{ variant: 'body2', color: 'error' }}
                  />
                </ListItem>
              ))}
            </List>
          </Box>
        )}
      </AccordionDetails>
    </Accordion>
  );
};

// Add spinning animation for sync icon
const style = document.createElement('style');
style.textContent = `
  @keyframes spin {
    from { transform: rotate(0deg); }
    to { transform: rotate(360deg); }
  }
`;
document.head.appendChild(style);

export default KeycloakSyncPage;
