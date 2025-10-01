/**
 * Extrahuje název realmu z URL issueru (claim 'iss' v JWT).
 *
 * @param {string | undefined} iss - URL issueru, např. "https://core-platform.local/realms/my-tenant".
 * @returns {string | undefined} Název realmu (např. "my-tenant") nebo undefined, pokud se nepodaří extrakce.
 */
export function realmFromIss(iss) {
  if (!iss || typeof iss !== 'string') {
    return undefined;
  }

  const realmMarker = '/realms/';
  const realmIndex = iss.indexOf(realmMarker);

  if (realmIndex === -1) {
    return undefined;
  }

  // Získáme část za '/realms/' a odstraníme případné koncové lomítko
  let realm = iss.substring(realmIndex + realmMarker.length).replace(/\/$/, '');

  // Odstraníme vše za prvním dalším lomítkem, pokud existuje
  if (realm.includes('/')) {
    realm = realm.substring(0, realm.indexOf('/'));
  }

  return realm || undefined;
}
