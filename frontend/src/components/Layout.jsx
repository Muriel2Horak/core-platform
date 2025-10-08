import { useState, useEffect, useCallback, useRef } from 'react';
import PropTypes from 'prop-types';
import { useLocation, useNavigate } from 'react-router-dom';
import {
  AppBar,
  Box,
  Drawer,
  IconButton,
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
  Badge,
  Skeleton,
  Tooltip,
  ListSubheader,
} from '@mui/material';
import {
  Menu as MenuIcon,
  Logout as LogoutIcon,
  AccountCircle as AccountCircleIcon,
  SwapHoriz as SwitchTenantIcon,
  Domain as DomainIcon,
  ExpandMore as ExpandMoreIcon,
  CheckCircle as CheckCircleIcon,
  ChevronLeft as ChevronLeftIcon,
  ChevronRight as ChevronRightIcon,
} from '@mui/icons-material';

// 🎨 Import nového design systému - OPRAVENO: přímý import z TypeScript souborů
import SidebarNav from '../shared/ui/SidebarNav';
import { tokens } from '../shared/theme/tokens';
import { defaultMenuItems } from '../shared/ui/SidebarNav';
import apiService from '../services/api.js';
import { UserPropType } from '../shared/propTypes.js';

// 🎯 Resize constants
const SIDEBAR_MIN_WIDTH = 60; // Collapsed mode (icons only)
const SIDEBAR_MAX_WIDTH = 400; // Maximum expanded width
const SIDEBAR_DEFAULT_WIDTH = parseInt(tokens.components.layout.sidebarWidth, 10); // 240px
const SIDEBAR_COLLAPSE_THRESHOLD = 150; // Auto-collapse below this width
const SIDEBAR_STORAGE_KEY = 'sidebar-width';

const drawerWidthExpanded = parseInt(tokens.components.layout.sidebarWidth, 10); // 280px z tokens
const drawerWidthCollapsed = 72; // Šířka pro collapsed režim

function Layout({ children, user, onLogout }) {
  const theme = useTheme();
  const location = useLocation();
  const navigate = useNavigate();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  
  const [mobileOpen, setMobileOpen] = useState(false);
  
  // 📏 Resizable sidebar width
  const [sidebarWidth, setSidebarWidth] = useState(() => {
    const saved = localStorage.getItem(SIDEBAR_STORAGE_KEY);
    return saved ? parseInt(saved, 10) : SIDEBAR_DEFAULT_WIDTH;
  });
  
  const [isResizing, setIsResizing] = useState(false);
  const drawerRef = useRef(null);
  
  // Auto-collapse when width is below threshold
  const sidebarCollapsed = sidebarWidth < SIDEBAR_COLLAPSE_THRESHOLD;
  
  const [anchorEl, setAnchorEl] = useState(null);
  
  // ✅ FIXED: Používáme user.tenant místo API volání
  const [availableTenants, setAvailableTenants] = useState([]);
  const [tenantMenuAnchor, setTenantMenuAnchor] = useState(null);

  useEffect(() => {
    // ✅ FIXED: Odstraněno loadTenantInfo() - tenant už je v user objektu
    if (user?.roles?.includes('CORE_ROLE_ADMIN')) {
      loadAvailableTenants();
    }
  }, [user]);

  const loadAvailableTenants = async () => {
    try {
      const tenants = await apiService.getAllTenants();
      setAvailableTenants(tenants || []);
    } catch (error) {
      console.error('Failed to load available tenants:', error);
    }
  };

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

  // 🎯 Handler pro SidebarNav
  const handleSidebarNavClick = (item) => {
    navigate(item.href);
    if (isMobile) {
      setMobileOpen(false);
    }
  };

  // 🖱️ Resize handlers
  const handleMouseMove = useCallback((e) => {
    if (!isResizing) return;
    
    const newWidth = e.clientX;
    
    // Clamp between min and max
    if (newWidth >= SIDEBAR_MIN_WIDTH && newWidth <= SIDEBAR_MAX_WIDTH) {
      setSidebarWidth(newWidth);
      localStorage.setItem(SIDEBAR_STORAGE_KEY, newWidth.toString());
    }
  }, [isResizing]);

  const handleMouseUp = useCallback(() => {
    setIsResizing(false);
  }, []);

  const handleMouseDown = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    setIsResizing(true);
  }, []);

  // 📌 Add/remove event listeners for resize
  useEffect(() => {
    if (isResizing) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    } else {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
  }, [isResizing, handleMouseMove, handleMouseUp]);

  const handleTenantMenuOpen = (event) => {
    setTenantMenuAnchor(event.currentTarget);
  };

  const handleTenantMenuClose = () => {
    setTenantMenuAnchor(null);
  };

  const handleTenantSwitch = (tenantKey) => {
    const tenantUrl = `https://${tenantKey}.core-platform.local`;
    window.location.href = tenantUrl;
    handleTenantMenuClose();
  };

  const toggleSidebar = () => {
    // Toggle between collapsed (60px) and default width (240px)
    const newWidth = sidebarCollapsed ? SIDEBAR_DEFAULT_WIDTH : SIDEBAR_MIN_WIDTH;
    setSidebarWidth(newWidth);
    localStorage.setItem(SIDEBAR_STORAGE_KEY, newWidth.toString());
  };

  // Use sidebarWidth directly instead of conditional
  const drawerWidth = sidebarWidth;

  const getUserDisplayName = () => {
    if (user?.firstName && user?.lastName) {
      return `${user.firstName} ${user.lastName}`;
    }
    return user?.username || user?.email || 'Uživatel';
  };

  const getUserInitials = () => {
    if (!user) {
      return '??';  // Fallback pokud user není dostupný
    }
    
    const name = getUserDisplayName();
    if (!name || name === 'Uživatel') {
      return '??';  // Fallback pro neznámého uživatele
    }
    
    const parts = name.split(' ');
    if (parts.length >= 2 && parts[0] && parts[1]) {
      return `${parts[0][0]}${parts[1][0]}`.toUpperCase();
    }
    
    if (name.length >= 2) {
      return name.substring(0, 2).toUpperCase();
    }
    
    return name.toUpperCase();
  };

  const getTenantDisplayInfo = () => {
    // ✅ SIMPLIFIED: Zobrazí přímo hodnotu z user.tenant bez mapování
    const tenantKey = user?.tenant;
    
    if (!tenantKey) {
      return { name: 'Unknown Tenant', key: 'unknown' };
    }
    
    return {
      name: tenantKey,  // Zobrazí přímo tenant key (např. "admin")
      key: tenantKey
    };
  };

  // 🎨 Detekce dark módu
  const prefersDarkMode = theme.palette.mode === 'dark';

  // 🎨 Glassmorphic drawer design s SidebarNav
  const drawer = (
    <Box sx={{ 
      height: '100%', 
      display: 'flex', 
      flexDirection: 'column',
      background: prefersDarkMode 
        ? 'linear-gradient(180deg, #1a1a1a 0%, #2d2d2d 100%)' 
        : 'linear-gradient(180deg, #ffffff 0%, #f5f5f7 100%)',
      borderRight: prefersDarkMode
        ? '1px solid rgba(255, 255, 255, 0.15)'
        : '1px solid rgba(0, 0, 0, 0.1)',
    }}>
      {/* Header s Axiom logo */}
      <Paper 
        elevation={0}
        sx={{ 
          m: 2, 
          p: sidebarCollapsed ? 1.5 : 3, 
          borderRadius: tokens.radius.xl,
          background: prefersDarkMode 
            ? 'rgba(255, 255, 255, 0.08)' 
            : 'rgba(0, 0, 0, 0.04)',
          textAlign: 'center',
          border: prefersDarkMode
            ? '1px solid rgba(255, 255, 255, 0.15)'
            : '1px solid rgba(0, 0, 0, 0.1)',
          transition: 'all 0.3s ease',
          position: 'relative',
        }}
      >
        {!sidebarCollapsed && (
          <>
            <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'center', flex: 1 }}>
                {/* Axiom Logo SVG */}
                <svg width="40" height="40" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <defs>
                    <linearGradient id="axiomGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                      <stop offset="0%" style={{stopColor: '#1976d2', stopOpacity: 1}} />
                      <stop offset="100%" style={{stopColor: '#42a5f5', stopOpacity: 1}} />
                    </linearGradient>
                  </defs>
                  {/* A symbol with geometric design */}
                  <path d="M20 2 L35 15 L30 15 L20 8 L10 15 L5 15 Z" fill="url(#axiomGradient)" />
                  <path d="M8 18 L12 18 L20 28 L28 18 L32 18 L20 33 Z" fill="url(#axiomGradient)" />
                  <rect x="18" y="16" width="4" height="18" fill="url(#axiomGradient)" opacity="0.8" />
                </svg>
              </Box>
              
              {/* Toggle Button - beside logo */}
              {!isMobile && (
                <IconButton
                  onClick={toggleSidebar}
                  size="small"
                  sx={{
                    bgcolor: prefersDarkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.05)',
                    '&:hover': {
                      bgcolor: prefersDarkMode ? 'rgba(255, 255, 255, 0.15)' : 'rgba(0, 0, 0, 0.08)',
                    },
                  }}
                >
                  <ChevronLeftIcon fontSize="small" />
                </IconButton>
              )}
            </Box>
            <Typography variant="h5" component="div" sx={{ 
              fontWeight: tokens.typography.fontWeight.bold, 
              mb: 0.5,
              fontSize: tokens.typography.fontSize.xl,
              background: 'linear-gradient(135deg, #1976d2 0%, #42a5f5 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundClip: 'text',
            }}>
              Axiom
            </Typography>
            <Typography variant="body2" sx={{ 
              opacity: 0.7, 
              fontSize: tokens.typography.fontSize.sm,
            }}>
              {getTenantDisplayInfo().name}
            </Typography>
          </>
        )}
        {sidebarCollapsed && (
          <Box sx={{ 
            display: 'flex', 
            flexDirection: 'column',
            alignItems: 'center', 
            justifyContent: 'center',
            gap: 1,
          }}>
            {/* Compact Logo */}
            <svg width="32" height="32" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="axiomGradientSmall" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style={{stopColor: '#1976d2', stopOpacity: 1}} />
                  <stop offset="100%" style={{stopColor: '#42a5f5', stopOpacity: 1}} />
                </linearGradient>
              </defs>
              <path d="M20 2 L35 15 L30 15 L20 8 L10 15 L5 15 Z" fill="url(#axiomGradientSmall)" />
              <path d="M8 18 L12 18 L20 28 L28 18 L32 18 L20 33 Z" fill="url(#axiomGradientSmall)" />
              <rect x="18" y="16" width="4" height="18" fill="url(#axiomGradientSmall)" opacity="0.8" />
            </svg>
            
            {/* Expand button for collapsed state */}
            {!isMobile && (
              <IconButton
                onClick={toggleSidebar}
                size="small"
                sx={{
                  bgcolor: prefersDarkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.05)',
                  '&:hover': {
                    bgcolor: prefersDarkMode ? 'rgba(255, 255, 255, 0.15)' : 'rgba(0, 0, 0, 0.08)',
                  },
                }}
              >
                <ChevronRightIcon fontSize="small" />
              </IconButton>
            )}
          </Box>
        )}
      </Paper>

      {/* 🎨 Nový SidebarNav komponent */}
      <Box sx={{
        flexGrow: 1, 
        overflow: 'auto',
        overflowX: 'hidden',
        px: 1,
        '&::-webkit-scrollbar': {
          width: '6px',
        },
        '&::-webkit-scrollbar-track': {
          background: 'transparent',
        },
        '&::-webkit-scrollbar-thumb': {
          background: prefersDarkMode ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.2)',
          borderRadius: '3px',
          '&:hover': {
            background: prefersDarkMode ? 'rgba(255, 255, 255, 0.3)' : 'rgba(0, 0, 0, 0.3)',
          },
        },
      }}>
        <SidebarNav
          onItemClick={handleSidebarNavClick}
          currentPath={location.pathname}
          userRoles={user?.roles || []}
          collapsed={!isMobile && sidebarCollapsed}
        />
      </Box>
    </Box>
  );

  // 🔍 Najdeme aktivní menu item pro AppBar title
  const getPageTitle = () => {
    const activeItem = defaultMenuItems.find(item => {
      if (item.href === '/' && location.pathname === '/') return true;
      if (item.href !== '/' && location.pathname.startsWith(item.href)) return true;
      return false;
    });
    return activeItem?.label || 'Axiom';
  };

  return (
    <Box sx={{ display: 'flex' }}>
      {/* AppBar - glassmorphic design */}
      <AppBar
        position="fixed"
        sx={{
          width: { md: `calc(100% - ${drawerWidth}px)` },
          ml: { md: `${drawerWidth}px` },
          background: prefersDarkMode 
            ? 'rgba(30, 30, 30, 0.8)' 
            : 'rgba(255, 255, 255, 0.8)',
          backdropFilter: 'blur(20px) saturate(180%)',
          borderBottom: prefersDarkMode
            ? '1px solid rgba(255, 255, 255, 0.1)'
            : '1px solid rgba(0, 0, 0, 0.08)',
          boxShadow: 'none',
          color: prefersDarkMode ? '#ffffff' : '#1a1a1a',
          transition: 'width 0.3s cubic-bezier(0.4, 0, 0.2, 1), margin 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        }}
      >
        <Toolbar sx={{ justifyContent: 'space-between', minHeight: tokens.components.layout.headerHeight }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <IconButton
              color="inherit"
              aria-label="open drawer"
              edge="start"
              onClick={handleDrawerToggle}
              sx={{ 
                display: { md: 'none' },
                '&:focus-visible': {
                  outline: `${tokens.a11y.focusRing.width} ${tokens.a11y.focusRing.style} ${tokens.a11y.focusRing.color}`,
                  outlineOffset: tokens.a11y.focusRing.offset,
                },
              }}
            >
              <MenuIcon />
            </IconButton>
            
            <Typography variant="h6" noWrap component="div" sx={{ 
              fontWeight: tokens.typography.fontWeight.semibold,
              fontSize: tokens.typography.fontSize.lg,
            }}>
              {getPageTitle()}
            </Typography>
          </Box>

          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {/* User menu button */}
            <Tooltip title="Uživatelské menu">
              <IconButton
                size="large"
                aria-label="account menu"
                aria-controls="account-menu"
                aria-haspopup="true"
                onClick={handleMenuClick}
                color="inherit"
                sx={{
                  background: prefersDarkMode 
                    ? 'rgba(255,255,255,0.1)' 
                    : 'rgba(0,0,0,0.05)',
                  backdropFilter: 'blur(10px)',
                  border: prefersDarkMode
                    ? '1px solid rgba(255, 255, 255, 0.1)'
                    : '1px solid rgba(0, 0, 0, 0.08)',
                  transition: `all ${tokens.components.animation.normal} ${tokens.components.animation.easing}`,
                  '&:hover': {
                    background: prefersDarkMode 
                      ? 'rgba(255,255,255,0.15)' 
                      : 'rgba(0,0,0,0.08)',
                    transform: 'scale(1.05)',
                  },
                  '&:focus-visible': {
                    outline: `${tokens.a11y.focusRing.width} ${tokens.a11y.focusRing.style} ${tokens.a11y.focusRing.color}`,
                    outlineOffset: tokens.a11y.focusRing.offset,
                  },
                }}
              >
                <Badge
                  badgeContent={user?.roles?.includes('CORE_ROLE_ADMIN') ? '👑' : null}
                  anchorOrigin={{
                    vertical: 'top',
                    horizontal: 'right',
                  }}
                  sx={{
                    '& .MuiBadge-badge': {
                      fontSize: '10px',
                      minWidth: 16,
                      height: 16,
                      background: 'transparent',
                      color: '#FFD700',
                      border: 'none'
                    }
                  }}
                >
                  <Avatar 
                    sx={{ 
                      bgcolor: 'primary.main', 
                      width: 36, 
                      height: 36,
                      fontWeight: tokens.typography.fontWeight.bold,
                    }}
                  >
                    {getUserInitials()}
                  </Avatar>
                </Badge>
              </IconButton>
            </Tooltip>
          </Box>
        </Toolbar>
      </AppBar>

      {/* Tenant switch menu */}
      <Menu
        id="tenant-menu"
        anchorEl={tenantMenuAnchor}
        open={Boolean(tenantMenuAnchor)}
        onClose={handleTenantMenuClose}
        PaperProps={{
          elevation: 8,
          sx: {
            mt: 1.5,
            minWidth: 280,
            maxWidth: 400,
            borderRadius: tokens.radius.lg,
            background: theme => theme.palette.mode === 'dark'
              ? 'linear-gradient(135deg, rgba(30, 30, 30, 0.95) 0%, rgba(50, 50, 50, 0.95) 100%)'
              : 'linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255,255,255,0.2)',
            boxShadow: tokens.shadows.xxl,
            '& .MuiMenuItem-root': {
              borderRadius: tokens.radius.md,
              mx: 1,
              my: 0.5,
              px: 2,
              py: 1.5,
              transition: `all ${tokens.components.animation.normal} ${tokens.components.animation.easing}`,
              '&:hover': {
                backgroundColor: theme => theme.palette.mode === 'dark'
                  ? 'rgba(25, 118, 210, 0.15)'
                  : 'rgba(25, 118, 210, 0.08)',
                transform: 'translateX(4px)',
              },
              '&:focus-visible': {
                outline: `${tokens.a11y.focusRing.width} ${tokens.a11y.focusRing.style} ${tokens.a11y.focusRing.color}`,
                outlineOffset: tokens.a11y.focusRing.offset,
              },
            }
          },
        }}
      >
        <ListSubheader sx={{ 
          background: 'transparent', 
          fontWeight: tokens.typography.fontWeight.semibold,
          color: 'text.primary',
          fontSize: tokens.typography.fontSize.sm,
        }}>
          🏢 Přepnout tenant
        </ListSubheader>
        {availableTenants.map((tenant) => (
          <MenuItem 
            key={tenant.key}
            onClick={() => handleTenantSwitch(tenant.key)}
            disabled={tenant.key === user?.tenant?.key}
            sx={{
              opacity: tenant.key === user?.tenant?.key ? 0.6 : 1
            }}
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, width: '100%' }}>
              <Avatar
                sx={{
                  width: 32,
                  height: 32,
                  fontSize: tokens.typography.fontSize.sm,
                  fontWeight: tokens.typography.fontWeight.bold,
                  bgcolor: tenant.key === user?.tenant?.key ? 'primary.main' : 'grey.400'
                }}
              >
                {(tenant.name || tenant.key).substring(0, 2).toUpperCase()}
              </Avatar>
              <Box sx={{ flexGrow: 1 }}>
                <Typography variant="body2" sx={{ fontWeight: tokens.typography.fontWeight.semibold }}>
                  {tenant.name || tenant.key}
                </Typography>
                <Typography variant="caption" color="text.secondary">
                  {tenant.key}
                </Typography>
              </Box>
              {tenant.key === user?.tenant?.key && (
                <CheckCircleIcon 
                  fontSize="small" 
                  sx={{ color: 'success.main' }}
                />
              )}
            </Box>
          </MenuItem>
        ))}
        
        {availableTenants.length === 0 && (
          <MenuItem disabled>
            <Typography variant="body2" color="text.secondary">
              Žádné další tenanty nejsou k dispozici
            </Typography>
          </MenuItem>
        )}
      </Menu>

      {/* User account menu */}
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
            minWidth: 280,
            borderRadius: tokens.radius.lg,
            background: theme => theme.palette.mode === 'dark'
              ? 'linear-gradient(135deg, rgba(30, 30, 30, 0.95) 0%, rgba(50, 50, 50, 0.95) 100%)'
              : 'linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%)',
            backdropFilter: 'blur(20px)',
            border: '1px solid rgba(255,255,255,0.2)',
            boxShadow: tokens.shadows.xxl,
            '& .MuiMenuItem-root': {
              borderRadius: tokens.radius.md,
              mx: 1,
              my: 0.5,
              px: 2,
              py: 1.5,
              transition: `all ${tokens.components.animation.normal} ${tokens.components.animation.easing}`,
              '&:hover': {
                backgroundColor: theme => theme.palette.mode === 'dark'
                  ? 'rgba(25, 118, 210, 0.15)'
                  : 'rgba(25, 118, 210, 0.08)',
                transform: 'translateX(4px)',
              },
              '&:focus-visible': {
                outline: `${tokens.a11y.focusRing.width} ${tokens.a11y.focusRing.style} ${tokens.a11y.focusRing.color}`,
                outlineOffset: tokens.a11y.focusRing.offset,
              },
            }
          },
        }}
      >
        {/* User info header */}
        <Box sx={{ px: 2, py: 2, borderBottom: '1px solid rgba(0,0,0,0.1)' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Avatar
              sx={{
                width: 48,
                height: 48,
                fontWeight: tokens.typography.fontWeight.bold,
                bgcolor: 'primary.main'
              }}
            >
              {getUserInitials()}
            </Avatar>
            <Box>
              <Typography variant="subtitle1" sx={{ 
                fontWeight: tokens.typography.fontWeight.semibold 
              }}>
                {getUserDisplayName()}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {user?.email}
              </Typography>
              {user?.tenant && (
                <Chip
                  size="small"
                  icon={<DomainIcon sx={{ fontSize: '14px !important' }} />}
                  label={getTenantDisplayInfo().name}
                  sx={{
                    mt: 0.5,
                    height: 20,
                    fontSize: tokens.typography.fontSize.xs,
                    bgcolor: 'primary.light',
                    color: 'primary.dark'
                  }}
                />
              )}
            </Box>
          </Box>
        </Box>

        <MenuItem onClick={() => handleNavigation('/profile')}>
          <AccountCircleIcon fontSize="small" sx={{ mr: 2, color: 'primary.main' }} />
          <Box>
            <Typography variant="body2" sx={{ fontWeight: tokens.typography.fontWeight.semibold }}>
              Můj profil
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Osobní nastavení a informace
            </Typography>
          </Box>
        </MenuItem>

        {user?.roles?.includes('CORE_ROLE_ADMIN') && availableTenants.length > 1 && (
          <MenuItem onClick={handleTenantMenuOpen}>
            <SwitchTenantIcon fontSize="small" sx={{ mr: 2, color: 'info.main' }} />
            <Box>
              <Typography variant="body2" sx={{ fontWeight: tokens.typography.fontWeight.semibold }}>
                Přepnout tenant
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Dostupných: {availableTenants.length}
              </Typography>
            </Box>
          </MenuItem>
        )}
        
        <Divider sx={{ my: 1 }} />
        
        <MenuItem onClick={handleLogout} sx={{ color: 'error.main' }}>
          <LogoutIcon fontSize="small" sx={{ mr: 2 }} />
          <Box>
            <Typography variant="body2" sx={{ fontWeight: tokens.typography.fontWeight.semibold }}>
              Odhlásit se
            </Typography>
            <Typography variant="caption" color="text.secondary">
              Ukončit relaci
            </Typography>
          </Box>
        </MenuItem>
      </Menu>

      {/* Drawer navigation */}
      <Box
        component="nav"
        sx={{ 
          width: { md: drawerWidth }, 
          flexShrink: { md: 0 },
          transition: 'width 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        }}
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true,
          }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': {
              boxSizing: 'border-box',
              width: drawerWidthExpanded,
              border: 'none',
            },
          }}
        >
          {drawer}
        </Drawer>
        
        <Drawer
          variant="permanent"
          ref={drawerRef}
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': {
              boxSizing: 'border-box',
              width: drawerWidth,
              border: 'none',
              boxShadow: tokens.shadows.xl,
              transition: isResizing ? 'none' : 'width 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
              overflowX: 'hidden',
              position: 'relative',
            },
          }}
          open
        >
          {drawer}
          
          {/* Resize Handle */}
          <Box
            onMouseDown={handleMouseDown}
            sx={{
              position: 'absolute',
              right: '-4px',
              top: 0,
              bottom: 0,
              width: '8px',
              cursor: 'col-resize',
              backgroundColor: 'transparent',
              transition: 'background-color 0.2s ease-in-out',
              zIndex: 9999,
              
              '&::after': {
                content: '""',
                position: 'absolute',
                right: '3px',
                top: '50%',
                transform: 'translateY(-50%)',
                width: '2px',
                height: '40px',
                backgroundColor: 'divider',
                borderRadius: '2px',
                opacity: 0,
                transition: 'opacity 0.2s ease-in-out',
              },
              
              '&:hover': {
                backgroundColor: 'rgba(25, 118, 210, 0.08)',
                
                '&::after': {
                  opacity: 0.8,
                  backgroundColor: 'primary.main',
                },
              },
              
              '&:active': {
                backgroundColor: 'rgba(25, 118, 210, 0.12)',
                
                '&::after': {
                  opacity: 1,
                  backgroundColor: 'primary.dark',
                },
              },
            }}
            title="Přetažením změňte šířku sidebaru"
          />
        </Drawer>
      </Box>

      {/* Main content area */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          width: { md: `calc(100% - ${drawerWidth}px)` },
          minHeight: '100vh',
          background: `linear-gradient(135deg, ${tokens.colors.grey[50]} 0%, #f0f2f5 100%)`,
          transition: 'width 0.3s cubic-bezier(0.4, 0, 0.2, 1), margin 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
        }}
      >
        <Toolbar sx={{ minHeight: tokens.components.layout.headerHeight }} />
        {children}
      </Box>
    </Box>
  );
}

Layout.propTypes = {
  children: PropTypes.node.isRequired,
  user: UserPropType,
  onLogout: PropTypes.func,
};

export default Layout;