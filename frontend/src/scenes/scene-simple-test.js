/**
 * ULTRA SIMPLIFIED TEST SCENE
 * Just one text panel to test if rendering works at all
 */

import { 
  EmbeddedScene, 
  SceneFlexLayout, 
  SceneFlexItem, 
  PanelBuilders 
} from '@grafana/scenes';

export async function createSimpleTestScene() {
  console.log('[simple-test] Creating simple test scene...');

  const scene = new EmbeddedScene({
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        new SceneFlexItem({
          body: PanelBuilders.text()
            .setTitle('🎉 Test Scene')
            .setOption('content', '# It Works!\n\nIf you see this, Grafana Scenes rendering is working!')
            .build(),
        }),
      ],
    }),
  });

  console.log('[simple-test] Scene created, activating...');
  scene.activate();
  console.log('[simple-test] Scene activated successfully');

  return scene;
}
