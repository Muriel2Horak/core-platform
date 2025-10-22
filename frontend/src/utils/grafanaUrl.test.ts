import { describe, it, expect } from 'vitest';
import { buildGrafanaUrl } from './grafanaUrl';

describe('buildGrafanaUrl', () => {
  const SUBPATH = '/core-admin/monitoring';

  it('should build basic URL with orgId and defaults', () => {
    const result = buildGrafanaUrl(SUBPATH, '/d/abc', 2);
    expect(result).toBe('/core-admin/monitoring/d/abc?orgId=2');
  });

  it('should add default theme and kiosk when provided', () => {
    const result = buildGrafanaUrl(SUBPATH, '/d/abc', 2, { theme: 'light', kiosk: true });
    expect(result).toBe('/core-admin/monitoring/d/abc?orgId=2&theme=light&kiosk=1');
  });

  it('should preserve existing theme parameter', () => {
    const result = buildGrafanaUrl(SUBPATH, '/d/abc?theme=dark', 2, { theme: 'light', kiosk: true });
    expect(result).toBe('/core-admin/monitoring/d/abc?theme=dark&orgId=2&kiosk=1');
  });

  it('should always overwrite existing orgId parameter', () => {
    const result = buildGrafanaUrl(SUBPATH, '/d/abc?orgId=1', 2, { theme: 'light', kiosk: true });
    expect(result).toBe('/core-admin/monitoring/d/abc?orgId=2&theme=light&kiosk=1');
  });

  it('should preserve custom query parameters', () => {
    const result = buildGrafanaUrl(SUBPATH, '/d/abc?var_x=1&var_y=test', 2, { theme: 'light', kiosk: true });
    expect(result).toBe('/core-admin/monitoring/d/abc?var_x=1&var_y=test&orgId=2&theme=light&kiosk=1');
  });

  it('should handle path without leading slash', () => {
    const result = buildGrafanaUrl(SUBPATH, 'd/abc', 2);
    expect(result).toBe('/core-admin/monitoring/d/abc?orgId=2');
  });

  it('should handle baseSubPath with trailing slash', () => {
    const result = buildGrafanaUrl('/core-admin/monitoring/', '/d/abc', 2);
    expect(result).toBe('/core-admin/monitoring/d/abc?orgId=2');
  });

  it('should merge existing query params correctly', () => {
    const result = buildGrafanaUrl(
      SUBPATH,
      '/d/abc?var_service=api&refresh=30s',
      2,
      { theme: 'light', kiosk: true }
    );
    expect(result).toBe('/core-admin/monitoring/d/abc?var_service=api&refresh=30s&orgId=2&theme=light&kiosk=1');
  });

  it('should not add kiosk if not requested', () => {
    const result = buildGrafanaUrl(SUBPATH, '/d/abc', 2, { theme: 'light' });
    expect(result).toBe('/core-admin/monitoring/d/abc?orgId=2&theme=light');
  });

  it('should handle complex dashboard paths', () => {
    const result = buildGrafanaUrl(
      SUBPATH,
      '/d/axiom_sys_overview/system-overview?var_tenant=admin&from=now-6h&to=now',
      2,
      { theme: 'light', kiosk: true }
    );
    expect(result).toContain('/core-admin/monitoring/d/axiom_sys_overview/system-overview');
    expect(result).toContain('orgId=2');
    expect(result).toContain('var_tenant=admin');
    expect(result).toContain('from=now-6h');
    expect(result).toContain('to=now');
    expect(result).toContain('theme=light');
    expect(result).toContain('kiosk=1');
  });
});
