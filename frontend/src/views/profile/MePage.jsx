import React from 'react';
import {
  Box,
  Grid,
  Typography,
  Button,
  Avatar,
  Chip,
  Divider,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  Alert,
  CircularProgress,
  IconButton,
  Tooltip
} from '@mui/material';
import {
  IconUser,
  IconMail,
  IconKey,
  IconSettings,
  IconExternalLink,
  IconShield,
  IconCalendar,
  IconClock,
  IconRefresh,
  IconBuilding,
  IconUsers,
  IconPhone,
  IconMapPin
} from '@tabler/icons-react';

import PageContainer from 'src/components/container/PageContainer';
import DashboardCard from 'src/components/shared/DashboardCard';
import keycloakService from 'src/services/keycloakService';
import { useUserInfo } from 'src/hooks/useUserInfo';

const MePage = () => {
  const { 
    userInfo, 
    loading, 
    error, 
    lastUpdated, 
    refreshUserInfo,
    getDisplayName,
    // hasRole,  // odstraněno - nepoužívá se
    isAdmin,
    tokenExpiresIn
  } = useUserInfo({
    refreshInterval: 3 * 60 * 1000, // Refresh každé 3 minuty
    autoRefresh: true
  });

  if (loading) {
    return (
      <PageContainer title="Můj účet" description="Přehled uživatelského účtu">
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="300px">
          <CircularProgress />
          <Typography variant="h6" sx={{ ml: 2 }}>Načítám aktuální údaje...</Typography>
        </Box>
      </PageContainer>
    );
  }

  if (error) {
    return (
      <PageContainer title="Můj účet" description="Přehled uživatelského účtu">
        <Alert severity="error" sx={{ mb: 2 }}>
          Chyba při načítání údajů: {error.message}
        </Alert>
        <Button variant="contained" onClick={refreshUserInfo}>
          Zkusit znovu
        </Button>
      </PageContainer>
    );
  }

  return (
    <PageContainer title="Můj účet" description="Přehled uživatelského účtu">
      <Grid container spacing={3}>
        
        {/* Header s refresh tlačítkem */}
        <Grid size={{ xs: 12 }}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Typography variant="h4" component="h1">
              Můj účet
            </Typography>
            <Box display="flex" alignItems="center" gap={1}>
              {lastUpdated && (
                <Typography variant="caption" color="text.secondary">
                  Aktualizováno: {lastUpdated.toLocaleTimeString('cs-CZ')}
                </Typography>
              )}
              <Tooltip title="Obnovit údaje">
                <IconButton onClick={refreshUserInfo} size="small">
                  <IconRefresh />
                </IconButton>
              </Tooltip>
            </Box>
          </Box>
        </Grid>

        {/* Uživatelská karta */}
        <Grid size={{ xs: 12, md: 6 }}>
          <DashboardCard title="Základní informace">
            <Box display="flex" alignItems="center" mb={3}>
              <Avatar
                sx={{
                  width: 80,
                  height: 80,
                  fontSize: '1.5rem',
                  bgcolor: isAdmin() ? 'error.main' : 'primary.main',
                  mr: 3
                }}
              >
                {userInfo?.firstName?.charAt(0) || userInfo?.username?.charAt(0) || 'U'}
              </Avatar>
              
              <Box>
                <Typography variant="h5" gutterBottom>
                  {getDisplayName()}
                  {isAdmin() && (
                    <Chip 
                      label="Admin" 
                      color="error" 
                      size="small" 
                      sx={{ ml: 1 }}
                    />
                  )}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  @{userInfo?.username}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {userInfo?.email}
                </Typography>
                {userInfo?.position && (
                  <Typography variant="body2" color="primary">
                    {userInfo.position}
                  </Typography>
                )}
              </Box>
            </Box>

            <Divider sx={{ my: 2 }} />

            <List dense>
              <ListItem>
                <ListItemIcon>
                  <IconUser size={20} />
                </ListItemIcon>
                <ListItemText 
                  primary="Uživatelské jméno" 
                  secondary={userInfo?.username || 'N/A'} 
                />
              </ListItem>
              
              <ListItem>
                <ListItemIcon>
                  <IconMail size={20} />
                </ListItemIcon>
                <ListItemText 
                  primary="E-mail" 
                  secondary={
                    <Box display="flex" alignItems="center" gap={1}>
                      {userInfo?.email || 'N/A'}
                      {userInfo?.emailVerified && (
                        <Chip label="Ověřeno" color="success" size="small" />
                      )}
                    </Box>
                  }
                  secondaryTypographyProps={{ component: 'div' }}
                />
              </ListItem>

              {userInfo?.department && (
                <ListItem>
                  <ListItemIcon>
                    <IconBuilding size={20} />
                  </ListItemIcon>
                  <ListItemText 
                    primary="Oddělení" 
                    secondary={userInfo.department} 
                  />
                </ListItem>
              )}

              {userInfo?.manager && (
                <ListItem>
                  <ListItemIcon>
                    <IconUsers size={20} />
                  </ListItemIcon>
                  <ListItemText 
                    primary="Manažer" 
                    secondary={userInfo.manager} 
                  />
                </ListItem>
              )}

              {userInfo?.phone && (
                <ListItem>
                  <ListItemIcon>
                    <IconPhone size={20} />
                  </ListItemIcon>
                  <ListItemText 
                    primary="Telefon" 
                    secondary={userInfo.phone} 
                  />
                </ListItem>
              )}

              {userInfo?.tokenIssuedAt && (
                <ListItem>
                  <ListItemIcon>
                    <IconCalendar size={20} />
                  </ListItemIcon>
                  <ListItemText 
                    primary="Přihlášen" 
                    secondary={userInfo.tokenIssuedAt.toLocaleString('cs-CZ')} 
                  />
                </ListItem>
              )}
              
              {userInfo?.tokenExpiresAt && (
                <ListItem>
                  <ListItemIcon>
                    <IconClock size={20} />
                  </ListItemIcon>
                  <ListItemText 
                    primary="Token vyprší" 
                    secondary={
                      <Box display="flex" alignItems="center" gap={1}>
                        {userInfo.tokenExpiresAt.toLocaleString('cs-CZ')}
                        {tokenExpiresIn && tokenExpiresIn < 300 && (
                          <Chip 
                            label={`${Math.floor(tokenExpiresIn / 60)}min`} 
                            color="warning" 
                            size="small" 
                          />
                        )}
                      </Box>
                    }
                    secondaryTypographyProps={{ component: 'div' }}
                  />
                </ListItem>
              )}
            </List>
          </DashboardCard>
        </Grid>

        {/* Role a akce */}
        <Grid size={{ xs: 12, md: 6 }}>
          <DashboardCard title="Role a oprávnění">
            <Box mb={3}>
              <Typography variant="h6" gutterBottom sx={{ display: 'flex', alignItems: 'center' }}>
                <IconShield size={20} style={{ marginRight: 8 }} />
                Přiřazené role
              </Typography>
              
              {userInfo?.roles && userInfo.roles.length > 0 ? (
                <Box display="flex" flexWrap="wrap" gap={1}>
                  {userInfo.roles.map((role, index) => (
                    <Chip 
                      key={index}
                      label={role}
                      variant="outlined"
                      color={role.includes('admin') ? 'error' : 'primary'}
                      size="small"
                    />
                  ))}
                </Box>
              ) : (
                <Alert severity="info" size="small">
                  Žádné role nejsou přiřazeny
                </Alert>
              )}
            </Box>

            <Divider sx={{ my: 2 }} />

            <Typography variant="h6" gutterBottom>
              Rychlé akce
            </Typography>
            
            <Box display="flex" flexDirection="column" gap={1}>
              <Button
                variant="outlined"
                startIcon={<IconKey />}
                onClick={() => keycloakService.openPasswordChange()}
                fullWidth
              >
                Změnit heslo
              </Button>
              
              <Button
                variant="outlined"
                color="info"
                startIcon={<IconSettings />}
                onClick={() => keycloakService.openPersonalInfo()}
                fullWidth
              >
                Upravit osobní údaje
              </Button>
              
              <Button
                variant="outlined"
                color="secondary"
                startIcon={<IconExternalLink />}
                onClick={() => keycloakService.openAccountConsole()}
                fullWidth
              >
                Keycloak Account Console
              </Button>
            </Box>
          </DashboardCard>
        </Grid>

        {/* Rozšířené informace pro workflow */}
        {(userInfo?.department || userInfo?.manager) && (
          <Grid size={{ xs: 12 }}>
            <DashboardCard title="Organizační struktura">
              <Alert severity="info" sx={{ mb: 2 }}>
                Tyto informace jsou používány pro schvalovací workflow a řízení přístupů.
              </Alert>
              
              <Grid container spacing={3}>
                {userInfo?.department && (
                  <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <Box textAlign="center">
                      <IconBuilding size={40} color="#1976d2" />
                      <Typography variant="h6" gutterBottom>
                        Oddělení
                      </Typography>
                      <Typography variant="body1" color="text.secondary">
                        {userInfo.department}
                      </Typography>
                    </Box>
                  </Grid>
                )}

                {userInfo?.manager && (
                  <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <Box textAlign="center">
                      <IconUsers size={40} color="#1976d2" />
                      <Typography variant="h6" gutterBottom>
                        Manažer
                      </Typography>
                      <Typography variant="body1" color="text.secondary">
                        {userInfo.manager}
                      </Typography>
                    </Box>
                  </Grid>
                )}

                {userInfo?.position && (
                  <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                    <Box textAlign="center">
                      <IconMapPin size={40} color="#1976d2" />
                      <Typography variant="h6" gutterBottom>
                        Pozice
                      </Typography>
                      <Typography variant="body1" color="text.secondary">
                        {userInfo.position}
                      </Typography>
                    </Box>
                  </Grid>
                )}

                <Grid size={{ xs: 12, sm: 6, md: 3 }}>
                  <Box textAlign="center">
                    <IconShield size={40} color={isAdmin() ? '#d32f2f' : '#1976d2'} />
                    <Typography variant="h6" gutterBottom>
                      Oprávnění
                    </Typography>
                    <Typography variant="body1" color="text.secondary">
                      {isAdmin() ? 'Administrátor' : 'Standardní uživatel'}
                    </Typography>
                  </Box>
                </Grid>
              </Grid>
            </DashboardCard>
          </Grid>
        )}

        {/* Token informace pro vývojáře */}
        {import.meta.env.DEV && userInfo && (
          <Grid size={{ xs: 12 }}>
            <DashboardCard title="Debug informace (Development)">
              <Alert severity="info" sx={{ mb: 2 }}>
                Tyto informace jsou zobrazeny pouze ve vývojovém prostředí.
              </Alert>
              
              <Box component="pre" sx={{ 
                backgroundColor: 'grey.100', 
                p: 2, 
                borderRadius: 1,
                fontSize: '0.75rem',
                overflow: 'auto',
                maxHeight: '300px'
              }}>
                {JSON.stringify({
                  issuer: userInfo.issuer,
                  tokenIssuedAt: userInfo.tokenIssuedAt?.toISOString(),
                  tokenExpiresAt: userInfo.tokenExpiresAt?.toISOString(),
                  tokenExpiresIn: tokenExpiresIn,
                  lastUpdated: lastUpdated?.toISOString(),
                  userInfo: userInfo
                }, null, 2)}
              </Box>
            </DashboardCard>
          </Grid>
        )}

      </Grid>
    </PageContainer>
  );
};

export default MePage;