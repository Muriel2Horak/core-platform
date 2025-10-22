import { Box, Container, Typography, Tabs, Tab, Button, Chip } from '@mui/material';
import { Assessment, OpenInNew, Security, Stream, VerifiedUser, Settings, Dashboard as DashboardIcon } from '@mui/icons-material';
import { useState } from 'react';
import { GlassPaper } from '../../shared/ui';
import { AiHelpWidget } from '../../components/AiHelpWidget';
import { GrafanaEmbed } from '../../components/GrafanaEmbed';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`axiom-monitoring-tabpanel-${index}`}
      aria-labelledby={`axiom-monitoring-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ py: 2 }}>{children}</Box>}
    </div>
  );
}

export const AxiomMonitoringPage = () => {
  const [tabValue, setTabValue] = useState(0);
  const routeId = 'admin.axiom-monitoring';

  const openFullGrafana = (dashboardUid: string) => {
    const protocol = window.location.protocol;
    const host = window.location.host;
    const path = `/core-admin/monitoring/d/${dashboardUid}`;
    window.open(`${protocol}//${host}${path}?orgId=1`, '_blank');
  };

  const handleTabChange = (_event: React.SyntheticEvent, newValue: number) => {
    setTabValue(newValue);
  };

  // Dashboard UIDs with slugs - Grafana requires full path including slug
  const dashboards = {
    system: 'axiom_sys_overview/system-overview',
    runtime: 'axiom_adv_runtime/runtime-monitoring',
    database: 'axiom_adv_db/database-monitoring',
    redis: 'axiom_adv_redis/redis-monitoring',
    kafkaLag: 'axiom_kafka_lag/kafka-lag-monitoring',
    security: 'axiom_security/security-monitoring',
    audit: 'axiom_audit/audit-monitoring',
    logs: 'loki-overview/loki-overview',
    performance: 'performance-dashboard/performance-dashboard',
    platformHealth: 'core-platform-status/platform-health',
  };

  return (
    <Container maxWidth="xl" sx={{ py: 4 }} data-route-id={routeId}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Assessment fontSize="large" color="primary" />
          <Box>
            <Box display="flex" alignItems="center" gap={1}>
              <Typography variant="h4">Axiom Monitoring Package</Typography>
              <Chip label="Production Grade" color="success" size="small" />
            </Box>
            <Typography variant="body2" color="text.secondary">
              Comprehensive observability platform with SLO tracking, alerts, and multi-tenant support
            </Typography>
          </Box>
        </Box>
        <Box display="flex" gap={1}>
          <AiHelpWidget routeId={routeId} />
          <Button
            variant="outlined"
            startIcon={<OpenInNew />}
            onClick={() => openFullGrafana(dashboards.system)}
            size="small"
          >
            Open in Grafana
          </Button>
        </Box>
      </Box>

      <GlassPaper>
        <Tabs 
          value={tabValue} 
          onChange={handleTabChange}
          variant="scrollable"
          scrollButtons="auto"
          sx={{ borderBottom: 1, borderColor: 'divider' }}
        >
          <Tab 
            icon={<DashboardIcon />} 
            iconPosition="start" 
            label="System Overview" 
          />
          <Tab 
            icon={<Settings />} 
            iconPosition="start" 
            label="Advanced" 
          />
          <Tab 
            icon={<Stream />} 
            iconPosition="start" 
            label="Streaming (Kafka)" 
          />
          <Tab 
            icon={<Security />} 
            iconPosition="start" 
            label="Security" 
          />
          <Tab 
            icon={<VerifiedUser />} 
            iconPosition="start" 
            label="Audit" 
          />
          <Tab 
            icon={<Assessment />} 
            iconPosition="start" 
            label="Performance" 
          />
          <Tab 
            icon={<DashboardIcon />} 
            iconPosition="start" 
            label="Platform Health" 
          />
          <Tab 
            icon={<Assessment />} 
            iconPosition="start" 
            label="Logs" 
          />
        </Tabs>

        {/* System Overview Tab */}
        <TabPanel value={tabValue} index={0}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Box>
              <Typography variant="h6" gutterBottom>
                System Overview
              </Typography>
              <Typography variant="body2" color="text.secondary">
                KPI cards, SLO tracking (99.9%), error budget, latency p95/p99, dependencies
              </Typography>
            </Box>
            <Button
              variant="text"
              size="small"
              startIcon={<OpenInNew />}
              onClick={() => openFullGrafana(dashboards.system)}
            >
              Full Screen
            </Button>
          </Box>
          <GrafanaEmbed
            path={`/d/${dashboards.system}`}
            height={900}
          />
        </TabPanel>

        {/* Advanced Tab - Sub-dashboards */}
        <TabPanel value={tabValue} index={1}>
          <Typography variant="h6" gutterBottom>
            Advanced Monitoring
          </Typography>
          <Typography variant="body2" color="text.secondary" mb={3}>
            Deep-dive into Runtime (JVM), Database, Redis, HTTP, Nginx
          </Typography>
          
          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle1" gutterBottom fontWeight="bold">
              Runtime & JVM
            </Typography>
            <GrafanaEmbed 
              path={`/d/${dashboards.runtime}`} 
              height={600} 
            />
          </Box>

          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle1" gutterBottom fontWeight="bold">
              Database Operations
            </Typography>
            <GrafanaEmbed 
              path={`/d/${dashboards.database}`} 
              height={600} 
            />
          </Box>

          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle1" gutterBottom fontWeight="bold">
              Redis Cache
            </Typography>
            <GrafanaEmbed 
              path={`/d/${dashboards.redis}`} 
              height={600} 
            />
          </Box>
        </TabPanel>

        {/* Streaming (Kafka) Tab */}
        <TabPanel value={tabValue} index={2}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Box>
              <Typography variant="h6" gutterBottom>
                Kafka Consumer Lag Analysis
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Consumer lag per topic/partition, produce/consume rate, lag velocity, DLQ monitoring
              </Typography>
            </Box>
            <Button
              variant="text"
              size="small"
              startIcon={<OpenInNew />}
              onClick={() => openFullGrafana(dashboards.kafkaLag)}
            >
              Full Screen
            </Button>
          </Box>
          <GrafanaEmbed 
            path={`/d/${dashboards.kafkaLag}`} 
            height={900} 
          />
        </TabPanel>

        {/* Security Tab */}
        <TabPanel value={tabValue} index={3}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Box>
              <Typography variant="h6" gutterBottom>
                Security & Compliance
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Failed logins, 403/401/429 spikes, JWT errors, anomalies, TLS cert expiry
              </Typography>
            </Box>
            <Button
              variant="text"
              size="small"
              startIcon={<OpenInNew />}
              onClick={() => openFullGrafana(dashboards.security)}
            >
              Full Screen
            </Button>
          </Box>
          <GrafanaEmbed 
            path={`/d/${dashboards.security}`} 
            height={900} 
          />
        </TabPanel>

        {/* Audit Tab */}
        <TabPanel value={tabValue} index={4}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Box>
              <Typography variant="h6" gutterBottom>
                Audit & Governance
              </Typography>
              <Typography variant="body2" color="text.secondary">
                CRUD events, workflow transitions, bulk operations, Grafana access, FE events
              </Typography>
            </Box>
            <Button
              variant="text"
              size="small"
              startIcon={<OpenInNew />}
              onClick={() => openFullGrafana(dashboards.audit)}
            >
              Full Screen
            </Button>
          </Box>
          <GrafanaEmbed 
            path={`/d/${dashboards.audit}`} 
            height={900} 
          />
        </TabPanel>

        {/* Performance Tab */}
        <TabPanel value={tabValue} index={5}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Box>
              <Typography variant="h6" gutterBottom>
                Application Performance (RED Method)
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Request Rate, Error Rate, Duration - Performance monitoring across all services
              </Typography>
            </Box>
            <Button
              variant="text"
              size="small"
              startIcon={<OpenInNew />}
              onClick={() => openFullGrafana(dashboards.performance)}
            >
              Full Screen
            </Button>
          </Box>
          <GrafanaEmbed 
            path={`/d/${dashboards.performance}`} 
            height={900} 
          />
        </TabPanel>

        {/* Platform Health Tab */}
        <TabPanel value={tabValue} index={6}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Box>
              <Typography variant="h6" gutterBottom>
                Platform Health & SLI/SLO
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Overall platform status, service level indicators and objectives
              </Typography>
            </Box>
            <Button
              variant="text"
              size="small"
              startIcon={<OpenInNew />}
              onClick={() => openFullGrafana(dashboards.platformHealth)}
            >
              Full Screen
            </Button>
          </Box>
          <GrafanaEmbed 
            path={`/d/${dashboards.platformHealth}`} 
            height={900} 
          />
        </TabPanel>

        {/* Logs Tab */}
        <TabPanel value={tabValue} index={7}>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
            <Box>
              <Typography variant="h6" gutterBottom>
                Log Analysis (Loki)
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Centralized log search and analysis with Loki
              </Typography>
            </Box>
            <Button
              variant="text"
              size="small"
              startIcon={<OpenInNew />}
              onClick={() => openFullGrafana(dashboards.logs)}
            >
              Full Screen
            </Button>
          </Box>
          <GrafanaEmbed 
            path={`/d/${dashboards.logs}`} 
            height={900} 
          />
        </TabPanel>
      </GlassPaper>
    </Container>
  );
};

export default AxiomMonitoringPage;
