/**
 * 📝 LogsSceneWrapper - Logs Dashboard (Loki)
 * 
 * Wraps scene-monitoring-logs.js for React integration
 * Features: Real-time log search, filtering, log volume metrics
 * Datasource: Loki
 */

import React, { useEffect, useState } from 'react';
import { Box, CircularProgress, Alert } from '@mui/material';

export const LogsSceneWrapper = ({
  height = 900,
  timeRange = { from: 'now-1h', to: 'now' },
}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [scene, setScene] = useState(null);

  useEffect(() => {
    initializeScene();
    
    return () => {
      if (scene) {
        console.log('[LogsSceneWrapper] Cleaning up scene');
      }
    };
  }, []);

  const initializeScene = async () => {
    try {
      console.log('[LogsSceneWrapper] 🚀 Starting scene initialization...');
      setLoading(true);
      setError(null);

      // Dynamically import scene creation function
      const { createLogsScene } = await import('../../scenes/scene-monitoring-logs');
      
      // Create scene
      console.log('[LogsSceneWrapper] 🎨 Creating logs scene...');
      const sceneInstance = await createLogsScene(null, { timeRange });

      console.log('[LogsSceneWrapper] ✅ Scene created and activated');
      setScene(sceneInstance);
      setLoading(false);
    } catch (err) {
      console.error('[LogsSceneWrapper] ❌ Initialization failed:', err);
      setError(err.message);
      setLoading(false);
    }
  };

  if (error) {
    return (
      <Alert severity="error">
        Nepodařilo se načíst Logs dashboard: {error}
      </Alert>
    );
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height={height}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box 
      sx={{ 
        width: '100%',
        minHeight: height,
        position: 'relative',
      }} 
    />
  );
};

export default LogsSceneWrapper;
