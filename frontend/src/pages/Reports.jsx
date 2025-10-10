import React, { useEffect, useRef, useState } from 'react';
import { Box, Typography, Alert, CircularProgress, Tab, Tabs, Button } from '@mui/material';
import { Assessment, OpenInNew } from '@mui/icons-material';
import { EmbeddedScene, SceneTimeRange, SceneFlexLayout, SceneFlexItem, PanelBuilders } from '@grafana/scenes';
import { GrafanaSceneDataSource } from '../services/grafanaSceneDataSource';

/**
 * Reports Page with Grafana Scenes Integration
 * Uses BFF proxy (/api/monitoring/*) for secure datasource queries
 * NO tokens exposed to browser
 */
export default function Reports() {
  const containerRef = useRef(null);
  const [scene, setScene] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [activeTab, setActiveTab] = useState(0);

  useEffect(() => {
    initializeScene();
    
    return () => {
      if (scene) {
        scene.deactivate();
      }
    };
  }, [activeTab]);

  const initializeScene = async () => {
    try {
      setLoading(true);
      setError(null);

      // Create custom datasource that uses our BFF proxy
      const dataSource = new GrafanaSceneDataSource();

      // Define scene based on active tab
      let sceneConfig;
      
      if (activeTab === 0) {
        // Application Overview
        sceneConfig = createApplicationOverviewScene(dataSource);
      } else if (activeTab === 1) {
        // Infrastructure Metrics
        sceneConfig = createInfrastructureScene(dataSource);
      } else {
        // Logs & Tracing
        sceneConfig = createLogsScene(dataSource);
      }

      const newScene = new EmbeddedScene(sceneConfig);

      if (containerRef.current) {
        newScene.activate();
        const unsub = newScene.subscribe(() => {
          // Handle scene updates
        });
        
        setScene(newScene);
        setLoading(false);

        return () => unsub();
      }
    } catch (err) {
      console.error('Failed to initialize Grafana scene:', err);
      setError(err.message);
      setLoading(false);
    }
  };

  const createApplicationOverviewScene = (dataSource) => ({
    $timeRange: new SceneTimeRange({ from: 'now-6h', to: 'now' }),
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        new SceneFlexItem({
          height: 200,
          body: PanelBuilders.timeseries()
            .setTitle('HTTP Request Rate')
            .setData({
              datasource: dataSource,
              queries: [{
                refId: 'A',
                expr: 'rate(http_server_requests_total[5m])',
              }],
            })
            .build(),
        }),
        new SceneFlexItem({
          height: 200,
          body: PanelBuilders.timeseries()
            .setTitle('Response Time (P95)')
            .setData({
              datasource: dataSource,
              queries: [{
                refId: 'A',
                expr: 'histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))',
              }],
            })
            .build(),
        }),
        new SceneFlexItem({
          height: 200,
          body: PanelBuilders.timeseries()
            .setTitle('Active Users (WebSocket)')
            .setData({
              datasource: dataSource,
              queries: [{
                refId: 'A',
                expr: 'websocket_active_connections',
              }],
            })
            .build(),
        }),
      ],
    }),
  });

  const createInfrastructureScene = (dataSource) => ({
    $timeRange: new SceneTimeRange({ from: 'now-1h', to: 'now' }),
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        new SceneFlexItem({
          height: 200,
          body: PanelBuilders.timeseries()
            .setTitle('Database Connection Pool')
            .setData({
              datasource: dataSource,
              queries: [{
                refId: 'A',
                expr: 'hikaricp_connections_active',
              }],
            })
            .build(),
        }),
        new SceneFlexItem({
          height: 200,
          body: PanelBuilders.timeseries()
            .setTitle('Redis Operations')
            .setData({
              datasource: dataSource,
              queries: [{
                refId: 'A',
                expr: 'rate(redis_commands_total[5m])',
              }],
            })
            .build(),
        }),
      ],
    }),
  });

  const createLogsScene = (dataSource) => ({
    $timeRange: new SceneTimeRange({ from: 'now-30m', to: 'now' }),
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        new SceneFlexItem({
          height: 400,
          body: PanelBuilders.logs()
            .setTitle('Application Logs')
            .setData({
              datasource: dataSource,
              queries: [{
                refId: 'A',
                expr: '{container="backend"} | json',
              }],
            })
            .build(),
        }),
      ],
    }),
  });

  const handleTabChange = (_, newValue) => {
    setActiveTab(newValue);
  };

  const openFullGrafana = () => {
    window.open(`https://admin.${window.location.hostname.split('.').slice(-2).join('.')}/monitoring/`, '_blank');
  };

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">
          Failed to load monitoring dashboard: {error}
        </Alert>
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3, height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Box display="flex" alignItems="center" gap={2}>
          <Assessment fontSize="large" color="primary" />
          <Box>
            <Typography variant="h4">Reporting & Analytics</Typography>
            <Typography variant="body2" color="text.secondary">
              Real-time metrics, logs and performance dashboards
            </Typography>
          </Box>
        </Box>
        <Button
          variant="outlined"
          startIcon={<OpenInNew />}
          onClick={openFullGrafana}
        >
          Open in Grafana
        </Button>
      </Box>

      {/* Tabs */}
      <Tabs value={activeTab} onChange={handleTabChange} sx={{ mb: 2 }}>
        <Tab label="📊 Application" />
        <Tab label="🔧 Infrastructure" />
        <Tab label="📋 Logs" />
      </Tabs>

      {/* Scene Container */}
      {loading ? (
        <Box display="flex" justifyContent="center" alignItems="center" height={400}>
          <CircularProgress />
        </Box>
      ) : (
        <Box
          ref={containerRef}
          sx={{
            flex: 1,
            width: '100%',
            height: '100%',
            border: '1px solid',
            borderColor: 'divider',
            borderRadius: 1,
            overflow: 'auto',
          }}
        />
      )}
    </Box>
  );
}
