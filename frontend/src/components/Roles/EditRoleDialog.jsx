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
} from '@mui/material';
import {
  Save as SaveIcon,
  Close as CloseIcon,
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';

/**
 * ✏️ Edit Role Dialog
 * Dialog pro editaci existujících rolí
 */
export const EditRoleDialog = ({ open, role, onClose, onSuccess }) => {
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (role) {
      setDescription(role.description || '');
    }
  }, [role]);

  const handleSubmit = async () => {
    try {
      setLoading(true);
      setError(null);

      logger.userAction('ROLE_UPDATE_ATTEMPT', { name: role.name });

      await apiService.updateRole(role.name, {
        name: role.name, // název se nemění
        description: description,
        composite: role.composite,
      });

      logger.userAction('ROLE_UPDATED', { name: role.name });

      onSuccess && onSuccess();
      handleClose();

    } catch (err) {
      logger.error('Failed to update role', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se aktualizovat roli');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setDescription('');
    setError(null);
    onClose();
  };

  if (!role) return null;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Upravit roli: {role.name}</DialogTitle>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Alert severity="info" sx={{ mb: 2 }}>
          Název role nelze změnit. Pro změnu hierarchie použijte "Spravovat hierarchii".
        </Alert>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
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
            disabled={loading}
            autoFocus
          />
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
          disabled={loading}
          startIcon={loading ? <CircularProgress size={20} /> : <SaveIcon />}
        >
          Uložit změny
        </Button>
      </DialogActions>
    </Dialog>
  );
};

EditRoleDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  role: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func,
};
