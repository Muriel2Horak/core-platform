import { Box, Container, Typography, Alert, Chip } from '@mui/material';
import { Assessment } from '@mui/icons-material';
import { GlassPaper } from '../../shared/ui';
import { AiHelpWidget } from '../../components/AiHelpWidget';

export const AxiomMonitoringPage = () => {
  const routeId = 'admin.axiom-monitoring';

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} data-route-id={routeId}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Assessment fontSize="large" color="primary" />
          <Box>
            <Typography variant="h4">Axiom Monitoring Package</Typography>
            <Typography variant="body2" color="text.secondary">
              Production-grade observability platform
            </Typography>
          </Box>
        </Box>
        <AiHelpWidget routeId={routeId} />
      </Box>

      <Alert severity="info" sx={{ mb: 3 }}>
        <Typography variant="subtitle1" gutterBottom>
          📊 Axiom Monitoring Package
        </Typography>
        <Typography variant="body2">
          Všechny monitoring funkce jsou nyní dostupné v nativním <strong>Loki UI</strong>.
          Použijte hlavní Monitoring sekci pro přístup k logům a metrikám.
        </Typography>
        <Typography variant="body2" mt={1}>
          Axiom package obsahoval 8 Grafana dashboardů (System, Advanced, Streaming, Security, Audit, Performance, Health, Logs),
          které byly nahrazeny flexibilnějším LogQL query builderem s tenant isolation.
        </Typography>
      </Alert>

      <GlassPaper sx={{ p: 4, textAlign: 'center' }}>
        <Assessment sx={{ fontSize: 80, color: 'primary.main', mb: 2 }} />
        <Typography variant="h5" gutterBottom>
          Přejděte na Monitoring Page
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Všechny observability funkce dostupné v hlavní Monitoring sekci
        </Typography>
      </GlassPaper>
    </Container>
  );
};

export default AxiomMonitoringPage;
