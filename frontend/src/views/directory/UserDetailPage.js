import React, { useState, useEffect } from 'react';
import { useParams, useSearchParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Grid,
  Typography,
  TextField,
  Button,
  Alert,
  CircularProgress,
  Card,
  CardContent,
  Avatar,
  Chip,
  Switch,
  FormControlLabel,
  Paper,
  Tooltip,
  IconButton,
  Divider
} from '@mui/material';
import {
  IconArrowLeft,
  IconEdit,
  IconSave,
  IconX,
  IconUser,
  IconMail,
  IconPhone,
  IconBuilding,
  IconShield,
  IconCalendar,
  IconExternalLink
} from '@tabler/icons-react';
import PageContainer from '../../components/container/PageContainer';
import DashboardCard from '../../components/shared/DashboardCard';
import usersDirectoryService from '../../services/usersDirectoryService';
import { useUserInfo } from '../../hooks/useUserInfo';
import logger from '../../services/logger';

const UserDetailPage = () => {
  const { userId } = useParams();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const editMode = searchParams.get('edit') === 'true';
  
  // State for user data
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // State for editing
  const [isEditing, setIsEditing] = useState(editMode);
  const [editedUser, setEditedUser] = useState({});
  const [hasChanges, setHasChanges] = useState(false);

  // User info and permissions
  const { userInfo } = useUserInfo();

  useEffect(() => {
    loadUserDetail();
  }, [userId]);

  useEffect(() => {
    if (user && isEditing) {
      setEditedUser({
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        department: user.department || '',
        title: user.title || '',
        phoneNumber: user.phoneNumber || ''
      });
    }
  }, [user, isEditing]);

  const loadUserDetail = async () => {
    try {
      setLoading(true);
      setError(null);
      
      logger.pageView(`/directory/${userId}`, { operation: 'load_user_detail' });
      
      const data = await usersDirectoryService.getUserDetail(userId);
      setUser(data);
      
      logger.userAction('USER_DETAIL_LOADED', { userId });
      
    } catch (err) {
      console.error('Failed to load user detail:', err);
      setError('Nepodařilo se načíst detail uživatele: ' + err.message);
      logger.error('USER_DETAIL_LOAD_ERROR', err.message, { userId });
    } finally {
      setLoading(false);
    }
  };

  const handleEditToggle = () => {
    if (isEditing && hasChanges) {
      // Ask for confirmation if there are unsaved changes
      if (window.confirm('Máte neuložené změny. Opravdu chcete zrušit editaci?')) {
        setIsEditing(false);
        setHasChanges(false);
        setEditedUser({});
      }
    } else {
      setIsEditing(!isEditing);
      if (!isEditing) {
        // Entering edit mode
        setEditedUser({
          firstName: user.firstName || '',
          lastName: user.lastName || '',
          department: user.department || '',
          title: user.title || '',
          phoneNumber: user.phoneNumber || ''
        });
      }
    }
  };

  const handleFieldChange = (field, value) => {
    setEditedUser(prev => ({
      ...prev,
      [field]: value
    }));
    setHasChanges(true);
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      setError(null);
      
      // Only send changed fields
      const updates = {};
      Object.keys(editedUser).forEach(key => {
        if (editedUser[key] !== (user[key] || '')) {
          updates[key] = editedUser[key];
        }
      });

      if (Object.keys(updates).length === 0) {
        setSuccess('Žádné změny k uložení');
        setTimeout(() => setSuccess(null), 3000);
        return;
      }

      const updatedUser = await usersDirectoryService.updateUser(userId, updates);
      
      setUser(updatedUser);
      setIsEditing(false);
      setHasChanges(false);
      setSuccess('Uživatel byl úspěšně aktualizován');
      
      setTimeout(() => setSuccess(null), 5000);
      
    } catch (err) {
      console.error('Failed to update user:', err);
      setError('Nepodařilo se aktualizovat uživatele: ' + err.message);
    } finally {
      setSaving(false);
    }
  };

  const canEdit = user && usersDirectoryService.canEditUser(user, userInfo);
  const isReadOnly = user && usersDirectoryService.isReadOnly(user, userInfo);
  const isADUser = user && (user.isFederated || user.directorySource === 'AD');

  if (loading) {
    return (
      <PageContainer title="Loading..." description="Načítám detail uživatele">
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
          <CircularProgress />
        </Box>
      </PageContainer>
    );
  }

  if (!user) {
    return (
      <PageContainer title="Uživatel nenalezen" description="Detail uživatele">
        <Box textAlign="center" py={4}>
          <Typography variant="h5" color="text.secondary">
            Uživatel nenalezen
          </Typography>
          <Button 
            startIcon={<IconArrowLeft />}
            onClick={() => navigate('/directory')}
            sx={{ mt: 2 }}
          >
            Zpět na Directory
          </Button>
        </Box>
      </PageContainer>
    );
  }

  return (
    <PageContainer 
      title={usersDirectoryService.getDisplayName(user)} 
      description="Detail uživatele"
    >
      <Grid container spacing={3}>
        
        {/* Success/Error Messages */}
        {success && (
          <Grid item xs={12}>
            <Alert severity="success" onClose={() => setSuccess(null)}>
              {success}
            </Alert>
          </Grid>
        )}
        
        {error && (
          <Grid item xs={12}>
            <Alert severity="error" onClose={() => setError(null)}>
              {error}
            </Alert>
          </Grid>
        )}

        {/* Header */}
        <Grid item xs={12}>
          <Box display="flex" alignItems="center" justifyContent="space-between" mb={2}>
            <Button 
              startIcon={<IconArrowLeft />}
              onClick={() => navigate('/directory')}
              variant="outlined"
            >
              Zpět na Directory
            </Button>
            
            <Box display="flex" gap={1}>
              {canEdit && (
                <>
                  {isEditing ? (
                    <>
                      <Button
                        startIcon={<IconX />}
                        onClick={handleEditToggle}
                        disabled={saving}
                      >
                        Zrušit
                      </Button>
                      <Button
                        startIcon={<IconSave />}
                        variant="contained"
                        onClick={handleSave}
                        disabled={saving || !hasChanges}
                      >
                        {saving ? 'Ukládám...' : 'Uložit'}
                      </Button>
                    </>
                  ) : (
                    <Button
                      startIcon={<IconEdit />}
                      variant="contained"
                      onClick={handleEditToggle}
                    >
                      Upravit
                    </Button>
                  )}
                </>
              )}
            </Box>
          </Box>
        </Grid>

        {/* User Profile Card */}
        <Grid item xs={12} md={4}>
          <DashboardCard title="Profil uživatele">
            <Box textAlign="center" p={2}>
              <Avatar
                sx={{
                  width: 120,
                  height: 120,
                  fontSize: '2rem',
                  margin: '0 auto 16px',
                  backgroundColor: 'primary.main'
                }}
              >
                {usersDirectoryService.getDisplayName(user).substring(0, 2).toUpperCase()}
              </Avatar>
              
              <Typography variant="h5" gutterBottom>
                {usersDirectoryService.getDisplayName(user)}
              </Typography>
              
              <Typography variant="body2" color="text.secondary" gutterBottom>
                @{user.username}
              </Typography>

              <Box display="flex" justifyContent="center" gap={1} mt={2}>
                <Chip
                  label={usersDirectoryService.getDirectorySourceBadge(user)}
                  color={isADUser ? 'info' : 'default'}
                  size="small"
                />
                {user.tenantName && (
                  <Chip
                    label={user.tenantName}
                    color="primary"
                    variant="outlined"
                    size="small"
                  />
                )}
              </Box>

              {isADUser && (
                <Alert severity="info" sx={{ mt: 2 }}>
                  <Typography variant="body2">
                    Uživatel je spravován externě (AD). Některá pole jsou pouze pro čtení.
                  </Typography>
                </Alert>
              )}
            </Box>
          </DashboardCard>
        </Grid>

        {/* User Details */}
        <Grid item xs={12} md={8}>
          <DashboardCard title="Informace o uživateli">
            <Grid container spacing={3}>
              
              {/* Basic Information */}
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <IconUser size={20} />
                  Základní údaje
                </Typography>
                <Divider sx={{ mb: 2 }} />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Křestní jméno"
                  value={isEditing ? editedUser.firstName : (user.firstName || '')}
                  onChange={(e) => handleFieldChange('firstName', e.target.value)}
                  disabled={!isEditing || saving}
                  variant={isEditing ? 'outlined' : 'filled'}
                  InputProps={{
                    readOnly: !isEditing,
                    startAdornment: <IconUser size={20} style={{ marginRight: 8, opacity: 0.7 }} />
                  }}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Příjmení"
                  value={isEditing ? editedUser.lastName : (user.lastName || '')}
                  onChange={(e) => handleFieldChange('lastName', e.target.value)}
                  disabled={!isEditing || saving}
                  variant={isEditing ? 'outlined' : 'filled'}
                  InputProps={{
                    readOnly: !isEditing,
                    startAdornment: <IconUser size={20} style={{ marginRight: 8, opacity: 0.7 }} />
                  }}
                />
              </Grid>

              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Email"
                  value={user.email || ''}
                  disabled={true}
                  variant="filled"
                  InputProps={{
                    readOnly: true,
                    startAdornment: <IconMail size={20} style={{ marginRight: 8, opacity: 0.7 }} />
                  }}
                  helperText={isADUser ? "Spravováno v AD" : "Systémové pole"}
                />
              </Grid>

              {/* Contact Information */}
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 2 }}>
                  <IconPhone size={20} />
                  Kontaktní údaje
                </Typography>
                <Divider sx={{ mb: 2 }} />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Telefon"
                  value={isEditing ? editedUser.phoneNumber : (user.phoneNumber || '')}
                  onChange={(e) => handleFieldChange('phoneNumber', e.target.value)}
                  disabled={!isEditing || saving}
                  variant={isEditing ? 'outlined' : 'filled'}
                  InputProps={{
                    readOnly: !isEditing,
                    startAdornment: <IconPhone size={20} style={{ marginRight: 8, opacity: 0.7 }} />
                  }}
                />
              </Grid>

              {/* Organization Information */}
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 2 }}>
                  <IconBuilding size={20} />
                  Organizační údaje
                </Typography>
                <Divider sx={{ mb: 2 }} />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Oddělení"
                  value={isEditing ? editedUser.department : (user.department || '')}
                  onChange={(e) => handleFieldChange('department', e.target.value)}
                  disabled={!isEditing || saving}
                  variant={isEditing ? 'outlined' : 'filled'}
                  InputProps={{
                    readOnly: !isEditing,
                    startAdornment: <IconBuilding size={20} style={{ marginRight: 8, opacity: 0.7 }} />
                  }}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Pozice"
                  value={isEditing ? editedUser.title : (user.title || '')}
                  onChange={(e) => handleFieldChange('title', e.target.value)}
                  disabled={!isEditing || saving}
                  variant={isEditing ? 'outlined' : 'filled'}
                  InputProps={{
                    readOnly: !isEditing,
                    startAdornment: <IconShield size={20} style={{ marginRight: 8, opacity: 0.7 }} />
                  }}
                />
              </Grid>

              {/* System Information */}
              <Grid item xs={12}>
                <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center', gap: 1, mt: 2 }}>
                  <IconCalendar size={20} />
                  Systémové údaje
                </Typography>
                <Divider sx={{ mb: 2 }} />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Stav"
                  value={user.active ? 'Aktivní' : 'Neaktivní'}
                  disabled={true}
                  variant="filled"
                  InputProps={{
                    readOnly: true,
                    startAdornment: <IconShield size={20} style={{ marginRight: 8, opacity: 0.7 }} />
                  }}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Poslední aktualizace"
                  value={user.updatedAt ? new Date(user.updatedAt).toLocaleString('cs-CZ') : 'N/A'}
                  disabled={true}
                  variant="filled"
                  InputProps={{
                    readOnly: true,
                    startAdornment: <IconCalendar size={20} style={{ marginRight: 8, opacity: 0.7 }} />
                  }}
                />
              </Grid>
            </Grid>
          </DashboardCard>
        </Grid>
      </Grid>
    </PageContainer>
  );
};

export default UserDetailPage;