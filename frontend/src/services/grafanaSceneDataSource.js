import axios from 'axios';

/**
 * Custom Grafana Scenes DataSource that proxies through BFF
 * All requests go to /api/monitoring/ds/query with JWT auth
 * NO Grafana tokens exposed to browser
 */
export class GrafanaSceneDataSource {
  constructor() {
    this.uid = 'prometheus'; // Default datasource UID
    this.type = 'prometheus';
    this.name = 'Prometheus (BFF)';
  }

  /**
   * Query method called by Grafana Scenes
   * Proxies request through /api/monitoring/ds/query
   */
  async query(request) {
    console.log('[GrafanaSceneDataSource] 📊 Query request:', {
      targets: request.targets?.length,
      range: request.range,
      intervalMs: request.intervalMs,
    });
    
    try {
      console.log('[GrafanaSceneDataSource] 🌐 Sending POST to /api/monitoring/ds/query...');
      const response = await axios.post('/api/monitoring/ds/query', {
        queries: request.targets.map(target => ({
          refId: target.refId,
          expr: target.expr,
          datasource: {
            uid: this.uid,
            type: this.type,
          },
          format: target.format || 'time_series',
          intervalMs: request.intervalMs,
          maxDataPoints: request.maxDataPoints,
        })),
        range: {
          from: request.range.from.toISOString(),
          to: request.range.to.toISOString(),
          raw: {
            from: request.range.raw.from,
            to: request.range.raw.to,
          },
        },
        intervalMs: request.intervalMs,
        maxDataPoints: request.maxDataPoints,
      }, {
        // Axios will automatically include JWT token from cookie/header
        withCredentials: true,
      });

      console.log('[GrafanaSceneDataSource] ✅ Query response:', {
        status: response.status,
        dataCount: response.data.results?.length,
      });

      return {
        data: response.data.results || [],
      };
    } catch (error) {
      console.error('[GrafanaSceneDataSource] ❌ BFF query failed:', error);
      console.error('[GrafanaSceneDataSource] Error details:', {
        message: error.message,
        response: error.response?.data,
        status: error.response?.status,
      });
      throw new Error(`Failed to query datasource: ${error.message}`);
    }
  }

  /**
   * Test datasource connection
   */
  async testDatasource() {
    try {
      await axios.get('/api/monitoring/health');
      return {
        status: 'success',
        message: 'Datasource connected via BFF',
      };
    } catch (error) {
      return {
        status: 'error',
        message: `Connection failed: ${error.message}`,
      };
    }
  }

  /**
   * Get datasource metadata
   */
  async getResource(path) {
    try {
      const response = await axios.get(`/api/monitoring${path}`, {
        withCredentials: true,
      });
      return response.data;
    } catch (error) {
      console.error('Failed to get resource:', error);
      throw error;
    }
  }
}

/**
 * Factory function to create Loki datasource
 */
export class GrafanaLokiDataSource extends GrafanaSceneDataSource {
  constructor() {
    super();
    this.uid = 'loki';
    this.type = 'loki';
    this.name = 'Loki (BFF)';
  }

  async query(request) {
    try {
      const response = await axios.post('/api/monitoring/ds/query', {
        queries: request.targets.map(target => ({
          refId: target.refId,
          expr: target.expr,
          datasource: {
            uid: this.uid,
            type: this.type,
          },
          maxLines: target.maxLines || 1000,
          intervalMs: request.intervalMs,
        })),
        range: {
          from: request.range.from.toISOString(),
          to: request.range.to.toISOString(),
          raw: {
            from: request.range.raw.from,
            to: request.range.raw.to,
          },
        },
        intervalMs: request.intervalMs,
      }, {
        withCredentials: true,
      });

      return {
        data: response.data.results || [],
      };
    } catch (error) {
      console.error('Loki query failed:', error);
      throw new Error(`Failed to query Loki: ${error.message}`);
    }
  }
}
