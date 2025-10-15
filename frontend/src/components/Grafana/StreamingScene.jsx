/**
 * 🚀 StreamingScene - Grafana Scenes Component
 * 
 * Kafka/streaming monitoring with throughput, lag, consumer groups.
 * Replaces 3 GrafanaEmbed iframes in StreamingDashboardPage.
 * 
 * Migration:
 * - Before: 3x <GrafanaEmbed dashboardUid="..." />
 * - After: <StreamingScene />
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

export const StreamingScene = ({
  height = 900,
  timeRange = { from: 'now-1h', to: 'now' },
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
      setLoading(true);
      setError(null);

      const dataSource = new GrafanaSceneDataSource();

      const sceneConfig = {
        $timeRange: new SceneTimeRange({ 
          from: timeRange.from, 
          to: timeRange.to,
        }),
        body: new SceneFlexLayout({
          direction: 'column',
          children: [
            // Row 1: Kafka Throughput + Consumer Lag
            new SceneFlexLayout({
              direction: 'row',
              children: [
                new SceneFlexItem({
                  width: '50%',
                  height: 250,
                  body: PanelBuilders.timeseries()
                    .setTitle('📨 Kafka Message Throughput')
                    .setDescription('Messages/sec by topic')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'sum(rate(kafka_messages_in_total[1m])) by (topic)',
                      }],
                    })
                    .setMin(0)
                    .build(),
                }),
                new SceneFlexItem({
                  width: '50%',
                  height: 250,
                  body: PanelBuilders.timeseries()
                    .setTitle('⏱️ Consumer Lag')
                    .setDescription('Lag by consumer group')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'sum(kafka_consumer_lag) by (consumer_group)',
                      }],
                    })
                    .setMin(0)
                    .build(),
                }),
              ],
            }),

            // Row 2: Topic Partitions + Broker Health
            new SceneFlexLayout({
              direction: 'row',
              children: [
                new SceneFlexItem({
                  width: '33%',
                  height: 250,
                  body: PanelBuilders.stat()
                    .setTitle('📂 Active Topics')
                    .setDescription('Total number of topics')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'count(count by (topic) (kafka_topic_partitions))',
                      }],
                    })
                    .setThresholds({
                      mode: 'absolute',
                      steps: [
                        { value: 0, color: 'green' },
                        { value: 50, color: 'yellow' },
                        { value: 100, color: 'red' },
                      ],
                    })
                    .build(),
                }),
                new SceneFlexItem({
                  width: '33%',
                  height: 250,
                  body: PanelBuilders.stat()
                    .setTitle('🔌 Online Brokers')
                    .setDescription('Active Kafka brokers')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'count(kafka_broker_online)',
                      }],
                    })
                    .setThresholds({
                      mode: 'absolute',
                      steps: [
                        { value: 0, color: 'red' },
                        { value: 1, color: 'yellow' },
                        { value: 3, color: 'green' },
                      ],
                    })
                    .build(),
                }),
                new SceneFlexItem({
                  width: '34%',
                  height: 250,
                  body: PanelBuilders.stat()
                    .setTitle('👥 Consumer Groups')
                    .setDescription('Active consumer groups')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'count(count by (consumer_group) (kafka_consumer_group_members))',
                      }],
                    })
                    .setThresholds({
                      mode: 'absolute',
                      steps: [
                        { value: 0, color: 'green' },
                      ],
                    })
                    .build(),
                }),
              ],
            }),

            // Row 3: Processing Time + Error Rate
            new SceneFlexLayout({
              direction: 'row',
              children: [
                new SceneFlexItem({
                  width: '50%',
                  height: 250,
                  body: PanelBuilders.timeseries()
                    .setTitle('⚡ Message Processing Time')
                    .setDescription('P95 processing latency')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'histogram_quantile(0.95, sum(rate(kafka_processing_duration_seconds_bucket[5m])) by (le, consumer_group))',
                      }],
                    })
                    .setUnit('s')
                    .setMin(0)
                    .build(),
                }),
                new SceneFlexItem({
                  width: '50%',
                  height: 250,
                  body: PanelBuilders.timeseries()
                    .setTitle('❌ Processing Error Rate')
                    .setDescription('Errors/sec by topic')
                    .setData({
                      queries: [{
                        refId: 'A',
                        expr: 'sum(rate(kafka_processing_errors_total[5m])) by (topic)',
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

      const newScene = new EmbeddedScene(sceneConfig);

      if (containerRef.current) {
        newScene.activate();
        setScene(newScene);
        setLoading(false);
      }
    } catch (err) {
      console.error('Failed to initialize StreamingScene:', err);
      setError(err.message);
      setLoading(false);
    }
  };

  if (error) {
    return (
      <Alert severity="error">
        Nepodařilo se načíst streaming monitoring: {error}
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
        🚀 Kafka Streaming Monitoring
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

export default StreamingScene;
