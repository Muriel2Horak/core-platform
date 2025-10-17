/**
 * TEST: Prometheus panel with BFF datasource
 * Uses backend /api/monitoring/ds/query endpoint
 */

import { 
  EmbeddedScene, 
  SceneTimeRange,
  SceneFlexLayout, 
  SceneFlexItem,
  SceneQueryRunner,
  PanelBuilders 
} from '@grafana/scenes';
import { setBackendSrv, config } from '@grafana/runtime';

// Custom backend service that calls our BFF
const bffBackendSrv = {
  async fetch(options) {
    console.log('[bff-backend] Fetching:', options);
    
    // Proxy to our backend
    const url = options.url.replace(/^\/api/, '/api/monitoring');
    const response = await fetch(url, {
      method: options.method || 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      body: options.data ? JSON.stringify(options.data) : undefined,
    });
    
    const data = await response.json();
    return { data };
  },
  
  async get(url, options) {
    return this.fetch({ ...options, url, method: 'GET' });
  },
  
  async post(url, data, options) {
    return this.fetch({ ...options, url, method: 'POST', data });
  },
};

// Initialize Grafana runtime config
function initializeGrafanaRuntime() {
  console.log('[bff-prometheus-test] Initializing Grafana runtime...');
  
  // Set backend service
  setBackendSrv(bffBackendSrv);
  
  // Configure Grafana runtime
  config.appUrl = window.location.origin;
  config.bootData = window.grafanaBootData || {};
  config.featureToggles = { scenes: true };
  config.buildInfo = {
    version: 'dev',
    commit: 'dev',
    env: 'production',
  };
  
  console.log('[bff-prometheus-test] Grafana runtime initialized:', {
    appUrl: config.appUrl,
    hasBootData: !!config.bootData,
    featureToggles: config.featureToggles,
  });
}

export async function createBFFPrometheusScene() {
  console.log('[bff-prometheus-test] Creating scene with BFF datasource...');
  
  // Initialize runtime
  initializeGrafanaRuntime();

  // Create query runner that uses our BFF endpoint
  const queryRunner = new SceneQueryRunner({
    datasource: {
      uid: 'prometheus', // This will use our backend proxy
      type: 'prometheus',
    },
    queries: [
      {
        refId: 'A',
        expr: 'up', // Simple query - "is Prometheus up?"
        range: true,
        instant: false,
      },
    ],
  });

  const scene = new EmbeddedScene({
    $timeRange: new SceneTimeRange({ 
      from: 'now-1h', 
      to: 'now',
    }),
    $data: queryRunner,
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        new SceneFlexItem({
          height: 300,
          body: PanelBuilders.timeseries()
            .setTitle('📊 Prometheus Status')
            .setDescription('Testing BFF datasource integration')
            .build(),
        }),
      ],
    }),
  });

  console.log('[bff-prometheus-test] Scene created with BFF datasource, activating...');
  scene.activate();
  console.log('[bff-prometheus-test] Scene activated successfully');

  return scene;
}
