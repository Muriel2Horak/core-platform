# Post-Deployment Verification

Automatické smoke testy, které se spouštějí po úspěšném `make up` nebo `make rebuild`.

## 🎯 Co testuje?

### Základní verze (`make verify`)
- ✅ Container health (všechny běží, žádné restarty)
- ✅ Backend API health endpoint
- ✅ Keycloak health endpoint
- ✅ Database connectivity
- ✅ API endpoints (root, docs, actuator)
- ✅ Frontend přístupnost (HTTPS)
- ✅ Observability stack (Grafana, Loki, Prometheus)
- ✅ Keycloak realm konfigurace

**Čas**: ~10-15 sekund

### Plná verze (`make verify-full`)
Vše výše +
- ✅ Multitenancy smoke tests
- ✅ Streaming integration tests
- 📊 Generování detailního reportu

**Čas**: ~2-3 minuty

## 🚀 Použití

### Automaticky
```bash
# Automaticky se spustí po úspěšném up/rebuild
make up
make rebuild
```

### Manuálně
```bash
# Rychlé smoke testy
make verify

# Plné testy
make verify-full

# Nebo přímo
bash scripts/build/post-deployment-check.sh

# S plnými testy
RUN_FULL_TESTS=true bash scripts/build/post-deployment-check.sh
```

## 📊 Výstup

### Úspěch
```
✅ All checks passed! (23/23)

🎉 Environment is ready to use!

📍 Access points:
   Frontend:  https://core-platform.local/
   Admin:     https://admin.core-platform.local/
   API:       http://localhost:8080/api
   Keycloak:  http://localhost:8081/admin/
   Grafana:   http://localhost:3001/
```

### Selhání
```
❌ Some checks failed: 2/23
✅ Passed: 21

💡 Troubleshooting:
   1. Check logs: make logs-errors
   2. Check containers: docker ps
   3. Check diagnostics: ls -lh diagnostics/
   4. Run full tests: RUN_FULL_TESTS=true make verify
```

## 🔧 Konfigurace

Skript načítá proměnné z `.env`:
- `POSTGRES_USER` - pro database connectivity test
- `DOMAIN` - pro frontend URL testy
- Další env vars podle potřeby

## 🐛 Troubleshooting

### Test selže na frontend HTTPS
```bash
# Zkontroluj /etc/hosts
cat /etc/hosts | grep core-platform

# Zkontroluj nginx/proxy
docker logs core-nginx
```

### Test selže na backend health
```bash
# Zkontroluj backend logy
make logs-backend

# Zkontroluj, jestli backend běží
docker ps | grep backend
curl http://localhost:8080/actuator/health | jq '.'
```

### Test selže na Keycloak
```bash
# Zkontroluj Keycloak logy
make logs-keycloak

# Zkontroluj realm
curl http://localhost:8081/realms/core-platform/.well-known/openid-configuration | jq '.issuer'
```

### Přeskočit verifikaci
```bash
# Použít dev-up bez Build Doctoru
make dev-up

# Nebo zavolat inner target přímo (NEDOPORUČENO)
make _up_inner
```

## 📚 Související

- [BUILD_DOCTOR.md](BUILD_DOCTOR.md) - Kompletní dokumentace Build Doctoru
- [TESTING.md](TESTING.md) - E2E a unit testy
- [../tests/README_tests.txt](../tests/README_tests.txt) - Existující testy

## 🎯 Integrace s CI/CD

Smoke workflow (`.github/workflows/smoke.yml`) automaticky spouští:
1. `make rebuild` - build s Build Doctorem
2. Automatická verifikace - post-deployment checks
3. Upload diagnostics artifacts

Na self-hosted runnerech běží proti skutečnému PROD-like prostředí.
