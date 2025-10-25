import { Container, Typography, Box, Grid } from '@mui/material';
import { Shield as ShieldIcon } from '@mui/icons-material';
import { AiHelpWidget } from '../../components/AiHelpWidget';
import { LogViewer, MetricCard } from '../../components/Monitoring';

export const AdminSecurityPage = () => {
  const routeId = 'admin.security.monitoring';

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} data-route-id={routeId}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <ShieldIcon fontSize="large" color="primary" />
        <Box flex={1}>
          <Typography variant="h4">Bezpečnostní monitoring</Typography>
          <Typography variant="body2" color="text.secondary">
            Failed logins, 401/403 errors, security events
          </Typography>
        </Box>
        <Box>
          <AiHelpWidget routeId={routeId} />
        </Box>
      </Box>
      
      <Grid container spacing={3}>
        <Grid item xs={12} md={4}>
          <MetricCard title="Security Metrics (24h)" hours={24} />
        </Grid>
        
        <Grid item xs={12}>
          <LogViewer 
            defaultQuery='{service=~".+"} |~ "(?i)(401|403|unauthorized|failed|denied|security)"'
            defaultHours={24}
            showQueryBuilder={true}
          />
        </Grid>
      </Grid>
    </Container>
  );
};

export default AdminSecurityPage;
