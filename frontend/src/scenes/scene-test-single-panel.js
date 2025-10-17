/**
 * TEST: Single Prometheus panel to isolate the issue
 */

import { 
  EmbeddedScene, 
  SceneTimeRange,
  SceneFlexLayout, 
  SceneFlexItem, 
  PanelBuilders 
} from '@grafana/scenes';

export async function createSinglePanelScene() {
  console.log('[single-panel-test] Creating scene with one Prometheus panel...');

  const scene = new EmbeddedScene({
    $timeRange: new SceneTimeRange({ 
      from: 'now-6h', 
      to: 'now',
    }),
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        new SceneFlexItem({
          height: 300,
          body: PanelBuilders.timeseries()
            .setTitle('💻 CPU Usage')
            .setDescription('System CPU usage over time')
            .setData({
              queries: [{
                refId: 'A',
                expr: 'system_cpu_usage',
              }],
            })
            .setMin(0)
            .setMax(100)
            .setUnit('percent')
            .build(),
        }),
      ],
    }),
  });

  console.log('[single-panel-test] Scene created, activating...');
  scene.activate();
  console.log('[single-panel-test] Scene activated successfully');

  return scene;
}
