import PropTypes from 'prop-types';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Alert,
  Typography,
  Box
} from '@mui/material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';
import { useState } from 'react';

const DeleteGroupDialog = ({ open, group, onClose, onGroupDeleted }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleDelete = async () => {
    try {
      setLoading(true);
      setError(null);

      await apiService.deleteGroup(group.name);
      logger.userAction('GROUP_DELETED', { name: group.name });

      onGroupDeleted();
      onClose();
    } catch (err) {
      console.error('Failed to delete group:', err);
      setError('Nepodařilo se smazat skupinu: ' + err.message);
      logger.error('GROUP_DELETE_ERROR', err.message);
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
        🗑️ Smazat skupinu
      </DialogTitle>

      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>
            {error}
          </Alert>
        )}

        <Box>
          <Typography variant="body1" gutterBottom>
            Opravdu chcete smazat skupinu <strong>{group?.name}</strong>?
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Tato akce je nevratná. Všichni členové budou ze skupiny odebráni.
          </Typography>
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
          onClick={handleDelete}
          variant="contained"
          color="error"
          disabled={loading}
          sx={{ borderRadius: 2 }}
        >
          {loading ? 'Mazání...' : 'Smazat'}
        </Button>
      </DialogActions>
    </Dialog>
  );
};

DeleteGroupDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  group: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onGroupDeleted: PropTypes.func.isRequired
};

export default DeleteGroupDialog;
