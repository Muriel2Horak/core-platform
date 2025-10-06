import { useState, useEffect } from 'react';
import PropTypes from 'prop-types';
import {
  Box,
  Card,
  CardContent,
  Typography,
  Grid,
  Avatar,
  Alert,
  Tabs,
  Tab,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Paper,
  Slide,
  Fade,
  IconButton,
  Badge,
  Chip,
} from '@mui/material';
import {
  Person as PersonIcon,
  Email as EmailIcon,
  Business as BusinessIcon,
  Security as SecurityIcon,
  Edit as EditIcon,
  Close as CloseIcon,
  Key as KeyIcon,
  PhotoCamera as CameraIcon,
  Work as WorkIcon,
  LocationOn as LocationIcon,
  SupervisorAccount as ManagerIcon,
  SwapHoriz as DeputyIcon,
  Phone as PhoneIcon,
  AccountBalance as CostCenterIcon,
  Check as CheckIcon,
} from '@mui/icons-material';

// 🎨 Import nového design systému - OPRAVENO: jednotlivé importy z TypeScript souborů
import PageHeader from '../shared/ui/PageHeader';
import FormField from '../shared/ui/FormField';
import AppButton from '../shared/ui/AppButton';
import Loader from '../shared/ui/Loader';
import EmptyState from '../shared/ui/EmptyState';
import { tokens } from '../shared/theme/tokens';

import apiService from '../services/api.js';
import logger from '../services/logger.js';
import { UserPropType } from '../shared/propTypes.js';

// Tab Panel Component s prop validací
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

TabPanel.propTypes = {
  children: PropTypes.node,
  value: PropTypes.number.isRequired,
  index: PropTypes.number.isRequired,
};

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

      const updatedProfile = await apiService.updateMe(editedProfile);
      
      logger.userAction('PROFILE_UPDATED', { 
        userId: profile.id,
        updatedFields: Object.keys(editedProfile).filter(key => 
          editedProfile[key] !== profile[key]
        )
      });
      
      setProfile(updatedProfile);
      setIsEditing(false);
      setSuccess('Profil byl úspěšně aktualizován');

      setTimeout(() => setSuccess(null), 5000);

    } catch (err) {
      console.error('Failed to update profile:', err);
      setError('Nepodařilo se aktualizovat profil: ' + (err.response?.data?.message || err.message));
      logger.error('PROFILE_UPDATE_ERROR', err.message);
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

      await apiService.changeMyPassword({
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword,
        confirmPassword: passwordData.confirmPassword
      });

      logger.userAction('PASSWORD_CHANGED', { userId: profile?.id });

      setPasswordDialog(false);
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
      setSuccess('Heslo bylo úspěšně změněno');

      setTimeout(() => setSuccess(null), 5000);

    } catch (err) {
      console.error('Failed to change password:', err);
      setPasswordError(err.response?.data?.message || err.message || 'Nepodařilo se změnit heslo');
      logger.error('PASSWORD_CHANGE_ERROR', err.message);
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

  // Loading state
  if (loading) {
    return (
      <Box>
        <PageHeader title="Můj profil" />
        <Loader variant="page" text="Načítá se profil..." />
      </Box>
    );
  }

  // Error state
  if (error && !profile) {
    return (
      <Box>
        <PageHeader title="Můj profil" />
        <EmptyState
          icon={<PersonIcon />}
          title="Chyba při načítání profilu"
          description={error}
          action={
            <AppButton variant="primary" onClick={loadProfile}>
              Zkusit znovu
            </AppButton>
          }
        />
      </Box>
    );
  }

  const data = profile || user;

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
      {/* 🎨 Nový PageHeader s akcemi */}
      <PageHeader
        title="Můj profil"
        subtitle="Správa osobních údajů a nastavení účtu"
        actions={
          <AppButton
            variant={isEditing ? 'secondary' : 'primary'}
            startIcon={isEditing ? <CloseIcon /> : <EditIcon />}
            onClick={handleEditToggle}
            loading={saving}
          >
            {isEditing ? 'Zrušit' : 'Upravit profil'}
          </AppButton>
        }
      />

      {/* Success/Error Messages */}
      {success && (
        <Slide direction="down" in={Boolean(success)} mountOnEnter unmountOnExit>
          <Alert
            severity="success"
            onClose={() => setSuccess(null)}
            sx={{ mb: 3, borderRadius: tokens.radius.lg }}
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
            sx={{ mb: 3, borderRadius: tokens.radius.lg }}
            variant="filled"
          >
            {error}
          </Alert>
        </Slide>
      )}

      {/* Profile Header Card - Modern Glassmorphism Design */}
      <Paper
        elevation={0}
        sx={{
          mb: 3,
          borderRadius: tokens.radius.xl,
          background: tokens.colors.gradients.primary,
          color: 'white',
          overflow: 'hidden',
          boxShadow: tokens.shadows.glass,
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
                          backgroundColor: 'rgba(255,255,255,0.9)',
                          color: 'primary.main',
                          border: '2px solid rgba(255,255,255,0.3)',
                          '&:hover': { 
                            backgroundColor: 'rgba(255,255,255,1)',
                            transform: 'scale(1.1)'
                          },
                          transition: 'all 0.3s ease'
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
                      width: 120,
                      height: 120,
                      backgroundColor: 'rgba(255,255,255,0.2)',
                      fontSize: '3rem',
                      fontWeight: 'bold',
                      border: '4px solid rgba(255,255,255,0.3)',
                      boxShadow: tokens.shadows.lg,
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
          </Box>
        </CardContent>
      </Paper>

      {/* Main Content with Tabs */}
      <Paper 
        elevation={0} 
        sx={{ 
          borderRadius: tokens.radius.xl, 
          overflow: 'hidden',
          background: 'rgba(255,255,255,0.7)',
          border: `1px solid ${tokens.colors.grey[200]}`,
          boxShadow: tokens.shadows.md,
        }}
      >
        <Box sx={{
          borderBottom: 1,
          borderColor: 'divider',
          background: `linear-gradient(135deg, ${tokens.colors.grey[50]} 0%, ${tokens.colors.grey[100]} 100%)`,
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
                color: tokens.colors.grey[600], // ✅ Tmavá barva pro lepší kontrast
                '&.Mui-selected': {
                  color: tokens.colors.primary[600], // ✅ Primární barva pro vybraný tab
                }
              },
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
              label="Zástupy"
              icon={<DeputyIcon />}
              iconPosition="top"
            />
            <Tab
              label="Bezpečnost"
              icon={<SecurityIcon />}
              iconPosition="top"
            />
          </Tabs>
        </Box>

        {/* Tab 0: Základní údaje - Nový grid layout podle specs */}
        <TabPanel value={currentTab} index={0}>
          <Typography variant="h5" gutterBottom color="primary" fontWeight="bold">
            📋 Základní údaje
          </Typography>
          <Typography variant="body1" color="text.secondary" paragraph>
            Základní informace o vašem profilu
          </Typography>

          <Grid container spacing={3}>
            {/* ✅ Řádek 1: Jméno (6) + Příjmení (6) */}
            <Grid item xs={12} sm={6}>
              <FormField
                label="Jméno"
                value={editedProfile.firstName || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, firstName: e.target.value }))}
                readonly={!isEditing}
                showEmptyDash={!isEditing}
                startIcon={<PersonIcon />}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <FormField
                label="Příjmení"
                value={editedProfile.lastName || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, lastName: e.target.value }))}
                readonly={!isEditing}
                showEmptyDash={!isEditing}
                startIcon={<PersonIcon />}
              />
            </Grid>

            {/* ✅ Řádek 2: Email (6) + Username (3) + Tenant (3) */}
            <Grid item xs={12} sm={6}>
              <FormField
                label="Email"
                type="email"
                value={editedProfile.email || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, email: e.target.value }))}
                readonly={!isEditing}
                showEmptyDash={!isEditing}
                startIcon={<EmailIcon />}
              />
            </Grid>

            <Grid item xs={12} sm={3}>
              <FormField
                label="Uživatelské jméno"
                value={data?.username || ''}
                readonly={true} // ✅ Vždy readonly podle specs
                showEmptyDash={true}
                helperText="Nelze změnit"
              />
            </Grid>

            <Grid item xs={12} sm={3}>
              <FormField
                label="Tenant"
                value={data?.tenant || ''}
                readonly={true} // ✅ Vždy readonly podle specs
                showEmptyDash={true}
                helperText="Nelze změnit"
              />
            </Grid>

            {/* Akční tlačítka v edit módu */}
            {isEditing && (
              <Grid item xs={12}>
                <Box display="flex" gap={2} justifyContent="center" mt={3}>
                  <AppButton
                    variant="secondary"
                    size="large"
                    onClick={handleEditToggle}
                    disabled={saving}
                  >
                    Zrušit
                  </AppButton>
                  <AppButton
                    variant="primary"
                    size="large"
                    onClick={handleProfileSave}
                    loading={saving}
                  >
                    {saving ? 'Ukládám...' : 'Uložit změny'}
                  </AppButton>
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
              <FormField
                label="Oddělení"
                value={editedProfile.department || data?.department || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, department: e.target.value }))}
                readonly={!isEditing}
                showEmptyDash={!isEditing}
                startIcon={<BusinessIcon />}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <FormField
                label="Pozice"
                value={editedProfile.position || data?.position || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, position: e.target.value }))}
                readonly={!isEditing}
                showEmptyDash={!isEditing}
                startIcon={<WorkIcon />}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <FormField
                label="Manažer"
                value={data?.managerName || data?.manager || ''}
                readonly={true}
                showEmptyDash={true}
                startIcon={<ManagerIcon />}
                helperText="Spravováno systémem"
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <FormField
                label="Středisko"
                value={data?.costCenter || ''}
                readonly={true}
                showEmptyDash={true}
                startIcon={<CostCenterIcon />}
                helperText="Spravováno systémem"
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <FormField
                label="Telefon"
                value={editedProfile.phone || data?.phone || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, phone: e.target.value }))}
                readonly={!isEditing}
                showEmptyDash={!isEditing}
                startIcon={<PhoneIcon />}
              />
            </Grid>

            <Grid item xs={12} sm={6}>
              <FormField
                label="Lokace"
                value={editedProfile.location || data?.location || ''}
                onChange={(e) => setEditedProfile(prev => ({ ...prev, location: e.target.value }))}
                readonly={!isEditing}
                showEmptyDash={!isEditing}
                startIcon={<LocationIcon />}
              />
            </Grid>

            {isEditing && (
              <Grid item xs={12}>
                <Box display="flex" gap={2} justifyContent="center" mt={3}>
                  <AppButton variant="secondary" size="large" onClick={handleEditToggle}>
                    Zrušit
                  </AppButton>
                  <AppButton variant="primary" size="large" onClick={handleProfileSave} loading={saving}>
                    {saving ? 'Ukládám...' : 'Uložit změny'}
                  </AppButton>
                </Box>
              </Grid>
            )}
          </Grid>
        </TabPanel>

        {/* Tab 2: Zástupy - NYNÍ S REÁLNÝMI DATY */}
        <TabPanel value={currentTab} index={2}>
          <Typography variant="h5" gutterBottom color="primary" fontWeight="bold">
            🔄 Zástupy
          </Typography>
          <Typography variant="body1" color="text.secondary" paragraph>
            Správa zástupů při nepřítomnosti
          </Typography>

          <Grid container spacing={3}>
            {/* Aktuální zástup */}
            <Grid item xs={12}>
              <Card
                elevation={0}
                sx={{
                  border: `1px solid ${tokens.colors.grey[200]}`,
                  borderRadius: tokens.radius.lg,
                  background: tokens.colors.grey[50],
                }}
              >
                <CardContent sx={{ p: 3 }}>
                  <Box display="flex" alignItems="center" gap={2} mb={2}>
                    <DeputyIcon color="primary" />
                    <Typography variant="h6" fontWeight="bold">
                      Aktuální zástup
                    </Typography>
                  </Box>

                  <Grid container spacing={3}>
                    <Grid item xs={12} sm={6}>
                      <FormField
                        label="Zástup"
                        value={data?.deputyName || data?.deputy || ''}
                        readonly={true}
                        showEmptyDash={true}
                        helperText="Spravováno manažerem"
                      />
                    </Grid>

                    <Grid item xs={12} sm={3}>
                      <FormField
                        label="Od"
                        type="date"
                        value={data?.deputyFrom ? new Date(data.deputyFrom).toISOString().split('T')[0] : ''}
                        readonly={true}
                        showEmptyDash={true}
                        helperText="Začátek zástupství"
                      />
                    </Grid>

                    <Grid item xs={12} sm={3}>
                      <FormField
                        label="Do"
                        type="date"
                        value={data?.deputyTo ? new Date(data.deputyTo).toISOString().split('T')[0] : ''}
                        readonly={true}
                        showEmptyDash={true}
                        helperText="Konec zástupství"
                      />
                    </Grid>

                    <Grid item xs={12}>
                      <FormField
                        label="Důvod zástupství"
                        value={data?.deputyReason || ''}
                        readonly={true}
                        showEmptyDash={true}
                        multiline
                        rows={2}
                        helperText="Důvod nepřítomnosti"
                      />
                    </Grid>
                  </Grid>

                  {(!data?.deputy || !data?.deputyFrom) && (
                    <Box mt={2} p={2} bgcolor="info.light" borderRadius={1}>
                      <Typography variant="body2" color="info.dark">
                        💡 V současné době nemáte nastaveného zástupu. Obraťte se na svého manažera pro nastavení zástupství při plánované nepřítomnosti.
                      </Typography>
                    </Box>
                  )}
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </TabPanel>

        {/* Tab 3: Bezpečnost */}
        <TabPanel value={currentTab} index={3}>
          <Typography variant="h5" gutterBottom color="primary" fontWeight="bold">
            🔒 Bezpečnost
          </Typography>
          <Typography variant="body1" color="text.secondary" paragraph>
            Správa hesla a bezpečnostních nastavení
          </Typography>

          <Grid container spacing={3}>
            {/* Změna hesla */}
            <Grid item xs={12} sm={6}>
              <Card
                elevation={0}
                sx={{
                  border: `1px solid ${tokens.colors.grey[200]}`,
                  borderRadius: tokens.radius.lg,
                  background: tokens.colors.grey[50],
                }}
              >
                <CardContent sx={{ p: 3 }}>
                  <Box display="flex" alignItems="center" gap={2} mb={2}>
                    <KeyIcon color="primary" />
                    <Typography variant="h6" fontWeight="bold">
                      Změna hesla
                    </Typography>
                  </Box>
                  <Typography variant="body2" color="text.secondary" paragraph>
                    Změňte si heslo pro zvýšení bezpečnosti vašeho účtu
                  </Typography>
                  <AppButton
                    variant="primary"
                    onClick={() => setPasswordDialog(true)}
                    startIcon={<KeyIcon />}
                  >
                    Změnit heslo
                  </AppButton>
                </CardContent>
              </Card>
            </Grid>

            {/* ✅ NOVÉ: Bezpečnostní informace */}
            <Grid item xs={12} sm={6}>
              <Card
                elevation={0}
                sx={{
                  border: `1px solid ${tokens.colors.grey[200]}`,
                  borderRadius: tokens.radius.lg,
                  background: tokens.colors.grey[50],
                }}
              >
                <CardContent sx={{ p: 3 }}>
                  <Box display="flex" alignItems="center" gap={2} mb={2}>
                    <SecurityIcon color="primary" />
                    <Typography variant="h6" fontWeight="bold">
                      Bezpečnostní údaje
                    </Typography>
                  </Box>

                  <Grid container spacing={2}>
                    {/* Uživatelské ID */}
                    <Grid item xs={12}>
                      <FormField
                        label="Uživatelské ID"
                        value={data?.id || data?.sub || ''}
                        readonly={true}
                        showEmptyDash={true}
                        helperText="Systémové identifikační číslo"
                        size="small"
                      />
                    </Grid>

                    {/* ✅ NOVÉ: Role uživatele */}
                    <Grid item xs={12}>
                      <Box sx={{ mb: 1 }}>
                        <Typography variant="body2" color="text.secondary" gutterBottom>
                          Přiřazené role
                        </Typography>
                      </Box>
                      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                        {user?.roles && user.roles.length > 0 ? (
                          user.roles.map((role, index) => {
                            // Mapování rolí na zobrazované názvy
                            const roleDisplayName = {
                              'CORE_ROLE_ADMIN': 'Administrátor',
                              'CORE_USER_MANAGER': 'Správce uživatelů',
                              'CORE_ROLE_USER': 'Uživatel',
                            }[role] || role.replace('CORE_ROLE_', '');

                            // Barva podle typu role
                            const roleColor = {
                              'CORE_ROLE_ADMIN': 'error',
                              'CORE_USER_MANAGER': 'warning',
                              'CORE_ROLE_USER': 'success',
                            }[role] || 'default';

                            return (
                              <Chip
                                key={index}
                                label={roleDisplayName}
                                size="small"
                                color={roleColor}
                                icon={<SecurityIcon />}
                                sx={{ 
                                  fontWeight: 600,
                                  borderRadius: tokens.radius.md,
                                }}
                              />
                            );
                          })
                        ) : (
                          <Chip
                            label="Žádné role"
                            size="small"
                            variant="outlined"
                            sx={{ borderRadius: tokens.radius.md }}
                          />
                        )}
                      </Box>
                      <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                        Role určují vaše oprávnění v systému
                      </Typography>
                    </Grid>

                    {/* Ověření emailu */}
                    <Grid item xs={12}>
                      <Box display="flex" alignItems="center" justifyContent="space-between" p={1}>
                        <Typography variant="body2" color="text.secondary">
                          Stav ověření emailu
                        </Typography>
                        <Chip
                          label={data?.email_verified ? 'Ověřeno' : 'Neověřeno'}
                          size="small"
                          color={data?.email_verified ? 'success' : 'warning'}
                          icon={data?.email_verified ? <CheckIcon /> : <SecurityIcon />}
                        />
                      </Box>
                    </Grid>

                    {/* Session informace */}
                    <Grid item xs={12}>
                      <FormField
                        label="ID relace"
                        value={data?.session_state || data?.session_id || ''}
                        readonly={true}
                        showEmptyDash={true}
                        helperText="Aktuální session ID"
                        size="small"
                      />
                    </Grid>

                    {/* Token expiration */}
                    {data?.token_expires_at && (
                      <Grid item xs={12}>
                        <FormField
                          label="Token vyprší"
                          value={new Date(data.token_expires_at * 1000).toLocaleString('cs-CZ')}
                          readonly={true}
                          showEmptyDash={true}
                          helperText="Čas vypršení přihlášení"
                          size="small"
                        />
                      </Grid>
                    )}
                  </Grid>
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
            borderRadius: tokens.radius.xl,
            boxShadow: tokens.shadows.xl,
          }
        }}
      >
        <DialogTitle>
          <Box display="flex" alignItems="center" justifyContent="space-between">
            <Typography variant="h5" fontWeight="bold">
              🔑 Změna hesla
            </Typography>
            <IconButton
              onClick={() => setPasswordDialog(false)}
              size="small"
              sx={{ color: 'grey.500' }}
            >
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>

        <DialogContent sx={{ pt: 2 }}>
          {passwordError && (
            <Alert severity="error" sx={{ mb: 3, borderRadius: tokens.radius.md }}>
              {passwordError}
            </Alert>
          )}

          <Grid container spacing={3}>
            <Grid item xs={12}>
              <FormField
                type="password"
                label="Současné heslo"
                value={passwordData.currentPassword}
                onChange={(e) => setPasswordData(prev => ({
                  ...prev,
                  currentPassword: e.target.value
                }))}
                required
              />
            </Grid>

            <Grid item xs={12}>
              <FormField
                type="password"
                label="Nové heslo"
                value={passwordData.newPassword}
                onChange={(e) => setPasswordData(prev => ({
                  ...prev,
                  newPassword: e.target.value
                }))}
                required
                helperText="Minimálně 8 znaků"
              />
            </Grid>

            <Grid item xs={12}>
              <FormField
                type="password"
                label="Potvrdit nové heslo"
                value={passwordData.confirmPassword}
                onChange={(e) => setPasswordData(prev => ({
                  ...prev,
                  confirmPassword: e.target.value
                }))}
                required
              />
            </Grid>
          </Grid>
        </DialogContent>

        <DialogActions sx={{ p: 3 }}>
          <AppButton
            variant="secondary"
            onClick={() => setPasswordDialog(false)}
            disabled={saving}
          >
            Zrušit
          </AppButton>
          <AppButton
            variant="primary"
            onClick={handlePasswordChange}
            loading={saving}
          >
            {saving ? 'Měním heslo...' : 'Změnit heslo'}
          </AppButton>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

Profile.propTypes = {
  user: UserPropType,
};

export default Profile;