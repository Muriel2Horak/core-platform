import React from 'react';
import { useLocation } from 'react-router';
import { Box, Typography, List, ListItem, ListItemButton, ListItemText } from '@mui/material';
import { Link, NavLink } from 'react-router';
import Menuitems from './MenuItems';
import Logo from '../shared/logo/Logo';
import useUserRoles from '../../../hooks/useUserRoles';

const SidebarItems = () => {
  const location = useLocation();
  const pathDirect = location.pathname;
  const { hasAnyRole, loading: rolesLoading } = useUserRoles();

  const renderMenuItems = (menuItems) => {
    return menuItems
      .filter((item) => {
        // Pokud položka nemá definované role, zobrazit vždy
        if (!item.roles || item.roles.length === 0) {
          return true;
        }
        
        // Pokud se ještě načítají role, nezobrazovat role-restricted položky
        if (rolesLoading) {
          return false;
        }
        
        // Zobrazit pouze pokud má uživatel požadované role
        return hasAnyRole(item.roles);
      })
      .map((item) => {
        if (item.navlabel) {
          return React.createElement(Typography, {
            key: item.subheader,
            variant: "caption",
            sx: {
              mt: 2,
              mb: 1,
              ml: 2,
              fontSize: '11px',
              fontWeight: 700,
              color: '#A5B4CB',
              textTransform: 'uppercase',
              letterSpacing: '0.5px'
            }
          }, item.subheader);
        }

        const isSelected = pathDirect === item.href;
        
        return React.createElement(ListItem, {
          key: item.id,
          disablePadding: true
        },
          React.createElement(ListItemButton, {
            component: Link,
            to: item.href,
            target: item.href.startsWith("https") ? "_blank" : "_self",
            rel: "noopener noreferrer",
            selected: isSelected,
            sx: {
              borderRadius: '8px',
              mx: 1,
              my: 0.5,
              backgroundColor: isSelected ? '#5D87FF' : 'transparent',
              color: isSelected ? '#fff' : 'text.primary',
              '&:hover': {
                backgroundColor: isSelected ? '#4570EA' : 'rgba(93, 135, 255, 0.1)',
                color: isSelected ? '#fff' : 'primary.main'
              },
              '&.Mui-selected': {
                backgroundColor: '#5D87FF',
                color: '#fff',
                '&:hover': {
                  backgroundColor: '#4570EA'
                }
              }
            }
          },
            React.createElement(ListItemText, {
              primary: item.title,
              primaryTypographyProps: {
                color: isSelected ? '#fff' : 'inherit',
                fontWeight: isSelected ? 600 : 400
              }
            })
          )
        );
      });
  };

  return React.createElement(Box, {
    sx: { 
      px: "24px", 
      overflowX: 'hidden',
      height: '100%',
      display: 'flex',
      flexDirection: 'column'
    }
  },
    React.createElement(Box, {
      sx: { margin: "0 -24px", mb: 2 }
    },
      React.createElement(Logo, {
        component: NavLink,
        to: "/"
      }, "Core Platform")
    ),
    React.createElement(List, {
      sx: { flex: 1, pt: 0 }
    },
      ...renderMenuItems(Menuitems)
    )
  );
};

export default SidebarItems;

