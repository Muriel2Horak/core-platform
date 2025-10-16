/**
 * Grafana Boot Data Initialization
 * Ensures window.grafanaBootData is set before any @grafana/runtime imports
 */

declare global {
  interface Window {
    grafanaBootData: {
      user: {
        isSignedIn: boolean;
        orgId: number;
        orgRole: string;
        timeZone: string;
      };
      settings: {
        appSubUrl: string;
        baseUrl: string;
        buildInfo: {
          version: string;
          commit: string;
          env: string;
        };
        featureToggles: {
          scenes: boolean;
        };
        defaultDatasource: string | null;
        theme2: string;
      };
    };
  }
}

export function ensureBootData(): void {
  if (window.grafanaBootData) {
    console.info('[bootdata] ✅ grafanaBootData already exists');
    return;
  }

  const theme = localStorage.getItem('grafana.user.theme') || 'dark';
  
  window.grafanaBootData = {
    user: {
      isSignedIn: true,
      orgId: 1,
      orgRole: 'Admin',
      timeZone: 'browser',
    },
    settings: {
      appSubUrl: '',
      baseUrl: '',
      buildInfo: {
        version: 'dev',
        commit: 'dev',
        env: (window as any).NODE_ENV || 'production',
      },
      featureToggles: {
        scenes: true,
      },
      defaultDatasource: null,
      theme2: theme,
    },
  };

  console.info('[bootdata] ✅ grafanaBootData initialized');
}
