import React, { useState, useEffect } from 'react';
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

import { IconLogout, IconUser, IconUserCircle } from '@tabler/icons-react';
import authService from '../../../services/auth';

const Profile = () => {
  const [anchorEl2, setAnchorEl2] = useState(null);
  const [userInfo, setUserInfo] = useState(null);

  useEffect(() => {
    // Načtení informací o uživateli z backend API
    const loadUserInfo = async () => {
      try {
        const user = await authService.getUserInfo();
        setUserInfo(user);
      } catch (error) {
        // Fallback na synchronní verzi pro rychlé zobrazení
        const fallbackUser = authService.getUserInfoSync();
        setUserInfo(fallbackUser);
      }
    };

    loadUserInfo();
  }, []);

  const handleClick2 = (event) => {
    setAnchorEl2(event.currentTarget);
  };

  const handleClose2 = () => {
    setAnchorEl2(null);
  };

  const handleLogout = async () => {
    handleClose2();
    await authService.logout();
  };

  // Zobrazení prvního písmena jména jako avatar
  const getAvatarLetter = () => {
    if (userInfo?.name) {
      return userInfo.name.charAt(0).toUpperCase();
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
            bgcolor: 'primary.main',
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
            {userInfo?.name || userInfo?.username || 'Uživatel'}
          </Typography>
          {userInfo?.email && (
            <Typography variant="body2" color="textSecondary">
              {userInfo.email}
            </Typography>
          )}
          {userInfo?.roles && userInfo.roles.length > 0 && (
            <Typography variant="caption" color="primary">
              Role: {userInfo.roles.join(', ')}
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

        <MenuItem component={Link} to="/profile" onClick={handleClose2}>
          <ListItemIcon>
            <IconUser width={20} />
          </ListItemIcon>
          <ListItemText>
            <Typography variant="subtitle2">Můj profil</Typography>
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
