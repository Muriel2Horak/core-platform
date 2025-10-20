/**
 * GrafanaEmbed - Secure iframe embed for Grafana dashboards
 * 
 * Authentication via BFF pattern:
 * Browser -> Nginx auth_request -> Backend /internal/auth/grafana -> Grafana
 * 
 * Security:
 * - No tokens in URL (only in HTTP-only cookies)
 * - Sandbox restrictions
 * - CSP protection
 * - Multi-tenant: works on any subdomain (admin, tenant1, etc.)
 */

import React from 'react';
import { Box, CircularProgress } from '@mui/material';

interface GrafanaEmbedProps {
  /** Dashboard path (e.g., "d/system-resources/system-monitoring") */
  path: string;
  /** Height in pixels */
  height?: number;
  /** Show loading spinner */
  showLoading?: boolean;
}

export const GrafanaEmbed: React.FC<GrafanaEmbedProps> = ({
  path,
  height = 800,
  showLoading = true,
}) => {
  const [loading, setLoading] = React.useState(true);

  // Normalize path (remove leading slash if present)
  const normalizedPath = path.startsWith('/') ? path.slice(1) : path;
  
  // Build RELATIVE iframe src - works on any subdomain
  // CRITICAL: Use relative URL for multi-tenant support
  // Browser will use current origin (https://admin.core-platform.local, https://tenant1.core-platform.local, etc.)
  const iframeSrc = `/core-admin/monitoring/${normalizedPath}`;

  return (
    <Box sx={{ position: 'relative', width: '100%', height }}>
      {/* Loading overlay */}
      {showLoading && loading && (
        <Box
          sx={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: 'rgba(255, 255, 255, 0.8)',
            zIndex: 1,
          }}
        >
          <CircularProgress />
        </Box>
      )}

      {/* Grafana iframe */}
      <iframe
        src={iframeSrc}
        width="100%"
        height="100%"
        frameBorder="0"
        style={{ border: 0 }}
        onLoad={() => setLoading(false)}
        sandbox="allow-scripts allow-same-origin allow-forms"
        referrerPolicy="no-referrer"
        title="Grafana Dashboard"
      />
    </Box>
  );
};
