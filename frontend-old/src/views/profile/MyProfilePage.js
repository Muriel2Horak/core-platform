import { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Typography,
  TextField,
  Button,
  Alert,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Divider,
  Avatar,
  Chip
} from '@mui/material';
import { IconUser, IconKey, IconMail, IconEdit, IconCheck, IconX } from '@tabler/icons-react';
import PageContainer from '../../components/container/PageContainer';
import DashboardCard from '../../components/shared/DashboardCard';
import userManagementService from '../../services/userManagementService';
import logger from '../../services/logger';

const MyProfilePage = () => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  
  // Profile editing state
  const [isEditing, setIsEditing] = useState(false);
  const [editedProfile, setEditedProfile] = useState({});
  
  // Password change state
  const [passwordDialog, setPasswordDialog] = useState(false);
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });
  const [passwordError, setPasswordError] = useState(null);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      setError(null);
      
      logger.pageView('/me', { operation: 'load_profile' });
      
      const data = await userManagementService.getMyProfile();
      setProfile(data);
      setEditedProfile({
        email: data.email || '',
        firstName: data.firstName || '',
        lastName: data.lastName || ''
      });
      
      logger.userAction('PROFILE_LOADED', { userId: data.id });
      
    } catch (err) {
      console.error('Failed to load profile:', err);
      setError('Nepodařilo se načíst profil uživatele');
      logger.error('PROFILE_LOAD_ERROR', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleEditToggle = () => {
    if (isEditing) {
      // Zrušit změny
      setEditedProfile({
        email: profile.email || '',
        firstName: profile.firstName || '',
        lastName: profile.lastName || ''
      });
    }
    setIsEditing(!isEditing);
    logger.userAction(isEditing ? 'PROFILE_EDIT_CANCELLED' : 'PROFILE_EDIT_STARTED');
  };

  const handleProfileSave = async () => {
    try {
      setSaving(true);
      setError(null);
      setSuccess(null);
      
      const updatedProfile = await userManagementService.updateMyProfile(editedProfile);
      setProfile(updatedProfile);
      setIsEditing(false);
      setSuccess('Profil byl úspěšně aktualizován');
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(null), 3000);
      
    } catch (err) {
      console.error('Failed to update profile:', err);
      setError('Nepodařilo se aktualizovat profil');
    } finally {
      setSaving(false);
    }
  };

  const handlePasswordChange = async () => {
    try {
      setPasswordError(null);
      
      // Validation
      if (!passwordData.currentPassword || !passwordData.newPassword || !passwordData.confirmPassword) {
        setPasswordError('Všechna pole jsou povinná');
        return;
      }
      
      if (passwordData.newPassword !== passwordData.confirmPassword) {
        setPasswordError('Nové heslo a potvrzení se neshodují');
        return;
      }
      
      if (passwordData.newPassword.length < 8) {
        setPasswordError('Nové heslo musí mít alespoň 8 znaků');
        return;
      }
      
      setSaving(true);
      
      await userManagementService.changeMyPassword({
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword,
        confirmPassword: passwordData.confirmPassword
      });
      
      setPasswordDialog(false);
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setSuccess('Heslo bylo úspěšně změněno');
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccess(null), 3000);
      
    } catch (err) {
      console.error('Failed to change password:', err);
      setPasswordError(err.message || 'Nepodařilo se změnit heslo');
    } finally {
      setSaving(false);
    }
  };

  const getDisplayName = () => {
    if (!profile) return 'N/A';
    if (profile.firstName && profile.lastName) {
      return `${profile.firstName} ${profile.lastName}`;
    }
    return profile.username || profile.email || 'N/A';
  };

  const getInitials = () => {
    if (!profile) return '?';
    if (profile.firstName && profile.lastName) {
      return `${profile.firstName[0]}${profile.lastName[0]}`.toUpperCase();
    }
    if (profile.username) {
      return profile.username.substring(0, 2).toUpperCase();
    }
    return '?';
  };

  if (loading) {
    return (
      <PageContainer title="Můj profil" description="Správa uživatelského profilu">
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
          <CircularProgress />
        </Box>
      </PageContainer>
    );
  }

  return (
    <PageContainer title="Můj profil" description="Správa uživatelského profilu">
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

        {/* Profile Overview Card */}
        <Grid item xs={12} md={4}>
          <DashboardCard title="Přehled profilu">
            <Box display="flex" flexDirection="column" alignItems="center" p={2}>
              <Avatar 
                sx={{ 
                  width: 80, 
                  height: 80, 
                  mb: 2,
                  backgroundColor: 'primary.main',
                  fontSize: '2rem'
                }}
              >
                {getInitials()}
              </Avatar>
              
              <Typography variant="h5" gutterBottom>
                {getDisplayName()}
              </Typography>
              
              <Typography variant="body2" color="text.secondary" gutterBottom>
                @{profile?.username}
              </Typography>
              
              <Box display="flex" gap={1} flexWrap="wrap" justifyContent="center" mt={2}>
                {profile?.roles?.map((role) => (
                  <Chip 
                    key={role} 
                    label={role} 
                    size="small" 
                    color="primary" 
                    variant="outlined" 
                  />
                ))}
              </Box>
              
              <Divider sx={{ width: '100%', my: 2 }} />
              
              <Box width="100%">
                <Typography variant="body2" color="text.secondary">
                  <strong>Status:</strong> {profile?.enabled ? 'Aktivní' : 'Neaktivní'}
                </Typography>
                
                <Typography variant="body2" color="text.secondary" mt={1}>
                  <strong>Email ověřen:</strong> {profile?.emailVerified ? 'Ano' : 'Ne'}
                </Typography>
                
                {profile?.createdTimestamp && (
                  <Typography variant="body2" color="text.secondary" mt={1}>
                    <strong>Registrace:</strong> {new Date(profile.createdTimestamp).toLocaleDateString('cs-CZ')}
                  </Typography>
                )}
              </Box>
            </Box>
          </DashboardCard>
        </Grid>

        {/* Profile Details Card */}
        <Grid item xs={12} md={8}>
          <DashboardCard 
            title="Detaily profilu"
            action={
              <Button
                variant={isEditing ? "outlined" : "contained"}
                startIcon={isEditing ? <IconX /> : <IconEdit />}
                onClick={handleEditToggle}
                disabled={saving}
              >
                {isEditing ? 'Zrušit' : 'Upravit'}
              </Button>
            }
          >
            <Grid container spacing={3}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Jméno"
                  value={isEditing ? editedProfile.firstName : (profile?.firstName || '')}
                  onChange={(e) => setEditedProfile(prev => ({ ...prev, firstName: e.target.value }))}
                  disabled={!isEditing}
                  InputProps={{
                    startAdornment: <IconUser size={20} style={{ marginRight: 8 }} />
                  }}
                />
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Příjmení"
                  value={isEditing ? editedProfile.lastName : (profile?.lastName || '')}
                  onChange={(e) => setEditedProfile(prev => ({ ...prev, lastName: e.target.value }))}
                  disabled={!isEditing}
                  InputProps={{
                    startAdornment: <IconUser size={20} style={{ marginRight: 8 }} />
                  }}
                />
              </Grid>
              
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Email"
                  type="email"
                  value={isEditing ? editedProfile.email : (profile?.email || '')}
                  onChange={(e) => setEditedProfile(prev => ({ ...prev, email: e.target.value }))}
                  disabled={!isEditing}
                  InputProps={{
                    startAdornment: <IconMail size={20} style={{ marginRight: 8 }} />
                  }}
                />
              </Grid>
              
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Uživatelské jméno"
                  value={profile?.username || ''}
                  disabled
                  helperText="Uživatelské jméno nelze změnit"
                />
              </Grid>
              
              {isEditing && (
                <Grid item xs={12}>
                  <Box display="flex" gap={2} justifyContent="flex-end">
                    <Button
                      variant="outlined"
                      onClick={handleEditToggle}
                      disabled={saving}
                    >
                      Zrušit
                    </Button>
                    <Button
                      variant="contained"
                      startIcon={saving ? <CircularProgress size={16} /> : <IconCheck />}
                      onClick={handleProfileSave}
                      disabled={saving}
                    >
                      {saving ? 'Ukládám...' : 'Uložit změny'}
                    </Button>
                  </Box>
                </Grid>
              )}
            </Grid>
          </DashboardCard>
        </Grid>

        {/* Security Actions Card */}
        <Grid item xs={12}>
          <DashboardCard title="Bezpečnost">
            <Box>
              <Typography variant="body1" gutterBottom>
                Správa bezpečnostních nastavení vašeho účtu
              </Typography>
              
              <Button
                variant="outlined"
                startIcon={<IconKey />}
                onClick={() => setPasswordDialog(true)}
                sx={{ mt: 2 }}
              >
                Změnit heslo
              </Button>
            </Box>
          </DashboardCard>
        </Grid>
      </Grid>

      {/* Password Change Dialog */}
      <Dialog 
        open={passwordDialog} 
        onClose={() => setPasswordDialog(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Změna hesla</DialogTitle>
        <DialogContent>
          {passwordError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {passwordError}
            </Alert>
          )}
          
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Současné heslo"
                type="password"
                value={passwordData.currentPassword}
                onChange={(e) => setPasswordData(prev => ({ ...prev, currentPassword: e.target.value }))}
                required
              />
            </Grid>
            
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Nové heslo"
                type="password"
                value={passwordData.newPassword}
                onChange={(e) => setPasswordData(prev => ({ ...prev, newPassword: e.target.value }))}
                required
                helperText="Alespoň 8 znaků"
              />
            </Grid>
            
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Potvrzení nového hesla"
                type="password"
                value={passwordData.confirmPassword}
                onChange={(e) => setPasswordData(prev => ({ ...prev, confirmPassword: e.target.value }))}
                required
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button 
            onClick={() => {
              setPasswordDialog(false);
              setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
              setPasswordError(null);
            }}
            disabled={saving}
          >
            Zrušit
          </Button>
          <Button
            variant="contained"
            onClick={handlePasswordChange}
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : null}
          >
            {saving ? 'Měním heslo...' : 'Změnit heslo'}
          </Button>
        </DialogActions>
      </Dialog>
    </PageContainer>
  );
};

export default MyProfilePage;