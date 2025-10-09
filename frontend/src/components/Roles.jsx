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
  RoleDetailDialog,
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
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [roleUsersDialogOpen, setRoleUsersDialogOpen] = useState(false);
  const [selectedRole, setSelectedRole] = useState(null);

  // Menu state
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [menuRole, setMenuRole] = useState(null);

  // Check if user has permission
  const isCoreAdmin = user?.roles?.includes('CORE_ROLE_ADMIN');
  const isTenantAdmin = user?.roles?.includes('CORE_ROLE_TENANT_ADMIN');
  const hasPermission = user?.roles?.includes('CORE_ROLE_USER_MANAGER') || isCoreAdmin || isTenantAdmin;
  const canManageRoles = isCoreAdmin || isTenantAdmin;

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
          const defaultTenant = userTenant?.key || response[0]?.key || '';
          console.log('🔧 Setting default tenant:', defaultTenant, 'from tenants:', response);
          setSelectedTenant(defaultTenant);
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
      console.log('⚠️ CORE_ADMIN without selected tenant, skipping role load');
      setRoles([]);
      setFilteredRoles([]);
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      // Určíme tenant podle role
      let tenantToLoad = selectedTenant;
      
      // Pro tenant adminy použijeme jejich tenant
      if (isTenantAdmin && !isCoreAdmin) {
        tenantToLoad = user?.tenantKey;
      }
      
      console.log('📥 Loading roles for tenant:', tenantToLoad, 'isCoreAdmin:', isCoreAdmin, 'isTenantAdmin:', isTenantAdmin);
      
      // Pro CORE_ADMIN načteme role z vybraného tenantu
      const rolesData = tenantToLoad
        ? await apiService.getRolesByTenant(tenantToLoad)
        : await apiService.getRoles();
      
      console.log('📦 Loaded roles:', rolesData?.length || 0);
      
      // Pro každou roli načteme počet uživatelů
      const rolesWithUserCount = await Promise.all(
        (rolesData || []).map(async (role) => {
          try {
            // Pro CORE_ADMIN předáme tenantKey
            const users = isCoreAdmin 
              ? await apiService.getRoleUsers(role.name, tenantToLoad)
              : await apiService.getRoleUsers(role.name);
            console.log(`👥 Users for ${role.name}:`, users?.length || 0, users);
            return {
              ...role,
              userCount: Array.isArray(users) ? users.length : 0,
            };
          } catch (err) {
            logger.warn('Failed to load user count for role', { roleName: role.name, error: err.message });
            console.warn(`❌ Failed to load users for ${role.name}:`, err);
            return { ...role, userCount: 0 };
          }
        })
      );
        
      setRoles(rolesWithUserCount || []);
      setFilteredRoles(rolesWithUserCount || []);
      logger.info('Roles loaded with user counts', { 
        count: rolesWithUserCount?.length || 0, 
        tenant: tenantToLoad,
        rolesData: rolesWithUserCount 
      });
      console.log('🔍 DEBUG - Roles state:', {
        rolesCount: rolesWithUserCount?.length,
        filteredRolesCount: rolesWithUserCount?.length,
        firstRole: rolesWithUserCount?.[0],
        allRoles: rolesWithUserCount
      });
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
    // Tenant admin načte role automaticky
    if (isTenantAdmin && !isCoreAdmin) {
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
          icon={<AccountTreeIcon fontSize="small" />}
          sx={{ 
            bgcolor: 'secondary.main',
            color: 'white',
            fontWeight: 600,
            '& .MuiChip-icon': {
              color: 'white'
            }
          }}
        />
      );
    }
    return (
      <Chip 
        label="Basic" 
        size="small" 
        color="default"
        sx={{ 
          bgcolor: 'grey.200',
          color: 'text.primary',
          fontWeight: 600,
          border: '1px solid',
          borderColor: 'grey.400'
        }}
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

  const handleOpenDetail = (role) => {
    setSelectedRole(role);
    setDetailDialogOpen(true);
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
    setDetailDialogOpen(false);
    setSelectedRole(null);
    loadRoles();
  };

  const handleRoleDeleted = () => {
    setDetailDialogOpen(false);
    setSelectedRole(null);
    loadRoles();
  };

  // DataTable columns definition
  const columns = [
    {
      field: 'name',
      label: 'Název role',
      sortable: true,
      type: 'custom',
      render: (value, role) => (
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
      type: 'custom',
      render: (value, role) => (
        <Typography variant="body2">
          {role.description || 'Bez popisu'}
        </Typography>
      ),
    },
  ];

  // Add Tenant column only for CORE_ADMIN
  if (isCoreAdmin) {
    columns.push({
      field: 'tenant',
      label: 'Tenant',
      sortable: false,
      type: 'custom',
      render: (value, role) => (
        <Chip 
          label={role.tenantKey || selectedTenant || user?.tenantKey || 'admin'} 
          size="small"
          icon={<BusinessIcon />}
          color="primary"
          variant="outlined"
        />
      ),
    });
  }

  columns.push(
    {
      field: 'type',
      label: 'Typ',
      sortable: false,
      type: 'custom',
      render: (value, role) => getRoleTypeChip(role),
    },
    {
      field: 'userCount',
      label: 'Uživatelé',
      sortable: true,
      align: 'center',
      type: 'custom',
      render: (value, role) => (
        <Chip 
          label={role.userCount || 0}
          size="small"
          onClick={(e) => {
            e.stopPropagation();
            setSelectedRole(role);
            setRoleUsersDialogOpen(true);
          }}
          sx={{ 
            cursor: 'pointer',
            bgcolor: role.userCount > 0 ? 'primary.main' : 'grey.300',
            color: role.userCount > 0 ? 'white' : 'text.secondary',
            fontWeight: 600,
            '&:hover': { opacity: 0.8 }
          }}
        />
      ),
    }
  );

  if (canManageRoles) {
    columns.push({
      field: 'actions',
      label: 'Akce',
      sortable: false,
      align: 'right',
      type: 'custom',
      render: (value, role) => (
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
      handleOpenDetail(role);
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
            <>
              {console.log('🚨 Showing empty state!', { 
                loading, 
                rolesLength: roles.length,
                filteredRolesLength: filteredRoles.length,
                searchQuery 
              })}
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
            </>
          ) : (
            <>
              {console.log('✅ Showing DataTable!', { 
                loading,
                rolesLength: roles.length,
                filteredRolesLength: filteredRoles.length, 
                columnsLength: columns.length,
                firstRole: filteredRoles[0],
                searchQuery
              })}
              <DataTable
                columns={columns}
                data={filteredRoles}
                onRowClick={handleRowClick}
                loading={loading}
              />
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
        <MenuItem onClick={() => { handleOpenDetail(menuRole); handleMenuClose(); }}>
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Otevřít detail</ListItemText>
        </MenuItem>
      </Menu>

      {/* Dialogs */}
      <CreateRoleDialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        onSuccess={handleRoleCreated}
        tenantKey={isTenantAdmin && !isCoreAdmin ? user?.tenantKey : selectedTenant}
      />

      <RoleDetailDialog
        open={detailDialogOpen}
        onClose={() => {
          setDetailDialogOpen(false);
          setSelectedRole(null);
        }}
        role={selectedRole}
        onSuccess={handleRoleUpdated}
        onDelete={handleRoleDeleted}
        user={user}
        tenantKey={isCoreAdmin ? selectedTenant : null}
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
