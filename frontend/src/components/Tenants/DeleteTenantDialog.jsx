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

export const DeleteTenantDialog = ({ open, tenant, onClose, onSuccess }) => {
  const [confirmText, setConfirmText] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleDelete = async () => {
    if (confirmText !== tenant.key) {
      setError('Tenant key se neshoduje');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      logger.userAction('TENANT_DELETE_ATTEMPT', { key: tenant.key });

      await apiService.deleteTenant(tenant.key);

      logger.userAction('TENANT_DELETED', { key: tenant.key });

      onSuccess && onSuccess();
      handleClose();

    } catch (err) {
      logger.error('Failed to delete tenant', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se smazat tenant');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setConfirmText('');
    setError(null);
    onClose();
  };

  if (!tenant) return null;

  return (
    <Dialog 
      open={open} 
      onClose={handleClose} 
      maxWidth="sm" 
      fullWidth
      PaperProps={{
        sx: {
          background: 'rgba(255, 255, 255, 0.95)',
          backdropFilter: 'blur(10px)',
          borderRadius: 2,
        }
      }}
    >
      <DialogTitle>Smazat tenant</DialogTitle>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <Alert severity="error" sx={{ mb: 2 }}>
          ⚠️ VAROVÁNÍ: Tato akce je nevratná!
        </Alert>

        <Alert severity="warning" sx={{ mb: 2 }}>
          Smazání tenantu odstraní:
          <ul style={{ margin: '8px 0 0 0', paddingLeft: '20px' }}>
            <li>Keycloak realm ({tenant.key})</li>
            <li>Všechny uživatele tenantu</li>
            <li>Všechna data tenantu</li>
            <li>Grafana organizaci (pokud existuje)</li>
          </ul>
        </Alert>

        <DialogContentText sx={{ mb: 3 }}>
          Opravdu chcete smazat tenant <strong>{tenant.displayName || tenant.key}</strong>?
        </DialogContentText>

        <Box sx={{ mb: 2 }}>
          <DialogContentText sx={{ mb: 1 }}>
            Pro potvrzení zadejte přesný tenant key:
          </DialogContentText>
          <TextField
            fullWidth
            value={confirmText}
            onChange={(e) => setConfirmText(e.target.value)}
            placeholder={tenant.key}
            autoFocus
            disabled={loading}
            error={confirmText.length > 0 && confirmText !== tenant.key}
            helperText={confirmText.length > 0 && confirmText !== tenant.key ? 'Key se neshoduje' : ''}
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
          disabled={loading || confirmText !== tenant.key}
          startIcon={loading ? <CircularProgress size={20} /> : <DeleteIcon />}
        >
          Smazat tenant
        </Button>
      </DialogActions>
    </Dialog>
  );
};

DeleteTenantDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  tenant: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onSuccess: PropTypes.func,
};
