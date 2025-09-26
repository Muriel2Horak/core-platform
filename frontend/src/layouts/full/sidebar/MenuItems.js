import { uniqueId } from 'lodash';

import {
  IconUserCircle, IconUsers
} from '@tabler/icons-react';

const Menuitems = [
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
    subheader: 'Administrace',
  },
  {
    id: uniqueId(),
    title: 'Správa uživatelů',
    icon: IconUsers,
    href: '/admin/users',
    roles: ['CORE_ROLE_ADMIN', 'CORE_ROLE_USER_MANAGER'], // Role-based visibility
  },
];

export default Menuitems;
