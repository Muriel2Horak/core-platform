# E2E Configuration & Helpers

Tato složka obsahuje sdílenou konfiguraci a utility pro E2E testy.

## Struktura

```
e2e/
├── config/
│   └── read-config.ts      # Čte konfiguraci z .env a application.properties
├── .auth/
│   └── README.md           # Placeholder pro session storage states
├── .gitignore              # Ignoruje .auth/*.json a test artifacts
└── package.json            # ES module config
```

## Použití

### Config Reader

```typescript
import { readE2EConfig } from '../../e2e/config/read-config.js';

const config = readE2EConfig();
console.log(config.baseUrl);        // https://core-platform.local
console.log(config.ignoreTLS);      // false (nebo true pokud E2E_IGNORE_TLS=true)
console.log(config.keycloak.realm); // admin
```

### Environment Variables Override

```bash
# Base URL
E2E_BASE_URL=https://custom.local

# TLS validation
E2E_IGNORE_TLS=true

# Keycloak
E2E_REALM=my-realm
E2E_CLIENT_ID=my-client

# Test user
E2E_USER=admin
E2E_PASS=Admin.1234
```

## Poznámky

- **Spec soubory** jsou ve `frontend/tests/e2e/` kvůli module resolution (@playwright/test)
- **Helpers** jsou také ve `frontend/tests/e2e/helpers/` ze stejného důvodu
- **Config** zůstává v `e2e/config/` protože nemá závislosti na Playwright
- Vše čte z existujících konfiguračních souborů projektu (trunk-based, žádné duplikace)
