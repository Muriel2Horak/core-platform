import React, { useState } from "react";
import { styled, Container, Box } from '@mui/material';
import Header from './header/Header';
import Sidebar from './sidebar/Sidebar';
import { Outlet } from "react-router";
import Footer from "./footer/Footer";

const MainWrapper = styled('div')(() => ({
  display: 'flex',
  width: '100%',
}));

const PageWrapper = styled('div')(() => ({
  display: 'flex',
  flexGrow: 1,
  flexDirection: 'column',
  zIndex: 1,
  backgroundColor: 'transparent',
}));

const FullLayout = () => {
  const [isSidebarOpen, setSidebarOpen] = useState(true);
  const [isMobileSidebarOpen, setMobileSidebarOpen] = useState(false);

  return React.createElement(React.Fragment, null,
    React.createElement(MainWrapper, { className: 'mainwrapper' },
      React.createElement(Sidebar, {
        isSidebarOpen: isSidebarOpen,
        isMobileSidebarOpen: isMobileSidebarOpen,
        onSidebarClose: () => setMobileSidebarOpen(false)
      }),
      
      React.createElement(PageWrapper, { className: "page-wrapper" },
        React.createElement(Header, {
          toggleSidebar: () => setSidebarOpen(!isSidebarOpen),
          toggleMobileSidebar: () => setMobileSidebarOpen(true)
        }),
        
        React.createElement(Container, {
          sx: {
            paddingTop: "20px",
            maxWidth: '1200px',
          }
        },
          React.createElement(Box, {
            sx: { minHeight: 'calc(100vh - 250px)' }
          },
            React.createElement(Outlet, null)
          )
        ),
        
        React.createElement(Footer, null)
      )
    )
  );
};

export default FullLayout;
