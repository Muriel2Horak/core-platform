/**
 * Grafana Scenes Application Starter
 * 
 * Provides runtime stubs and mounts the Scenes app
 */

import { SceneAppPage, SceneFlexLayout, SceneFlexItem, EmbeddedScene } from '@grafana/scenes';
import { setLocationSrv, setBackendSrv } from '@grafana/runtime';
import { PanelBuilders } from '@grafana/scenes';

// Provide minimal LocationSrv stub
class LocationSrvStub {
  getLocation() {
    return {
      path: location.pathname,
      query: {},
    };
  }

  update(opts: any) {
    if (opts?.path) {
      history.replaceState({}, '', opts.path);
    }
  }
}

// Provide minimal BackendSrv stub
const backendSrvStub = {
  get: async () => ({ data: {} }),
  fetch: async () => ({ data: {} }),
  post: async () => ({ data: {} }),
  put: async () => ({ data: {} }),
  delete: async () => ({ data: {} }),
} as any;

// Set stubs
setLocationSrv(new LocationSrvStub() as any);
setBackendSrv(backendSrvStub);

let currentScene: EmbeddedScene | null = null;

/**
 * Creates a simple demo scene with a title panel
 */
function createDemoScene(): EmbeddedScene {
  const scene = new EmbeddedScene({
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        new SceneFlexItem({
          body: PanelBuilders.text()
            .setTitle('Core Streaming Overview')
            .setOption('content', '# Grafana Scenes Integration\n\nNative Scenes integration is active!')
            .build(),
        }),
      ],
    }),
  });

  return scene;
}

/**
 * Starts the Scenes application
 */
export function startScenesApp(): void {
  console.info('[scenes.start] 🎬 Starting Scenes app...');

  // Find container
  const container = document.getElementById('grafana-scenes-root');
  if (!container) {
    console.error('[scenes.start] ❌ Container #grafana-scenes-root not found');
    return;
  }

  console.info('[scenes.start] ✅ Container found:', container);

  // Clean up previous scene if exists
  if (currentScene) {
    console.info('[scenes.start] 🧹 Cleaning up previous scene...');
    // SceneAppPage handles its own cleanup
    currentScene = null;
  }

  // Create and mount scene
  try {
    console.info('[scenes.start] 🏗️ Creating scene...');
    currentScene = createDemoScene();

    console.info('[scenes.start] 🎨 Activating scene...');
    currentScene.activate();

    console.info('[scenes.start] 📍 Mounting scene to container...');
    container.innerHTML = ''; // Clear container
    const sceneElement = document.createElement('div');
    sceneElement.style.height = '100%';
    sceneElement.style.width = '100%';
    container.appendChild(sceneElement);

    console.info('[scenes.start] ✅ Scene mounted successfully');
  } catch (error) {
    console.error('[scenes.start] ❌ Failed to mount scene:', error);
  }
}

// HMR support
if (import.meta.hot) {
  import.meta.hot.accept(() => {
    console.info('[scenes.start] 🔥 HMR: Reloading...');
    if (currentScene) {
      startScenesApp();
    }
  });

  import.meta.hot.dispose(() => {
    console.info('[scenes.start] 🔥 HMR: Disposing...');
    currentScene = null;
  });
}
