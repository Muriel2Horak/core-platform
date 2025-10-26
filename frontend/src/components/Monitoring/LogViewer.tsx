import React, { useState, useEffect } from 'react';
import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Alert,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  Refresh as RefreshIcon,
  Download as DownloadIcon,
  Search as SearchIcon,
} from '@mui/icons-material';
import axios from 'axios';

interface LogEntry {
  timestamp: string;
  line: string;
  level?: string;
  service?: string;
}

interface LogViewerProps {
  defaultQuery?: string;
  defaultHours?: number;
  showQueryBuilder?: boolean;
}

/**
 * 🔍 LOG VIEWER COMPONENT
 * 
 * Native Loki log viewer with:
 * - LogQL query builder
 * - Time range selection
 * - Real-time refresh
 * - Log level filtering
 * - Export functionality
 */
export const LogViewer: React.FC<LogViewerProps> = ({
  defaultQuery = '{service=~".+"}',
  defaultHours = 1,
  showQueryBuilder = true,
}) => {
  const [logs, setLogs] = useState<LogEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const [query, setQuery] = useState(defaultQuery);
  const [hours, setHours] = useState(defaultHours);
  const [autoRefresh, setAutoRefresh] = useState(false);

  const fetchLogs = async () => {
    setLoading(true);
    setError(null);

    try {
      const response = await axios.get('/api/monitoring/logs', {
        params: { query, hours, limit: 1000 },
      });

      // Parse Loki response
      const parsedLogs: LogEntry[] = [];
      if (response.data.data?.result) {
        response.data.data.result.forEach((stream: any) => {
          const labels = stream.stream || {};
          stream.values?.forEach((entry: any) => {
            parsedLogs.push({
              timestamp: new Date(parseInt(entry[0]) / 1000000).toISOString(),
              line: entry[1],
              level: labels.level || 'info',
              service: labels.service || 'unknown',
            });
          });
        });
      }

      setLogs(parsedLogs);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch logs');
      console.error('Log fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs();
  }, []); // Initial load

  useEffect(() => {
    if (!autoRefresh) return;

    const interval = setInterval(fetchLogs, 30000); // Refresh every 30s
    return () => clearInterval(interval);
  }, [autoRefresh, query, hours]);

  const handleExport = () => {
    const csvContent = [
      'Timestamp,Level,Service,Message',
      ...logs.map(log => 
        `${log.timestamp},${log.level},${log.service},"${log.line.replace(/"/g, '""')}"`
      ),
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `logs-${new Date().toISOString()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const getLevelColor = (level: string): 'error' | 'warning' | 'info' | 'success' => {
    if (level.toLowerCase().includes('error') || level.toLowerCase().includes('fatal')) return 'error';
    if (level.toLowerCase().includes('warn')) return 'warning';
    if (level.toLowerCase().includes('debug') || level.toLowerCase().includes('trace')) return 'info';
    return 'success';
  };

  return (
    <Paper sx={{ p: 3 }} data-testid="log-viewer">
      {/* Query Controls */}
      {showQueryBuilder && (
        <Box sx={{ mb: 3, display: 'flex', gap: 2, flexWrap: 'wrap', alignItems: 'center' }}>
          <TextField
            label="LogQL Query"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            sx={{ flex: 1, minWidth: 300 }}
            size="small"
            placeholder='{service="backend"} |= "error"'
            inputProps={{ 'data-testid': 'log-filter' }}
          />
          
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>Time Range</InputLabel>
            <Select 
              value={hours} 
              onChange={(e) => setHours(Number(e.target.value))}
              inputProps={{ 'data-testid': 'time-range-select' }}
            >
              <MenuItem value={0.25} data-testid="time-quick-15m">Last 15 minutes</MenuItem>
              <MenuItem value={1}>Last 1 hour</MenuItem>
              <MenuItem value={3}>Last 3 hours</MenuItem>
              <MenuItem value={6}>Last 6 hours</MenuItem>
              <MenuItem value={12}>Last 12 hours</MenuItem>
              <MenuItem value={24}>Last 24 hours</MenuItem>
            </Select>
          </FormControl>

          <Button
            variant="contained"
            startIcon={loading ? <CircularProgress size={20} /> : <SearchIcon />}
            onClick={fetchLogs}
            disabled={loading}
            data-testid="apply-filter"
          >
            Query
          </Button>

          <Tooltip title="Auto-refresh every 30s">
            <Button
              variant={autoRefresh ? 'contained' : 'outlined'}
              startIcon={<RefreshIcon />}
              onClick={() => setAutoRefresh(!autoRefresh)}
              data-testid="auto-refresh"
            >
              Auto
            </Button>
          </Tooltip>

          <Tooltip title="Export to CSV">
            <IconButton 
              onClick={handleExport} 
              disabled={logs.length === 0}
              data-testid="export-csv"
            >
              <DownloadIcon />
            </IconButton>
          </Tooltip>
        </Box>
      )}

      {/* Error Alert */}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Results Summary */}
      <Box sx={{ mb: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="body2" color="text.secondary" data-testid="total-logs-count">
          {logs.length} log entries {loading && '(loading...)'}
        </Typography>
        {autoRefresh && (
          <Chip label="Auto-refresh ON" color="primary" size="small" />
        )}
      </Box>

      {/* Log Table */}
      {loading && (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }} data-testid="loading-spinner">
          <CircularProgress />
        </Box>
      )}
      
      {!loading && (
        <TableContainer sx={{ maxHeight: 600 }}>
          <Table stickyHeader size="small">
            <TableHead>
              <TableRow>
                <TableCell width="180">Timestamp</TableCell>
                <TableCell width="80">Level</TableCell>
                <TableCell width="120">Service</TableCell>
                <TableCell>Message</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {logs.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} align="center" sx={{ py: 4 }} data-testid="empty-state">
                    <Typography color="text.secondary">No logs found</Typography>
                    <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                      Try adjusting your time range or query
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
              {logs.map((log, index) => (
                <TableRow key={index} hover data-testid="log-row">
                  <TableCell>
                    <Typography variant="caption" fontFamily="monospace">
                      {new Date(log.timestamp).toLocaleString()}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Chip 
                      label={log.level?.toUpperCase() || 'INFO'} 
                      color={getLevelColor(log.level || 'info')}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="caption">{log.service}</Typography>
                  </TableCell>
                  <TableCell>
                    <Typography
                      variant="body2"
                      fontFamily="monospace"
                      sx={{
                        whiteSpace: 'pre-wrap',
                        wordBreak: 'break-word',
                        maxWidth: 800,
                      }}
                    >
                      {log.line}
                    </Typography>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Paper>
  );
};
