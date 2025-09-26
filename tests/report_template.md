# Multitenancy Smoke Tests Report

**Datum:** {{TIMESTAMP}}  
**Verze:** {{VERSION}}  
**Konfigurace:** {{CONFIG_SUMMARY}}

## 📋 Přehled testů

| Test Scénář | Status | Popis |
|------------|---------|-------|
| JWT Token Získání | {{STATUS_JWT_ACQUISITION}} | Získání access tokenů pro oba uživatele |
| JWT Tenant Claim | {{STATUS_JWT_TENANT_CLAIM}} | Ověření přítomnosti tenant claimu v JWT |
| Tenant API (/api/tenants/me) | {{STATUS_TENANT_API}} | Ověření správného tenantu pro každého uživatele |
| User API (/api/users/me) | {{STATUS_USER_API}} | Ověření uživatelských dat podle tenantu |
| User Search API | {{STATUS_USER_SEARCH}} | Ověření datové izolace v user search |
| Database Seed | {{STATUS_DB_SEED}} | Vytvoření testovacích dat v DB |
| Loki Logging | {{STATUS_LOKI_LOGGING}} | Ověření tenant MDC v logách |
| Negativní testy | {{STATUS_NEGATIVE_TESTS}} | Testy bez tokenu a s neplatným tokenem |

## 🔧 Konfigurace

```bash
# Masked configuration (bez secrets)
{{MASKED_CONFIG}}
```

## 🔑 JWT Token Analýza

### Tenant 1 ({{TENANT1_KEY}})
```json
{{JWT_PAYLOAD_T1}}
```

### Tenant 2 ({{TENANT2_KEY}})
```json
{{JWT_PAYLOAD_T2}}
```

## 🎯 API Response Testů

### /api/tenants/me

**Tenant 1 Response:**
```json
{{TENANTS_ME_T1}}
```

**Tenant 2 Response:**
```json
{{TENANTS_ME_T2}}
```

### /api/users/me

**Tenant 1 Response:**
```json
{{USERS_ME_T1}}
```

**Tenant 2 Response:**
```json
{{USERS_ME_T2}}
```

### /api/users/search?q=a

**Tenant 1 Results:** {{SEARCH_COUNT_T1}} uživatelů
<details>
<summary>Zobrazit response</summary>

```json
{{USERS_SEARCH_T1}}
```
</details>

**Tenant 2 Results:** {{SEARCH_COUNT_T2}} uživatelů
<details>
<summary>Zobrazit response</summary>

```json
{{USERS_SEARCH_T2}}
```
</details>

## 📊 Datová Izolace

{{DATA_ISOLATION_SUMMARY}}

## 📝 Loki Logging Analýza

**Tenant 1 ({{TENANT1_KEY}}):** {{LOKI_COUNT_T1}} log záznamů  
**Tenant 2 ({{TENANT2_KEY}}):** {{LOKI_COUNT_T2}} log záznamů

{{LOKI_ANALYSIS}}

## ❌ Negativní Testy

{{NEGATIVE_TESTS_SUMMARY}}

## 📁 Artefakty

Všechny raw data jsou uloženy v `artifacts/`:
- `jwt_t1.json`, `jwt_t2.json` - Dekódované JWT payloady
- `tenants_me_t1.json`, `tenants_me_t2.json` - /api/tenants/me responses
- `users_me_t1.json`, `users_me_t2.json` - /api/users/me responses  
- `search_t1.json`, `search_t2.json` - User search responses
- `loki_{{TENANT1_KEY}}.json`, `loki_{{TENANT2_KEY}}.json` - Loki query results
- `summary.json` - Kompletní test summary

## 🏁 Závěr

{{OVERALL_STATUS}}

{{RECOMMENDATIONS}}

---
**Generováno:** {{GENERATION_TIME}}  
**Test Runner:** `tests/multitenancy_smoke.sh`