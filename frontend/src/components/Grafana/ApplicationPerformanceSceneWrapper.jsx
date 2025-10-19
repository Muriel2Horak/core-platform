/**
 * 🚀 ApplicationPerformanceSceneWrapper - Application Performance Dashboard (RED Method)
 * 
 * Wraps scene-monitoring-app.js for React integration
 * Monitors: Request Rate, Error Rate, Duration
 * Method: RED (Rate, Errors, Duration)
 */

import React, { useEffect, useState, useRef } from 'react';
import { Box, CircularProgress, Alert } from '@mui/material';

export const ApplicationPerformanceSceneWrapper = ({
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
        console.error('[ApplicationPerformanceSceneWrapper] ❌ Container ref is still null after timeout');
        setError('Container ref not available');
        setLoading(false);
      }
    }, 0);
    
    return () => {
      clearTimeout(timer);
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

      // Get container element from ref (should be available now)
      const container = containerRef.current;
      if (!container) {
        throw new Error('Container element not found');
      }

      console.log('[ApplicationPerformanceSceneWrapper] 📦 Container element found:', container);

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
        }}
      >
        {/* Render Grafana Scene as React component */}
        {scene && <scene.Component model={scene} />}
      </Box>
    </Box>
  );
};

export default ApplicationPerformanceSceneWrapper;
