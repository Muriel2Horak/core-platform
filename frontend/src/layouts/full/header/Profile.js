import React from 'react';
import { Link } from 'react-router';
import {
  Avatar,
  Box,
  Menu,
  Button,
  IconButton,
  MenuItem,
  ListItemIcon,
  ListItemText, 
  Typography,
  Divider
} from '@mui/material';

import { IconLogout, IconUser, IconUserCircle, IconKey, IconSettings } from '@tabler/icons-react';
import keycloakService from '../../../services/keycloakService';
import { useUserInfo } from '../../../hooks/useUserInfo';

const Profile = () => {
  const [anchorEl2, setAnchorEl2] = React.useState(null);
  
  // Použij useUserInfo hook pro aktuální data
  const { userInfo, getDisplayName, isAdmin } = useUserInfo({
    refreshInterval: 5 * 60 * 1000, // Refresh každých 5 minut
    autoRefresh: true
  });

  const handleClick2 = (event) => {
    setAnchorEl2(event.currentTarget);
  };

  const handleClose2 = () => {
    setAnchorEl2(null);
  };

  const handleLogout = async () => {
    handleClose2();
    await keycloakService.logout();
  };

  // Zobrazení prvního písmena jména jako avatar
  const getAvatarLetter = () => {
    if (userInfo?.firstName) {
      return userInfo.firstName.charAt(0).toUpperCase();
    }
    if (userInfo?.username) {
      return userInfo.username.charAt(0).toUpperCase();
    }
    return 'U';
  };

  return (
    <Box>
      <IconButton
        size="large"
        aria-label="user profile menu"
        color="inherit"
        aria-controls="profile-menu"
        aria-haspopup="true"
        sx={{
          ...(typeof anchorEl2 === 'object' && {
            color: 'primary.main',
          }),
        }}
        onClick={handleClick2}
      >
        <Avatar
          sx={{
            width: 35,
            height: 35,
            bgcolor: isAdmin() ? 'error.main' : 'primary.main',
            color: 'white'
          }}
        >
          {getAvatarLetter()}
        </Avatar>
      </IconButton>

      <Menu
        id="profile-menu"
        anchorEl={anchorEl2}
        keepMounted
        open={Boolean(anchorEl2)}
        onClose={handleClose2}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        sx={{
          '& .MuiMenu-paper': {
            width: '280px',
          },
        }}
      >
        {/* Informace o uživateli */}
        <Box px={2} py={1}>
          <Typography variant="subtitle1" fontWeight="bold">
            {getDisplayName()}
          </Typography>
          {userInfo?.email && (
            <Typography variant="body2" color="textSecondary">
              {userInfo.email}
            </Typography>
          )}
          {userInfo?.department && (
            <Typography variant="caption" color="primary">
              {userInfo.department}
            </Typography>
          )}
          {userInfo?.roles && userInfo.roles.length > 0 && (
            <Typography variant="caption" color="primary" display="block">
              Role: {userInfo.roles.slice(0, 2).join(', ')}{userInfo.roles.length > 2 ? '...' : ''}
            </Typography>
          )}
        </Box>

        <Divider />

        {/* Menu položky */}
        <MenuItem component={Link} to="/dashboard" onClick={handleClose2}>
          <ListItemIcon>
            <IconUserCircle width={20} />
          </ListItemIcon>
          <ListItemText>
            <Typography variant="subtitle2">Dashboard</Typography>
          </ListItemText>
        </MenuItem>

        <MenuItem component={Link} to="/me" onClick={handleClose2}>
          <ListItemIcon>
            <IconUser width={20} />
          </ListItemIcon>
          <ListItemText>
            <Typography variant="subtitle2">Můj účet</Typography>
          </ListItemText>
        </MenuItem>

        <MenuItem onClick={() => { keycloakService.openPasswordChange(); handleClose2(); }}>
          <ListItemIcon>
            <IconKey width={20} />
          </ListItemIcon>
          <ListItemText>
            <Typography variant="subtitle2">Změnit heslo</Typography>
          </ListItemText>
        </MenuItem>

        <MenuItem onClick={() => { keycloakService.openAccountConsole(); handleClose2(); }}>
          <ListItemIcon>
            <IconSettings width={20} />
          </ListItemIcon>
          <ListItemText>
            <Typography variant="subtitle2">Správa účtu</Typography>
          </ListItemText>
        </MenuItem>

        <Divider />

        {/* Odhlášení */}
        <Box px={2} py={1}>
          <Button 
            variant="outlined" 
            color="error" 
            fullWidth
            startIcon={<IconLogout />}
            onClick={handleLogout}
          >
            Odhlásit se
          </Button>
        </Box>
      </Menu>
    </Box>
  );
};

export default Profile;
