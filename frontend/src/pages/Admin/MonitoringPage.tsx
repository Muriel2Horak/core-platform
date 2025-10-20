import { Box, Container, Typography, Grid, Button, Tabs, Tab } from '@mui/material';
import { Assessment, OpenInNew, Dashboard as DashboardIcon } from '@mui/icons-material';
import { useState } from 'react';
import { GlassPaper } from '../../shared/ui';
import { AiHelpWidget } from '../../components/AiHelpWidget';
import { GrafanaEmbed } from '../../components/GrafanaEmbed';

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
            <Typography variant="h4">Monitoring</Typography>
            <Typography variant="body2" color="text.secondary">
              Systémový monitoring a metriky
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

      <GlassPaper>
        <Tabs value={tabValue} onChange={handleTabChange}>
          <Tab label="Systém" />
          <Tab label="Zabezpečení" />
          <Tab label="Audit" />
        </Tabs>

        <TabPanel value={tabValue} index={0}>
          <GrafanaEmbed path="/d/system-resources?orgId=1&theme=light&kiosk" height="800px" />
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <GrafanaEmbed path="/d/security?orgId=1&theme=light&kiosk" height="800px" />
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          <GrafanaEmbed path="/d/audit?orgId=1&theme=light&kiosk" height="800px" />
        </TabPanel>
      </GlassPaper>
    </Container>
  );
};

export default MonitoringPage;
