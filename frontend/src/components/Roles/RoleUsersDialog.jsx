import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  Typography,
  TextField,
  InputAdornment,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Avatar,
  IconButton,
  Chip,
  CircularProgress,
  Alert,
  Divider,
  Tab,
  Tabs,
} from '@mui/material';
import {
  Search as SearchIcon,
  PersonAdd as PersonAddIcon,
  PersonRemove as PersonRemoveIcon,
  Close as CloseIcon,
} from '@mui/icons-material';
import axios from 'axios';
import logger from '../../services/logger';

/**
 * Dialog pro správu uživatelů v roli
 * - Zobrazení aktuálních uživatelů v roli
 * - Přidání nových uživatelů do role
 * - Odebrání uživatelů z role
 * - Vyhledávání
 */
export const RoleUsersDialog = ({ open, onClose, role, tenantKey, onUpdate }) => {
  const [activeTab, setActiveTab] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  
  // Uživatelé v roli
  const [roleUsers, setRoleUsers] = useState([]);
  const [roleUsersSearch, setRoleUsersSearch] = useState('');
  
  // Dostupní uživatelé (pro přidání)
  const [availableUsers, setAvailableUsers] = useState([]);
  const [availableUsersSearch, setAvailableUsersSearch] = useState('');

  // Načtení uživatelů v roli
  useEffect(() => {
    if (open && role) {
      loadRoleUsers();
      loadAvailableUsers();
    }
  }, [open, role]);

  const loadRoleUsers = async () => {
    if (!role?.name) return;
    
    try {
      setLoading(true);
      setError(null);
      
      const endpoint = tenantKey 
        ? `/api/admin/roles/${role.name}/users?tenantKey=${tenantKey}`
        : `/api/roles/${role.name}/users`;
        
      const response = await axios.get(endpoint);
      setRoleUsers(response.data || []);
      logger.info('Role users loaded', { role: role.name, count: response.data?.length });
    } catch (err) {
      logger.error('Failed to load role users', { error: err.message });
      setError('Nepodařilo se načíst uživatele v roli');
    } finally {
      setLoading(false);
    }
  };

  const loadAvailableUsers = async () => {
    if (!tenantKey) return;
    
    try {
      const endpoint = `/api/admin/users/tenant/${tenantKey}`;
      const response = await axios.get(endpoint);
      
      // Filtrujeme uživatele, kteří už v roli nejsou
      const roleUserIds = new Set(roleUsers.map(u => u.id));
      const available = (response.data || []).filter(u => !roleUserIds.has(u.id));
      
      setAvailableUsers(available);
    } catch (err) {
      logger.error('Failed to load available users', { error: err.message });
    }
  };

  const handleAddUser = async (user) => {
    try {
      setLoading(true);
      
      const endpoint = tenantKey
        ? `/api/admin/roles/${role.name}/users/${user.id}?tenantKey=${tenantKey}`
        : `/api/roles/${role.name}/users/${user.id}`;
        
      await axios.post(endpoint);
      
      logger.info('User added to role', { role: role.name, user: user.username });
      
      // Aktualizujeme seznamy
      setRoleUsers([...roleUsers, user]);
      setAvailableUsers(availableUsers.filter(u => u.id !== user.id));
      
      if (onUpdate) onUpdate();
    } catch (err) {
      logger.error('Failed to add user to role', { error: err.message });
      setError('Nepodařilo se přidat uživatele do role');
    } finally {
      setLoading(false);
    }
  };

  const handleRemoveUser = async (user) => {
    try {
      setLoading(true);
      
      const endpoint = tenantKey
        ? `/api/admin/roles/${role.name}/users/${user.id}?tenantKey=${tenantKey}`
        : `/api/roles/${role.name}/users/${user.id}`;
        
      await axios.delete(endpoint);
      
      logger.info('User removed from role', { role: role.name, user: user.username });
      
      // Aktualizujeme seznamy
      setRoleUsers(roleUsers.filter(u => u.id !== user.id));
      setAvailableUsers([...availableUsers, user]);
      
      if (onUpdate) onUpdate();
    } catch (err) {
      logger.error('Failed to remove user from role', { error: err.message });
      setError('Nepodařilo se odebrat uživatele z role');
    } finally {
      setLoading(false);
    }
  };

  const filteredRoleUsers = roleUsers.filter(u => 
    !roleUsersSearch || 
    u.username?.toLowerCase().includes(roleUsersSearch.toLowerCase()) ||
    u.email?.toLowerCase().includes(roleUsersSearch.toLowerCase()) ||
    `${u.firstName} ${u.lastName}`.toLowerCase().includes(roleUsersSearch.toLowerCase())
  );

  const filteredAvailableUsers = availableUsers.filter(u => 
    !availableUsersSearch || 
    u.username?.toLowerCase().includes(availableUsersSearch.toLowerCase()) ||
    u.email?.toLowerCase().includes(availableUsersSearch.toLowerCase()) ||
    `${u.firstName} ${u.lastName}`.toLowerCase().includes(availableUsersSearch.toLowerCase())
  );

  const getUserInitials = (user) => {
    if (user.firstName && user.lastName) {
      return `${user.firstName[0]}${user.lastName[0]}`.toUpperCase();
    }
    return user.username?.substring(0, 2).toUpperCase() || '??';
  };

  const getUserDisplayName = (user) => {
    if (user.firstName || user.lastName) {
      return `${user.firstName || ''} ${user.lastName || ''}`.trim();
    }
    return user.username || 'Neznámý';
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      PaperProps={{
        sx: { borderRadius: 3 }
      }}
    >
      <DialogTitle sx={{ fontWeight: 600, fontSize: '1.5rem', pb: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Box>
            👥 Uživatelé v roli: <Chip label={role?.name} color="primary" size="small" sx={{ ml: 1 }} />
          </Box>
          <IconButton onClick={onClose} size="small">
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>

      <Divider />

      {error && (
        <Alert severity="error" sx={{ m: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)} sx={{ borderBottom: 1, borderColor: 'divider', px: 3 }}>
        <Tab label={`V roli (${roleUsers.length})`} />
        <Tab label={`Přidat uživatele (${availableUsers.length})`} />
      </Tabs>

      <DialogContent sx={{ p: 3 }}>
        {/* Tab: Uživatelé v roli */}
        {activeTab === 0 && (
          <Box>
            <TextField
              fullWidth
              placeholder="Hledat uživatele v roli..."
              value={roleUsersSearch}
              onChange={(e) => setRoleUsersSearch(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
              sx={{ mb: 2 }}
            />

            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                <CircularProgress />
              </Box>
            ) : filteredRoleUsers.length === 0 ? (
              <Alert severity="info">
                {roleUsersSearch ? 'Žádní uživatelé nenalezeni' : 'V této roli zatím nejsou žádní uživatelé'}
              </Alert>
            ) : (
              <List>
                {filteredRoleUsers.map((user) => (
                  <ListItem
                    key={user.id}
                    secondaryAction={
                      <IconButton
                        edge="end"
                        onClick={() => handleRemoveUser(user)}
                        disabled={loading}
                        color="error"
                      >
                        <PersonRemoveIcon />
                      </IconButton>
                    }
                  >
                    <ListItemAvatar>
                      <Avatar sx={{ bgcolor: 'primary.main' }}>
                        {getUserInitials(user)}
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={getUserDisplayName(user)}
                      secondary={
                        <Box component="span">
                          <Typography variant="body2" component="span" color="text.secondary">
                            @{user.username}
                          </Typography>
                          {user.email && (
                            <Typography variant="body2" component="span" color="text.secondary" sx={{ ml: 1 }}>
                              • {user.email}
                            </Typography>
                          )}
                        </Box>
                      }
                    />
                  </ListItem>
                ))}
              </List>
            )}
          </Box>
        )}

        {/* Tab: Přidat uživatele */}
        {activeTab === 1 && (
          <Box>
            <TextField
              fullWidth
              placeholder="Hledat dostupné uživatele..."
              value={availableUsersSearch}
              onChange={(e) => setAvailableUsersSearch(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              }}
              sx={{ mb: 2 }}
            />

            {loading ? (
              <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                <CircularProgress />
              </Box>
            ) : filteredAvailableUsers.length === 0 ? (
              <Alert severity="info">
                {availableUsersSearch ? 'Žádní uživatelé nenalezeni' : 'Všichni uživatelé už jsou v této roli'}
              </Alert>
            ) : (
              <List>
                {filteredAvailableUsers.map((user) => (
                  <ListItem
                    key={user.id}
                    secondaryAction={
                      <IconButton
                        edge="end"
                        onClick={() => handleAddUser(user)}
                        disabled={loading}
                        color="primary"
                      >
                        <PersonAddIcon />
                      </IconButton>
                    }
                  >
                    <ListItemAvatar>
                      <Avatar sx={{ bgcolor: 'grey.400' }}>
                        {getUserInitials(user)}
                      </Avatar>
                    </ListItemAvatar>
                    <ListItemText
                      primary={getUserDisplayName(user)}
                      secondary={
                        <Box component="span">
                          <Typography variant="body2" component="span" color="text.secondary">
                            @{user.username}
                          </Typography>
                          {user.email && (
                            <Typography variant="body2" component="span" color="text.secondary" sx={{ ml: 1 }}>
                              • {user.email}
                            </Typography>
                          )}
                        </Box>
                      }
                    />
                  </ListItem>
                ))}
              </List>
            )}
          </Box>
        )}
      </DialogContent>

      <DialogActions sx={{ p: 3, pt: 0 }}>
        <Button onClick={onClose} variant="contained">
          Zavřít
        </Button>
      </DialogActions>
    </Dialog>
  );
};

export default RoleUsersDialog;
