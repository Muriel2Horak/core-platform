import { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  DialogContentText,
  Button,
  TextField,
  FormControlLabel,
  Switch,
  Box,
  Alert,
  CircularProgress,
} from '@mui/material';
import {
  Delete as DeleteIcon,
  Close as CloseIcon,
  LockReset as LockResetIcon,
} from '@mui/icons-material';
import apiService from '../../services/api.js';
import logger from '../../services/logger.js';
import PropTypes from 'prop-types';

/**
 * 🗑️ Delete User Confirmation Dialog
 */
export const DeleteUserDialog = ({ open, user, onClose, onUserDeleted }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [confirmText, setConfirmText] = useState('');

  const handleDelete = async () => {
    if (confirmText !== user?.username) {
      setError('Zadejte přesně uživatelské jméno pro potvrzení');
      return;
    }

    try {
      setLoading(true);
      setError(null);

      logger.userAction('USER_DELETE_ATTEMPT', { userId: user.id, username: user.username });

      await apiService.deleteUser(user.id);

      logger.userAction('USER_DELETED', { userId: user.id, username: user.username });

      onUserDeleted && onUserDeleted(user);
      onClose();

    } catch (err) {
      logger.error('Failed to delete user', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se smazat uživatele');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setConfirmText('');
    setError(null);
    onClose();
  };

  if (!user) return null;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ color: 'error.main' }}>
        Smazat uživatele
      </DialogTitle>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <DialogContentText>
          Opravdu chcete smazat uživatele <strong>{user.username}</strong>?
        </DialogContentText>

        <DialogContentText sx={{ mt: 2, color: 'error.main' }}>
          ⚠️ Tato akce je nevratná! Všechna data tohoto uživatele budou trvale odstraněna.
        </DialogContentText>

        <TextField
          label="Potvrďte zadáním uživatelského jména"
          value={confirmText}
          onChange={(e) => setConfirmText(e.target.value)}
          fullWidth
          sx={{ mt: 3 }}
          placeholder={user.username}
          disabled={loading}
          helperText={`Napište "${user.username}" pro potvrzení`}
        />
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
          disabled={loading || confirmText !== user.username}
          startIcon={loading ? <CircularProgress size={20} /> : <DeleteIcon />}
        >
          Smazat uživatele
        </Button>
      </DialogActions>
    </Dialog>
  );
};

DeleteUserDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  user: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onUserDeleted: PropTypes.func,
};

/**
 * 🔐 Reset Password Dialog
 */
export const ResetPasswordDialog = ({ open, user, onClose, onPasswordReset }) => {
  const [password, setPassword] = useState('');
  const [temporaryPassword, setTemporaryPassword] = useState(true);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [formErrors, setFormErrors] = useState({});

  const validatePassword = () => {
    const errors = {};
    
    if (!password || password.length < 8) {
      errors.password = 'Heslo musí mít alespoň 8 znaků';
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleReset = async () => {
    if (!validatePassword()) {
      return;
    }

    try {
      setLoading(true);
      setError(null);

      logger.userAction('PASSWORD_RESET_ATTEMPT', { userId: user.id, username: user.username });

      await apiService.resetUserPassword(user.id, {
        newPassword: password,
        requirePasswordChange: temporaryPassword,
      });

      logger.userAction('PASSWORD_RESET', { userId: user.id, temporary: temporaryPassword });

      onPasswordReset && onPasswordReset(user);
      handleClose();

    } catch (err) {
      logger.error('Failed to reset password', { error: err.message });
      setError(err.response?.data?.message || 'Nepodařilo se resetovat heslo');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setPassword('');
    setTemporaryPassword(true);
    setError(null);
    setFormErrors({});
    onClose();
  };

  if (!user) return null;

  return (
    <Dialog open={open} onClose={handleClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        Resetovat heslo: {user.username}
      </DialogTitle>
      
      <DialogContent>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        <DialogContentText sx={{ mb: 3 }}>
          Nastavte nové heslo pro uživatele <strong>{user.username}</strong>.
        </DialogContentText>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <TextField
            label="Nové heslo *"
            type="password"
            value={password}
            onChange={(e) => {
              setPassword(e.target.value);
              if (formErrors.password) {
                setFormErrors({});
              }
            }}
            error={!!formErrors.password}
            helperText={formErrors.password || 'Minimálně 8 znaků'}
            fullWidth
            autoFocus
            disabled={loading}
          />

          <FormControlLabel
            control={
              <Switch
                checked={temporaryPassword}
                onChange={(e) => setTemporaryPassword(e.target.checked)}
                disabled={loading}
              />
            }
            label="Vyžadovat změnu hesla při příštím přihlášení"
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
          onClick={handleReset}
          variant="contained"
          disabled={loading}
          startIcon={loading ? <CircularProgress size={20} /> : <LockResetIcon />}
        >
          Resetovat heslo
        </Button>
      </DialogActions>
    </Dialog>
  );
};

ResetPasswordDialog.propTypes = {
  open: PropTypes.bool.isRequired,
  user: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  onPasswordReset: PropTypes.func,
};
