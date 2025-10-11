import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Alert,
  CircularProgress,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Checkbox,
  ListItemText,
  OutlinedInput,
  SelectChangeEvent,
} from '@mui/material';
import { Save, Cancel } from '@mui/icons-material';
import axios from 'axios';
import { usePresence } from '../../lib/presence/usePresence';
import { PresenceIndicator } from '../../components/presence/PresenceIndicator';

interface Role {
  id: string;
  name: string;
  description?: string;
  permissions: string[];
}

// Available permissions in the system
const AVAILABLE_PERMISSIONS = [
  'USER_READ',
  'USER_WRITE',
  'USER_DELETE',
  'ROLE_READ',
  'ROLE_WRITE',
  'ROLE_DELETE',
  'TENANT_READ',
  'TENANT_WRITE',
  'TENANT_DELETE',
  'AUDIT_READ',
  'AUDIT_EXPORT',
  'SYSTEM_ADMIN',
];

/**
 * Role Edit Page with Real-Time Presence Tracking
 * 
 * Demonstrates S2 presence system integration:
 * - Shows who else is viewing this role
 * - Stale mode warning when role is being modified elsewhere
 * - Auto-save on blur (debounced 500ms)
 * - Immediate save on permission changes
 */
export const RoleEditPage = ({ currentUser }: { currentUser: any }) => {
  const { roleId } = useParams<{ roleId: string }>();
  const navigate = useNavigate();

  const [role, setRole] = useState<Role | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveTimer, setSaveTimer] = useState<NodeJS.Timeout | null>(null);

  // S2: Real-time presence tracking
  const { presence, acquireLock, releaseLock, error: presenceError } = usePresence(
    {
      entity: 'Role',
      id: roleId || '',
      tenantId: currentUser?.tenantKey || '',
      userId: currentUser?.id || '',
    },
    {
      enabled: !!roleId && !!currentUser?.id,
      wsUrl: 'ws://localhost:8080/ws/presence',
    }
  );

  // Fetch role data
  useEffect(() => {
    const fetchRole = async () => {
      try {
        setLoading(true);
        const response = await axios.get(`/api/roles/${roleId}`);
        setRole(response.data);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to fetch role');
      } finally {
        setLoading(false);
      }
    };

    if (roleId) {
      fetchRole();
    }
  }, [roleId]);

  // Cleanup save timer on unmount
  useEffect(() => {
    return () => {
      if (saveTimer) {
        clearTimeout(saveTimer);
      }
    };
  }, [saveTimer]);

  const handleFieldFocus = (fieldName: string) => {
    acquireLock(fieldName);
  };

  const handleFieldBlur = async (fieldName: string) => {
    releaseLock(fieldName);

    // Auto-save on blur (debounced 500ms)
    if (saveTimer) {
      clearTimeout(saveTimer);
    }

    const timer = setTimeout(async () => {
      await handleSave(false); // Silent save (no success snackbar)
    }, 500);

    setSaveTimer(timer);
  };

  const handleFieldChange = (fieldName: keyof Role, value: string) => {
    if (role) {
      setRole({ ...role, [fieldName]: value });
    }
  };

  const handlePermissionsChange = (event: SelectChangeEvent<string[]>) => {
    const value = event.target.value;
    if (role) {
      setRole({
        ...role,
        permissions: typeof value === 'string' ? value.split(',') : value,
      });
      // Trigger immediate save for permission changes
      setTimeout(async () => {
        await handleSave(true);
      }, 100);
    }
  };

  const handleSave = async (showSnackbar = true) => {
    if (!role) return;

    try {
      setSaving(true);
      await axios.put(`/api/roles/${roleId}`, role);
      if (showSnackbar) {
        // Success handled by WithPresenceFeedback wrapper (future)
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save role');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    navigate('/core-admin/roles');
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error && !role) {
    return (
      <Box p={3}>
        <Alert severity="error">{error}</Alert>
        <Button variant="outlined" onClick={handleCancel} sx={{ mt: 2 }}>
          Back to Roles
        </Button>
      </Box>
    );
  }

  if (!role) {
    return (
      <Box p={3}>
        <Alert severity="warning">Role not found</Alert>
        <Button variant="outlined" onClick={handleCancel} sx={{ mt: 2 }}>
          Back to Roles
        </Button>
      </Box>
    );
  }

  const isStale = presence.stale;
  const busyBy = presence.busyBy;

  return (
    <Box p={3}>
      {/* Header with Presence Indicator */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5">Edit Role: {role.name}</Typography>
        <PresenceIndicator presence={presence} currentUserId={currentUser?.id} />
      </Box>

      {/* Error Alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      {/* Presence Error */}
      {presenceError && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Presence tracking unavailable: {presenceError}
        </Alert>
      )}

      {/* Stale Mode Warning */}
      {isStale && (
        <Alert severity="warning" sx={{ mb: 2 }} data-testid="stale-warning">
          ⚠️ This role is currently being modified by <strong>{busyBy}</strong>. Your changes may conflict.
        </Alert>
      )}

      <Paper elevation={2} sx={{ p: 3 }}>
        {/* Form Fields */}
        <Box component="form" noValidate sx={{ mt: 2 }}>
          {/* Name Field */}
          <TextField
            fullWidth
            label="Role Name"
            value={role.name}
            onChange={(e) => handleFieldChange('name', e.target.value)}
            onFocus={() => handleFieldFocus('name')}
            onBlur={() => handleFieldBlur('name')}
            disabled={isStale}
            margin="normal"
            required
            data-testid="field-name"
          />

          {/* Description Field */}
          <TextField
            fullWidth
            label="Description"
            value={role.description || ''}
            onChange={(e) => handleFieldChange('description', e.target.value)}
            onFocus={() => handleFieldFocus('description')}
            onBlur={() => handleFieldBlur('description')}
            disabled={isStale}
            margin="normal"
            multiline
            rows={3}
            data-testid="field-description"
          />

          {/* Permissions Multi-Select */}
          <FormControl fullWidth margin="normal">
            <InputLabel id="permissions-label">Permissions</InputLabel>
            <Select
              labelId="permissions-label"
              multiple
              value={role.permissions}
              onChange={handlePermissionsChange}
              input={<OutlinedInput label="Permissions" />}
              renderValue={(selected) => selected.join(', ')}
              disabled={isStale}
              data-testid="field-permissions"
            >
              {AVAILABLE_PERMISSIONS.map((permission) => (
                <MenuItem key={permission} value={permission}>
                  <Checkbox checked={role.permissions.includes(permission)} />
                  <ListItemText primary={permission} />
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          {/* Action Buttons */}
          <Box display="flex" gap={2} mt={3}>
            <Button
              variant="contained"
              color="primary"
              startIcon={saving ? <CircularProgress size={20} /> : <Save />}
              onClick={() => handleSave(true)}
              disabled={saving}
            >
              {saving ? 'Saving...' : 'Save'}
            </Button>
            <Button
              variant="outlined"
              startIcon={<Cancel />}
              onClick={handleCancel}
              disabled={saving}
            >
              Cancel
            </Button>
          </Box>
        </Box>
      </Paper>
    </Box>
  );
};
