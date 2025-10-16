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

import React, { useEffect, useState, useRef } from 'react';
import { Box, CircularProgress, Alert } from '@mui/material';

export const SecurityScene = ({
  height = 600,
  timeRange = { from: 'now-24h', to: 'now' },
}) => {
  const containerRef = useRef(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [scene, setScene] = useState(null);

  useEffect(() => {
    // Mount #grafana-scenes-root into this component's container
    if (containerRef.current) {
      const scenesRoot = document.getElementById('grafana-scenes-root');
      if (scenesRoot && !containerRef.current.contains(scenesRoot)) {
        containerRef.current.appendChild(scenesRoot);
      }
    }
    
    initializeScene();
    
    return () => {
      // Cleanup: Move scenes container back to body and hide it
      const scenesRoot = document.getElementById('grafana-scenes-root');
      if (scenesRoot) {
        scenesRoot.style.display = 'none';
        document.body.appendChild(scenesRoot);
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

  // Container that will hold the #grafana-scenes-root div
  // Scene is mounted by moving the global container into this ref
  return (
    <Box 
      ref={containerRef}
      sx={{ 
        width: '100%',
        height: height,
        position: 'relative',
        overflow: 'auto',
      }} 
    />
  );
};

export default SecurityScene;
