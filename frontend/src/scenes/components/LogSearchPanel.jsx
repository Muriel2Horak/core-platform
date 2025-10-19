/**
 * LogSearchPanel - Custom React component for searching logs in Loki
 * Provides real-time log streaming, filtering, and search capabilities
 */

import React, { useEffect, useState } from 'react';
import { 
  Box, 
  Typography, 
  TextField, 
  Select, 
  MenuItem, 
  FormControl, 
  InputLabel,
  CircularProgress,
  Paper,
  Chip,
  useTheme,
  IconButton
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import RefreshIcon from '@mui/icons-material/Refresh';
import ErrorIcon from '@mui/icons-material/Error';
import WarningIcon from '@mui/icons-material/Warning';
import InfoIcon from '@mui/icons-material/Info';

export const LogSearchPanel = ({ 
  datasourceUid = 'loki',
  refreshInterval = 10000,
  maxLines = 100,
}) => {
  const theme = useTheme();
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [logLevel, setLogLevel] = useState('all');
  const [service, setService] = useState('all');

  useEffect(() => {
    fetchLogs();
    const interval = setInterval(fetchLogs, refreshInterval);
    return () => clearInterval(interval);
  }, [logLevel, service, refreshInterval]);

  const fetchLogs = async () => {
    try {
      console.log('[LogSearchPanel] Fetching logs...');
      
      // Build LogQL query based on filters
      let logQuery = '{job=~".+"}'; // All jobs
      
      if (service !== 'all') {
        logQuery = `{job="${service}"}`;
      }
      
      if (logLevel !== 'all') {
        logQuery += ` |= "${logLevel.toUpperCase()}"`;
      }
      
      if (searchQuery) {
        logQuery += ` |= "${searchQuery}"`;
      }

      const requestBody = {
        queries: [{
          refId: 'A',
          datasource: {
            uid: datasourceUid,
            type: 'loki'
          },
          expr: logQuery,
          maxLines: maxLines,
        }],
        from: 'now-1h',
        to: 'now'
      };
      
      const response = await fetch('/api/monitoring/ds/query', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify(requestBody),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result = await response.json();
      console.log('[LogSearchPanel] Logs received:', result);

      // Parse Loki response format
      const parsedLogs = parseLokiResponse(result);
      setLogs(parsedLogs);
      setLoading(false);
      setError(null);
    } catch (err) {
      console.error('[LogSearchPanel] Error fetching logs:', err);
      setError(err.message);
      setLoading(false);
    }
  };

  const parseLokiResponse = (result) => {
    try {
      // Parse Grafana/Loki response format
      if (result?.results?.A?.frames) {
        const frames = result.results.A.frames;
        const logEntries = [];
        
        frames.forEach(frame => {
          if (frame.data?.values) {
            const timestamps = frame.data.values[0] || [];
            const messages = frame.data.values[1] || [];
            
            for (let i = 0; i < timestamps.length; i++) {
              logEntries.push({
                timestamp: new Date(timestamps[i]),
                message: messages[i],
                level: detectLogLevel(messages[i]),
              });
            }
          }
        });
        
        // Sort by timestamp descending (newest first)
        return logEntries.sort((a, b) => b.timestamp - a.timestamp);
      }
      
      return [];
    } catch (err) {
      console.warn('[LogSearchPanel] Could not parse logs:', err);
      return [];
    }
  };

  const detectLogLevel = (message) => {
    const msgLower = message.toLowerCase();
    if (msgLower.includes('error') || msgLower.includes('err')) return 'error';
    if (msgLower.includes('warn')) return 'warning';
    if (msgLower.includes('info')) return 'info';
    if (msgLower.includes('debug')) return 'debug';
    return 'info';
  };

  const getLevelIcon = (level) => {
    switch (level) {
      case 'error':
        return <ErrorIcon sx={{ fontSize: '1rem', color: theme.palette.error.main }} />;
      case 'warning':
        return <WarningIcon sx={{ fontSize: '1rem', color: theme.palette.warning.main }} />;
      case 'info':
        return <InfoIcon sx={{ fontSize: '1rem', color: theme.palette.info.main }} />;
      default:
        return null;
    }
  };

  const getLevelColor = (level) => {
    switch (level) {
      case 'error':
        return theme.palette.error.main;
      case 'warning':
        return theme.palette.warning.main;
      case 'info':
        return theme.palette.info.main;
      default:
        return theme.palette.text.secondary;
    }
  };

  return (
    <Box
      sx={{
        width: '100%',
        height: '100%',
        padding: 2,
        background: theme.palette.mode === 'dark' 
          ? 'linear-gradient(135deg, rgba(30, 30, 30, 0.9) 0%, rgba(42, 42, 42, 0.9) 100%)'
          : 'linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(250, 250, 250, 0.9) 100%)',
        color: theme.palette.text.primary,
        borderRadius: 2,
        border: `1px solid ${theme.palette.divider}`,
        backdropFilter: 'blur(10px)',
        boxShadow: theme.shadows[2],
        display: 'flex',
        flexDirection: 'column',
      }}
    >
      {/* Header & Filters */}
      <Box sx={{ mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
          <Typography variant="h6" sx={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: 1 }}>
            📝 Log Search
          </Typography>
          <IconButton onClick={fetchLogs} size="small" sx={{ color: theme.palette.primary.main }}>
            <RefreshIcon />
          </IconButton>
        </Box>

        {/* Filters */}
        <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
          <TextField
            size="small"
            placeholder="Search logs..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && fetchLogs()}
            InputProps={{
              startAdornment: <SearchIcon sx={{ mr: 1, opacity: 0.5 }} />,
            }}
            sx={{ flex: 1, minWidth: 200 }}
          />
          
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>Level</InputLabel>
            <Select value={logLevel} label="Level" onChange={(e) => setLogLevel(e.target.value)}>
              <MenuItem value="all">All</MenuItem>
              <MenuItem value="error">Error</MenuItem>
              <MenuItem value="warning">Warning</MenuItem>
              <MenuItem value="info">Info</MenuItem>
              <MenuItem value="debug">Debug</MenuItem>
            </Select>
          </FormControl>
          
          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel>Service</InputLabel>
            <Select value={service} label="Service" onChange={(e) => setService(e.target.value)}>
              <MenuItem value="all">All Services</MenuItem>
              <MenuItem value="backend">Backend</MenuItem>
              <MenuItem value="frontend">Frontend</MenuItem>
              <MenuItem value="grafana">Grafana</MenuItem>
              <MenuItem value="postgres">PostgreSQL</MenuItem>
              <MenuItem value="kafka">Kafka</MenuItem>
            </Select>
          </FormControl>
        </Box>

        <Box sx={{ display: 'flex', gap: 1 }}>
          <Chip label={`${logs.length} logs`} size="small" variant="outlined" />
          {logLevel !== 'all' && (
            <Chip 
              label={`Level: ${logLevel}`} 
              size="small" 
              onDelete={() => setLogLevel('all')} 
              color="primary"
            />
          )}
          {service !== 'all' && (
            <Chip 
              label={`Service: ${service}`} 
              size="small" 
              onDelete={() => setService('all')} 
              color="primary"
            />
          )}
        </Box>
      </Box>

      {/* Log Stream */}
      <Box sx={{ flex: 1, overflow: 'auto' }}>
        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
            <CircularProgress size={40} />
          </Box>
        ) : error ? (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Typography variant="body2" color="error" sx={{ mb: 1 }}>
              ⚠️ Error fetching logs
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.6 }}>
              {error}
            </Typography>
          </Box>
        ) : logs.length === 0 ? (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <Typography variant="body2" sx={{ opacity: 0.6 }}>
              No logs found
            </Typography>
          </Box>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
            {logs.map((log, index) => (
              <Paper
                key={index}
                elevation={0}
                sx={{
                  p: 1.5,
                  borderLeft: `3px solid ${getLevelColor(log.level)}`,
                  background: theme.palette.mode === 'dark'
                    ? 'rgba(255, 255, 255, 0.03)'
                    : 'rgba(0, 0, 0, 0.02)',
                  '&:hover': {
                    background: theme.palette.mode === 'dark'
                      ? 'rgba(255, 255, 255, 0.05)'
                      : 'rgba(0, 0, 0, 0.04)',
                  },
                }}
              >
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1 }}>
                  {getLevelIcon(log.level)}
                  <Box sx={{ flex: 1 }}>
                    <Typography 
                      variant="caption" 
                      sx={{ 
                        display: 'block', 
                        opacity: 0.6,
                        mb: 0.5,
                        fontSize: '0.7rem',
                      }}
                    >
                      {log.timestamp.toLocaleString()}
                    </Typography>
                    <Typography 
                      variant="body2" 
                      sx={{ 
                        fontFamily: 'monospace', 
                        fontSize: '0.85rem',
                        wordBreak: 'break-word',
                      }}
                    >
                      {log.message}
                    </Typography>
                  </Box>
                </Box>
              </Paper>
            ))}
          </Box>
        )}
      </Box>
    </Box>
  );
};

export default LogSearchPanel;
