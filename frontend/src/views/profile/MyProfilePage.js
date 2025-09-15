import React, { useState, useEffect } from 'react';
import {
  Box,
  Grid,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Alert,
  Divider,
  Avatar,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  CircularProgress,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  IconUser,
  IconMail,
  IconKey,
  IconShield,
  IconEdit,
  IconCheck,
  IconX,
  IconUserCircle
} from '@tabler/icons-react';

import PageContainer from 'src/components/container/PageContainer';
import DashboardCard from 'src/components/shared/DashboardCard';
import authService from 'src/services/auth';

const MyProfilePage = () => {
  const [userInfo, setUserInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [passwordDialogOpen, setPasswordDialogOpen] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });

  // Form states
  const [formData, setFormData] = useState({
    firstName: '',
    lastName: '',
    email: '',
    username: ''
  });

  // Password form state
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: ''
  });

  const [passwordErrors, setPasswordErrors] = useState({});
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadUserInfo();
  }, []);

  const loadUserInfo = async () => {
    try {
      setLoading(true);
      const user = await authService.getUserInfo();
      setUserInfo(user);
      setFormData({
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        email: user.email || '',
        username: user.username || ''
      });
    } catch (error) {
      console.error('Chyba při načítání profilu:', error);
      setMessage({ type: 'error', text: 'Nepodařilo se načíst profil uživatele' });
    } finally {
      setLoading(false);
    }
  };

  const handleSaveProfile = async () => {
    try {
      setSaving(true);
      setMessage({ type: '', text: '' });

      const response = await authService.apiCall('/auth/profile', {
        method: 'PUT',
        body: JSON.stringify({
          firstName: formData.firstName,
          lastName: formData.lastName,
          email: formData.email
        })
      });

      if (response && response.ok) {
        const updatedUser = await response.json();
        setUserInfo(updatedUser);
        setEditing(false);
        setMessage({ type: 'success', text: 'Profil byl úspěšně aktualizován' });
      } else if (response) {
        const error = await response.json();
        setMessage({ type: 'error', text: error.detail || 'Nepodařilo se aktualizovat profil' });
      } else {
        // authService.apiCall vrátí null při 401 - uživatel byl odhlášen
        setMessage({ type: 'error', text: 'Relace vypršela, budete přesměrováni na přihlášení' });
      }
    } catch (error) {
      console.error('Chyba při ukládání profilu:', error);
      setMessage({ type: 'error', text: 'Chyba při ukládání profilu' });
    } finally {
      setSaving(false);
    }
  };

  const handleChangePassword = async () => {
    try {
      setPasswordErrors({});
      
      if (passwordData.newPassword !== passwordData.confirmPassword) {
        setPasswordErrors({ confirmPassword: 'Hesla se neshodují' });
        return;
      }

      if (passwordData.newPassword.length < 6) {
        setPasswordErrors({ newPassword: 'Heslo musí mít alespoň 6 znaků' });
        return;
      }

      setSaving(true);

      const response = await authService.apiCall('/auth/change-password', {
        method: 'POST',
        body: JSON.stringify({
          currentPassword: passwordData.currentPassword,
          newPassword: passwordData.newPassword
        })
      });

      if (response && response.ok) {
        setPasswordDialogOpen(false);
        setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' });
        setMessage({ type: 'success', text: 'Heslo bylo úspěšně změněno' });
      } else if (response) {
        const error = await response.json();
        setMessage({ type: 'error', text: error.detail || 'Nepodařilo se změnit heslo' });
      } else {
        // authService.apiCall vrátí null při 401 - uživatel byl odhlášen
        setMessage({ type: 'error', text: 'Relace vypršela, budete přesměrováni na přihlášení' });
      }
    } catch (error) {
      console.error('Chyba při změně hesla:', error);
      setMessage({ type: 'error', text: 'Chyba při změně hesla' });
    } finally {
      setSaving(false);
    }
  };

  const handleCancelEdit = () => {
    setFormData({
      firstName: userInfo?.firstName || '',
      lastName: userInfo?.lastName || '',
      email: userInfo?.email || '',
      username: userInfo?.username || ''
    });
    setEditing(false);
    setMessage({ type: '', text: '' });
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
        {/* Hlavní profil */}
        <Grid item xs={12} md={8}>
          <DashboardCard title="Osobní údaje">
            {message.text && (
              <Alert severity={message.type} sx={{ mb: 3 }}>
                {message.text}
              </Alert>
            )}
            
            <Grid container spacing={3}>
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Křestní jméno"
                  value={formData.firstName}
                  onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                  disabled={!editing}
                  InputProps={{
                    startAdornment: <IconUser size={20} style={{ marginRight: 8 }} />
                  }}
                />
              </Grid>
              
              <Grid item xs={12} sm={6}>
                <TextField
                  fullWidth
                  label="Příjmení"
                  value={formData.lastName}
                  onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                  disabled={!editing}
                  InputProps={{
                    startAdornment: <IconUser size={20} style={{ marginRight: 8 }} />
                  }}
                />
              </Grid>
              
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="E-mail"
                  type="email"
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  disabled={!editing}
                  InputProps={{
                    startAdornment: <IconMail size={20} style={{ marginRight: 8 }} />
                  }}
                />
              </Grid>
              
              <Grid item xs={12}>
                <TextField
                  fullWidth
                  label="Uživatelské jméno"
                  value={formData.username}
                  disabled={true}
                  helperText="Uživatelské jméno nelze změnit"
                  InputProps={{
                    startAdornment: <IconUserCircle size={20} style={{ marginRight: 8 }} />
                  }}
                />
              </Grid>
            </Grid>
            
            <Box mt={3} display="flex" gap={2}>
              {!editing ? (
                <>
                  <Button
                    variant="contained"
                    startIcon={<IconEdit />}
                    onClick={() => setEditing(true)}
                  >
                    Upravit profil
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<IconKey />}
                    onClick={() => setPasswordDialogOpen(true)}
                  >
                    Změnit heslo
                  </Button>
                </>
              ) : (
                <>
                  <Button
                    variant="contained"
                    startIcon={<IconCheck />}
                    onClick={handleSaveProfile}
                    disabled={saving}
                  >
                    {saving ? <CircularProgress size={20} /> : 'Uložit'}
                  </Button>
                  <Button
                    variant="outlined"
                    startIcon={<IconX />}
                    onClick={handleCancelEdit}
                    disabled={saving}
                  >
                    Zrušit
                  </Button>
                </>
              )}
            </Box>
          </DashboardCard>
        </Grid>

        {/* Sidebar s avatarem a základními info */}
        <Grid item xs={12} md={4}>
          <DashboardCard title="Profil">
            <Box display="flex" flexDirection="column" alignItems="center" textAlign="center">
              <Avatar
                sx={{
                  width: 100,
                  height: 100,
                  fontSize: '2rem',
                  bgcolor: 'primary.main',
                  mb: 2
                }}
              >
                {userInfo?.firstName?.charAt(0) || userInfo?.username?.charAt(0) || 'U'}
              </Avatar>
              
              <Typography variant="h5" gutterBottom>
                {userInfo?.firstName && userInfo?.lastName 
                  ? `${userInfo.firstName} ${userInfo.lastName}`
                  : userInfo?.name || userInfo?.username || 'Uživatel'
                }
              </Typography>
              
              <Typography variant="body2" color="textSecondary" gutterBottom>
                @{userInfo?.username}
              </Typography>
              
              <Typography variant="body2" color="textSecondary">
                {userInfo?.email}
              </Typography>
            </Box>
          </DashboardCard>
        </Grid>

        {/* Role uživatele */}
        <Grid item xs={12}>
          <DashboardCard title="Uživatelské role">
            <Box display="flex" alignItems="center" mb={2}>
              <IconShield size={20} style={{ marginRight: 8 }} />
              <Typography variant="h6">Přiřazené role</Typography>
            </Box>
            
            {userInfo?.roles && userInfo.roles.length > 0 ? (
              <TableContainer component={Paper} variant="outlined">
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>Role</TableCell>
                      <TableCell>Popis</TableCell>
                      <TableCell>Status</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {userInfo.roles.map((role, index) => (
                      <TableRow key={index}>
                        <TableCell>
                          <Box display="flex" alignItems="center">
                            <Chip 
                              label={role}
                              variant="outlined"
                              color={role.includes('admin') ? 'error' : 'primary'}
                              size="small"
                            />
                          </Box>
                        </TableCell>
                        <TableCell>
                          {getRoleDescription(role)}
                        </TableCell>
                        <TableCell>
                          <Chip 
                            label="Aktivní"
                            color="success"
                            size="small"
                          />
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            ) : (
              <Alert severity="info">
                Nejsou přiřazeny žádné role
              </Alert>
            )}
          </DashboardCard>
        </Grid>
      </Grid>

      {/* Dialog pro změnu hesla */}
      <Dialog 
        open={passwordDialogOpen} 
        onClose={() => setPasswordDialogOpen(false)}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Změna hesla</DialogTitle>
        <DialogContent>
          <Box pt={1}>
            <TextField
              fullWidth
              label="Současné heslo"
              type="password"
              value={passwordData.currentPassword}
              onChange={(e) => setPasswordData({ ...passwordData, currentPassword: e.target.value })}
              margin="normal"
              error={!!passwordErrors.currentPassword}
              helperText={passwordErrors.currentPassword}
            />
            
            <TextField
              fullWidth
              label="Nové heslo"
              type="password"
              value={passwordData.newPassword}
              onChange={(e) => setPasswordData({ ...passwordData, newPassword: e.target.value })}
              margin="normal"
              error={!!passwordErrors.newPassword}
              helperText={passwordErrors.newPassword || "Heslo musí mít alespoň 6 znaků"}
            />
            
            <TextField
              fullWidth
              label="Potvrdit nové heslo"
              type="password"
              value={passwordData.confirmPassword}
              onChange={(e) => setPasswordData({ ...passwordData, confirmPassword: e.target.value })}
              margin="normal"
              error={!!passwordErrors.confirmPassword}
              helperText={passwordErrors.confirmPassword}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPasswordDialogOpen(false)} disabled={saving}>
            Zrušit
          </Button>
          <Button 
            onClick={handleChangePassword} 
            variant="contained"
            disabled={saving || !passwordData.currentPassword || !passwordData.newPassword || !passwordData.confirmPassword}
          >
            {saving ? <CircularProgress size={20} /> : 'Změnit heslo'}
          </Button>
        </DialogActions>
      </Dialog>
    </PageContainer>
  );
};

// Pomocná funkce pro popis rolí
const getRoleDescription = (role) => {
  const descriptions = {
    'admin': 'Administrátorská role s plnými právy',
    'user': 'Standardní uživatelská role',
    'moderator': 'Moderátorská role s rozšířenými právy',
    'editor': 'Role editora s právy k úpravám obsahu',
    'viewer': 'Role pouze pro čtení',
    'default-roles-core-platform': 'Výchozí systémová role',
    'offline_access': 'Přístup k offline funkcím',
    'uma_authorization': 'Autorizační role'
  };
  
  return descriptions[role] || 'Systémová role';
};

export default MyProfilePage;