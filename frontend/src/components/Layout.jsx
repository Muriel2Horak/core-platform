import React, { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Box,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Avatar,
  Menu,
  MenuItem,
  Divider,
  useMediaQuery,
  useTheme,
  Paper,
  Chip,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Dashboard as DashboardIcon,
  People as PeopleIcon,
  Person as PersonIcon,
  Business as BusinessIcon,
  Logout as LogoutIcon,
  AccountCircle as AccountCircleIcon,
  ContactMail as DirectoryIcon,
  Settings as SettingsIcon,
} from '@mui/icons-material';

const drawerWidth = 280;

const menuItems = [
  { 
    id: 'dashboard', 
    label: 'Dashboard', 
    icon: <DashboardIcon />, 
    path: '/dashboard',
    description: 'Přehled systému' 
  },
  { 
    id: 'profile', 
    label: 'Můj profil', 
    icon: <PersonIcon />, 
    path: '/profile',
    description: 'Osobní nastavení' 
  },
  { 
    id: 'user-directory', 
    label: 'User Directory', 
    icon: <DirectoryIcon />, 
    path: '/user-directory', 
    roles: ['CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN'],
    description: 'Vyhledávání uživatelů',
    badge: 'NEW' 
  },
  { 
    id: 'users', 
    label: 'Správa uživatelů', 
    icon: <PeopleIcon />, 
    path: '/users', 
    roles: ['CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN'],
    description: 'Základní správa' 
  },
  { 
    id: 'tenant-management', 
    label: 'Správa tenantů', 
    icon: <SettingsIcon />, 
    path: '/tenant-management', 
    roles: ['CORE_ROLE_ADMIN'],
    description: 'Multi-tenant správa',
    badge: 'NEW' 
  },
  { 
    id: 'tenants', 
    label: 'Tenanti (legacy)', 
    icon: <BusinessIcon />, 
    path: '/tenants', 
    roles: ['CORE_ROLE_ADMIN'],
    description: 'Základní přehled' 
  },
];

function Layout({ children, user, onLogout }) {
  const theme = useTheme();
  const location = useLocation();
  const navigate = useNavigate();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  
  const [mobileOpen, setMobileOpen] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleMenuClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleMenuClose = () => {
    setAnchorEl(null);
  };

  const handleLogout = () => {
    handleMenuClose();
    onLogout();
  };

  const handleNavigation = (path) => {
    navigate(path);
    if (isMobile) {
      setMobileOpen(false);
    }
  };

  // Check if user has required role for menu item
  const hasRole = (requiredRoles) => {
    if (!requiredRoles || requiredRoles.length === 0) return true;
    if (!user?.roles) return false;
    
    return requiredRoles.some(role => user.roles.includes(role));
  };

  // Get user display name
  const getUserDisplayName = () => {
    if (user?.firstName && user?.lastName) {
      return `${user.firstName} ${user.lastName}`;
    }
    return user?.username || user?.email || 'Uživatel';
  };

  // Get user initials for avatar
  const getUserInitials = () => {
    const name = getUserDisplayName();
    const parts = name.split(' ');
    if (parts.length >= 2) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    return name.substring(0, 2).toUpperCase();
  };

  const drawer = (
    <Box sx={{ 
      height: '100%', 
      display: 'flex', 
      flexDirection: 'column',
      background: 'linear-gradient(180deg, #f8f9fa 0%, #e9ecef 100%)'
    }}>
      {/* Logo/Brand - Modern Header */}
      <Paper 
        elevation={2}
        sx={{ 
          m: 2, 
          p: 3, 
          borderRadius: 3,
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          color: 'white',
          textAlign: 'center'
        }}
      >
        <Typography variant="h5" component="div" sx={{ fontWeight: 700, mb: 0.5 }}>
          🚀 Core Platform
        </Typography>
        <Typography variant="body2" sx={{ opacity: 0.9, fontSize: '0.85rem' }}>
          {user?.tenant || 'Multi-tenant systém'}
        </Typography>
      </Paper>

      {/* Navigation - Modern Style */}
      <List sx={{ flexGrow: 1, px: 2, py: 1 }}>
        {menuItems
          .filter(item => hasRole(item.roles))
          .map((item) => (
            <ListItem key={item.id} disablePadding sx={{ mb: 1 }}>
              <ListItemButton
                onClick={() => handleNavigation(item.path)}
                selected={location.pathname === item.path}
                sx={{
                  borderRadius: '12px',
                  py: 1.5,
                  px: 2,
                  transition: 'all 0.3s ease',
                  '&:hover': {
                    backgroundColor: 'rgba(102, 126, 234, 0.1)',
                    transform: 'translateX(4px)',
                    boxShadow: '0 4px 12px rgba(0,0,0,0.1)'
                  },
                  '&.Mui-selected': {
                    backgroundColor: 'rgba(102, 126, 234, 0.15)',
                    boxShadow: '0 4px 12px rgba(102, 126, 234, 0.3)',
                    '&:hover': {
                      backgroundColor: 'rgba(102, 126, 234, 0.2)',
                      transform: 'translateX(4px)',
                    },
                    '& .MuiListItemIcon-root': {
                      color: 'primary.main',
                    },
                    '& .MuiListItemText-primary': {
                      color: 'primary.main',
                      fontWeight: 700,
                    },
                  },
                }}
              >
                <ListItemIcon sx={{ minWidth: 40 }}>
                  {item.icon}
                </ListItemIcon>
                <ListItemText 
                  primary={
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {item.label}
                      </Typography>
                      {item.badge && (
                        <Chip 
                          label={item.badge} 
                          size="small" 
                          color="primary"
                          sx={{ 
                            fontSize: '0.65rem',
                            height: 18,
                            '& .MuiChip-label': { px: 0.8 }
                          }} 
                        />
                      )}
                    </Box>
                  }
                  secondary={
                    <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.7rem' }}>
                      {item.description}
                    </Typography>
                  }
                />
              </ListItemButton>
            </ListItem>
          ))}
      </List>

      {/* User info at bottom - Modern Card */}
      <Paper 
        elevation={2}
        sx={{ 
          m: 2, 
          p: 2, 
          borderRadius: 3,
          background: 'linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)',
          border: '1px solid rgba(102, 126, 234, 0.2)'
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
          <Avatar 
            sx={{ 
              bgcolor: 'primary.main', 
              mr: 2, 
              width: 40, 
              height: 40,
              fontWeight: 'bold',
              boxShadow: '0 4px 12px rgba(102, 126, 234, 0.4)'
            }}
          >
            {getUserInitials()}
          </Avatar>
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="body2" sx={{ fontWeight: 600, color: 'primary.main' }}>
              {getUserDisplayName()}
            </Typography>
            <Typography variant="caption" color="text.secondary" sx={{ fontSize: '0.75rem' }}>
              {user?.email}
            </Typography>
          </Box>
        </Box>
        
        {/* Role badges */}
        {user?.roles && user.roles.length > 0 && (
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5, mt: 1 }}>
            {user.roles.slice(0, 2).map((role) => (
              <Chip
                key={role}
                label={role.replace('CORE_ROLE_', '')}
                size="small"
                variant="outlined"
                color={role.includes('ADMIN') ? 'error' : 'primary'}
                sx={{ 
                  fontSize: '0.65rem',
                  height: 18,
                  '& .MuiChip-label': { px: 0.8 }
                }}
              />
            ))}
            {user.roles.length > 2 && (
              <Chip
                label={`+${user.roles.length - 2}`}
                size="small"
                variant="outlined"
                sx={{ 
                  fontSize: '0.65rem',
                  height: 18,
                  '& .MuiChip-label': { px: 0.8 }
                }}
              />
            )}
          </Box>
        )}
      </Paper>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex' }}>
      {/* App Bar - Modern Gradient */}
      <AppBar
        position="fixed"
        sx={{
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          boxShadow: '0 4px 20px rgba(0,0,0,0.1)'
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { md: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1, fontWeight: 600 }}>
            {menuItems.find(item => item.path === location.pathname)?.label || 'Core Platform'}
          </Typography>

          {/* User menu */}
          <IconButton
            size="large"
            aria-label="account menu"
            aria-controls="account-menu"
            aria-haspopup="true"
            onClick={handleMenuClick}
            color="inherit"
            sx={{
              '&:hover': {
                transform: 'scale(1.05)',
                transition: 'transform 0.2s ease'
              }
            }}
          >
            <Avatar 
              sx={{ 
                bgcolor: 'rgba(255,255,255,0.2)', 
                width: 36, 
                height: 36,
                fontWeight: 'bold',
                border: '2px solid rgba(255,255,255,0.3)'
              }}
            >
              {getUserInitials()}
            </Avatar>
          </IconButton>
          
          <Menu
            id="account-menu"
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleMenuClose}
            onClick={handleMenuClose}
            PaperProps={{
              elevation: 8,
              sx: {
                mt: 1.5,
                minWidth: 220,
                borderRadius: 2,
                background: 'linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)',
                '& .MuiAvatar-root': {
                  width: 24,
                  height: 24,
                  ml: -0.5,
                  mr: 1,
                },
                '& .MuiMenuItem-root': {
                  borderRadius: 1,
                  mx: 1,
                  my: 0.5,
                  '&:hover': {
                    backgroundColor: 'rgba(102, 126, 234, 0.1)',
                    transform: 'translateX(2px)',
                    transition: 'all 0.2s ease'
                  }
                }
              },
            }}
          >
            <MenuItem onClick={() => handleNavigation('/profile')}>
              <AccountCircleIcon fontSize="small" sx={{ mr: 1, color: 'primary.main' }} />
              <Box>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  Můj profil
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Osobní nastavení
                </Typography>
              </Box>
            </MenuItem>
            <Divider sx={{ my: 1 }} />
            <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
              <LogoutIcon fontSize="small" sx={{ mr: 1 }} />
              <Box>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  Odhlásit se
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  Ukončit relaci
                </Typography>
              </Box>
            </MenuItem>
          </Menu>
        </Toolbar>
      </AppBar>

      {/* Sidebar */}
      <Box
        component="nav"
        sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}
      >
        {/* Mobile drawer */}
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true, // Better open performance on mobile.
          }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': {
              boxSizing: 'border-box',
              width: drawerWidth,
            },
          }}
        >
          {drawer}
        </Drawer>
        
        {/* Desktop drawer */}
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': {
              boxSizing: 'border-box',
              width: drawerWidth,
              border: 'none',
              boxShadow: '4px 0 20px rgba(0,0,0,0.1)'
            },
          }}
          open
        >
          {drawer}
        </Drawer>
      </Box>

      {/* Main content */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { md: `calc(100% - ${drawerWidth}px)` },
          minHeight: '100vh',
          backgroundColor: '#f8f9fa',
        }}
      >
        <Toolbar /> {/* Spacer for fixed app bar */}
        {children}
      </Box>
    </Box>
  );
}

export default Layout;