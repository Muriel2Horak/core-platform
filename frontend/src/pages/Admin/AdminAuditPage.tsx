import { Container, Typography, Box } from '@mui/material';
import { BugReport as BugReportIcon } from '@mui/icons-material';
import { AiHelpWidget } from '../../components/AiHelpWidget';
import { GrafanaEmbed } from '../../components/GrafanaEmbed';

export const AdminAuditPage = () => {
  const routeId = 'admin.audit.log';

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} data-route-id={routeId}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <BugReportIcon fontSize="large" color="primary" />
        <Box flex={1}>
          <Typography variant="h4">Audit Log</Typography>
          <Typography variant="body2" color="text.secondary">
            Sledování všech důležitých událostí v systému
          </Typography>
        </Box>
        <Box>
          <AiHelpWidget routeId={routeId} />
        </Box>
      </Box>
      <GrafanaEmbed path="/d/axiom_audit?orgId=1&theme=light&kiosk" height="800px" />
    </Container>
  );
};

export default AdminAuditPage;
