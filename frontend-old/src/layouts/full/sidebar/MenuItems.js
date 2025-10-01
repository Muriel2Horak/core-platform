import {
  IconAperture, IconCopy, IconLayoutDashboard, IconLogin, IconMoodHappy, IconTypography, IconUser, IconUsers, IconBuildingStore, IconUserCircle, IconUsersGroup
} from '@tabler/icons-react';

import { uniqueId } from 'lodash';

const Menuitems = [
  {
    navlabel: true,
    subheader: 'Home',
  },

  {
    id: uniqueId(),
    title: 'Dashboard',
    icon: IconLayoutDashboard,
    href: '/dashboard',
  },

  {
    navlabel: true,
    subheader: 'Utilities',
  },
  {
    id: uniqueId(),
    title: 'Typography',
    icon: IconTypography,
    href: '/utilities/typography',
  },
  {
    id: uniqueId(),
    title: 'Shadow',
    icon: IconCopy,
    href: '/utilities/shadow',
  },
  {
    navlabel: true,
    subheader: 'Auth',
  },
  {
    id: uniqueId(),
    title: 'Login',
    icon: IconLogin,
    href: '/auth/login',
  },
  {
    id: uniqueId(),
    title: 'Register',
    icon: IconUser,
    href: '/auth/register',
  },
  {
    navlabel: true,
    subheader: 'Extra',
  },
  {
    id: uniqueId(),
    title: 'Icons',
    icon: IconMoodHappy,
    href: '/icons/tabler',
  },
  {
    id: uniqueId(),
    title: 'Sample Page',
    icon: IconAperture,
    href: '/sample-page',
  },

  {
    navlabel: true,
    subheader: 'Profil',
  },
  {
    id: uniqueId(),
    title: 'Můj profil',
    icon: IconUserCircle,
    href: '/profile',
  },
  {
    navlabel: true,
    subheader: 'Directory',
  },
  {
    id: uniqueId(),
    title: 'Directory',
    icon: IconUsersGroup,
    href: '/directory',
    // Žádné role = viditelné pro všechny přihlášené uživatele
  },
  {
    navlabel: true,
    subheader: 'Administrace',
  },
  {
    id: uniqueId(),
    title: 'Správa uživatelů',
    icon: IconUsers,
    href: '/admin-core/users',
    roles: ['CORE_ROLE_ADMIN', 'CORE_ROLE_USER_MANAGER'], // Role-based visibility
  },

  // Role-based Management Section
  {
    navlabel: true,
    subheader: 'Management',
    roles: ['CORE_ROLE_USER_MANAGER', 'CORE_ROLE_TENANT_ADMIN', 'CORE_ROLE_TENANT_MANAGER', 'CORE_ROLE_SYSTEM_ADMIN', 'CORE_ROLE_ADMIN'], // Visible to managers and above
  },
  {
    id: uniqueId(),
    title: 'User Management',
    icon: IconUsers,
    href: '/admin-core/users',
    roles: ['CORE_ROLE_USER_MANAGER', 'CORE_ROLE_TENANT_ADMIN', 'CORE_ROLE_TENANT_MANAGER', 'CORE_ROLE_SYSTEM_ADMIN', 'CORE_ROLE_ADMIN'], // User managers and above
  },

  // Tenant Management Section
  {
    navlabel: true,
    subheader: 'System Administration',
    roles: ['CORE_ROLE_TENANT_MANAGER', 'CORE_ROLE_SYSTEM_ADMIN', 'CORE_ROLE_ADMIN'], // Only for tenant managers and above
  },
  {
    id: uniqueId(),
    title: 'Tenant Management',
    icon: IconBuildingStore,
    href: '/admin-core/tenants',
    roles: ['CORE_ROLE_TENANT_MANAGER', 'CORE_ROLE_SYSTEM_ADMIN', 'CORE_ROLE_ADMIN'], // Only for tenant managers and above
  },
];

export default Menuitems;
