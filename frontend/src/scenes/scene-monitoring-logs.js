/**
 * Logs Dashboard - Real-time Log Search & Analysis (Loki Integration)
 * 
 * This dashboard provides:
 * - Real-time log streaming from Loki
 * - Log search with filtering by level and service
 * - Log volume metrics
 * - Error rate derived from logs
 */

import React from 'react';
import { 
  EmbeddedScene, 
  SceneTimeRange,
  SceneFlexLayout, 
  SceneFlexItem,
  SceneObjectBase 
} from '@grafana/scenes';
import { LogSearchPanel } from './components/LogSearchPanel';
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

export async function createLogsScene(container) {
  console.log('[scene-monitoring-logs] Creating Logs Dashboard...');
  
  const scene = new EmbeddedScene({
    $timeRange: new SceneTimeRange({ 
      from: 'now-1h', 
      to: 'now' 
    }),
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        // === LOG METRICS ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '33.33%',
              height: 250,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Log Rate (Total)',
                  icon: '📝',
                  query: 'sum(rate({job=~".+"} [5m]))',
                  unit: 'logs/s',
                  thresholds: { warning: 100, critical: 500 },
                  refreshInterval: 15000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 250,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Error Logs Rate',
                  icon: '❌',
                  query: 'sum(rate({job=~".+"} |= "ERROR" [5m]))',
                  unit: 'errors/s',
                  thresholds: { warning: 5, critical: 20 },
                  refreshInterval: 15000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 250,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Warning Logs Rate',
                  icon: '⚠️',
                  query: 'sum(rate({job=~".+"} |= "WARN" [5m]))',
                  unit: 'warnings/s',
                  thresholds: { warning: 10, critical: 50 },
                  refreshInterval: 15000,
                })
              ),
            }),
          ],
        }),

        // === LOG SEARCH PANEL (Main Component) ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '100%',
              height: 600,
              body: new SceneReactWrapper(
                React.createElement(LogSearchPanel, {
                  datasourceUid: 'loki',
                  refreshInterval: 10000,
                  maxLines: 100,
                })
              ),
            }),
          ],
        }),
      ],
    }),
  });

  console.log('[scene-monitoring-logs] Scene created, activating...');
  scene.activate();
  console.log('[scene-monitoring-logs] Scene activated successfully');

  return scene;
}
