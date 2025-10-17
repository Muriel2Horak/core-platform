/**
 * System Monitoring Scene - Native Components with Real Data
 * Uses custom React components wrapped in Scenes objects
 * 
 * Fetches real data from Prometheus via BFF proxy
 */

import React from 'react';
import { 
  EmbeddedScene, 
  SceneTimeRange,
  SceneFlexLayout, 
  SceneFlexItem,
  SceneCanvasText,
  SceneObjectBase 
} from '@grafana/scenes';
import { MetricPanel } from './components/MetricPanel';

/**
 * Wrapper to embed React component in Grafana Scene
 */
class SceneReactWrapper extends SceneObjectBase {
  static Component = ({ model }) => {
    return model.state.component;
  };
  
  constructor(component) {
    super({ component });
  }
}

/**
 * Creates System Monitoring Scene with native components
 * No datasources, no plugins, no PanelBuilders
 */
export async function createSystemMonitoringScene(container, options = {}) {
  const { 
    timeRange = { from: 'now-6h', to: 'now' } 
  } = options;

  console.log('[scene-monitoring-native] Creating monitoring scene with native components...');

  const scene = new EmbeddedScene({
    $timeRange: new SceneTimeRange({ 
      from: timeRange.from, 
      to: timeRange.to,
    }),
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        // Row 1: CPU (with real data) + Memory (placeholder)
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '50%',
              height: 250,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'CPU Usage',
                  icon: '💻',
                  query: '100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)',
                  unit: '%',
                })
              ),
            }),
            new SceneFlexItem({
              width: '50%',
              height: 250,
              body: new SceneCanvasText({
                text: '🧠 Memory Usage (%)\n\nSystem memory utilization\n\nQuery: 100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes))',
                fontSize: 14,
                align: 'left',
              }),
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
              body: new SceneCanvasText({
                text: '🌐 HTTP Requests (req/s)\n\nHTTP request rate\n\nQuery: sum(rate(http_server_requests_seconds_count[5m]))',
                fontSize: 14,
                align: 'left',
              }),
            }),
            new SceneFlexItem({
              width: '50%',
              height: 250,
              body: new SceneCanvasText({
                text: '📨 Kafka Messages (msg/s)\n\nKafka message throughput\n\nQuery: sum(rate(kafka_server_brokertopicmetrics_messagesin_total[5m]))',
                fontSize: 14,
                align: 'left',
              }),
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
              body: new SceneCanvasText({
                text: '🗄️ PostgreSQL Connections\n\nActive database connections\n\nQuery: pg_stat_database_numbackends{datname!="template0",datname!="template1"}',
                fontSize: 12,
                align: 'left',
              }),
            }),
            new SceneFlexItem({
              width: '33%',
              height: 250,
              body: new SceneCanvasText({
                text: '⚠️ Error Rate (%)\n\nHTTP 5xx error rate\n\nQuery: 100 * sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))',
                fontSize: 12,
                align: 'left',
              }),
            }),
            new SceneFlexItem({
              width: '33%',
              height: 250,
              body: new SceneCanvasText({
                text: '⏱️ Response Time (ms)\n\nAverage HTTP response time\n\nQuery: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) * 1000',
                fontSize: 12,
                align: 'left',
              }),
            }),
          ],
        }),
      ],
    }),
  });

  console.log('[scene-monitoring-native] Scene created, activating...');
  scene.activate();
  console.log('[scene-monitoring-native] Scene activated successfully');

  return scene;
}
