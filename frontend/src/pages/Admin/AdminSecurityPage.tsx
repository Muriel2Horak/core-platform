import { Container, Typography, Box, Paper, Alert } from '@mui/material';
import { Shield as ShieldIcon, Construction } from '@mui/icons-material';
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
            Nativní Loki security monitoring (v přípravě)
          </Typography>
        </Box>
        <Box>
          <AiHelpWidget routeId={routeId} />
        </Box>
      </Box>
      
      <Alert severity="info" icon={<Construction />} sx={{ mb: 3 }}>
        Migrace na nativní Loki UI probíhá - ETA S4 fáze
      </Alert>
      
      <Paper sx={{ p: 4, textAlign: 'center', bgcolor: 'background.default', minHeight: 600 }}>
        <Construction sx={{ fontSize: 64, color: 'text.secondary', mb: 2 }} />
        <Typography variant="h6" color="text.secondary">
          Coming Soon - Security Dashboard
        </Typography>
      </Paper>
    </Container>
  );
};

export default AdminSecurityPage;
