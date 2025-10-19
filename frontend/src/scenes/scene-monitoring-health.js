/**
 * Platform Health Dashboard - Service Health & SLI/SLO Monitoring
 * 
 * SLI (Service Level Indicators) & SLO (Service Level Objectives) focus on:
 * - Availability: Service uptime percentage
 * - Performance: Key component performance metrics
 * - Reliability: Circuit breaker states and failure rates
 * 
 * This dashboard monitors:
 * - PostgreSQL database health
 * - Kafka message broker health
 * - Circuit breaker states (Resilience4j)
 * - Service availability indicators
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

export async function createPlatformHealthScene(container) {
  console.log('[scene-monitoring-health] Creating Platform Health Dashboard...');
  
  const scene = new EmbeddedScene({
    $timeRange: new SceneTimeRange({ 
      from: 'now-6h', 
      to: 'now' 
    }),
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        // === DATABASE HEALTH (PostgreSQL) ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'PostgreSQL Connections',
                  icon: '🗄️',
                  query: 'sum(pg_stat_database_numbackends{datname!="template0",datname!="template1"})',
                  unit: 'connections',
                  thresholds: { warning: 80, critical: 95 },
                  refreshInterval: 15000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'DB Query Time (avg)',
                  icon: '⏱️',
                  query: 'avg(rate(pg_stat_statements_mean_exec_time[5m]))',
                  unit: 'ms',
                  thresholds: { warning: 100, critical: 500 },
                  refreshInterval: 15000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'DB Transactions/s',
                  icon: '🔄',
                  query: 'sum(rate(pg_stat_database_xact_commit[5m]))',
                  unit: 'tx/s',
                  thresholds: { warning: 1000, critical: 5000 },
                  refreshInterval: 15000,
                })
              ),
            }),
          ],
        }),

        // === MESSAGE BROKER HEALTH (Kafka) ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Kafka Message Rate',
                  icon: '📨',
                  query: 'sum(rate(kafka_server_brokertopicmetrics_messagesin_total[5m]))',
                  unit: 'msg/s',
                  thresholds: { warning: 1000, critical: 5000 },
                  refreshInterval: 15000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Kafka Consumer Lag',
                  icon: '⏳',
                  query: 'sum(kafka_consumergroup_lag)',
                  unit: 'messages',
                  thresholds: { warning: 1000, critical: 10000 },
                  refreshInterval: 15000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Kafka Failed Messages',
                  icon: '❌',
                  query: 'sum(rate(kafka_server_brokertopicmetrics_failedproducerequests_total[5m]))',
                  unit: 'errors/s',
                  thresholds: { warning: 1, critical: 10 },
                  refreshInterval: 15000,
                })
              ),
            }),
          ],
        }),

        // === RESILIENCE (Circuit Breakers) ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Circuit Breaker: Open',
                  icon: '🔴',
                  query: 'sum(resilience4j_circuitbreaker_state{state="open"})',
                  unit: 'breakers',
                  thresholds: { warning: 1, critical: 3 },
                  refreshInterval: 10000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Circuit Breaker: Half-Open',
                  icon: '🟡',
                  query: 'sum(resilience4j_circuitbreaker_state{state="half_open"})',
                  unit: 'breakers',
                  thresholds: { warning: 1, critical: 2 },
                  refreshInterval: 10000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '33.33%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'CB Failure Rate',
                  icon: '📉',
                  query: '100 * sum(resilience4j_circuitbreaker_failure_rate)',
                  unit: '%',
                  thresholds: { warning: 10, critical: 25 },
                  refreshInterval: 10000,
                })
              ),
            }),
          ],
        }),

        // === SERVICE AVAILABILITY (SLI) ===
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Service Availability (24h)',
                  icon: '✅',
                  query: '100 * (1 - (sum(rate(http_server_requests_seconds_count{status=~"5.."}[24h])) / sum(rate(http_server_requests_seconds_count[24h]))))',
                  unit: '%',
                  thresholds: { warning: 99.9, critical: 99.0 }, // SLO: 99.9% availability
                  refreshInterval: 30000,
                })
              ),
            }),
            new SceneFlexItem({
              width: '50%',
              height: 300,
              body: new SceneReactWrapper(
                React.createElement(MetricPanel, {
                  title: 'Service Uptime',
                  icon: '⏰',
                  query: 'time() - process_start_time_seconds',
                  unit: 'seconds',
                  thresholds: { warning: 86400, critical: 3600 }, // Warning if < 24h uptime
                  refreshInterval: 30000,
                })
              ),
            }),
          ],
        }),
      ],
    }),
  });

  console.log('[scene-monitoring-health] Scene created, activating...');
  scene.activate();
  console.log('[scene-monitoring-health] Scene activated successfully');

  return scene;
}
