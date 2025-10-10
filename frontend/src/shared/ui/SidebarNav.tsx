/**
 * 🗂️ SidebarNav - Core Platform Sidebar Navigation Component
 * 
 * Refaktorovaný sidebar s WCAG AA kontrasty, collapsed módem a       // Monitoring - druhá úroveň
      {
        id: 'monitoring-section',
        label: 'Monitoring',
        icon: <AssessmentIcon />,
        href: '/core-admin/monitoring',
        description: 'Sledování výkonu',
        requiredRoles: ['CORE_ROLE_ADMIN'],
      },
      
      // Bezpečnost - druhá úroveň
      {
        id: 'security-section',
        label: 'Bezpečnost',
        icon: <ShieldIcon />,
        href: '#',  // Jen expandable kontejner
        description: 'Zabezpečení systému',
        requiredRoles: ['CORE_ROLE_ADMIN'],
        children: [odporuje desktop expanded/collapsed režimy s tooltips.
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
  Collapse,
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
  Assessment as AssessmentIcon,
  Shield as ShieldIcon,
  BugReport as BugReportIcon,
  Sync as SyncIcon,
  History as HistoryIcon,
  ExpandLess,
  ExpandMore,
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
  /** Vnořené položky (submenu) */
  children?: SidebarNavItem[];
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

// 🔧 Default menu items - hierarchická struktura
export const defaultMenuItems: SidebarNavItem[] = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    icon: <DashboardIcon />,
    href: '/dashboard',
    description: 'Přehled systému',
  },
  {
    id: 'user-directory',
    label: 'Adresář',
    icon: <PersonIcon />,
    href: '/user-directory',
    description: 'Vyhledávání uživatelů',
  },
  {
    id: 'reports',
    label: 'Reporting',
    icon: <AssessmentIcon />,
    href: '/reports',
    description: 'Analytické reporty a metriky',
  },
  
  // 👥 Administrace - parent položka s vnořeným menu
  {
    id: 'administration',
    label: 'Administrace',
    icon: <SettingsIcon />,
    href: '#',  // Jen expandable kontejner, ne odkaz
    description: 'Správa systému',
    requiredRoles: ['CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN'],
    children: [
      // Správa Keycloak - druhá úroveň
      {
        id: 'keycloak-admin',
        label: 'Správa Keycloak',
        icon: <SecurityIcon />,
        href: '#',  // Jen expandable kontejner
        description: 'Správa identit a přístupů',
        requiredRoles: ['CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN'],
        children: [
          {
            id: 'core-admin-users',
            label: 'Uživatelé',
            icon: <PeopleIcon />,
            href: '/core-admin/users',
            description: 'Správa uživatelů',
            requiredRoles: ['CORE_ROLE_USER_MANAGER', 'CORE_ROLE_ADMIN'],
          },
          {
            id: 'core-admin-roles',
            label: 'Role',
            icon: <SecurityIcon />,
            href: '/core-admin/roles',
            description: 'Správa rolí',
            requiredRoles: ['CORE_ROLE_ADMIN'],
          },
          {
            id: 'core-admin-groups',
            label: 'Skupiny',
            icon: <PeopleIcon />,
            href: '/core-admin/groups',
            description: 'Správa skupin',
            requiredRoles: ['CORE_ROLE_ADMIN'],
          },
          {
            id: 'core-admin-tenants',
            label: 'Tenanti',
            icon: <BusinessIcon />,
            href: '/core-admin/tenants',
            description: 'Multi-tenant správa',
            requiredRoles: ['CORE_ROLE_ADMIN'],
          },
          {
            id: 'core-admin-keycloak-sync',
            label: 'Synchronizace',
            icon: <SyncIcon />,
            href: '/core-admin/keycloak-sync',
            description: 'Synchronizace z Keycloak',
            requiredRoles: ['CORE_ROLE_ADMIN'],
          },
          {
            id: 'core-admin-sync-history',
            label: 'Historie Sync',
            icon: <HistoryIcon />,
            href: '/core-admin/sync-history',
            description: 'Historie synchronizací',
            requiredRoles: ['CORE_ROLE_ADMIN'],
          },
        ],
      },
      
      // Monitoring - druhá úroveň
      {
        id: 'monitoring-section',
        label: 'Monitoring',
        icon: <AssessmentIcon />,
        href: '/core-admin/monitoring',
        description: 'Sledování výkonu',
        requiredRoles: ['CORE_ROLE_ADMIN'],
      },
      
      // Bezpečnost - druhá úroveň
      {
        id: 'security-section',
        label: 'Bezpečnost',
        icon: <ShieldIcon />,
        href: '/core-admin/security',
        description: 'Zabezpečení systému',
        requiredRoles: ['CORE_ROLE_ADMIN'],
        children: [
          {
            id: 'core-admin-audit',
            label: 'Audit',
            icon: <BugReportIcon />,
            href: '/core-admin/audit',
            description: 'Auditní logy',
            requiredRoles: ['CORE_ROLE_ADMIN'],
          },
        ],
      },
    ],
  },
  
  // 👤 Tenant Administration - sekce pro tenant adminy
  {
    id: 'tenant-administration',
    label: 'Tenant Administrace',
    icon: <BusinessIcon />,
    href: '#',  // Jen expandable kontejner
    description: 'Správa tenantu',
    requiredRoles: ['CORE_ROLE_TENANT_ADMIN'],
    children: [
      {
        id: 'tenant-admin-dashboard',
        label: 'Dashboard',
        icon: <DashboardIcon />,
        href: '/tenant-admin',
        description: 'Přehled tenantu',
        requiredRoles: ['CORE_ROLE_TENANT_ADMIN'],
      },
      {
        id: 'tenant-admin-users',
        label: 'Uživatelé',
        icon: <PeopleIcon />,
        href: '/tenant-admin/users',
        description: 'Správa uživatelů tenantu',
        requiredRoles: ['CORE_ROLE_TENANT_ADMIN'],
      },
      {
        id: 'tenant-admin-roles',
        label: 'Role',
        icon: <SecurityIcon />,
        href: '/tenant-admin/roles',
        description: 'Správa rolí tenantu',
        requiredRoles: ['CORE_ROLE_TENANT_ADMIN'],
      },
      {
        id: 'tenant-admin-groups',
        label: 'Skupiny',
        icon: <PeopleIcon />,
        href: '/tenant-admin/groups',
        description: 'Správa skupin tenantu',
        requiredRoles: ['CORE_ROLE_TENANT_ADMIN'],
      },
      {
        id: 'tenant-admin-keycloak-sync',
        label: 'Synchronizace',
        icon: <SyncIcon />,
        href: '/tenant-admin/keycloak-sync',
        description: 'Synchronizace tenantu',
        requiredRoles: ['CORE_ROLE_TENANT_ADMIN'],
      },
    ],
  },
  
  // 🆕 DEMO položky na root úrovni
  {
    id: 'examples-table',
    label: 'DataTable',
    icon: <TableChartIcon />,
    href: '/examples/data-table',
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
];

/**
 * SidebarNav komponent s hierarchickou navigací
 */
export const SidebarNav: React.FC<SidebarNavProps> = ({
  items = defaultMenuItems,
  onItemClick,
  currentPath = '',
  userRoles = [],
  collapsed = false,
}) => {
  const [expandedItems, setExpandedItems] = React.useState<Set<string>>(new Set(['administration', 'keycloak-admin']));

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
  const renderNavItem = (item: SidebarNavItem, level: number = 0) => {
    const active = isItemActive(item);
    const isExpanded = expandedItems.has(item.id);
    const hasChildren = item.children && item.children.length > 0;

    // Filter children based on roles
    const filteredChildren = hasChildren 
      ? item.children!.filter(child => {
          if (!child.requiredRoles || child.requiredRoles.length === 0) {
            return true;
          }
          return child.requiredRoles.some(role => userRoles.includes(role));
        })
      : [];

    const buttonContent = (
      <ListItemButton
        onClick={() => {
          if (hasChildren) {
            setExpandedItems(prev => {
              const newSet = new Set(prev);
              if (newSet.has(item.id)) {
                newSet.delete(item.id);
              } else {
                newSet.add(item.id);
              }
              return newSet;
            });
          } else {
            onItemClick?.(item);
          }
        }}
        selected={active}
        aria-current={active ? 'page' : undefined}
        aria-label={collapsed ? item.label : undefined}
        disableGutters={false}
        sx={{
          // Indentation ONLY for nested items (level > 0)
          // Root items (level 0) have no left padding, nested items are indented
          pl: collapsed ? 2 : (level === 0 ? 0 : 2 + (level * 2)),
          pr: 2, // Ensure consistent right padding
          
          // Active state styling - glassmorphic effect
          ...(active && !hasChildren && {
            background: theme => theme.palette.mode === 'dark' 
              ? 'rgba(25, 118, 210, 0.08)' 
              : 'rgba(25, 118, 210, 0.04)',
            borderLeft: '3px solid',
            borderLeftColor: 'primary.main',
            position: 'relative',
            '&::before': {
              content: '""',
              position: 'absolute',
              top: 0,
              left: 0,
              right: 0,
              bottom: 0,
              background: theme => theme.palette.mode === 'dark'
                ? 'linear-gradient(90deg, rgba(25, 118, 210, 0.1) 0%, transparent 100%)'
                : 'linear-gradient(90deg, rgba(25, 118, 210, 0.06) 0%, transparent 100%)',
              pointerEvents: 'none',
            },
            '&:hover': {
              background: theme => theme.palette.mode === 'dark'
                ? 'rgba(25, 118, 210, 0.12)'
                : 'rgba(25, 118, 210, 0.06)',
            },
          }),
          
          // Parent item styling
          ...(hasChildren && {
            '&:hover': {
              backgroundColor: 'action.hover',
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
          ml: level === 0 ? 0 : 1, // Margin left only for nested items
          mr: 1, // Keep right margin for visual spacing
          mb: 0.5,
          transition: 'all 0.2s ease-in-out',
        }}
      >
        {/* Icon */}
        <ListItemIcon
          sx={{
            minWidth: collapsed ? 'auto' : 40,
            color: active 
              ? 'primary.main' 
              : 'text.secondary',
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
                    fontSize: level > 0 ? tokens.typography.fontSize.sm : tokens.components.sidebar.fontSize,
                    fontWeight: active 
                      ? tokens.typography.fontWeight.semibold 
                      : tokens.components.sidebar.fontWeight,
                    color: active 
                      ? 'primary.main'
                      : 'text.primary',
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
                
                {/* Subtle indicator for root expandable items */}
                {hasChildren && level === 0 && filteredChildren.length > 0 && (
                  <Typography
                    component="span"
                    sx={{
                      fontSize: '0.7rem',
                      color: 'text.disabled',
                      ml: 0.5,
                    }}
                  >
                    ({filteredChildren.length})
                  </Typography>
                )}
              </Box>
            }
          />
        )}

        {/* Expand/Collapse Icon - ONLY for nested items (level > 0) */}
        {hasChildren && !collapsed && level > 0 && (
          <Box sx={{ color: 'text.secondary' }}>
            {isExpanded ? <ExpandLess /> : <ExpandMore />}
          </Box>
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
      <React.Fragment key={item.id}>
        <ListItem disablePadding>
          {buttonContent}
        </ListItem>
        {hasChildren && filteredChildren.length > 0 && (
          <Collapse in={isExpanded} timeout="auto" unmountOnExit>
            <List component="div" disablePadding>
              {filteredChildren.map(child => renderNavItem(child, level + 1))}
            </List>
          </Collapse>
        )}
      </React.Fragment>
    );
  };

  return (
    <Box
      component="nav"
      role="navigation"
      aria-label="Hlavní navigace"
      sx={{
        p: 0, // No padding
        m: 0, // No margin
      }}
    >
      <List disablePadding sx={{ p: 0, m: 0 }}>
        {filteredItems.map(item => renderNavItem(item, 0))}
      </List>
    </Box>
  );
};

export default SidebarNav;