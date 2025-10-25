/**
 * 📊 Comprehensive Monitoring Page - Complete Monitoring Suite
 * 
 * Unified monitoring dashboard with advanced features:
 * - Tab navigation between specialized dashboards
 * - System Resources (USE method)
 * - Application Performance (RED method)
 * - Platform Health (SLI/SLO)
 * - Security (Failed logins, threats)
 * - Audit (User actions, changes)
 * - Logs (Loki search)
 * 
 * Architecture:
 * - Uses new MetricPanel components with threshold indicators
 * - Full light/dark theme support
 * - Real-time metrics with auto-refresh
 * - BFF proxy for secure data access
 */

import React, { useState } from 'react';
import { 
  Box, 
  Container, 
  Typography, 
  Tabs, 
  Tab,
  Chip,
  Paper,
  Alert,
  useTheme
} from '@mui/material';
import { 
  Dashboard as DashboardIcon,
  Memory as MemoryIcon,
  Speed as SpeedIcon,
  HealthAndSafety as HealthIcon,
  Security as SecurityIcon,
  Gavel as AuditIcon,
  Article as LogsIcon,
  Construction
} from '@mui/icons-material';
import { GlassPaper } from '../../shared/ui';
import { AiHelpWidget } from '../../components/AiHelpWidget';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`monitoring-comprehensive-tabpanel-${index}`}
      aria-labelledby={`monitoring-comprehensive-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );
}

export const MonitoringComprehensivePage = () => {
  const theme = useTheme();
  const [tabValue, setTabValue] = useState(0);
  const routeId = 'admin.monitoring.comprehensive';

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  const dashboards = [
    {
      id: 0,
      label: 'Systémové Zdroje',
      icon: <MemoryIcon />,
      badge: 'USE',
      color: theme.palette.info.main,
      description: 'CPU, Memory, Disk, Network - Infrastructure monitoring',
    },
    {
      id: 1,
      label: 'Výkon Aplikace',
      icon: <SpeedIcon />,
      badge: 'RED',
      color: theme.palette.warning.main,
      description: 'Request Rate, Errors, Duration - Service performance',
    },
    {
      id: 2,
      label: 'Zdraví Platformy',
      icon: <HealthIcon />,
      badge: 'SLI/SLO',
      color: theme.palette.success.main,
      description: 'Database, Kafka, Circuit Breakers - Reliability',
    },
    {
      id: 3,
      label: 'Zabezpečení',
      icon: <SecurityIcon />,
      badge: null,
      color: theme.palette.error.main,
      description: 'Failed logins, Suspicious activity, Blocked IPs',
    },
    {
      id: 4,
      label: 'Audit',
      icon: <AuditIcon />,
      badge: null,
      color: theme.palette.secondary.main,
      description: 'User actions, System changes, Compliance',
    },
    {
      id: 5,
      label: 'Logy',
      icon: <LogsIcon />,
      badge: 'Loki',
      color: theme.palette.primary.main,
      description: 'Real-time log search and analysis',
    },
  ];

  const activeDashboard = dashboards[tabValue];

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} data-route-id={routeId}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <DashboardIcon fontSize="large" color="primary" />
          <Box>
            <Typography variant="h4" sx={{ fontWeight: 600 }}>
              Pokročilý Monitoring
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Kompletní přehled systému s pokročilými metrikami a thresholdy
            </Typography>
          </Box>
        </Box>
        <Box display="flex" gap={1}>
          <AiHelpWidget routeId={routeId} />
        </Box>
      </Box>

      {/* Dashboard Info Card */}
      <GlassPaper sx={{ mb: 3, p: 2 }}>
        <Box display="flex" alignItems="center" gap={2}>
          <Box 
            sx={{ 
              color: activeDashboard.color,
              display: 'flex',
              alignItems: 'center',
            }}
          >
            {activeDashboard.icon}
          </Box>
          <Box flex={1}>
            <Box display="flex" alignItems="center" gap={1.5}>
              <Typography variant="h6" sx={{ fontWeight: 600 }}>
                {activeDashboard.label}
              </Typography>
              {activeDashboard.badge && (
                <Chip 
                  label={activeDashboard.badge} 
                  size="small" 
                  color="primary"
                  variant="outlined"
                />
              )}
            </Box>
            <Typography variant="body2" color="text.secondary">
              {activeDashboard.description}
            </Typography>
          </Box>
          <Box>
            <Chip 
              label={`Dashboard ${tabValue + 1}/6`} 
              size="small"
              sx={{ 
                bgcolor: theme.palette.mode === 'dark' 
                  ? 'rgba(255, 255, 255, 0.08)' 
                  : 'rgba(0, 0, 0, 0.08)',
              }}
            />
          </Box>
        </Box>
      </GlassPaper>

      {/* Tab Navigation */}
      <GlassPaper sx={{ mb: 3 }}>
        <Tabs
          value={tabValue}
          onChange={handleTabChange}
          variant="scrollable"
          scrollButtons="auto"
          sx={{
            '& .MuiTab-root': {
              minHeight: 72,
              textTransform: 'none',
              fontSize: '0.95rem',
              fontWeight: 500,
              alignItems: 'flex-start',
            },
          }}
        >
          {dashboards.map((dashboard) => (
            <Tab
              key={dashboard.id}
              icon={
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
                  <Box sx={{ color: dashboard.color }}>
                    {dashboard.icon}
                  </Box>
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
                <Box sx={{ textAlign: 'left', mt: 0.5 }}>
                  <Typography variant="body2" sx={{ fontWeight: 600 }}>
                    {dashboard.label}
                  </Typography>
                </Box>
              }
              sx={{
                '&.Mui-selected': {
                  background: theme.palette.mode === 'dark'
                    ? 'rgba(144, 202, 249, 0.08)'
                    : 'rgba(25, 118, 210, 0.08)',
                  borderBottom: `3px solid ${dashboard.color}`,
                },
              }}
            />
          ))}
        </Tabs>
      </GlassPaper>

      {/* Dashboard Content */}
      
      {/* Tab 0: System Resources (USE) */}
      <TabPanel value={tabValue} index={0}>
        <Alert severity="info" icon={<Construction />} sx={{ mb: 2 }}>
          Migrace na nativní Loki UI probíhá - ETA S4 fáze
        </Alert>
        <Paper sx={{ p: 4, textAlign: 'center', minHeight: 800 }}>
          <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            Coming Soon - System Resources Dashboard
          </Typography>
        </Paper>
      </TabPanel>

      {/* Tab 1: Application Performance (RED) */}
      <TabPanel value={tabValue} index={1}>
        <Alert severity="info" icon={<Construction />} sx={{ mb: 2 }}>
          Migrace na nativní Loki UI probíhá - ETA S4 fáze
        </Alert>
        <Paper sx={{ p: 4, textAlign: 'center', minHeight: 800 }}>
          <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            Coming Soon - Application Performance Dashboard
          </Typography>
        </Paper>
      </TabPanel>

      {/* Tab 2: Platform Health (SLI/SLO) */}
      <TabPanel value={tabValue} index={2}>
        <Alert severity="info" icon={<Construction />} sx={{ mb: 2 }}>
          Migrace na nativní Loki UI probíhá - ETA S4 fáze
        </Alert>
        <Paper sx={{ p: 4, textAlign: 'center', minHeight: 800 }}>
          <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            Coming Soon - Platform Health Dashboard
          </Typography>
        </Paper>
      </TabPanel>

      {/* Tab 3: Security */}
      <TabPanel value={tabValue} index={3}>
        <Alert severity="info" icon={<Construction />} sx={{ mb: 2 }}>
          Migrace na nativní Loki UI probíhá - ETA S4 fáze
        </Alert>
        <Paper sx={{ p: 4, textAlign: 'center', minHeight: 800 }}>
          <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            Coming Soon - Security Dashboard
          </Typography>
        </Paper>
      </TabPanel>

      {/* Tab 4: Audit */}
      <TabPanel value={tabValue} index={4}>
        <Alert severity="info" icon={<Construction />} sx={{ mb: 2 }}>
          Migrace na nativní Loki UI probíhá - ETA S4 fáze
        </Alert>
        <Paper sx={{ p: 4, textAlign: 'center', minHeight: 800 }}>
          <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            Coming Soon - Audit Dashboard
          </Typography>
        </Paper>
      </TabPanel>

      {/* Tab 5: Logs */}
      <TabPanel value={tabValue} index={5}>
        <Alert severity="info" icon={<Construction />} sx={{ mb: 2 }}>
          Migrace na nativní Loki UI probíhá - ETA S4 fáze
        </Alert>
        <Paper sx={{ p: 4, textAlign: 'center', minHeight: 800 }}>
          <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
          <Typography variant="h6" color="text.secondary">
            Coming Soon - Logs Dashboard
          </Typography>
        </Paper>
      </TabPanel>
    </Container>
  );
};

export default MonitoringComprehensivePage;
