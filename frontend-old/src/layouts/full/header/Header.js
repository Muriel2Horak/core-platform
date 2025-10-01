import React, { useState } from 'react'
import {
  Box,
  AppBar,
  Toolbar,
  styled,
  Stack,
  IconButton,
  Badge,
  Menu,
  MenuItem,
  Typography,
} from '@mui/material'
import PropTypes from 'prop-types'
import { useTenant } from '../../../context/TenantContext'
import { assertIsComponent } from '../../../utils/ensureComponent'

// components
import Profile from './Profile'
import { IconBellRinging, IconMenu } from '@tabler/icons-react'

const tenants = {
  'core-platform': {
    name: 'CORE Platform',
    logo: '/assets/logos/logo-core.svg',
    theme: 'dark',
  },
  'acme': {
    name: 'ACME Corporation',
    logo: '/assets/logos/logo-acme.svg',
    theme: 'light',
  },
  // Přidej další tenanty podle potřeby
}

const Header = (props) => {
  const { tenant } = useTenant()

  // notification dd - hooks musí být na začátku komponenty
  const [anchorEl, setAnchorEl] = useState(null)
  const [menuPosition, setMenuPosition] = useState(null)

  const AppBarStyled = styled(AppBar)(({ theme }) => ({
    boxShadow: 'none',
    background: theme.palette.background.paper,
    justifyContent: 'center',
    backdropFilter: 'blur(4px)',
    [theme.breakpoints.up('lg')]: {
      minHeight: '70px',
    },
  }))
  const ToolbarStyled = styled(Toolbar)(({ theme }) => ({
    width: '100%',
    color: theme.palette.text.secondary,
  }))

  // Guard pro případ, že tenant není (ještě) definován nebo neexistuje v mapě
  if (!tenant || !tenants[tenant]) {
    console.error(
      `❌ Header: Neznámý nebo nedefinovaný tenant: '${tenant}'. Používám fallback.`
    )
    const fallbackTenant = tenants['core-platform']
    // Ověř, že fallback tenant existuje
    assertIsComponent(fallbackTenant, `tenants['core-platform'] fallback`)
    
    return (
      <Box sx={{ display: 'flex', alignItems: 'center' }}>
        <img
          src={fallbackTenant.logo}
          alt={`${fallbackTenant.name} Logo`}
          style={{ height: '30px', marginRight: '10px' }}
        />
        <Typography variant='h6' color='error'>
          {fallbackTenant.name} (Unknown Tenant)
        </Typography>
      </Box>
    )
  }

  const currentTenant = tenants[tenant]
  // Ověř, že currentTenant existuje
  assertIsComponent(currentTenant, `tenants[${tenant}]`)

  const handleClick = (event) => {
    const rect = event.currentTarget.getBoundingClientRect() // Get exact position
    setMenuPosition({
      top: rect.bottom + window.scrollY, // Position menu below the icon
      left: rect.left + window.scrollX, // Align with icon
    })
    setAnchorEl(event.currentTarget)
  }

  const handleClose = () => {
    setAnchorEl(null)
  }

  return (
    <AppBarStyled position='sticky' color='default'>
      <ToolbarStyled>
        <IconButton
          color='inherit'
          aria-label='menu'
          onClick={props.toggleMobileSidebar}
          sx={{
            display: {
              lg: 'none',
              xs: 'inline',
            },
          }}>
          <IconMenu width='20' height='20' />
        </IconButton>

        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <img
            src={currentTenant.logo}
            alt={`${currentTenant.name} Logo`}
            style={{ height: '30px', marginRight: '10px' }}
          />
          <Typography variant='h6' noWrap>
            {currentTenant.name}
          </Typography>
        </Box>

        <Box>
          <IconButton
            aria-label='show 4 new mails'
            color='inherit'
            aria-controls='notification-menu'
            aria-haspopup='true'
            onClick={handleClick}>
            <Badge variant='dot' color='primary'>
              <IconBellRinging size='21' stroke='1.5' />
            </Badge>
          </IconButton>

          <Menu
            id='notification-menu'
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleClose}
            anchorReference='anchorPosition' // Use custom positioning
            anchorPosition={
              menuPosition
                ? { top: menuPosition.top, left: menuPosition.left }
                : undefined
            }
            slotProps={{
              paper: {
                sx: {
                  mt: 1, // Ensures the menu appears slightly below the bell icon
                  boxShadow: 9, // Optional: Improves visibility with a shadow
                  minWidth: '200px', // Adjust width to ensure proper alignment
                },
              },
            }}>
            <MenuItem onClick={handleClose}>
              <Typography variant='body1'>Item 1</Typography>
            </MenuItem>
            <MenuItem onClick={handleClose}>
              <Typography variant='body1'>Item 2</Typography>
            </MenuItem>
          </Menu>
        </Box>
        <Box flexGrow={1} />
        <Stack spacing={1} direction='row' alignItems='center'>
          <Profile />
        </Stack>
      </ToolbarStyled>
    </AppBarStyled>
  )
}

Header.propTypes = {
  sx: PropTypes.object,
}

export default Header
