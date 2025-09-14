# core-platform

Univerzální vývojová platforma pro tvorbu enterprise aplikací, založená na modulárním **CORE** frameworku. Odděluje doménově nezávislou funkcionalitu od konkrétních aplikací – umožňuje rychlé vytváření nadstavbových řešení pomocí sdíleného jádra.

## 🧱 Architektura

### Frontend
- **React** s **TypeScriptem**
- Vite jako dev server a bundler
- Modulární GUI komponenty
- Vizualizační nástroje (např. React Flow pro řízení stavů)

### Backend
- **Spring Boot** (Java 21)
- REST API
- Připojení na PostgreSQL
- Připraven pro rozšíření o:
  - Metadata engine
  - Procesní engine
  - Modulární security
  - DMS integrace
  - Auditní logy

### Infrastruktura
- Docker Compose pro vývojové prostředí
- PostgreSQL jako databáze
- PgAdmin pro správu DB
- Grafana + Loki + Promtail pro logování
- Jaeger (volitelně) pro tracing
- Keycloak pro autentizaci (OIDC)

## 🧪 Vývojové prostředí

### Požadavky
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- [Node.js ≥ 20](https://nodejs.org/) (pouze pro frontend development)
- [Java 21](https://adoptium.net/)
- [Visual Studio Code](https://code.visualstudio.com/) + doporučené pluginy:
  - ESLint
  - Prettier
  - Java
  - Docker
  - GitHub Copilot

### Složky
```
📦 core-platform/
 ┣ 📂 backend/             ← Spring Boot backend
 ┣ 📂 frontend/            ← React frontend
 ┣ 📂 docker/              ← Docker Compose, konfigurace Grafana, Loki atd.
 ┣ 📂 docs/                ← Dokumentace platformy
 ┣ 📂 tools/               ← Nástroje a utility (např. datové migrace)
 ┣ 📂 .vscode/             ← Nastavení vývojového prostředí
 ┗ 📄 README.md
```

## 🚀 Spuštění vývojového prostředí

```bash
cd docker
docker compose up -d --build
```

Frontend poběží na http://localhost:3000  
Backend na http://localhost:8080  
Grafana na http://localhost:3001  
PgAdmin na http://localhost:5050

## 🔗 Proxy API (Vite)

Frontend proxy přesměrovává `/api` na backend, viz `vite.config.ts`:
```ts
server: {
  proxy: {
    '/api': {
      target: 'http://core-backend:8080',
      changeOrigin: true,
      rewrite: path => path.replace(/^\/api/, ''),
    }
  }
}
```

## 🧑‍💻 Příprava pro GitHub Copilot

Copilot získá kontext z:
- `README.md` (tento soubor)
- Projektové struktury a názvů složek
- `.vscode/settings.json`, `tsconfig.json`, `.prettierrc`, `.eslintrc`
- Názvů a struktur komponent, endpointů a entit

➡️ Doporučeno začít vývoj s komentovaným skeletonem komponent / API / služeb.

## 📦 Roadmapa

- [x] Docker stack pro vývoj
- [x] Vite + React + Spring Boot propojeno
- [ ] Logging do Loki
- [ ] GUI Designer
- [ ] Metadata Editor
- [ ] BPM Engine
- [ ] Uživatelské role a oprávnění
- [ ] Auditní logy

## 🔐 Přihlášení (Keycloak)

Umístění
- docker/docker-compose.yml → služby db a keycloak (import realmu)
- docker/keycloak/realm-core-platform.json → realm core-platform (klienti web, api; role admin; uživatel test/Test.1234)
- frontend/public/auth → přihlašovací stránka (keycloak-js), přesměrování do aplikace

Spuštění
- docker compose -f docker/docker-compose.yml up -d db keycloak
- Keycloak UI: http://localhost:8081 (admin/admin)
- Login stránka: http://localhost:3000/auth/

Frontend
- KEYCLOAK_CFG je v frontend/public/auth/index.html (url, realm, clientId=web)
- APP_URL určuje, kam po přihlášení pokračovat (výchozí „/")

Backend
- Ověřuj JWT proti issueru: http://localhost:8081/realms/core-platform (z hosta)
- V kontejneru používej: http://keycloak:8080/realms/core-platform
- Očekávaná audience: api (claim aud)
- Role z claimu: realm_access.roles
- Doporučené env: OIDC_ISSUER, OIDC_API_AUDIENCE=api, CORS_ORIGINS=http://localhost:3000

Troubleshooting
- 401: zkontroluj ISS a audience=api, i čas (clock skew)
- Redirect/CORS: Redirect URIs a Web Origins u klienta „web“ musí obsahovat http://localhost:3000/*
- Chybí audience=api: client scope „api-audience“ je v defaultClientScopes klienta „web“

Kontrola docker-compose (rychlý checklist)
- keycloak běží na 8081, db běží a je healthy
- Volume pro import realmu: docker/keycloak → /opt/keycloak/data/import:ro
- Backend má v docker-compose dependency na keycloak a env OIDC_ISSUER=http://keycloak:8080/realms/core-platform
- Frontend běží na 3000 a obsluhuje /auth

🧹 Úklid: docker/node_modules
- Složka docker/node_modules do repozitáře nepatří. Smaž ji a přidej do .gitignore:
  - rm -rf docker/node_modules
  - do .gitignore přidej řádek: docker/node_modules/

---

> Tento repozitář obsahuje **jádro platformy**. Konkrétní aplikace budou vznikat jako samostatné repozitáře, které budou importovat `core-platform` jako submodul nebo závislost.
