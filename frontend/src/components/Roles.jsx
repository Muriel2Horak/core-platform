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
} from '@mui/material';
import {
  Security as SecurityIcon,
  Add as AddIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  AccountTree as AccountTreeIcon,
  People as PeopleIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
  Business as BusinessIcon,
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';
import { UserPropType } from '../shared/propTypes.js';
import { DataTable } from './common/DataTable.jsx';
import {
  CreateRoleDialog,
  EditRoleDialog,
  DeleteRoleDialog,
  CompositeRoleBuilder,
  RoleUsersView,
  RoleUsersDialog,
} from './Roles/index.js';

function Roles({ user }) {
  const [roles, setRoles] = useState([]);
  const [filteredRoles, setFilteredRoles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  
  // Tenant selector for CORE_ADMIN
  const [selectedTenant, setSelectedTenant] = useState('');
  const [tenants, setTenants] = useState([]);
  const [tenantsLoading, setTenantsLoading] = useState(false);

  // Dialog states
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [compositeBuilderOpen, setCompositeBuilderOpen] = useState(false);
  const [usersViewOpen, setUsersViewOpen] = useState(false);
  const [roleUsersDialogOpen, setRoleUsersDialogOpen] = useState(false);
  const [selectedRole, setSelectedRole] = useState(null);

  // Menu state
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [menuRole, setMenuRole] = useState(null);

  // Check if user has permission
  const hasPermission = user?.roles?.includes('CORE_ROLE_USER_MANAGER') || user?.roles?.includes('CORE_ROLE_ADMIN');
  const canManageRoles = user?.roles?.includes('CORE_ROLE_ADMIN');
  const isCoreAdmin = user?.roles?.includes('CORE_ROLE_ADMIN');

  // Load tenants for CORE_ADMIN
  useEffect(() => {
    const fetchTenants = async () => {
      if (!isCoreAdmin) return;
      
      try {
        setTenantsLoading(true);
        const response = await apiService.getTenants();
        setTenants(response || []);
        // Nastavíme default tenant na tenant uživatele
        if (response?.length > 0 && !selectedTenant) {
          const userTenant = response.find(t => t.key === user?.tenantKey);
          setSelectedTenant(userTenant?.key || response[0]?.key || '');
        }
      } catch (error) {
        logger.error('Failed to fetch tenants', { error: error.message });
      } finally {
        setTenantsLoading(false);
      }
    };

    fetchTenants();
  }, [isCoreAdmin, user]);

  // Load roles
  const loadRoles = async () => {
    if (!hasPermission) {
      setError('Nemáte oprávnění k zobrazení seznamu rolí.');
      setLoading(false);
      return;
    }

    // CORE_ADMIN musí mít vybraný tenant
    if (isCoreAdmin && !selectedTenant) {
      setRoles([]);
      setFilteredRoles([]);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      // Pro CORE_ADMIN načteme role z vybraného tenantu
      const rolesData = isCoreAdmin && selectedTenant
        ? await apiService.getRolesByTenant(selectedTenant)
        : await apiService.getRoles();
        
      setRoles(rolesData || []);
      setFilteredRoles(rolesData || []);
      logger.info('Roles loaded', { count: rolesData?.length || 0, tenant: selectedTenant });
    } catch (error) {
      logger.error('Failed to load roles', { error: error.message });
      setError('Nepodařilo se načíst seznam rolí.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!isCoreAdmin || selectedTenant) {
      loadRoles();
    }
  }, [selectedTenant]);

  // Search/filter roles
  useEffect(() => {
    if (!searchQuery) {
      setFilteredRoles(roles);
      return;
    }

    const query = searchQuery.toLowerCase();
    const filtered = roles.filter(r => 
      r.name?.toLowerCase().includes(query) ||
      r.description?.toLowerCase().includes(query)
    );
    setFilteredRoles(filtered);
  }, [searchQuery, roles]);

  // Get role display name
  const getRoleDisplayName = (roleName) => {
    switch (roleName) {
      case 'CORE_ROLE_ADMIN':
        return 'Administrátor';
      case 'CORE_ROLE_USER_MANAGER':
        return 'Správce uživatelů';
      case 'CORE_ROLE_USER':
        return 'Běžný uživatel';
      default:
        return roleName?.replace('CORE_ROLE_', '') || roleName;
    }
  };

  // Get role type chip
  const getRoleTypeChip = (role) => {
    if (role.composite) {
      return (
        <Chip 
          label="Composite" 
          size="small" 
          color="secondary" 
          icon={<AccountTreeIcon fontSize="small" />}
        />
      );
    }
    return (
      <Chip 
        label="Basic" 
        size="small" 
        variant="outlined"
      />
    );
  };

  // Action handlers
  const handleMenuOpen = (event, role) => {
    setMenuAnchor(event.currentTarget);
    setMenuRole(role);
  };

  const handleMenuClose = () => {
    setMenuAnchor(null);
    setMenuRole(null);
  };

  const handleCreateRole = () => {
    setCreateDialogOpen(true);
  };

  const handleEditRole = (role) => {
    setSelectedRole(role);
    setEditDialogOpen(true);
    handleMenuClose();
  };

  const handleDeleteRole = (role) => {
    setSelectedRole(role);
    setDeleteDialogOpen(true);
    handleMenuClose();
  };

  const handleManageComposites = (role) => {
    setSelectedRole(role);
    setCompositeBuilderOpen(true);
    handleMenuClose();
  };

  const handleViewUsers = (role) => {
    setSelectedRole(role);
    setUsersViewOpen(true);
    handleMenuClose();
  };

  const handleRefresh = () => {
    loadRoles();
  };

  // Success handlers
  const handleRoleCreated = () => {
    setCreateDialogOpen(false);
    loadRoles();
  };

  const handleRoleUpdated = () => {
    setEditDialogOpen(false);
    setSelectedRole(null);
    loadRoles();
  };

  const handleRoleDeleted = () => {
    setDeleteDialogOpen(false);
    setSelectedRole(null);
    loadRoles();
  };

  const handleCompositesUpdated = () => {
    setCompositeBuilderOpen(false);
    setSelectedRole(null);
    loadRoles();
  };

  // DataTable columns definition
  const columns = [
    {
      field: 'name',
      label: 'Název role',
      sortable: true,
      render: (role) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <SecurityIcon color="primary" fontSize="small" />
          <Box>
            <Typography variant="body2" sx={{ fontWeight: 500 }}>
              {getRoleDisplayName(role.name)}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {role.name}
            </Typography>
          </Box>
        </Box>
      ),
    },
    {
      field: 'description',
      label: 'Popis',
      sortable: false,
      render: (role) => (
        <Typography variant="body2">
          {role.description || 'Bez popisu'}
        </Typography>
      ),
    },
    {
      field: 'tenant',
      label: 'Tenant',
      sortable: false,
      render: (role) => (
        <Chip 
          label={isCoreAdmin ? selectedTenant : (user?.tenantKey || 'admin')} 
          size="small"
          icon={<BusinessIcon />}
          color="primary"
          variant="outlined"
        />
      ),
    },
    {
      field: 'type',
      label: 'Typ',
      sortable: false,
      render: (role) => getRoleTypeChip(role),
    },
    {
      field: 'userCount',
      label: 'Uživatelé',
      sortable: true,
      align: 'center',
      render: (role) => (
        <Chip 
          label={role.userCount || 0}
          size="small"
          color={role.userCount > 0 ? 'primary' : 'default'}
          onClick={(e) => {
            e.stopPropagation();
            setSelectedRole(role);
            setRoleUsersDialogOpen(true);
          }}
          sx={{ cursor: 'pointer', '&:hover': { opacity: 0.8 } }}
        />
      ),
    },
  ];

  if (canManageRoles) {
    columns.push({
      field: 'actions',
      label: 'Akce',
      sortable: false,
      align: 'right',
      render: (role) => (
        <IconButton
          size="small"
          onClick={(e) => {
            e.stopPropagation();
            handleMenuOpen(e, role);
          }}
        >
          <MoreVertIcon />
        </IconButton>
      ),
    });
  }

  const handleRowClick = (role) => {
    if (canManageRoles) {
      handleEditRole(role);
    }
  };

  if (!hasPermission) {
    return (
      <Box>
        <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600 }}>
          Role
        </Typography>
        
        <Alert severity="error">
          Nemáte oprávnění k zobrazení seznamu rolí.
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
            Správa rolí
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Zde můžete spravovat role a jejich hierarchii v systému.
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="Obnovit seznam">
            <IconButton onClick={handleRefresh} disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          {canManageRoles && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={handleCreateRole}
              disabled={loading || (isCoreAdmin && !selectedTenant)}
            >
              Vytvořit roli
            </Button>
          )}
        </Box>
      </Box>

      {/* Tenant selector for CORE_ADMIN */}
      {isCoreAdmin && (
        <Box sx={{ mb: 3, p: 2, background: 'rgba(25, 118, 210, 0.05)', borderRadius: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <BusinessIcon color="primary" />
            <Typography variant="h6">Výběr tenantu</Typography>
          </Box>
          <Box sx={{ mt: 2 }}>
            <TextField
              select
              fullWidth
              label="Tenant pro správu rolí"
              value={selectedTenant}
              onChange={(e) => setSelectedTenant(e.target.value)}
              disabled={tenantsLoading}
              SelectProps={{
                native: false,
              }}
            >
              {tenants.map((tenant) => (
                <MenuItem key={tenant.key} value={tenant.key}>
                  {tenant.name || tenant.key}
                </MenuItem>
              ))}
            </TextField>
            <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
              Role budou načteny a spravovány pro vybraný tenant
            </Typography>
          </Box>
        </Box>
      )}

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
              placeholder="Hledat role..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
              disabled={loading || roles.length === 0}
            />
          </Box>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress />
            </Box>
          ) : filteredRoles.length === 0 ? (
            <Box sx={{ textAlign: 'center', p: 3 }}>
              <SecurityIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                {searchQuery ? 'Nebyly nalezeny žádné role odpovídající vyhledávání' : 'Nebyly nalezeny žádné role'}
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
              data={filteredRoles}
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
        <MenuItem onClick={() => handleEditRole(menuRole)}>
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Upravit</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleManageComposites(menuRole)}>
          <ListItemIcon>
            <AccountTreeIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Spravovat hierarchii</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleViewUsers(menuRole)}>
          <ListItemIcon>
            <PeopleIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Zobrazit uživatele</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleDeleteRole(menuRole)} sx={{ color: 'error.main' }}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText>Smazat</ListItemText>
        </MenuItem>
      </Menu>

      {/* Dialogs */}
      <CreateRoleDialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        onSuccess={handleRoleCreated}
        tenantKey={selectedTenant}
      />

      <EditRoleDialog
        open={editDialogOpen}
        onClose={() => {
          setEditDialogOpen(false);
          setSelectedRole(null);
        }}
        role={selectedRole}
        onSuccess={handleRoleUpdated}
      />

      <DeleteRoleDialog
        open={deleteDialogOpen}
        onClose={() => {
          setDeleteDialogOpen(false);
          setSelectedRole(null);
        }}
        role={selectedRole}
        onSuccess={handleRoleDeleted}
      />

      <CompositeRoleBuilder
        open={compositeBuilderOpen}
        onClose={() => {
          setCompositeBuilderOpen(false);
          setSelectedRole(null);
        }}
        role={selectedRole}
        onSuccess={handleCompositesUpdated}
      />

      <RoleUsersView
        open={usersViewOpen}
        onClose={() => {
          setUsersViewOpen(false);
          setSelectedRole(null);
        }}
        role={selectedRole}
      />

      <RoleUsersDialog
        open={roleUsersDialogOpen}
        onClose={() => setRoleUsersDialogOpen(false)}
        role={selectedRole}
        tenantKey={isCoreAdmin ? selectedTenant : user?.tenantKey}
        onUpdate={loadRoles}
      />
    </Box>
  );
}

Roles.propTypes = {
  user: UserPropType,
};

export default Roles;
