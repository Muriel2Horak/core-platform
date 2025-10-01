import { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Grid,
  Typography,
  TextField,
  Button,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
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
  InputAdornment
} from '@mui/material';
import {
  IconSearch,
  IconPlus,
  IconDotsVertical,
  IconEdit,
  IconTrash,
  IconKey,
  IconUserPlus,
  IconRefresh
} from '@tabler/icons-react';
import PageContainer from '../../components/container/PageContainer';
import DashboardCard from '../../components/shared/DashboardCard';
import userManagementService from '../../services/userManagementService';
import authService from '../../services/auth';
import logger from '../../services/logger';

const UserManagementPage = () => {
  // State for users list
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // State for current user permissions
  const [_userInfo, setUserInfo] = useState(null);
  const [hasUserManagerRole, setHasUserManagerRole] = useState(false);

  // State for pagination and filtering
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [searchFilters, setSearchFilters] = useState({
    search: '',
    enabled: '',
    emailVerified: ''
  });

  // State for roles
  const [allRoles, setAllRoles] = useState([]);

  // State for user actions
  const [selectedUser, setSelectedUser] = useState(null);
  const [actionMenuAnchor, setActionMenuAnchor] = useState(null);
  
  // Dialog states
  const [createUserDialog, setCreateUserDialog] = useState(false);
  const [editUserDialog, setEditUserDialog] = useState(false);
  const [deleteUserDialog, setDeleteUserDialog] = useState(false);
  const [resetPasswordDialog, setResetPasswordDialog] = useState(false);
  const [manageRolesDialog, setManageRolesDialog] = useState(false);

  // Form states
  const [userForm, setUserForm] = useState({
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    enabled: true,
    emailVerified: false,
    password: '',
    confirmPassword: ''
  });
  const [formErrors, setFormErrors] = useState({});
  const [saving, setSaving] = useState(false);

  // User roles management
  const [userRoles, setUserRoles] = useState([]);
  const [selectedRole, setSelectedRole] = useState('');

  // Check user permissions on component mount
  useEffect(() => {
    const checkPermissions = async () => {
      try {
        const info = await authService.getUserInfo();
        setUserInfo(info);
        
        // Check if user has CORE_ROLE_USER_MANAGER or CORE_ROLE_ADMIN
        const hasManagerRole = info?.roles?.includes('CORE_ROLE_USER_MANAGER') || 
                               info?.roles?.includes('CORE_ROLE_ADMIN');
        setHasUserManagerRole(hasManagerRole);
        
        if (!hasManagerRole) {
          setError('Nemáte oprávnění pro správu uživatelů. Vyžaduje se role CORE_ROLE_USER_MANAGER nebo CORE_ROLE_ADMIN.');
          logger.security('UNAUTHORIZED_ACCESS_ATTEMPT', 'User tried to access user management without proper role', {
            userRoles: info?.roles || [],
            requiredRole: 'CORE_ROLE_USER_MANAGER',
            username: info?.username
          });
        }
      } catch (err) {
        console.error('Failed to check user permissions:', err);
        setError('Nepodařilo se ověřit oprávnění uživatele');
      }
    };
    
    checkPermissions();
  }, []);

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      logger.pageView('/admin-core/users', { operation: 'load_users', page, filters: searchFilters });
      
      const params = {
        page: page - 1, // Backend uses 0-based pagination
        size: 20,
        ...searchFilters
      };
      
      const data = await userManagementService.searchUsers(params);
      
      // Handle pagination - adjust based on your backend response structure
      if (Array.isArray(data)) {
        setUsers(data);
        setTotalPages(Math.ceil(data.length / 20)); // Fallback calculation
      } else {
        const list = data.content || [];
        setUsers(list);
        setTotalPages(data.totalPages || 1);
      }
      
      const count = Array.isArray(data) ? data.length : (data.content?.length || 0);
      logger.userAction('USERS_LOADED', { count, page });
      
    } catch (err) {
      console.error('Failed to load users:', err);
      setError('Nepodařilo se načíst seznam uživatelů');
      logger.error('USERS_LOAD_ERROR', err.message);
    } finally {
      setLoading(false);
    }
  }, [page, searchFilters]);

  const loadRoles = useCallback(async () => {
    try {
      const roles = await userManagementService.getAllRoles();
      setAllRoles(roles);
    } catch (err) {
      console.error('Failed to load roles:', err);
    }
  }, []);

  useEffect(() => {
    loadUsers();
    loadRoles();
  }, [loadUsers, loadRoles]);

  const handleSearchChange = (field, value) => {
    setSearchFilters(prev => ({ ...prev, [field]: value }));
    setPage(1); // Reset to first page when searching
  };

  const handleActionMenuOpen = (event, user) => {
    setActionMenuAnchor(event.currentTarget);
    setSelectedUser(user);
  };

  const handleActionMenuClose = () => {
    setActionMenuAnchor(null);
    setSelectedUser(null);
  };

  const openCreateUserDialog = () => {
    setUserForm({
      username: '',
      email: '',
      firstName: '',
      lastName: '',
      enabled: true,
      emailVerified: false,
      password: '',
      confirmPassword: ''
    });
    setFormErrors({});
    setCreateUserDialog(true);
    logger.userAction('CREATE_USER_DIALOG_OPENED');
  };

  const openEditUserDialog = (user) => {
    setSelectedUser(user);
    setUserForm({
      username: user.username,
      email: user.email || '',
      firstName: user.firstName || '',
      lastName: user.lastName || '',
      enabled: user.enabled,
      emailVerified: user.emailVerified,
      password: '',
      confirmPassword: ''
    });
    setFormErrors({});
    setEditUserDialog(true);
    handleActionMenuClose();
    logger.userAction('EDIT_USER_DIALOG_OPENED', { userId: user.id });
  };

  const openDeleteUserDialog = (user) => {
    setSelectedUser(user);
    setDeleteUserDialog(true);
    handleActionMenuClose();
    logger.userAction('DELETE_USER_DIALOG_OPENED', { userId: user.id });
  };

  const openResetPasswordDialog = (user) => {
    setSelectedUser(user);
    setUserForm({ password: '', confirmPassword: '' });
    setFormErrors({});
    setResetPasswordDialog(true);
    handleActionMenuClose();
    logger.userAction('RESET_PASSWORD_DIALOG_OPENED', { userId: user.id });
  };

  const openManageRolesDialog = async (user) => {
    setSelectedUser(user);
    setSelectedRole('');
    try {
      const roles = await userManagementService.getUserRoles(user.id);
      setUserRoles(roles);
    } catch (err) {
      console.error('Failed to load user roles:', err);
      setUserRoles([]);
    }
    setManageRolesDialog(true);
    handleActionMenuClose();
    logger.userAction('MANAGE_ROLES_DIALOG_OPENED', { userId: user.id });
  };

  const validateUserForm = (isCreate = false) => {
    const errors = {};
    
    if (!userForm.username?.trim()) {
      errors.username = 'Uživatelské jméno je povinné';
    }
    
    if (!userForm.email?.trim()) {
      errors.email = 'Email je povinný';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(userForm.email)) {
      errors.email = 'Neplatný formát emailu';
    }
    
    if (isCreate) {
      if (!userForm.password) {
        errors.password = 'Heslo je povinné';
      } else if (userForm.password.length < 8) {
        errors.password = 'Heslo musí mít alespoň 8 znaků';
      }
      
      if (userForm.password !== userForm.confirmPassword) {
        errors.confirmPassword = 'Hesla se neshodují';
      }
    }
    
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreateUser = async () => {
    if (!validateUserForm(true)) return;
    
    try {
      setSaving(true);
      setError(null);
      
      const userData = {
        username: userForm.username,
        email: userForm.email,
        firstName: userForm.firstName,
        lastName: userForm.lastName,
        enabled: userForm.enabled,
        emailVerified: userForm.emailVerified,
        password: userForm.password
      };
      
      await userManagementService.createUser(userData);
      
      setCreateUserDialog(false);
      setSuccess('Uživatel byl úspěšně vytvořen');
      loadUsers(); // Reload users list
      
      setTimeout(() => setSuccess(null), 3000);
      
    } catch (err) {
      console.error('Failed to create user:', err);
      setError('Nepodařilo se vytvořit uživatele: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleUpdateUser = async () => {
    if (!validateUserForm(false)) return;
    
    try {
      setSaving(true);
      setError(null);
      
      const userData = {
        email: userForm.email,
        firstName: userForm.firstName,
        lastName: userForm.lastName,
        enabled: userForm.enabled,
        emailVerified: userForm.emailVerified
      };
      
      await userManagementService.updateUser(selectedUser.id, userData);
      
      setEditUserDialog(false);
      setSuccess('Uživatel byl úspěšně aktualizován');
      loadUsers(); // Reload users list
      
      setTimeout(() => setSuccess(null), 3000);
      
    } catch (err) {
      console.error('Failed to update user:', err);
      setError('Nepodařilo se aktualizovat uživatele: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteUser = async () => {
    try {
      setSaving(true);
      setError(null);
      
      await userManagementService.deleteUser(selectedUser.id);
      
      setDeleteUserDialog(false);
      setSuccess('Uživatel byl úspěšně smazán');
      loadUsers(); // Reload users list
      
      setTimeout(() => setSuccess(null), 3000);
      
    } catch (err) {
      console.error('Failed to delete user:', err);
      setError('Nepodařilo se smazat uživatele: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleResetPassword = async () => {
    if (!userForm.password || userForm.password !== userForm.confirmPassword) {
      setFormErrors({
        password: !userForm.password ? 'Heslo je povinné' : '',
        confirmPassword: userForm.password !== userForm.confirmPassword ? 'Hesla se neshodují' : ''
      });
      return;
    }
    
    if (userForm.password.length < 8) {
      setFormErrors({ password: 'Heslo musí mít alespoň 8 znaků' });
      return;
    }
    
    try {
      setSaving(true);
      setError(null);
      
      await userManagementService.resetUserPassword(selectedUser.id, {
        newPassword: userForm.password,
        temporary: false
      });
      
      setResetPasswordDialog(false);
      setSuccess('Heslo bylo úspěšně resetováno');
      
      setTimeout(() => setSuccess(null), 3000);
      
    } catch (err) {
      console.error('Failed to reset password:', err);
      setError('Nepodařilo se resetovat heslo: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleAssignRole = async () => {
    if (!selectedRole) return;
    
    try {
      setSaving(true);
      
      await userManagementService.assignRoleToUser(selectedUser.id, selectedRole);
      
      // Reload user roles
      const roles = await userManagementService.getUserRoles(selectedUser.id);
      setUserRoles(roles);
      setSelectedRole('');
      setSuccess('Role byla úspěšně přiřazena');
      
      setTimeout(() => setSuccess(null), 3000);
      
    } catch (err) {
      console.error('Failed to assign role:', err);
      setError('Nepodařilo se přiřadit roli: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const handleRemoveRole = async (roleName) => {
    try {
      setSaving(true);
      
      await userManagementService.removeRoleFromUser(selectedUser.id, roleName);
      
      // Reload user roles
      const roles = await userManagementService.getUserRoles(selectedUser.id);
      setUserRoles(roles);
      setSuccess('Role byla úspěšně odebrána');
      
      setTimeout(() => setSuccess(null), 3000);
      
    } catch (err) {
      console.error('Failed to remove role:', err);
      setError('Nepodařilo se odebrat roli: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const getDisplayName = (user) => {
    if (user.firstName && user.lastName) {
      return `${user.firstName} ${user.lastName}`;
    }
    return user.username || user.email || 'N/A';
  };

  const getInitials = (user) => {
    if (user.firstName && user.lastName) {
      return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
    }
    if (user.username) {
      return user.username.substring(0, 2).toUpperCase();
    }
    return '?';
  };

  return (
    <PageContainer title="Správa uživatelů" description="Administrace uživatelských účtů">
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

        {/* Filters and Actions */}
        <Grid item xs={12}>
          <Card>
            <CardContent>
              <Grid container spacing={2} alignItems="center">
                <Grid item xs={12} sm={6} md={4}>
                  <TextField
                    fullWidth
                    placeholder="Hledat uživatele..."
                    value={searchFilters.search}
                    onChange={(e) => handleSearchChange('search', e.target.value)}
                    InputProps={{
                      startAdornment: (
                        <InputAdornment position="start">
                          <IconSearch size={20} />
                        </InputAdornment>
                      )
                    }}
                  />
                </Grid>
                
                <Grid item xs={12} sm={3} md={2}>
                  <FormControl fullWidth>
                    <InputLabel>Status</InputLabel>
                    <Select
                      value={searchFilters.enabled}
                      onChange={(e) => handleSearchChange('enabled', e.target.value)}
                      label="Status"
                    >
                      <MenuItem value="">Všechny</MenuItem>
                      <MenuItem value="true">Aktivní</MenuItem>
                      <MenuItem value="false">Neaktivní</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                
                <Grid item xs={12} sm={3} md={2}>
                  <FormControl fullWidth>
                    <InputLabel>Email ověřen</InputLabel>
                    <Select
                      value={searchFilters.emailVerified}
                      onChange={(e) => handleSearchChange('emailVerified', e.target.value)}
                      label="Email ověřen"
                    >
                      <MenuItem value="">Všechny</MenuItem>
                      <MenuItem value="true">Ověřeno</MenuItem>
                      <MenuItem value="false">Neověřeno</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                
                <Grid item xs={12} sm={12} md={4}>
                  <Box display="flex" gap={1} justifyContent="flex-end">
                    <Button
                      variant="outlined"
                      startIcon={<IconRefresh />}
                      onClick={loadUsers}
                      disabled={loading || !hasUserManagerRole}
                    >
                      Obnovit
                    </Button>
                    {hasUserManagerRole && (
                      <Button
                        variant="contained"
                        startIcon={<IconPlus />}
                        onClick={openCreateUserDialog}
                      >
                        Nový uživatel
                      </Button>
                    )}
                  </Box>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        </Grid>

        {/* Users Table */}
        <Grid item xs={12}>
          <DashboardCard title="Seznam uživatelů">
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
                        <TableCell>Status</TableCell>
                        <TableCell>Email ověřen</TableCell>
                        <TableCell>Role</TableCell>
                        <TableCell>Registrace</TableCell>
                        <TableCell align="right">Akce</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {users.map((user) => (
                        <TableRow key={user.id} hover>
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
                                  {getDisplayName(user)}
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
                          
                          <TableCell>
                            <Chip
                              label={user.enabled ? 'Aktivní' : 'Neaktivní'}
                              color={user.enabled ? 'success' : 'error'}
                              size="small"
                            />
                          </TableCell>
                          
                          <TableCell>
                            <Chip
                              label={user.emailVerified ? 'Ověřeno' : 'Neověřeno'}
                              color={user.emailVerified ? 'success' : 'warning'}
                              size="small"
                            />
                          </TableCell>
                          
                          <TableCell>
                            <Box display="flex" gap={0.5} flexWrap="wrap">
                              {user.roles?.slice(0, 2).map((role) => (
                                <Chip
                                  key={role}
                                  label={role}
                                  size="small"
                                  variant="outlined"
                                />
                              ))}
                              {user.roles?.length > 2 && (
                                <Chip
                                  label={`+${user.roles.length - 2}`}
                                  size="small"
                                  variant="outlined"
                                />
                              )}
                            </Box>
                          </TableCell>
                          
                          <TableCell>
                            {user.createdTimestamp
                              ? new Date(user.createdTimestamp).toLocaleDateString('cs-CZ')
                              : 'N/A'
                            }
                          </TableCell>
                          
                          <TableCell align="right">
                            {hasUserManagerRole ? (
                              <Tooltip title="Akce">
                                <IconButton
                                  onClick={(e) => handleActionMenuOpen(e, user)}
                                >
                                  <IconDotsVertical size={20} />
                                </IconButton>
                              </Tooltip>
                            ) : (
                              <Tooltip title="Nemáte oprávnění pro správu uživatelů">
                                <span>
                                  <IconButton disabled>
                                    <IconDotsVertical size={20} />
                                  </IconButton>
                                </span>
                              </Tooltip>
                            )}
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
                      page={page}
                      onChange={(e, newPage) => setPage(newPage)}
                      color="primary"
                    />
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
        <MenuItem onClick={() => openEditUserDialog(selectedUser)}>
          <IconEdit size={16} style={{ marginRight: 8 }} />
          Upravit
        </MenuItem>
        <MenuItem onClick={() => openManageRolesDialog(selectedUser)}>
          <IconUserPlus size={16} style={{ marginRight: 8 }} />
          Spravovat role
        </MenuItem>
        <MenuItem onClick={() => openResetPasswordDialog(selectedUser)}>
          <IconKey size={16} style={{ marginRight: 8 }} />
          Resetovat heslo
        </MenuItem>
        <MenuItem 
          onClick={() => openDeleteUserDialog(selectedUser)}
          sx={{ color: 'error.main' }}
        >
          <IconTrash size={16} style={{ marginRight: 8 }} />
          Smazat
        </MenuItem>
      </Menu>

      {/* Create User Dialog */}
      <Dialog open={createUserDialog} onClose={() => setCreateUserDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Nový uživatel</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Uživatelské jméno *"
                value={userForm.username}
                onChange={(e) => setUserForm(prev => ({ ...prev, username: e.target.value }))}
                error={!!formErrors.username}
                helperText={formErrors.username}
                required
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Email *"
                type="email"
                value={userForm.email}
                onChange={(e) => setUserForm(prev => ({ ...prev, email: e.target.value }))}
                error={!!formErrors.email}
                helperText={formErrors.email}
                required
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Jméno"
                value={userForm.firstName}
                onChange={(e) => setUserForm(prev => ({ ...prev, firstName: e.target.value }))}
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Příjmení"
                value={userForm.lastName}
                onChange={(e) => setUserForm(prev => ({ ...prev, lastName: e.target.value }))}
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Heslo *"
                type="password"
                value={userForm.password}
                onChange={(e) => setUserForm(prev => ({ ...prev, password: e.target.value }))}
                error={!!formErrors.password}
                helperText={formErrors.password || 'Alespoň 8 znaků'}
                required
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Potvrzení hesla *"
                type="password"
                value={userForm.confirmPassword}
                onChange={(e) => setUserForm(prev => ({ ...prev, confirmPassword: e.target.value }))}
                error={!!formErrors.confirmPassword}
                helperText={formErrors.confirmPassword}
                required
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={userForm.enabled}
                  onChange={(e) => setUserForm(prev => ({ ...prev, enabled: e.target.value }))}
                  label="Status"
                >
                  <MenuItem value={true}>Aktivní</MenuItem>
                  <MenuItem value={false}>Neaktivní</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Email ověřen</InputLabel>
                <Select
                  value={userForm.emailVerified}
                  onChange={(e) => setUserForm(prev => ({ ...prev, emailVerified: e.target.value }))}
                  label="Email ověřen"
                >
                  <MenuItem value={true}>Ověřeno</MenuItem>
                  <MenuItem value={false}>Neověřeno</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCreateUserDialog(false)} disabled={saving}>
            Zrušit
          </Button>
          <Button
            variant="contained"
            onClick={handleCreateUser}
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : null}
          >
            {saving ? 'Vytvářím...' : 'Vytvořit uživatele'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Edit User Dialog */}
      <Dialog open={editUserDialog} onClose={() => setEditUserDialog(false)} maxWidth="md" fullWidth>
        <DialogTitle>Upravit uživatele</DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Uživatelské jméno"
                value={userForm.username}
                disabled
                helperText="Uživatelské jméno nelze změnit"
              />
            </Grid>
            
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Email *"
                type="email"
                value={userForm.email}
                onChange={(e) => setUserForm(prev => ({ ...prev, email: e.target.value }))}
                error={!!formErrors.email}
                helperText={formErrors.email}
                required
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Jméno"
                value={userForm.firstName}
                onChange={(e) => setUserForm(prev => ({ ...prev, firstName: e.target.value }))}
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Příjmení"
                value={userForm.lastName}
                onChange={(e) => setUserForm(prev => ({ ...prev, lastName: e.target.value }))}
              />
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  value={userForm.enabled}
                  onChange={(e) => setUserForm(prev => ({ ...prev, enabled: e.target.value }))}
                  label="Status"
                >
                  <MenuItem value={true}>Aktivní</MenuItem>
                  <MenuItem value={false}>Neaktivní</MenuItem>
                </Select>
              </FormControl>
            </Grid>
            
            <Grid item xs={12} sm={6}>
              <FormControl fullWidth>
                <InputLabel>Email ověřen</InputLabel>
                <Select
                  value={userForm.emailVerified}
                  onChange={(e) => setUserForm(prev => ({ ...prev, emailVerified: e.target.value }))}
                  label="Email ověřen"
                >
                  <MenuItem value={true}>Ověřeno</MenuItem>
                  <MenuItem value={false}>Neověřeno</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditUserDialog(false)} disabled={saving}>
            Zrušit
          </Button>
          <Button
            variant="contained"
            onClick={handleUpdateUser}
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : null}
          >
            {saving ? 'Ukládám...' : 'Uložit změny'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Delete User Dialog */}
      <Dialog open={deleteUserDialog} onClose={() => setDeleteUserDialog(false)}>
        <DialogTitle>Smazat uživatele</DialogTitle>
        <DialogContent>
          <Typography>
            Opravdu chcete smazat uživatele <strong>{selectedUser?.username}</strong>?
          </Typography>
          <Typography variant="body2" color="error" sx={{ mt: 1 }}>
            Tato akce je nevratná!
          </Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteUserDialog(false)} disabled={saving}>
            Zrušit
          </Button>
          <Button
            color="error"
            variant="contained"
            onClick={handleDeleteUser}
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : null}
          >
            {saving ? 'Mažu...' : 'Smazat'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Reset Password Dialog */}
      <Dialog open={resetPasswordDialog} onClose={() => setResetPasswordDialog(false)}>
        <DialogTitle>Resetovat heslo</DialogTitle>
        <DialogContent>
          <Typography gutterBottom>
            Resetovat heslo pro uživatele <strong>{selectedUser?.username}</strong>
          </Typography>
          
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Nové heslo"
                type="password"
                value={userForm.password}
                onChange={(e) => setUserForm(prev => ({ ...prev, password: e.target.value }))}
                error={!!formErrors.password}
                helperText={formErrors.password || 'Alespoň 8 znaků'}
                required
              />
            </Grid>
            
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Potvrzení hesla"
                type="password"
                value={userForm.confirmPassword}
                onChange={(e) => setUserForm(prev => ({ ...prev, confirmPassword: e.target.value }))}
                error={!!formErrors.confirmPassword}
                helperText={formErrors.confirmPassword}
                required
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setResetPasswordDialog(false)} disabled={saving}>
            Zrušit
          </Button>
          <Button
            variant="contained"
            onClick={handleResetPassword}
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : null}
          >
            {saving ? 'Resetuji...' : 'Resetovat heslo'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Manage Roles Dialog */}
      <Dialog open={manageRolesDialog} onClose={() => setManageRolesDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Spravovat role - {selectedUser?.username}</DialogTitle>
        <DialogContent>
          {/* Current roles */}
          <Typography variant="subtitle1" gutterBottom>
            Aktuální role:
          </Typography>
          
          <Box display="flex" gap={1} flexWrap="wrap" mb={3}>
            {userRoles.length > 0 ? (
              userRoles.map((role) => (
                <Chip
                  key={role.name}
                  label={role.name}
                  onDelete={() => handleRemoveRole(role.name)}
                  color="primary"
                />
              ))
            ) : (
              <Typography variant="body2" color="text.secondary">
                Žádné role přiřazeny
              </Typography>
            )}
          </Box>
          
          {/* Add role */}
          <Typography variant="subtitle1" gutterBottom>
            Přidat roli:
          </Typography>
          
          <Box display="flex" gap={2} alignItems="center">
            <FormControl sx={{ flex: 1 }}>
              <InputLabel>Vyberte roli</InputLabel>
              <Select
                value={selectedRole}
                onChange={(e) => setSelectedRole(e.target.value)}
                label="Vyberte roli"
              >
                {allRoles
                  .filter(role => !userRoles.some(userRole => userRole.name === role.name))
                  .map((role) => (
                    <MenuItem key={role.name} value={role.name}>
                      {role.name}
                    </MenuItem>
                  ))
                }
              </Select>
            </FormControl>
            
            <Button
              variant="contained"
              onClick={handleAssignRole}
              disabled={!selectedRole || saving}
              startIcon={<IconUserPlus size={16} />}
            >
              Přidat
            </Button>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setManageRolesDialog(false)}>
            Zavřít
          </Button>
        </DialogActions>
      </Dialog>
    </PageContainer>
  );
};

export default UserManagementPage;