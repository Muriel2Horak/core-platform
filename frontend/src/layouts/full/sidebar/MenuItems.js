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
    roles: ['admin', 'user-manager'], // Role-based visibility
  },
];

export default Menuitems;
