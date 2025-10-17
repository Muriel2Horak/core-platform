/**
 * MetricPanel - Custom React component for displaying Prometheus metrics
 * Fetches data from BFF proxy (/api/monitoring/ds/query)
 */

import React, { useEffect, useState } from 'react';
import { Box, Typography, CircularProgress } from '@mui/material';

export const MetricPanel = ({ 
  title, 
  query, 
  unit = '',
  icon = '📊',
  refreshInterval = 30000, // 30 seconds
}) => {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, refreshInterval);
    return () => clearInterval(interval);
  }, [query, refreshInterval]);

  const fetchData = async () => {
    try {
      console.log(`[MetricPanel] Fetching data for: ${title}`);
      console.log(`[MetricPanel] Query:`, query);
      
      const requestBody = {
        queries: [{
          refId: 'A',
          datasourceUid: 'prometheus',
          expr: query,
          range: {
            from: 'now-5m',
            to: 'now',
          },
        }],
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

  return (
    <Box
      sx={{
        width: '100%',
        height: '100%',
        padding: 2,
        background: 'linear-gradient(135deg, #1e1e1e 0%, #2a2a2a 100%)',
        color: '#fff',
        borderRadius: 1,
        border: '1px solid rgba(255, 255, 255, 0.1)',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
      }}
    >
      {/* Header */}
      <Box>
        <Typography variant="caption" sx={{ opacity: 0.7, display: 'block', mb: 0.5 }}>
          {icon}
        </Typography>
        <Typography variant="h6" sx={{ fontWeight: 600, mb: 1 }}>
          {title}
        </Typography>
      </Box>

      {/* Value */}
      <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {loading ? (
          <CircularProgress size={40} sx={{ color: '#90caf9' }} />
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
          <Box sx={{ textAlign: 'center' }}>
            <Typography 
              variant="h2" 
              sx={{ 
                fontWeight: 700, 
                fontSize: '3rem',
                background: 'linear-gradient(135deg, #90caf9 0%, #64b5f6 100%)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
              }}
            >
              {formatValue(data)}
            </Typography>
            {unit && (
              <Typography variant="body1" sx={{ opacity: 0.8, mt: 1 }}>
                {unit}
              </Typography>
            )}
          </Box>
        )}
      </Box>

      {/* Footer - Query info */}
      <Box>
        <Typography 
          variant="caption" 
          sx={{ 
            opacity: 0.5, 
            display: 'block',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
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
