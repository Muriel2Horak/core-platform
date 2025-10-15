import { Box, Container, Typography, Grid, Button, Tabs, Tab } from '@mui/material';
import { Assessment, OpenInNew, Dashboard as DashboardIcon } from '@mui/icons-material';
import { useState } from 'react';
import { GrafanaEmbed } from '../../components/Monitoring';
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
            <GlassPaper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                🚀 Application Overview
              </Typography>
              <Typography variant="body2" color="text.secondary" mb={2}>
                Celkový přehled aktivity aplikace, logů a událostí
              </Typography>
              <GrafanaEmbed
                dashboardUid="app-overview-dashboard"
                height="800px"
                theme="light"
              />
            </GlassPaper>
          </Grid>
        </Grid>
      </TabPanel>

      {/* Tab 1: Performance */}
      <TabPanel value={tabValue} index={1}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <GlassPaper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                ⚡ Performance Monitoring
              </Typography>
              <Typography variant="body2" color="text.secondary" mb={2}>
                Výkon aplikace, odezvy API a využití zdrojů
              </Typography>
              <GrafanaEmbed
                dashboardUid="performance-dashboard"
                height="800px"
                theme="light"
              />
            </GlassPaper>
          </Grid>
        </Grid>
      </TabPanel>

      {/* Tab 2: Security */}
      <TabPanel value={tabValue} index={2}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <GlassPaper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                🔒 Security Monitoring
              </Typography>
              <Typography variant="body2" color="text.secondary" mb={2}>
                Bezpečnostní události, neúspěšné přihlášení a hrozby
              </Typography>
              <GrafanaEmbed
                dashboardUid="security-dashboard"
                height="800px"
                theme="light"
              />
            </GlassPaper>
          </Grid>
        </Grid>
      </TabPanel>

      {/* Tab 3: Audit */}
      <TabPanel value={tabValue} index={3}>
        <Grid container spacing={3}>
          <Grid item xs={12}>
            <GlassPaper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                📝 Audit Trail
              </Typography>
              <Typography variant="body2" color="text.secondary" mb={2}>
                Audit log všech důležitých událostí a změn v systému
              </Typography>
              <GrafanaEmbed
                dashboardUid="audit-dashboard"
                height="800px"
                theme="light"
              />
            </GlassPaper>
          </Grid>
        </Grid>
      </TabPanel>

      {/* Tab 4: Infrastructure */}
      <TabPanel value={tabValue} index={4}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={6}>
            <GlassPaper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                📊 Loki Overview
              </Typography>
              <GrafanaEmbed
                dashboardUid="loki-overview"
                height="400px"
                theme="light"
              />
            </GlassPaper>
          </Grid>
          <Grid item xs={12} md={6}>
            <GlassPaper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                🖥️ Infrastructure Overview
              </Typography>
              <GrafanaEmbed
                dashboardUid="infra-overview"
                height="400px"
                theme="light"
              />
            </GlassPaper>
          </Grid>
          <Grid item xs={12}>
            <GlassPaper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                💚 Core Platform Status
              </Typography>
              <GrafanaEmbed
                dashboardUid="core-platform-status"
                height="400px"
                theme="light"
              />
            </GlassPaper>
          </Grid>
        </Grid>
      </TabPanel>
    </Container>
  );
};

export default MonitoringPage;
