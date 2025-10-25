import { Box, Container, Typography, Tabs, Tab, Chip, Paper, Alert } from '@mui/material';
import { Assessment, Security, Stream, VerifiedUser, Settings, Dashboard as DashboardIcon, Construction } from '@mui/icons-material';
import { useState } from 'react';
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
      id={`axiom-monitoring-tabpanel-${index}`}
      aria-labelledby={`axiom-monitoring-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ py: 2 }}>{children}</Box>}
    </div>
  );
}

export const AxiomMonitoringPage = () => {
  const [tabValue, setTabValue] = useState(0);
  const routeId = 'admin.axiom-monitoring';

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} data-route-id={routeId}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Assessment fontSize="large" color="primary" />
          <Box>
            <Box display="flex" alignItems="center" gap={1}>
              <Typography variant="h4">Axiom Monitoring Package</Typography>
              <Chip label="Production Grade" color="success" size="small" />
            </Box>
            <Typography variant="body2" color="text.secondary">
              Comprehensive observability platform with SLO tracking, alerts, and multi-tenant support
            </Typography>
          </Box>
        </Box>
        <AiHelpWidget routeId={routeId} />
      </Box>

      <GlassPaper>
        <Tabs 
          value={tabValue} 
          onChange={handleTabChange}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ borderBottom: 1, borderColor: 'divider' }}
        >
          <Tab 
            icon={<DashboardIcon />} 
            iconPosition="start" 
            label="System Overview" 
          />
          <Tab 
            icon={<Settings />} 
            iconPosition="start" 
            label="Advanced" 
          />
          <Tab 
            icon={<Stream />} 
            iconPosition="start" 
            label="Streaming (Kafka)" 
          />
          <Tab 
            icon={<Security />} 
            iconPosition="start" 
            label="Security" 
          />
          <Tab 
            icon={<VerifiedUser />} 
            iconPosition="start" 
            label="Audit" 
          />
          <Tab 
            icon={<Assessment />} 
            iconPosition="start" 
            label="Performance" 
          />
          <Tab 
            icon={<DashboardIcon />} 
            iconPosition="start" 
            label="Platform Health" 
          />
          <Tab 
            icon={<Assessment />} 
            iconPosition="start" 
            label="Logs" 
          />
        </Tabs>

        {/* System Overview Tab */}
        <TabPanel value={tabValue} index={0}>
          <Alert severity="info" icon={<Construction />} sx={{ mb: 2 }}>
            Migrace na nativní Loki UI probíhá - ETA S4 fáze
          </Alert>
          <Paper sx={{ p: 4, textAlign: 'center', minHeight: 800 }}>
            <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary">
              Coming Soon - System Overview Dashboard
            </Typography>
          </Paper>
        </TabPanel>

        {/* Advanced Tab - Sub-dashboards */}
        <TabPanel value={tabValue} index={1}>
          <Alert severity="info" icon={<Construction />} sx={{ mb: 2 }}>
            Migrace na nativní Loki UI probíhá - ETA S4 fáze
          </Alert>
          <Paper sx={{ p: 4, textAlign: 'center', minHeight: 800 }}>
            <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary">
              Coming Soon - Advanced Monitoring Dashboards
            </Typography>
          </Paper>
        </TabPanel>

        {/* Streaming (Kafka) Tab */}
        <TabPanel value={tabValue} index={2}>
          <Alert severity="info" icon={<Construction />} sx={{ mb: 2 }}>
            Migrace na nativní Loki UI probíhá - ETA S4 fáze
          </Alert>
          <Paper sx={{ p: 4, textAlign: 'center', minHeight: 800 }}>
            <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary">
              Coming Soon - Kafka Lag Analysis Dashboard
            </Typography>
          </Paper>
        </TabPanel>

        {/* Security Tab */}
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

        {/* Audit Tab */}
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

        {/* Performance Tab */}
        <TabPanel value={tabValue} index={5}>
          <Alert severity="info" icon={<Construction />} sx={{ mb: 2 }}>
            Migrace na nativní Loki UI probíhá - ETA S4 fáze
          </Alert>
          <Paper sx={{ p: 4, textAlign: 'center', minHeight: 800 }}>
            <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary">
              Coming Soon - Performance Dashboard
            </Typography>
          </Paper>
        </TabPanel>

        {/* Platform Health Tab */}
        <TabPanel value={tabValue} index={6}>
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

        {/* Logs Tab */}
        <TabPanel value={tabValue} index={7}>
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
      </GlassPaper>
    </Container>
  );
};

export default AxiomMonitoringPage;
