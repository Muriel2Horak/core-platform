/**
 * 📊 Comprehensive Monitoring Page - Complete Monitoring Suite
 * 
 * Unified monitoring dashboard with advanced features:
 * - Tab navigation between specialized dashboards
 * - System Resources (USE method)
 * - Application Performance (RED method)
 * - Platform Health (SLI/SLO)
 * - Security (Failed logins, threats)
 * - Audit (User actions, changes)
 * - Logs (Loki search)
 * 
 * Architecture:
 * - Uses new MetricPanel components with threshold indicators
 * - Full light/dark theme support
 * - Real-time metrics with auto-refresh
 * - BFF proxy for secure data access
 */

import React from 'react';
import { 
  Box, 
  Container, 
  Typography, 
  Alert,
} from '@mui/material';
import { 
  Dashboard as DashboardIcon,
} from '@mui/icons-material';
import { GlassPaper } from '../../shared/ui';
import { AiHelpWidget } from '../../components/AiHelpWidget';

export const MonitoringComprehensivePage = () => {
  const routeId = 'admin.monitoring.comprehensive';

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} data-route-id={routeId}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <DashboardIcon fontSize="large" color="primary" />
          <Box>
            <Typography variant="h4">Comprehensive Monitoring</Typography>
            <Typography variant="body2" color="text.secondary">
              Advanced monitoring dashboards
            </Typography>
          </Box>
        </Box>
        <Box display="flex" gap={1}>
          <AiHelpWidget routeId={routeId} />
        </Box>
      </Box>

      <Alert severity="info" sx={{ mb: 3 }}>
        <Typography variant="subtitle1" gutterBottom>
          📊 Comprehensive Monitoring Dashboards
        </Typography>
        <Typography variant="body2">
          Pro pokročilé monitoring metriky použijte hlavní <strong>Monitoring</strong> stránku,
          která nyní nabízí nativní Loki UI s real-time log analysis.
        </Typography>
        <Typography variant="body2" mt={1}>
          Tato stránka obsahovala 6 specializovaných Grafana dashboardů, které byly nahrazeny
          flexibilnějším LogQL query builderem.
        </Typography>
      </Alert>

      <GlassPaper sx={{ p: 4, textAlign: 'center' }}>
        <DashboardIcon sx={{ fontSize: 80, color: 'primary.main', mb: 2 }} />
        <Typography variant="h5" gutterBottom>
          Použijte Monitoring Page
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Všechny monitoring funkce jsou dostupné v hlavní Monitoring sekci
        </Typography>
      </GlassPaper>
    </Container>
  );
};

export default MonitoringComprehensivePage;
