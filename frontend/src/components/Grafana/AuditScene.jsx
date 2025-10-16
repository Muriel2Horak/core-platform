/**
 * 📋 AuditScene - Grafana Scenes Component
 * 
 * Audit log monitoring with user actions, system changes, compliance events.
 * Replaces GrafanaEmbed iframe in AdminAuditPage.
 * 
 * Migration:
 * - Before: <GrafanaEmbed dashboardUid="audit-logs" />
 * - After: <AuditScene />
 */

import React, { useEffect, useRef, useState } from 'react';
import { Box, CircularProgress, Alert, Paper, Typography } from '@mui/material';
import { 
  EmbeddedScene, 
  SceneTimeRange, 
  SceneFlexLayout, 
  SceneFlexItem, 
  PanelBuilders 
} from '@grafana/scenes';
import { GrafanaSceneDataSource } from '../../services/grafanaSceneDataSource';

export const AuditScene = ({
  height = 700,
  timeRange = { from: 'now-7d', to: 'now' },
}) => {
  const containerRef = useRef(null);
  const [scene, setScene] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    initializeScene();
    
    return () => {
      // Cleanup handled by React
    };
  }, []);

  const initializeScene = async () => {
    try {
      console.log('[AuditScene] 🚀 Starting initialization...');
      console.log('[AuditScene] � Checking grafanaBootData:', window.grafanaBootData ? '✅ EXISTS' : '❌ MISSING');
      setLoading(true);
      setError(null);

      console.log('[AuditScene] 📝 Creating BFF datasource...');
      const dataSource = new GrafanaSceneDataSource();
      console.log('[AuditScene] ✅ DataSource created:', dataSource);

      console.log('[AuditScene] 🎨 Building scene config...');
      const sceneConfig = {
        $timeRange: new SceneTimeRange({ 
          from: timeRange.from, 
          to: timeRange.to,
        }),
        body: new SceneFlexLayout({
          direction: 'column',
          children: [
            // Row 1: Audit Events Timeline + User Actions
            new SceneFlexLayout({
              direction: 'row',
              children: [
                new SceneFlexItem({
                  width: '60%',
                  height: 250,
                  body: PanelBuilders.timeseries()
                    .setTitle('📊 Audit Events Timeline')
                    .setDescription('All audit events over time')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'sum(rate(audit_events_total[5m])) by (event_type)',
                      }],
                    })
                    .setMin(0)
                    .build(),
                }),
                new SceneFlexItem({
                  width: '40%',
                  height: 250,
                  body: PanelBuilders.stat()
                    .setTitle('👥 Active Users Today')
                    .setDescription('Unique users with audit events')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'count(count by (user_id) (audit_events_total{timestamp>now()-24h}))',
                      }],
                    })
                    .setThresholds({
                      mode: 'absolute',
                      steps: [
                        { value: 0, color: 'green' },
                        { value: 100, color: 'yellow' },
                        { value: 500, color: 'red' },
                      ],
                    })
                    .build(),
                }),
              ],
            }),

            // Row 2: Recent Audit Logs Table
            new SceneFlexLayout({
              direction: 'row',
              children: [
                new SceneFlexItem({
                  width: '100%',
                  height: 400,
                  body: PanelBuilders.table()
                    .setTitle('📝 Recent Audit Logs')
                    .setDescription('Latest 100 audit events')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'topk(100, audit_log_entries)',
                      }],
                    })
                    .build(),
                }),
              ],
            }),
          ],
        }),
      };

      console.log('[AuditScene] 🎨 Creating EmbeddedScene...');
      const newScene = new EmbeddedScene(sceneConfig);
      console.log('[AuditScene] ✅ Scene created:', newScene);

      if (containerRef.current) {
        console.log('[AuditScene] 🎬 Activating scene...');
        newScene.activate();
        console.log('[AuditScene] ✅ Scene activated!');
        setScene(newScene);
        setLoading(false);
        console.log('[AuditScene] 🎉 Initialization complete!');
      } else {
        console.warn('[AuditScene] ⚠️  Container ref is null');
        setError('Container not ready');
        setLoading(false);
      }
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

  return (
    <Paper elevation={0} sx={{ p: 2, height }}>
      <Typography variant="h6" gutterBottom>
        📋 Audit Log Monitoring
      </Typography>
      <Box 
        ref={containerRef} 
        sx={{ 
          height: height - 60,
          width: '100%',
          border: '1px solid',
          borderColor: 'divider',
          borderRadius: 1,
        }} 
      />
    </Paper>
  );
};

export default AuditScene;
