/**
 * 💻 SystemResourcesSceneWrapper - System Resources Dashboard (USE Method)
 * 
 * Wraps scene-monitoring-system.js for React integration
 * Monitors: CPU, Memory, Disk, Network
 * Method: USE (Utilization, Saturation, Errors)
 */

import React, { useEffect, useState, useRef } from 'react';
import { Box, CircularProgress, Alert } from '@mui/material';

export const SystemResourcesSceneWrapper = ({
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
        console.error('[SystemResourcesSceneWrapper] ❌ Container ref is still null after timeout');
        setError('Container ref not available');
        setLoading(false);
      }
    }, 0);
    
    return () => {
      clearTimeout(timer);
      if (scene) {
        // Cleanup scene if needed
        console.log('[SystemResourcesSceneWrapper] Cleaning up scene');
      }
    };
  }, []);

  const initializeScene = async () => {
    try {
      console.log('[SystemResourcesSceneWrapper] 🚀 Starting scene initialization...');
      setLoading(true);
      setError(null);

      // Get container element from ref (should be available now)
      const container = containerRef.current;
      if (!container) {
        throw new Error('Container element not found');
      }

      console.log('[SystemResourcesSceneWrapper] 📦 Container element found:', container);

      // Dynamically import scene creation function
      const { createSystemResourcesScene } = await import('../../scenes/scene-monitoring-system');
      
      // Create scene and mount it
      console.log('[SystemResourcesSceneWrapper] 🎨 Creating system resources scene...');
      const sceneInstance = await createSystemResourcesScene(container, { timeRange });

      console.log('[SystemResourcesSceneWrapper] ✅ Scene created and activated');
      setScene(sceneInstance);
      setLoading(false);
    } catch (err) {
      console.error('[SystemResourcesSceneWrapper] ❌ Initialization failed:', err);
      setError(err.message);
      setLoading(false);
    }
  };

  if (error) {
    return (
      <Alert severity="error">
        Nepodařilo se načíst System Resources dashboard: {error}
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

export default SystemResourcesSceneWrapper;
