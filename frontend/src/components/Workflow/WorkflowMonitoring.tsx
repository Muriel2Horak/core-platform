import { useEffect, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  Alert,
  LinearProgress,
  Chip,
  List,
  ListItem,
  ListItemText,
} from '@mui/material';
import {
  Error as ErrorIcon,
  CheckCircle as SuccessIcon,
  Warning as WarningIcon,
  Speed as SpeedIcon,
} from '@mui/icons-material';

/**
 * 📊 W12: Workflow Monitoring Dashboard
 * 
 * Real-time monitoring UI:
 * - Health overview (active instances, success rate, stuck, SLA)
 * - State distribution chart
 * - Recent stuck instances
 * - SLA violations list
 * 
 * @since 2025-01-14
 */

interface WorkflowMonitoringProps {
  entityType: string;
}

interface WorkflowHealth {
  entityType: string;
  tenantId: string;
  activeInstances: number;
  completedLast24h: number;
  stuckInstances: number;
  slaViolations: number;
  successRate: number;
  avgDurationSeconds: number;
  timestamp: string;
}

interface StateDistribution {
  state: string;
  count: number;
}

interface StuckInstance {
  instanceId: number;
  entityId: string;
  currentState: string;
  lastUpdated: string;
  hoursStuck: number;
}

interface SLAViolation {
  instanceId: number;
  entityId: string;
  currentState: string;
  startedAt: string;
  minutesRunning: number;
  slaMinutes: number;
}

export const WorkflowMonitoring = ({ entityType }: WorkflowMonitoringProps) => {
  const [health, setHealth] = useState<WorkflowHealth | null>(null);
  const [distribution, setDistribution] = useState<StateDistribution[]>([]);
  const [stuck, setStuck] = useState<StuckInstance[]>([]);
  const [violations, setViolations] = useState<SLAViolation[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);

        // Fetch health
        const healthRes = await fetch(`/api/v1/workflows/monitoring/health/${entityType}`, {
          headers: { 'X-Tenant-Id': 'T1' },
        });
        const healthData = await healthRes.json();
        setHealth(healthData);

        // Fetch distribution
        const distRes = await fetch(`/api/v1/workflows/monitoring/distribution/${entityType}`, {
          headers: { 'X-Tenant-Id': 'T1' },
        });
        const distData = await distRes.json();
        setDistribution(distData);

        // Fetch stuck instances
        const stuckRes = await fetch(`/api/v1/workflows/monitoring/stuck/${entityType}?hoursThreshold=2`, {
          headers: { 'X-Tenant-Id': 'T1' },
        });
        const stuckData = await stuckRes.json();
        setStuck(stuckData);

        // Fetch SLA violations
        const violRes = await fetch(`/api/v1/workflows/monitoring/sla-violations/${entityType}?slaMinutes=60`, {
          headers: { 'X-Tenant-Id': 'T1' },
        });
        const violData = await violRes.json();
        setViolations(violData);

        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to fetch monitoring data');
      } finally {
        setLoading(false);
      }
    };

    fetchData();

    // Auto-refresh every 30s
    const interval = setInterval(fetchData, 30000);
    return () => clearInterval(interval);
  }, [entityType]);

  if (loading && !health) {
    return (
      <Box p={3}>
        <LinearProgress />
        <Typography variant="body2" mt={2}>
          Loading monitoring data...
        </Typography>
      </Box>
    );
  }

  if (error) {
    return (
      <Box p={3}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  if (!health) {
    return null;
  }

  const healthColor = health.successRate >= 95 ? 'success' : health.successRate >= 90 ? 'warning' : 'error';

  return (
    <Box p={3}>
      <Typography variant="h4" gutterBottom>
        Workflow Monitoring - {entityType}
      </Typography>

      {/* Health Overview */}
      <Grid container spacing={2} mb={3}>
        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Active Instances
              </Typography>
              <Typography variant="h4">{health.activeInstances}</Typography>
              <Typography variant="caption" color="textSecondary">
                {health.completedLast24h} completed (24h)
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Success Rate
              </Typography>
              <Box display="flex" alignItems="center">
                <Typography variant="h4" mr={1}>
                  {health.successRate.toFixed(1)}%
                </Typography>
                {health.successRate >= 95 ? (
                  <SuccessIcon color="success" />
                ) : health.successRate >= 90 ? (
                  <WarningIcon color="warning" />
                ) : (
                  <ErrorIcon color="error" />
                )}
              </Box>
              <Chip
                label={healthColor}
                color={healthColor}
                size="small"
                sx={{ mt: 1 }}
              />
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                Stuck Instances
              </Typography>
              <Typography variant="h4" color={health.stuckInstances > 0 ? 'error' : 'success'}>
                {health.stuckInstances}
              </Typography>
              <Typography variant="caption" color="textSecondary">
                No activity &gt;2h
              </Typography>
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} sm={6} md={3}>
          <Card>
            <CardContent>
              <Typography color="textSecondary" gutterBottom>
                SLA Violations
              </Typography>
              <Typography variant="h4" color={health.slaViolations > 0 ? 'warning' : 'success'}>
                {health.slaViolations}
              </Typography>
              <Typography variant="caption" color="textSecondary">
                Running &gt;60min
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Avg Duration */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Box display="flex" alignItems="center" mb={1}>
            <SpeedIcon sx={{ mr: 1 }} />
            <Typography variant="h6">Average Execution Time</Typography>
          </Box>
          <Typography variant="h4">{health.avgDurationSeconds.toFixed(2)}s</Typography>
        </CardContent>
      </Card>

      {/* State Distribution */}
      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>
            State Distribution
          </Typography>
          <List>
            {distribution.map((item) => (
              <ListItem key={item.state}>
                <ListItemText
                  primary={item.state}
                  secondary={`${item.count} instances`}
                />
                <Chip label={item.count} color="primary" />
              </ListItem>
            ))}
          </List>
        </CardContent>
      </Card>

      {/* Stuck Instances */}
      {stuck.length > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom color="error">
              Stuck Instances
            </Typography>
            <List>
              {stuck.map((item) => (
                <ListItem key={item.instanceId}>
                  <ListItemText
                    primary={`${item.entityId} (${item.currentState})`}
                    secondary={`Stuck for ${item.hoursStuck.toFixed(1)} hours`}
                  />
                  <Chip label={`#${item.instanceId}`} size="small" />
                </ListItem>
              ))}
            </List>
          </CardContent>
        </Card>
      )}

      {/* SLA Violations */}
      {violations.length > 0 && (
        <Card sx={{ mb: 3 }}>
          <CardContent>
            <Typography variant="h6" gutterBottom color="warning">
              SLA Violations
            </Typography>
            <List>
              {violations.map((item) => (
                <ListItem key={item.instanceId}>
                  <ListItemText
                    primary={`${item.entityId} (${item.currentState})`}
                    secondary={`Running for ${item.minutesRunning.toFixed(0)} minutes (SLA: ${item.slaMinutes} min)`}
                  />
                  <Chip
                    label={`+${(item.minutesRunning - item.slaMinutes).toFixed(0)} min`}
                    color="warning"
                    size="small"
                  />
                </ListItem>
              ))}
            </List>
          </CardContent>
        </Card>
      )}

      <Typography variant="caption" color="textSecondary">
        Last updated: {new Date(health.timestamp).toLocaleString()} • Auto-refresh: 30s
      </Typography>
    </Box>
  );
};
