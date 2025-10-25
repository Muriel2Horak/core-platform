import { Box, Container, Typography, Tabs, Tab, Grid } from '@mui/material';
import { Assessment } from '@mui/icons-material';
import { useState } from 'react';
import { GlassPaper } from '../../shared/ui';
import { AiHelpWidget } from '../../components/AiHelpWidget';
import { LogViewer, MetricCard } from '../../components/Monitoring';

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
      id={`monitoring-tabpanel-${index}`}
      aria-labelledby={`monitoring-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );
}

export const MonitoringPage = () => {
  const [tabValue, setTabValue] = useState(0);
  const routeId = 'admin.monitoring';

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} data-route-id={routeId}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Assessment fontSize="large" color="primary" />
          <Box>
            <Typography variant="h4">Monitoring</Typography>
            <Typography variant="body2" color="text.secondary">
              Native Loki monitoring - System, Security, Audit
            </Typography>
          </Box>
        </Box>
        <Box display="flex" gap={1}>
          <AiHelpWidget routeId={routeId} />
        </Box>
      </Box>

      <GlassPaper>
        <Tabs value={tabValue} onChange={handleTabChange}>
          <Tab label="Systém" />
          <Tab label="Zabezpečení" />
          <Tab label="Audit" />
        </Tabs>

        {/* Tab 0: System */}
        <TabPanel value={tabValue} index={0}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={4}>
              <MetricCard title="System Metrics (1h)" hours={1} />
            </Grid>
            <Grid item xs={12}>
              <LogViewer 
                defaultQuery='{service=~"backend|frontend|nginx"}'
                defaultHours={1}
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Tab 1: Security */}
        <TabPanel value={tabValue} index={1}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={4}>
              <MetricCard title="Security Metrics (24h)" hours={24} />
            </Grid>
            <Grid item xs={12}>
              <LogViewer 
                defaultQuery='{service=~".+"} |~ "(?i)(401|403|unauthorized|security)"'
                defaultHours={24}
              />
            </Grid>
          </Grid>
        </TabPanel>

        {/* Tab 2: Audit */}
        <TabPanel value={tabValue} index={2}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={4}>
              <MetricCard title="Audit Metrics (12h)" hours={12} />
            </Grid>
            <Grid item xs={12}>
              <LogViewer 
                defaultQuery='{service="backend"} |~ "(?i)(audit|created|updated|deleted)"'
                defaultHours={12}
              />
            </Grid>
          </Grid>
        </TabPanel>
      </GlassPaper>
    </Container>
  );
};

export default MonitoringPage;
