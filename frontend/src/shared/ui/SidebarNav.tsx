/**
 * 🗂️ SidebarNav - Core Platform Sidebar Navigation Component
 * 
 * Refaktorovaný sidebar s WCAG AA kontrasty, collapsed módem a čistými stavy.
 * Podporuje desktop expanded/collapsed režimy s tooltips.
 */

import React from 'react';
import {
  Box,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
  Chip,
  Tooltip,
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  People as PeopleIcon,
  Business as BusinessIcon,
  Person as PersonIcon,
  Security as SecurityIcon,
  TableChart as TableChartIcon,
  ViewKanban as ViewKanbanIcon,
  Settings as SettingsIcon,
} from '@mui/icons-material';
import { tokens } from '../theme/tokens';

export interface SidebarNavItem {
  /** Unikátní ID položky */
  id: string;
  /** Zobrazovaný text */
  label: string;
  /** Ikona položky */
  icon: React.ReactNode;
  /** URL path */
  href: string;
  /** Je položka aktivní */
  active?: boolean;
  /** Popis položky pro tooltip */
  description?: string;
  /** Badge text */
  badge?: string;
  /** Badge barva */
  badgeColor?: 'primary' | 'secondary' | 'success' | 'warning' | 'error' | 'info';
  /** Vyžadované role pro zobrazení */
  requiredRoles?: string[];
}

export interface SidebarNavProps {
  /** Seznam navigačních položek */
  items?: SidebarNavItem[];
  /** Callback pro kliknutí na položku */
  onItemClick?: (item: SidebarNavItem) => void;
  /** Aktuální path pro určení aktivní položky */
  currentPath?: string;
  /** Uživatelské role pro filtrování položek */
  userRoles?: string[];
  /** Collapsed režim (jen ikony) */
  collapsed?: boolean;
}

// 🔧 Default menu items s novými Examples
export const defaultMenuItems: SidebarNavItem[] = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    icon: <DashboardIcon />,
    href: '/',
    description: 'Přehled systému',
  },
  {
    id: 'users',
    label: 'Uživatelé',
    icon: <PeopleIcon />,
    href: '/users',
    description: 'Správa uživatelů',
    requiredRoles: ['CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN'],
  },
  {
    id: 'user-directory',
    label: 'Adresář',
    icon: <PersonIcon />,
    href: '/user-directory',
    description: 'Vyhledávání uživatelů',
  },
  {
    id: 'roles',
    label: 'Role',
    icon: <SecurityIcon />,
    href: '/roles',
    description: 'Správa rolí',
    requiredRoles: ['CORE_ROLE_ADMIN'],
  },
  {
    id: 'tenants',
    label: 'Tenanti',
    icon: <BusinessIcon />,
    href: '/tenant-management',
    description: 'Multi-tenant správa',
    requiredRoles: ['CORE_ROLE_ADMIN'],
  },
  // 🆕 Examples sekce
  {
    id: 'examples-table',
    label: 'DataTable',
    icon: <TableChartIcon />,
    href: '/examples/table',
    description: 'Ukázka TanStack Table',
    badge: 'DEMO',
    badgeColor: 'info',
  },
  {
    id: 'examples-kanban',
    label: 'Kanban',
    icon: <ViewKanbanIcon />,
    href: '/examples/kanban',
    description: 'Ukázka Kanban board',
    badge: 'DEMO',
    badgeColor: 'info',
  },
  {
    id: 'admin',
    label: 'Správa',
    icon: <SettingsIcon />,
    href: '/admin',
    description: 'Systémová správa',
    requiredRoles: ['CORE_ROLE_ADMIN'],
  },
];

/**
 * SidebarNav komponent s refaktorovaným designem
 */
export const SidebarNav: React.FC<SidebarNavProps> = ({
  items = defaultMenuItems,
  onItemClick,
  currentPath = '',
  userRoles = [],
  collapsed = false,
}) => {
  // 🔍 Filter items based on user roles
  const filteredItems = items.filter(menuItem => {
    if (!menuItem.requiredRoles || menuItem.requiredRoles.length === 0) {
      return true;
    }
    return menuItem.requiredRoles.some(role => userRoles.includes(role));
  });

  // 🎯 Check if item is active
  const isItemActive = (item: SidebarNavItem) => {
    if (item.active !== undefined) {
      return item.active;
    }
    
    // Exact match for home page
    if (item.href === '/' && currentPath === '/') {
      return true;
    }
    
    // Path starts with item href (but not for home page)
    if (item.href !== '/' && currentPath.startsWith(item.href)) {
      return true;
    }
    
    return false;
  };

  // 🎨 Render navigation item
  const renderNavItem = (item: SidebarNavItem) => {
    const active = isItemActive(item);
    
    const buttonContent = (
      <ListItemButton
        onClick={() => onItemClick?.(item)}
        selected={active} // MUI prop instead of custom 'active'
        aria-current={active ? 'page' : undefined}
        aria-label={collapsed ? item.label : undefined}
        sx={{
          // Active state styling
          ...(active && {
            backgroundColor: tokens.colors.primary[50],
            borderRight: `3px solid ${tokens.colors.primary[500]}`,
            '&:hover': {
              backgroundColor: tokens.colors.primary[100],
            },
          }),
          // Collapsed mode styling
          ...(collapsed && {
            justifyContent: 'center',
            minHeight: 48,
            px: 2,
          }),
          // Default styling
          borderRadius: 1,
          mx: 1,
          mb: 0.5,
          transition: 'all 0.2s ease-in-out',
        }}
      >
        {/* Icon */}
        <ListItemIcon
          sx={{
            minWidth: collapsed ? 'auto' : 40,
            color: active ? tokens.colors.white : tokens.colors.sidebar.textMuted, // ✅ Bílé ikony na tmavém pozadí
            transition: 'color 0.2s ease-in-out',
          }}
        >
          {item.icon}
        </ListItemIcon>

        {/* Text Content - hidden in collapsed mode */}
        {!collapsed && (
          <ListItemText
            primary={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Typography
                  component="span"
                  sx={{
                    fontSize: tokens.components.sidebar.fontSize,
                    fontWeight: active 
                      ? tokens.typography.fontWeight.semibold 
                      : tokens.components.sidebar.fontWeight,
                    color: active 
                      ? tokens.colors.white // ✅ Aktivní text bílý
                      : tokens.colors.sidebar.text, // ✅ Neaktivní text také bílý
                    transition: 'all 0.2s ease-in-out',
                  }}
                >
                  {item.label}
                </Typography>
                
                {/* Badge */}
                {item.badge && (
                  <Chip
                    label={item.badge}
                    size="small"
                    color={item.badgeColor || 'primary'}
                    sx={{
                      height: 18,
                      fontSize: '0.65rem',
                      fontWeight: tokens.typography.fontWeight.bold,
                      '& .MuiChip-label': {
                        px: 0.8,
                      },
                    }}
                  />
                )}
              </Box>
            }
          />
        )}
      </ListItemButton>
    );

    // Wrap in tooltip for collapsed mode
    if (collapsed) {
      return (
        <Tooltip
          key={item.id}
          title={
            <Box>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                {item.label}
              </Typography>
              {item.description && (
                <Typography variant="caption" sx={{ opacity: 0.8 }}>
                  {item.description}
                </Typography>
              )}
            </Box>
          }
          placement="right"
          arrow
        >
          <ListItem disablePadding>
            {buttonContent}
          </ListItem>
        </Tooltip>
      );
    }

    return (
      <ListItem key={item.id} disablePadding>
        {buttonContent}
      </ListItem>
    );
  };

  return (
    <Box
      component="nav"
      role="navigation"
      aria-label="Hlavní navigace"
    >
      <List>
        {filteredItems.map(renderNavItem)}
      </List>
    </Box>
  );
};

export default SidebarNav;