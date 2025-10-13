# 🧪 Rychlý Testing Guide - Grafana Provisioning

## 🚀 Rychlý Start

### 1. Rebuild a Restart Backend

```bash
cd /Users/martinhorak/Projects/core-platform

# Stop backend
docker stop backend

# Rebuild
docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml build backend

# Start
docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml up -d backend

# Sledovat startup
docker logs -f backend
```

**Čekáme na:**
- ✅ `Flyway: Migrating schema "public" to version "3 - grafana tenant bindings"`
- ✅ `Started BackendApplication in X seconds`

### 2. Ověřit Migraci

```bash
docker exec -it db psql -U core -d core -c "\d grafana_tenant_bindings"
```

**Očekávaný výstup:**
```
                         Table "public.grafana_tenant_bindings"
        Column         |           Type           | Collation | Nullable | Default
-----------------------+--------------------------+-----------+----------+---------
 id                    | bigint                   |           | not null | nextval(...)
 tenant_id             | character varying(100)   |           | not null |
 grafana_org_id        | bigint                   |           | not null |
 service_account_id    | bigint                   |           | not null |
 service_account_name  | character varying(200)   |           | not null |
 service_account_token | character varying(500)   |           | not null |
 created_at            | timestamp with time zone |           | not null |
 updated_at            | timestamp with time zone |           | not null |
```

### 3. Test #1: Vytvoření Nového Tenantu

```bash
# Přihlásit se jako admin na https://admin.core-platform.local
# Nebo použít API:

TOKEN="<JWT_TOKEN_FROM_BROWSER_DEVTOOLS>"

curl -X POST https://admin.core-platform.local/api/admin/tenants \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "acme-corp",
    "displayName": "ACME Corporation"
  }'
```

**Sledovat backend logy:**
```bash
docker logs -f backend | grep -i grafana
```

**Očekávaný výstup:**
```
🚀 Starting Grafana provisioning for tenant: acme-corp
🏢 Creating Grafana organization: Tenant: acme-corp
✅ Grafana organization created: Tenant: acme-corp (orgId: 3)
🤖 Creating Grafana service account: sa-acme-corp in orgId: 3 with role: Admin
✅ Grafana service account created: sa-acme-corp (id: 5)
🔑 Creating Grafana service account token: token-acme-corp for SA: 5 in orgId: 3
✅ Grafana service account token created: token-acme-corp (key: glsa_*******)
✅ Grafana provisioning completed for tenant: acme-corp (orgId: 3, saId: 5, token: glsa_***)
```

### 4. Test #2: Ověření v DB

```bash
docker exec -it db psql -U core -d core -c "SELECT tenant_id, grafana_org_id, service_account_id, service_account_name, LEFT(service_account_token, 15) || '***' as token_preview, created_at FROM grafana_tenant_bindings;"
```

**Očekávaný výstup:**
```
 tenant_id | grafana_org_id | service_account_id | service_account_name | token_preview  |         created_at
-----------+----------------+--------------------+----------------------+----------------+----------------------------
 acme-corp |              3 |                  5 | sa-acme-corp         | glsa_xxxxxxxxxxx*** | 2024-10-13 12:34:56.789+00
```

### 5. Test #3: Ověření v Grafana UI

```bash
# Otevřít prohlížeč
open http://localhost:3000

# Přihlásit jako admin/admin
# Navigovat: Configuration → Organizations

# Měla by tam být nová organizace:
# - "Tenant: acme-corp" (ID: 3)
```

**Screenshot kontrola:**
- ✅ Organization list obsahuje "Tenant: acme-corp"
- ✅ Org ID = 3 (nebo vyšší, podle pořadí)

### 6. Test #4: Monitoring Dashboard

```bash
# 1. Přihlásit se jako user z acme-corp tenantu
#    (nebo vytvořit test usera v Keycloak admin console)

# 2. Navigovat na Dashboard → Monitoring

# 3. Dashboard by měl načíst data (NE spinner!)
```

**Očekávané chování:**
- ✅ Dashboard se načte
- ✅ Grafy zobrazují data (nebo "No data" pokud žádné logy)
- ❌ NENÍ zobrazeno: Loading spinner nebo error "Grafana org not configured"

### 7. Test #5: Error Handling

```bash
# Zastavit Grafana
docker stop grafana

# Zkusit vytvořit tenant
curl -X POST https://admin.core-platform.local/api/admin/tenants \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "test-fail",
    "displayName": "Test Failure"
  }'

# Sledovat logy
docker logs backend | tail -20
```

**Očekávaný výstup:**
```
⚠️ Grafana provisioning failed for tenant: test-fail (tenant created but monitoring unavailable)
✅ Tenant created successfully: test-fail (realm_id: xxx)
```

**Důležité:**
- ✅ Tenant SE VYTVOŘÍ (je v DB)
- ⚠️ Grafana binding NENÍ vytvořen
- ✅ Keycloak realm EXISTUJE

```bash
# Spustit Grafana zpět
docker start grafana
```

### 8. Test #6: Smazání Tenantu

```bash
curl -X DELETE https://admin.core-platform.local/api/admin/tenants/acme-corp \
  -H "Authorization: Bearer $TOKEN"
```

**Sledovat logy:**
```bash
docker logs -f backend | grep -i grafana
```

**Očekávaný výstup:**
```
🗑️ Starting Grafana deprovisioning for tenant: acme-corp
🗑️ Deleting Grafana organization: 3
✅ Grafana organization deleted: 3
✅ Grafana deprovisioning completed for tenant: acme-corp (orgId: 3)
```

**Ověřit v DB:**
```sql
SELECT * FROM grafana_tenant_bindings WHERE tenant_id = 'acme-corp';
-- (no rows)
```

**Ověřit v Grafana UI:**
- ✅ Organization "Tenant: acme-corp" JIŽ NENÍ v seznamu

---

## 🔥 Pro Existující Tenanty (core-platform, test-tenant)

Tyto tenanty NEMAJÍ Grafana binding, proto dashboard zobrazuje spinner.

### Řešení A: Manual Provision (Doporučeno)

```bash
# 1. Vytvořit Grafana orgs a SA manuálně v Grafana UI
#    - Organization: "Tenant: core-platform" → Org ID 1
#    - Service Account: "sa-core-platform" → SA ID 1
#    - Token: vygenerovat → Copy

# 2. INSERT do DB
docker exec -it db psql -U core -d core

INSERT INTO grafana_tenant_bindings 
  (tenant_id, grafana_org_id, service_account_id, service_account_name, service_account_token, created_at, updated_at)
VALUES 
  ('core-platform', 1, 1, 'sa-core-platform', 'glsa_PASTE_REAL_TOKEN_HERE', NOW(), NOW()),
  ('test-tenant', 2, 2, 'sa-test-tenant', 'glsa_PASTE_REAL_TOKEN_HERE', NOW(), NOW());
```

### Řešení B: Provision přes API (Rychlejší)

```java
// Přidat dočasný endpoint do TenantManagementController:

@PostMapping("/admin/tenants/{tenantKey}/provision-grafana")
public ResponseEntity<Map<String, Object>> provisionGrafana(@PathVariable String tenantKey) {
    GrafanaTenantBinding binding = grafanaProvisioningService.provisionTenant(tenantKey);
    return ResponseEntity.ok(Map.of(
        "success", true,
        "orgId", binding.getGrafanaOrgId(),
        "serviceAccountId", binding.getServiceAccountId()
    ));
}
```

Pak zavolat:
```bash
curl -X POST https://admin.core-platform.local/api/admin/tenants/core-platform/provision-grafana \
  -H "Authorization: Bearer $TOKEN"
```

---

## ✅ Checklist Po Testování

- [ ] Backend se spustil bez chyb
- [ ] Flyway migration V3 byla aplikována
- [ ] Tabulka `grafana_tenant_bindings` existuje
- [ ] Nový tenant vytvořen → Grafana org vytvořena
- [ ] Binding v DB existuje
- [ ] Grafana UI zobrazuje novou organizaci
- [ ] Dashboard nového tenantu funguje (ne spinner)
- [ ] Smazání tenantu → Grafana org smazána
- [ ] Binding z DB odstraněn
- [ ] Error handling funguje (tenant se vytvoří i když Grafana padne)

---

## 🐛 Troubleshooting

### Backend Nespadl, ale Grafana Org Není Vytvořena

**Kontrola:**
```bash
# 1. Zkontrolovat Grafana dostupnost
curl -u admin:admin http://localhost:3000/api/health

# 2. Zkontrolovat credentials
grep GRAFANA .env

# 3. Zkontrolovat backend logy
docker logs backend 2>&1 | grep -i "grafana.*error"
```

### Dashboard Pořád Spinner

**Možné příčiny:**
1. Token je špatný → Zkontrolovat v DB, vygenerovat nový
2. Service Account nemá permissions → Zkontrolovat v Grafana UI
3. Org ID je špatně → Zkontrolovat binding vs. Grafana UI

**Řešení:**
```sql
-- Smazat binding
DELETE FROM grafana_tenant_bindings WHERE tenant_id = 'tenant-key';

-- Re-provision
-- (vytvořit tenant znovu NEBO zavolat provision endpoint)
```

### Circuit Breaker Open

```bash
# Počkat 60 sekund nebo restartovat backend
docker restart backend
```

---

**Status:** ✅ Ready to Test  
**Datum:** 2024-10-13  
**Next Step:** Spustit backend a testovat!
