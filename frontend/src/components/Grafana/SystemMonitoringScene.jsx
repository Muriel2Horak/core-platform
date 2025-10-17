/**
 * 📊 SystemMonitoringScene - Grafana Scenes Component (Native ESM Integration)
 * 
 * Uses native Grafana Scenes with centralized #grafana-scenes-root container.
 * Leverages ESM bootstrap (scenes.bootstrap.js) for scene initialization.
 * BFF proxy (/api/monitoring/*) handles secure datasource queries.
 * 
 * Features:
 * - CPU, Memory, HTTP metrics
 * - Kafka message rates
 * - PostgreSQL connections
 * - Error rates and response times
 * 
 * Architecture:
 * - Mounts to centralized #grafana-scenes-root (no local containerRef)
 * - Uses scenes.bootstrap.js ESM entry point
 * - Boot data guaranteed by inline script in index.html
 */

import React, { useEffect, useState, useRef } from 'react';
import { Box, CircularProgress, Alert } from '@mui/material';

export const SystemMonitoringScene = ({
  height = 800,
  timeRange = { from: 'now-6h', to: 'now' },
}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [scene, setScene] = useState(null);

  useEffect(() => {
    initializeScene();
  }, []);

  const initializeScene = async () => {
    try {
      console.log('[SystemMonitoringScene] 🚀 Starting scene initialization...');
      setLoading(true);
      setError(null);

      // Dynamically import scene creation function
      console.log('[SystemMonitoringScene] 📦 Loading scene factory...');
      
      // Load native monitoring scene (NO plugins, NO datasources)
      const { createSystemMonitoringScene } = await import('../../scenes/scene-monitoring-native');
      
      // Create scene (no container needed - will render via React)
      console.log('[SystemMonitoringScene] 🎨 Creating monitoring scene with native components...');
      const sceneInstance = await createSystemMonitoringScene(null, { timeRange });

      console.log('[SystemMonitoringScene] ✅ Scene created and activated');
      
      // Store the entire scene instance
      setScene(sceneInstance);
      setLoading(false);
      console.log('[SystemMonitoringScene] 🎉 Initialization complete!');
    } catch (err) {
      console.error('[SystemMonitoringScene] ❌ Initialization failed:', err);
      console.error('[SystemMonitoringScene] Error stack:', err.stack);
      setError(err.message);
      setLoading(false);
    }
  };

  if (error) {
    return (
      <Alert severity="error">
        Nepodařilo se načíst monitoring: {error}
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

  if (!scene) {
    return (
      <Alert severity="warning">
        Scene not available
      </Alert>
    );
  }

  // Render the Grafana Scene using its React Component
  // Pass the scene itself as the model prop
  const SceneComponent = scene.Component;
  
  return (
    <Box 
      data-testid="grafana-scene-system-monitoring"
      sx={{ 
        width: '100%',
        height: height,
        position: 'relative',
        overflow: 'auto',
      }} 
    >
      <SceneComponent model={scene} />
    </Box>
  );
};

export default SystemMonitoringScene;
