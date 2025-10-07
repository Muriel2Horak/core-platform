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
} from '@mui/material';
import {
  Save as SaveIcon,
  Close as CloseIcon,
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';
import PropTypes from 'prop-types';

/**
 * 🆕 Create User Dialog
 * 
 * Dialog pro vytvoření nového uživatele s následujícími funkcemi:
 * - Basic user info (username, email, firstName, lastName)
 * - Password (temporary or permanent)
 * - Enable/disable user
 * - Role assignment (multi-select)
 * - Form validation
 */
export const CreateUserDialog = ({ open, onClose, onUserCreated }) => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    firstName: '',
    lastName: '',
    password: '',
    enabled: true,
    temporaryPassword: true,
    emailVerified: false,
  });

  const [selectedRoles, setSelectedRoles] = useState([]);
  const [availableRoles, setAvailableRoles] = useState([]);
  const [loading, setLoading] = useState(false);
  const [loadingRoles, setLoadingRoles] = useState(false);
  const [error, setError] = useState(null);
  const [formErrors, setFormErrors] = useState({});

  // Load available roles when dialog opens
  useEffect(() => {
    if (open) {
      loadRoles();
      resetForm();
    }
  }, [open]);

  const loadRoles = async () => {
    try {
      setLoadingRoles(true);
      const roles = await apiService.getRoles();
      setAvailableRoles(roles || []);
      logger.info('Roles loaded for user creation', { count: roles?.length });
    } catch (err) {
      logger.error('Failed to load roles', { error: err.message });
      setError('Nepodařilo se načíst seznam rolí');
    } finally {
      setLoadingRoles(false);
    }
  };

  const resetForm = () => {
    setFormData({
      username: '',
      email: '',
      firstName: '',
      lastName: '',
      password: '',
      enabled: true,
      temporaryPassword: true,
      emailVerified: false,
    });
    setSelectedRoles([]);
    setError(null);
    setFormErrors({});
  };

  const validateForm = () => {
    const errors = {};

    if (!formData.username || formData.username.trim().length < 3) {
      errors.username = 'Uživatelské jméno musí mít alespoň 3 znaky';
    }

    if (!formData.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      errors.email = 'Zadejte platnou e-mailovou adresu';
    }

    if (!formData.password || formData.password.length < 8) {
      errors.password = 'Heslo musí mít alespoň 8 znaků';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleChange = (field) => (event) => {
    const value = event.target.type === 'checkbox' ? event.target.checked : event.target.value;
    setFormData(prev => ({ ...prev, [field]: value }));
    // Clear field error when user types
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

      logger.userAction('USER_CREATE_ATTEMPT', { username: formData.username });

      // Create user
      const newUser = await apiService.createUser({
        username: formData.username,
        email: formData.email,
        firstName: formData.firstName,
        lastName: formData.lastName,
        enabled: formData.enabled,
        emailVerified: formData.emailVerified,
        credentials: [{
          type: 'password',
          value: formData.password,
          temporary: formData.temporaryPassword,
        }],
      });

      // Assign selected roles
      if (selectedRoles.length > 0) {
        for (const role of selectedRoles) {
          try {
            await apiService.assignRoleToUser(newUser.id, { roleName: role.name });
          } catch (roleErr) {
            logger.error('Failed to assign role', { 
              userId: newUser.id, 
              role: role.name, 
              error: roleErr.message 
            });
          }
        }
      }

      logger.userAction('USER_CREATED', { 
        userId: newUser.id, 
        username: formData.username,
        rolesCount: selectedRoles.length,
      });

      onUserCreated && onUserCreated(newUser);
      onClose();

    } catch (err) {
      logger.error('Failed to create user', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se vytvořit uživatele');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        Vytvořit nového uživatele
      </DialogTitle>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          {/* Username */}
          <TextField
            label="Uživatelské jméno *"
            value={formData.username}
            onChange={handleChange('username')}
            error={!!formErrors.username}
            helperText={formErrors.username}
            fullWidth
            autoFocus
            disabled={loading}
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

          {/* Password */}
          <TextField
            label="Heslo *"
            type="password"
            value={formData.password}
            onChange={handleChange('password')}
            error={!!formErrors.password}
            helperText={formErrors.password || 'Minimálně 8 znaků'}
            fullWidth
            disabled={loading}
          />

          {/* Roles */}
          <Autocomplete
            multiple
            options={availableRoles}
            getOptionLabel={(option) => option.name}
            value={selectedRoles}
            onChange={(event, newValue) => setSelectedRoles(newValue)}
            loading={loadingRoles}
            disabled={loading}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Role"
                placeholder="Vyberte role"
                helperText="Uživatel může mít více rolí"
              />
            )}
            renderTags={(value, getTagProps) =>
              value.map((option, index) => (
                <Chip
                  label={option.name}
                  {...getTagProps({ index })}
                  size="small"
                  key={option.id}
                />
              ))
            }
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
                  checked={formData.temporaryPassword}
                  onChange={handleChange('temporaryPassword')}
                  disabled={loading}
                />
              }
              label="Vyžadovat změnu hesla při prvním přihlášení"
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
          Vytvořit uživatele
        </Button>
      </DialogActions>
    </Dialog>
  );
};

CreateUserDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onUserCreated: PropTypes.func,
};

export default CreateUserDialog;
