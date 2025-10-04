/**
 * 🎨 Core Platform Design System
 * 
 * Centrální export všech shared komponent, témat a utilit.
 */

// Theme & Tokens
export { tokens } from './theme/tokens';
export { default as corePlatformTheme } from './theme/theme';
export { GlobalStyles } from './theme/GlobalStyles';

// UI Components
export { default as Loader } from './ui/Loader';
export { default as EmptyState } from './ui/EmptyState';
export { default as AppButton } from './ui/AppButton';
export { default as SidebarNav } from './ui/SidebarNav';
export { default as PageHeader } from './ui/PageHeader';
export { default as FormField } from './ui/FormField';
export { PageContainer } from './ui/PageContainer';
export { WorkArea, WorkSection } from './ui/WorkArea';

// Layouts
export { AppShell } from '../app/layouts/AppShell';

// Re-export pro lepší DX
export type { LoaderProps } from './ui/Loader';
export type { EmptyStateProps } from './ui/EmptyState';
export type { AppButtonProps } from './ui/AppButton';
export type { SidebarNavProps, SidebarNavItem } from './ui/SidebarNav';
export type { PageContainerProps } from './ui/PageContainer';
export type { WorkAreaProps, WorkSectionProps } from './ui/WorkArea';
export type { AppShellProps } from '../app/layouts/AppShell';