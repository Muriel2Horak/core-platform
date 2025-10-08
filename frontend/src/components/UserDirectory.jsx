import { useState, useEffect, useCallback } from 'react';

import {
  Box,
  Grid,
  Typography,
  TextField,
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
  Slide
} from '@mui/material';
import {
  Search as SearchIcon,
  Visibility as VisibilityIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  MoreVert as MoreVertIcon,
  Refresh as RefreshIcon,
  People as PeopleIcon,
  Cloud as CloudIcon,
  Storage as ServerIcon,
  // FilterList as FilterIcon, // Nepoužívané - odstraněno
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';
import { UserPropType } from '../shared/propTypes.js';

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
  // 🗑️ Odebrána proměnná canManageUsers - již není potřeba

  // State for user actions
  const [selectedUser, setSelectedUser] = useState(null);
  const [actionMenuAnchor, setActionMenuAnchor] = useState(null);
  const [deleteConfirmDialog, setDeleteConfirmDialog] = useState(false);
  const [viewDialog, setViewDialog] = useState(false);

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

  const handleActionMenuOpen = (event, userData) => {
    setActionMenuAnchor(event.currentTarget);
    setSelectedUser(userData);
  };

  const handleActionMenuClose = () => {
    setActionMenuAnchor(null);
    setSelectedUser(null);
  };

  const handleViewUser = (userData) => {
    setSelectedUser(userData);
    setViewDialog(true);
    // Nevoláme handleActionMenuClose() tady - menu už je zavřené přes onClick na TableRow
    logger.userAction('USER_VIEW_CLICKED', { userId: userData.id });
  };

  const handleEditUser = (userData) => {
    // In a real app, this would navigate to edit user page
    console.log('Edit user:', userData);
    logger.userAction('USER_EDIT_CLICKED', { userId: userData.id });
  };

  const openDeleteDialog = (userData) => {
    setSelectedUser(userData);
    setDeleteConfirmDialog(true);
    handleActionMenuClose();
  };

  const handleDeleteUser = async () => {
    if (!selectedUser) return;

    try {
      setLoading(true);
      // 🆕 Používáme User Directory API pro mazání uživatelů
      await apiService.deleteUser(selectedUser.id); // Toto volá /api/users (admin API)
      
      setDeleteConfirmDialog(false);
      setSuccess(`Uživatel ${getDisplayName(selectedUser)} byl úspěšně smazán`);
      
      // Reload data
      await loadUsers();
      
      setTimeout(() => setSuccess(null), 5000);
      
    } catch (err) {
      console.error('Failed to delete user:', err);
      setError('Nepodařilo se smazat uživatele: ' + err.message);
    } finally {
      setLoading(false);
    }
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

  const canEditUser = (userData) => {
    return isAdmin || (isUserManager && userData?.tenant === user?.tenant);
  };

  const canDeleteUser = (userData) => {
    return isAdmin || (isUserManager && userData?.tenant === user?.tenant && userData?.id !== user?.id);
  };

  // 🆕 OPRAVENO: Odebrána kontrola canManageUsers - všichni autentifikovaní uživatelé mají přístup k User Directory
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

      {/* Users Table */}
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
        ) : (
          <>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow sx={{ backgroundColor: 'grey.50' }}>
                    <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Uživatel</TableCell>
                    <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Email</TableCell>
                    {canViewAllTenants && <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Tenant</TableCell>}
                    <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Zdroj</TableCell>
                    <TableCell sx={{ fontWeight: 600, color: 'text.primary' }}>Role</TableCell>
                    <TableCell align="right" sx={{ fontWeight: 600, color: 'text.primary' }}>Akce</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {users.map((userData) => (
                    <TableRow 
                      key={userData.id} 
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
                      onClick={() => handleViewUser(userData)}
                    >
                      <TableCell>
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
                      </TableCell>
                      
                      <TableCell>
                        <Typography variant="body2">
                          {userData?.email || 'N/A'}
                        </Typography>
                      </TableCell>
                      
                      {canViewAllTenants && (
                        <TableCell>
                          <Chip
                            label={userData?.tenantKey || 'Unknown'}
                            size="small"
                            color="primary"
                            variant="outlined"
                            sx={{ borderRadius: 2 }}
                          />
                        </TableCell>
                      )}
                      
                      <TableCell>
                        <Chip
                          icon={userData?.isFederated ? <CloudIcon fontSize="small" /> : <ServerIcon fontSize="small" />}
                          label={userData?.directorySource || (userData?.isFederated ? 'AD' : 'LOCAL')}
                          size="small"
                          color={userData?.isFederated ? 'info' : 'success'}
                          sx={{ borderRadius: 2 }}
                        />
                      </TableCell>
                      
                      <TableCell>
                        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                          {userData?.roles && userData.roles.length > 0 ? (
                            userData.roles.map((role, idx) => (
                              <Chip
                                key={idx}
                                label={role}
                                size="small"
                                color="primary"
                                variant="outlined"
                                sx={{ borderRadius: 1, fontSize: '0.75rem' }}
                              />
                            ))
                          ) : (
                            <Chip
                              label="Žádné role"
                              size="small"
                              variant="outlined"
                              color="default"
                              sx={{ borderRadius: 1, fontSize: '0.75rem' }}
                            />
                          )}
                        </Box>
                      </TableCell>
                      
                      <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                        <Tooltip title="Akce">
                          <IconButton
                            onClick={(e) => handleActionMenuOpen(e, userData)}
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

            {/* Empty State */}
            {users.length === 0 && !loading && (
              <Box textAlign="center" py={6}>
                <PeopleIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
                <Typography variant="h6" color="text.secondary" gutterBottom>
                  Žádní uživatelé nenalezeni
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Zkuste změnit vyhledávací kritéria
                </Typography>
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
        <MenuItem onClick={() => { handleViewUser(selectedUser); handleActionMenuClose(); }}>
          <VisibilityIcon sx={{ mr: 1 }} fontSize="small" />
          Zobrazit
        </MenuItem>
        
        {selectedUser && canEditUser(selectedUser) && (
          <MenuItem onClick={() => { handleEditUser(selectedUser); handleActionMenuClose(); }}>
            <EditIcon sx={{ mr: 1 }} fontSize="small" />
            Upravit
          </MenuItem>
        )}
        
        {selectedUser && canDeleteUser(selectedUser) && (
          <MenuItem 
            onClick={() => openDeleteDialog(selectedUser)}
            sx={{ color: 'error.main' }}
          >
            <DeleteIcon sx={{ mr: 1 }} fontSize="small" />
            Smazat
          </MenuItem>
        )}
      </Menu>

      {/* View User Dialog */}
      <Dialog
        open={viewDialog}
        onClose={() => setViewDialog(false)}
        maxWidth="sm"
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
        <DialogContent sx={{ p: 3 }}>
          {console.log('🔍 Selected user in dialog:', selectedUser)}
          {selectedUser ? (
            <Box>
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

              <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                <Box>
                  <Typography variant="body2" color="text.secondary">Email</Typography>
                  <Typography variant="body1" sx={{ fontWeight: 500 }}>
                    {selectedUser.email || 'Neuvedeno'}
                  </Typography>
                </Box>

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
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>Role</Typography>
                  <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                    {selectedUser.roles && selectedUser.roles.length > 0 ? (
                      selectedUser.roles.map((role, idx) => (
                        <Chip
                          key={idx}
                          label={role}
                          size="small"
                          color="primary"
                          variant="outlined"
                        />
                      ))
                    ) : (
                      <Chip
                        label="Žádné role"
                        size="small"
                        variant="outlined"
                        color="default"
                      />
                    )}
                  </Box>
                </Box>
              </Box>
            </Box>
          ) : (
            <Typography>Načítání...</Typography>
          )}
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <Button
            onClick={() => setViewDialog(false)}
            variant="contained"
            sx={{ borderRadius: 2 }}
          >
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
          Smazat uživatele
        </DialogTitle>
        <DialogContent>
          <Typography>
            Opravdu chcete smazat uživatele <strong>{selectedUser && getDisplayName(selectedUser)}</strong>?
          </Typography>
          {selectedUser?.tenant && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Tenant: {selectedUser.tenant}
            </Typography>
          )}
          <Typography variant="body2" color="error" sx={{ mt: 1 }}>
            Tato akce je nevratná!
          </Typography>
        </DialogContent>
        <DialogActions sx={{ p: 3 }}>
          <Button 
            onClick={() => setDeleteConfirmDialog(false)} 
            disabled={loading}
            sx={{ borderRadius: 2 }}
          >
            Zrušit
          </Button>
          <Button
            color="error"
            variant="contained"
            onClick={handleDeleteUser}
            disabled={loading}
            startIcon={loading ? <CircularProgress size={16} /> : null}
            sx={{ borderRadius: 2 }}
          >
            {loading ? 'Mažu...' : 'Smazat'}
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