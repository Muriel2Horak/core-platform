import { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
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
  Avatar,
} from '@mui/material';
import {
  Business as BusinessIcon,
  Add as AddIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  People as PeopleIcon,
  Security as SecurityIcon,
  Group as GroupIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Schedule as ScheduleIcon,
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';
import { UserPropType } from '../shared/propTypes.js';
import { DataTable } from './common/DataTable.jsx';
import {
  CreateTenantDialog,
  EditTenantDialog,
  DeleteTenantDialog,
} from './Tenants/index.js';

function Tenants({ user }) {
  const [tenants, setTenants] = useState([]);
  const [filteredTenants, setFilteredTenants] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Dialog states
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
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
      
      // Pro každý tenant načteme statistiky (role, skupiny, uživatelé)
      const tenantsWithStats = await Promise.all(
        tenantsData.map(async (tenant) => {
          try {
            // Načteme role pro tenant
            const roles = await apiService.getRolesByTenant(tenant.key);
            const roleCount = Array.isArray(roles) ? roles.length : 0;
            
            // Načteme skupiny pro tenant (pokud máme API)
            let groupCount = 0;
            try {
              const groups = await apiService.getGroups(); // TODO: getGroupsByTenant
              groupCount = Array.isArray(groups) ? groups.length : 0;
            } catch (err) {
              logger.warn('Failed to load groups for tenant', { tenantKey: tenant.key });
            }
            
            // Počet uživatelů - načteme z user directory
            let userCount = 0;
            try {
              const users = await apiService.getUsersDirectory({ tenantKey: tenant.key, size: 1 });
              userCount = users?.totalElements || 0;
            } catch (err) {
              logger.warn('Failed to load users count for tenant', { tenantKey: tenant.key });
            }
            
            return {
              ...tenant,
              roleCount,
              groupCount,
              userCount,
            };
          } catch (err) {
            logger.warn('Failed to load stats for tenant', { tenantKey: tenant.key });
            return { ...tenant, roleCount: 0, groupCount: 0, userCount: 0 };
          }
        })
      );
      
      setTenants(tenantsWithStats);
      setFilteredTenants(tenantsWithStats);
      logger.info('Tenants loaded with stats', { count: tenantsWithStats.length });
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
  
  const getInitials = (name) => {
    if (!name) return 'T';
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };
  
  // DataTable columns definition
  const columns = [
    {
      field: 'tenant',
      label: 'Tenant',
      sortable: true,
      render: (tenant) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Avatar
            sx={{
              width: 40,
              height: 40,
              backgroundColor: 'primary.main',
              fontWeight: 'bold'
            }}
          >
            {getInitials(tenant.displayName || tenant.key)}
          </Avatar>
          <Box>
            <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
              {tenant.displayName || tenant.key}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {tenant.key}
            </Typography>
          </Box>
        </Box>
      ),
    },
    {
      field: 'subdomain',
      label: 'Subdoména',
      sortable: true,
      render: (tenant) => (
        <Chip
          label={tenant.subdomain || 'N/A'}
          size="small"
          variant="outlined"
        />
      ),
    },
    {
      field: 'status',
      label: 'Stav',
      sortable: false,
      render: (tenant) => (
        <Chip
          label={tenant.active ? 'Aktivní' : 'Neaktivní'}
          size="small"
          sx={{
            backgroundColor: tenant.active ? 'success.main' : 'error.main',
            color: 'white',
            fontWeight: 600,
          }}
        />
      ),
    },
    {
      field: 'stats',
      label: 'Statistiky',
      sortable: false,
      render: (tenant) => (
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="Role">
            <Chip
              icon={<SecurityIcon fontSize="small" />}
              label={tenant.roleCount || 0}
              size="small"
              variant="outlined"
            />
          </Tooltip>
          <Tooltip title="Skupiny">
            <Chip
              icon={<GroupIcon fontSize="small" />}
              label={tenant.groupCount || 0}
              size="small"
              variant="outlined"
            />
          </Tooltip>
          <Tooltip title="Uživatelé">
            <Chip
              icon={<PeopleIcon fontSize="small" />}
              label={tenant.userCount || 0}
              size="small"
              variant="outlined"
              color="primary"
            />
          </Tooltip>
        </Box>
      ),
    },
    {
      field: 'created',
      label: 'Vytvořeno',
      sortable: true,
      render: (tenant) => (
        <Box>
          <Typography variant="body2">
            {tenant.createdAt ? new Date(tenant.createdAt).toLocaleDateString('cs-CZ') : 'N/A'}
          </Typography>
          {tenant.createdBy && (
            <Typography variant="caption" color="text.secondary">
              {tenant.createdBy}
            </Typography>
          )}
        </Box>
      ),
    },
    {
      field: 'actions',
      label: 'Akce',
      sortable: false,
      align: 'right',
      render: (tenant) => (
        <IconButton
          size="small"
          onClick={(e) => {
            e.stopPropagation();
            handleMenuOpen(e, tenant);
          }}
        >
          <MoreVertIcon />
        </IconButton>
      ),
    },
  ];
  
  const handleRowClick = (tenant) => {
    handleEditTenant(tenant);
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
            <DataTable
              columns={columns}
              data={filteredTenants}
              onRowClick={handleRowClick}
              loading={loading}
            />
          )}
        </CardContent>
      </Card>

      {/* Context menu */}
      <Menu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={() => { handleEditTenant(menuTenant); handleMenuClose(); }}>
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Upravit</ListItemText>
        </MenuItem>
        <MenuItem 
          onClick={() => { handleDeleteTenant(menuTenant); handleMenuClose(); }} 
          sx={{ color: 'error.main' }}
          disabled={menuTenant?.key === 'admin'}
        >
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
    </Box>
  );
}

Tenants.propTypes = {
  user: UserPropType,
};

export default Tenants;
