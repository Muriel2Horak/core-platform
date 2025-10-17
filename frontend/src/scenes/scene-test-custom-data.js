/**
 * Grafana Scenes with CUSTOM DATA (no datasource needed)
 * Uses custom $data provider instead of SceneQueryRunner
 */

import { 
  EmbeddedScene, 
  SceneTimeRange,
  SceneFlexLayout, 
  SceneFlexItem,
  SceneDataTransformer,
  PanelBuilders 
} from '@grafana/scenes';

export async function createCustomDataScene() {
  console.log('[custom-data-test] Creating scene with custom data...');

  // Create scene with custom data provider
  const scene = new EmbeddedScene({
    $timeRange: new SceneTimeRange({ 
      from: 'now-6h', 
      to: 'now',
    }),
    body: new SceneFlexLayout({
      direction: 'row',
      children: [
        // Panel 1: Simple metric
        new SceneFlexItem({
          width: '50%',
          height: 300,
          body: PanelBuilders.stat()
            .setTitle('💻 CPU Usage')
            .setDescription('Current CPU usage')
            .setOption('graphMode', 'area')
            .setOption('textMode', 'value_and_name')
            .setUnit('percent')
            .setData(
              new SceneDataTransformer({
                $data: {
                  // Mock data - in real app would call /api/monitoring/ds/query
                  subscribe: (observer) => {
                    observer.next({
                      state: 'Done',
                      series: [
                        {
                          name: 'CPU',
                          fields: [
                            { name: 'Time', values: [Date.now()] },
                            { name: 'Value', values: [45.3] },
                          ],
                        },
                      ],
                    });
                    return { unsubscribe: () => {} };
                  },
                },
                transformations: [],
              })
            )
            .build(),
        }),
        
        // Panel 2: Another metric
        new SceneFlexItem({
          width: '50%',
          height: 300,
          body: PanelBuilders.stat()
            .setTitle('🧠 Memory Usage')
            .setDescription('Current memory usage')
            .setOption('graphMode', 'area')
            .setOption('textMode', 'value_and_name')
            .setUnit('percent')
            .setData(
              new SceneDataTransformer({
                $data: {
                  subscribe: (observer) => {
                    observer.next({
                      state: 'Done',
                      series: [
                        {
                          name: 'Memory',
                          fields: [
                            { name: 'Time', values: [Date.now()] },
                            { name: 'Value', values: [67.8] },
                          ],
                        },
                      ],
                    });
                    return { unsubscribe: () => {} };
                  },
                },
                transformations: [],
              })
            )
            .build(),
        }),
      ],
    }),
  });

  console.log('[custom-data-test] Scene created, activating...');
  scene.activate();
  console.log('[custom-data-test] Scene activated successfully');

  return scene;
}
