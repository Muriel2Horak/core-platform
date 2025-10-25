import { describe, it, expect } from 'vitest';
import { buildGrafanaUrl } from './grafanaUrl';

describe('buildGrafanaUrl', () => {
  const SUBPATH = '/core-admin/monitoring';

  it('should build basic URL WITHOUT orgId (server-side provisioning)', () => {
    const result = buildGrafanaUrl(SUBPATH, '/d/abc', 2);
    expect(result).toBe('/core-admin/monitoring/d/abc');
    expect(result).not.toContain('orgId'); // orgId no longer in URL
  });

  it('should add default theme and kiosk when provided (no orgId)', () => {
    const result = buildGrafanaUrl(SUBPATH, '/d/abc', 2, { theme: 'light', kiosk: true });
    expect(result).toBe('/core-admin/monitoring/d/abc?theme=light&kiosk=1');
    expect(result).not.toContain('orgId'); // orgId no longer in URL
  });

  it('should preserve existing theme parameter', () => {
    const result = buildGrafanaUrl(SUBPATH, '/d/abc?theme=dark', 2, { theme: 'light', kiosk: true });
    expect(result).toBe('/core-admin/monitoring/d/abc?theme=dark&kiosk=1');
    expect(result).not.toContain('orgId'); // orgId no longer in URL
  });

  it('should REMOVE orgId from URL even if present in input path', () => {
    const result = buildGrafanaUrl(SUBPATH, '/d/abc?orgId=1', 2, { theme: 'light', kiosk: true });
    // orgId should be stripped from input path (backend handles org via setUserActiveOrg)
    expect(result).toBe('/core-admin/monitoring/d/abc?theme=light&kiosk=1');
    expect(result).not.toContain('orgId'); // CRITICAL: orgId must NOT be in URL
  });

  it('should preserve custom query parameters (no orgId)', () => {
    const result = buildGrafanaUrl(SUBPATH, '/d/abc?var_x=1&var_y=test', 2, { theme: 'light', kiosk: true });
    expect(result).toBe('/core-admin/monitoring/d/abc?var_x=1&var_y=test&theme=light&kiosk=1');
    expect(result).not.toContain('orgId'); // orgId no longer in URL
  });

  it('should handle path without leading slash', () => {
    const result = buildGrafanaUrl(SUBPATH, 'd/abc', 2);
    expect(result).toBe('/core-admin/monitoring/d/abc');
    expect(result).not.toContain('orgId'); // orgId no longer in URL
  });

  it('should handle baseSubPath with trailing slash', () => {
    const result = buildGrafanaUrl('/core-admin/monitoring/', '/d/abc', 2);
    expect(result).toBe('/core-admin/monitoring/d/abc');
    expect(result).not.toContain('orgId'); // orgId no longer in URL
  });

  it('should merge existing query params correctly (no orgId)', () => {
    const result = buildGrafanaUrl(
      SUBPATH,
      '/d/abc?var_service=api&refresh=30s',
      2,
      { theme: 'light', kiosk: true }
    );
    expect(result).toBe('/core-admin/monitoring/d/abc?var_service=api&refresh=30s&theme=light&kiosk=1');
    expect(result).not.toContain('orgId'); // orgId no longer in URL
  });

  it('should not add kiosk if not requested', () => {
    const result = buildGrafanaUrl(SUBPATH, '/d/abc', 2, { theme: 'light' });
    expect(result).toBe('/core-admin/monitoring/d/abc?theme=light');
    expect(result).not.toContain('orgId'); // orgId no longer in URL
  });

  it('should handle complex dashboard paths (no orgId)', () => {
    const result = buildGrafanaUrl(
      SUBPATH,
      '/d/axiom_sys_overview/system-overview?var_tenant=admin&from=now-6h&to=now',
      2,
      { theme: 'light', kiosk: true }
    );
    expect(result).toContain('/core-admin/monitoring/d/axiom_sys_overview/system-overview');
    expect(result).not.toContain('orgId'); // orgId no longer in URL (backend handles it)
    expect(result).toContain('var_tenant=admin');
    expect(result).toContain('from=now-6h');
    expect(result).toContain('to=now');
    expect(result).toContain('theme=light');
    expect(result).toContain('kiosk=1');
  });
});
