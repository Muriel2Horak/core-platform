import { useState } from 'react';
import PropTypes from 'prop-types';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
  TextField,
  Alert,
  CircularProgress,
  Box,
} from '@mui/material';
import {
  Delete as DeleteIcon,
  Close as CloseIcon,
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';

/**
 * 🗑️ Delete Role Dialog
 * Dialog pro bezpečné mazání rolí s potvrzením
 */
export const DeleteRoleDialog = ({ open, role, onClose, onSuccess }) => {
  const [confirmText, setConfirmText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleDelete = async () => {
    if (confirmText !== role.name) {
      setError('Název role se neshoduje');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      logger.userAction('ROLE_DELETE_ATTEMPT', { name: role.name });

      await apiService.deleteRole(role.name);

      logger.userAction('ROLE_DELETED', { name: role.name });

      onSuccess && onSuccess();
      handleClose();

    } catch (err) {
      logger.error('Failed to delete role', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se smazat roli');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setConfirmText('');
    setError(null);
    onClose();
  };

  if (!role) return null;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>Smazat roli</DialogTitle>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Alert severity="warning" sx={{ mb: 2 }}>
          Tato akce je nevratná! Role bude odstraněna ze všech uživatelů.
        </Alert>

        <DialogContentText sx={{ mb: 3 }}>
          Opravdu chcete smazat roli <strong>{role.name}</strong>?
          {role.description && (
            <>
              <br />
              <em>({role.description})</em>
            </>
          )}
        </DialogContentText>

        <Box sx={{ mb: 2 }}>
          <DialogContentText sx={{ mb: 1 }}>
            Pro potvrzení zadejte přesný název role:
          </DialogContentText>
          <TextField
            fullWidth
            value={confirmText}
            onChange={(e) => setConfirmText(e.target.value)}
            placeholder={role.name}
            autoFocus
            disabled={loading}
            error={confirmText.length > 0 && confirmText !== role.name}
            helperText={confirmText.length > 0 && confirmText !== role.name ? 'Název se neshoduje' : ''}
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
          onClick={handleDelete}
          variant="contained"
          color="error"
          disabled={loading || confirmText !== role.name}
          startIcon={loading ? <CircularProgress size={20} /> : <DeleteIcon />}
        >
          Smazat roli
        </Button>
      </DialogActions>
    </Dialog>
  );
};

DeleteRoleDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  role: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func,
};
