/**
 * ⚡ ApplicationPerformanceSceneWrapper - Application Performance Dashboard (RED Method)
 * 
 * Wraps scene-monitoring-app.js for React integration
 * Monitors: Request Rate, Error Rate, Response Time, Active Connections
 * Method: RED (Rate, Errors, Duration)
 */

import React, { useEffect, useState } from 'react';
import { Box, CircularProgress, Alert } from '@mui/material';

export const ApplicationPerformanceSceneWrapper = ({
  height = 1200,
  timeRange = { from: 'now-1h', to: 'now' },
}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [scene, setScene] = useState(null);

  useEffect(() => {
    initializeScene();
    
    return () => {
      if (scene) {
        console.log('[ApplicationPerformanceSceneWrapper] Cleaning up scene');
      }
    };
  }, []);

  const initializeScene = async () => {
    try {
      console.log('[ApplicationPerformanceSceneWrapper] 🚀 Starting scene initialization...');
      setLoading(true);
      setError(null);

      // Get container element
      const container = document.getElementById('app-performance-scene-root');
      if (!container) {
        throw new Error('Container element not found');
      }

      // Dynamically import scene creation function
      const { createApplicationPerformanceScene } = await import('../../scenes/scene-monitoring-app');
      
      // Create scene and mount it
      console.log('[ApplicationPerformanceSceneWrapper] 🎨 Creating application performance scene...');
      const sceneInstance = await createApplicationPerformanceScene(container, { timeRange });

      console.log('[ApplicationPerformanceSceneWrapper] ✅ Scene created and activated');
      setScene(sceneInstance);
      setLoading(false);
    } catch (err) {
      console.error('[ApplicationPerformanceSceneWrapper] ❌ Initialization failed:', err);
      setError(err.message);
      setLoading(false);
    }
  };

  if (error) {
    return (
      <Alert severity="error">
        Nepodařilo se načíst Application Performance dashboard: {error}
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
      id="app-performance-scene-root"
      sx={{ 
        width: '100%',
        minHeight: height,
        position: 'relative',
      }} 
    />
  );
};

export default ApplicationPerformanceSceneWrapper;
