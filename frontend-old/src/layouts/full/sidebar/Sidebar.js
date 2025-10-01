import React from 'react';
import { useMediaQuery, Box, Drawer } from '@mui/material';
import SidebarItems from './SidebarItems';
import Scrollbar from "../../../components/custom-scroll/Scrollbar";

const Sidebar = (props) => {
  const lgUp = useMediaQuery((theme) => theme.breakpoints.up("lg"));
  const sidebarWidth = '270px';

  if (lgUp) {
    return (
      React.createElement(Box, {
        sx: {
          width: sidebarWidth,
          flexShrink: 0,
        }
      },
        React.createElement(Drawer, {
          anchor: "left",
          open: props.isSidebarOpen,
          variant: "permanent",
          slotProps: {
            paper: {
              sx: {
                width: sidebarWidth,
                boxSizing: 'border-box',
                top: '70px',
              },
            }
          }
        },
          React.createElement(Scrollbar, {
            sx: { height: "calc(100% - 73px)" }
          },
            React.createElement(Box, null,
              React.createElement(SidebarItems, null)
            )
          )
        )
      )
    );
  }

  return (
    React.createElement(Drawer, {
      anchor: "left",
      open: props.isMobileSidebarOpen,
      onClose: props.onSidebarClose,
      variant: "temporary",
      slotProps: {
        paper: {
          sx: {
            width: sidebarWidth,
            boxShadow: (theme) => theme.shadows[8],
          }
        }
      }
    },
      React.createElement(SidebarItems, null)
    )
  );
};

export default Sidebar;
