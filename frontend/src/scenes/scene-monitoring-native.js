/**
 * System Monitoring Scene - Native Components with Real Data
 * Uses custom React components wrapped in Scenes objects
 * 
 * Fetches real data from Prometheus via BFF proxy
 * 
 * Features:
 * - Threshold-based visual indicators (green/yellow/red)
 * - Light/dark theme support
 * - Real-time metrics with auto-refresh
 * - Best practices: RED method & USE method
 */

import React from 'react';
import { 
  EmbeddedScene, 
  SceneTimeRange,
  SceneFlexLayout, 
  SceneFlexItem,
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
        // Row 1: CPU + Memory (Infrastructure - USE Method: Utilization)
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'CPU Usage',
                  icon: '💻',
                  query: '100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)',
                  unit: '%',
                  thresholds: { warning: 70, critical: 85 },
                })
              ),
            }),
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Memory Usage',
                  icon: '🧠',
                  query: '100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes))',
                  unit: '%',
                  thresholds: { warning: 75, critical: 90 },
                })
              ),
            }),
          ],
        }),

        // Row 2: HTTP Requests + Error Rate (Application - RED Method: Rate & Errors)
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'HTTP Requests',
                  icon: '🌐',
                  query: 'sum(rate(http_server_requests_seconds_count[5m]))',
                  unit: 'req/s',
                  thresholds: { warning: 100, critical: 200 },
                })
              ),
            }),
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Error Rate',
                  icon: '⚠️',
                  query: '100 * sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))',
                  unit: '%',
                  thresholds: { warning: 1, critical: 5 },
                })
              ),
            }),
          ],
        }),

        // Row 3: Response Time + Kafka Messages (RED: Duration & Infrastructure)
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Response Time (p95)',
                  icon: '⏱️',
                  query: 'histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) * 1000',
                  unit: 'ms',
                  thresholds: { warning: 500, critical: 1000 },
                })
              ),
            }),
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Kafka Messages',
                  icon: '📨',
                  query: 'sum(rate(kafka_server_brokertopicmetrics_messagesin_total[5m]))',
                  unit: 'msg/s',
                  thresholds: { warning: 1000, critical: 5000 },
                })
              ),
            }),
          ],
        }),

        // Row 4: PostgreSQL Connections (Database Health)
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '100%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'PostgreSQL Connections',
                  icon: '🗄️',
                  query: 'sum(pg_stat_database_numbackends{datname!="template0",datname!="template1"})',
                  unit: 'connections',
                  thresholds: { warning: 80, critical: 95 },
                })
              ),
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
