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
import { GrafanaEmbed } from '../../components/GrafanaEmbed';
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
    const interval = setInterval(fetchMetrics, 30000);
    return () => clearInterval(interval);
  }, []);

  const fetchMetrics = async () => {
    try {
      const response = await axios.get('/api/admin/streaming/metrics');
      setMetrics(response.data);
      setError(null);
    } catch (err: any) {
      setMetrics({
        queueDepth: 0,
        unsentOutbox: 0,
        successRate: 100,
        dlqMessages: 0,
      });
      setError(null);
    } finally {
      setLoading(false);
    }
  };

  const openFullGrafana = () => {
    window.open('https://' + window.location.host + '/monitoring/d/streaming?orgId=1', '_blank');
  };

  return (
    <Box sx={{ p: 3 }} data-route-id={routeId}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Stream fontSize="large" color="primary" />
          <Typography variant="h4">Streaming Dashboard</Typography>
        </Box>
        <Box display="flex" gap={1}>
          <AiHelpWidget routeId={routeId} />
          <Button variant="contained" startIcon={<OpenInNew />} onClick={openFullGrafana}>
            Otevřít v Grafaně
          </Button>
        </Box>
      </Box>

      {loading && (
        <Box display="flex" justifyContent="center" p={4}>
          <CircularProgress />
        </Box>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error}
        </Alert>
      )}

      {metrics && (
        <Grid container spacing={3} mb={3}>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>
                  Queue Depth
                </Typography>
                <Typography variant="h4">{metrics.queueDepth}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>
                  Unsent Outbox
                </Typography>
                <Typography variant="h4">{metrics.unsentOutbox}</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>
                  Success Rate
                </Typography>
                <Typography variant="h4">{metrics.successRate}%</Typography>
              </CardContent>
            </Card>
          </Grid>
          <Grid item xs={12} md={3}>
            <Card>
              <CardContent>
                <Typography color="text.secondary" gutterBottom>
                  DLQ Messages
                </Typography>
                <Typography variant="h4">{metrics.dlqMessages}</Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      )}

      <GrafanaEmbed path="/d/streaming?orgId=1&theme=light&kiosk" height="800px" />
    </Box>
  );
};

export default StreamingDashboardPage;
