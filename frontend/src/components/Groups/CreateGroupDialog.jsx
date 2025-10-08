import { useState } from 'react';
import PropTypes from 'prop-types';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Alert,
  Box
} from '@mui/material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';

const CreateGroupDialog = ({ open, onClose, onGroupCreated, tenantKey }) => {
  const [formData, setFormData] = useState({
    name: '',
    path: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
    setError(null);
  };

  const handleSubmit = async () => {
    if (!formData.name.trim()) {
      setError('Název skupiny je povinný');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      const groupData = {
        name: formData.name.trim(),
        path: formData.path.trim() || `/${formData.name.trim()}`,
        tenantKey: tenantKey || undefined
      };

      await apiService.createGroup(groupData);
      logger.userAction('GROUP_CREATED', { name: groupData.name });

      setFormData({ name: '', path: '' });
      onGroupCreated();
      onClose();
    } catch (err) {
      console.error('Failed to create group:', err);
      setError('Nepodařilo se vytvořit skupinu: ' + err.message);
      logger.error('GROUP_CREATE_ERROR', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    if (!loading) {
      setFormData({ name: '', path: '' });
      setError(null);
      onClose();
    }
  };

  return (
    <Dialog
      open={open}
      onClose={handleClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{ sx: { borderRadius: 3 } }}
    >
      <DialogTitle sx={{ fontWeight: 600, fontSize: '1.5rem' }}>
        ➕ Vytvořit novou skupinu
      </DialogTitle>

      <DialogContent sx={{ pt: 2 }}>
        {error && (
          <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>
            {error}
          </Alert>
        )}

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            label="Název skupiny *"
            value={formData.name}
            onChange={(e) => handleChange('name', e.target.value)}
            fullWidth
            disabled={loading}
            helperText="Např: developers, managers, hr-team"
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
          />

          <TextField
            label="Cesta"
            value={formData.path}
            onChange={(e) => handleChange('path', e.target.value)}
            fullWidth
            disabled={loading}
            helperText="Volitelná hierarchická cesta, např: /company/it/developers"
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
          />
        </Box>
      </DialogContent>

      <DialogActions sx={{ p: 3 }}>
        <Button
          onClick={handleClose}
          disabled={loading}
          sx={{ borderRadius: 2 }}
        >
          Zrušit
        </Button>
        <Button
          onClick={handleSubmit}
          variant="contained"
          disabled={loading}
          sx={{ borderRadius: 2 }}
        >
          {loading ? 'Vytváření...' : 'Vytvořit'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

CreateGroupDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onGroupCreated: PropTypes.func.isRequired,
  tenantKey: PropTypes.string
};

export default CreateGroupDialog;
