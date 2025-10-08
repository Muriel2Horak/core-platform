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
} from '@mui/material';
import {
  Add as AddIcon,
  Close as CloseIcon,
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';

export const CreateTenantDialog = ({ open, onClose, onSuccess }) => {
  const [formData, setFormData] = useState({
    key: '',
    displayName: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [formErrors, setFormErrors] = useState({});

  const validateForm = () => {
    const errors = {};

    if (!formData.key || formData.key.length < 3) {
      errors.key = 'Key musí mít alespoň 3 znaky';
    }

    if (formData.key && !/^[a-z0-9-]+$/.test(formData.key)) {
      errors.key = 'Key může obsahovat pouze malá písmena, číslice a pomlčky';
    }

    if (!formData.displayName) {
      errors.displayName = 'Název je povinný';
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

      logger.userAction('TENANT_CREATE_ATTEMPT', { key: formData.key });

      await apiService.createTenant({
        key: formData.key,
        displayName: formData.displayName,
      });

      logger.userAction('TENANT_CREATED', { key: formData.key });

      onSuccess && onSuccess();
      handleClose();

    } catch (err) {
      logger.error('Failed to create tenant', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se vytvořit tenant');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setFormData({
      key: '',
      displayName: '',
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
    <Dialog 
      open={open} 
      onClose={handleClose} 
      maxWidth="sm" 
      fullWidth
      PaperProps={{
        sx: {
          background: 'rgba(255, 255, 255, 0.95) !important',
          backdropFilter: 'blur(10px)',
          borderRadius: 2,
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
        }
      }}
    >
      <DialogTitle>Vytvořit nový tenant</DialogTitle>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Alert severity="info" sx={{ mb: 2 }}>
          Vytvoření tenantu automaticky založí izolovaný Keycloak realm a tenant admin účet.
        </Alert>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
          <TextField
            label="Tenant Key *"
            value={formData.key}
            onChange={(e) => handleInputChange('key', e.target.value.toLowerCase())}
            error={!!formErrors.key}
            helperText={formErrors.key || 'URL-safe identifikátor (např.: company-a)'}
            fullWidth
            autoFocus
            disabled={loading}
            placeholder="company-a"
          />

          <TextField
            label="Název tenantu *"
            value={formData.displayName}
            onChange={(e) => handleInputChange('displayName', e.target.value)}
            error={!!formErrors.displayName}
            helperText={formErrors.displayName || 'Přátelský název (např.: Company A s.r.o.)'}
            fullWidth
            disabled={loading}
            placeholder="Company A s.r.o."
          />

          <Alert severity="warning">
            Tento proces vytvoří:
            <ul style={{ margin: '8px 0 0 0', paddingLeft: '20px' }}>
              <li>Nový Keycloak realm ({formData.key || 'tenant-key'})</li>
              <li>Tenant admin uživatel</li>
              <li>Subdoménu: {formData.key || 'tenant-key'}.core-platform.local</li>
            </ul>
          </Alert>
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
          disabled={loading || !formData.key || !formData.displayName}
          startIcon={loading ? <CircularProgress size={20} /> : <AddIcon />}
        >
          Vytvořit tenant
        </Button>
      </DialogActions>
    </Dialog>
  );
};

CreateTenantDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func,
};
