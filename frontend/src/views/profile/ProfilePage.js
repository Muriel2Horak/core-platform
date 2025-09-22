import React, { useState, useEffect } from 'react';
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
  Avatar,
  Chip,
  Tabs,
  Tab,
  Autocomplete,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Card,
  CardContent,
  Fade,
  Slide,
  Paper,
  IconButton,
  Badge
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import dayjs from 'dayjs';
import 'dayjs/locale/cs';
import { 
  IconUser, 
  IconKey, 
  IconMail, 
  IconEdit, 
  IconCheck, 
  IconX,
  IconBuilding,
  IconUsers,
  IconPhone,
  IconMapPin,
  IconCalendar,
  IconShield,
  IconUserCheck,
  IconBriefcase,
  IconCamera,
  IconCloud,
  IconServer
} from '@tabler/icons-react';
import PageContainer from '../../components/container/PageContainer';
import userManagementService from '../../services/userManagementService';
import authService from '../../services/auth';
import logger from '../../services/logger';

// Nastavení lokalizace pro dayjs
dayjs.locale('cs');

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

const ProfilePage = () => {
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

  // User suggestions for manager/deputy
  const [userSuggestions, setUserSuggestions] = useState([]);

  useEffect(() => {
    loadProfile();
    loadUserSuggestions();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      setError(null);
      
      logger.pageView('/profile', { operation: 'load_profile' });
      
      const data = await userManagementService.getMyProfile();
      setProfile(data);
      
      // Převést datum hodnoty na dayjs objekty pro DatePicker komponenty
      const editedData = { 
        ...data,
        deputyFrom: data.deputyFrom ? dayjs(data.deputyFrom) : null,
        deputyTo: data.deputyTo ? dayjs(data.deputyTo) : null
      };
      setEditedProfile(editedData);
      
      logger.userAction('PROFILE_LOADED', { userId: data.id });
      
    } catch (err) {
      console.error('Failed to load profile:', err);
      setError('Nepodařilo se načíst profil uživatele');
      logger.error('PROFILE_LOAD_ERROR', err.message);
    } finally {
      setLoading(false);
    }
  };

  const loadUserSuggestions = async () => {
    try {
      // Zkontrolovat, zda má uživatel oprávnění pro načítání seznamu uživatelů
      const currentUserInfo = await authService.getUserInfo();
      const hasUserManagerRole = currentUserInfo?.roles?.includes('CORE_ROLE_USER_MANAGER') || 
                                 currentUserInfo?.roles?.includes('CORE_ROLE_ADMIN');
      
      if (!hasUserManagerRole) {
        console.log('User does not have permission to load user suggestions');
        setUserSuggestions([]);
        return;
      }
      
      const users = await userManagementService.searchUsers({});
      const suggestions = users.map(user => ({
        label: `${user.firstName} ${user.lastName} (${user.username})`,
        value: user.username,
        department: user.department
      }));
      setUserSuggestions(suggestions);
    } catch (err) {
      console.error('Failed to load user suggestions:', err);
      // Fallback na prázdný seznam namísto chyby
      setUserSuggestions([]);
    }
  };

  const handleTabChange = (event, newValue) => {
    setCurrentTab(newValue);
  };

  const handleEditToggle = () => {
    if (isEditing) {
      // Zrušit změny a převést datum hodnoty na dayjs objekty
      const resetData = { 
        ...profile,
        deputyFrom: profile.deputyFrom ? dayjs(profile.deputyFrom) : null,
        deputyTo: profile.deputyTo ? dayjs(profile.deputyTo) : null
      };
      setEditedProfile(resetData);
    }
    setIsEditing(!isEditing);
    logger.userAction(isEditing ? 'PROFILE_EDIT_CANCELLED' : 'PROFILE_EDIT_STARTED');
  };

  const handleProfileSave = async () => {
    try {
      setSaving(true);
      setError(null);
      setSuccess(null);
      
      // Převést dayjs objekty na ISO string pro backend
      const dataToSave = {
        ...editedProfile,
        deputyFrom: editedProfile.deputyFrom ? editedProfile.deputyFrom.format('YYYY-MM-DD') : null,
        deputyTo: editedProfile.deputyTo ? editedProfile.deputyTo.format('YYYY-MM-DD') : null
      };
      
      const updatedProfile = await userManagementService.updateMyProfile(dataToSave);
      setProfile(updatedProfile);
      
      // Aktualizovat editedProfile s novými daty a převést datum hodnoty na dayjs objekty
      const editedData = {
        ...updatedProfile,
        deputyFrom: updatedProfile.deputyFrom ? dayjs(updatedProfile.deputyFrom) : null,
        deputyTo: updatedProfile.deputyTo ? dayjs(updatedProfile.deputyTo) : null
      };
      setEditedProfile(editedData);
      
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
          <CircularProgress size={60} thickness={4} />
        </Box>
      </PageContainer>
    );
  }

  return (
    <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="cs">
      <PageContainer title="Můj profil" description="Správa uživatelského profilu">
        <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
          
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

          {/* Profile Header Card - Redesigned */}
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
                  {/* Profilová fotka s možností změny */}
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
                            <IconCamera size={16} />
                            <input
                              type="file"
                              hidden
                              accept="image/*"
                              onChange={(e) => {
                                const file = e.target.files[0];
                                if (file) {
                                  // TODO: Implementovat upload profilové fotky
                                  console.log('Selected file:', file);
                                }
                              }}
                            />
                          </IconButton>
                        ) : null
                      }
                    >
                      <Avatar 
                        src={profile?.profilePicture}
                        sx={{ 
                          width: 100, 
                          height: 100,
                          backgroundColor: 'rgba(255,255,255,0.2)',
                          fontSize: '2.5rem',
                          fontWeight: 'bold',
                          border: '4px solid rgba(255,255,255,0.3)'
                        }}
                      >
                        {!profile?.profilePicture && getInitials()}
                      </Avatar>
                    </Badge>
                  </Box>
                  
                  <Box>
                    <Typography variant="h3" fontWeight="bold" gutterBottom>
                      {getDisplayName()}
                    </Typography>
                    
                    <Box display="flex" alignItems="center" gap={1} mb={1}>
                      <Typography variant="h6" sx={{ opacity: 0.9 }}>
                        @{profile?.username}
                      </Typography>
                      
                      {/* Zobrazení zdroje identity */}
                      <Chip
                        size="small"
                        icon={profile?.isLocalUser ? <IconServer size={16} /> : <IconCloud size={16} />}
                        label={profile?.identityProviderAlias || 'Neznámý zdroj'}
                        sx={{
                          backgroundColor: profile?.isLocalUser 
                            ? 'rgba(76, 175, 80, 0.8)'  // Zelená pro lokální
                            : 'rgba(33, 150, 243, 0.8)', // Modrá pro federovaný
                          color: 'white',
                          fontWeight: 'bold',
                          ml: 1
                        }}
                      />
                    </Box>
                    
                    {/* Federovaný username pokud existuje */}
                    {profile?.federatedUsername && (
                      <Typography variant="body2" sx={{ opacity: 0.8 }} gutterBottom>
                        AD účet: {profile.federatedUsername}
                      </Typography>
                    )}
                    
                    {/* Role odebrány - přesunuty do tabu Bezpečnost */}
                  </Box>
                </Box>

                {/* Floating Edit Button */}
                <Box>
                  <Button
                    variant={isEditing ? "outlined" : "contained"}
                    size="large"
                    startIcon={isEditing ? <IconX /> : <IconEdit />}
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

          {/* Main Content with Tabs - Enhanced */}
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
                  icon={<IconUser size={24} />} 
                  iconPosition="top"
                />
                <Tab 
                  label="Organizační struktura" 
                  icon={<IconBuilding size={24} />} 
                  iconPosition="top"
                />
                <Tab 
                  label="Zastupování" 
                  icon={<IconUserCheck size={24} />} 
                  iconPosition="top"
                />
                <Tab 
                  label="Bezpečnost" 
                  icon={<IconShield size={24} />} 
                  iconPosition="top"
                />
              </Tabs>
            </Box>

            {/* Tab 0: Základní údaje - Enhanced */}
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
                      startAdornment: <IconUser size={20} style={{ marginRight: 8, color: '#667eea' }} />
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
                      startAdornment: <IconUser size={20} style={{ marginRight: 8, color: '#667eea' }} />
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
                      startAdornment: <IconMail size={20} style={{ marginRight: 8, color: '#667eea' }} />
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
                    label="Telefon"
                    value={editedProfile.phone || ''}
                    onChange={(e) => setEditedProfile(prev => ({ ...prev, phone: e.target.value }))}
                    disabled={!isEditing}
                    variant="outlined"
                    InputProps={{
                      startAdornment: <IconPhone size={20} style={{ marginRight: 8, color: '#667eea' }} />
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
                    value={profile?.username || ''}
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
                        startIcon={saving ? <CircularProgress size={16} /> : <IconCheck />}
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

            {/* Tab 1: Organizační struktura - Enhanced */}
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
                      startAdornment: <IconBuilding size={20} style={{ marginRight: 8, color: '#667eea' }} />
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
                    label="Pozice"
                    value={editedProfile.position || ''}
                    onChange={(e) => setEditedProfile(prev => ({ ...prev, position: e.target.value }))}
                    disabled={!isEditing}
                    variant="outlined"
                    InputProps={{
                      startAdornment: <IconBriefcase size={20} style={{ marginRight: 8, color: '#667eea' }} />
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
                  <Autocomplete
                    options={userSuggestions}
                    value={userSuggestions.find(u => u.value === editedProfile.manager) || null}
                    onChange={(event, newValue) => {
                      setEditedProfile(prev => ({ ...prev, manager: newValue?.value || '' }));
                    }}
                    disabled={!isEditing}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        label="Nadřízený"
                        variant="outlined"
                        InputProps={{
                          ...params.InputProps,
                          startAdornment: <IconUsers size={20} style={{ marginRight: 8, color: '#667eea' }} />,
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
                    )}
                  />
                </Grid>
                
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    label="Nákladové středisko"
                    value={editedProfile.costCenter || ''}
                    onChange={(e) => setEditedProfile(prev => ({ ...prev, costCenter: e.target.value }))}
                    disabled={!isEditing}
                    variant="outlined"
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
                    label="Lokace / Pobočka"
                    value={editedProfile.location || ''}
                    onChange={(e) => setEditedProfile(prev => ({ ...prev, location: e.target.value }))}
                    disabled={!isEditing}
                    variant="outlined"
                    InputProps={{
                      startAdornment: <IconMapPin size={20} style={{ marginRight: 8, color: '#667eea' }} />
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

                {(profile?.managerName || profile?.department) && (
                  <Grid item xs={12}>
                    <Card 
                      sx={{ 
                        background: 'linear-gradient(135deg, #e3f2fd 0%, #f3e5f5 100%)',
                        borderRadius: 2,
                        border: '1px solid rgba(102, 126, 234, 0.2)'
                      }}
                    >
                      <CardContent>
                        <Typography variant="h6" gutterBottom color="primary" fontWeight="bold">
                          📊 Aktuální organizační struktura
                        </Typography>
                        {profile.managerName && (
                          <Typography variant="body1" sx={{ mb: 1 }}>
                            <strong>👤 Nadřízený:</strong> {profile.managerName}
                          </Typography>
                        )}
                        {profile.department && (
                          <Typography variant="body1">
                            <strong>🏢 Oddělení:</strong> {profile.department}
                          </Typography>
                        )}
                      </CardContent>
                    </Card>
                  </Grid>
                )}

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
                        startIcon={saving ? <CircularProgress size={16} /> : <IconCheck />}
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

            {/* Tab 2: Zastupování - Enhanced */}
            <TabPanel value={currentTab} index={2}>
              <Typography variant="h5" gutterBottom color="primary" fontWeight="bold">
                👥 Zastupování
              </Typography>
              <Typography variant="body1" color="text.secondary" paragraph>
                Definujte, kdo vás bude zastupovat a v jakém období
              </Typography>
              
              <Grid container spacing={3}>
                <Grid item xs={12} sm={6}>
                  <Autocomplete
                    options={userSuggestions}
                    value={userSuggestions.find(u => u.value === editedProfile.deputy) || null}
                    onChange={(event, newValue) => {
                      setEditedProfile(prev => ({ ...prev, deputy: newValue?.value || '' }));
                    }}
                    disabled={!isEditing}
                    renderInput={(params) => (
                      <TextField
                        {...params}
                        label="Zástup"
                        variant="outlined"
                        InputProps={{
                          ...params.InputProps,
                          startAdornment: <IconUserCheck size={20} style={{ marginRight: 8, color: '#667eea' }} />,
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
                    )}
                  />
                </Grid>
                
                <Grid item xs={12} sm={6}>
                  <FormControl fullWidth disabled={!isEditing} variant="outlined">
                    <InputLabel>Důvod zastupování</InputLabel>
                    <Select
                      value={editedProfile.deputyReason || ''}
                      onChange={(e) => setEditedProfile(prev => ({ ...prev, deputyReason: e.target.value }))}
                      label="Důvod zastupování"
                      sx={{
                        borderRadius: 2,
                        '&:hover .MuiOutlinedInput-notchedOutline': {
                          borderColor: 'primary.main',
                        }
                      }}
                    >
                      <MenuItem value="dovolená">🏖️ Dovolená</MenuItem>
                      <MenuItem value="nemocenská">🏥 Nemocenská</MenuItem>
                      <MenuItem value="služební cesta">✈️ Služební cesta</MenuItem>
                      <MenuItem value="školení">📚 Školení</MenuItem>
                      <MenuItem value="jiný">❓ Jiný důvod</MenuItem>
                    </Select>
                  </FormControl>
                </Grid>
                
                <Grid item xs={12} sm={6}>
                  <DatePicker
                    label="Zastupování od"
                    value={editedProfile.deputyFrom}
                    onChange={(newValue) => setEditedProfile(prev => ({ ...prev, deputyFrom: newValue }))}
                    disabled={!isEditing}
                    renderInput={(params) => (
                      <TextField 
                        {...params} 
                        fullWidth 
                        variant="outlined"
                        InputProps={{
                          ...params.InputProps,
                          startAdornment: <IconCalendar size={20} style={{ marginRight: 8, color: '#667eea' }} />
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
                    )}
                  />
                </Grid>
                
                <Grid item xs={12} sm={6}>
                  <DatePicker
                    label="Zastupování do"
                    value={editedProfile.deputyTo}
                    onChange={(newValue) => setEditedProfile(prev => ({ ...prev, deputyTo: newValue }))}
                    disabled={!isEditing}
                    renderInput={(params) => (
                      <TextField 
                        {...params} 
                        fullWidth 
                        variant="outlined"
                        InputProps={{
                          ...params.InputProps,
                          startAdornment: <IconCalendar size={20} style={{ marginRight: 8, color: '#667eea' }} />
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
                    )}
                  />
                </Grid>

                {(profile?.deputyName || profile?.deputyReason) && (
                  <Grid item xs={12}>
                    <Card 
                      sx={{ 
                        background: 'linear-gradient(135deg, #e8f5e8 0%, #f0f8f0 100%)',
                        borderRadius: 2,
                        border: '1px solid rgba(76, 175, 80, 0.3)'
                      }}
                    >
                      <CardContent>
                        <Typography variant="h6" gutterBottom sx={{ color: 'success.main' }} fontWeight="bold">
                          ✅ Aktivní zastupování
                        </Typography>
                        {profile.deputyName && (
                          <Typography variant="body1" sx={{ mb: 1 }}>
                            <strong>👤 Zástup:</strong> {profile.deputyName}
                          </Typography>
                        )}
                        {profile.deputyReason && (
                          <Typography variant="body1" sx={{ mb: 1 }}>
                            <strong>📝 Důvod:</strong> {profile.deputyReason}
                          </Typography>
                        )}
                        {profile.deputyFrom && profile.deputyTo && (
                          <Typography variant="body1">
                            <strong>📅 Období:</strong> {new Date(profile.deputyFrom).toLocaleDateString('cs-CZ')} - {new Date(profile.deputyTo).toLocaleDateString('cs-CZ')}
                          </Typography>
                        )}
                      </CardContent>
                    </Card>
                  </Grid>
                )}

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
                        startIcon={saving ? <CircularProgress size={16} /> : <IconCheck />}
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

            {/* Tab 3: Bezpečnost - Enhanced */}
            <TabPanel value={currentTab} index={3}>
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
                        {profile?.roles?.length > 0 ? (
                          profile.roles.map((role) => (
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
                        startIcon={<IconKey />}
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
                            label={profile?.enabled ? 'Aktivní' : 'Neaktivní'} 
                            color={profile?.enabled ? 'success' : 'error'}
                            size="small"
                          />
                          <Typography variant="body2" component="span" sx={{ fontSize: '0.9rem', color: '#666' }}>
                            Status účtu
                          </Typography>
                        </Box>
                        
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Chip 
                            label={profile?.emailVerified ? 'Ověřeno' : 'Neověřeno'} 
                            color={profile?.emailVerified ? 'success' : 'warning'}
                            size="small"
                          />
                          <Typography variant="body2" component="span" sx={{ fontSize: '0.9rem', color: '#666' }}>
                            Email
                          </Typography>
                        </Box>
                        
                        {profile?.createdTimestamp && (
                          <Typography variant="body2" color="text.secondary">
                            <strong>📅 Registrace:</strong> {new Date(profile.createdTimestamp).toLocaleDateString('cs-CZ')}
                          </Typography>
                        )}
                      </Box>
                    </CardContent>
                  </Card>
                </Grid>
              </Grid>
            </TabPanel>
          </Paper>
        </Box>

        {/* Password Change Dialog - Enhanced */}
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
              startIcon={saving ? <CircularProgress size={16} /> : <IconKey />}
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
      </PageContainer>
    </LocalizationProvider>
  );
};

export default ProfilePage;