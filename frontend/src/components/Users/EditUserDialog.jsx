import { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControlLabel,
  Switch,
  Box,
  Alert,
  CircularProgress,
  Autocomplete,
  Chip,
  Tabs,
  Tab,
  Typography,
} from '@mui/material';
import {
  Save as SaveIcon,
  Close as CloseIcon,
  Person as PersonIcon,
  Security as SecurityIcon,
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';
import PropTypes from 'prop-types';

/**
 * ✏️ Edit User Dialog
 * 
 * Dialog pro editaci existujícího uživatele:
 * - Tab 1: Basic Info (username, email, firstName, lastName, enabled)
 * - Tab 2: Roles (multi-select s možností add/remove)
 * - Form validation
 * - Real-time role sync
 */
export const EditUserDialog = ({ open, user, onClose, onUserUpdated }) => {
  const [activeTab, setActiveTab] = useState(0);
  
  const [formData, setFormData] = useState({
    email: '',
    firstName: '',
    lastName: '',
    enabled: true,
    emailVerified: false,
    manager: null,
  });

  const [userRoles, setUserRoles] = useState([]);
  const [availableRoles, setAvailableRoles] = useState([]);
  const [availableUsers, setAvailableUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingRoles, setLoadingRoles] = useState(false);
  const [error, setError] = useState(null);
  const [formErrors, setFormErrors] = useState({});

  // Load user data when dialog opens
  useEffect(() => {
    if (open && user) {
      loadUserData();
      loadRoles();
      loadUsers();
    }
  }, [open, user]);

  const loadUserData = async () => {
    setFormData({
      email: user.email || '',
      firstName: user.firstName || '',
      lastName: user.lastName || '',
      enabled: user.enabled !== false,
      emailVerified: user.emailVerified || false,
      manager: user.manager || null,
    });

    // Load user roles
    try {
      const roles = await apiService.getUserRoles(user.id);
      // Convert role names to role objects
      const roleNames = Array.isArray(roles) ? roles : [];
      setUserRoles(roleNames);
      logger.info('User roles loaded', { userId: user.id, count: roleNames.length });
    } catch (err) {
      logger.error('Failed to load user roles', { error: err.message });
    }
  };

  const loadRoles = async () => {
    try {
      setLoadingRoles(true);
      const roles = await apiService.getRoles();
      setAvailableRoles(roles || []);
    } catch (err) {
      logger.error('Failed to load roles', { error: err.message });
      setError('Nepodařilo se načíst seznam rolí');
    } finally {
      setLoadingRoles(false);
    }
  };

  const loadUsers = async () => {
    try {
      const users = await apiService.getUsers();
      // Exclude current user from manager selection
      setAvailableUsers((users || []).filter(u => u.id !== user?.id));
    } catch (err) {
      logger.error('Failed to load users', { error: err.message });
    }
  };

  const validateForm = () => {
    const errors = {};

    if (!formData.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = 'Zadejte platnou e-mailovou adresu';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleChange = (field) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value;
    setFormData(prev => ({ ...prev, [field]: value }));
    if (formErrors[field]) {
      setFormErrors(prev => ({ ...prev, [field]: undefined }));
    }
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      return;
    }

    try {
      setLoading(true);
      setError(null);

      logger.userAction('USER_UPDATE_ATTEMPT', { userId: user.id });

      // Update user basic info
      const updatedUser = await apiService.updateUser(user.id, formData);

      // Sync roles
      await syncRoles();

      logger.userAction('USER_UPDATED', { userId: user.id });

      onUserUpdated && onUserUpdated(updatedUser);
      onClose();

    } catch (err) {
      logger.error('Failed to update user', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se aktualizovat uživatele');
    } finally {
      setLoading(false);
    }
  };

  const syncRoles = async () => {
    // Get current roles from backend
    const currentRoles = await apiService.getUserRoles(user.id);
    const currentRoleNames = new Set(currentRoles);
    
    // Convert selected roles to names
    const selectedRoleNames = new Set(userRoles.map(r => typeof r === 'string' ? r : r.name));

    // Add new roles
    for (const roleName of selectedRoleNames) {
      if (!currentRoleNames.has(roleName)) {
        try {
          await apiService.assignRoleToUser(user.id, { roleName });
          logger.info('Role assigned', { userId: user.id, role: roleName });
        } catch (err) {
          logger.error('Failed to assign role', { 
            userId: user.id, 
            role: roleName, 
            error: err.message 
          });
        }
      }
    }

    // Remove old roles
    for (const roleName of currentRoleNames) {
      if (!selectedRoleNames.has(roleName)) {
        try {
          await apiService.removeRoleFromUser(user.id, roleName);
          logger.info('Role removed', { userId: user.id, role: roleName });
        } catch (err) {
          logger.error('Failed to remove role', { 
            userId: user.id, 
            role: roleName, 
            error: err.message 
          });
        }
      }
    }
  };

  const handleRolesChange = (event, newValue) => {
    setUserRoles(newValue);
  };

  if (!user) return null;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        Upravit uživatele: {user.username}
      </DialogTitle>

      <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tab icon={<PersonIcon />} label="Základní údaje" />
        <Tab icon={<SecurityIcon />} label="Role" />
      </Tabs>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {/* TAB 1: Basic Info */}
        {activeTab === 0 && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 2 }}>
            {/* Username (read-only) */}
            <TextField
              label="Uživatelské jméno"
              value={user.username}
              fullWidth
              disabled
              helperText="Uživatelské jméno nelze změnit"
            />

            {/* Email */}
            <TextField
              label="E-mail *"
              type="email"
              value={formData.email}
              onChange={handleChange('email')}
              error={!!formErrors.email}
              helperText={formErrors.email}
              fullWidth
              disabled={loading}
            />

            {/* First Name */}
            <TextField
              label="Jméno"
              value={formData.firstName}
              onChange={handleChange('firstName')}
              fullWidth
              disabled={loading}
            />

            {/* Last Name */}
            <TextField
              label="Příjmení"
              value={formData.lastName}
              onChange={handleChange('lastName')}
              fullWidth
              disabled={loading}
            />

            {/* Manager */}
            <Autocomplete
              options={availableUsers}
              getOptionLabel={(option) => `${option.firstName || ''} ${option.lastName || ''} (${option.username})`.trim()}
              value={formData.manager}
              onChange={(event, newValue) => {
                setFormData(prev => ({ ...prev, manager: newValue }));
              }}
              disabled={loading}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Nadřízený (Manager)"
                  helperText="Vyberte nadřízeného uživatele"
                />
              )}
              renderOption={(props, option) => (
                <li {...props}>
                  <Box>
                    <Typography variant="body1">
                      {option.firstName} {option.lastName}
                    </Typography>
                    <Typography variant="body2" color="text.secondary">
                      {option.username} • {option.email}
                    </Typography>
                  </Box>
                </li>
              )}
              isOptionEqualToValue={(option, value) => option?.id === value?.id}
            />

            {/* Switches */}
            <Box>
              <FormControlLabel
                control={
                  <Switch
                    checked={formData.enabled}
                    onChange={handleChange('enabled')}
                    disabled={loading}
                  />
                }
                label="Účet aktivní"
              />

              <FormControlLabel
                control={
                  <Switch
                    checked={formData.emailVerified}
                    onChange={handleChange('emailVerified')}
                    disabled={loading}
                  />
                }
                label="E-mail ověřený"
              />
            </Box>
          </Box>
        )}

        {/* TAB 2: Roles */}
        {activeTab === 1 && (
          <Box sx={{ mt: 2 }}>
            <Autocomplete
              multiple
              options={availableRoles}
              getOptionLabel={(option) => typeof option === 'string' ? option : option.name}
              value={userRoles}
              onChange={handleRolesChange}
              loading={loadingRoles}
              disabled={loading}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Role uživatele"
                  placeholder="Vyberte role"
                  helperText="Přidejte nebo odeberte role kliknutím"
                />
              )}
              renderTags={(value, getTagProps) =>
                value.map((option, index) => {
                  const label = typeof option === 'string' ? option : option.name;
                  return (
                    <Chip
                      label={label}
                      {...getTagProps({ index })}
                      size="small"
                      key={index}
                    />
                  );
                })
              }
            />
          </Box>
        )}
      </DialogContent>

      <DialogActions>
        <Button 
          onClick={onClose} 
          disabled={loading}
          startIcon={<CloseIcon />}
        >
          Zrušit
        </Button>
        <Button 
          onClick={handleSubmit}
          variant="contained"
          disabled={loading}
          startIcon={loading ? <CircularProgress size={20} /> : <SaveIcon />}
        >
          Uložit změny
        </Button>
      </DialogActions>
    </Dialog>
  );
};

EditUserDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  user: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onUserUpdated: PropTypes.func,
};

export default EditUserDialog;
