import { useState, useEffect } from 'react';
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
  Button,
  IconButton,
  Menu,
  MenuItem,
  ListItemIcon,
  ListItemText,
  Tooltip,
  TextField,
  InputAdornment,
  Tabs,
  Tab,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import {
  Business as BusinessIcon,
  Add as AddIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  People as PeopleIcon,
  BarChart as BarChartIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Info as InfoIcon,
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';
import { UserPropType } from '../shared/propTypes.js';
import {
  CreateTenantDialog,
  EditTenantDialog,
  DeleteTenantDialog,
  TenantStatsDialog,
  TenantUsersDialog,
} from './Tenants/index.js';

function Tenants({ user }) {
  const [tenants, setTenants] = useState([]);
  const [filteredTenants, setFilteredTenants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentTab, setCurrentTab] = useState(0);

  // Dialog states
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [statsDialogOpen, setStatsDialogOpen] = useState(false);
  const [usersDialogOpen, setUsersDialogOpen] = useState(false);
  const [selectedTenant, setSelectedTenant] = useState(null);

  // Menu state
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [menuTenant, setMenuTenant] = useState(null);

  // Check if user has permission
  const hasPermission = user?.roles?.includes('CORE_ROLE_ADMIN');

  // Load tenants
  const loadTenants = async () => {
    if (!hasPermission) {
      setError('Nemáte oprávnění k zobrazení seznamu tenantů.');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      const response = await apiService.getTenants();
      const tenantsData = response.tenants || response || [];
      setTenants(tenantsData);
      setFilteredTenants(tenantsData);
      logger.info('Tenants loaded', { count: tenantsData.length });
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

  // Search/filter tenants
  useEffect(() => {
    if (!searchQuery) {
      setFilteredTenants(tenants);
      return;
    }

    const query = searchQuery.toLowerCase();
    const filtered = tenants.filter(t => 
      t.key?.toLowerCase().includes(query) ||
      t.displayName?.toLowerCase().includes(query) ||
      t.subdomain?.toLowerCase().includes(query)
    );
    setFilteredTenants(filtered);
  }, [searchQuery, tenants]);

  // Action handlers
  const handleMenuOpen = (event, tenant) => {
    setMenuAnchor(event.currentTarget);
    setMenuTenant(tenant);
  };

  const handleMenuClose = () => {
    setMenuAnchor(null);
    setMenuTenant(null);
  };

  const handleCreateTenant = () => {
    setCreateDialogOpen(true);
  };

  const handleEditTenant = (tenant) => {
    setSelectedTenant(tenant);
    setEditDialogOpen(true);
    handleMenuClose();
  };

  const handleDeleteTenant = (tenant) => {
    setSelectedTenant(tenant);
    setDeleteDialogOpen(true);
    handleMenuClose();
  };

  const handleViewStats = (tenant) => {
    setSelectedTenant(tenant);
    setStatsDialogOpen(true);
    handleMenuClose();
  };

  const handleViewUsers = (tenant) => {
    setSelectedTenant(tenant);
    setUsersDialogOpen(true);
    handleMenuClose();
  };

  const handleRefresh = () => {
    loadTenants();
  };

  // Success handlers
  const handleTenantCreated = () => {
    setCreateDialogOpen(false);
    loadTenants();
  };

  const handleTenantUpdated = () => {
    setEditDialogOpen(false);
    setSelectedTenant(null);
    loadTenants();
  };

  const handleTenantDeleted = () => {
    setDeleteDialogOpen(false);
    setSelectedTenant(null);
    loadTenants();
  };

  if (!hasPermission) {
    return (
      <Box>
        <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600 }}>
          Tenanti
        </Typography>
        
        <Alert severity="error">
          Nemáte oprávnění k zobrazení seznamu tenantů.
        </Alert>
      </Box>
    );
  }

  return (
    <Box>
      {/* Header with actions */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Box>
          <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600 }}>
            Správa tenantů
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Multi-tenant systém s izolovanými Keycloak realms
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="Obnovit seznam">
            <IconButton onClick={handleRefresh} disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={handleCreateTenant}
            disabled={loading}
          >
            Vytvořit tenant
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      <Card>
        <CardContent>
          {/* Search bar */}
          <Box sx={{ mb: 3 }}>
            <TextField
              fullWidth
              placeholder="Hledat tenenty..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
              disabled={loading || tenants.length === 0}
            />
          </Box>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress />
            </Box>
          ) : filteredTenants.length === 0 ? (
            <Box sx={{ textAlign: 'center', p: 3 }}>
              <BusinessIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                {searchQuery ? 'Nebyli nalezeni žádní tenanti odpovídající vyhledávání' : 'Nebyli nalezeni žádní tenanti'}
              </Typography>
              {searchQuery && (
                <Button 
                  variant="text" 
                  onClick={() => setSearchQuery('')}
                  sx={{ mt: 2 }}
                >
                  Zrušit filtr
                </Button>
              )}
            </Box>
          ) : (
            <>
              <TableContainer component={Paper} elevation={0}>
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Tenant</TableCell>
                      <TableCell>Key / Realm</TableCell>
                      <TableCell>Subdoména</TableCell>
                      <TableCell align="center">Statistiky</TableCell>
                      <TableCell align="right">Akce</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {filteredTenants.map((tenant, index) => (
                      <TableRow key={tenant.id || index} hover>
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                            <BusinessIcon color="primary" />
                            <Box>
                              <Typography variant="body1" sx={{ fontWeight: 500 }}>
                                {tenant.displayName || tenant.key}
                              </Typography>
                              {tenant.displayName && tenant.key !== tenant.displayName && (
                                <Typography variant="body2" color="text.secondary">
                                  {tenant.key}
                                </Typography>
                              )}
                            </Box>
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Chip 
                            label={tenant.realm || tenant.key} 
                            size="small" 
                            variant="outlined"
                          />
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2" sx={{ fontFamily: 'monospace' }}>
                            {tenant.subdomain || `${tenant.key}.core-platform.local`}
                          </Typography>
                        </TableCell>
                        <TableCell align="center">
                          <Tooltip title="Zobrazit statistiky">
                            <IconButton
                              size="small"
                              onClick={() => handleViewStats(tenant)}
                            >
                              <BarChartIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
                        </TableCell>
                        <TableCell align="right">
                          <IconButton
                            size="small"
                            onClick={(e) => handleMenuOpen(e, tenant)}
                          >
                            <MoreVertIcon />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>

              {/* Results count */}
              <Box sx={{ mt: 2, textAlign: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                  Zobrazeno {filteredTenants.length} z {tenants.length} tenantů
                </Typography>
              </Box>
            </>
          )}
        </CardContent>
      </Card>

      {/* Context menu */}
      <Menu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={() => handleViewStats(menuTenant)}>
          <ListItemIcon>
            <BarChartIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Statistiky</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleViewUsers(menuTenant)}>
          <ListItemIcon>
            <PeopleIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Uživatelé</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleEditTenant(menuTenant)}>
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Upravit</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleDeleteTenant(menuTenant)} sx={{ color: 'error.main' }}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText>Smazat</ListItemText>
        </MenuItem>
      </Menu>

      {/* Dialogs */}
      <CreateTenantDialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        onSuccess={handleTenantCreated}
      />

      <EditTenantDialog
        open={editDialogOpen}
        onClose={() => {
          setEditDialogOpen(false);
          setSelectedTenant(null);
        }}
        tenant={selectedTenant}
        onSuccess={handleTenantUpdated}
      />

      <DeleteTenantDialog
        open={deleteDialogOpen}
        onClose={() => {
          setDeleteDialogOpen(false);
          setSelectedTenant(null);
        }}
        tenant={selectedTenant}
        onSuccess={handleTenantDeleted}
      />

      <TenantStatsDialog
        open={statsDialogOpen}
        onClose={() => {
          setStatsDialogOpen(false);
          setSelectedTenant(null);
        }}
        tenant={selectedTenant}
      />

      <TenantUsersDialog
        open={usersDialogOpen}
        onClose={() => {
          setUsersDialogOpen(false);
          setSelectedTenant(null);
        }}
        tenant={selectedTenant}
      />
    </Box>
  );
}

Tenants.propTypes = {
  user: UserPropType,
};

export default Tenants;
