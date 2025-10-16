/**
 * 📊 SystemMonitoringScene - Grafana Scenes Component
 * 
 * Replaces GrafanaEmbed iframes with native Grafana Scenes for system monitoring.
 * Uses BFF proxy (/api/monitoring/*) for secure datasource queries.
 * 
 * Features:
 * - CPU, Memory, HTTP metrics
 * - Kafka message rates
 * - PostgreSQL connections
 * - Error rates and response times
 * 
 * Migration:
 * - Before: <GrafanaEmbed dashboardUid="system-metrics" panelId={1} />
 * - After: <SystemMonitoringScene />
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

export const SystemMonitoringScene = ({
  height = 800,
  timeRange = { from: 'now-6h', to: 'now' },
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
      console.log('[SystemMonitoringScene] 🚀 Starting initialization...');
      setLoading(true);
      setError(null);

      // Create BFF-proxied datasource (not used directly in setData, but available)
      console.log('[SystemMonitoringScene] 📊 Creating BFF datasource...');
      const dataSource = new GrafanaSceneDataSource();
      console.log('[SystemMonitoringScene] ✅ DataSource created:', dataSource);

      // Define scene with 7 panels (matching MonitoringPage)
      const sceneConfig = {
        $timeRange: new SceneTimeRange({ 
          from: timeRange.from, 
          to: timeRange.to,
        }),
        body: new SceneFlexLayout({
          direction: 'column',
          children: [
            // Row 1: CPU + Memory
            new SceneFlexLayout({
              direction: 'row',
              children: [
                new SceneFlexItem({
                  width: '50%',
                  height: 250,
                  body: PanelBuilders.timeseries()
                    .setTitle('💻 CPU Usage (%)')
                    .setDescription('System CPU utilization')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: '100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)',
                      }],
                    })
                    .setMin(0)
                    .setMax(100)
                    .setUnit('percent')
                    .build(),
                }),
                new SceneFlexItem({
                  width: '50%',
                  height: 250,
                  body: PanelBuilders.timeseries()
                    .setTitle('🧠 Memory Usage (%)')
                    .setDescription('System memory utilization')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: '100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes))',
                      }],
                    })
                    .setMin(0)
                    .setMax(100)
                    .setUnit('percent')
                    .build(),
                }),
              ],
            }),

            // Row 2: HTTP Requests + Kafka Messages
            new SceneFlexLayout({
              direction: 'row',
              children: [
                new SceneFlexItem({
                  width: '50%',
                  height: 250,
                  body: PanelBuilders.timeseries()
                    .setTitle('🌐 HTTP Requests (req/s)')
                    .setDescription('HTTP request rate')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'sum(rate(http_server_requests_seconds_count[5m]))',
                      }],
                    })
                    .setMin(0)
                    .build(),
                }),
                new SceneFlexItem({
                  width: '50%',
                  height: 250,
                  body: PanelBuilders.timeseries()
                    .setTitle('📨 Kafka Messages (msg/s)')
                    .setDescription('Kafka message throughput')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'sum(rate(kafka_server_brokertopicmetrics_messagesin_total[5m]))',
                      }],
                    })
                    .setMin(0)
                    .build(),
                }),
              ],
            }),

            // Row 3: PostgreSQL + Error Rate + Response Time
            new SceneFlexLayout({
              direction: 'row',
              children: [
                new SceneFlexItem({
                  width: '33%',
                  height: 250,
                  body: PanelBuilders.timeseries()
                    .setTitle('🗄️ PostgreSQL Connections')
                    .setDescription('Active database connections')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'pg_stat_database_numbackends{datname!="template0",datname!="template1"}',
                      }],
                    })
                    .setMin(0)
                    .build(),
                }),
                new SceneFlexItem({
                  width: '33%',
                  height: 250,
                  body: PanelBuilders.timeseries()
                    .setTitle('⚠️ Error Rate (%)')
                    .setDescription('HTTP 5xx error rate')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: '100 * sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))',
                      }],
                    })
                    .setMin(0)
                    .setMax(100)
                    .setUnit('percent')
                    .build(),
                }),
                new SceneFlexItem({
                  width: '33%',
                  height: 250,
                  body: PanelBuilders.timeseries()
                    .setTitle('⏱️ Response Time (P95)')
                    .setDescription('95th percentile response time')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))',
                      }],
                    })
                    .setMin(0)
                    .setUnit('s')
                    .build(),
                }),
              ],
            }),
          ],
        }),
      };

      console.log('[SystemMonitoringScene] 🎨 Creating EmbeddedScene with config...');
      const newScene = new EmbeddedScene(sceneConfig);
      console.log('[SystemMonitoringScene] ✅ Scene created:', newScene);

      if (containerRef.current) {
        console.log('[SystemMonitoringScene] 🎬 Activating scene...');
        newScene.activate();
        console.log('[SystemMonitoringScene] ✅ Scene activated successfully!');
        setScene(newScene);
        setLoading(false);
        console.log('[SystemMonitoringScene] 🎉 Initialization complete!');
      } else {
        console.warn('[SystemMonitoringScene] ⚠️  Container ref is null, cannot activate scene');
        setError('Container not ready');
        setLoading(false);
      }
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

  return (
    <Paper elevation={0} sx={{ p: 2, height }}>
      <Typography variant="h6" gutterBottom>
        📊 System Monitoring
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

export default SystemMonitoringScene;
