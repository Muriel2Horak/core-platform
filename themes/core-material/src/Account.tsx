import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  TextField,
  Button,
  Typography,
  Alert,
  Grid,
  Divider,
  Paper,
  Container,
  Avatar,
  Tabs,
  Tab
} from '@mui/material';
import { PersonOutlined, EmailOutlined, SaveOutlined, LockOutlined } from '@mui/icons-material';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel({ children, value, index }: TabPanelProps) {
  return (
    <div hidden={value !== index}>
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

interface AccountProps {
  // Keycloak předá tyto props
  url?: {
    accountUrl?: string;
    passwordUrl?: string;
  };
  realm?: {
    displayName?: string;
  };
  message?: {
    type?: 'success' | 'warning' | 'error' | 'info';
    summary?: string;
  };
  account?: {
    username?: string;
    email?: string;
    firstName?: string;
    lastName?: string;
    emailVerified?: boolean;
  };
  stateChecker?: string;
}

const Account: React.FC<AccountProps> = ({
  url = {},
  realm = {},
  message,
  account = {},
  stateChecker
}) => {
  const [tabValue, setTabValue] = useState(0);
  const [loading, setLoading] = useState(false);
  
  // Personal info form
  const [firstName, setFirstName] = useState(account.firstName || '');
  const [lastName, setLastName] = useState(account.lastName || '');
  const [email, setEmail] = useState(account.email || '');
  
  // Password form
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  const handleTabChange = (event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const handlePersonalInfoSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    setLoading(true);
    
    // Keycloak form submission pro personal info
    if (url.accountUrl) {
      const form = document.createElement('form');
      form.method = 'POST';
      form.action = url.accountUrl;
      
      const firstNameInput = document.createElement('input');
      firstNameInput.type = 'hidden';
      firstNameInput.name = 'firstName';
      firstNameInput.value = firstName;
      form.appendChild(firstNameInput);
      
      const lastNameInput = document.createElement('input');
      lastNameInput.type = 'hidden';
      lastNameInput.name = 'lastName';
      lastNameInput.value = lastName;
      form.appendChild(lastNameInput);
      
      const emailInput = document.createElement('input');
      emailInput.type = 'hidden';
      emailInput.name = 'email';
      emailInput.value = email;
      form.appendChild(emailInput);
      
      if (stateChecker) {
        const stateInput = document.createElement('input');
        stateInput.type = 'hidden';
        stateInput.name = 'stateChecker';
        stateInput.value = stateChecker;
        form.appendChild(stateInput);
      }
      
      document.body.appendChild(form);
      form.submit();
    }
  };

  const handlePasswordSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    
    if (newPassword !== confirmPassword) {
      alert('Nová hesla se neshodují');
      return;
    }
    
    setLoading(true);
    
    // Keycloak form submission pro password
    if (url.passwordUrl) {
      const form = document.createElement('form');
      form.method = 'POST';
      form.action = url.passwordUrl;
      
      const currentPasswordInput = document.createElement('input');
      currentPasswordInput.type = 'hidden';
      currentPasswordInput.name = 'password';
      currentPasswordInput.value = currentPassword;
      form.appendChild(currentPasswordInput);
      
      const newPasswordInput = document.createElement('input');
      newPasswordInput.type = 'hidden';
      newPasswordInput.name = 'password-new';
      newPasswordInput.value = newPassword;
      form.appendChild(newPasswordInput);
      
      const confirmPasswordInput = document.createElement('input');
      confirmPasswordInput.type = 'hidden';
      confirmPasswordInput.name = 'password-confirm';
      confirmPasswordInput.value = confirmPassword;
      form.appendChild(confirmPasswordInput);
      
      if (stateChecker) {
        const stateInput = document.createElement('input');
        stateInput.type = 'hidden';
        stateInput.name = 'stateChecker';
        stateInput.value = stateChecker;
        form.appendChild(stateInput);
      }
      
      document.body.appendChild(form);
      form.submit();
    }
  };

  const getMessageColor = (type?: string) => {
    switch (type) {
      case 'success': return 'success';
      case 'warning': return 'warning';
      case 'error': return 'error';
      case 'info': return 'info';
      default: return 'info';
    }
  };

  const getInitials = () => {
    const first = firstName || account.firstName || '';
    const last = lastName || account.lastName || '';
    return `${first.charAt(0)}${last.charAt(0)}`.toUpperCase();
  };

  return (
    <Box
      sx={{
        minHeight: '100vh',
        backgroundColor: 'background.default',
        py: 4
      }}
    >
      <Container maxWidth="md">
        <Paper elevation={3} sx={{ borderRadius: 3, overflow: 'hidden' }}>
          {/* Header */}
          <Box
            sx={{
              background: 'linear-gradient(135deg, #5D87FF 0%, #4570EA 100%)',
              color: 'white',
              p: 4
            }}
          >
            <Box display="flex" alignItems="center" gap={3}>
              <Avatar
                sx={{
                  width: 80,
                  height: 80,
                  backgroundColor: 'rgba(255,255,255,0.2)',
                  fontSize: '2rem',
                  fontWeight: 'bold'
                }}
              >
                {getInitials()}
              </Avatar>
              <Box>
                <Typography variant="h4" component="h1" gutterBottom>
                  Můj účet
                </Typography>
                <Typography variant="body1" sx={{ opacity: 0.9 }}>
                  {account.username && `@${account.username}`}
                </Typography>
                {realm.displayName && (
                  <Typography variant="body2" sx={{ opacity: 0.8 }}>
                    {realm.displayName}
                  </Typography>
                )}
              </Box>
            </Box>
          </Box>

          {/* Zprávy */}
          {message?.summary && (
            <Box sx={{ p: 3, pb: 0 }}>
              <Alert severity={getMessageColor(message.type)}>
                {message.summary}
              </Alert>
            </Box>
          )}

          {/* Tabs */}
          <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
            <Tabs value={tabValue} onChange={handleTabChange}>
              <Tab label="Osobní údaje" />
              <Tab label="Heslo" />
            </Tabs>
          </Box>

          {/* Personal Info Tab */}
          <TabPanel value={tabValue} index={0}>
            <Box component="form" onSubmit={handlePersonalInfoSubmit}>
              <Typography variant="h6" gutterBottom>
                Osobní informace
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Upravte vaše osobní údaje
              </Typography>

              <Grid container spacing={3}>
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    id="firstName"
                    name="firstName"
                    label="Jméno"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    variant="outlined"
                    required
                    InputProps={{
                      startAdornment: (
                        <PersonOutlined sx={{ color: 'text.secondary', mr: 1 }} />
                      ),
                    }}
                  />
                </Grid>
                
                <Grid item xs={12} sm={6}>
                  <TextField
                    fullWidth
                    id="lastName"
                    name="lastName"
                    label="Příjmení"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    variant="outlined"
                    required
                    InputProps={{
                      startAdornment: (
                        <PersonOutlined sx={{ color: 'text.secondary', mr: 1 }} />
                      ),
                    }}
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    id="email"
                    name="email"
                    label="Email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    variant="outlined"
                    required
                    InputProps={{
                      startAdornment: (
                        <EmailOutlined sx={{ color: 'text.secondary', mr: 1 }} />
                      ),
                    }}
                    helperText={
                      account.emailVerified
                        ? "✓ Email je ověřený"
                        : "⚠ Email není ověřený"
                    }
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    id="username"
                    name="username"
                    label="Uživatelské jméno"
                    value={account.username || ''}
                    variant="outlined"
                    disabled
                    helperText="Uživatelské jméno nelze změnit"
                  />
                </Grid>
              </Grid>

              <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  disabled={loading}
                  startIcon={<SaveOutlined />}
                  sx={{ py: 1.5 }}
                >
                  {loading ? 'Ukládám...' : 'Uložit změny'}
                </Button>
              </Box>
            </Box>
          </TabPanel>

          {/* Password Tab */}
          <TabPanel value={tabValue} index={1}>
            <Box component="form" onSubmit={handlePasswordSubmit}>
              <Typography variant="h6" gutterBottom>
                Změna hesla
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                Chcete-li změnit heslo, vyplňte všechna pole níže
              </Typography>

              <Grid container spacing={3}>
                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    id="currentPassword"
                    name="currentPassword"
                    label="Současné heslo"
                    type="password"
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    variant="outlined"
                    required
                    autoComplete="current-password"
                    InputProps={{
                      startAdornment: (
                        <LockOutlined sx={{ color: 'text.secondary', mr: 1 }} />
                      ),
                    }}
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    id="newPassword"
                    name="newPassword"
                    label="Nové heslo"
                    type="password"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    variant="outlined"
                    required
                    autoComplete="new-password"
                    InputProps={{
                      startAdornment: (
                        <LockOutlined sx={{ color: 'text.secondary', mr: 1 }} />
                      ),
                    }}
                  />
                </Grid>

                <Grid item xs={12}>
                  <TextField
                    fullWidth
                    id="confirmPassword"
                    name="confirmPassword"
                    label="Potvrdit nové heslo"
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    variant="outlined"
                    required
                    autoComplete="new-password"
                    error={confirmPassword !== '' && newPassword !== confirmPassword}
                    helperText={
                      confirmPassword !== '' && newPassword !== confirmPassword
                        ? 'Hesla se neshodují'
                        : ''
                    }
                    InputProps={{
                      startAdornment: (
                        <LockOutlined sx={{ color: 'text.secondary', mr: 1 }} />
                      ),
                    }}
                  />
                </Grid>
              </Grid>

              <Box sx={{ mt: 3, display: 'flex', gap: 2 }}>
                <Button
                  type="submit"
                  variant="contained"
                  size="large"
                  disabled={loading || newPassword !== confirmPassword}
                  startIcon={<SaveOutlined />}
                  sx={{ py: 1.5 }}
                >
                  {loading ? 'Ukládám...' : 'Změnit heslo'}
                </Button>
              </Box>
            </Box>
          </TabPanel>
        </Paper>
      </Container>
    </Box>
  );
};

export default Account;