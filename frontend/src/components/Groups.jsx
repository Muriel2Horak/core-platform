import { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Typography,
  Button,
  Alert,
  Slide,
  Chip,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Card,
  CardContent,
  IconButton,
  Menu,
  Tooltip
} from '@mui/material';
import {
  Add as AddIcon,
  Refresh as RefreshIcon,
  Group as GroupIcon,
  MoreVert as MoreVertIcon,
  Visibility as VisibilityIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  People as PeopleIcon,
  Business as BusinessIcon
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';
import { UserPropType } from '../shared/propTypes.js';
import { DataTable } from './common/DataTable.jsx';
import CreateGroupDialog from './Groups/CreateGroupDialog.jsx';
import EditGroupDialog from './Groups/EditGroupDialog.jsx';
import DeleteGroupDialog from './Groups/DeleteGroupDialog.jsx';
import GroupMembersDialog from './Groups/GroupMembersDialog.jsx';
import ViewGroupDialog from './Groups/ViewGroupDialog.jsx';

function Groups({ user }) {
  const [groups, setGroups] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Dialogs state
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [membersDialogOpen, setMembersDialogOpen] = useState(false);
  const [viewDialogOpen, setViewDialogOpen] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);

  // Menu state
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [menuGroup, setMenuGroup] = useState(null);

  // Tenants for admin
  const [tenants, setTenants] = useState([]);
  const [selectedTenant, setSelectedTenant] = useState('');

  // Permissions
  const isAdmin = user?.roles?.includes('CORE_ROLE_ADMIN');
  const isTenantAdmin = user?.roles?.includes('CORE_ROLE_TENANT_ADMIN');
  const canManageGroups = isAdmin || isTenantAdmin;

  useEffect(() => {
    if (canManageGroups) {
      loadGroups();
      if (isAdmin) {
        loadTenants();
      }
    }
  }, [canManageGroups, isAdmin, selectedTenant]);

  const loadGroups = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      logger.pageView('/groups', { operation: 'load_groups' });

      let groupsData = await apiService.getGroups();
      
      // Filter by tenant for tenant admins
      if (isTenantAdmin && !isAdmin) {
        const userTenant = user?.tenantKey;
        groupsData = groupsData.filter(g => g.tenantKey === userTenant);
      } else if (selectedTenant && selectedTenant !== 'all') {
        groupsData = groupsData.filter(g => g.tenantKey === selectedTenant);
      }

      // Load member count for each group
      const groupsWithMembers = await Promise.all(
        groupsData.map(async (group) => {
          try {
            const members = await apiService.getGroupMembers(group.name);
            return {
              ...group,
              memberCount: members?.length || 0
            };
          } catch (err) {
            console.warn(`Failed to load members for group ${group.name}:`, err);
            return {
              ...group,
              memberCount: 0
            };
          }
        })
      );

      setGroups(groupsWithMembers);
      logger.userAction('GROUPS_LOADED', { count: groupsWithMembers.length });
    } catch (err) {
      console.error('Failed to load groups:', err);
      setError('Nepodařilo se načíst seznam skupin: ' + err.message);
      logger.error('GROUPS_LOAD_ERROR', err.message);
    } finally {
      setLoading(false);
    }
  }, [isAdmin, isTenantAdmin, selectedTenant, user]);

  const loadTenants = useCallback(async () => {
    try {
      const data = await apiService.getTenants();
      setTenants(data || []);
    } catch (err) {
      console.error('Failed to load tenants:', err);
    }
  }, []);

  const handleMenuOpen = (event, group) => {
    event.stopPropagation();
    setMenuAnchor(event.currentTarget);
    setMenuGroup(group);
  };

  const handleMenuClose = () => {
    setMenuAnchor(null);
    setMenuGroup(null);
  };

  const handleViewGroup = (group) => {
    setSelectedGroup(group || menuGroup);
    setViewDialogOpen(true);
    handleMenuClose();
  };

  const handleEditGroup = (group) => {
    setSelectedGroup(group || menuGroup);
    setEditDialogOpen(true);
    handleMenuClose();
  };

  const handleDeleteGroup = (group) => {
    setSelectedGroup(group || menuGroup);
    setDeleteDialogOpen(true);
    handleMenuClose();
  };

  const handleManageMembers = (group) => {
    setSelectedGroup(group || menuGroup);
    setMembersDialogOpen(true);
    handleMenuClose();
  };

  const handleRefresh = () => {
    loadGroups();
  };

  const handleGroupCreated = () => {
    setSuccess('Skupina byla úspěšně vytvořena');
    loadGroups();
    setTimeout(() => setSuccess(null), 5000);
  };

  const handleGroupUpdated = () => {
    setSuccess('Skupina byla úspěšně aktualizována');
    loadGroups();
    setTimeout(() => setSuccess(null), 5000);
  };

  const handleGroupDeleted = () => {
    setSuccess('Skupina byla úspěšně smazána');
    loadGroups();
    setTimeout(() => setSuccess(null), 5000);
  };

  // DataTable columns
  const columns = [
    {
      field: 'name',
      label: 'Název skupiny',
      sortable: true,
      render: (group) => (
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <GroupIcon color="primary" />
          <Box>
            <Typography variant="body1" sx={{ fontWeight: 600 }}>
              {group.name}
            </Typography>
            {group.path && (
              <Typography variant="caption" color="text.secondary">
                {group.path}
              </Typography>
            )}
          </Box>
        </Box>
      ),
    },
  ];

  if (isAdmin) {
    columns.push({
      field: 'tenant',
      label: 'Tenant',
      sortable: true,
      render: (group) => (
        <Chip
          icon={<BusinessIcon fontSize="small" />}
          label={group.tenantKey || user?.tenantKey || 'N/A'}
          size="small"
          color="primary"
          variant="outlined"
          sx={{ borderRadius: 2 }}
        />
      ),
    });
  }

  columns.push(
    {
      field: 'memberCount',
      label: 'Počet členů',
      sortable: true,
      render: (group) => (
        <Tooltip title="Klikněte pro zobrazení členů">
          <Chip
            icon={<PeopleIcon fontSize="small" />}
            label={group.memberCount || 0}
            size="small"
            color="info"
            onClick={(e) => {
              e.stopPropagation();
              handleManageMembers(group);
            }}
            sx={{ 
              borderRadius: 2,
              cursor: 'pointer',
              '&:hover': {
                backgroundColor: 'info.dark'
              }
            }}
          />
        </Tooltip>
      ),
    },
    {
      field: 'actions',
      label: 'Akce',
      sortable: false,
      align: 'right',
      render: (group) => (
        <IconButton
          size="small"
          onClick={(e) => handleMenuOpen(e, group)}
        >
          <MoreVertIcon />
        </IconButton>
      ),
    }
  );

  if (!canManageGroups) {
    return (
      <Box sx={{ maxWidth: 1400, mx: 'auto', p: 3 }}>
        <Alert severity="warning">
          Nemáte oprávnění pro správu skupin.
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ maxWidth: 1400, mx: 'auto' }}>
      <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600, mb: 4 }}>
        Seznam skupin
      </Typography>

      {/* Messages */}
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

      {/* Header */}
      <Card sx={{ mb: 3, borderRadius: 3 }}>
        <CardContent sx={{ p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 2 }}>
            <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
              {isAdmin && (
                <FormControl sx={{ minWidth: 200 }}>
                  <InputLabel>Tenant</InputLabel>
                  <Select
                    value={selectedTenant}
                    onChange={(e) => setSelectedTenant(e.target.value)}
                    label="Tenant"
                    size="small"
                    sx={{ borderRadius: 2 }}
                  >
                    <MenuItem value="">Všechny tenanty</MenuItem>
                    {tenants.map((tenant) => (
                      <MenuItem key={tenant.key} value={tenant.key}>
                        {tenant.displayName || tenant.key}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              )}
            </Box>

            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={handleRefresh}
                disabled={loading}
                sx={{ borderRadius: 2 }}
              >
                Obnovit
              </Button>
              <Button
                variant="contained"
                startIcon={<AddIcon />}
                onClick={() => setCreateDialogOpen(true)}
                sx={{ borderRadius: 2 }}
              >
                Vytvořit skupinu
              </Button>
            </Box>
          </Box>
        </CardContent>
      </Card>

      {/* Groups Table */}
      <Card sx={{ borderRadius: 3 }}>
        <DataTable
          columns={columns}
          data={groups}
          loading={loading}
          onRowClick={handleViewGroup}
        />
      </Card>

      {/* Context Menu */}
      <Menu
        anchorEl={menuAnchor}
        open={Boolean(menuAnchor)}
        onClose={handleMenuClose}
      >
        <MenuItem onClick={() => handleViewGroup(menuGroup)}>
          <VisibilityIcon fontSize="small" sx={{ mr: 1 }} />
          Zobrazit detail
        </MenuItem>
        <MenuItem onClick={() => handleEditGroup(menuGroup)}>
          <EditIcon fontSize="small" sx={{ mr: 1 }} />
          Upravit
        </MenuItem>
        <MenuItem onClick={() => handleManageMembers(menuGroup)}>
          <PeopleIcon fontSize="small" sx={{ mr: 1 }} />
          Spravovat členy
        </MenuItem>
        <MenuItem onClick={() => handleDeleteGroup(menuGroup)}>
          <DeleteIcon fontSize="small" sx={{ mr: 1 }} />
          Smazat
        </MenuItem>
      </Menu>

      {/* Dialogs */}
      <CreateGroupDialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        onGroupCreated={handleGroupCreated}
        tenantKey={isTenantAdmin && !isAdmin ? user?.tenantKey : selectedTenant}
      />

      <EditGroupDialog
        open={editDialogOpen}
        group={selectedGroup}
        onClose={() => {
          setEditDialogOpen(false);
          setSelectedGroup(null);
        }}
        onGroupUpdated={handleGroupUpdated}
      />

      <DeleteGroupDialog
        open={deleteDialogOpen}
        group={selectedGroup}
        onClose={() => {
          setDeleteDialogOpen(false);
          setSelectedGroup(null);
        }}
        onGroupDeleted={handleGroupDeleted}
      />

      <GroupMembersDialog
        open={membersDialogOpen}
        group={selectedGroup}
        onClose={() => {
          setMembersDialogOpen(false);
          setSelectedGroup(null);
        }}
        onMembersUpdated={handleRefresh}
      />

      <ViewGroupDialog
        open={viewDialogOpen}
        group={selectedGroup}
        onClose={() => {
          setViewDialogOpen(false);
          setSelectedGroup(null);
        }}
        onEdit={() => {
          setViewDialogOpen(false);
          handleEditGroup(selectedGroup);
        }}
        onDelete={() => {
          setViewDialogOpen(false);
          handleDeleteGroup(selectedGroup);
        }}
      />
    </Box>
  );
}

Groups.propTypes = {
  user: UserPropType,
};

export default Groups;
