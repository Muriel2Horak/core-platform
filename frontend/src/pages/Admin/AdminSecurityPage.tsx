import { Container, Typography, Box } from '@mui/material';
import { Shield as ShieldIcon } from '@mui/icons-material';
import { GrafanaEmbed } from '../../components/Monitoring';
import { GlassPaper } from '../../shared/ui';

export const AdminSecurityPage = () => {
  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <ShieldIcon fontSize="large" color="primary" />
        <Box>
          <Typography variant="h4">Bezpečnostní monitoring</Typography>
          <Typography variant="body2" color="text.secondary">
            Přehled bezpečnostních událostí a hrozeb
          </Typography>
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
