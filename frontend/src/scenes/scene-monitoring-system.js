/**
 * System Resources Dashboard - Infrastructure Monitoring (USE Method)
 * 
 * USE Method focuses on:
 * - Utilization: How busy is the resource?
 * - Saturation: How much work is queued?
 * - Errors: Are there errors occurring?
 * 
 * This dashboard monitors:
 * - CPU utilization and load average
 * - Memory utilization and swap usage
 * - Disk I/O, IOPS, and space
 * - Network traffic and errors
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

export async function createSystemResourcesScene(container, options = {}) {
  console.log('[scene-monitoring-system] Creating System Resources Dashboard...');
  
  const scene = new EmbeddedScene({
    $timeRange: new SceneTimeRange({ 
      from: 'now-1h', 
      to: 'now' 
    }),
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        // === CPU METRICS (Utilization + Saturation) ===
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
                  query: '100 - (avg(irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)',
                  unit: '%',
                  thresholds: { warning: 70, critical: 85 },
                  refreshInterval: 15000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'CPU Load Average (1m)',
                  icon: '⚙️',
                  query: 'avg(node_load1)',
                  unit: '',
                  thresholds: { warning: 2, critical: 4 }, // Adjust based on CPU count
                  refreshInterval: 15000,
                })
              ),
            }),
          ],
        }),

        // === MEMORY METRICS (Utilization + Saturation) ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Memory Usage',
                  icon: '🧠',
                  query: '100 * (1 - (avg(node_memory_MemAvailable_bytes) / avg(node_memory_MemTotal_bytes)))',
                  unit: '%',
                  thresholds: { warning: 75, critical: 90 },
                  refreshInterval: 15000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Swap Usage',
                  icon: '💾',
                  query: '100 * (1 - (avg(node_memory_SwapFree_bytes) / avg(node_memory_SwapTotal_bytes)))',
                  unit: '%',
                  thresholds: { warning: 50, critical: 80 },
                  refreshInterval: 15000,
                })
              ),
            }),
          ],
        }),

        // === DISK METRICS (Utilization + Saturation) ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Disk Usage',
                  icon: '💿',
                  query: '100 * (1 - (avg(node_filesystem_avail_bytes{mountpoint="/"}) / avg(node_filesystem_size_bytes{mountpoint="/"})))',
                  unit: '%',
                  thresholds: { warning: 70, critical: 85 },
                  refreshInterval: 30000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Disk I/O Read',
                  icon: '📖',
                  query: 'sum(rate(node_disk_read_bytes_total[5m])) / 1024 / 1024',
                  unit: 'MB/s',
                  thresholds: { warning: 100, critical: 200 },
                  refreshInterval: 15000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Disk I/O Write',
                  icon: '✍️',
                  query: 'sum(rate(node_disk_written_bytes_total[5m])) / 1024 / 1024',
                  unit: 'MB/s',
                  thresholds: { warning: 100, critical: 200 },
                  refreshInterval: 15000,
                })
              ),
            }),
          ],
        }),

        // === NETWORK METRICS (Utilization + Errors) ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Network In',
                  icon: '📥',
                  query: 'sum(rate(node_network_receive_bytes_total{device!="lo"}[5m])) / 1024 / 1024',
                  unit: 'MB/s',
                  thresholds: { warning: 80, critical: 100 },
                  refreshInterval: 15000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Network Out',
                  icon: '📤',
                  query: 'sum(rate(node_network_transmit_bytes_total{device!="lo"}[5m])) / 1024 / 1024',
                  unit: 'MB/s',
                  thresholds: { warning: 80, critical: 100 },
                  refreshInterval: 15000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Network Errors',
                  icon: '⚠️',
                  query: 'sum(rate(node_network_receive_errs_total[5m])) + sum(rate(node_network_transmit_errs_total[5m]))',
                  unit: 'errors/s',
                  thresholds: { warning: 1, critical: 10 },
                  refreshInterval: 15000,
                })
              ),
            }),
          ],
        }),
      ],
    }),
  });

  console.log('[scene-monitoring-system] Scene created, activating...');
  scene.activate();
  console.log('[scene-monitoring-system] Scene activated successfully');

  return scene;
}
