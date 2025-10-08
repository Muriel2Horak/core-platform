import { useState } from 'react';
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
  FormControlLabel,
  Switch,
} from '@mui/material';
import {
  Add as AddIcon,
  Close as CloseIcon,
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';
import { CompositeRoleBuilder } from './CompositeRoleBuilder.jsx';

/**
 * 🆕 Create Role Dialog
 * Dialog pro vytváření nových rolí
 */
export const CreateRoleDialog = ({ open, onClose, onSuccess, tenantKey }) => {
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    composite: false,
    childRoles: [],
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [formErrors, setFormErrors] = useState({});

  const validateForm = () => {
    const errors = {};

    if (!formData.name || formData.name.length < 3) {
      errors.name = 'Název musí mít alespoň 3 znaky';
    }

    // Validace názvu - měl by být ve formátu UPPER_CASE nebo začínat CORE_ROLE_
    if (formData.name && !formData.name.startsWith('CORE_ROLE_')) {
      errors.name = 'Název role musí začínat "CORE_ROLE_"';
    }

    if (formData.name && !/^[A-Z_]+$/.test(formData.name)) {
      errors.name = 'Název může obsahovat pouze velká písmena a podtržítka';
    }

    // Validace composite role - musí mít alespoň 1 child roli
    if (formData.composite && (!formData.childRoles || formData.childRoles.length === 0)) {
      errors.childRoles = 'Composite role musí obsahovat alespoň jednu podřízenou roli';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validateForm()) {
      return;
    }

    try {
      setLoading(true);
      setError(null);

      logger.userAction('ROLE_CREATE_ATTEMPT', { name: formData.name, tenantKey });

      await apiService.createRole({
        name: formData.name,
        description: formData.description,
        composite: formData.composite,
        childRoles: formData.composite ? formData.childRoles : undefined,
        tenantKey: tenantKey, // Přidáme tenantKey pro multi-tenant podporu
      });

      logger.userAction('ROLE_CREATED', { name: formData.name, tenantKey });

      onSuccess && onSuccess();
      handleClose();

    } catch (err) {
      logger.error('Failed to create role', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se vytvořit roli');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setFormData({
      name: '',
      description: '',
      composite: false,
      childRoles: [],
    });
    setError(null);
    setFormErrors({});
    onClose();
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    if (formErrors[field]) {
      setFormErrors(prev => {
        const newErrors = { ...prev };
        delete newErrors[field];
        return newErrors;
      });
    }
  };

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Vytvořit novou roli</DialogTitle>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField
            label="Název role *"
            value={formData.name}
            onChange={(e) => handleInputChange('name', e.target.value.toUpperCase())}
            error={!!formErrors.name}
            helperText={formErrors.name || 'Např.: CORE_ROLE_DEVELOPER'}
            fullWidth
            autoFocus
            disabled={loading}
            placeholder="CORE_ROLE_"
          />

          <TextField
            label="Popis"
            value={formData.description}
            onChange={(e) => handleInputChange('description', e.target.value)}
            multiline
            rows={3}
            fullWidth
            disabled={loading}
            helperText="Volitelný popis účelu role"
          />

          <FormControlLabel
            control={
              <Switch
                checked={formData.composite}
                onChange={(e) => handleInputChange('composite', e.target.checked)}
                disabled={loading}
              />
            }
            label="Composite role (může obsahovat další role)"
          />

          {formData.composite && (
            <Box sx={{ mt: 2 }}>
              <CompositeRoleBuilder
                selectedRoles={formData.childRoles}
                onRolesChange={(roles) => handleInputChange('childRoles', roles)}
                excludeRoles={[formData.name]}
                disabled={loading}
              />
              {formErrors.childRoles && (
                <Alert severity="error" sx={{ mt: 1 }}>
                  {formErrors.childRoles}
                </Alert>
              )}
            </Box>
          )}
        </Box>
      </DialogContent>

      <DialogActions>
        <Button 
          onClick={handleClose} 
          disabled={loading}
          startIcon={<CloseIcon />}
        >
          Zrušit
        </Button>
        <Button 
          onClick={handleSubmit}
          variant="contained"
          disabled={loading || !formData.name}
          startIcon={loading ? <CircularProgress size={20} /> : <AddIcon />}
        >
          Vytvořit roli
        </Button>
      </DialogActions>
    </Dialog>
  );
};

CreateRoleDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func,
  tenantKey: PropTypes.string,
};
