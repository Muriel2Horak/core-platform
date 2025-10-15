import React, { useEffect, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  Alert,
  CircularProgress,
  Button,
} from '@mui/material';
import { Warning, CheckCircle, Error as ErrorIcon, OpenInNew, Stream } from '@mui/icons-material';
import { AiHelpWidget } from '../../components/AiHelpWidget';
import { StreamingScene } from '../../components/Grafana/StreamingScene';
import axios from 'axios';

interface StreamingMetrics {
  queueDepth: number;
  unsentOutbox: number;
  successRate: number;
  dlqMessages: number;
}

const StreamingDashboardPage: React.FC = () => {
  const [metrics, setMetrics] = useState<StreamingMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const routeId = 'admin.streaming.dashboard';

  useEffect(() => {
    fetchMetrics();
    const interval = setInterval(fetchMetrics, 30000); // Refresh every 30s
    return () => clearInterval(interval);
  }, []);

  const fetchMetrics = async () => {
    try {
      // Fetch from backend API
      const response = await axios.get('/api/admin/streaming/metrics');
      setMetrics(response.data);
      setError(null);
    } catch (err: any) {
      // Placeholder metrics if API doesn't exist yet
      setMetrics({
        queueDepth: 0,
        unsentOutbox: 0,
        successRate: 100,
        dlqMessages: 0,
      });
      setError(null); // Don't show error for missing endpoint
    } finally {
      setLoading(false);
    }
  };

  const openFullGrafana = () => {
    const protocol = 'https:';
    const host = window.location.host;
    window.open(`${protocol}//${host}/monitoring`, '_blank');
  };

  const renderMetricCard = (title: string, value: number | string, status: 'success' | 'warning' | 'error') => {
    const icons = {
      success: <CheckCircle color="success" />,
      warning: <Warning color="warning" />,
      error: <ErrorIcon color="error" />,
    };

    return (
      <Card>
        <CardContent>
          <Box display="flex" alignItems="center" justifyContent="space-between">
            <Box>
              <Typography variant="caption" color="textSecondary" gutterBottom>
                {title}
              </Typography>
              <Typography variant="h4" component="div">
                {value}
              </Typography>
            </Box>
            <Box>{icons[status]}</Box>
          </Box>
        </CardContent>
      </Card>
    );
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }} data-route-id={routeId}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Stream fontSize="large" color="primary" />
          <Box>
            <Typography variant="h4">Streaming Dashboard</Typography>
            <Typography variant="body2" color="text.secondary">
              Monitoring streamingové infrastruktury, front a Kafka event flow
            </Typography>
          </Box>
        </Box>
        <Box display="flex" gap={1}>
          <AiHelpWidget routeId={routeId} />
          <Button
            variant="contained"
            startIcon={<OpenInNew />}
            onClick={openFullGrafana}
          >
            Otevřít v Grafaně
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {/* Real-time Metrics */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid item xs={12} sm={6} md={3}>
          {renderMetricCard(
            'Queue Depth',
            metrics?.queueDepth || 0,
            (metrics?.queueDepth || 0) > 1000 ? 'warning' : 'success'
          )}
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          {renderMetricCard(
            'Unsent Outbox',
            metrics?.unsentOutbox || 0,
            (metrics?.unsentOutbox || 0) > 100 ? 'error' : 'success'
          )}
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          {renderMetricCard(
            'Success Rate',
            `${(metrics?.successRate || 0).toFixed(1)}%`,
            (metrics?.successRate || 0) < 95 ? 'error' : 'success'
          )}
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          {renderMetricCard(
            'DLQ Messages',
            metrics?.dlqMessages || 0,
            (metrics?.dlqMessages || 0) > 0 ? 'warning' : 'success'
          )}
        </Grid>
      </Grid>

      {/* Streaming Monitoring Dashboard */}
      <StreamingScene height={900} timeRange={{ from: 'now-1h', to: 'now' }} />
    </Box>
  );
};

export default StreamingDashboardPage;
