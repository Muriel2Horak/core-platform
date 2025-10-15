import { Container, Typography, Box } from '@mui/material';
import { Shield as ShieldIcon } from '@mui/icons-material';
import { GrafanaEmbed } from '../../components/Monitoring';
import { GlassPaper } from '../../shared/ui';
import { AiHelpWidget } from '../../components/AiHelpWidget';

export const AdminSecurityPage = () => {
  const routeId = 'admin.security.monitoring';

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} data-route-id={routeId}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <ShieldIcon fontSize="large" color="primary" />
        <Box flex={1}>
          <Typography variant="h4">Bezpečnostní monitoring</Typography>
          <Typography variant="body2" color="text.secondary">
            Přehled bezpečnostních událostí a hrozeb
          </Typography>
        </Box>
        <Box>
          <AiHelpWidget routeId={routeId} />
        </Box>
      </Box>
      <GlassPaper sx={{ p: 3 }}>
        <GrafanaEmbed
          dashboardUid="security-dashboard"
          height="800px"
          theme="light"
        />
      </GlassPaper>
    </Container>
  );
};

export default AdminSecurityPage;
