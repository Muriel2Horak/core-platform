# Grafana Scenes Usage Guide

## Overview

This guide explains how to use Grafana Scenes in our frontend application to create embedded monitoring dashboards without iframes.

## What are Grafana Scenes?

Grafana Scenes (@grafana/scenes v6.39.5) is a library for building Grafana-like dashboards directly in React applications. It provides:

- 📊 **Native React Components**: No iframes, full control over styling
- 🔒 **Secure**: All queries go through BFF proxy, no token exposure
- 🎨 **Customizable**: Full control over layout, theme, and interactions
- 🚀 **Type-Safe**: TypeScript support with full type definitions

## Architecture

```
Frontend (React + Scenes) → BFF (/api/monitoring/*) → Grafana API
                ↓                    ↓                      ↓
        GrafanaSceneDataSource  JWT → SAT + Org-Id    Prometheus/Loki
```

## Getting Started

### 1. Install Dependencies

```bash
cd frontend
npm install @grafana/scenes @grafana/data @grafana/runtime @grafana/ui
```

### 2. Create Custom DataSource

Create `src/utils/grafanaSceneDataSource.js`:

```javascript
import { SceneDataProvider, SceneDataState } from '@grafana/scenes';

export class GrafanaSceneDataSource extends SceneDataProvider {
  constructor() {
    super({
      uid: 'bff-proxy',
      type: 'prometheus', // or 'loki' for logs
    });
  }

  async query(request) {
    const response = await fetch('/api/monitoring/ds/query', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('jwt')}`,
      },
      body: JSON.stringify({
        queries: request.targets,
        from: request.range.from.valueOf(),
        to: request.range.to.valueOf(),
      }),
    });

    const data = await response.json();
    return { data: data.results };
  }
}
```

### 3. Create a Simple Scene

Create `src/pages/Reports.jsx`:

```javascript
import React, { useMemo } from 'react';
import {
  EmbeddedScene,
  SceneFlexLayout,
  SceneFlexItem,
  SceneQueryRunner,
  VizPanel,
} from '@grafana/scenes';
import { GrafanaSceneDataSource } from '../utils/grafanaSceneDataSource';

export default function Reports() {
  const scene = useMemo(() => {
    return new EmbeddedScene({
      body: new SceneFlexLayout({
        direction: 'column',
        children: [
          new SceneFlexItem({
            height: 300,
            body: new VizPanel({
              title: 'CPU Usage',
              pluginId: 'timeseries',
              $data: new SceneQueryRunner({
                datasource: new GrafanaSceneDataSource(),
                queries: [
                  {
                    refId: 'A',
                    expr: 'rate(process_cpu_seconds_total[5m])',
                    legendFormat: '{{instance}}',
                  },
                ],
              }),
            }),
          }),
        ],
      }),
    });
  }, []);

  return (
    <div className="h-screen">
      <scene.Component model={scene} />
    </div>
  );
}
```

## Common Patterns

### Panel with Multiple Queries

```javascript
new VizPanel({
  title: 'Request Rate & Error Rate',
  pluginId: 'timeseries',
  $data: new SceneQueryRunner({
    datasource: new GrafanaSceneDataSource(),
    queries: [
      {
        refId: 'A',
        expr: 'sum(rate(http_requests_total[5m]))',
        legendFormat: 'Request Rate',
      },
      {
        refId: 'B',
        expr: 'sum(rate(http_requests_total{status=~"5.."}[5m]))',
        legendFormat: 'Error Rate',
      },
    ],
  }),
});
```

### Panel with Variables

```javascript
import { SceneVariableSet, QueryVariable } from '@grafana/scenes';

const scene = new EmbeddedScene({
  $variables: new SceneVariableSet({
    variables: [
      new QueryVariable({
        name: 'instance',
        label: 'Instance',
        query: 'label_values(up, instance)',
        datasource: new GrafanaSceneDataSource(),
      }),
    ],
  }),
  body: new VizPanel({
    title: 'CPU by Instance',
    $data: new SceneQueryRunner({
      datasource: new GrafanaSceneDataSource(),
      queries: [
        {
          refId: 'A',
          expr: 'rate(process_cpu_seconds_total{instance="$instance"}[5m])',
        },
      ],
    }),
  }),
});
```

### Tabs Layout

```javascript
import { SceneTabs, SceneFlexLayout } from '@grafana/scenes';

new EmbeddedScene({
  body: new SceneTabs({
    tabs: [
      new SceneFlexLayout({
        title: 'Application',
        children: [/* app metrics panels */],
      }),
      new SceneFlexLayout({
        title: 'Infrastructure',
        children: [/* infra panels */],
      }),
      new SceneFlexLayout({
        title: 'Logs',
        children: [/* log panels with Loki datasource */],
      }),
    ],
  }),
});
```

### Loki Logs Panel

```javascript
new VizPanel({
  title: 'Application Logs',
  pluginId: 'logs',
  $data: new SceneQueryRunner({
    datasource: new GrafanaSceneDataSource(), // type: 'loki'
    queries: [
      {
        refId: 'A',
        expr: '{app="backend", level="error"}',
      },
    ],
  }),
});
```

## Visualization Types

### Available VizPanel Plugins

- `timeseries`: Time series graph (line, bar, points)
- `stat`: Single stat panel with gauge/spark line
- `table`: Data table
- `logs`: Log viewer (for Loki datasource)
- `heatmap`: Heatmap visualization
- `bargauge`: Bar gauge panel
- `piechart`: Pie chart

Example Stat Panel:

```javascript
new VizPanel({
  title: 'Active Users',
  pluginId: 'stat',
  options: {
    graphMode: 'area',
    colorMode: 'value',
    orientation: 'horizontal',
  },
  $data: new SceneQueryRunner({
    datasource: new GrafanaSceneDataSource(),
    queries: [
      {
        refId: 'A',
        expr: 'count(up{job="backend"} == 1)',
      },
    ],
  }),
});
```

## Time Range Controls

```javascript
import { SceneTimeRange, SceneTimePicker } from '@grafana/scenes';

const scene = new EmbeddedScene({
  $timeRange: new SceneTimeRange({
    from: 'now-1h',
    to: 'now',
  }),
  controls: [new SceneTimePicker({})],
  body: /* panels */,
});
```

## Refresh Interval

```javascript
import { SceneRefreshPicker } from '@grafana/scenes';

const scene = new EmbeddedScene({
  controls: [
    new SceneRefreshPicker({
      intervals: ['5s', '10s', '30s', '1m', '5m'],
      refresh: '10s', // default
    }),
  ],
  body: /* panels */,
});
```

## Error Handling

```javascript
export class GrafanaSceneDataSource extends SceneDataProvider {
  async query(request) {
    try {
      const response = await fetch('/api/monitoring/ds/query', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('jwt')}`,
        },
        body: JSON.stringify({
          queries: request.targets,
          from: request.range.from.valueOf(),
          to: request.range.to.valueOf(),
        }),
      });

      if (!response.ok) {
        if (response.status === 429) {
          throw new Error('Rate limit exceeded. Please try again later.');
        }
        if (response.status === 503) {
          throw new Error('Monitoring service unavailable. Circuit breaker open.');
        }
        throw new Error(`Query failed: ${response.statusText}`);
      }

      const data = await response.json();
      return { data: data.results };
    } catch (error) {
      console.error('Grafana Scenes query error:', error);
      return { data: [], error: error.message };
    }
  }
}
```

## Best Practices

### 1. Use useMemo for Scene Creation

```javascript
const scene = useMemo(() => new EmbeddedScene({ /* ... */ }), []);
```

Prevents scene recreation on every render.

### 2. Lazy Load Panels

```javascript
import { lazy, Suspense } from 'react';

const ReportsPage = lazy(() => import('./pages/Reports'));

function App() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <ReportsPage />
    </Suspense>
  );
}
```

### 3. Handle BFF Errors Gracefully

```javascript
$data: new SceneQueryRunner({
  datasource: new GrafanaSceneDataSource(),
  queries: [/* ... */],
  errorHandling: 'silent', // or 'show'
});
```

### 4. Cache Dashboard Metadata

The BFF already caches `/datasources` and `/dashboards/uid/:uid` responses for 30s.

### 5. Use Template Variables for Multi-Tenancy

```javascript
new QueryVariable({
  name: 'tenant',
  label: 'Tenant',
  query: 'label_values(up, tenant)',
  datasource: new GrafanaSceneDataSource(),
});
```

Then use `$tenant` in queries: `{tenant="$tenant"}`.

## Troubleshooting

### "Query failed: 429"

**Cause**: Rate limit exceeded (100 req/min per tenant).

**Solution**: Increase refresh interval or reduce number of panels.

```javascript
controls: [
  new SceneRefreshPicker({
    refresh: '30s', // Increase from 5s to 30s
  }),
],
```

### "Circuit breaker open"

**Cause**: Grafana is unhealthy or unreachable.

**Solution**: Check BFF logs and Grafana health:

```bash
curl https://app.core-platform.local/api/monitoring/health
```

### Panels Not Loading

**Cause**: JWT expired or missing.

**Solution**: Refresh token and ensure Authorization header is set:

```javascript
headers: {
  Authorization: `Bearer ${localStorage.getItem('jwt')}`,
}
```

## Advanced Example: Complete Dashboard

```javascript
import React, { useMemo } from 'react';
import {
  EmbeddedScene,
  SceneFlexLayout,
  SceneFlexItem,
  SceneQueryRunner,
  VizPanel,
  SceneTabs,
  SceneTimeRange,
  SceneTimePicker,
  SceneRefreshPicker,
  SceneVariableSet,
  QueryVariable,
} from '@grafana/scenes';
import { GrafanaSceneDataSource } from '../utils/grafanaSceneDataSource';

export default function Reports() {
  const scene = useMemo(() => {
    const datasource = new GrafanaSceneDataSource();

    return new EmbeddedScene({
      $timeRange: new SceneTimeRange({ from: 'now-1h', to: 'now' }),
      $variables: new SceneVariableSet({
        variables: [
          new QueryVariable({
            name: 'instance',
            label: 'Instance',
            query: 'label_values(up, instance)',
            datasource,
          }),
        ],
      }),
      controls: [
        new SceneTimePicker({}),
        new SceneRefreshPicker({
          intervals: ['10s', '30s', '1m', '5m'],
          refresh: '30s',
        }),
      ],
      body: new SceneTabs({
        tabs: [
          new SceneFlexLayout({
            title: 'Application',
            direction: 'column',
            children: [
              new SceneFlexItem({
                height: 300,
                body: new VizPanel({
                  title: 'Request Rate',
                  pluginId: 'timeseries',
                  $data: new SceneQueryRunner({
                    datasource,
                    queries: [
                      {
                        refId: 'A',
                        expr: 'sum(rate(http_requests_total{instance="$instance"}[5m]))',
                        legendFormat: '{{method}} {{path}}',
                      },
                    ],
                  }),
                }),
              }),
              new SceneFlexItem({
                height: 300,
                body: new VizPanel({
                  title: 'Error Rate',
                  pluginId: 'timeseries',
                  $data: new SceneQueryRunner({
                    datasource,
                    queries: [
                      {
                        refId: 'A',
                        expr: 'sum(rate(http_requests_total{instance="$instance",status=~"5.."}[5m]))',
                        legendFormat: 'Errors',
                      },
                    ],
                  }),
                }),
              }),
            ],
          }),
          new SceneFlexLayout({
            title: 'Logs',
            direction: 'column',
            children: [
              new SceneFlexItem({
                height: 600,
                body: new VizPanel({
                  title: 'Application Logs',
                  pluginId: 'logs',
                  $data: new SceneQueryRunner({
                    datasource, // Loki datasource
                    queries: [
                      {
                        refId: 'A',
                        expr: '{instance="$instance", level="error"}',
                      },
                    ],
                  }),
                }),
              }),
            ],
          }),
        ],
      }),
    });
  }, []);

  return (
    <div className="h-screen p-4">
      <h1 className="text-2xl font-bold mb-4">Monitoring Dashboard</h1>
      <scene.Component model={scene} />
    </div>
  );
}
```

## References

- [Grafana Scenes Documentation](https://grafana.com/developers/scenes/)
- [Grafana Scenes GitHub](https://github.com/grafana/scenes)
- [BFF API Documentation](https://app.core-platform.local/swagger-ui.html)
- [Monitoring BFF Architecture](../MONITORING_BFF_ARCHITECTURE.md)
