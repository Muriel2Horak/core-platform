/**
 * MonitoringDashboardSelector - Tab Navigation for Monitoring Dashboards
 * 
 * Provides tabbed interface to switch between:
 * - Overview (current scene-monitoring-native.js)
 * - System Resources (USE method)
 * - Application Performance (RED method)
 * - Platform Health (SLI/SLO)
 * - Logs & Search (Loki)
 */

import React, { useState } from 'react';
import { 
  Box, 
  Tabs, 
  Tab, 
  useTheme, 
  Paper,
  Typography,
  Chip
} from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import MemoryIcon from '@mui/icons-material/Memory';
import SpeedIcon from '@mui/icons-material/Speed';
import HealthAndSafetyIcon from '@mui/icons-material/HealthAndSafety';
import ArticleIcon from '@mui/icons-material/Article';

export const MonitoringDashboardSelector = ({ onDashboardChange, currentDashboard = 'overview' }) => {
  const theme = useTheme();
  const [activeTab, setActiveTab] = useState(currentDashboard);

  const dashboards = [
    {
      id: 'overview',
      label: 'Overview',
      icon: <DashboardIcon />,
      description: 'Key metrics at a glance',
      badge: null,
    },
    {
      id: 'system',
      label: 'System Resources',
      icon: <MemoryIcon />,
      description: 'CPU, Memory, Disk, Network (USE)',
      badge: 'USE',
    },
    {
      id: 'application',
      label: 'Application',
      icon: <SpeedIcon />,
      description: 'Request Rate, Errors, Duration (RED)',
      badge: 'RED',
    },
    {
      id: 'health',
      label: 'Platform Health',
      icon: <HealthAndSafetyIcon />,
      description: 'Database, Kafka, Circuit Breakers',
      badge: 'SLI/SLO',
    },
    {
      id: 'logs',
      label: 'Logs',
      icon: <ArticleIcon />,
      description: 'Log search and analysis (Loki)',
      badge: null,
    },
  ];

  const handleChange = (event, newValue) => {
    setActiveTab(newValue);
    if (onDashboardChange) {
      onDashboardChange(newValue);
    }
  };

  return (
    <Box
      sx={{
        width: '100%',
        background: theme.palette.mode === 'dark' 
          ? 'linear-gradient(135deg, rgba(30, 30, 30, 0.95) 0%, rgba(42, 42, 42, 0.95) 100%)'
          : 'linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(250, 250, 250, 0.95) 100%)',
        backdropFilter: 'blur(10px)',
        borderRadius: 2,
        border: `1px solid ${theme.palette.divider}`,
        boxShadow: theme.shadows[3],
        mb: 2,
      }}
    >
      {/* Tab Bar */}
      <Tabs
        value={activeTab}
        onChange={handleChange}
        variant="scrollable"
        scrollButtons="auto"
        sx={{
          borderBottom: `1px solid ${theme.palette.divider}`,
          '& .MuiTab-root': {
            minHeight: 72,
            textTransform: 'none',
            fontSize: '0.95rem',
            fontWeight: 500,
          },
        }}
      >
        {dashboards.map((dashboard) => (
          <Tab
            key={dashboard.id}
            value={dashboard.id}
            icon={
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                {dashboard.icon}
                {dashboard.badge && (
                  <Chip 
                    label={dashboard.badge} 
                    size="small" 
                    color="primary" 
                    variant="outlined"
                    sx={{ fontSize: '0.7rem', height: 20 }}
                  />
                )}
              </Box>
            }
            iconPosition="start"
            label={
              <Box sx={{ textAlign: 'left', ml: 1 }}>
                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                  {dashboard.label}
                </Typography>
                <Typography variant="caption" sx={{ opacity: 0.6, display: 'block' }}>
                  {dashboard.description}
                </Typography>
              </Box>
            }
            sx={{
              alignItems: 'flex-start',
              justifyContent: 'flex-start',
              '&.Mui-selected': {
                background: theme.palette.mode === 'dark'
                  ? 'rgba(144, 202, 249, 0.08)'
                  : 'rgba(25, 118, 210, 0.08)',
              },
            }}
          />
        ))}
      </Tabs>

      {/* Active Dashboard Info */}
      <Box sx={{ px: 2, py: 1.5 }}>
        {dashboards.map((dashboard) => {
          if (dashboard.id === activeTab) {
            return (
              <Box key={dashboard.id} sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                <Box sx={{ color: theme.palette.primary.main }}>
                  {dashboard.icon}
                </Box>
                <Box>
                  <Typography variant="h6" sx={{ fontWeight: 600 }}>
                    {dashboard.label}
                  </Typography>
                  <Typography variant="body2" sx={{ opacity: 0.7 }}>
                    {dashboard.description}
                  </Typography>
                </Box>
                {dashboard.badge && (
                  <Chip 
                    label={dashboard.badge} 
                    color="primary" 
                    size="small"
                    sx={{ ml: 'auto' }}
                  />
                )}
              </Box>
            );
          }
          return null;
        })}
      </Box>
    </Box>
  );
};

export default MonitoringDashboardSelector;
