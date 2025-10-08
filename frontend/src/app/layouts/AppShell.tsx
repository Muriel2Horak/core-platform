/**
 * 🏗️ AppShell - Main Application Shell Layout
 * 
 * Hlavní layout wrapper který kombinuje sidebar navigaci s obsahovou oblastí.
 * Podporuje dva módy: 'content' (centrálně zarovnaný) a 'work' (plná šířka).
 * Sidebar je resizable s auto-collapse při zúžení.
 */

import React, { useState, useEffect, useCallback, useRef } from 'react';
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

// � Sidebar resize constants
const SIDEBAR_MIN_WIDTH = 60; // Collapsed mode (icons only)
const SIDEBAR_MAX_WIDTH = 400; // Maximum expanded width
const SIDEBAR_DEFAULT_WIDTH = 240; // Default width (from tokens)
const SIDEBAR_COLLAPSE_THRESHOLD = 150; // Auto-collapse below this width
const SIDEBAR_STORAGE_KEY = 'sidebar-width';

// �🎨 Styled komponenty
const ShellContainer = styled(Box)(() => ({
  display: 'flex',
  minHeight: '100vh',
  backgroundColor: tokens.colors.grey[50],
}));

const SidebarArea = styled(Box)<{ width: number; isResizing: boolean }>(({ width, isResizing }) => ({
  width: `${width}px`,
  flexShrink: 0,
  backgroundColor: tokens.colors.sidebar.bg,
  borderRight: `1px solid ${tokens.colors.grey[200]}`,
  position: 'relative',
  transition: isResizing ? 'none' : 'width 0.2s ease-in-out',
  overflow: 'hidden', // Prevent content overflow during resize

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

// Resize handle
const ResizeHandle = styled(Box)(({ theme }) => ({
  position: 'absolute',
  right: '-4px', // Centered on border
  top: 0,
  bottom: 0,
  width: '8px', // Wider hit area
  cursor: 'col-resize',
  backgroundColor: 'rgba(255, 0, 0, 0.1)', // DEBUG: Temporary red background
  transition: 'background-color 0.2s ease-in-out',
  zIndex: 9999, // Very high to be on top of everything
  
  // DEBUG: Visible border
  borderLeft: '2px solid red',
  
  '&::after': {
    content: '""',
    position: 'absolute',
    right: '3px',
    top: '50%',
    transform: 'translateY(-50%)',
    width: '2px',
    height: '40px',
    backgroundColor: theme.palette.divider,
    borderRadius: '2px',
    opacity: 0,
    transition: 'opacity 0.2s ease-in-out',
  },
  
  '&:hover': {
    backgroundColor: 'rgba(25, 118, 210, 0.2)',
    
    '&::after': {
      opacity: 0.8,
      backgroundColor: theme.palette.primary.main,
    },
  },
  
  '&:active': {
    backgroundColor: 'rgba(25, 118, 210, 0.3)',
    
    '&::after': {
      opacity: 1,
      backgroundColor: theme.palette.primary.dark,
    },
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

  // 📏 Sidebar width state
  const [sidebarWidth, setSidebarWidth] = useState<number>(() => {
    // Load from localStorage or use default
    const saved = localStorage.getItem(SIDEBAR_STORAGE_KEY);
    return saved ? parseInt(saved, 10) : SIDEBAR_DEFAULT_WIDTH;
  });
  
  const [isResizing, setIsResizing] = useState(false);
  const sidebarRef = useRef<HTMLDivElement>(null);

  // Auto-collapse when width is below threshold
  const isCollapsed = sidebarWidth < SIDEBAR_COLLAPSE_THRESHOLD;

  // 🖱️ Mouse move handler for resizing
  const handleMouseMove = useCallback((e: MouseEvent) => {
    if (!isResizing) return;
    
    const newWidth = e.clientX;
    
    // Clamp between min and max
    if (newWidth >= SIDEBAR_MIN_WIDTH && newWidth <= SIDEBAR_MAX_WIDTH) {
      setSidebarWidth(newWidth);
      localStorage.setItem(SIDEBAR_STORAGE_KEY, newWidth.toString());
    }
  }, [isResizing]);

  // 🖱️ Mouse up handler
  const handleMouseUp = useCallback(() => {
    setIsResizing(false);
  }, []);

  // 🎯 Start resizing
  const handleMouseDown = useCallback((e: React.MouseEvent) => {
    e.preventDefault();
    setIsResizing(true);
  }, []);

  // 📌 Add/remove event listeners
  useEffect(() => {
    if (isResizing) {
      document.addEventListener('mousemove', handleMouseMove);
      document.addEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = 'col-resize';
      document.body.style.userSelect = 'none';
    } else {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    }

    return () => {
      document.removeEventListener('mousemove', handleMouseMove);
      document.removeEventListener('mouseup', handleMouseUp);
      document.body.style.cursor = '';
      document.body.style.userSelect = '';
    };
  }, [isResizing, handleMouseMove, handleMouseUp]);

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
      <SidebarArea ref={sidebarRef} width={sidebarWidth} isResizing={isResizing}>
        <Box sx={{ 
          height: '100%', 
          overflow: 'auto',
          position: 'relative',
          zIndex: 1,
        }}>
          <SidebarNav
            onItemClick={handleNavigation}
            currentPath={location.pathname}
            userRoles={userRoles}
            collapsed={isCollapsed}
          />
        </Box>
        
        {/* Resize Handle - Must be last to be on top */}
        <ResizeHandle 
          onMouseDown={handleMouseDown}
          title="Přetažením změňte šířku sidebaru"
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