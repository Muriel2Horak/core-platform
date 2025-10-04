/**
 * 🏗️ AppShell - Main Application Shell Layout
 * 
 * Hlavní layout wrapper který kombinuje sidebar navigaci s obsahovou oblastí.
 * Podporuje dva módy: 'content' (centrálně zarovnaný) a 'work' (plná šířka).
 */

import React from 'react';
import { Box, styled } from '@mui/material';
import { SidebarNav, SidebarNavItem } from '../../shared/ui/SidebarNav';
import { PageContainer } from '../../shared/ui/PageContainer.tsx';
import { WorkArea } from '../../shared/ui/WorkArea.tsx';
import { tokens } from '../../shared/theme/tokens';
import { useAuth } from '../../components/AuthProvider';
import { useLocation } from 'react-router-dom';

// Define user type interface
interface User {
  id?: string | number;
  username?: string;
  email?: string;
  firstName?: string;
  lastName?: string;
  roles?: string[];
  tenant?: string;
  [key: string]: unknown; // More specific than any - allows additional properties but requires type checking
}

export interface AppShellProps {
  children: React.ReactNode;
  /** Layout variant */
  variant?: 'content' | 'work';
  /** Max width pro content variant */
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl' | false;
}

// 🎨 Styled komponenty
const ShellContainer = styled(Box)(() => ({
  display: 'flex',
  minHeight: '100vh',
  backgroundColor: tokens.colors.grey[50],
}));

const SidebarArea = styled(Box)(() => ({
  width: tokens.components.layout.sidebarWidth,
  flexShrink: 0,
  backgroundColor: tokens.colors.sidebar.bg,
  borderRight: `1px solid ${tokens.colors.grey[200]}`,

  // Sidebar shadow
  boxShadow: tokens.shadows.md,
  zIndex: 1200,

  '@media (max-width: 768px)': {
    position: 'fixed',
    left: 0,
    top: 0,
    height: '100vh',
    zIndex: 1300,
  },
}));

const MainArea = styled(Box)<{ variant: 'content' | 'work' }>(({ variant }) => ({
  flex: 1,
  display: 'flex',
  flexDirection: 'column',

  // Content variant má padding, work variant nemá
  ...(variant === 'content' && {
    padding: tokens.spacing.lg,
  }),

  // Work variant má minimální padding jen na stranách
  ...(variant === 'work' && {
    paddingLeft: tokens.spacing.md,
    paddingRight: tokens.spacing.md,
    paddingTop: tokens.spacing.md,
  }),

  '@media (max-width: 768px)': {
    marginLeft: 0,
    padding: tokens.spacing.md,
  },
}));

/**
 * AppShell komponent
 */
export const AppShell: React.FC<AppShellProps> = ({
  children,
  variant = 'content',
  maxWidth = 'xl',
}) => {
  const { user } = useAuth();
  const location = useLocation();

  const handleNavigation = (item: SidebarNavItem) => {
    // V reálné aplikaci by toto bylo řešeno routerem
    window.location.href = item.href;
  };

  // Bezpečné získání user roles s proper typing
  const typedUser = user as User | null;
  const userRoles: string[] = typedUser?.roles || [];

  return (
    <ShellContainer>
      {/* Sidebar */}
      <SidebarArea>
        <SidebarNav
          onItemClick={handleNavigation}
          currentPath={location.pathname}
          userRoles={userRoles}
          collapsed={false}
        />
      </SidebarArea>

      {/* Main Content Area */}
      <MainArea variant={variant}>
        {variant === 'content' ? (
          <PageContainer maxWidth={maxWidth}>
            {children}
          </PageContainer>
        ) : (
          <WorkArea>
            {children}
          </WorkArea>
        )}
      </MainArea>
    </ShellContainer>
  );
};

export default AppShell;