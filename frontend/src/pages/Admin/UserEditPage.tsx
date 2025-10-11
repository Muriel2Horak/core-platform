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
  Chip,
  Avatar,
  Tooltip,
} from '@mui/material';
import { Save, Cancel, Lock, Visibility, Edit } from '@mui/icons-material';
import axios from 'axios';
import { usePresence } from '../../lib/presence/usePresence';

interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  tenantKey: string;
  roles: string[];
}

/**
 * User Edit Page with Real-Time Presence Tracking
 * 
 * Demonstrates S2 presence system integration:
 * - Shows who else is viewing this user
 * - Field-level locks on focus
 * - Stale mode warning when user is being modified elsewhere
 */
export const UserEditPage = ({ currentUser }: { currentUser: any }) => {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();

  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [focusedField, setFocusedField] = useState<string | null>(null);

  // S2: Real-time presence tracking
  const { presence, acquireLock, releaseLock, error: presenceError } = usePresence(
    {
      entity: 'User',
      id: userId || '',
      tenantId: currentUser?.tenantKey || '',
      userId: currentUser?.id || '',
    },
    {
      enabled: !!userId && !!currentUser?.id,
      wsUrl: 'ws://localhost:8080/ws/presence',
    }
  );

  // Fetch user data
  useEffect(() => {
    const fetchUser = async () => {
      try {
        setLoading(true);
        const response = await axios.get(`/api/users/${userId}`);
        setUser(response.data);
      } catch (err: any) {
        setError(err.response?.data?.message || 'Failed to fetch user');
      } finally {
        setLoading(false);
      }
    };

    if (userId) {
      fetchUser();
    }
  }, [userId]);

  const handleFieldFocus = (fieldName: string) => {
    setFocusedField(fieldName);
    acquireLock(fieldName);
  };

  const handleFieldBlur = (fieldName: string) => {
    setFocusedField(null);
    releaseLock(fieldName);
  };

  const handleFieldChange = (fieldName: keyof User, value: string) => {
    if (user) {
      setUser({ ...user, [fieldName]: value });
    }
  };

  const handleSave = async () => {
    if (!user) return;

    try {
      setSaving(true);
      await axios.put(`/api/users/${userId}`, user);
      navigate('/admin/users');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save user');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '50vh' }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error && !user) {
    return (
      <Alert severity="error" sx={{ m: 3 }}>
        {error}
      </Alert>
    );
  }

  const viewingUsers = presence.users.filter((u) => typeof u === 'string' && u !== currentUser?.id) as string[];
  const isStale = presence.stale;
  const busyBy = presence.busyBy;

  return (
    <Box sx={{ p: 3 }}>
      {/* Header with Presence Indicator */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">
          Edit User: {user?.username || 'Loading...'}
        </Typography>

        {/* Presence Indicator */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {/* Connection Status */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Box
              sx={{
                width: 8,
                height: 8,
                borderRadius: '50%',
                bgcolor: presence.connected ? 'success.main' : 'grey.400',
                animation: presence.connected ? 'pulse 2s infinite' : 'none',
              }}
            />
            <Typography variant="caption" color="text.secondary">
              {presence.connected ? 'Live' : 'Offline'}
            </Typography>
          </Box>

          {/* Stale Badge */}
          {isStale && busyBy && (
            <Chip
              icon={<Edit sx={{ fontSize: 16 }} />}
              label={`Editing by ${busyBy}`}
              color="warning"
              size="small"
              data-testid="stale-badge"
            />
          )}

          {/* Active Users */}
          {presence.users.length > 0 && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Visibility fontSize="small" color="action" />
              <Typography variant="body2" fontWeight="medium" data-testid="presence-users">
                {presence.users.length}
              </Typography>

              {/* User Avatars */}
              <Box sx={{ display: 'flex', ml: 1 }}>
                {viewingUsers.slice(0, 3).map((userId) => (
                  <Tooltip key={userId} title={userId}>
                    <Avatar
                      sx={{
                        width: 28,
                        height: 28,
                        fontSize: '0.75rem',
                        ml: -1,
                        border: '2px solid white',
                      }}
                      data-testid={`presence-avatar-${userId}`}
                    >
                      {userId.substring(0, 2).toUpperCase()}
                    </Avatar>
                  </Tooltip>
                ))}
                {viewingUsers.length > 3 && (
                  <Avatar
                    sx={{
                      width: 28,
                      height: 28,
                      fontSize: '0.75rem',
                      ml: -1,
                      border: '2px solid white',
                      bgcolor: 'grey.400',
                    }}
                  >
                    +{viewingUsers.length - 3}
                  </Avatar>
                )}
              </Box>
            </Box>
          )}

          {/* Version Badge */}
          {presence.version !== null && (
            <Chip label={`v${presence.version}`} size="small" variant="outlined" data-testid="entity-version" />
          )}
        </Box>
      </Box>

      {/* Stale Warning */}
      {isStale && (
        <Alert severity="warning" sx={{ mb: 3 }} data-testid="stale-warning">
          ⚠️ This user is currently being modified by <strong>{busyBy}</strong>. Your changes may conflict.
        </Alert>
      )}

      {/* Presence Error */}
      {presenceError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          Presence tracking error: {presenceError}
        </Alert>
      )}

      {/* Form */}
      <Paper sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
          {/* Username */}
          <Box>
            <TextField
              fullWidth
              label="Username"
              value={user?.username || ''}
              onChange={(e) => handleFieldChange('username', e.target.value)}
              onFocus={() => handleFieldFocus('username')}
              onBlur={() => handleFieldBlur('username')}
              disabled={isStale}
              InputProps={{
                endAdornment: focusedField === 'username' && (
                  <Lock fontSize="small" sx={{ color: 'success.main' }} data-testid="lock-username" />
                ),
              }}
              data-testid="field-username"
            />
          </Box>

          {/* Email */}
          <Box>
            <TextField
              fullWidth
              label="Email"
              type="email"
              value={user?.email || ''}
              onChange={(e) => handleFieldChange('email', e.target.value)}
              onFocus={() => handleFieldFocus('email')}
              onBlur={() => handleFieldBlur('email')}
              disabled={isStale}
              InputProps={{
                endAdornment: focusedField === 'email' && (
                  <Lock fontSize="small" sx={{ color: 'success.main' }} data-testid="lock-email" />
                ),
              }}
              data-testid="field-email"
            />
          </Box>

          {/* First Name */}
          <Box>
            <TextField
              fullWidth
              label="First Name"
              value={user?.firstName || ''}
              onChange={(e) => handleFieldChange('firstName', e.target.value)}
              onFocus={() => handleFieldFocus('firstName')}
              onBlur={() => handleFieldBlur('firstName')}
              disabled={isStale}
              InputProps={{
                endAdornment: focusedField === 'firstName' && (
                  <Lock fontSize="small" sx={{ color: 'success.main' }} data-testid="lock-firstName" />
                ),
              }}
              data-testid="field-firstName"
            />
          </Box>

          {/* Last Name */}
          <Box>
            <TextField
              fullWidth
              label="Last Name"
              value={user?.lastName || ''}
              onChange={(e) => handleFieldChange('lastName', e.target.value)}
              onFocus={() => handleFieldFocus('lastName')}
              onBlur={() => handleFieldBlur('lastName')}
              disabled={isStale}
              InputProps={{
                endAdornment: focusedField === 'lastName' && (
                  <Lock fontSize="small" sx={{ color: 'success.main' }} data-testid="lock-lastName" />
                ),
              }}
              data-testid="field-lastName"
            />
          </Box>

          {/* Tenant (Read-only) */}
          <TextField fullWidth label="Tenant" value={user?.tenantKey || ''} disabled />

          {/* Actions */}
          <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
            <Button variant="outlined" startIcon={<Cancel />} onClick={() => navigate('/admin/users')}>
              Cancel
            </Button>
            <Button
              variant="contained"
              startIcon={saving ? <CircularProgress size={20} /> : <Save />}
              onClick={handleSave}
              disabled={saving || isStale}
            >
              {saving ? 'Saving...' : 'Save'}
            </Button>
          </Box>
        </Box>
      </Paper>

      {/* Debug Info (Development Only) */}
      {process.env.NODE_ENV === 'development' && (
        <Paper sx={{ mt: 3, p: 2, bgcolor: 'grey.50' }}>
          <Typography variant="caption" fontWeight="bold">
            Debug: Presence State
          </Typography>
          <pre style={{ fontSize: '0.75rem', margin: 0 }}>
            {JSON.stringify(
              {
                users: presence.users,
                stale: presence.stale,
                busyBy: presence.busyBy,
                version: presence.version,
                connected: presence.connected,
                focusedField,
              },
              null,
              2
            )}
          </pre>
        </Paper>
      )}
    </Box>
  );
};
