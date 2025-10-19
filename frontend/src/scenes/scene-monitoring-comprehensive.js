/**
 * Comprehensive Monitoring Container - Multi-Dashboard Suite
 * 
 * Integrates all monitoring dashboards with navigation:
 * 1. Overview Dashboard (Quick insights)
 * 2. System Resources Dashboard (USE method)
 * 3. Application Performance Dashboard (RED method)
 * 4. Platform Health Dashboard (SLI/SLO)
 * 5. Logs Dashboard (Loki integration)
 * 
 * Features:
 * - Tab navigation between dashboards
 * - Theme support (light/dark)
 * - Threshold-based visual indicators
 * - Real-time metrics with auto-refresh
 */

import React, { useState } from 'react';
import { Box } from '@mui/material';
import { useTheme } from '@mui/material/styles';
import { 
  EmbeddedScene, 
  SceneTimeRange,
  SceneFlexLayout, 
  SceneFlexItem,
  SceneObjectBase 
} from '@grafana/scenes';
import { MonitoringDashboardSelector } from './components/MonitoringDashboardSelector';
import { createSystemResourcesScene } from './scene-monitoring-system';
import { createApplicationPerformanceScene } from './scene-monitoring-app';
import { createPlatformHealthScene } from './scene-monitoring-health';
import { createLogsScene } from './scene-monitoring-logs';

/**
 * Main Monitoring Container Component
 * Manages dashboard switching and navigation
 */
const MonitoringContainer = () => {
  const theme = useTheme();
  const [activeDashboard, setActiveDashboard] = useState('overview');
  const [dashboardContainerRef, setDashboardContainerRef] = useState(null);

  const handleDashboardChange = async (dashboardId) => {
    console.log(`[MonitoringContainer] Switching to dashboard: ${dashboardId}`);
    setActiveDashboard(dashboardId);
    
    // Clear existing scene
    if (dashboardContainerRef) {
      dashboardContainerRef.innerHTML = '';
    }
    
    // Load new scene
    if (dashboardContainerRef) {
      try {
        let newScene = null;
        
        switch (dashboardId) {
          case 'system':
            newScene = await createSystemResourcesScene(dashboardContainerRef);
            break;
          case 'application':
            newScene = await createApplicationPerformanceScene(dashboardContainerRef);
            break;
          case 'health':
            newScene = await createPlatformHealthScene(dashboardContainerRef);
            break;
          case 'logs':
            newScene = await createLogsScene(dashboardContainerRef);
            break;
          case 'overview':
          default:
            // Load overview dashboard (existing scene-monitoring-native.js)
            const { createSystemMonitoringScene } = await import('./scene-monitoring-native');
            newScene = await createSystemMonitoringScene(dashboardContainerRef);
            break;
        }
        
        console.log(`[MonitoringContainer] Dashboard ${dashboardId} loaded:`, newScene);
        console.log(`[MonitoringContainer] Dashboard ${dashboardId} loaded successfully`);
      } catch (error) {
        console.error(`[MonitoringContainer] Error loading dashboard ${dashboardId}:`, error);
      }
    }
  };

  return (
    <Box
      sx={{
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        background: theme.palette.background.default,
        minHeight: '100vh',
        padding: 2,
      }}
    >
      {/* Dashboard Navigation */}
      <MonitoringDashboardSelector
        currentDashboard={activeDashboard}
        onDashboardChange={handleDashboardChange}
      />
      
      {/* Dashboard Content Container */}
      <Box
        ref={setDashboardContainerRef}
        sx={{
          flex: 1,
          width: '100%',
          overflow: 'auto',
        }}
      />
    </Box>
  );
};

/**
 * Wrapper to embed React component in Grafana Scene
 */
class SceneReactWrapper extends SceneObjectBase {
  static Component = ({ model }) => {
    return model.state.component;
  };

  constructor(component) {
    super({ component });
  }
}

/**
 * Create comprehensive monitoring scene with dashboard navigation
 */
export async function createComprehensiveMonitoringScene(container) {
  console.log('[scene-monitoring-comprehensive] Creating comprehensive monitoring dashboard...');
  
  const scene = new EmbeddedScene({
    $timeRange: new SceneTimeRange({ 
      from: 'now-6h', 
      to: 'now' 
    }),
    body: new SceneFlexLayout({
      direction: 'column',
      children: [
        new SceneFlexItem({
          width: '100%',
          height: '100%',
          body: new SceneReactWrapper(
            React.createElement(MonitoringContainer)
          ),
        }),
      ],
    }),
  });

  console.log('[scene-monitoring-comprehensive] Scene created, activating...');
  scene.activate();
  console.log('[scene-monitoring-comprehensive] Scene activated successfully');

  return scene;
}

export default createComprehensiveMonitoringScene;
