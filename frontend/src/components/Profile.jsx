import React, { useState, useEffect } from 'react';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Avatar,
  Chip,
  Alert,
  Divider,
  Tabs,
  Tab,
  TextField,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Paper,
  Slide,
  Fade,
  IconButton,
  Badge,
  CircularProgress,
} from '@mui/material';
import {
  Person as PersonIcon,
  Email as EmailIcon,
  Business as BusinessIcon,
  Security as SecurityIcon,
  Edit as EditIcon,
  Check as CheckIcon,
  Close as CloseIcon,
  Key as KeyIcon,
  PhotoCamera as CameraIcon,
  Work as WorkIcon,
  LocationOn as LocationIcon,
  PersonAdd as PersonAddIcon,
} from '@mui/icons-material';
import apiService from '../services/api.js';
import logger from '../services/logger.js';

// Tab Panel Component with animations
function TabPanel({ children, value, index, ...other }) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`profile-tabpanel-${index}`}
      aria-labelledby={`profile-tab-${index}`}
      {...other}
    >
      {value === index && (
        <Fade in={true} timeout={500}>
          <Box sx={{ p: { xs: 2, md: 3 } }}>
            {children}
          </Box>
        </Fade>
      )}
    </div>
  );
}

function Profile({ user }) {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);

  // Tab state
  const [currentTab, setCurrentTab] = useState(0);

  // Editing states
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

      logger.pageView('/profile', { operation: 'load_profile' });

      const data = await apiService.getMe();
      setProfile(data);
      setEditedProfile(data);

      logger.userAction('PROFILE_LOADED', { userId: data.id });

    } catch (err) {
      console.error('Failed to load profile:', err);
      setError('Nepodařilo se načíst profil uživatele');
      logger.error('PROFILE_LOAD_ERROR', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleTabChange = (event, newValue) => {
    setCurrentTab(newValue);
  };

  const handleEditToggle = () => {
    if (isEditing) {
      // Reset changes
      setEditedProfile(profile);
    }
    setIsEditing(!isEditing);
    logger.userAction(isEditing ? 'PROFILE_EDIT_CANCELLED' : 'PROFILE_EDIT_STARTED');
  };

  const handleProfileSave = async () => {
    try {
      setSaving(true);
      setError(null);
      setSuccess(null);

      // Here we would call API to update profile
      // For now, just simulate success
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      setProfile(editedProfile);
      setIsEditing(false);
      setSuccess('Profil byl úspěšně aktualizován');

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

      // Here we would call API to change password
      await new Promise(resolve => setTimeout(resolve, 1000));

      setPasswordDialog(false);
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setSuccess('Heslo bylo úspěšně změněno');

      setTimeout(() => setSuccess(null), 3000);

    } catch (err) {
      console.error('Failed to change password:', err);
      setPasswordError(err.message || 'Nepodařilo se změnit heslo');
    } finally {
      setSaving(false);
    }
  };

  const getDisplayName = () => {
    const data = profile || user;
    if (data?.firstName && data?.lastName) {
      return `${data.firstName} ${data.lastName}`;
    }
    return data?.username || data?.email || 'N/A';
  };

  const getInitials = () => {
    const name = getDisplayName();
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  if (loading) {
    return (
      <Box>
        <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600 }}>
          Můj profil
        </Typography>
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
          <CircularProgress size={60} thickness={4} />
        </Box>
      </Box>
    );
  }

  const data = profile || user;

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
      <Typography variant="h4" component="h1" gutterBottom sx={{ fontWeight: 600, mb: 4 }}>
        Můj profil
      </Typography>

      {/* Success/Error Messages */}
      {success && (
        <Slide direction="down" in={Boolean(success)} mountOnEnter unmountOnExit>
          <Alert
            severity="success"
            onClose={() => setSuccess(null)}
            sx={{ mb: 3, borderRadius: 2 }}
            variant="filled"
          >
            {success}
          </Alert>
        </Slide>
      )}

      {error && (
        <Slide direction="down" in={Boolean(error)} mountOnEnter unmountOnExit>
          <Alert
            severity="error"
            onClose={() => setError(null)}
            sx={{ mb: 3, borderRadius: 2 }}
            variant="filled"
          >
            {error}
          </Alert>
        </Slide>
      )}

      {/* Profile Header Card - Modern Design */}
      <Paper
        elevation={3}
        sx={{
          mb: 3,
          borderRadius: 3,
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          color: 'white',
          overflow: 'hidden'
        }}
      >
        <CardContent sx={{ p: 4 }}>
          <Box display="flex" alignItems="center" justifyContent="space-between" flexWrap="wrap" gap={3}>
            <Box display="flex" alignItems="center" gap={3}>
              {/* Profile Avatar with Camera */}
              <Box position="relative">
                <Badge
                  overlap="circular"
                  anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
                  badgeContent={
                    isEditing ? (
                      <IconButton
                        size="small"
                        component="label"
                        sx={{
                          backgroundColor: 'white',
                          color: 'primary.main',
                          '&:hover': { backgroundColor: 'grey.100' }
                        }}
                      >
                        <CameraIcon fontSize="small" />
                        <input
                          type="file"
                          hidden
                          accept="image/*"
                          onChange={(e) => {
                            const file = e.target.files[0];
                            if (file) {
                              console.log('Selected file:', file);
                            }
                          }}
                        />
                      </IconButton>
                    ) : null
                  }
                >
                  <Avatar
                    src={data?.profilePicture}
                    sx={{
                      width: 100,
                      height: 100,
                      backgroundColor: 'rgba(255,255,255,0.2)',
                      fontSize: '2.5rem',
                      fontWeight: 'bold',
                      border: '4px solid rgba(255,255,255,0.3)'
                    }}
                  >
                    {!data?.profilePicture && getInitials()}
                  </Avatar>
                </Badge>
              </Box>

              <Box>
                <Typography variant="h3" fontWeight="bold" gutterBottom>
                  {getDisplayName()}
                </Typography>

                <Typography variant="h6" sx={{ opacity: 0.9 }} gutterBottom>
                  @{data?.username}
                </Typography>

                <Typography variant="body2" sx={{ opacity: 0.8 }}>
                  {data?.email}
                </Typography>
              </Box>
            </Box>

            {/* Edit Button */}
            <Box>
              <Button
                variant={isEditing ? "outlined" : "contained"}
                size="large"
                startIcon={isEditing ? <CloseIcon /> : <EditIcon />}
                onClick={handleEditToggle}
                disabled={saving}
                sx={{
                  backgroundColor: isEditing ? 'transparent' : 'rgba(255,255,255,0.9)',
                  color: isEditing ? 'white' : 'primary.main',
                  border: isEditing ? '2px solid white' : 'none',
                  px: 3,
                  py: 1.5,
                  borderRadius: 3,
                  fontWeight: 'bold',
                  fontSize: '1.1rem',
                  '&:hover': {
                    backgroundColor: isEditing ? 'rgba(255,255,255,0.1)' : 'rgba(255,255,255,1)',
                    transform: 'translateY(-2px)',
                    boxShadow: '0 8px 20px rgba(0,0,0,0.2)'
                  },
                  transition: 'all 0.3s ease'
                }}
              >
                {isEditing ? 'Zrušit' : 'Upravit profil'}
              </Button>
            </Box>
          </Box>
        </CardContent>
      </Paper>

      {/* Main Content with Tabs */}
      <Paper elevation={2} sx={{ borderRadius: 3, overflow: 'hidden' }}>
        <Box sx={{
          borderBottom: 1,
          borderColor: 'divider',
          background: 'linear-gradient(to right, #f8f9fa, #e9ecef)'
        }}>
          <Tabs
            value={currentTab}
            onChange={handleTabChange}
            aria-label="Profile tabs"
            variant="fullWidth"
            sx={{
              '& .MuiTab-root': {
                minHeight: 80,
                fontSize: '1rem',
                fontWeight: 'bold',
                '&.Mui-selected': {
                  color: 'primary.main'
                }
              }
            }}
          >
            <Tab
              label="Základní údaje"
              icon={<PersonIcon />}
              iconPosition="top"
            />
            <Tab
              label="Organizační struktura"
              icon={<BusinessIcon />}
              iconPosition="top"
            />
            <Tab
              label="Bezpečnost"
              icon={<SecurityIcon />}
              iconPosition="top"
            />
          </Tabs>
        </Box>

        {/* Tab 0: Základní údaje */}
        <TabPanel value={currentTab} index={0}>
          <Typography variant="h5" gutterBottom color="primary" fontWeight="bold">
            📋 Základní údaje
          </Typography>
          <Typography variant="body1" color="text.secondary" paragraph>
            Základní informace o vašem profilu
          </Typography>

          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Jméno"
                value={editedProfile.firstName || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, firstName: e.target.value }))}
                disabled={!isEditing}
                variant="outlined"
                InputProps={{
                  startAdornment: <PersonIcon sx={{ mr: 1, color: '#667eea' }} />
                }}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    '&:hover .MuiOutlinedInput-notchedOutline': {
                      borderColor: 'primary.main',
                    }
                  }
                }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Příjmení"
                value={editedProfile.lastName || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, lastName: e.target.value }))}
                disabled={!isEditing}
                variant="outlined"
                InputProps={{
                  startAdornment: <PersonIcon sx={{ mr: 1, color: '#667eea' }} />
                }}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    '&:hover .MuiOutlinedInput-notchedOutline': {
                      borderColor: 'primary.main',
                    }
                  }
                }}
              />
            </Grid>

            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Email"
                type="email"
                value={editedProfile.email || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, email: e.target.value }))}
                disabled={!isEditing}
                variant="outlined"
                InputProps={{
                  startAdornment: <EmailIcon sx={{ mr: 1, color: '#667eea' }} />
                }}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    '&:hover .MuiOutlinedInput-notchedOutline': {
                      borderColor: 'primary.main',
                    }
                  }
                }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Uživatelské jméno"
                value={data?.username || ''}
                disabled
                variant="outlined"
                helperText="Uživatelské jméno nelze změnit"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2
                  }
                }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Tenant"
                value={data?.tenant || ''}
                disabled
                variant="outlined"
                helperText="Tenant nelze změnit"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2
                  }
                }}
              />
            </Grid>

            {isEditing && (
              <Grid item xs={12}>
                <Box display="flex" gap={2} justifyContent="center" mt={3}>
                  <Button
                    variant="outlined"
                    size="large"
                    onClick={handleEditToggle}
                    disabled={saving}
                    sx={{
                      borderRadius: 3,
                      px: 4,
                      py: 1.5,
                      borderWidth: 2,
                      '&:hover': { borderWidth: 2 }
                    }}
                  >
                    Zrušit
                  </Button>
                  <Button
                    variant="contained"
                    size="large"
                    startIcon={saving ? <CircularProgress size={16} /> : <CheckIcon />}
                    onClick={handleProfileSave}
                    disabled={saving}
                    sx={{
                      borderRadius: 3,
                      px: 4,
                      py: 1.5,
                      background: 'linear-gradient(45deg, #667eea, #764ba2)',
                      '&:hover': {
                        background: 'linear-gradient(45deg, #5a6fd8, #6b4190)',
                        transform: 'translateY(-2px)',
                        boxShadow: '0 8px 20px rgba(0,0,0,0.2)'
                      },
                      transition: 'all 0.3s ease'
                    }}
                  >
                    {saving ? 'Ukládám...' : 'Uložit změny'}
                  </Button>
                </Box>
              </Grid>
            )}
          </Grid>
        </TabPanel>

        {/* Tab 1: Organizační struktura */}
        <TabPanel value={currentTab} index={1}>
          <Typography variant="h5" gutterBottom color="primary" fontWeight="bold">
            🏢 Organizační struktura
          </Typography>
          <Typography variant="body1" color="text.secondary" paragraph>
            Informace o vašem zařazení v organizaci
          </Typography>

          <Grid container spacing={3}>
            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Oddělení"
                value={editedProfile.department || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, department: e.target.value }))}
                disabled={!isEditing}
                variant="outlined"
                InputProps={{
                  startAdornment: <BusinessIcon sx={{ mr: 1, color: '#667eea' }} />
                }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Pozice"
                value={editedProfile.position || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, position: e.target.value }))}
                disabled={!isEditing}
                variant="outlined"
                InputProps={{
                  startAdornment: <WorkIcon sx={{ mr: 1, color: '#667eea' }} />
                }}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <TextField
                fullWidth
                label="Lokace / Pobočka"
                value={editedProfile.location || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, location: e.target.value }))}
                disabled={!isEditing}
                variant="outlined"
                InputProps={{
                  startAdornment: <LocationIcon sx={{ mr: 1, color: '#667eea' }} />
                }}
              />
            </Grid>

            {isEditing && (
              <Grid item xs={12}>
                <Box display="flex" gap={2} justifyContent="center" mt={3}>
                  <Button
                    variant="outlined"
                    size="large"
                    onClick={handleEditToggle}
                    disabled={saving}
                    sx={{ borderRadius: 3, px: 4, py: 1.5 }}
                  >
                    Zrušit
                  </Button>
                  <Button
                    variant="contained"
                    size="large"
                    startIcon={saving ? <CircularProgress size={16} /> : <CheckIcon />}
                    onClick={handleProfileSave}
                    disabled={saving}
                    sx={{
                      borderRadius: 3,
                      px: 4,
                      py: 1.5,
                      background: 'linear-gradient(45deg, #667eea, #764ba2)'
                    }}
                  >
                    {saving ? 'Ukládám...' : 'Uložit změny'}
                  </Button>
                </Box>
              </Grid>
            )}
          </Grid>
        </TabPanel>

        {/* Tab 2: Bezpečnost */}
        <TabPanel value={currentTab} index={2}>
          <Typography variant="h5" gutterBottom color="primary" fontWeight="bold">
            🔐 Bezpečnost
          </Typography>
          <Typography variant="body1" color="text.secondary" paragraph>
            Správa bezpečnostních nastavení vašeho účtu
          </Typography>

          <Grid container spacing={3}>
            {/* Role a oprávnění */}
            <Grid item xs={12}>
              <Card
                sx={{
                  background: 'linear-gradient(135deg, #f3e5f5 0%, #e1f5fe 100%)',
                  borderRadius: 2,
                  border: '1px solid rgba(102, 126, 234, 0.3)',
                  mb: 3
                }}
              >
                <CardContent sx={{ p: 3 }}>
                  <Typography variant="h6" gutterBottom fontWeight="bold" sx={{ color: 'primary.main' }}>
                    🛡️ Role a oprávnění
                  </Typography>
                  <Typography variant="body2" color="text.secondary" paragraph>
                    Vaše aktuální role v systému a související oprávnění
                  </Typography>

                  <Box display="flex" gap={1} flexWrap="wrap" mt={2}>
                    {data?.roles?.length > 0 ? (
                      data.roles.map((role) => (
                        <Chip
                          key={role}
                          label={role}
                          size="medium"
                          color={role.includes('ADMIN') ? 'error' : 'primary'}
                          variant="filled"
                          sx={{
                            fontWeight: 'bold',
                            '&:hover': {
                              transform: 'translateY(-1px)',
                              boxShadow: '0 4px 8px rgba(0,0,0,0.2)'
                            },
                            transition: 'all 0.2s ease'
                          }}
                        />
                      ))
                    ) : (
                      <Typography variant="body2" color="text.secondary" sx={{ fontStyle: 'italic' }}>
                        Žádné role nejsou přiřazeny
                      </Typography>
                    )}
                  </Box>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={6}>
              <Card
                sx={{
                  height: '100%',
                  background: 'linear-gradient(135deg, #fff3e0 0%, #ffe0b2 100%)',
                  borderRadius: 2,
                  border: '1px solid rgba(255, 152, 0, 0.3)',
                  transition: 'transform 0.2s ease',
                  '&:hover': {
                    transform: 'translateY(-4px)',
                    boxShadow: '0 8px 25px rgba(0,0,0,0.15)'
                  }
                }}
              >
                <CardContent sx={{ p: 3 }}>
                  <Typography variant="h6" gutterBottom fontWeight="bold" sx={{ color: 'warning.main' }}>
                    🔑 Změna hesla
                  </Typography>
                  <Typography variant="body2" color="text.secondary" paragraph>
                    Pravidelně měňte heslo pro zvýšení bezpečnosti vašeho účtu.
                  </Typography>

                  <Button
                    variant="contained"
                    startIcon={<KeyIcon />}
                    onClick={() => setPasswordDialog(true)}
                    sx={{
                      mt: 2,
                      borderRadius: 2,
                      background: 'linear-gradient(45deg, #ff9800, #f57c00)',
                      '&:hover': {
                        background: 'linear-gradient(45deg, #f57c00, #e65100)',
                        transform: 'translateY(-2px)'
                      },
                      transition: 'all 0.3s ease'
                    }}
                  >
                    Změnit heslo
                  </Button>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={6}>
              <Card
                sx={{
                  height: '100%',
                  background: 'linear-gradient(135deg, #e1f5fe 0%, #b3e5fc 100%)',
                  borderRadius: 2,
                  border: '1px solid rgba(3, 169, 244, 0.3)',
                  transition: 'transform 0.2s ease',
                  '&:hover': {
                    transform: 'translateY(-4px)',
                    boxShadow: '0 8px 25px rgba(0,0,0,0.15)'
                  }
                }}
              >
                <CardContent sx={{ p: 3 }}>
                  <Typography variant="h6" gutterBottom fontWeight="bold" sx={{ color: 'info.main' }}>
                    ℹ️ Informace o účtu
                  </Typography>

                  <Box sx={{ mt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Chip
                        label={data?.enabled ? 'Aktivní' : 'Neaktivní'}
                        color={data?.enabled ? 'success' : 'error'}
                        size="small"
                      />
                      <Typography variant="body2" component="span" sx={{ fontSize: '0.9rem', color: '#666' }}>
                        Status účtu
                      </Typography>
                    </Box>

                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Chip
                        label={data?.emailVerified ? 'Ověřeno' : 'Neověřeno'}
                        color={data?.emailVerified ? 'success' : 'warning'}
                        size="small"
                      />
                      <Typography variant="body2" component="span" sx={{ fontSize: '0.9rem', color: '#666' }}>
                        Email
                      </Typography>
                    </Box>

                    <Typography variant="body2" color="text.secondary">
                      <strong>📅 Tenant:</strong> {data?.tenant}
                    </Typography>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </TabPanel>
      </Paper>

      {/* Password Change Dialog */}
      <Dialog
        open={passwordDialog}
        onClose={() => setPasswordDialog(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{
          sx: {
            borderRadius: 3,
            background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)'
          }
        }}
      >
        <DialogTitle sx={{
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          color: 'white',
          textAlign: 'center',
          fontSize: '1.5rem',
          fontWeight: 'bold'
        }}>
          🔑 Změna hesla
        </DialogTitle>
        <DialogContent sx={{ p: 3 }}>
          {passwordError && (
            <Alert severity="error" sx={{ mb: 2, borderRadius: 2 }}>
              {passwordError}
            </Alert>
          )}

          <Grid container spacing={3} sx={{ mt: 1 }}>
            <Grid item xs={12}>
              <TextField
                fullWidth
                label="Současné heslo"
                type="password"
                value={passwordData.currentPassword}
                onChange={(e) => setPasswordData(prev => ({ ...prev, currentPassword: e.target.value }))}
                required
                variant="outlined"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'white'
                  }
                }}
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
                variant="outlined"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'white'
                  }
                }}
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
                variant="outlined"
                sx={{
                  '& .MuiOutlinedInput-root': {
                    borderRadius: 2,
                    backgroundColor: 'white'
                  }
                }}
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ p: 3, justifyContent: 'center', gap: 2 }}>
          <Button
            onClick={() => {
              setPasswordDialog(false);
              setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
              setPasswordError(null);
            }}
            disabled={saving}
            variant="outlined"
            sx={{
              borderRadius: 2,
              px: 3,
              borderWidth: 2,
              '&:hover': { borderWidth: 2 }
            }}
          >
            Zrušit
          </Button>
          <Button
            variant="contained"
            onClick={handlePasswordChange}
            disabled={saving}
            startIcon={saving ? <CircularProgress size={16} /> : <KeyIcon />}
            sx={{
              borderRadius: 2,
              px: 3,
              background: 'linear-gradient(45deg, #667eea, #764ba2)',
              '&:hover': {
                background: 'linear-gradient(45deg, #5a6fd8, #6b4190)',
                transform: 'translateY(-2px)'
              },
              transition: 'all 0.3s ease'
            }}
          >
            {saving ? 'Měním heslo...' : 'Změnit heslo'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default Profile;