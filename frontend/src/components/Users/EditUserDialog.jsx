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
  Group as GroupIcon,
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
 * - Tab 3: Groups (multi-select s možností add/remove)
 * - Form validation
 * - Real-time role & group sync
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
  const [userGroups, setUserGroups] = useState([]);
  const [availableGroups, setAvailableGroups] = useState([]);
  const [availableUsers, setAvailableUsers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingRoles, setLoadingRoles] = useState(false);
  const [loadingGroups, setLoadingGroups] = useState(false);
  const [error, setError] = useState(null);
  const [formErrors, setFormErrors] = useState({});

  // Load user data when dialog opens
  useEffect(() => {
    if (open && user) {
      loadUserData();
      loadRoles();
      loadGroups();
      loadUsers();
    }
  }, [open, user]);

  const loadUserData = async () => {
    // First load available users to resolve manager object
    await loadUsers();
    
    setFormData({
      email: user.email || '',
      firstName: user.firstName || '',
      lastName: user.lastName || '',
      enabled: user.enabled !== false,
      emailVerified: user.emailVerified || false,
      manager: null, // Will be set after users are loaded
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
    
    // Load user groups
    try {
      const groups = await apiService.getUserGroups(user.id);
      setUserGroups(Array.isArray(groups) ? groups : []);
      logger.info('User groups loaded', { userId: user.id, count: groups?.length || 0 });
    } catch (err) {
      logger.error('Failed to load user groups', { error: err.message });
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
  
  const loadGroups = async () => {
    try {
      setLoadingGroups(true);
      const groups = await apiService.getGroups();
      setAvailableGroups(groups || []);
    } catch (err) {
      logger.error('Failed to load groups', { error: err.message });
      setError('Nepodařilo se načíst seznam skupin');
    } finally {
      setLoadingGroups(false);
    }
  };

  const loadUsers = async () => {
    try {
      const users = await apiService.getUsers();
      // Exclude current user from manager selection
      const filteredUsers = (users || []).filter(u => u.id !== user?.id);
      setAvailableUsers(filteredUsers);
      
      // 🔍 Resolve manager object from username
      if (user?.manager) {
        const managerObj = filteredUsers.find(u => u.username === user.manager);
        if (managerObj) {
          setFormData(prev => ({ ...prev, manager: managerObj }));
          logger.info('Manager resolved', { username: user.manager, managerId: managerObj.id });
        } else {
          logger.warn('Manager not found in user list', { username: user.manager });
        }
      }
    } catch (err) {
      logger.error('Failed to load users', { error: err.message });
    }
  };

  const validateForm = () => {
    const errors = {};

    // ✅ Email validation
    if (!formData.email) {
      errors.email = 'E-mail je povinný';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = 'Neplatný email formát';
    }

    // ✅ First Name validation (max 50 chars)
    if (formData.firstName && formData.firstName.length > 50) {
      errors.firstName = 'Jméno nesmí být delší než 50 znaků';
    }

    // ✅ Last Name validation (max 50 chars)
    if (formData.lastName && formData.lastName.length > 50) {
      errors.lastName = 'Příjmení nesmí být delší než 50 znaků';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleChange = (field) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value;
    setFormData(prev => ({ ...prev, [field]: value }));
    
    // Clear field error when user starts typing
    if (formErrors[field]) {
      setFormErrors(prev => ({ ...prev, [field]: undefined }));
    }
    
    // ✅ Real-time validation for string length
    if (typeof value === 'string') {
      const newErrors = { ...formErrors };
      
      if (field === 'firstName' && value.length > 50) {
        newErrors.firstName = 'Jméno nesmí být delší než 50 znaků';
      }
      
      if (field === 'lastName' && value.length > 50) {
        newErrors.lastName = 'Příjmení nesmí být delší než 50 znaků';
      }
      
      if (field === 'email' && value && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        newErrors.email = 'Neplatný email formát';
      }
      
      setFormErrors(newErrors);
    }
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      setActiveTab(0); // Switch to Basic Info tab if validation fails
      return;
    }

    try {
      setLoading(true);
      setError(null);
      setFormErrors({}); // Clear previous field errors

      logger.userAction('USER_UPDATE_ATTEMPT', { userId: user.id });

      // 🔧 Prepare request data - convert manager object to username string
      const requestData = {
        ...formData,
        manager: formData.manager?.username || null, // Send only username, not full object
      };

      // Update user basic info
      const updatedUser = await apiService.updateUser(user.id, requestData);

      // Sync roles
      await syncRoles();
      
      // Sync groups
      await syncGroups();

      logger.userAction('USER_UPDATED', { userId: user.id });

      onUserUpdated && onUserUpdated(updatedUser);
      onClose();

    } catch (err) {
      logger.error('Failed to update user', { error: err.message, response: err.response });
      
      // 🔍 Parse backend validation errors
      if (err.response?.status === 400) {
        const backendError = err.response?.data;
        
        // If it's a validation error with field-specific messages
        if (backendError?.fieldErrors && Object.keys(backendError.fieldErrors).length > 0) {
          setFormErrors(backendError.fieldErrors);
          setError('Opravte prosím chyby ve formuláři');
          setActiveTab(0); // Switch to Basic Info tab
        } else {
          // Generic validation error
          setError(backendError?.message || 'Neplatný formát požadavku nebo chybějící data');
        }
      } else {
        // Other errors (401, 403, 500, etc.)
        setError(err.response?.data?.message || 'Nepodařilo se aktualizovat uživatele');
      }
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
  
  const syncGroups = async () => {
    // Get current groups from backend
    const currentGroups = await apiService.getUserGroups(user.id);
    const currentGroupNames = new Set(currentGroups.map(g => typeof g === 'string' ? g : g.name));
    
    // Convert selected groups to names
    const selectedGroupNames = new Set(userGroups.map(g => typeof g === 'string' ? g : g.name));

    // Add new groups
    for (const groupName of selectedGroupNames) {
      if (!currentGroupNames.has(groupName)) {
        try {
          await apiService.assignGroupToUser(user.id, { groupName });
          logger.info('Group assigned', { userId: user.id, group: groupName });
        } catch (err) {
          logger.error('Failed to assign group', { 
            userId: user.id, 
            group: groupName, 
            error: err.message 
          });
        }
      }
    }

    // Remove old groups
    for (const groupName of currentGroupNames) {
      if (!selectedGroupNames.has(groupName)) {
        try {
          await apiService.removeGroupFromUser(user.id, groupName);
          logger.info('Group removed', { userId: user.id, group: groupName });
        } catch (err) {
          logger.error('Failed to remove group', { 
            userId: user.id, 
            group: groupName, 
            error: err.message 
          });
        }
      }
    }
  };

  const handleRolesChange = (event, newValue) => {
    setUserRoles(newValue);
  };
  
  const handleGroupsChange = (event, newValue) => {
    setUserGroups(newValue);
  };

  if (!user) return null;

  // Check if user is federated (AD) - attributes and enabled status are read-only
  const isFederated = user.isFederated || user.directorySource === 'AD';
  const attributesReadOnly = isFederated;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        Upravit uživatele: {user.username}
        {isFederated && (
          <Chip 
            label="Federovaný účet" 
            size="small" 
            color="info" 
            sx={{ ml: 2 }}
          />
        )}
      </DialogTitle>

      <Tabs value={activeTab} onChange={(e, v) => setActiveTab(v)} sx={{ borderBottom: 1, borderColor: 'divider' }}>
        <Tab icon={<PersonIcon />} label="Základní údaje" />
        <Tab icon={<SecurityIcon />} label="Role" />
        <Tab icon={<GroupIcon />} label="Skupiny" />
      </Tabs>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        
        {isFederated && (
          <Alert severity="info" sx={{ mb: 2 }}>
            Tento uživatel je federovaný z Active Directory. Osobní údaje a stav účtu jsou synchronizovány z AD a nelze je měnit.
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
              helperText={formErrors.email || (attributesReadOnly ? 'Synchronizováno z AD' : '')}
              fullWidth
              disabled={loading || attributesReadOnly}
            />

            {/* First Name */}
            <TextField
              label="Jméno"
              value={formData.firstName}
              onChange={handleChange('firstName')}
              error={!!formErrors.firstName}
              helperText={formErrors.firstName || (attributesReadOnly ? 'Synchronizováno z AD' : 'Maximálně 50 znaků')}
              fullWidth
              disabled={loading || attributesReadOnly}
            />

            {/* Last Name */}
            <TextField
              label="Příjmení"
              value={formData.lastName}
              onChange={handleChange('lastName')}
              error={!!formErrors.lastName}
              helperText={formErrors.lastName || (attributesReadOnly ? 'Synchronizováno z AD' : 'Maximálně 50 znaků')}
              fullWidth
              disabled={loading || attributesReadOnly}
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
                    disabled={loading || attributesReadOnly}
                  />
                }
                label={attributesReadOnly ? "Účet aktivní (synchronizováno z AD)" : "Účet aktivní"}
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
        
        {/* TAB 3: Groups */}
        {activeTab === 2 && (
          <Box sx={{ mt: 2 }}>
            <Autocomplete
              multiple
              options={availableGroups}
              getOptionLabel={(option) => typeof option === 'string' ? option : option.name}
              value={userGroups}
              onChange={handleGroupsChange}
              loading={loadingGroups}
              disabled={loading}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Skupiny uživatele"
                  placeholder="Vyberte skupiny"
                  helperText="Přidejte nebo odeberte skupiny kliknutím"
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
                      color="secondary"
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
