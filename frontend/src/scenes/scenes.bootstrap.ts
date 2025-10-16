/**
 * Grafana Scenes Application Bootstrap Entry Point
 * 
 * ESM-only entry that:
 * 1. Ensures boot data is initialized
 * 2. Lazily loads and starts the Scenes app
 * 
 * Note: Grafana UI uses CSS-in-JS (emotion), no separate CSS import needed
 */

// Ensure boot data is set BEFORE any runtime imports
import { ensureBootData } from './bootdata';

console.info('[scenes.bootstrap] 🚀 Starting Grafana Scenes bootstrap...');
console.info('[scenes.bootstrap] boot data present:', !!window.grafanaBootData);

// Initialize boot data if not already set
ensureBootData();

// Dynamically import and start the scenes app
(async () => {
  try {
    console.info('[scenes.bootstrap] 📦 Loading scenes app module...');
    const { startScenesApp } = await import('./scenes.start');
    
    console.info('[scenes.bootstrap] ▶️ Starting scenes app...');
    startScenesApp();
    
    console.info('[scenes.bootstrap] ✅ Scenes app started successfully');
  } catch (error) {
    console.error('[scenes.bootstrap] ❌ Failed to start scenes app:', error);
  }
})();
