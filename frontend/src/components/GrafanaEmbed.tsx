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
 * - Multi-tenant: orgId from /api/me (source of truth)
 * 
 * OrgId Resolution:
 * - ALWAYS fetched from GET /api/me endpoint (backend tenant mapping)
 * - NO client-side heuristics or subdomain detection
 * - Cached per session to avoid repeated API calls
 */

import React from 'react';
import { Box, CircularProgress, Alert, Button } from '@mui/material';
import { OpenInNew } from '@mui/icons-material';
import { useGrafanaOrgId } from '../hooks/useGrafanaOrgId';
import { buildGrafanaUrl } from '../utils/grafanaUrl';

// Grafana subpath - must match NGINX location and Grafana root_url
const GRAFANA_SUBPATH = '/core-admin/monitoring';

interface GrafanaEmbedProps {
  /** Dashboard path (e.g., "d/system-resources" or "/d/abc?var_service=api") */
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
  const [iframeLoading, setIframeLoading] = React.useState(true);
  const { orgId, loading: orgIdLoading, error: orgIdError } = useGrafanaOrgId();

  // Wait for orgId to load
  if (orgIdLoading) {
    return (
      <Box sx={{ position: 'relative', width: '100%', height, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <CircularProgress />
      </Box>
    );
  }

  // Handle orgId fetch error
  if (orgIdError || orgId === null) {
    return (
      <Box sx={{ position: 'relative', width: '100%', height }}>
        <Alert severity="error">
          Failed to load Grafana organization ID: {orgIdError?.message || 'Unknown error'}
          <br />
          <Button
            variant="outlined"
            size="small"
            startIcon={<OpenInNew />}
            onClick={() => window.open(`${GRAFANA_SUBPATH}/`, '_blank')}
            sx={{ mt: 1 }}
          >
            Open Grafana Directly
          </Button>
        </Alert>
      </Box>
    );
  }

  // Build iframe URL with proper query param merging
  const iframeSrc = buildGrafanaUrl(
    GRAFANA_SUBPATH,
    path,
    orgId,
    { theme: 'light', kiosk: true }
  );

  console.log(`🔍 GrafanaEmbed: orgId=${orgId} (from /api/me), src=${iframeSrc}`);

  return (
    <Box sx={{ position: 'relative', width: '100%', height }}>
      {/* Loading overlay */}
      {showLoading && iframeLoading && (
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
        onLoad={() => setIframeLoading(false)}
        sandbox="allow-scripts allow-same-origin allow-forms"
        referrerPolicy="no-referrer"
        title="Grafana Dashboard"
      />
    </Box>
  );
};
