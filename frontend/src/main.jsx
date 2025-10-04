import { createRoot } from 'react-dom/client';
import App from './App.jsx';
import logger from './services/logger.js';

// Log application startup
logger.info('Core Platform Frontend starting up');

// Get root element
const container = document.getElementById('root');
if (!container) {
  logger.error('Root element not found');
  throw new Error('Root element with id "root" not found');
}

// Create React root and render app
const root = createRoot(container);
root.render(<App />);

logger.info('Core Platform Frontend initialized successfully');
