import { Box, Container, Typography, Grid, Button, Tabs, Tab } from '@mui/material';
import { Assessment, OpenInNew, Dashboard as DashboardIcon } from '@mui/icons-material';
import { useState } from 'react';
import { GlassPaper } from '../../shared/ui';
import { AiHelpWidget } from '../../components/AiHelpWidget';
import { SystemMonitoringScene } from '../../components/Grafana/SystemMonitoringScene';
import { SecurityScene } from '../../components/Grafana/SecurityScene';
import { AuditScene } from '../../components/Grafana/AuditScene';

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

  const openFullGrafana = () => {
    // ✅ OPRAVA: Vždy HTTPS
    const protocol = 'https:';
    const host = window.location.host;
    window.open(`${protocol}//${host}/monitoring`, '_blank');
  };

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} data-route-id={routeId}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Assessment fontSize="large" color="primary" />
          <Box>
            <Typography variant="h4">Monitoring & Analytics</Typography>
            <Typography variant="body2" color="text.secondary">
              Sledování výkonu, bezpečnosti a aktivity systému v reálném čase
            </Typography>
          </Box>
        </Box>
        <Box display="flex" gap={1}>
          <AiHelpWidget routeId={routeId} />
          <Button
            variant="contained"
            startIcon={<OpenInNew />}
            onClick={openFullGrafana}
          >
            Otevřít v Grafaně
          </Button>
        </Box>
      </Box>

      <GlassPaper sx={{ mb: 3 }}>
        <Tabs value={tabValue} onChange={handleTabChange} aria-label="monitoring tabs">
          <Tab label="📊 Přehled" icon={<DashboardIcon />} iconPosition="start" />
          <Tab label="⚡ Výkon" />
          <Tab label="🔒 Bezpečnost" />
          <Tab label="📝 Audit" />
          <Tab label="📈 Infrastruktura" />
        </Tabs>
      </GlassPaper>

      {/* Tab 0: Application Overview */}
      <TabPanel value={tabValue} index={0}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <SystemMonitoringScene height={800} timeRange={{ from: 'now-6h', to: 'now' }} />
          </Grid>
        </Grid>
      </TabPanel>

      {/* Tab 1: Performance */}
      <TabPanel value={tabValue} index={1}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <SystemMonitoringScene height={800} timeRange={{ from: 'now-1h', to: 'now' }} />
          </Grid>
        </Grid>
      </TabPanel>

      {/* Tab 2: Security */}
      <TabPanel value={tabValue} index={2}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <SecurityScene height={800} timeRange={{ from: 'now-24h', to: 'now' }} />
          </Grid>
        </Grid>
      </TabPanel>

      {/* Tab 3: Audit */}
      <TabPanel value={tabValue} index={3}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <AuditScene height={800} timeRange={{ from: 'now-7d', to: 'now' }} />
          </Grid>
        </Grid>
      </TabPanel>

      {/* Tab 4: Infrastructure */}
      <TabPanel value={tabValue} index={4}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <SystemMonitoringScene height={800} timeRange={{ from: 'now-24h', to: 'now' }} />
          </Grid>
        </Grid>
      </TabPanel>
    </Container>
  );
};

export default MonitoringPage;
