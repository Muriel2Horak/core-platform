import React from 'react';
import { Box, Typography, Alert, Tab, Tabs, Container, Paper } from '@mui/material';
import { Assessment, Construction } from '@mui/icons-material';

export default function Reports() {
  const [activeTab, setActiveTab] = React.useState(0);

  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Assessment fontSize="large" color="primary" />
          <Box>
            <Typography variant="h4">Reporty</Typography>
            <Typography variant="body2" color="text.secondary">
              Nativní Loki monitoring (v přípravě)
            </Typography>
          </Box>
        </Box>
      </Box>

      <Alert severity="info" icon={<Construction />} sx={{ mb: 3 }}>
        <Typography variant="subtitle1" gutterBottom>
          🚧 Nové Monitoring UI v přípravě
        </Typography>
        <Typography variant="body2">
          Migrujeme na nativní React komponenty nad Loki API. 
          Grafana zůstává dostupná jako samostatný admin nástroj.
        </Typography>
      </Alert>

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)}>
          <Tab label="Systém" />
          <Tab label="Aplikace" />
          <Tab label="Zabezpečení" />
        </Tabs>
      </Box>

      <Paper sx={{ p: 4, textAlign: 'center', bgcolor: 'background.default' }}>
        <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
        <Typography variant="h6" color="text.secondary">
          Coming Soon - Nativní Loki monitoring UI
        </Typography>
        <Typography variant="body2" color="text.secondary" mt={1}>
          ETA: S4 fáze (3-4 dny)
        </Typography>
      </Paper>
    </Container>
  );
}
