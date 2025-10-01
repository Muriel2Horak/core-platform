import { realmFromIss } from './tenantUtils';

describe('realmFromIss', () => {
  test('should extract realm from a standard URL', () => {
    const iss = 'https://auth.example.com/realms/core-platform';
    expect(realmFromIss(iss)).toBe('core-platform');
  });

  test('should extract realm from a URL with a trailing slash', () => {
    const iss = 'https://core-platform.local/realms/acme/';
    expect(realmFromIss(iss)).toBe('acme');
  });

  test('should return undefined for a URL without /realms/', () => {
    const iss = 'https://auth.example.com/core-platform';
    expect(realmFromIss(iss)).toBeUndefined();
  });

  test('should return undefined for an empty string', () => {
    expect(realmFromIss('')).toBeUndefined();
  });

  test('should return undefined for null or undefined input', () => {
    expect(realmFromIss(null)).toBeUndefined();
    expect(realmFromIss(undefined)).toBeUndefined();
  });

  test('should return undefined for a URL where realm is empty', () => {
    const iss = 'https://auth.example.com/realms/';
    expect(realmFromIss(iss)).toBeUndefined();
  });

  test('should handle complex paths after realm', () => {
    const iss = 'https://keycloak.my-company.net/realms/customer-1/protocol/openid-connect/auth';
    expect(realmFromIss(iss)).toBe('customer-1');
  });
});
