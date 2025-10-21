import { Container, Typography, Box } from '@mui/material';
import { Shield as ShieldIcon } from '@mui/icons-material';
import { AiHelpWidget } from '../../components/AiHelpWidget';
import { GrafanaEmbed } from '../../components/GrafanaEmbed';

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
      <GrafanaEmbed path="/d/axiom_security?orgId=1&theme=light&kiosk" height={800} />
    </Container>
  );
};

export default AdminSecurityPage;
