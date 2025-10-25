import { Container, Typography, Box, Grid } from '@mui/material';
import { Gavel as AuditIcon } from '@mui/icons-material';
import { AiHelpWidget } from '../../components/AiHelpWidget';
import { LogViewer, MetricCard } from '../../components/Monitoring';

export const AdminAuditPage = () => {
  const routeId = 'admin.audit.log';

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} data-route-id={routeId}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <AuditIcon fontSize="large" color="primary" />
        <Box flex={1}>
          <Typography variant="h4">Audit Log</Typography>
          <Typography variant="body2" color="text.secondary">
            CRUD events, workflow transitions, system changes
          </Typography>
        </Box>
        <Box>
          <AiHelpWidget routeId={routeId} />
        </Box>
      </Box>
      
      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <MetricCard title="Audit Metrics (24h)" hours={24} />
        </Grid>
        
        <Grid item xs={12}>
          <LogViewer 
            defaultQuery='{service="backend"} |~ "(?i)(audit|created|updated|deleted|transition)"'
            defaultHours={12}
            showQueryBuilder={true}
          />
        </Grid>
      </Grid>
    </Container>
  );
};

export default AdminAuditPage;
