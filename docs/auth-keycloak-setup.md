# Keycloak integrace (lokální vývoj)

Co je kde
- docker/keycloak/realm-core-platform.json – import realmu (klienti web, api; role admin; uživatel test/Test.1234)
- docker/docker-compose.yml – služby db (sdílená Postgres) a keycloak
- frontend/public/auth – přihlašovací stránka (Keycloak JS)

Spuštění
- docker compose -f docker/docker-compose.yml up -d db keycloak
- Keycloak: http://localhost:8081 (admin/admin)
- Přihlašování: otevři http://localhost:3000/auth/ (frontend dev server musí běžet)

Klienti v Keycloak
- web (public, PKCE): redirectUris/webOrigins už nastaveny na http://localhost:3000/*
- api (bearer-only): audience „api“ je do access tokenu přidána přes client scope api-audience

Frontend
- Přihlašovací stránka je statická v /auth a po přihlášení nabízí „Pokračovat do aplikace“ (APP_URL = "/").
- Pokud aplikace běží jinde, uprav APP_URL v frontend/public/auth/index.html.

Backend (napojení)
- Ověřuj JWT pomocí JWKS z issueru: http://localhost:8081/realms/core-platform
- Očekávej audience: api (claim aud)
- Role čti z claimu realm_access.roles
- Běžné env pro BE:
  - OIDC_ISSUER=http://localhost:8081/realms/core-platform
  - OIDC_API_AUDIENCE=api
  - CORS_ORIGINS=http://localhost:3000

Poznámky
- Sdílená DB: služba „db“ hostuje databáze „core“ i „keycloak“ (vytvořeno skriptem docker/db/init/10-keycloak.sql).
- Keycloak je infrastrukturní služba → zůstává v docker/, nikoli v backend/. Složka backend/auth je určena pro aplikační middleware (ověření tokenu, role).
- Při změně FE domény/portu přidej hodnoty do Redirect URIs a Web Origins u klienta „web“.
- Pokud token neobsahuje audience=api, zkontroluj, že client scope „api-audience“ je v defaultClientScopes klienta „web“.
