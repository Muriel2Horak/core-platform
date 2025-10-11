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
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  SelectChangeEvent,
} from '@mui/material';
import { Save, Cancel } from '@mui/icons-material';
import axios from 'axios';
import { usePresence } from '../../lib/presence/usePresence';
import { PresenceIndicator } from '../../components/presence/PresenceIndicator';

interface Tenant {
  id: string;
  name: string;
  slug: string;
  status: 'ACTIVE' | 'INACTIVE';
  description?: string;
}

/**
 * Tenant Edit Page with Real-Time Presence Tracking
 * 
 * Demonstrates S2 presence system integration:
 * - Shows who else is viewing this tenant
 * - Stale mode warning when tenant is being modified elsewhere
 * - Auto-save on blur (debounced 500ms)
 */
export const TenantEditPage = ({ currentUser }: { currentUser: any }) => {
  const { tenantId } = useParams<{ tenantId: string }>();
  const navigate = useNavigate();

  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveTimer, setSaveTimer] = useState<NodeJS.Timeout | null>(null);

  // S2: Real-time presence tracking
  const { presence, acquireLock, releaseLock, error: presenceError } = usePresence(
    {
      entity: 'Tenant',
      id: tenantId || '',
      tenantId: tenantId || '', // Editing tenant itself
      userId: currentUser?.id || '',
    },
    {
      enabled: !!tenantId && !!currentUser?.id,
      wsUrl: 'ws://localhost:8080/ws/presence',
    }
  );

  // Fetch tenant data
  useEffect(() => {
    const fetchTenant = async () => {
      try {
        setLoading(true);
        const response = await axios.get(`/api/tenants/${tenantId}`);
        setTenant(response.data);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to fetch tenant');
      } finally {
        setLoading(false);
      }
    };

    if (tenantId) {
      fetchTenant();
    }
  }, [tenantId]);

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

  const handleFieldChange = (fieldName: keyof Tenant, value: string) => {
    if (tenant) {
      setTenant({ ...tenant, [fieldName]: value });
    }
  };

  const handleStatusChange = (event: SelectChangeEvent<'ACTIVE' | 'INACTIVE'>) => {
    if (tenant) {
      setTenant({ ...tenant, status: event.target.value as 'ACTIVE' | 'INACTIVE' });
      // Trigger immediate save for status changes
      setTimeout(async () => {
        await handleSave(true);
      }, 100);
    }
  };

  const handleSave = async (showSnackbar = true) => {
    if (!tenant) return;

    try {
      setSaving(true);
      await axios.put(`/api/tenants/${tenantId}`, tenant);
      if (showSnackbar) {
        // Snackbar handled by WithPresenceFeedback wrapper
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save tenant');
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    navigate('/admin/tenants');
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  if (error && !tenant) {
    return (
      <Box p={3}>
        <Alert severity="error">{error}</Alert>
        <Button variant="outlined" onClick={handleCancel} sx={{ mt: 2 }}>
          Back to Tenants
        </Button>
      </Box>
    );
  }

  if (!tenant) {
    return (
      <Box p={3}>
        <Alert severity="warning">Tenant not found</Alert>
        <Button variant="outlined" onClick={handleCancel} sx={{ mt: 2 }}>
          Back to Tenants
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
        <Typography variant="h5">Edit Tenant: {tenant.name}</Typography>
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
          ⚠️ This tenant is currently being modified by <strong>{busyBy}</strong>. Your changes may conflict.
        </Alert>
      )}

      <Paper elevation={2} sx={{ p: 3 }}>
        {/* Form Fields */}
        <Box component="form" noValidate sx={{ mt: 2 }}>
          {/* Name Field */}
          <TextField
            fullWidth
            label="Tenant Name"
            value={tenant.name}
            onChange={(e) => handleFieldChange('name', e.target.value)}
            onFocus={() => handleFieldFocus('name')}
            onBlur={() => handleFieldBlur('name')}
            disabled={isStale}
            margin="normal"
            required
            data-testid="field-name"
          />

          {/* Slug Field */}
          <TextField
            fullWidth
            label="Slug (URL-safe identifier)"
            value={tenant.slug}
            onChange={(e) => handleFieldChange('slug', e.target.value)}
            onFocus={() => handleFieldFocus('slug')}
            onBlur={() => handleFieldBlur('slug')}
            disabled={isStale}
            margin="normal"
            required
            helperText="Lowercase letters, numbers, and hyphens only"
            data-testid="field-slug"
          />

          {/* Status Field */}
          <FormControl fullWidth margin="normal">
            <InputLabel>Status</InputLabel>
            <Select
              value={tenant.status}
              onChange={handleStatusChange}
              label="Status"
            >
              <MenuItem value="ACTIVE">Active</MenuItem>
              <MenuItem value="INACTIVE">Inactive</MenuItem>
            </Select>
          </FormControl>

          {/* Description Field */}
          <TextField
            fullWidth
            label="Description"
            value={tenant.description || ''}
            onChange={(e) => handleFieldChange('description', e.target.value)}
            onFocus={() => handleFieldFocus('description')}
            onBlur={() => handleFieldBlur('description')}
            disabled={isStale}
            margin="normal"
            multiline
            rows={4}
            data-testid="field-description"
          />

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
