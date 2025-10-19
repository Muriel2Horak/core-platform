/**
 * Application Performance Dashboard - Service Monitoring (RED Method)
 * 
 * RED Method focuses on:
 * - Rate: Requests per second
 * - Errors: Error rate percentage
 * - Duration: Response time distribution
 * 
 * This dashboard monitors:
 * - Request rate and traffic patterns
 * - Error rate (4xx and 5xx responses)
 * - Response time percentiles (p50, p95, p99)
 * - Active connections and thread pool usage
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

export async function createApplicationPerformanceScene(container) {
  console.log('[scene-monitoring-app] Creating Application Performance Dashboard...');
  
  const scene = new EmbeddedScene({
    $timeRange: new SceneTimeRange({ 
      from: 'now-1h', 
      to: 'now' 
    }),
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        // === RATE: Request Traffic ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Request Rate',
                  icon: '🌐',
                  query: 'sum(rate(http_server_requests_seconds_count[5m]))',
                  unit: 'req/s',
                  thresholds: { warning: 100, critical: 200 },
                  refreshInterval: 10000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Total Requests (5m)',
                  icon: '📊',
                  query: 'sum(increase(http_server_requests_seconds_count[5m]))',
                  unit: 'requests',
                  thresholds: { warning: 30000, critical: 60000 },
                  refreshInterval: 10000,
                })
              ),
            }),
          ],
        }),

        // === ERRORS: Error Rate & 4xx/5xx ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Error Rate (5xx)',
                  icon: '❌',
                  query: '100 * sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))',
                  unit: '%',
                  thresholds: { warning: 1, critical: 5 },
                  refreshInterval: 10000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Client Errors (4xx)',
                  icon: '⚠️',
                  query: '100 * sum(rate(http_server_requests_seconds_count{status=~"4.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))',
                  unit: '%',
                  thresholds: { warning: 5, critical: 10 },
                  refreshInterval: 10000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Success Rate (2xx)',
                  icon: '✅',
                  query: '100 * sum(rate(http_server_requests_seconds_count{status=~"2.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m]))',
                  unit: '%',
                  thresholds: { warning: 95, critical: 90 }, // Inverted: lower is worse
                  refreshInterval: 10000,
                })
              ),
            }),
          ],
        }),

        // === DURATION: Response Time Percentiles ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Response Time (p50)',
                  icon: '⏱️',
                  query: 'histogram_quantile(0.50, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) * 1000',
                  unit: 'ms',
                  thresholds: { warning: 200, critical: 500 },
                  refreshInterval: 10000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Response Time (p95)',
                  icon: '⏱️',
                  query: 'histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) * 1000',
                  unit: 'ms',
                  thresholds: { warning: 500, critical: 1000 },
                  refreshInterval: 10000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Response Time (p99)',
                  icon: '⏱️',
                  query: 'histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) * 1000',
                  unit: 'ms',
                  thresholds: { warning: 1000, critical: 2000 },
                  refreshInterval: 10000,
                })
              ),
            }),
          ],
        }),

        // === SATURATION: Active Connections & Thread Pool ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Active Connections',
                  icon: '🔌',
                  query: 'sum(http_server_connections_active)',
                  unit: 'connections',
                  thresholds: { warning: 500, critical: 800 },
                  refreshInterval: 10000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Thread Pool Usage',
                  icon: '🧵',
                  query: '100 * sum(jvm_threads_live_threads) / sum(jvm_threads_peak_threads)',
                  unit: '%',
                  thresholds: { warning: 70, critical: 85 },
                  refreshInterval: 10000,
                })
              ),
            }),
          ],
        }),
      ],
    }),
  });

  console.log('[scene-monitoring-app] Scene created, activating...');
  scene.activate();
  console.log('[scene-monitoring-app] Scene activated successfully');

  return scene;
}
