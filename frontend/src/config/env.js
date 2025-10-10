// Environment configuration for Core Platform Frontend
export const ENV = {
  STREAMING_ENABLED: import.meta.env.VITE_STREAMING_ENABLED === 'true',
  GRAFANA_PUBLIC_URL: import.meta.env.VITE_GRAFANA_PUBLIC_URL || 'https://grafana.core-platform.local',
  API_BASE_URL: import.meta.env.VITE_API_BASE_URL || '/api',
};

export default ENV;
