/**
 * 🛡️ SecurityScene - Grafana Scenes Component
 * 
 * Security monitoring with failed logins, suspicious activity, blocked IPs.
 * Replaces GrafanaEmbed iframe in AdminSecurityPage.
 * 
 * Migration:
 * - Before: <GrafanaEmbed dashboardUid="security-events" />
 * - After: <SecurityScene />
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

export const SecurityScene = ({
  height = 600,
  timeRange = { from: 'now-24h', to: 'now' },
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
      console.log('[SecurityScene] 🚀 Starting initialization...');
      setLoading(true);
      setError(null);

      console.log('[SecurityScene] 🔒 Creating BFF datasource...');
      const dataSource = new GrafanaSceneDataSource();
      console.log('[SecurityScene] ✅ DataSource created:', dataSource);

      console.log('[SecurityScene] 🎨 Building scene config...');
      const sceneConfig = {
        $timeRange: new SceneTimeRange({ 
          from: timeRange.from, 
          to: timeRange.to,
        }),
        body: new SceneFlexLayout({
          direction: 'column',
          children: [
            // Row 1: Failed Logins + Suspicious Activity
            new SceneFlexLayout({
              direction: 'row',
              children: [
                new SceneFlexItem({
                  width: '50%',
                  height: 300,
                  body: PanelBuilders.timeseries()
                    .setTitle('🚫 Failed Login Attempts')
                    .setDescription('Authentication failures over time')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'sum(rate(keycloak_failed_login_attempts_total[5m]))',
                      }],
                    })
                    .setMin(0)
                    .build(),
                }),
                new SceneFlexItem({
                  width: '50%',
                  height: 300,
                  body: PanelBuilders.timeseries()
                    .setTitle('⚠️ Suspicious Activity Score')
                    .setDescription('Anomaly detection score')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'sum(security_anomaly_score) by (type)',
                      }],
                    })
                    .setMin(0)
                    .build(),
                }),
              ],
            }),

            // Row 2: Blocked IPs + Rate Limiting
            new SceneFlexLayout({
              direction: 'row',
              children: [
                new SceneFlexItem({
                  width: '50%',
                  height: 300,
                  body: PanelBuilders.table()
                    .setTitle('🚷 Blocked IP Addresses')
                    .setDescription('Currently blocked IPs')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'topk(10, count by (ip_address) (security_blocked_ips))',
                      }],
                    })
                    .build(),
                }),
                new SceneFlexItem({
                  width: '50%',
                  height: 300,
                  body: PanelBuilders.timeseries()
                    .setTitle('🛡️ Rate Limit Triggers')
                    .setDescription('Rate limiting events')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'sum(rate(rate_limit_exceeded_total[5m])) by (endpoint)',
                      }],
                    })
                    .setMin(0)
                    .build(),
                }),
              ],
            }),
          ],
        }),
      };

      console.log('[SecurityScene] 🎨 Creating EmbeddedScene...');
      const newScene = new EmbeddedScene(sceneConfig);
      console.log('[SecurityScene] ✅ Scene created:', newScene);

      if (containerRef.current) {
        console.log('[SecurityScene] 🎬 Activating scene...');
        newScene.activate();
        console.log('[SecurityScene] ✅ Scene activated!');
        setScene(newScene);
        setLoading(false);
        console.log('[SecurityScene] 🎉 Initialization complete!');
      } else {
        console.warn('[SecurityScene] ⚠️  Container ref is null');
        setError('Container not ready');
        setLoading(false);
      }
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

export default SecurityScene;
