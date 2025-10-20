/**
 * 📊 GrafanaEmbed - Secure Grafana Dashboard Embed Component
 * 
 * Embeds Grafana dashboards via iframe with:
 * - Same-origin embedding (no cross-origin issues)
 * - SSO authentication via BFF JWT (no tokens in URL)
 * - Secure iframe sandbox and CSP headers
 * - WebSocket support for live data
 * 
 * Usage:
 *   <GrafanaEmbed path="/d/<DASHBOARD_UID>?orgId=<ORG_ID>&theme=light&kiosk" />
 *   <GrafanaEmbed path="/d/system-resources?orgId=1&theme=light&kiosk" height="800px" />
 * 
 * Security:
 * - No auth tokens in URL (handled by BFF via auth_request bridge)
 * - Sandbox: allow-scripts, allow-same-origin, allow-forms
 * - CSP frame-ancestors enforced by Nginx
 * - referrerPolicy: no-referrer to prevent leaking context
 * 
 * @see docs/GRAFANA_EMBED_SSO.md for architecture details
 */

import React from 'react';
import { Box, CircularProgress, Alert } from '@mui/material';

interface GrafanaEmbedProps {
  /** 
   * Grafana dashboard path (without /monitoring prefix)
   * Example: "/d/system-resources?orgId=1&theme=light&kiosk"
   */
  path: string;
  
  /** 
   * Iframe height 
   * Default: 100% 
   */
  height?: string | number;
  
  /**
   * Show loading indicator
   * Default: true
   */
  showLoading?: boolean;
}

/**
 * Grafana Embed Component
 * 
 * Renders a Grafana dashboard in an iframe with secure settings.
 * Authentication is handled transparently via Nginx auth_request + BFF JWT.
 */
export const GrafanaEmbed: React.FC<GrafanaEmbedProps> = ({ 
  path, 
  height = '100%',
  showLoading = true,
}) => {
  const [loading, setLoading] = React.useState(showLoading);
  const [error, setError] = React.useState<string | null>(null);
  const iframeRef = React.useRef<HTMLIFrameElement>(null);

  // Normalize path (remove leading slash if present, we'll add /core-admin/monitoring/)
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  
  // Use same-origin path (admin.core-platform.local) with unified prefix
  // Browser will automatically send HTTP-only cookie with JWT token
  // NO TOKENS IN URL - authentication handled by cookie
  const iframeSrc = `/core-admin/monitoring${normalizedPath}`;

  React.useEffect(() => {
    const iframe = iframeRef.current;
    if (!iframe) return;

    const handleLoad = () => {
      setLoading(false);
      setError(null);
    };

    const handleError = () => {
      setLoading(false);
      setError('Nepodařilo se načíst Grafana dashboard. Zkontrolujte přihlášení.');
    };

    iframe.addEventListener('load', handleLoad);
    iframe.addEventListener('error', handleError);

    return () => {
      iframe.removeEventListener('load', handleLoad);
      iframe.removeEventListener('error', handleError);
    };
  }, [iframeSrc]);

  if (error) {
    return (
      <Alert severity="error" sx={{ width: '100%', minHeight: height }}>
        {error}
      </Alert>
    );
  }

  return (
    <Box sx={{ width: '100%', height, position: 'relative' }}>
      {/* Loading overlay */}
      {loading && (
        <Box 
          display="flex" 
          justifyContent="center" 
          alignItems="center" 
          sx={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            zIndex: 10,
            backgroundColor: 'rgba(255, 255, 255, 0.8)',
          }}
        >
          <CircularProgress />
        </Box>
      )}
      
      {/* Grafana iframe - secure embed */}
      <iframe
        ref={iframeRef}
        src={iframeSrc}
        title="Grafana Dashboard"
        sandbox="allow-scripts allow-same-origin allow-forms"
        referrerPolicy="no-referrer"
        allow="fullscreen"
        style={{ 
          width: '100%', 
          height: '100%', 
          border: 0,
          display: loading ? 'none' : 'block',
        }}
      />
    </Box>
  );
};

export default GrafanaEmbed;
