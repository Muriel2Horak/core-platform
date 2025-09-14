import React from 'react';
import {
  Box,
  Typography,
  Button,
  Paper,
  Grid,
  Container,
  Card,
  CardContent,
  Avatar,
} from '@mui/material';
import { Link } from 'react-router-dom';
import SecurityIcon from '@mui/icons-material/Security';
import SpeedIcon from '@mui/icons-material/Speed';
import CloudIcon from '@mui/icons-material/Cloud';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';

const HomePage: React.FC = () => {
  return (
    <Container maxWidth="lg">
      {/* Hero Section */}
      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          textAlign: 'center',
          py: 8,
        }}
      >
        <Typography
          variant="h2"
          component="h1"
          gutterBottom
          sx={{
            fontWeight: 700,
            background: 'linear-gradient(45deg, #1976d2 30%, #9c27b0 90%)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            mb: 3,
          }}
        >
          Vítejte v Core Platform
        </Typography>
        
        <Typography
          variant="h5"
          component="h2"
          color="text.secondary"
          sx={{ mb: 4, maxWidth: 600 }}
        >
          Moderní, bezpečná a škálovatelná platforma pro vaše aplikace
        </Typography>
        
        <Button
          variant="contained"
          size="large"
          component={Link}
          to="/account"
          startIcon={<AccountCircleIcon />}
          sx={{
            px: 4,
            py: 1.5,
            fontSize: '1.1rem',
            boxShadow: 3,
            '&:hover': {
              boxShadow: 6,
            },
          }}
        >
          Přihlásit se
        </Button>
      </Box>

      {/* Features Section */}
      <Box sx={{ py: 8 }}>
        <Typography
          variant="h4"
          component="h2"
          textAlign="center"
          gutterBottom
          sx={{ mb: 6 }}
        >
          Klíčové funkce
        </Typography>
        
        <Grid container spacing={4}>
          <Grid item xs={12} md={4}>
            <Card
              elevation={3}
              sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                transition: 'transform 0.2s ease-in-out',
                '&:hover': {
                  transform: 'translateY(-4px)',
                },
              }}
            >
              <CardContent sx={{ flexGrow: 1, textAlign: 'center', p: 4 }}>
                <Avatar
                  sx={{
                    bgcolor: 'primary.main',
                    width: 64,
                    height: 64,
                    mx: 'auto',
                    mb: 2,
                  }}
                >
                  <SecurityIcon fontSize="large" />
                </Avatar>
                <Typography variant="h6" component="h3" gutterBottom>
                  Bezpečnost
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Enterprise-grade bezpečnost s Keycloak autentizací a moderními bezpečnostními standardy.
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={4}>
            <Card
              elevation={3}
              sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                transition: 'transform 0.2s ease-in-out',
                '&:hover': {
                  transform: 'translateY(-4px)',
                },
              }}
            >
              <CardContent sx={{ flexGrow: 1, textAlign: 'center', p: 4 }}>
                <Avatar
                  sx={{
                    bgcolor: 'secondary.main',
                    width: 64,
                    height: 64,
                    mx: 'auto',
                    mb: 2,
                  }}
                >
                  <SpeedIcon fontSize="large" />
                </Avatar>
                <Typography variant="h6" component="h3" gutterBottom>
                  Výkon
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Optimalizovaná architektura pro rychlé načítání a plynulý uživatelský zážitek.
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid item xs={12} md={4}>
            <Card
              elevation={3}
              sx={{
                height: '100%',
                display: 'flex',
                flexDirection: 'column',
                transition: 'transform 0.2s ease-in-out',
                '&:hover': {
                  transform: 'translateY(-4px)',
                },
              }}
            >
              <CardContent sx={{ flexGrow: 1, textAlign: 'center', p: 4 }}>
                <Avatar
                  sx={{
                    bgcolor: 'success.main',
                    width: 64,
                    height: 64,
                    mx: 'auto',
                    mb: 2,
                  }}
                >
                  <CloudIcon fontSize="large" />
                </Avatar>
                <Typography variant="h6" component="h3" gutterBottom>
                  Škálovatelnost
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Cloudová architektura připravená pro růst a vysoké zatížení vaší aplikace.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Box>

      {/* CTA Section */}
      <Paper
        elevation={2}
        sx={{
          py: 6,
          px: 4,
          textAlign: 'center',
          background: 'linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%)',
        }}
      >
        <Typography variant="h5" component="h3" gutterBottom>
          Připraveni začít?
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          Přihlaste se a objevte všechny možnosti naší platformy.
        </Typography>
        <Button
          variant="contained"
          size="large"
          component={Link}
          to="/account"
          sx={{ px: 4, py: 1.5 }}
        >
          Přejít k účtu
        </Button>
      </Paper>
    </Container>
  );
};

export default HomePage;