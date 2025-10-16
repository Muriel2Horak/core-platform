/**
 * 📋 AuditScene - Grafana Scenes Component (Native ESM Integration)
 * 
 * Uses native Grafana Scenes with centralized #grafana-scenes-root container.
 * Leverages ESM bootstrap (scenes.bootstrap.js) for scene initialization.
 * 
 * Features:
 * - Audit Events Timeline
 * - Active Users Today (stat)
 * - Recent Audit Logs (table)
 * 
 * Architecture:
 * - Mounts to centralized #grafana-scenes-root (no local containerRef)
 * - Uses scenes.bootstrap.js ESM entry point
 * - Boot data guaranteed by inline script in index.html
 */

import React, { useEffect, useState, useRef } from 'react';
import { Box, CircularProgress, Alert } from '@mui/material';

export const AuditScene = ({
  height = 700,
  timeRange = { from: 'now-7d', to: 'now' },
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
      console.log('[AuditScene] 🚀 Starting native ESM initialization...');
      setLoading(true);
      setError(null);

      // Check if grafanaBootData exists (should be set by inline script)
      if (!window.grafanaBootData) {
        throw new Error('grafanaBootData not initialized. ESM bootstrap may have failed.');
      }
      console.log('[AuditScene] ✅ grafanaBootData exists');

      // Find centralized scenes container
      const scenesRoot = document.getElementById('grafana-scenes-root');
      if (!scenesRoot) {
        throw new Error('#grafana-scenes-root container not found in DOM');
      }
      console.log('[AuditScene] ✅ Scenes root container found');

      // Show and prepare container
      scenesRoot.style.display = 'block';
      scenesRoot.style.width = '100%';
      scenesRoot.style.height = `${height}px`;

      // Dynamically import scene creation function from ESM bootstrap
      console.log('[AuditScene] 📦 Loading scene factory...');
      const { createAuditScene } = await import('../../scenes/scene-factories');
      
      // Create and mount scene using centralized factory
      console.log('[AuditScene] 🎨 Creating scene...');
      const scene = await createAuditScene(scenesRoot, {
        timeRange: {
          from: timeRange.from,
          to: timeRange.to,
        },
      });

      console.log('[AuditScene] ✅ Scene created and activated:', scene);
      setScene(scene);
      setLoading(false);
      console.log('[AuditScene] 🎉 Initialization complete!');
    } catch (err) {
      console.error('[AuditScene] ❌ Initialization failed:', err);
      console.error('[AuditScene] Error stack:', err.stack);
      setError(err.message);
      setLoading(false);
    }
  };

  if (error) {
    return (
      <Alert severity="error">
        Nepodařilo se načíst audit monitoring: {error}
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

export default AuditScene;
