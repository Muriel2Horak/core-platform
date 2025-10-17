/**
 * Mock Grafana Plugin Registry
 * Provides functional plugin implementations to bypass full Grafana plugin system
 */

import React from 'react';
import { setPluginImportUtils } from '@grafana/runtime';

// Mock plugin metadata
const mockPlugins = {
  'text': {
    id: 'text',
    type: 'panel',
    name: 'Text',
    module: 'app/plugins/panel/text/module',
  },
  'timeseries': {
    id: 'timeseries',
    type: 'panel',
    name: 'Time series',
    module: 'app/plugins/panel/timeseries/module',
  },
  'stat': {
    id: 'stat',
    type: 'panel',
    name: 'Stat',
    module: 'app/plugins/panel/stat/module',
  },
};

/**
 * Simple functional panel component
 * Renders panel with title and basic data display
 */
const SimplePanelComponent = ({ width, height, data, options, title }) => {
  return React.createElement('div', {
    style: {
      width: '100%',
      height: '100%',
      padding: '16px',
      background: '#1a1a1a',
      color: '#fff',
      borderRadius: '4px',
      border: '1px solid #333',
      overflow: 'auto',
    }
  }, [
    React.createElement('h3', { key: 'title', style: { marginTop: 0 } }, title || 'Panel'),
    React.createElement('div', { key: 'content', style: { marginTop: '12px' } }, 
      data && data.series && data.series.length > 0
        ? `${data.series.length} data series loaded`
        : options?.content || 'No data'
    )
  ]);
};

/**
 * Initialize mock plugin system with functional panel components
 */
export function initializeMockPluginSystem() {
  console.log('[plugin-mock] Initializing mock plugin system with functional panels...');
  
  try {
    // Create mock plugin import function
    const mockImportPluginModule = (path) => {
      console.log('[plugin-mock] importPluginModule called with path:', path);
      
      // Determine plugin type from path
      const pluginId = path.includes('timeseries') ? 'timeseries' 
                     : path.includes('stat') ? 'stat'
                     : 'text';
      
      // Return a promise that resolves to a module with plugin export
      return Promise.resolve({
        plugin: {
          meta: mockPlugins[pluginId] || mockPlugins['text'],
          // Functional React component that renders something visible
          PanelComponent: SimplePanelComponent,
        },
      });
    };
    
    // Set the plugin import utilities with our mock
    setPluginImportUtils({
      importPluginModule: mockImportPluginModule,
    });
    
    console.log('[plugin-mock] ✅ Mock plugin loader registered with functional panels');
  } catch (err) {
    console.error('[plugin-mock] ❌ Failed to set plugin utils:', err);
  }
}
