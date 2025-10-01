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
  DialogActions
} from '@mui/material';
import {
  IconSearch,
  IconEye,
  IconEdit,
  IconTrash,
  IconDotsVertical,
  IconRefresh,
  IconUsersGroup
} from '@tabler/icons-react';
import PageContainer from '../../components/container/PageContainer';
import DashboardCard from '../../components/shared/DashboardCard';
import usersDirectoryService from '../../services/usersDirectoryService';
import { useUserInfo } from '../../hooks/useUserInfo';
import logger from '../../services/logger';

const UserDirectoryPage = () => {
  // State for data
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // State for pagination
  const [page, setPage] = useState(0); // Backend uses 0-based pagination
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // State for filters
  const [searchFilters, setSearchFilters] = useState({
    q: '',
    source: '',
    tenantId: ''
  });
  const [debouncedQuery, setDebouncedQuery] = useState('');

  // State for tenants (core-admin filter)
  const [tenants, setTenants] = useState([]);
  const [tenantsLoading, setTenantsLoading] = useState(false);

  // User info and permissions
  const { userInfo, isAdmin: _isAdmin } = useUserInfo();
  const _isCoreAdmin = userInfo?.roles?.includes('CORE_ROLE_ADMIN');
  const canViewAllTenants = usersDirectoryService.canViewAllTenants(userInfo);

  // State for user actions
  const [selectedUser, setSelectedUser] = useState(null);
  const [actionMenuAnchor, setActionMenuAnchor] = useState(null);
  const [deleteConfirmDialog, setDeleteConfirmDialog] = useState(false);

  // Debounce search query
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedQuery(searchFilters.q);
    }, 400);

    return () => clearTimeout(timer);
  }, [searchFilters.q]);

  // Load data when filters change
  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  // Load tenants for core-admin
  useEffect(() => {
    if (canViewAllTenants) {
      loadTenants();
    }
  }, [canViewAllTenants, loadTenants]);

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      logger.pageView('/directory', { 
        operation: 'load_directory', 
        page, 
        filters: searchFilters 
      });
      
      const options = {
        q: debouncedQuery,
        source: searchFilters.source,
        page,
        size: 20,
        sort: 'username'
      };

      // Add tenant filter for core-admin
      if (canViewAllTenants && searchFilters.tenantId) {
        options.tenantId = searchFilters.tenantId;
      }
      
      const data = await usersDirectoryService.searchDirectory(options);
      
      setUsers(data.content || []);
      setTotalPages(data.totalPages || 1);
      setTotalElements(data.totalElements || 0);
      
      logger.userAction('DIRECTORY_LOADED', { 
        count: data.content?.length || 0, 
        page,
        totalElements: data.totalElements
      });
      
    } catch (err) {
      console.error('Failed to load directory:', err);
      setError('Nepodařilo se načíst directory: ' + err.message);
      logger.error('DIRECTORY_LOAD_ERROR', err.message);
    } finally {
      setLoading(false);
    }
  }, [page, debouncedQuery, canViewAllTenants, searchFilters]);

  const loadTenants = useCallback(async () => {
    try {
      setTenantsLoading(true);
      const data = await usersDirectoryService.getAllTenants();
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

  const handleActionMenuOpen = (event, user) => {
    setActionMenuAnchor(event.currentTarget);
    setSelectedUser(user);
  };

  const handleActionMenuClose = () => {
    setActionMenuAnchor(null);
    setSelectedUser(null);
  };

  const handleViewUser = (user) => {
    // Navigate to user detail/profile page
    window.location.href = `/directory/${user.id}`;
  };

  const handleEditUser = (user) => {
    // Navigate to edit mode
    window.location.href = `/directory/${user.id}?edit=true`;
  };

  const openDeleteDialog = (user) => {
    setSelectedUser(user);
    setDeleteConfirmDialog(true);
    handleActionMenuClose();
  };

  const handleDeleteUser = async () => {
    if (!selectedUser) return;

    try {
      setLoading(true);
      await usersDirectoryService.deleteUser(selectedUser.id);
      
      setDeleteConfirmDialog(false);
      setSuccess(`Uživatel ${usersDirectoryService.getDisplayName(selectedUser)} byl úspěšně smazán`);
      
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

  const getInitials = (user) => {
    const displayName = usersDirectoryService.getDisplayName(user);
    const parts = displayName.split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return displayName.substring(0, 2).toUpperCase();
  };

  const canEditUser = (user) => {
    return usersDirectoryService.canEditUser(user, userInfo);
  };

  const canDeleteUser = (user) => {
    return usersDirectoryService.canDeleteUser(user, userInfo);
  };

  return (
    <PageContainer title="User Directory" description="Firemní adresář uživatelů">
      <Grid container spacing={3}>
        
        {/* Success/Error Messages */}
        {success && (
          <Grid item xs={12}>
            <Alert severity="success" onClose={() => setSuccess(null)}>
              {success}
            </Alert>
          </Grid>
        )}
        
        {error && (
          <Grid item xs={12}>
            <Alert severity="error" onClose={() => setError(null)}>
              {error}
            </Alert>
          </Grid>
        )}

        {/* Header & Filters */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Box sx={{ mb: 2 }}>
                <Typography variant="h5" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <IconUsersGroup size={24} />
                  User Directory
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Vyhledejte a zobrazte informace o uživatelích ve vaší organizaci
                </Typography>
              </Box>

              <Grid container spacing={2} alignItems="center">
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
                          <IconSearch size={20} />
                        </InputAdornment>
                      )
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
                    >
                      <MenuItem value="">Všechny</MenuItem>
                      <MenuItem value="AD">AD</MenuItem>
                      <MenuItem value="LOCAL">Local</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>

                {/* Tenant Filter (pouze pro core-admin) */}
                {canViewAllTenants && (
                  <Grid item xs={12} sm={3} md={3}>
                    <FormControl fullWidth>
                      <InputLabel>Tenant</InputLabel>
                      <Select
                        value={searchFilters.tenantId}
                        onChange={(e) => handleSearchChange('tenantId', e.target.value)}
                        label="Tenant"
                        disabled={tenantsLoading}
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
                      startIcon={<IconRefresh />}
                      onClick={loadUsers}
                      disabled={loading}
                    >
                      Obnovit
                    </Button>
                  </Box>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Users Table */}
        <Grid item xs={12}>
          <DashboardCard title={`Seznam uživatelů (${totalElements})`}>
            {loading ? (
              <Box display="flex" justifyContent="center" p={4}>
                <CircularProgress />
              </Box>
            ) : (
              <>
                <TableContainer>
                  <Table>
                    <TableHead>
                      <TableRow>
                        <TableCell>Uživatel</TableCell>
                        <TableCell>Email</TableCell>
                        {canViewAllTenants && <TableCell>Tenant</TableCell>}
                        <TableCell>Zdroj</TableCell>
                        <TableCell>Aktualizace</TableCell>
                        <TableCell align="right">Akce</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {users.map((user) => (
                        <TableRow 
                          key={user.id} 
                          hover 
                          sx={{ cursor: 'pointer' }}
                          onClick={() => handleViewUser(user)}
                        >
                          <TableCell>
                            <Box display="flex" alignItems="center" gap={2}>
                              <Avatar
                                sx={{
                                  width: 40,
                                  height: 40,
                                  backgroundColor: 'primary.main'
                                }}
                              >
                                {getInitials(user)}
                              </Avatar>
                              <Box>
                                <Typography variant="subtitle2">
                                  {usersDirectoryService.getDisplayName(user)}
                                </Typography>
                                <Typography variant="body2" color="text.secondary">
                                  @{user.username}
                                </Typography>
                              </Box>
                            </Box>
                          </TableCell>
                          
                          <TableCell>
                            {user.email || 'N/A'}
                          </TableCell>
                          
                          {canViewAllTenants && (
                            <TableCell>
                              <Chip
                                label={user.tenantName || 'Unknown'}
                                size="small"
                                color="primary"
                                variant="outlined"
                              />
                            </TableCell>
                          )}
                          
                          <TableCell>
                            <Chip
                              label={usersDirectoryService.getDirectorySourceBadge(user)}
                              size="small"
                              color={user.directorySource === 'AD' || user.isFederated ? 'info' : 'default'}
                            />
                          </TableCell>
                          
                          <TableCell>
                            {user.updatedAt 
                              ? new Date(user.updatedAt).toLocaleDateString('cs-CZ')
                              : 'N/A'
                            }
                          </TableCell>
                          
                          <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                            <Tooltip title="Akce">
                              <IconButton
                                onClick={(e) => handleActionMenuOpen(e, user)}
                              >
                                <IconDotsVertical size={20} />
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
                  <Box display="flex" justifyContent="center" mt={2}>
                    <Pagination
                      count={totalPages}
                      page={page + 1} // MUI uses 1-based pagination
                      onChange={(e, newPage) => setPage(newPage - 1)}
                      color="primary"
                    />
                  </Box>
                )}

                {/* Empty State */}
                {users.length === 0 && !loading && (
                  <Box textAlign="center" py={4}>
                    <IconUsersGroup size={48} color="#ccc" />
                    <Typography variant="h6" color="text.secondary" mt={2}>
                      Žádní uživatelé nenalezeni
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      Zkuste změnit vyhledávací kritéria
                    </Typography>
                  </Box>
                )}
              </>
            )}
          </DashboardCard>
        </Grid>
      </Grid>

      {/* Action Menu */}
      <Menu
        anchorEl={actionMenuAnchor}
        open={Boolean(actionMenuAnchor)}
        onClose={handleActionMenuClose}
      >
        <MenuItem onClick={() => { handleViewUser(selectedUser); handleActionMenuClose(); }}>
          <IconEye size={16} style={{ marginRight: 8 }} />
          Zobrazit
        </MenuItem>
        
        {selectedUser && canEditUser(selectedUser) && (
          <MenuItem onClick={() => { handleEditUser(selectedUser); handleActionMenuClose(); }}>
            <IconEdit size={16} style={{ marginRight: 8 }} />
            Upravit
          </MenuItem>
        )}
        
        {selectedUser && canDeleteUser(selectedUser) && (
          <MenuItem 
            onClick={() => openDeleteDialog(selectedUser)}
            sx={{ color: 'error.main' }}
          >
            <IconTrash size={16} style={{ marginRight: 8 }} />
            Smazat
          </MenuItem>
        )}
      </Menu>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteConfirmDialog} onClose={() => setDeleteConfirmDialog(false)}>
        <DialogTitle>Smazat uživatele</DialogTitle>
        <DialogContent>
          <Typography>
            Opravdu chcete smazat uživatele <strong>{selectedUser && usersDirectoryService.getDisplayName(selectedUser)}</strong>?
          </Typography>
          {selectedUser?.tenantName && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
              Tenant: {selectedUser.tenantName}
            </Typography>
          )}
          <Typography variant="body2" color="error" sx={{ mt: 1 }}>
            Tato akce je nevratná!
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteConfirmDialog(false)} disabled={loading}>
            Zrušit
          </Button>
          <Button
            color="error"
            variant="contained"
            onClick={handleDeleteUser}
            disabled={loading}
            startIcon={loading ? <CircularProgress size={16} /> : null}
          >
            {loading ? 'Mažu...' : 'Smazat'}
          </Button>
        </DialogActions>
      </Dialog>
    </PageContainer>
  );
};

export default UserDirectoryPage;