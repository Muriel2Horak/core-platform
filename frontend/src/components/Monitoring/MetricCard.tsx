import React, { useState, useEffect } from 'react';
import { Paper, Typography, Box, CircularProgress, Chip } from '@mui/material';
import {
  TrendingUp as TrendingUpIcon,
  TrendingDown as TrendingDownIcon,
  Error as ErrorIcon,
  CheckCircle as SuccessIcon,
} from '@mui/icons-material';
import axios from 'axios';

interface MetricCardProps {
  title: string;
  hours?: number;
  refreshInterval?: number; // milliseconds
}

interface MetricsSummary {
  totalLogs: number;
  errorLogs: number;
  errorRate: string;
  timeRange: string;
  tenant: string;
}

/**
 * 📊 METRIC CARD COMPONENT
 * 
 * Displays monitoring metrics from Loki:
 * - Total log volume
 * - Error count
 * - Error rate percentage
 * - Auto-refresh support
 */
export const MetricCard: React.FC<MetricCardProps> = ({
  title,
  hours = 1,
  refreshInterval = 60000, // 1 minute default
}) => {
  const [metrics, setMetrics] = useState<MetricsSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchMetrics = async () => {
    try {
      const response = await axios.get('/api/monitoring/metrics-summary', {
        params: { hours },
      });
      setMetrics(response.data);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch metrics');
      console.error('Metrics fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchMetrics();
  }, [hours]);

  useEffect(() => {
    const interval = setInterval(fetchMetrics, refreshInterval);
    return () => clearInterval(interval);
  }, [hours, refreshInterval]);

  if (loading) {
    return (
      <Paper sx={{ p: 3, textAlign: 'center' }}>
        <CircularProgress size={40} />
        <Typography variant="body2" color="text.secondary" mt={2}>
          Loading metrics...
        </Typography>
      </Paper>
    );
  }

  if (error) {
    return (
      <Paper sx={{ p: 3, bgcolor: 'error.light' }}>
        <ErrorIcon color="error" sx={{ fontSize: 40 }} />
        <Typography variant="h6" color="error" mt={1}>
          Metrics Error
        </Typography>
        <Typography variant="body2" color="error.dark">
          {error}
        </Typography>
      </Paper>
    );
  }

  if (!metrics) {
    return null;
  }

  const errorRateValue = parseFloat(metrics.errorRate.replace('%', ''));
  const isHealthy = errorRateValue < 5; // < 5% error rate = healthy

  return (
    <Paper sx={{ p: 3 }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={2}>
        <Typography variant="h6">{title}</Typography>
        <Chip 
          label={metrics.timeRange} 
          size="small" 
          color="primary" 
          variant="outlined" 
        />
      </Box>

      {/* Main Metrics */}
      <Box display="flex" gap={3} mb={2}>
        {/* Total Logs */}
        <Box flex={1}>
          <Typography variant="caption" color="text.secondary">
            Total Logs
          </Typography>
          <Typography variant="h4" fontWeight="bold">
            {metrics.totalLogs.toLocaleString()}
          </Typography>
        </Box>

        {/* Error Logs */}
        <Box flex={1}>
          <Typography variant="caption" color="text.secondary">
            Errors
          </Typography>
          <Typography variant="h4" fontWeight="bold" color="error.main">
            {metrics.errorLogs.toLocaleString()}
          </Typography>
        </Box>
      </Box>

      {/* Error Rate with Health Indicator */}
      <Box 
        display="flex" 
        alignItems="center" 
        gap={1}
        p={2}
        bgcolor={isHealthy ? 'success.light' : 'error.light'}
        borderRadius={1}
      >
        {isHealthy ? (
          <>
            <SuccessIcon color="success" />
            <TrendingDownIcon color="success" />
          </>
        ) : (
          <>
            <ErrorIcon color="error" />
            <TrendingUpIcon color="error" />
          </>
        )}
        <Box flex={1}>
          <Typography variant="caption" color="text.secondary">
            Error Rate
          </Typography>
          <Typography variant="h5" fontWeight="bold">
            {metrics.errorRate}
          </Typography>
        </Box>
        <Chip 
          label={isHealthy ? 'Healthy' : 'Degraded'} 
          color={isHealthy ? 'success' : 'error'}
          size="small"
        />
      </Box>

      {/* Tenant Info */}
      <Typography variant="caption" color="text.secondary" mt={2} display="block">
        Tenant: {metrics.tenant}
      </Typography>
    </Paper>
  );
};
