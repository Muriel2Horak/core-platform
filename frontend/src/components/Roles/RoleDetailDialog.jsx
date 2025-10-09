import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
  Typography,
  Chip,
  IconButton,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  Paper,
  Divider,
  InputAdornment,
} from '@mui/material';
import {
  Save as SaveIcon,
  Close as CloseIcon,
  AccountTree as AccountTreeIcon,
  People as PeopleIcon,
  Delete as DeleteIcon,
  Info as InfoIcon,
  Warning as WarningIcon,
  Add as AddIcon,
  Search as SearchIcon,
  RemoveCircleOutline as RemoveIcon,
  Business as BusinessIcon,
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';

/**
 * 📑 Role Detail Dialog with Tabs
 * Kompletní správa role v jednom dialogu s taby:
 * - Přehled (základní info)
 * - Hierarchie (jen pro composite)
 * - Uživatelé (správa přiřazení)
 * - Nebezpečná zóna (smazání)
 */
export const RoleDetailDialog = ({ open, role, onClose, onSuccess, onDelete, user, tenantKey = null }) => {
  const [currentTab, setCurrentTab] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Přehled tab
  const [description, setDescription] = useState('');

  // Hierarchie tab
  const [composites, setComposites] = useState([]);
  const [availableRoles, setAvailableRoles] = useState([]);
  const [compositesLoading, setCompositesLoading] = useState(false);

  // Uživatelé tab
  const [users, setUsers] = useState([]);
  const [availableUsers, setAvailableUsers] = useState([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [userSearchQuery, setUserSearchQuery] = useState('');

  // Smazání
  const [deleteConfirmation, setDeleteConfirmation] = useState('');

  const isCoreAdmin = user?.roles?.includes('CORE_ROLE_ADMIN');

  // Permission mapping - co role umožňuje (Backend API + External Systems)
  const rolePermissions = {
    'CORE_ROLE_ADMIN': [
      'API: Přístup ke všem /api/* endpointům',
      'API: Správa tenantů (POST/PUT/DELETE /api/tenants)',
      'API: Správa uživatelů napříč tenanty',
      'API: Správa rolí napříč tenanty',
      'Grafana: Admin úroveň (všechny workspace)',
      'Loki: Přístup ke všem logům (všechny tenanty)',
      'Keycloak: Admin konzole',
    ],
    'CORE_ROLE_TENANT_ADMIN': [
      'API: Správa uživatelů (GET/POST/PUT /api/users) - vlastní tenant',
      'API: Správa rolí (GET/POST/PUT /api/roles) - vlastní tenant',
      'API: Čtení skupin (GET /api/groups) - vlastní tenant',
      'Grafana: Editor úroveň (tenant workspace)',
      'Loki: Přístup k logům vlastního tenantu',
      'Data scope: Pouze vlastní tenant',
    ],
    'CORE_ROLE_USER_MANAGER': [
      'API: Správa uživatelů (GET/POST/PUT /api/users) - vlastní tenant',
      'API: Čtení rolí (GET /api/roles) - vlastní tenant',
      'API: Přiřazování existujících rolí uživatelům',
      'Data scope: Pouze vlastní tenant',
    ],
    'CORE_ROLE_USER': [
      'API: Čtení vlastního profilu (GET /api/users/me)',
      'API: Aktualizace vlastního profilu (PUT /api/users/me)',
      'Grafana: Viewer úroveň (omezené dashboardy)',
      'Data scope: Pouze vlastní data',
    ],
  };

  useEffect(() => {
    if (role) {
      setDescription(role.description || '');
      setCurrentTab(0);
      setError(null);
      setDeleteConfirmation('');
    }
  }, [role]);

  // Load composites when tab changes
  useEffect(() => {
    if (role && currentTab === 1 && role.composite) {
      loadComposites();
    }
  }, [currentTab, role]);

  // Load users when tab changes
  useEffect(() => {
    if (role && currentTab === 2) {
      loadUsers();
    }
  }, [currentTab, role]);

  const loadComposites = async () => {
    try {
      setCompositesLoading(true);
      const [current, available] = await Promise.all([
        apiService.getRoleComposites(role.name),
        apiService.getRoles(),
      ]);
      setComposites(current || []);
      // Filter out current role and already added composites
      const availableFiltered = (available || []).filter(
        r => r.name !== role.name && !current?.some(c => c.name === r.name)
      );
      setAvailableRoles(availableFiltered);
    } catch (err) {
      logger.error('Failed to load composites', { error: err.message });
      setError('Nepodařilo se načíst hierarchii rolí');
    } finally {
      setCompositesLoading(false);
    }
  };

  const loadUsers = async () => {
    try {
      setUsersLoading(true);
      const [roleUsers, allUsers] = await Promise.all([
        apiService.getRoleUsers(role.name, tenantKey),
        apiService.getUsers(),
      ]);
      
      setUsers(roleUsers || []);
      // Filter out users who already have this role
      const availableFiltered = (allUsers || []).filter(
        u => !roleUsers?.some(ru => ru.id === u.id)
      );
      setAvailableUsers(availableFiltered);
    } catch (err) {
      logger.error('Failed to load users', { error: err.message });
      setError('Nepodařilo se načíst uživatele role');
    } finally {
      setUsersLoading(false);
    }
  };

  const handleUpdateDescription = async () => {
    try {
      setLoading(true);
      setError(null);

      await apiService.updateRole(role.name, {
        name: role.name,
        description: description,
        composite: role.composite,
      });

      logger.userAction('ROLE_UPDATED', { name: role.name });
      onSuccess && onSuccess();

    } catch (err) {
      logger.error('Failed to update role', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se aktualizovat roli');
    } finally {
      setLoading(false);
    }
  };

  const handleAddComposite = async (compositeName) => {
    try {
      setLoading(true);
      setError(null);

      await apiService.addCompositeRole(role.name, { name: compositeName });
      logger.userAction('ROLE_COMPOSITE_ADDED', { parent: role.name, child: compositeName });
      
      await loadComposites();
      onSuccess && onSuccess();

    } catch (err) {
      logger.error('Failed to add composite', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se přidat roli do hierarchie');
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveComposite = async (compositeName) => {
    try {
      setLoading(true);
      setError(null);

      await apiService.removeCompositeRole(role.name, compositeName);
      logger.userAction('ROLE_COMPOSITE_REMOVED', { parent: role.name, child: compositeName });
      
      await loadComposites();
      onSuccess && onSuccess();

    } catch (err) {
      logger.error('Failed to remove composite', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se odebrat roli z hierarchie');
    } finally {
      setLoading(false);
    }
  };

  const handleAddUserToRole = async (userId) => {
    try {
      setLoading(true);
      setError(null);

      await apiService.assignRoleToUser(userId, role.name);
      logger.userAction('USER_ROLE_ASSIGNED', { userId, roleName: role.name });
      
      await loadUsers();
      onSuccess && onSuccess();

    } catch (err) {
      logger.error('Failed to assign role to user', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se přiřadit roli uživateli');
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveUserFromRole = async (userId) => {
    try {
      setLoading(true);
      setError(null);

      await apiService.removeRoleFromUser(userId, role.name);
      logger.userAction('USER_ROLE_REMOVED', { userId, roleName: role.name });
      
      await loadUsers();
      onSuccess && onSuccess();

    } catch (err) {
      logger.error('Failed to remove role from user', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se odebrat roli uživateli');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (deleteConfirmation !== role.name) {
      setError('Pro smazání role musíte přesně napsat její název');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      await apiService.deleteRole(role.name);
      logger.userAction('ROLE_DELETED', { name: role.name });
      
      onDelete && onDelete();
      handleClose();

    } catch (err) {
      logger.error('Failed to delete role', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se smazat roli');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setDescription('');
    setError(null);
    setCurrentTab(0);
    setDeleteConfirmation('');
    onClose();
  };

  if (!role) return null;

  const filteredAvailableUsers = availableUsers.filter(u => 
    u.username?.toLowerCase().includes(userSearchQuery.toLowerCase()) ||
    u.email?.toLowerCase().includes(userSearchQuery.toLowerCase()) ||
    u.firstName?.toLowerCase().includes(userSearchQuery.toLowerCase()) ||
    u.lastName?.toLowerCase().includes(userSearchQuery.toLowerCase())
  );

  const permissions = rolePermissions[role.name] || [];

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="h6" component="span">
            {role.name}
          </Typography>
          {role.composite && (
            <Chip 
              label="Composite" 
              size="small" 
              icon={<AccountTreeIcon />}
              sx={{ 
                bgcolor: 'secondary.main',
                color: 'white',
                '& .MuiChip-icon': { color: 'white' }
              }}
            />
          )}
          {role.tenantKey && (
            <Chip 
              label={role.tenantKey} 
              size="small" 
              icon={<BusinessIcon />}
              variant="outlined"
            />
          )}
        </Box>
      </DialogTitle>

      <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tabs value={currentTab} onChange={(e, v) => setCurrentTab(v)}>
          <Tab icon={<InfoIcon />} label="Přehled" iconPosition="start" />
          {role.composite && (
            <Tab icon={<AccountTreeIcon />} label="Hierarchie" iconPosition="start" />
          )}
          <Tab icon={<PeopleIcon />} label="Uživatelé" iconPosition="start" />
          <Tab icon={<WarningIcon />} label="Nebezpečná zóna" iconPosition="start" />
        </Tabs>
      </Box>

      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {/* Tab 0: Přehled */}
        {currentTab === 0 && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3, mt: 2 }}>
            <TextField
              label="Název role"
              value={role.name}
              fullWidth
              disabled
              helperText="Název role nelze změnit"
            />

            <TextField
              label="Popis"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              multiline
              rows={3}
              fullWidth
            />

            <Box>
              <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 600 }}>
                Typ role
              </Typography>
              <Chip 
                label={role.composite ? 'Composite (sdružuje více rolí)' : 'Basic (základní role)'}
                color={role.composite ? 'secondary' : 'default'}
              />
            </Box>

            {permissions.length > 0 && (
              <Paper variant="outlined" sx={{ p: 2 }}>
                <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: 1 }}>
                  <InfoIcon fontSize="small" />
                  Oprávnění role
                </Typography>
                <List dense>
                  {permissions.map((perm, idx) => (
                    <ListItem key={idx}>
                      <ListItemText primary={`• ${perm}`} />
                    </ListItem>
                  ))}
                </List>
              </Paper>
            )}
          </Box>
        )}

        {/* Tab 1: Hierarchie (jen pro composite) */}
        {currentTab === 1 && role.composite && (
          <Box sx={{ mt: 2 }}>
            {compositesLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                <CircularProgress />
              </Box>
            ) : (
              <>
                <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 600 }}>
                  Aktuální role v hierarchii ({composites.length})
                </Typography>
                {composites.length === 0 ? (
                  <Alert severity="info" sx={{ mb: 3 }}>
                    Tato composite role zatím neobsahuje žádné vnořené role.
                  </Alert>
                ) : (
                  <Paper variant="outlined" sx={{ mb: 3 }}>
                    <List>
                      {composites.map((comp, idx) => (
                        <Box key={comp.name}>
                          {idx > 0 && <Divider />}
                          <ListItem>
                            <ListItemText 
                              primary={comp.name}
                              secondary={comp.description}
                            />
                            <ListItemSecondaryAction>
                              <IconButton 
                                edge="end" 
                                onClick={() => handleRemoveComposite(comp.name)}
                                disabled={loading}
                              >
                                <RemoveIcon />
                              </IconButton>
                            </ListItemSecondaryAction>
                          </ListItem>
                        </Box>
                      ))}
                    </List>
                  </Paper>
                )}

                <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 600 }}>
                  Dostupné role k přidání ({availableRoles.length})
                </Typography>
                {availableRoles.length === 0 ? (
                  <Alert severity="info">
                    Všechny dostupné role jsou již přidány.
                  </Alert>
                ) : (
                  <Paper variant="outlined">
                    <List>
                      {availableRoles.map((avail, idx) => (
                        <Box key={avail.name}>
                          {idx > 0 && <Divider />}
                          <ListItem>
                            <ListItemText 
                              primary={avail.name}
                              secondary={avail.description}
                            />
                            <ListItemSecondaryAction>
                              <IconButton 
                                edge="end" 
                                onClick={() => handleAddComposite(avail.name)}
                                disabled={loading}
                                color="primary"
                              >
                                <AddIcon />
                              </IconButton>
                            </ListItemSecondaryAction>
                          </ListItem>
                        </Box>
                      ))}
                    </List>
                  </Paper>
                )}
              </>
            )}
          </Box>
        )}

        {/* Tab 2: Uživatelé */}
        {currentTab === (role.composite ? 2 : 1) && (
          <Box sx={{ mt: 2 }}>
            {usersLoading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
                <CircularProgress />
              </Box>
            ) : (
              <>
                <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 600 }}>
                  Uživatelé s touto rolí ({users.length})
                </Typography>
                {users.length === 0 ? (
                  <Alert severity="info" sx={{ mb: 3 }}>
                    Tato role zatím není přiřazena žádnému uživateli.
                  </Alert>
                ) : (
                  <Paper variant="outlined" sx={{ mb: 3 }}>
                    <List>
                      {users.map((u, idx) => (
                        <Box key={u.id}>
                          {idx > 0 && <Divider />}
                          <ListItem>
                            <ListItemText 
                              primary={`${u.firstName} ${u.lastName} (${u.username})`}
                              secondary={u.email}
                            />
                            <ListItemSecondaryAction>
                              <IconButton 
                                edge="end" 
                                onClick={() => handleRemoveUserFromRole(u.id)}
                                disabled={loading}
                              >
                                <RemoveIcon />
                              </IconButton>
                            </ListItemSecondaryAction>
                          </ListItem>
                        </Box>
                      ))}
                    </List>
                  </Paper>
                )}

                <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 600 }}>
                  Přidat uživatele do role
                </Typography>
                
                <TextField
                  fullWidth
                  placeholder="Hledat uživatele..."
                  value={userSearchQuery}
                  onChange={(e) => setUserSearchQuery(e.target.value)}
                  InputProps={{
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchIcon />
                      </InputAdornment>
                    ),
                  }}
                  sx={{ mb: 2 }}
                />

                {filteredAvailableUsers.length === 0 ? (
                  <Alert severity="info">
                    {userSearchQuery 
                      ? 'Nebyli nalezeni žádní uživatelé odpovídající hledání.'
                      : 'Všichni uživatelé již mají tuto roli přiřazenou.'
                    }
                  </Alert>
                ) : (
                  <Paper variant="outlined" sx={{ maxHeight: 300, overflow: 'auto' }}>
                    <List>
                      {filteredAvailableUsers.map((u, idx) => (
                        <Box key={u.id}>
                          {idx > 0 && <Divider />}
                          <ListItem>
                            <ListItemText 
                              primary={`${u.firstName} ${u.lastName} (${u.username})`}
                              secondary={u.email}
                            />
                            <ListItemSecondaryAction>
                              <IconButton 
                                edge="end" 
                                onClick={() => handleAddUserToRole(u.id)}
                                disabled={loading}
                                color="primary"
                              >
                                <AddIcon />
                              </IconButton>
                            </ListItemSecondaryAction>
                          </ListItem>
                        </Box>
                      ))}
                    </List>
                  </Paper>
                )}
              </>
            )}
          </Box>
        )}

        {/* Tab 3: Nebezpečná zóna */}
        {currentTab === (role.composite ? 3 : 2) && (
          <Box sx={{ mt: 2 }}>
            <Alert severity="error" sx={{ mb: 3 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 1 }}>
                ⚠️ Varování před smazáním role
              </Typography>
              <Typography variant="body2">
                Smazání role je trvalá akce, kterou nelze vrátit zpět. Uživatelé s touto rolí 
                ztratí přístup k funkcím, které role poskytuje.
              </Typography>
            </Alert>

            {permissions.length > 0 && (
              <Paper variant="outlined" sx={{ p: 2, mb: 3, bgcolor: 'error.light', color: 'error.contrastText' }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 600, mb: 1 }}>
                  Ztráta přístupu k těmto funkcím:
                </Typography>
                <List dense>
                  {permissions.map((perm, idx) => (
                    <ListItem key={idx}>
                      <ListItemText primary={`• ${perm}`} />
                    </ListItem>
                  ))}
                </List>
              </Paper>
            )}

            <Box sx={{ bgcolor: 'grey.50', p: 3, borderRadius: 1, border: '2px dashed', borderColor: 'error.main' }}>
              <Typography variant="subtitle2" gutterBottom sx={{ fontWeight: 600 }}>
                Smazat roli: {role.name}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Pro potvrzení smazání napište přesný název role:
              </Typography>
              <TextField
                fullWidth
                placeholder={role.name}
                value={deleteConfirmation}
                onChange={(e) => setDeleteConfirmation(e.target.value)}
                error={deleteConfirmation.length > 0 && deleteConfirmation !== role.name}
                helperText={
                  deleteConfirmation.length > 0 && deleteConfirmation !== role.name
                    ? 'Název role se neshoduje'
                    : ''
                }
                sx={{ mb: 2 }}
              />
              <Button
                variant="contained"
                color="error"
                startIcon={<DeleteIcon />}
                onClick={handleDelete}
                disabled={deleteConfirmation !== role.name || loading}
                fullWidth
              >
                {loading ? <CircularProgress size={24} /> : 'Smazat roli'}
              </Button>
            </Box>
          </Box>
        )}
      </DialogContent>

      <DialogActions>
        <Button onClick={handleClose} startIcon={<CloseIcon />}>
          Zavřít
        </Button>
        {currentTab === 0 && (
          <Button
            variant="contained"
            onClick={handleUpdateDescription}
            disabled={loading}
            startIcon={loading ? <CircularProgress size={20} /> : <SaveIcon />}
          >
            Uložit změny
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
};

RoleDetailDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  role: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func,
  onDelete: PropTypes.func,
  user: PropTypes.object,
  tenantKey: PropTypes.string,
};
