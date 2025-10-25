import React from 'react';
import { Box, Typography, Tab, Tabs, Container, Grid } from '@mui/material';
import { Assessment } from '@mui/icons-material';
import { LogViewer, MetricCard } from '../components/Monitoring';

export default function Reports() {
  const [activeTab, setActiveTab] = React.useState(0);

  const TabPanel = ({ children, value, index }) => (
    <div hidden={value !== index}>
      {value === index && <Box sx={{ py: 3 }}>{children}</Box>}
    </div>
  );

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Assessment fontSize="large" color="primary" />
          <Box>
            <Typography variant="h4">Monitoring & Logs</Typography>
            <Typography variant="body2" color="text.secondary">
              Nativní Loki monitoring - Realtime log analysis
            </Typography>
          </Box>
        </Box>
      </Box>

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)}>
          <Tab label="Systém" />
          <Tab label="Aplikace" />
          <Tab label="Zabezpečení" />
        </Tabs>
      </Box>

      {/* Tab 0: System Logs */}
      <TabPanel value={activeTab} index={0}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={4}>
            <MetricCard title="System Metrics" hours={1} />
          </Grid>
          <Grid item xs={12}>
            <LogViewer 
              defaultQuery='{service=~"backend|frontend|nginx"}'
              defaultHours={1}
            />
          </Grid>
        </Grid>
      </TabPanel>

      {/* Tab 1: Application Logs */}
      <TabPanel value={activeTab} index={1}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={4}>
            <MetricCard title="Application Metrics" hours={3} />
          </Grid>
          <Grid item xs={12}>
            <LogViewer 
              defaultQuery='{service="backend"} |~ "(?i)(error|exception)"'
              defaultHours={3}
            />
          </Grid>
        </Grid>
      </TabPanel>

      {/* Tab 2: Security Logs */}
      <TabPanel value={activeTab} index={2}>
        <Grid container spacing={3}>
          <Grid item xs={12} md={4}>
            <MetricCard title="Security Metrics" hours={24} />
          </Grid>
          <Grid item xs={12}>
            <LogViewer 
              defaultQuery='{service=~".+"} |~ "(?i)(401|403|failed|unauthorized)"'
              defaultHours={24}
            />
          </Grid>
        </Grid>
      </TabPanel>
    </Container>
  );
}
