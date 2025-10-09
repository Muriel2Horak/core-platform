# Reporting Module - Phase 3

> Backend reporting a analytics s Cube.js integrací, cache, rate limiting a bulk operations

## 📊 Status

- **Větev:** `feature/be-reporting-phase3`
- **Progress:** 20% (Fáze 3.0-3.1 částečně)
- **Commity:** 3 (foundation, plan, summary)

## ✅ Hotové

### Fáze 3.0 - Konfigurace (KOMPLETNÍ)
- Reporting modul struktura (8 packages)
- `application-reporting.yml` konfigurace
- Redis + Caffeine cache
- Feature toggles
- Cube.js RestClient

### Fáze 3.1 - DSL (ČÁSTEČNĚ)
- `QueryRequest` / `QueryResponse` DTO
- `@ValidQuery` validace + guardrails
- `QueryFingerprint` (SHA-256)
- `MetamodelSpecService` integrace
- Unit testy (100% coverage)

## 🚧 TODO

| Fáze | Komponenty | Priority |
|------|-----------|----------|
| 3.2 | Cube.js client, mapper, security context | **P0** |
| 3.3 | REST API, rate-limit filter, error handling | **P0** |
| 3.4 | Report views (CRUD + RBAC) | P1 |
| 3.5 | Bulk update jobs (async worker) | P1 |
| 3.6 | Metriky, structured logging | P2 |
| 3.7 | Security hardening, RLS, guardrails | P0 |
| 3.8 | Cache optimalizace, invalidace | P2 |
| 3.9 | Cleanup Grafana CDC sync | P3 |
| 3.10 | Load testy, backpressure | P2 |
| 3.11 | Dokumentace, runbooky | P3 |

## 🏗️ Architektura

```
User → JWT Auth → Rate Limit → Validation → Cache Check
                                                ├─ HIT → Response
                                                └─ MISS ↓
                                            Cube.js API → Cache Store → Response
```

## 📂 Struktura

```
reporting/
├── api/          REST kontrolery (TODO)
├── app/          Konfigurace ✅
├── cube/         Cube.js integrace (TODO)
├── dsl/          Query DSL ✅
├── jobs/         Bulk operations (TODO)
├── model/        JPA entity (TODO)
├── repo/         Repository (TODO)
├── security/     RLS, rate-limit (TODO)
└── support/      Helpers ✅
```

## 🎯 Klíčové vlastnosti

- **Configuration-driven:** Feature toggles, limity v YAML
- **Guardrails:** Max 50k rows, max 92 days interval
- **Cache:** Redis (primary) + Caffeine (fallback), TTL 60s
- **Rate-limit:** 120 req/min/tenant (Bucket4j)
- **Metamodel:** Validace polí proti schématu
- **Audit:** Bulk operations s before/after snapshoty

## 🚀 Quick Start

```bash
# Aktivovat profil
spring:
  profiles:
    active: reporting

# Environment vars
export CUBE_BASE_URL=http://localhost:4000
export CUBE_API_TOKEN=your-token-here
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Kontrola konfigurace
curl http://localhost:8080/actuator/env | jq '.propertySources[] | select(.name | contains("reporting"))'
```

## 📖 Dokumentace

| Dokument | Popis |
|----------|-------|
| [PHASE_3_0_REPORTING_CONFIG.md](PHASE_3_0_REPORTING_CONFIG.md) | Fáze 3.0 detail |
| [PHASE_3_IMPLEMENTATION_PLAN.md](PHASE_3_IMPLEMENTATION_PLAN.md) | Kompletní plán 11 subfází |
| [PHASE_3_SUMMARY.md](PHASE_3_SUMMARY.md) | Executive summary |

## 🧪 Testy

```bash
# Unit testy (hotové)
./mvnw test -Dtest=ReportingPropertiesTest
./mvnw test -Dtest=ReportingFeatureToggleTest

# Integrační testy (TODO)
./mvnw verify -Dit.test=ReportQueryControllerIT
```

## 📊 Metriky (plánované)

```
report_query_latency_seconds{tenant, entity, cache_hit, quantile}
report_query_total{tenant, entity, status}
report_query_rows{tenant, entity}
bulk_jobs_running{tenant}
cube_api_latency_seconds{status}
```

## 🔒 Security

- **RLS:** Cube.js context + SQL WHERE tenant_id
- **RBAC:** Report views (private/group/tenant/global)
- **Sanitizace:** DSL → jOOQ, whitelist polí
- **Rate-limit:** Per tenant, 429 s Retry-After

## 🎯 Acceptance Criteria

- [ ] `/api/reports/query` s cache, rate-limit, metriky
- [ ] `/api/report-views` CRUD + RBAC
- [ ] `/api/entities/{entity}/bulk-update` async jobs
- [ ] Redis cache + Caffeine fallback
- [ ] Bucket4j rate-limit
- [ ] Micrometer → Prometheus
- [ ] RLS v čtení i zápisu
- [ ] CDC Grafana sync odstraněn
- [ ] Testy zelené
- [ ] Load testy (p95 < 500ms)

## 🏁 Next Steps

1. **Implementovat Cube.js client** (Fáze 3.2)
   ```bash
   touch backend/src/main/java/cz/muriel/core/reporting/cube/CubeClient.java
   touch backend/src/main/java/cz/muriel/core/reporting/cube/CubeMapper.java
   ```

2. **REST API + rate-limit** (Fáze 3.3)
   ```bash
   touch backend/src/main/java/cz/muriel/core/reporting/api/ReportQueryController.java
   touch backend/src/main/java/cz/muriel/core/reporting/security/RateLimitFilter.java
   ```

3. **DB migrace + bulk jobs** (Fáze 3.4-3.5)
   ```bash
   touch backend/src/main/resources/db/migration/V202510091400__create_report_views.sql
   touch backend/src/main/resources/db/migration/V202510091500__create_bulk_jobs.sql
   ```

---

**Poznámka:** Pro pokračování v implementaci konkrétní subfáze viz [PHASE_3_IMPLEMENTATION_PLAN.md](PHASE_3_IMPLEMENTATION_PLAN.md)
