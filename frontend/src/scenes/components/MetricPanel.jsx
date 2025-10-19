/**
 * MetricPanel - Custom React component for displaying Prometheus metrics
 * Fetches data from BFF proxy (/api/monitoring/ds/query)
 * 
 * Features:
 * - Dynamic datasource discovery
 * - Threshold-based color coding (green/yellow/red zones)
 * - Light/dark theme support
 * - Auto-refresh with configurable interval
 */

import React, { useEffect, useState } from 'react';
import { Box, Typography, CircularProgress, useTheme } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningIcon from '@mui/icons-material/Warning';
import ErrorIcon from '@mui/icons-material/Error';

export const MetricPanel = ({ 
  title, 
  query, 
  unit = '',
  icon = '📊',
  refreshInterval = 30000, // 30 seconds
  thresholds = null, // { warning: 70, critical: 85 } for percentage-based thresholds
  displayMode = 'value', // 'value' | 'gauge'
}) => {
  const theme = useTheme();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [datasourceUid, setDatasourceUid] = useState(null);

  // Fetch datasource UID on mount
  useEffect(() => {
    const fetchDatasourceUid = async () => {
      try {
        console.log(`[MetricPanel] Fetching datasource UID...`);
        const response = await fetch('/api/monitoring/datasource/prometheus', {
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('token')}`,
          },
        });

        if (!response.ok) {
          throw new Error(`Failed to get datasource: ${response.status}`);
        }

        const dsInfo = await response.json();
        console.log(`[MetricPanel] Datasource info:`, dsInfo);
        setDatasourceUid(dsInfo.uid);
      } catch (err) {
        console.error(`[MetricPanel] Error fetching datasource:`, err);
        setError(`Failed to get datasource: ${err.message}`);
        setLoading(false);
      }
    };

    fetchDatasourceUid();
  }, []);

  useEffect(() => {
    if (!datasourceUid) {
      console.log(`[MetricPanel] Waiting for datasource UID...`);
      return;
    }

    fetchData();
    const interval = setInterval(fetchData, refreshInterval);
    return () => clearInterval(interval);
  }, [query, refreshInterval, datasourceUid]);

  const fetchData = async () => {
    if (!datasourceUid) {
      console.log(`[MetricPanel] No datasource UID available yet`);
      return;
    }

    try {
      console.log(`[MetricPanel] Fetching data for: ${title}`);
      console.log(`[MetricPanel] Query:`, query);
      console.log(`[MetricPanel] Using datasource UID:`, datasourceUid);
      
      const requestBody = {
        queries: [{
          refId: 'A',
          datasource: {
            uid: datasourceUid,
            type: 'prometheus'
          },
          expr: query,
          range: true
        }],
        from: 'now-5m',
        to: 'now'
      };
      
      console.log(`[MetricPanel] Request body:`, JSON.stringify(requestBody, null, 2));
      
      // Call BFF proxy endpoint
      const response = await fetch('/api/monitoring/ds/query', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify(requestBody),
      });

      console.log(`[MetricPanel] Response status:`, response.status, response.statusText);

      if (!response.ok) {
        const errorText = await response.text();
        console.error(`[MetricPanel] Error response body:`, errorText);
        throw new Error(`HTTP ${response.status}: ${errorText || response.statusText}`);
      }

      const result = await response.json();
      console.log(`[MetricPanel] Data received for ${title}:`, result);

      // Extract the latest value from the result
      const latestValue = extractLatestValue(result);
      setData(latestValue);
      setLoading(false);
      setError(null);
    } catch (err) {
      console.error(`[MetricPanel] Error fetching ${title}:`, err);
      setError(err.message);
      setLoading(false);
    }
  };

  const extractLatestValue = (result) => {
    try {
      // Parse Grafana/Prometheus response format
      if (result?.results?.A?.frames?.[0]?.data?.values) {
        const values = result.results.A.frames[0].data.values;
        // Get the last value from the time series
        const valueArray = values[1]; // values[0] is timestamps, values[1] is actual values
        if (valueArray && valueArray.length > 0) {
          return valueArray[valueArray.length - 1];
        }
      }
      
      // Alternative format: direct series data
      if (result?.data?.result?.[0]?.value) {
        return result.data.result[0].value[1]; // [timestamp, value]
      }
      
      return null;
    } catch (err) {
      console.warn(`[MetricPanel] Could not extract value from result:`, err);
      return null;
    }
  };

  const formatValue = (value) => {
    if (value === null || value === undefined) return 'N/A';
    
    const numValue = parseFloat(value);
    if (isNaN(numValue)) return 'N/A';
    
    // Format based on magnitude
    if (numValue >= 1000000) {
      return `${(numValue / 1000000).toFixed(2)}M`;
    } else if (numValue >= 1000) {
      return `${(numValue / 1000).toFixed(2)}K`;
    } else if (numValue >= 100) {
      return numValue.toFixed(0);
    } else if (numValue >= 1) {
      return numValue.toFixed(2);
    } else {
      return numValue.toFixed(3);
    }
  };

  // Get threshold status based on value
  const getThresholdStatus = (value) => {
    if (!thresholds || value === null || value === undefined) {
      return 'normal';
    }

    const numValue = parseFloat(value);
    if (isNaN(numValue)) return 'normal';

    if (thresholds.critical && numValue >= thresholds.critical) {
      return 'critical';
    }
    if (thresholds.warning && numValue >= thresholds.warning) {
      return 'warning';
    }
    return 'ok';
  };

  // Get color based on threshold status
  const getStatusColor = (status) => {
    switch (status) {
      case 'critical':
        return theme.palette.error.main;
      case 'warning':
        return theme.palette.warning.main;
      case 'ok':
        return theme.palette.success.main;
      default:
        return theme.palette.primary.main;
    }
  };

  // Get status icon
  const getStatusIcon = (status) => {
    switch (status) {
      case 'critical':
        return <ErrorIcon sx={{ fontSize: '1.5rem' }} />;
      case 'warning':
        return <WarningIcon sx={{ fontSize: '1.5rem' }} />;
      case 'ok':
        return <CheckCircleIcon sx={{ fontSize: '1.5rem' }} />;
      default:
        return null;
    }
  };

  const thresholdStatus = getThresholdStatus(data);
  const statusColor = getStatusColor(thresholdStatus);

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
        justifyContent: 'space-between',
        transition: 'all 0.3s ease',
        '&:hover': {
          boxShadow: theme.shadows[4],
          borderColor: statusColor,
        },
      }}
    >
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="h6" sx={{ opacity: 0.7 }}>
            {icon}
          </Typography>
          <Typography variant="h6" sx={{ fontWeight: 600 }}>
            {title}
          </Typography>
        </Box>
        
        {/* Status indicator */}
        {!loading && !error && thresholds && (
          <Box sx={{ color: statusColor }}>
            {getStatusIcon(thresholdStatus)}
          </Box>
        )}
      </Box>

      {/* Value */}
      <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', my: 2 }}>
        {loading ? (
          <CircularProgress size={40} sx={{ color: theme.palette.primary.main }} />
        ) : error ? (
          <Box sx={{ textAlign: 'center' }}>
            <Typography variant="body2" color="error" sx={{ mb: 1 }}>
              ⚠️ Error
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.6 }}>
              {error}
            </Typography>
          </Box>
        ) : (
          <Box sx={{ textAlign: 'center', width: '100%' }}>
            <Typography 
              variant="h2" 
              sx={{ 
                fontWeight: 700, 
                fontSize: '3rem',
                color: statusColor,
                transition: 'color 0.3s ease',
              }}
            >
              {formatValue(data)}
            </Typography>
            {unit && (
              <Typography variant="body1" sx={{ opacity: 0.8, mt: 1, color: theme.palette.text.secondary }}>
                {unit}
              </Typography>
            )}
            
            {/* Threshold indicator bar */}
            {thresholds && displayMode === 'value' && (
              <Box sx={{ mt: 2, px: 2 }}>
                <Box
                  sx={{
                    height: 8,
                    borderRadius: 4,
                    background: `linear-gradient(to right, 
                      ${theme.palette.success.main} 0%, 
                      ${theme.palette.success.main} ${thresholds.warning || 70}%, 
                      ${theme.palette.warning.main} ${thresholds.warning || 70}%, 
                      ${theme.palette.warning.main} ${thresholds.critical || 85}%, 
                      ${theme.palette.error.main} ${thresholds.critical || 85}%, 
                      ${theme.palette.error.main} 100%
                    )`,
                    position: 'relative',
                  }}
                >
                  {/* Current value indicator */}
                  <Box
                    sx={{
                      position: 'absolute',
                      left: `${Math.min(parseFloat(data) || 0, 100)}%`,
                      top: '50%',
                      transform: 'translate(-50%, -50%)',
                      width: 16,
                      height: 16,
                      borderRadius: '50%',
                      bgcolor: theme.palette.background.paper,
                      border: `2px solid ${statusColor}`,
                      boxShadow: theme.shadows[4],
                    }}
                  />
                </Box>
                
                {/* Threshold labels */}
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mt: 0.5, px: 0.5 }}>
                  <Typography variant="caption" sx={{ opacity: 0.5, fontSize: '0.65rem' }}>
                    0
                  </Typography>
                  {thresholds.warning && (
                    <Typography variant="caption" sx={{ opacity: 0.5, fontSize: '0.65rem' }}>
                      {thresholds.warning}
                    </Typography>
                  )}
                  {thresholds.critical && (
                    <Typography variant="caption" sx={{ opacity: 0.5, fontSize: '0.65rem' }}>
                      {thresholds.critical}
                    </Typography>
                  )}
                  <Typography variant="caption" sx={{ opacity: 0.5, fontSize: '0.65rem' }}>
                    100
                  </Typography>
                </Box>
              </Box>
            )}
          </Box>
        )}
      </Box>

      {/* Footer - Query info + Status text */}
      <Box>
        {thresholds && !loading && !error && (
          <Typography 
            variant="caption" 
            sx={{ 
              display: 'block',
              mb: 0.5,
              fontWeight: 600,
              color: statusColor,
            }}
          >
            {thresholdStatus === 'ok' && '✓ Normal'}
            {thresholdStatus === 'warning' && '⚠ Warning'}
            {thresholdStatus === 'critical' && '✗ Critical'}
          </Typography>
        )}
        <Typography 
          variant="caption" 
          sx={{ 
            opacity: 0.4, 
            display: 'block',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            fontSize: '0.65rem',
          }}
          title={query}
        >
          {query}
        </Typography>
      </Box>
    </Box>
  );
};

export default MetricPanel;
