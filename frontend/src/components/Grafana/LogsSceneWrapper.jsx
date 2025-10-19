/**
 * 📋 LogsSceneWrapper - Logs Dashboard (Loki Integration)
 * 
 * Wraps scene-monitoring-logs.js for React integration
 * Features: Log search, filtering, aggregation
 * Source: Loki datasource
 */

import React, { useEffect, useState, useRef } from 'react';
import { Box, CircularProgress, Alert } from '@mui/material';

export const LogsSceneWrapper = ({
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
        console.error('[LogsSceneWrapper] ❌ Container ref is still null after timeout');
        setError('Container ref not available');
        setLoading(false);
      }
    }, 0);
    
    return () => {
      clearTimeout(timer);
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

      // Get container element from ref (should be available now)
      const container = containerRef.current;
      if (!container) {
        throw new Error('Container element not found');
      }

      console.log('[LogsSceneWrapper] 📦 Container element found:', container);

      // Dynamically import scene creation function
      const { createLogsScene } = await import('../../scenes/scene-monitoring-logs');
      
      // Create scene and mount it
      console.log('[LogsSceneWrapper] 🎨 Creating logs scene...');
      const sceneInstance = await createLogsScene(container, { timeRange });

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

  return (
    <Box sx={{ width: '100%', minHeight: height, position: 'relative' }}>
      {/* Loading overlay */}
      {loading && (
        <Box 
          display="flex" 
          justifyContent="center" 
          alignItems="center" 
          sx={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            zIndex: 10,
            backgroundColor: 'rgba(255, 255, 255, 0.8)',
          }}
        >
          <CircularProgress />
        </Box>
      )}
      
      {/* Scene container - ALWAYS in DOM */}
      <Box 
        ref={containerRef}
        sx={{ 
          width: '100%',
          minHeight: height,
          position: 'relative',
          '& .grafana-scene': {
            width: '100%',
            height: '100%',
          },
        }} 
      />
    </Box>
  );
};
