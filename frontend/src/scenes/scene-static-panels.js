/**
 * Ultra simple Grafana Scenes - WITHOUT PanelBuilders
 * Using SceneCanvasText to completely bypass plugin system
 */

import { 
  EmbeddedScene, 
  SceneFlexLayout, 
  SceneFlexItem,
  SceneCanvasText 
} from '@grafana/scenes';

export async function createStaticPanelsScene() {
  console.log('[static-panels] Creating scene WITHOUT PanelBuilders (using SceneCanvasText)...');

  const scene = new EmbeddedScene({
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        // Row 1: Text components (NO plugins!)
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '33%',
              height: 200,
              body: new SceneCanvasText({
                text: '💻 CPU Monitoring\n\nSystem CPU metrics dashboard',
                fontSize: 16,
                align: 'center',
              }),
            }),
            new SceneFlexItem({
              width: '33%',
              height: 200,
              body: new SceneCanvasText({
                text: '🧠 Memory Monitoring\n\nMemory usage tracking',
                fontSize: 16,
                align: 'center',
              }),
            }),
            new SceneFlexItem({
              width: '33%',
              height: 200,
              body: new SceneCanvasText({
                text: '📊 System Health\n\nOverall status indicators',
                fontSize: 16,
                align: 'center',
              }),
            }),
          ],
        }),
        
        // Row 2: More text components
        new SceneFlexLayout({
          direction: 'row',
          children: [
            new SceneFlexItem({
              width: '50%',
              height: 200,
              body: new SceneCanvasText({
                text: '⚡ Performance\n\nResponse time metrics',
                fontSize: 16,
                align: 'center',
              }),
            }),
            new SceneFlexItem({
              width: '50%',
              height: 200,
              body: new SceneCanvasText({
                text: '🔒 Security\n\nSecurity event monitoring',
                fontSize: 16,
                align: 'center',
              }),
            }),
          ],
        }),
      ],
    }),
  });

  console.log('[static-panels] Scene created (NO plugins used), activating...');
  scene.activate();
  console.log('[static-panels] Scene activated successfully');

  return scene;
}
