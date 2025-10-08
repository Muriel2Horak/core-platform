import { useState, useEffect } from 'react';
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

const EditGroupDialog = ({ open, group, onClose, onGroupUpdated }) => {
  const [formData, setFormData] = useState({
    name: '',
    path: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (group && open) {
      setFormData({
        name: group.name || '',
        path: group.path || '',
      });
    }
  }, [group, open]);

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
      };

      await apiService.updateGroup(group.name, groupData);
      logger.userAction('GROUP_UPDATED', { name: group.name });

      onGroupUpdated();
      onClose();
    } catch (err) {
      console.error('Failed to update group:', err);
      setError('Nepodařilo se aktualizovat skupinu: ' + err.message);
      logger.error('GROUP_UPDATE_ERROR', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    if (!loading) {
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
        ✏️ Upravit skupinu
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
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: 2 } }}
          />

          <TextField
            label="Cesta"
            value={formData.path}
            onChange={(e) => handleChange('path', e.target.value)}
            fullWidth
            disabled={loading}
            helperText="Hierarchická cesta skupiny"
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
          {loading ? 'Ukládání...' : 'Uložit'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

EditGroupDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  group: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onGroupUpdated: PropTypes.func.isRequired
};

export default EditGroupDialog;
