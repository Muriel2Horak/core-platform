# 🎯 Development Rules - Core Platform

> **Pravidla pro vývoj a AI asistenty**
> Tato pravidla MUSÍ být dodržována při jakýchkoliv změnách v projektu.

---

## 🛠️ 1. ORCHESTRACE PROSTŘEDÍ

### ✅ POUŽIJ:
- **Make** pro všechny operace s prostředím
- **Loki** pro všechny logy (nikdy `docker logs`)
- Příklady:
  ```bash
  make dev-up          # Start dev prostředí
  make dev-down        # Stop dev prostředí
  make logs            # Logy přes Loki
  make logs-backend    # Backend logy
  make logs-frontend   # Frontend logy
  ```

### ❌ NIKDY:
- Nevolej Docker Compose přímo
- Nepoužívej `docker logs`
- Nepiš vlastní bash skripty pro orchestraci (vše do Makefile)

---

## 🔒 2. BEZPEČNOST

### ✅ POUŽIJ:
- **`.env`** pro všechny konfigurace (hesla, URL, porty)
- **SSL** pro všechny externí komunikace (Keycloak, API)
- **Environment variables** místo hardcoded hodnot
- **Secrets management** pro produkční hesla

### ❌ NIKDY:
- Hardcoduj hesla do kódu
- Hardcoduj domény (vždy `process.env.VITE_API_URL`)
- Commituj `.env` do gitu (je v `.gitignore`)
- Používej HTTP kde má být HTTPS

### 📝 Příklad:
```typescript
// ❌ ŠPATNĚ
const API_URL = "http://localhost:8080";

// ✅ SPRÁVNĚ
const API_URL = import.meta.env.VITE_API_URL;
```

---

## 🏗️ 3. PRODUCTION-LIKE MODE

### ✅ POUŽIJ:
- **Dev containers** s hot reload (ne lokální dev servery)
- **Nginx** i v dev módu (stejně jako v produkci)
- **Docker Compose** pro všechny služby
- **Multi-stage builds** v Dockerfile

### ❌ NIKDY:
- Nespouštěj Vite dev server přímo (`npm run dev`)
- Nespouštěj Spring Boot přímo (`mvn spring-boot:run` mimo Docker)
- Nevytvárej "dev-only" konfigurace, které nefungují v produkci

---

## 🚫 4. KÓD KVALITA

### ✅ POUŽIJ:
- **Kompletní implementace** všech funkcí
- **Error handling** všude
- **Logging** pro všechny kritické operace
- **TypeScript strict mode**

### ❌ NIKDY:
- **TODO komentáře** - implementuj hned
- **Prázdné catch bloky** - vždy loguj nebo propaguj chybu
- **Fallback funkce bez implementace** - implementuj nebo vyhoď chybu
- **`any` type** v TypeScript (použij proper typing)

### 📝 Příklad:
```typescript
// ❌ ŠPATNĚ
function loadData() {
  try {
    // TODO: implement later
  } catch (e) {}
}

// ✅ SPRÁVNĚ
function loadData(): Promise<Data> {
  try {
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`API error: ${response.status}`);
    }
    return await response.json();
  } catch (error) {
    logger.error('Failed to load data', error);
    throw new Error('Data loading failed');
  }
}
```

---

## 📦 5. DOCKER & KONTEJNERIZACE

### ✅ POUŽIJ:
- **Multi-stage builds** pro optimalizaci image size
- **Named volumes** pro persistent data
- **Health checks** pro všechny služby
- **.dockerignore** pro exclude nepotřebných souborů

### ❌ NIKDY:
- Nekopíruj `node_modules` nebo `target/` do image
- Nespouštěj služby jako `root` (use non-root user)
- Nepoužívaj `latest` tag (vždy konkrétní verze)

---

## 🔄 6. HOT RELOAD & DEVELOPMENT

### ✅ Dev režim:
```yaml
# Backend - Spring Boot DevTools
backend:
  volumes:
    - ../backend/src:/workspace/src
  environment:
    - SPRING_DEVTOOLS_RESTART_ENABLED=true

# Frontend - Nginx + Vite watch
frontend:
  develop:
    watch:
      - action: sync+restart
        path: ../frontend/src
```

### Očekávaný čas změny:
- **Backend Java změna**: 2-5 sekund (Spring DevTools restart)
- **Frontend změna**: 3-7 sekund (Vite rebuild + nginx reload)
- **První build**: 3-5 minut (jednou)

### ❌ NIKDY:
- Rebuild celého prostředí při změně kódu
- Čekej > 10 sekund na změnu v dev módu

---

## 📊 7. MONITORING & LOGGING

### ✅ Stack:
- **Loki** - centrální logging
- **Prometheus** - metriky
- **Grafana** - vizualizace
- **Jaeger** - distributed tracing

### ✅ Logy:
```bash
make logs              # Všechny služby
make logs-backend      # Backend
make logs-frontend     # Frontend  
make logs-keycloak     # Keycloak
```

### ❌ NIKDY:
- Nepoužívej `docker logs` přímo
- Neloguj do souboru v kontejneru
- Nedelej `console.log` v produkčním kódu (use proper logger)

---

## 🧪 8. TESTOVÁNÍ

### ✅ POUŽIJ:
- **Unit testy** pro business logiku
- **Integration testy** s testcontainers
- **E2E testy** pro kritické flow
- **Make příkazy** pro spouštění testů

```bash
make test-backend      # Backend unit testy
make test-frontend     # Frontend unit testy
make test-e2e          # E2E testy
```

### ❌ NIKDY:
- Commituj kód bez testů
- Skipuj testy v CI/CD
- Píš testy bez assertions

---

## 📁 9. STRUKTURA PROJEKTU

### ✅ Uspořádání:
```
backend/src/
  ├── main/java/cz/muriel/core/
  │   ├── controller/    # REST endpointy
  │   ├── service/       # Business logika
  │   ├── repository/    # Data access
  │   ├── entity/        # JPA entity
  │   └── config/        # Konfigurace

frontend/src/
  ├── components/        # React komponenty
  ├── pages/            # Stránky
  ├── services/         # API volání
  ├── hooks/            # Custom hooks
  └── types/            # TypeScript types

docker/                # Vše pro kontejnery
scripts/               # Utility skripty
docs/                  # Dokumentace
```

---

## 🔑 10. GIT WORKFLOW

### ✅ Commit zprávy:
```
feat: Add user authentication
fix: Resolve JWT token refresh issue
docs: Update deployment guide
refactor: Simplify tenant service logic
```

### ✅ Branch naming:
```
feature/user-authentication
bugfix/jwt-token-refresh
hotfix/security-vulnerability
```

### ❌ NIKDY:
- Commit s názvem "WIP" nebo "fix"
- Commit binárních souborů (JAR, node_modules)
- Commit `.env` s real credentials

---

## 📚 11. DOKUMENTACE

### ✅ VŽDY aktualizuj:
- `README.md` - hlavní návod
- Inline komentáře pro složitou logiku
- OpenAPI/Swagger pro API
- Architecture Decision Records (ADR)

### ❌ NIKDY:
- Nesmaž komentáře při refactoringu
- Nenech zastaralou dokumentaci
- Nepiš dokumentaci "později"

---

## 🚀 12. DEPLOYMENT

### ✅ Prostředí:
- **Development**: Hot reload, debug mode
- **Staging**: Production-like, test data
- **Production**: Optimalizované, monitoring

### ✅ Checklist před deploy:
- [ ] Všechny testy prošly
- [ ] Linting bez chyb
- [ ] Security scan (Dependabot)
- [ ] Dokumentace aktuální
- [ ] Environment variables připravené

---

## ⚡ 13. PERFORMANCE

### ✅ POUŽIJ:
- **Lazy loading** pro komponenty
- **Connection pooling** pro databázi
- **Redis cache** pro často používaná data
- **CDN** pro statické assety

### ❌ NIKDY:
- N+1 queries v databázi
- Blocking calls v async funkci
- Neoptimalizované obrázky
- Velké bundle sizes

---

## 🎨 14. UX/UI

### ✅ POUŽIJ:
- **Material-UI** komponenty (konzistence)
- **Responsive design** (mobile-first)
- **Loading states** pro async operace
- **Error boundaries** pro React komponenty

### ❌ NIKDY:
- Custom komponenty kde existuje MUI varianta
- Inline styles (use styled-components nebo MUI sx)
- Prázdné error messages

---

## 🔧 15. TROUBLESHOOTING

### Když něco nefunguje:

1. **Check logs**: `make logs-backend` nebo `make logs-frontend`
2. **Check environment**: `make dev-check`
3. **Restart services**: `make dev-restart`
4. **Rebuild**: `make dev-rebuild`
5. **Nuclear option**: `make dev-clean && make dev-up`

### Debug porty:
- Backend Java: `5005`
- Frontend (pokud potřeba): přes VS Code

---

## 📞 KONTAKT & HELP

Při problémech:
1. Check tento soubor
2. Check `docs/` složka
3. Check `Makefile` - všechny příkazy tam jsou
4. Ask team lead

---

**Poslední update**: 2025-01-06
**Version**: 1.0
**Owner**: Core Platform Team
