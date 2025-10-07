import { useEffect, useState } from 'react';
import { Box, CircularProgress, Alert } from '@mui/material';
import { useAuth } from '../AuthProvider';
import keycloakService from '../../services/keycloakService';

interface GrafanaEmbedProps {
  dashboardUid?: string;
  panelId?: number;
  height?: string;
  theme?: 'light' | 'dark';
  timeRange?: string;
}

export const GrafanaEmbed = ({ 
  dashboardUid, 
  panelId,
  height = '600px',
  theme = 'light',
  timeRange = 'now-6h'
}: GrafanaEmbedProps) => {
  const { isAuthenticated } = useAuth();
  const [iframeUrl, setIframeUrl] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      setError('Musíte být přihlášeni pro zobrazení monitoringu');
      setLoading(false);
      return;
    }

    try {
      // ✅ JWT Authentication: Get token from Keycloak
      const token = keycloakService.getToken();
      if (!token) {
        setError('Chybí autentizační token');
        setLoading(false);
        return;
      }

      // ✅ Build Grafana URL with HTTPS
      const protocol = 'https:';
      const host = window.location.host;
      const baseUrl = `${protocol}//${host}/monitoring`;
      
      const params = new URLSearchParams({
        theme,
        from: timeRange.startsWith('now') ? timeRange : 'now-6h',
        to: 'now',
        refresh: '30s',
        kiosk: 'tv',
      });

      let url = '';
      if (dashboardUid && panelId) {
        url = `${baseUrl}/d-solo/${dashboardUid}?${params.toString()}&panelId=${panelId}&auth_token=${encodeURIComponent(token)}`;
      } else if (dashboardUid) {
        url = `${baseUrl}/d/${dashboardUid}?${params.toString()}&auth_token=${encodeURIComponent(token)}`;
      } else {
        url = `${baseUrl}/?${params.toString()}&auth_token=${encodeURIComponent(token)}`;
      }

      setIframeUrl(url);
      setLoading(false);
    } catch (err) {
      setError('Chyba při načítání monitoringu');
      setLoading(false);
    }
  }, [isAuthenticated, dashboardUid, panelId, theme, timeRange]);

  if (!isAuthenticated) {
    return (
      <Alert severity="warning">
        Pro zobrazení monitoringu se prosím přihlaste.
      </Alert>
    );
  }

  if (error) {
    return (
      <Alert severity="error">
        {error}
      </Alert>
    );
  }

  if (loading || !iframeUrl) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" height={height}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box 
      sx={{ 
        width: '100%', 
        height,
        border: '1px solid',
        borderColor: 'divider',
        borderRadius: 1,
        overflow: 'hidden'
      }}
    >
      <iframe
        src={iframeUrl}
        width="100%"
        height="100%"
        frameBorder="0"
        style={{ border: 'none' }}
        title="Grafana Dashboard"
        onLoad={() => setLoading(false)}
        onError={() => setError('Chyba při načítání Grafana')}
        // ✅ JWT token is passed via query param, Nginx converts it to Authorization header
        // Grafana validates JWT using JWK_SET_URL from Keycloak
      />
    </Box>
  );
};

export default GrafanaEmbed;
