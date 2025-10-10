import React, { useEffect, useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Grid,
  Typography,
  Tab,
  Tabs,
  Alert,
  CircularProgress,
  Paper,
} from '@mui/material';
import { Warning, CheckCircle, Error as ErrorIcon } from '@mui/icons-material';
import axios from 'axios';

interface StreamingMetrics {
  queueDepth: number;
  unsentOutbox: number;
  successRate: number;
  dlqMessages: number;
}

const StreamingDashboardPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState(0);
  const [metrics, setMetrics] = useState<StreamingMetrics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Grafana embed URLs - using VITE_ prefix for Vite env vars
  const grafanaBaseUrl = (window as any).ENV?.GRAFANA_URL || 'http://localhost:3001';
  const overviewUrl = `${grafanaBaseUrl}/d/streaming-overview/core-streaming-overview?orgId=1&refresh=30s&theme=light&kiosk`;
  const entitiesUrl = `${grafanaBaseUrl}/d/streaming-entities/core-streaming-entities?orgId=1&refresh=30s&theme=light&kiosk`;
  const opsUrl = `${grafanaBaseUrl}/d/streaming-ops/core-streaming-operations?orgId=1&refresh=30s&theme=light&kiosk`;

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
      setError(err.message || 'Failed to fetch metrics');
    } finally {
      setLoading(false);
    }
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
    <Box sx={{ p: 3 }}>
      <Typography variant="h4" gutterBottom>
        Streaming Dashboard
      </Typography>

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

      {/* Grafana Dashboards */}
      <Paper sx={{ mb: 3 }}>
        <Tabs value={activeTab} onChange={(_, v) => setActiveTab(v)} sx={{ borderBottom: 1, borderColor: 'divider' }}>
          <Tab label="Overview" />
          <Tab label="Entities" />
          <Tab label="Operations" />
        </Tabs>

        <Box sx={{ p: 2 }}>
          {activeTab === 0 && (
            <iframe
              src={overviewUrl}
              width="100%"
              height="800px"
              frameBorder="0"
              title="Streaming Overview"
            />
          )}
          {activeTab === 1 && (
            <iframe
              src={entitiesUrl}
              width="100%"
              height="800px"
              frameBorder="0"
              title="Streaming Entities"
            />
          )}
          {activeTab === 2 && (
            <iframe
              src={opsUrl}
              width="100%"
              height="800px"
              frameBorder="0"
              title="Streaming Operations"
            />
          )}
        </Box>
      </Paper>
    </Box>
  );
};

export default StreamingDashboardPage;
