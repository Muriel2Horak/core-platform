import { Box, Container, Typography, Tabs, Tab, Alert, Paper } from '@mui/material';
import { Assessment, Construction } from '@mui/icons-material';
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
              Nativní Loki monitoring UI (v přípravě)
            </Typography>
          </Box>
        </Box>
        <Box display="flex" gap={1}>
          <AiHelpWidget routeId={routeId} />
        </Box>
      </Box>

      <Alert severity="info" icon={<Construction />} sx={{ mb: 3 }}>
        <Typography variant="subtitle2" gutterBottom>
          🚧 Migrace z Grafana iframe → Native Loki UI
        </Typography>
        <Typography variant="body2">
          Nové monitoring UI s React komponentami nad Loki API - ETA S4 fáze (3-4 dny)
        </Typography>
      </Alert>

      <GlassPaper>
        <Tabs value={tabValue} onChange={handleTabChange}>
          <Tab label="Systém" />
          <Tab label="Zabezpečení" />
          <Tab label="Audit" />
        </Tabs>

        <TabPanel value={tabValue} index={0}>
          <Paper sx={{ p: 4, textAlign: 'center', bgcolor: 'background.default', minHeight: 600 }}>
            <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary">
              Coming Soon - System Monitoring Dashboard
            </Typography>
          </Paper>
        </TabPanel>

        <TabPanel value={tabValue} index={1}>
          <Paper sx={{ p: 4, textAlign: 'center', bgcolor: 'background.default', minHeight: 600 }}>
            <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary">
              Coming Soon - Security Dashboard
            </Typography>
          </Paper>
        </TabPanel>

        <TabPanel value={tabValue} index={2}>
          <Paper sx={{ p: 4, textAlign: 'center', bgcolor: 'background.default', minHeight: 600 }}>
            <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
            <Typography variant="h6" color="text.secondary">
              Coming Soon - Audit Log Dashboard
            </Typography>
          </Paper>
        </TabPanel>
      </GlassPaper>
    </Container>
  );
};

export default MonitoringPage;
