import React, { useState } from 'react';
import {
  Box,
  Paper,
  TextField,
  Button,
  Typography,
  Container,
  Avatar,
  Alert,
  CircularProgress,
  Divider,
  Card,
  CardContent,
  Grid,
  Chip,
} from '@mui/material';
import { LoadingButton } from '@mui/lab';
import LoginIcon from '@mui/icons-material/Login';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import LogoutIcon from '@mui/icons-material/Logout';
import EmailIcon from '@mui/icons-material/Email';
import PersonIcon from '@mui/icons-material/Person';
import BadgeIcon from '@mui/icons-material/Badge';

import { useUser } from '../lib/auth';
import { login, logout } from '../lib/api';
import { LoginData } from '../lib/types';

const AccountPage: React.FC = () => {
  const { user, mutate, loggedOut, loading } = useUser();
  const [loginError, setLoginError] = useState<string | null>(null);
  const [isLogging, setIsLogging] = useState(false);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setLoginError(null);
    setIsLogging(true);

    const formData = new FormData(event.currentTarget);
    const loginData: LoginData = {
      username: formData.get('username') as string,
      password: formData.get('password') as string,
    };

    try {
      const userData = await login(loginData);
      mutate(userData);
    } catch (error: any) {
      console.error('Login failed:', error);
      if (error.response && error.response.status === 401) {
        setLoginError('Neplatné přihlašovací údaje.');
      } else {
        setLoginError('Došlo k chybě při přihlašování. Zkuste to prosím znovu.');
      }
    } finally {
      setIsLogging(false);
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
      mutate(undefined);
    } catch (error) {
      console.error('Logout failed:', error);
    }
  };

  if (loading) {
    return (
      <Container maxWidth="sm">
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: '60vh',
          }}
        >
          <CircularProgress size={48} />
          <Typography variant="body1" sx={{ mt: 2 }} color="text.secondary">
            Načítání informací o účtu...
          </Typography>
        </Box>
      </Container>
    );
  }

  if (loggedOut || !user) {
    return (
      <Container maxWidth="sm">
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: '60vh',
          }}
        >
          <Paper
            elevation={6}
            sx={{
              p: 4,
              width: '100%',
              borderRadius: 3,
            }}
          >
            <Box sx={{ textAlign: 'center', mb: 3 }}>
              <Avatar
                sx={{
                  mx: 'auto',
                  bgcolor: 'primary.main',
                  width: 64,
                  height: 64,
                  mb: 2,
                }}
              >
                <LoginIcon fontSize="large" />
              </Avatar>
              <Typography component="h1" variant="h4" fontWeight="600">
                Přihlášení
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Přihlaste se ke svému účtu
              </Typography>
            </Box>

            <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1 }}>
              <TextField
                margin="normal"
                required
                fullWidth
                id="username"
                label="Uživatelské jméno"
                name="username"
                autoComplete="username"
                autoFocus
                defaultValue="test"
                variant="outlined"
              />
              <TextField
                margin="normal"
                required
                fullWidth
                name="password"
                label="Heslo"
                type="password"
                id="password"
                autoComplete="current-password"
                defaultValue="test"
                variant="outlined"
              />

              {loginError && (
                <Alert severity="error" sx={{ mt: 2 }}>
                  {loginError}
                </Alert>
              )}

              <LoadingButton
                type="submit"
                fullWidth
                variant="contained"
                loading={isLogging}
                startIcon={<LoginIcon />}
                sx={{
                  mt: 3,
                  mb: 2,
                  py: 1.5,
                  fontSize: '1rem',
                }}
              >
                Přihlásit se
              </LoadingButton>
            </Box>
          </Paper>
        </Box>
      </Container>
    );
  }

  return (
    <Container maxWidth="md">
      <Box sx={{ py: 4 }}>
        <Paper
          elevation={3}
          sx={{
            p: 4,
            borderRadius: 3,
          }}
        >
          <Box sx={{ textAlign: 'center', mb: 4 }}>
            <Avatar
              sx={{
                mx: 'auto',
                bgcolor: 'success.main',
                width: 80,
                height: 80,
                mb: 2,
              }}
            >
              <AccountCircleIcon fontSize="large" />
            </Avatar>
            <Typography variant="h4" component="h1" fontWeight="600" gutterBottom>
              Můj účet
            </Typography>
            <Chip
              label="Přihlášený"
              color="success"
              variant="outlined"
              size="small"
            />
          </Box>

          <Divider sx={{ my: 3 }} />

          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <BadgeIcon color="primary" sx={{ mr: 1 }} />
                    <Typography variant="h6">ID uživatele</Typography>
                  </Box>
                  <Typography variant="body1" color="text.secondary">
                    {user.id}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={6}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <PersonIcon color="primary" sx={{ mr: 1 }} />
                    <Typography variant="h6">Uživatelské jméno</Typography>
                  </Box>
                  <Typography variant="body1" color="text.secondary">
                    {user.username}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={6}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <PersonIcon color="primary" sx={{ mr: 1 }} />
                    <Typography variant="h6">Jméno</Typography>
                  </Box>
                  <Typography variant="body1" color="text.secondary">
                    {user.firstName} {user.lastName}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>

            <Grid item xs={12} md={6}>
              <Card variant="outlined" sx={{ height: '100%' }}>
                <CardContent>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
                    <EmailIcon color="primary" sx={{ mr: 1 }} />
                    <Typography variant="h6">Email</Typography>
                  </Box>
                  <Typography variant="body1" color="text.secondary">
                    {user.email}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          <Box sx={{ mt: 4, textAlign: 'center' }}>
            <Button
              variant="outlined"
              color="error"
              size="large"
              startIcon={<LogoutIcon />}
              onClick={handleLogout}
              sx={{ px: 4, py: 1.5 }}
            >
              Odhlásit se
            </Button>
          </Box>
        </Paper>
      </Box>
    </Container>
  );
};

export default AccountPage;