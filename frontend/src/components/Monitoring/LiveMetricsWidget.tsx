import { Box, Chip, Paper, Typography } from '@mui/material';
import { useEffect, useRef, useState } from 'react';

interface MetricsSummary {
  totalLogs?: number;
  errorLogs?: number;
  errorRate?: string;
  timeRange?: string;
  tenant?: string;
  timestamp?: string;
}

const buildWsUrl = () => {
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws';
  const token = localStorage.getItem('token');
  const tokenParam = token ? `?token=${encodeURIComponent(token)}` : '';
  return `${protocol}://${window.location.host}/ws/monitoring/live${tokenParam}`;
};

export const LiveMetricsWidget = () => {
  const [summary, setSummary] = useState<MetricsSummary | null>(null);
  const [status, setStatus] = useState<'connecting' | 'connected' | 'offline'>('connecting');
  const socketRef = useRef<WebSocket | null>(null);

  useEffect(() => {
    const socket = new WebSocket(buildWsUrl());
    socketRef.current = socket;

    socket.onopen = () => setStatus('connected');
    socket.onclose = () => setStatus('offline');
    socket.onerror = () => setStatus('offline');

    socket.onmessage = (event) => {
      try {
        const payload = JSON.parse(event.data) as MetricsSummary;
        setSummary(payload);
      } catch (error) {
        console.error('Live metrics parse failed', error);
      }
    };

    return () => {
      socket.close();
    };
  }, []);

  return (
    <Paper sx={{ p: 2, display: 'flex', alignItems: 'center', gap: 3 }}>
      <Box>
        <Typography variant="subtitle2" color="text.secondary">
          Live Metrics
        </Typography>
        <Typography variant="h6">
          {summary?.errorRate || '—'} error rate
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {summary?.totalLogs?.toLocaleString() || 0} logs / {summary?.timeRange || '1h'}
        </Typography>
      </Box>
      <Chip
        label={status}
        color={status === 'connected' ? 'success' : status === 'connecting' ? 'warning' : 'default'}
        size="small"
      />
      <Box marginLeft="auto" textAlign="right">
        <Typography variant="caption" color="text.secondary">
          Tenant
        </Typography>
        <Typography variant="body2">{summary?.tenant || 'unknown'}</Typography>
      </Box>
    </Paper>
  );
};

export default LiveMetricsWidget;
