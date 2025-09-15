import { uniqueId } from 'lodash';

import {
  IconCopy, IconLayoutDashboard, IconTypography,
  IconAperture, IconAlignBoxLeftBottom, IconCheckbox, IconRadar, 
  IconSlideshow, IconCaretUpDown, IconTable, IconForms, IconUser
} from '@tabler/icons-react';

const Menuitems = [
  {
    navlabel: true,
    subheader: 'Hlavní',
  },
  {
    id: uniqueId(),
    title: 'Dashboard',
    icon: IconLayoutDashboard,
    href: '/dashboard',
  },
  {
    navlabel: true,
    subheader: 'Komponenty',
  },
  {
    id: uniqueId(),
    title: 'Autocomplete',
    icon: IconTypography,
    href: '/form-elements/autocomplete',
  },
  {
    id: uniqueId(),
    title: 'Buttons',
    icon: IconAlignBoxLeftBottom,
    href: '/form-elements/button',
  },
  {
    id: uniqueId(),
    title: 'Checkbox',
    icon: IconCheckbox,
    href: '/form-elements/checkbox',
  },
  {
    id: uniqueId(),
    title: 'Radio',
    icon: IconRadar,
    href: '/form-elements/radio',
  },
  {
    id: uniqueId(),
    title: 'Slider',
    icon: IconSlideshow,
    href: '/form-elements/slider',
  },
  {
    id: uniqueId(),
    title: 'Switch',
    icon: IconCaretUpDown,
    href: '/form-elements/switch',
  },
  {
    id: uniqueId(),
    title: 'Tables',
    icon: IconTable,
    href: '/tables/basic-table',
  },
  {
    id: uniqueId(),
    title: 'Form Layouts',
    icon: IconForms,
    href: '/form-layouts',
  },
  {
    id: uniqueId(),
    title: 'Typography',
    icon: IconTypography,
    href: '/ui/typography',
  },
  {
    id: uniqueId(),
    title: 'Shadow',
    icon: IconCopy,
    href: '/ui/shadow',
  },
  {
    navlabel: true,
    subheader: 'Uživatel',
  },
  {
    id: uniqueId(),
    title: 'Můj profil',
    icon: IconUser,
    href: '/profile',
  },
  {
    navlabel: true,
    subheader: 'Ostatní',
  },
  {
    id: uniqueId(),
    title: 'Sample Page',
    icon: IconAperture,
    href: '/sample-page',
  },
];

export default Menuitems;
