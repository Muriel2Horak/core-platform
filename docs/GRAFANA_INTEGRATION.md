# 📊 Grafana SSO Integrace - Návod

## ✅ Co bylo nakonfigurováno

### 1. **Keycloak OAuth2 Klient pro Grafana**
- **Client ID**: `grafana`
- **Client Secret**: `grafana-secret-change-in-prod` (změňte v produkci!)
- **Redirect URI**: `https://*.core-platform.local/monitoring/login/generic_oauth`
- **Role mapping**: CORE_ROLE_ADMIN → Admin, CORE_ROLE_USER → Editor

### 2. **Nginx Routing**
Grafana je dostupná na **všech doménách** pod cestou `/monitoring`:
- `https://core-platform.local/monitoring`
- `https://admin.core-platform.local/monitoring`
- `https://company-a.core-platform.local/monitoring` (pro tenant adminy)

### 3. **Grafana Konfigurace**
- **SSO přes Keycloak**: Automatické přihlášení s OAuth2
- **Role-based Access Control**: Admin/Editor/Viewer dle Keycloak rolí
- **Sub-path deployment**: Běží pod `/monitoring` cestou

## 🚀 Jak to spustit

### Krok 1: Rebuild Keycloak (kvůli novému klientovi)
```bash
cd /Users/martinhorak/Projects/core-platform
docker compose -f docker/docker-compose.yml down keycloak
docker compose -f docker/docker-compose.yml build keycloak --no-cache
docker compose -f docker/docker-compose.yml up -d
```

### Krok 2: Ověření
1. Otevřete: `https://core-platform.local/monitoring`
2. Klikněte na "Sign in with Keycloak"
3. Přihlaste se jako `test_admin` / `Test.1234`
4. Měli byste být přesměrováni do Grafany s Admin právy

## 🎨 Integrace do React FE

### Varianta A: iframe Embed (Simple)

```tsx
// src/components/Monitoring/GrafanaEmbed.tsx
import { useEffect, useState } from 'react';
import { useAuth } from '@/contexts/AuthContext';

interface GrafanaEmbedProps {
  dashboardUid?: string;
  panelId?: number;
  height?: string;
}

export const GrafanaEmbed = ({ 
  dashboardUid = 'd-solo', 
  panelId,
  height = '600px' 
}: GrafanaEmbedProps) => {
  const { isAuthenticated } = useAuth();
  const [iframeUrl, setIframeUrl] = useState('');

  useEffect(() => {
    if (isAuthenticated) {
      const baseUrl = `${window.location.origin}/monitoring`;
      
      if (dashboardUid && panelId) {
        // Embed konkrétní panel
        setIframeUrl(`${baseUrl}/d-solo/${dashboardUid}?orgId=1&panelId=${panelId}`);
      } else {
        // Celá Grafana
        setIframeUrl(`${baseUrl}/`);
      }
    }
  }, [isAuthenticated, dashboardUid, panelId]);

  if (!isAuthenticated || !iframeUrl) {
    return <div>Loading monitoring...</div>;
  }

  return (
    <iframe
      src={iframeUrl}
      width="100%"
      height={height}
      frameBorder="0"
      style={{ border: 'none' }}
      title="Grafana Dashboard"
    />
  );
};
```

### Varianta B: Odkaz do nové záložky

```tsx
// src/components/Monitoring/MonitoringLink.tsx
import { Button } from '@mui/material';
import { OpenInNew } from '@mui/icons-material';

export const MonitoringLink = () => {
  const openMonitoring = () => {
    window.open(`${window.location.origin}/monitoring`, '_blank');
  };

  return (
    <Button
      variant="outlined"
      onClick={openMonitoring}
      startIcon={<OpenInNew />}
    >
      Open Monitoring Dashboard
    </Button>
  );
};
```

### Varianta C: Material-UI Dialog s plnou Grafanou

```tsx
// src/components/Monitoring/MonitoringDialog.tsx
import { Dialog, DialogContent, DialogTitle, IconButton } from '@mui/material';
import { Close } from '@mui/icons-material';
import { useState } from 'react';

export const MonitoringDialog = ({ open, onClose }: { open: boolean; onClose: () => void }) => {
  return (
    <Dialog 
      open={open} 
      onClose={onClose}
      maxWidth="xl"
      fullWidth
      PaperProps={{
        sx: { height: '90vh' }
      }}
    >
      <DialogTitle>
        Monitoring Dashboard
        <IconButton
          onClick={onClose}
          sx={{ position: 'absolute', right: 8, top: 8 }}
        >
          <Close />
        </IconButton>
      </DialogTitle>
      <DialogContent sx={{ p: 0 }}>
        <iframe
          src={`${window.location.origin}/monitoring`}
          width="100%"
          height="100%"
          frameBorder="0"
          style={{ border: 'none' }}
          title="Grafana Dashboard"
        />
      </DialogContent>
    </Dialog>
  );
};
```

## 🔐 Přístupová práva

### Super Admin (CORE_ROLE_ADMIN)
- ✅ Plný přístup ke všem dashboardům
- ✅ Může vytvářet/editovat dashboardy
- ✅ Vidí data ze všech tenantů

### Tenant Admin (CORE_ROLE_USER)
- ✅ Editor přístup
- ✅ Může vytvářet dashboardy pro svůj tenant
- ✅ Vidí pouze data svého tenantu (filtrováno v Loki)

### Regular User
- ✅ Viewer přístup
- ✅ Pouze čtení dashboardů
- ✅ Vidí data svého tenantu

## 📊 Přidání do navigace

```tsx
// src/components/Layout/Navigation.tsx
import { Assessment } from '@mui/icons-material';

const menuItems = [
  // ...existing items...
  {
    title: 'Monitoring',
    icon: <Assessment />,
    path: '/monitoring',
    roles: ['CORE_ROLE_ADMIN', 'CORE_ROLE_USER'], // Jen admin a tenant admin
  },
];
```

## 🔍 Loki Data Source konfigurace

Grafana je již nakonfigurována s Loki data source, která filtruje logy podle tenantu:

```yaml
# docker/grafana/provisioning/datasources/loki.yml
apiVersion: 1
datasources:
  - name: Loki
    type: loki
    access: proxy
    url: http://loki:3100
    jsonData:
      derivedFields:
        - datasourceUid: loki
          matcherRegex: "tenant=(\\w+)"
          name: Tenant
          url: $${__value.raw}
```

## 🎯 Best Practices

### 1. **Tenant Izolace**
Pro multi-tenant support přidejte do Grafana query filtr:
```promql
{service="backend"} |~ "tenant=${TENANT_KEY}"
```

### 2. **Security Headers**
Nginx již obsahuje správné CSP headery pro iframe embed:
```nginx
frame-src 'self' https://*.core-platform.local;
frame-ancestors 'self' https://*.core-platform.local;
```

### 3. **Performance**
Pro embed použijte konkrétní panely místo celé Grafany:
```
/monitoring/d-solo/dashboard-uid?orgId=1&panelId=2
```

## 🐛 Troubleshooting

### Problém: "Invalid redirect_uri"
**Řešení**: Zkontrolujte Keycloak klienta - redirect URI musí obsahovat `/monitoring/login/generic_oauth`

### Problém: "403 Forbidden" při embedu
**Řešení**: Zkontrolujte CSP headers v Nginx konfiguraci

### Problém: Grafana nevidí Keycloak login
**Řešení**: Restartujte Keycloak a Grafana:
```bash
docker compose -f docker/docker-compose.yml restart keycloak grafana
```

## �� Environment Variables

V `.env` souboru máte tyto proměnné:
```bash
GRAFANA_PASSWORD=admin                          # Fallback admin heslo
GRAFANA_OAUTH_SECRET=grafana-secret-change-in-prod  # OAuth2 secret (změňte v produkci!)
GRAFANA_PORT=3001                               # Direct access port (není nutný)
```

## 🚀 Další kroky

1. **Vytvořit dashboardy pro:**
   - Application metrics (API latency, throughput)
   - Business metrics (users, tenants, events)
   - Infrastructure (CPU, memory, disk)

2. **Tenant-specific dashboardy:**
   - Každý tenant může mít vlastní dashboardy
   - Filtrovat data pomocí `tenant` labelu v Loki

3. **Alerts a notifications:**
   - Nastavit alerting rules v Grafaně
   - Propojit s email/Slack notifications
