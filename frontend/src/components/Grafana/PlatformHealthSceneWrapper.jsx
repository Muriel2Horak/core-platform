/**
 * 🏥 PlatformHealthSceneWrapper - Platform Health Dashboard (SLI/SLO)
 * 
 * Wraps scene-monitoring-health.js for React integration
 * Monitors: Database, Kafka, Circuit Breakers
 * Method: SLI/SLO (Service Level Indicators/Objectives)
 */

import React, { useEffect, useState, useRef } from 'react';
import { Box, CircularProgress, Alert } from '@mui/material';

export const PlatformHealthSceneWrapper = ({
  height = 1200,
  timeRange = { from: 'now-1h', to: 'now' },
}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [scene, setScene] = useState(null);
  const containerRef = useRef(null);

  useEffect(() => {
    // Wait for ref to be attached, then initialize
    const timer = setTimeout(() => {
      if (containerRef.current) {
        initializeScene();
      } else {
        console.error('[PlatformHealthSceneWrapper] ❌ Container ref is still null after timeout');
        setError('Container ref not available');
        setLoading(false);
      }
    }, 0);
    
    return () => {
      clearTimeout(timer);
      if (scene) {
        console.log('[PlatformHealthSceneWrapper] Cleaning up scene');
      }
    };
  }, []);

  const initializeScene = async () => {
    try {
      console.log('[PlatformHealthSceneWrapper] 🚀 Starting scene initialization...');
      setLoading(true);
      setError(null);

      // Get container element from ref (should be available now)
      const container = containerRef.current;
      if (!container) {
        throw new Error('Container element not found');
      }

      console.log('[PlatformHealthSceneWrapper] 📦 Container element found:', container);

      // Dynamically import scene creation function
      const { createPlatformHealthScene } = await import('../../scenes/scene-monitoring-health');
      
      // Create scene and mount it
      console.log('[PlatformHealthSceneWrapper] 🎨 Creating platform health scene...');
      const sceneInstance = await createPlatformHealthScene(container, { timeRange });

      console.log('[PlatformHealthSceneWrapper] ✅ Scene created and activated');
      setScene(sceneInstance);
      setLoading(false);
    } catch (err) {
      console.error('[PlatformHealthSceneWrapper] ❌ Initialization failed:', err);
      setError(err.message);
      setLoading(false);
    }
  };

  if (error) {
    return (
      <Alert severity="error">
        Nepodařilo se načíst Platform Health dashboard: {error}
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
      ref={containerRef}
      sx={{ 
        width: '100%',
        minHeight: height,
        position: 'relative',
      }} 
    />
  );
};

export default PlatformHealthSceneWrapper;
