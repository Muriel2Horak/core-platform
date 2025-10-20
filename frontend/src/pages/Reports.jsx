import React from 'react';
import { Box, Typography, Alert, Tab, Tabs, Button, Container } from '@mui/material';
import { Assessment, OpenInNew } from '@mui/icons-material';
import { GrafanaEmbed } from '../components/GrafanaEmbed';

export default function Reports() {
  const [activeTab, setActiveTab] = React.useState(0);

  const openFullGrafana = () => {
    window.open('https://' + window.location.host + '/monitoring', '_blank');
  };

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Assessment fontSize="large" color="primary" />
          <Box>
            <Typography variant="h4">Reporty</Typography>
            <Typography variant="body2" color="text.secondary">
              Grafana dashboardy a analýzy
            </Typography>
          </Box>
        </Box>
        <Button variant="contained" startIcon={<OpenInNew />} onClick={openFullGrafana}>
          Otevřít v Grafaně
        </Button>
      </Box>

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)}>
          <Tab label="Systém" />
          <Tab label="Aplikace" />
          <Tab label="Zabezpečení" />
        </Tabs>
      </Box>

      {activeTab === 0 && (
        <GrafanaEmbed path="/d/system-resources?orgId=1&theme=light&kiosk" height="800px" />
      )}
      {activeTab === 1 && (
        <GrafanaEmbed path="/d/app-performance?orgId=1&theme=light&kiosk" height="800px" />
      )}
      {activeTab === 2 && (
        <GrafanaEmbed path="/d/security?orgId=1&theme=light&kiosk" height="800px" />
      )}
    </Container>
  );
}
