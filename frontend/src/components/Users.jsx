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
  Avatar,
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
  People as PeopleIcon,
  Add as AddIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  LockReset as LockResetIcon,
  Search as SearchIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';
import { UserPropType } from '../shared/propTypes.js';
import {
  CreateUserDialog,
  EditUserDialog,
  DeleteUserDialog,
  ResetPasswordDialog,
} from './Users/index.js';

function Users({ user }) {
  const [users, setUsers] = useState([]);
  const [filteredUsers, setFilteredUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Dialog states
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [resetPasswordDialogOpen, setResetPasswordDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState(null);

  // Menu state
  const [menuAnchor, setMenuAnchor] = useState(null);
  const [menuUser, setMenuUser] = useState(null);

  // Check if user has permission to view users
  const hasPermission = user?.roles?.includes('CORE_ROLE_USER_MANAGER') || user?.roles?.includes('CORE_ROLE_ADMIN');
  const canManageUsers = user?.roles?.includes('CORE_ROLE_ADMIN');

  // Load users
  const loadUsers = async () => {
    if (!hasPermission) {
      setError('Nemáte oprávnění k zobrazení seznamu uživatelů.');
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      
      const usersData = await apiService.getUsers();
      setUsers(usersData || []);
      setFilteredUsers(usersData || []);
      logger.info('Users loaded', { count: usersData?.length || 0 });
    } catch (error) {
      logger.error('Failed to load users', { error: error.message });
      setError('Nepodařilo se načíst seznam uživatelů.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, []);

  // Search/filter users
  useEffect(() => {
    if (!searchQuery) {
      setFilteredUsers(users);
      return;
    }

    const query = searchQuery.toLowerCase();
    const filtered = users.filter(u => 
      u.username?.toLowerCase().includes(query) ||
      u.email?.toLowerCase().includes(query) ||
      u.firstName?.toLowerCase().includes(query) ||
      u.lastName?.toLowerCase().includes(query)
    );
    setFilteredUsers(filtered);
  }, [searchQuery, users]);

  // Get user display name
  const getUserDisplayName = (userData) => {
    if (userData?.firstName && userData?.lastName) {
      return `${userData.firstName} ${userData.lastName}`;
    }
    return userData?.username || userData?.email || 'Neznámý uživatel';
  };

  // Get user initials for avatar
  const getUserInitials = (userData) => {
    const name = getUserDisplayName(userData);
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  // Get user roles display
  const getUserRoles = (userData) => {
    if (!userData?.roles || userData.roles.length === 0) {
      return ['Žádné role'];
    }
    
    // Zobrazíme role přímo jak jsou, bez mappingu
    return userData.roles;
  };

  // Action handlers
  const handleMenuOpen = (event, userData) => {
    setMenuAnchor(event.currentTarget);
    setMenuUser(userData);
  };

  const handleMenuClose = () => {
    setMenuAnchor(null);
    setMenuUser(null);
  };

  const handleCreateUser = () => {
    setCreateDialogOpen(true);
  };

  const handleEditUser = (userData) => {
    setSelectedUser(userData);
    setEditDialogOpen(true);
    handleMenuClose();
  };

  const handleDeleteUser = (userData) => {
    setSelectedUser(userData);
    setDeleteDialogOpen(true);
    handleMenuClose();
  };

  const handleResetPassword = (userData) => {
    setSelectedUser(userData);
    setResetPasswordDialogOpen(true);
    handleMenuClose();
  };

  const handleRefresh = () => {
    loadUsers();
  };

  // Success handlers
  const handleUserCreated = () => {
    setCreateDialogOpen(false);
    loadUsers();
  };

  const handleUserUpdated = () => {
    setEditDialogOpen(false);
    setSelectedUser(null);
    loadUsers();
  };

  const handleUserDeleted = () => {
    setDeleteDialogOpen(false);
    setSelectedUser(null);
    loadUsers();
  };

  const handlePasswordReset = () => {
    setResetPasswordDialogOpen(false);
    setSelectedUser(null);
  };

  if (!hasPermission) {
    return (
      <Box>
        <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600 }}>
          Uživatelé
        </Typography>
        
        <Alert severity="error">
          Nemáte oprávnění k zobrazení seznamu uživatelů.
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
            Správa uživatelů
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Zde můžete spravovat uživatele v systému.
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="Obnovit seznam">
            <IconButton onClick={handleRefresh} disabled={loading}>
              <RefreshIcon />
            </IconButton>
          </Tooltip>
          {canManageUsers && (
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={handleCreateUser}
              disabled={loading}
            >
              Vytvořit uživatele
            </Button>
          )}
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
              placeholder="Hledat uživatele..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
              disabled={loading || users.length === 0}
            />
          </Box>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
              <CircularProgress />
            </Box>
          ) : filteredUsers.length === 0 ? (
            <Box sx={{ textAlign: 'center', p: 3 }}>
              <PeopleIcon sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
              <Typography variant="h6" color="text.secondary">
                {searchQuery ? 'Nebyli nalezeni žádní uživatelé odpovídající vyhledávání' : 'Nebyli nalezeni žádní uživatelé'}
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
                      <TableCell>Uživatel</TableCell>
                      <TableCell>Email</TableCell>
                      <TableCell>Tenant</TableCell>
                      <TableCell>Role</TableCell>
                      <TableCell>Stav</TableCell>
                      {canManageUsers && <TableCell align="right">Akce</TableCell>}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {filteredUsers.map((userData, index) => (
                      <TableRow 
                        key={userData.id || index} 
                        hover
                        onClick={() => canManageUsers && handleEditUser(userData)}
                        sx={{ cursor: canManageUsers ? 'pointer' : 'default' }}
                      >
                        <TableCell>
                          <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <Avatar sx={{ bgcolor: 'primary.main', mr: 2, width: 40, height: 40 }}>
                              {getUserInitials(userData)}
                            </Avatar>
                            <Box>
                              <Typography variant="body1" sx={{ fontWeight: 500 }}>
                                {getUserDisplayName(userData)}
                              </Typography>
                              <Typography variant="body2" color="text.secondary">
                                {userData?.username}
                              </Typography>
                            </Box>
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {userData?.email || 'Neuvedeno'}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={userData?.tenantName || userData?.tenantKey || 'Neuvedeno'}
                            size="small"
                            variant="outlined"
                            color="primary"
                          />
                        </TableCell>
                        <TableCell>
                          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
                            {getUserRoles(userData).map((role, roleIndex) => (
                              <Chip
                                key={roleIndex}
                                label={role}
                                size="small"
                                color="primary"
                                variant="outlined"
                              />
                            ))}
                          </Box>
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={userData?.enabled ? 'Aktivní' : 'Neaktivní'}
                            size="small"
                            color={userData?.enabled ? 'success' : 'default'}
                          />
                        </TableCell>
                        {canManageUsers && (
                          <TableCell align="right">
                            <IconButton
                              size="small"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleMenuOpen(e, userData);
                              }}
                            >
                              <MoreVertIcon />
                            </IconButton>
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>

              {/* Results count */}
              <Box sx={{ mt: 2, textAlign: 'center' }}>
                <Typography variant="body2" color="text.secondary">
                  Zobrazeno {filteredUsers.length} z {users.length} uživatelů
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
        <MenuItem onClick={() => handleEditUser(menuUser)}>
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Upravit</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleResetPassword(menuUser)}>
          <ListItemIcon>
            <LockResetIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Resetovat heslo</ListItemText>
        </MenuItem>
        <MenuItem onClick={() => handleDeleteUser(menuUser)} sx={{ color: 'error.main' }}>
          <ListItemIcon>
            <DeleteIcon fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText>Smazat</ListItemText>
        </MenuItem>
      </Menu>

      {/* Dialogs */}
      <CreateUserDialog
        open={createDialogOpen}
        onClose={() => setCreateDialogOpen(false)}
        onSuccess={handleUserCreated}
      />

      <EditUserDialog
        open={editDialogOpen}
        onClose={() => {
          setEditDialogOpen(false);
          setSelectedUser(null);
        }}
        user={selectedUser}
        onSuccess={handleUserUpdated}
      />

      <DeleteUserDialog
        open={deleteDialogOpen}
        onClose={() => {
          setDeleteDialogOpen(false);
          setSelectedUser(null);
        }}
        user={selectedUser}
        onSuccess={handleUserDeleted}
      />

      <ResetPasswordDialog
        open={resetPasswordDialogOpen}
        onClose={() => {
          setResetPasswordDialogOpen(false);
          setSelectedUser(null);
        }}
        user={selectedUser}
        onSuccess={handlePasswordReset}
      />
    </Box>
  );
}

Users.propTypes = {
  user: UserPropType,
};

export default Users;