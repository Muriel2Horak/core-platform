import { useState, useEffect, useCallback } from 'react';

import {
  Box,
  Grid,
  Typography,
  TextField,
  Button,
  Alert,
  CircularProgress,
  Chip,
  FormControl,
  InputLabel,
  Select,
  Card,
  CardContent,
  Avatar,
  Tooltip,
  Pagination,
  InputAdornment,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Paper,
  Slide,
  Tabs,
  Tab,
  Divider,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText
} from '@mui/material';
import {
  Search as SearchIcon,
  Visibility as VisibilityIcon,
  Refresh as RefreshIcon,
  People as PeopleIcon,
  Cloud as CloudIcon,
  Storage as ServerIcon,
  AccountTree as OrgChartIcon,
  List as ListIcon,
  Person as PersonIcon,
  ArrowUpward as ArrowUpwardIcon,
  ArrowDownward as ArrowDownwardIcon
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';
import { UserPropType } from '../shared/propTypes.js';
import { DataTable } from './common/DataTable.jsx';
import OrgChartView from './common/OrgChartView.jsx';
import UserOrgChart from './common/UserOrgChart.jsx';

function UserDirectory({ user }) {
  // State for data
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // State for pagination
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // State for filters
  const [searchFilters, setSearchFilters] = useState({
    q: '',
    source: '',
    tenantKey: ''
  });
  const [debouncedQuery, setDebouncedQuery] = useState('');

  // State for tenants (admin filter)
  const [tenants, setTenants] = useState([]);
  const [tenantsLoading, setTenantsLoading] = useState(false);

  // User permissions
  const isAdmin = user?.roles?.includes('CORE_ROLE_ADMIN');
  const isUserManager = user?.roles?.includes('CORE_ROLE_USER_MANAGER');
  const canViewAllTenants = isAdmin;

  // State for user view dialog
  const [selectedUser, setSelectedUser] = useState(null);
  const [viewDialog, setViewDialog] = useState(false);
  const [viewDialogTab, setViewDialogTab] = useState(0);
  
  // State for main view tabs (List / Org Chart)
  const [mainTab, setMainTab] = useState(0);

  // Debounce search query
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchFilters.q);
    }, 400);

    return () => clearTimeout(timer);
  }, [searchFilters.q]);

  // Load data when filters change - 🆕 Všichni autentifikovaní uživatelé mohou načíst User Directory
  useEffect(() => {
    loadUsers();
  }, [page, debouncedQuery, searchFilters.source, searchFilters.tenantKey]);

  // Load tenants for admin
  useEffect(() => {
    if (canViewAllTenants) {
      loadTenants();
    }
  }, [canViewAllTenants]);

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      logger.pageView('/directory', { 
        operation: 'load_users', 
        page, 
        filters: searchFilters 
      });
      
      // 🆕 Používáme nový User Directory API místo User Management API
      const params = {
        q: debouncedQuery,
        source: searchFilters.source,
        tenantKey: searchFilters.tenantKey,
        page,
        size: 20,
        sort: 'username'
      };
      
      const response = await apiService.getUsersDirectory(params);
      
      setUsers(response.content || []);
      setTotalElements(response.totalElements || 0);
      setTotalPages(response.totalPages || 1);
      
      logger.userAction('USERS_LOADED', { 
        count: response.content?.length || 0, 
        page,
        totalElements: response.totalElements || 0
      });
      
    } catch (err) {
      console.error('Failed to load users:', err);
      setError('Nepodařilo se načíst seznam uživatelů: ' + err.message);
      logger.error('USERS_LOAD_ERROR', err.message);
    } finally {
      setLoading(false);
    }
  }, [page, debouncedQuery, searchFilters]);

  const loadTenants = useCallback(async () => {
    try {
      setTenantsLoading(true);
      const data = await apiService.getTenants();
      setTenants(data || []);
    } catch (err) {
      console.error('Failed to load tenants:', err);
      // Non-critical error - don't show to user
    } finally {
      setTenantsLoading(false);
    }
  }, []);

  const handleSearchChange = (field, value) => {
    setSearchFilters(prev => ({ ...prev, [field]: value }));
    setPage(0); // Reset to first page when searching
  };

  const handleViewUser = (userData) => {
    setSelectedUser(userData);
    setViewDialog(true);
    logger.userAction('USER_VIEW_CLICKED', { userId: userData.id });
  };

  const getDisplayName = (userData) => {
    if (userData?.firstName && userData?.lastName) {
      return `${userData.firstName} ${userData.lastName}`;
    }
    return userData?.username || userData?.email || 'Neznámý uživatel';
  };

  const getInitials = (userData) => {
    const name = getDisplayName(userData);
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  // Org Chart Helper Functions
  const buildOrgHierarchy = useCallback(() => {
    // Build a tree structure from flat user list
    const userMap = {};
    const roots = [];
    
    users.forEach(user => {
      userMap[user.username] = { ...user, children: [] };
    });
    
    users.forEach(user => {
      if (user.manager && userMap[user.manager]) {
        userMap[user.manager].children.push(userMap[user.username]);
      } else if (!user.manager) {
        roots.push(userMap[user.username]);
      }
    });
    
    return roots;
  }, [users]);

  const getUserHierarchy = (username) => {
    // Get ancestors (managers above) and descendants (reports below) for a specific user
    const ancestors = [];
    const descendants = [];
    
    // Find ancestors
    let currentUsername = username;
    let currentUser = users.find(u => u.username === currentUsername);
    
    while (currentUser?.manager) {
      const managerUser = users.find(u => u.username === currentUser.manager);
      if (managerUser && !ancestors.find(a => a.username === managerUser.username)) {
        ancestors.unshift(managerUser); // Add to beginning
        currentUser = managerUser;
      } else {
        break;
      }
    }
    
    // Find descendants (direct reports)
    const findDescendants = (managerUsername) => {
      const reports = users.filter(u => u.manager === managerUsername);
      reports.forEach(report => {
        descendants.push(report);
        // Recursively find their reports
        findDescendants(report.username);
      });
    };
    
    findDescendants(username);
    
    return { ancestors, descendants };
  };

  // DataTable columns definition
  const columns = [
    {
      field: 'user',
      label: 'Uživatel',
      sortable: true,
      render: (userData) => (
        <Box display="flex" alignItems="center" gap={2}>
          <Avatar
            sx={{
              width: 40,
              height: 40,
              backgroundColor: 'primary.main',
              fontWeight: 'bold'
            }}
          >
            {getInitials(userData)}
          </Avatar>
          <Box>
            <Typography variant="subtitle2" sx={{ fontWeight: 600 }}>
              {getDisplayName(userData)}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              @{userData?.username}
            </Typography>
          </Box>
        </Box>
      ),
    },
    {
      field: 'email',
      label: 'Email',
      sortable: true,
      render: (userData) => (
        <Typography variant="body2">
          {userData?.email || 'N/A'}
        </Typography>
      ),
    },
  ];

  // Add tenant column only for admin users
  if (canViewAllTenants) {
    columns.push({
      field: 'tenantKey',
      label: 'Tenant',
      sortable: true,
      render: (userData) => (
        <Chip
          label={userData?.tenantKey || 'Unknown'}
          size="small"
          color="primary"
          variant="outlined"
          sx={{ borderRadius: 2 }}
        />
      ),
    });
  }

  columns.push({
    field: 'source',
    label: 'Zdroj',
    sortable: false,
    render: (userData) => (
      <Chip
        icon={userData?.isFederated ? <CloudIcon fontSize="small" /> : <ServerIcon fontSize="small" />}
        label={userData?.directorySource || (userData?.isFederated ? 'AD' : 'LOCAL')}
        size="small"
        color={userData?.isFederated ? 'info' : 'success'}
        sx={{ borderRadius: 2 }}
      />
    ),
  });

  columns.push({
    field: 'manager',
    label: 'Nadřízený',
    sortable: false,
    render: (userData) => {
      if (!userData?.manager) {
        return (
          <Typography variant="body2" color="text.secondary">
            -
          </Typography>
        );
      }
      
      // Find manager in users list to get full name
      const managerUser = users.find(u => u.username === userData.manager);
      const managerName = managerUser 
        ? `${managerUser.firstName || ''} ${managerUser.lastName || ''}`.trim() 
        : userData.manager;
      
      return (
        <Tooltip title={`@${userData.manager}`}>
          <Chip
            label={managerName || userData.manager}
            size="small"
            variant="outlined"
            sx={{ borderRadius: 2 }}
          />
        </Tooltip>
      );
    },
  });

  columns.push({
    field: 'enabled',
    label: 'Stav',
    sortable: false,
    render: (userData) => (
      <Chip
        label={userData?.enabled ? 'Aktivní' : 'Neaktivní'}
        size="small"
        sx={{
          borderRadius: 2,
          backgroundColor: userData?.enabled ? 'success.main' : 'error.main',
          color: 'white',
          fontWeight: 600,
          '& .MuiChip-label': {
            px: 2
          }
        }}
      />
    ),
  });
  
  columns.push({
    field: 'actions',
    label: 'Akce',
    sortable: false,
    align: 'right',
    render: (userData) => (
      <Tooltip title="Zobrazit detail">
        <Button
          size="small"
          startIcon={<VisibilityIcon />}
          onClick={(e) => {
            e.stopPropagation();
            handleViewUser(userData);
          }}
          sx={{ borderRadius: 2 }}
        >
          Detail
        </Button>
      </Tooltip>
    ),
  });

  // 🆕 OPRAVENO: Odebrána kontrola canManageUsers - všichni autentifikovaní uživatelé mají přístup k User Directory
  // 🚫 User Directory je READ-ONLY - žádné akce
  return (
    <Box sx={{ maxWidth: 1400, mx: 'auto' }}>
      <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600, mb: 4 }}>
        User Directory
      </Typography>
      
      <Typography variant="body1" color="text.secondary" sx={{ mb: 4 }}>
        Vyhledejte uživatele ve vaší organizaci.
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

      {/* Header & Filters */}
      <Card sx={{ mb: 3, borderRadius: 3 }}>
        <CardContent sx={{ p: 3 }}>
          <Box sx={{ mb: 3 }}>
            <Typography variant="h5" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1, fontWeight: 600 }}>
              <PeopleIcon />
              Pokročilé vyhledávání
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Použijte filtry pro přesné vyhledávání uživatelů
            </Typography>
          </Box>

          <Grid container spacing={3} alignItems="center">
            {/* Search */}
            <Grid item xs={12} sm={6} md={4}>
              <TextField
                fullWidth
                placeholder="Hledat uživatele..."
                value={searchFilters.q}
                onChange={(e) => handleSearchChange('q', e.target.value)}
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchIcon />
                    </InputAdornment>
                  )
                }}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2
                  }
                }}
              />
            </Grid>

            {/* Source Filter */}
            <Grid item xs={12} sm={3} md={2}>
              <FormControl fullWidth>
                <InputLabel>Zdroj</InputLabel>
                <Select
                  value={searchFilters.source}
                  onChange={(e) => handleSearchChange('source', e.target.value)}
                  label="Zdroj"
                  sx={{ borderRadius: 2 }}
                >
                  <MenuItem value="">Všechny</MenuItem>
                  <MenuItem value="AD">
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <CloudIcon fontSize="small" />
                      AD
                    </Box>
                  </MenuItem>
                  <MenuItem value="LOCAL">
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <ServerIcon fontSize="small" />
                      Local
                    </Box>
                  </MenuItem>
                </Select>
              </FormControl>
            </Grid>

            {/* Tenant Filter (pouze pro admin) */}
            {canViewAllTenants && (
              <Grid item xs={12} sm={3} md={3}>
                <FormControl fullWidth>
                  <InputLabel>Tenant</InputLabel>
                  <Select
                    value={searchFilters.tenantKey}
                    onChange={(e) => handleSearchChange('tenantKey', e.target.value)}
                    label="Tenant"
                    disabled={tenantsLoading}
                    sx={{ borderRadius: 2 }}
                  >
                    <MenuItem value="">Všechny tenanty</MenuItem>
                    {tenants.map((tenant) => (
                      <MenuItem key={tenant.key} value={tenant.key}>
                        {tenant.name}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
            )}

            {/* Refresh Button */}
            <Grid item xs={12} sm={12} md={canViewAllTenants ? 3 : 6}>
              <Box display="flex" gap={1} justifyContent="flex-end">
                <Button
                  variant="outlined"
                  startIcon={<RefreshIcon />}
                  onClick={loadUsers}
                  disabled={loading}
                  sx={{
                    borderRadius: 2,
                    borderWidth: 2,
                    '&:hover': { borderWidth: 2 }
                  }}
                >
                  Obnovit
                </Button>
              </Box>
            </Grid>
          </Grid>
        </CardContent>
      </Card>

      {/* Main View Tabs */}
      <Paper elevation={2} sx={{ borderRadius: 3, overflow: 'hidden', mb: 3 }}>
        <Tabs
          value={mainTab}
          onChange={(e, newValue) => setMainTab(newValue)}
          sx={{ 
            borderBottom: 1, 
            borderColor: 'divider',
            px: 2
          }}
        >
          <Tab 
            icon={<ListIcon />} 
            iconPosition="start" 
            label="Seznam uživatelů" 
          />
          <Tab 
            icon={<OrgChartIcon />} 
            iconPosition="start" 
            label="Org. Chart" 
          />
        </Tabs>
      </Paper>

      {/* Tab Panel: Users Table */}
      {mainTab === 0 && (
        <Paper elevation={2} sx={{ borderRadius: 3, overflow: 'hidden' }}>
          <Box sx={{ p: 3, borderBottom: 1, borderColor: 'divider' }}>
            <Typography variant="h6" sx={{ fontWeight: 600 }}>
              Seznam uživatelů ({totalElements})
            </Typography>
          </Box>

          {loading ? (
            <Box display="flex" justifyContent="center" p={4}>
              <CircularProgress size={40} />
            </Box>
          ) : users.length === 0 ? (
            <Box textAlign="center" py={6}>
              <PeopleIcon sx={{ fontSize: 80, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary" gutterBottom>
                Nebyly nalezeni žádní uživatelé
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Zkuste změnit vyhledávací kritéria nebo filtry.
              </Typography>
            </Box>
          ) : (
            <>
              <DataTable
                columns={columns}
                data={users}
                onRowClick={handleViewUser}
                loading={loading}
              />

              {/* Pagination */}
              {totalPages > 1 && (
                <Box display="flex" justifyContent="center" p={3}>
                  <Pagination
                    count={totalPages}
                    page={page + 1} // MUI uses 1-based pagination
                    onChange={(e, newPage) => setPage(newPage - 1)}
                    color="primary"
                    size="large"
                  />
                </Box>
              )}
            </>
          )}
        </Paper>
      )}

      {/* Tab Panel: Org Chart */}
      {mainTab === 1 && (
        <OrgChartView 
          users={users} 
          onUserClick={handleViewUser}
          getDisplayName={getDisplayName}
          getInitials={getInitials}
          buildOrgHierarchy={buildOrgHierarchy}
          loading={loading}
        />
      )}

      {/* View User Dialog */}
      <Dialog
        open={viewDialog}
        onClose={() => {
          setViewDialog(false);
          setViewDialogTab(0);
        }}
        maxWidth="md"
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: 3
          }
        }}
      >
        <DialogTitle sx={{ fontWeight: 600, fontSize: '1.5rem' }}>
          👤 Detail uživatele
        </DialogTitle>
        <DialogContent sx={{ p: 0 }}>
          {selectedUser ? (
            <Box>
              {/* User Header */}
              <Box sx={{ p: 3, pb: 0 }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
                  <Avatar
                    sx={{
                      width: 60,
                      height: 60,
                      backgroundColor: 'primary.main',
                      fontSize: '1.5rem',
                      fontWeight: 'bold'
                    }}
                  >
                    {getInitials(selectedUser)}
                  </Avatar>
                  <Box>
                    <Typography variant="h6" gutterBottom>
                      {getDisplayName(selectedUser)}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      @{selectedUser.username}
                    </Typography>
                  </Box>
                </Box>
              </Box>

              {/* Tabs */}
              <Tabs
                value={viewDialogTab}
                onChange={(e, newValue) => setViewDialogTab(newValue)}
                sx={{ borderBottom: 1, borderColor: 'divider', px: 3 }}
              >
                <Tab icon={<PersonIcon />} iconPosition="start" label="Informace" />
                <Tab icon={<OrgChartIcon />} iconPosition="start" label="Org. Chart" />
              </Tabs>

              {/* Tab 0: User Info */}
              {viewDialogTab === 0 && (
                <Box sx={{ p: 3 }}>
                  <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <Box>
                      <Typography variant="body2" color="text.secondary">Email</Typography>
                      <Typography variant="body1" sx={{ fontWeight: 500 }}>
                        {selectedUser.email || 'Neuvedeno'}
                      </Typography>
                    </Box>

                    {selectedUser.firstName && (
                      <Box>
                        <Typography variant="body2" color="text.secondary">Jméno</Typography>
                        <Typography variant="body1" sx={{ fontWeight: 500 }}>
                          {selectedUser.firstName}
                        </Typography>
                      </Box>
                    )}

                    {selectedUser.lastName && (
                      <Box>
                        <Typography variant="body2" color="text.secondary">Příjmení</Typography>
                        <Typography variant="body1" sx={{ fontWeight: 500 }}>
                          {selectedUser.lastName}
                        </Typography>
                      </Box>
                    )}

                    {selectedUser.manager && (
                      <Box>
                        <Typography variant="body2" color="text.secondary">Nadřízený</Typography>
                        <Box sx={{ mt: 0.5 }}>
                          <Chip
                            avatar={
                              <Avatar sx={{ width: 24, height: 24, fontSize: '0.75rem' }}>
                                {getInitials(users.find(u => u.username === selectedUser.manager) || { username: selectedUser.manager })}
                              </Avatar>
                            }
                            label={
                              users.find(u => u.username === selectedUser.manager)
                                ? getDisplayName(users.find(u => u.username === selectedUser.manager))
                                : selectedUser.manager
                            }
                            size="medium"
                            variant="outlined"
                            onClick={() => {
                              const managerUser = users.find(u => u.username === selectedUser.manager);
                              if (managerUser) handleViewUser(managerUser);
                            }}
                            sx={{ cursor: 'pointer' }}
                          />
                        </Box>
                      </Box>
                    )}

                    {canViewAllTenants && (
                      <Box>
                        <Typography variant="body2" color="text.secondary">Tenant</Typography>
                        <Chip
                          label={selectedUser.tenantKey || 'Unknown'}
                          size="small"
                          color="primary"
                          sx={{ mt: 0.5 }}
                        />
                      </Box>
                    )}

                    <Box>
                      <Typography variant="body2" color="text.secondary">Zdroj</Typography>
                      <Chip
                        icon={selectedUser.isFederated ? <CloudIcon fontSize="small" /> : <ServerIcon fontSize="small" />}
                        label={selectedUser.directorySource || (selectedUser.isFederated ? 'AD' : 'LOCAL')}
                        size="small"
                        color={selectedUser.isFederated ? 'info' : 'success'}
                        sx={{ mt: 0.5 }}
                      />
                    </Box>

                    <Box>
                      <Typography variant="body2" color="text.secondary">Stav</Typography>
                      <Chip
                        label={selectedUser.enabled ? 'Aktivní' : 'Neaktivní'}
                        size="small"
                        sx={{
                          mt: 0.5,
                          backgroundColor: selectedUser.enabled ? 'success.main' : 'error.main',
                          color: 'white',
                          fontWeight: 600
                        }}
                      />
                    </Box>

                    {selectedUser.emailVerified !== undefined && (
                      <Box>
                        <Typography variant="body2" color="text.secondary">Email ověřen</Typography>
                        <Chip
                          label={selectedUser.emailVerified ? 'Ano' : 'Ne'}
                          size="small"
                          color={selectedUser.emailVerified ? 'success' : 'default'}
                          sx={{ mt: 0.5 }}
                        />
                      </Box>
                    )}
                  </Box>
                </Box>
              )}

              {/* Tab 1: Org Chart */}
              {viewDialogTab === 1 && (
                <UserOrgChart
                  user={selectedUser}
                  users={users}
                  onUserClick={handleViewUser}
                  getDisplayName={getDisplayName}
                  getInitials={getInitials}
                  getUserHierarchy={getUserHierarchy}
                />
              )}
            </Box>
          ) : (
            <Box textAlign="center" py={6}>
              <CircularProgress />
            </Box>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 2 }}>
          <Button onClick={() => {
            setViewDialog(false);
            setViewDialogTab(0);
          }}>
            Zavřít
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

UserDirectory.propTypes = {
  user: UserPropType,
};

export default UserDirectory;