/**
 * Grafana Scene Factories
 * 
 * Factory functions for creating and mounting Grafana Scenes
 * Used by React components to create scenes in centralized #grafana-scenes-root
 */

import { 
  EmbeddedScene, 
  SceneTimeRange, 
  SceneFlexLayout, 
  SceneFlexItem, 
  PanelBuilders 
} from '@grafana/scenes';

/**
 * Creates System Monitoring Scene with CPU, Memory, HTTP, Kafka, PostgreSQL metrics
 */
export async function createSystemMonitoringScene(container, options = {}) {
  const { 
    timeRange = { from: 'now-6h', to: 'now' } 
  } = options;

  console.log('[scene-factories] Creating System Monitoring Scene...');

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

  console.log('[scene-factories] ✅ Scene config created');

  const scene = new EmbeddedScene(sceneConfig);
  console.log('[scene-factories] ✅ EmbeddedScene instance created');

  // Activate and mount
  scene.activate();
  console.log('[scene-factories] ✅ Scene activated');

  // Clear container and append scene
  container.innerHTML = '';
  const sceneDiv = document.createElement('div');
  sceneDiv.style.width = '100%';
  sceneDiv.style.height = '100%';
  container.appendChild(sceneDiv);

  console.log('[scene-factories] ✅ System Monitoring Scene mounted successfully');

  return scene;
}

/**
 * Creates Security Monitoring Scene
 */
export async function createSecurityScene(container, options = {}) {
  // TODO: Implement security scene
  console.log('[scene-factories] Security Scene not yet implemented');
  throw new Error('Security Scene factory not implemented');
}

/**
 * Creates Audit Scene
 */
export async function createAuditScene(container, options = {}) {
  // TODO: Implement audit scene
  console.log('[scene-factories] Audit Scene not yet implemented');
  throw new Error('Audit Scene factory not implemented');
}
