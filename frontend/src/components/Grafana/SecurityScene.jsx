/**
 * 🛡️ SecurityScene - Grafana Scenes Component (Native ESM Integration)
 * 
 * Uses native Grafana Scenes with centralized #grafana-scenes-root container.
 * Leverages ESM bootstrap (scenes.bootstrap.js) for scene initialization.
 * 
 * Features:
 * - Failed Login Attempts (Keycloak)
 * - Suspicious Activity Score
 * - Blocked IP Addresses
 * - Rate Limit Triggers
 * 
 * Architecture:
 * - Mounts to centralized #grafana-scenes-root (no local containerRef)
 * - Uses scenes.bootstrap.js ESM entry point
 * - Boot data guaranteed by inline script in index.html
 */

import React, { useEffect, useState } from 'react';
import { Box, CircularProgress, Alert, Paper, Typography } from '@mui/material';

export const SecurityScene = ({
  height = 600,
  timeRange = { from: 'now-24h', to: 'now' },
}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [scene, setScene] = useState(null);

  useEffect(() => {
    initializeScene();
    
    return () => {
      // Cleanup: Hide scenes container when component unmounts
      const scenesRoot = document.getElementById('grafana-scenes-root');
      if (scenesRoot) {
        scenesRoot.style.display = 'none';
      }
    };
  }, []);

  const initializeScene = async () => {
    try {
      console.log('[SecurityScene] 🚀 Starting native ESM initialization...');
      setLoading(true);
      setError(null);

      // Check if grafanaBootData exists (should be set by inline script)
      if (!window.grafanaBootData) {
        throw new Error('grafanaBootData not initialized. ESM bootstrap may have failed.');
      }
      console.log('[SecurityScene] ✅ grafanaBootData exists');

      // Find centralized scenes container
      const scenesRoot = document.getElementById('grafana-scenes-root');
      if (!scenesRoot) {
        throw new Error('#grafana-scenes-root container not found in DOM');
      }
      console.log('[SecurityScene] ✅ Scenes root container found');

      // Show and prepare container
      scenesRoot.style.display = 'block';
      scenesRoot.style.width = '100%';
      scenesRoot.style.height = `${height}px`;

      // Dynamically import scene creation function from ESM bootstrap
      console.log('[SecurityScene] 📦 Loading scene factory...');
      const { createSecurityScene } = await import('../../scenes/scene-factories');
      
      // Create and mount scene using centralized factory
      console.log('[SecurityScene] 🎨 Creating scene...');
      const scene = await createSecurityScene(scenesRoot, {
        timeRange: {
          from: timeRange.from,
          to: timeRange.to,
        },
      });

      console.log('[SecurityScene] ✅ Scene created and activated:', scene);
      setScene(scene);
      setLoading(false);
      console.log('[SecurityScene] 🎉 Initialization complete!');
    } catch (err) {
      console.error('[SecurityScene] ❌ Initialization failed:', err);
      console.error('[SecurityScene] Error stack:', err.stack);
      setError(err.message);
      setLoading(false);
    }
  };

  if (error) {
    return (
      <Alert severity="error">
        Nepodařilo se načíst security monitoring: {error}
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
    <Paper elevation={0} sx={{ p: 2, height }}>
      <Typography variant="h6" gutterBottom>
        🛡️ Security Monitoring
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
        Scene is rendered in centralized #grafana-scenes-root container
      </Typography>
    </Paper>
  );
};

export default SecurityScene;
