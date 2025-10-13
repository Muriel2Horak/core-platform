# 🚀 Automatický Grafana Provisioning

## 📋 Přehled

Tento systém automaticky vytváří a spravuje Grafana organizace, service accounts a přístupové tokeny při zakládání nových tenantů v platformě.

## 🏗️ Architektura

### Komponenty

1. **GrafanaAdminClient** - REST klient pro Grafana Admin API
   - Vytváření/mazání organizací
   - Správa service accounts
   - Generování tokenů
   - Circuit breaker pro odolnost vůči výpadkům

2. **GrafanaProvisioningService** - Orchestrace provisioning logiky
   - Kompletní provisioning workflow
   - Transactionální zpracování
   - Error handling a fallback

3. **GrafanaTenantBinding** (Entity) - Perzistentní storage
   - Tenant ID
   - Grafana Org ID
   - Service Account ID
   - Service Account Token
   - Časové značky

4. **GrafanaTenantBindingRepository** - JPA repository
   - CRUD operace
   - Query metody

5. **TenantOrgServiceImpl** - Refaktorovaná služba
   - Dynamické načítání z DB (místo static init)
   - Cache podpora (@Cacheable)

## 🔄 Workflow

### Vytvoření Tenantu

```
User → TenantManagementController 
     → KeycloakRealmManagementService.createTenant()
     → TenantService.createTenantRegistryWithRealmId()
     → GrafanaProvisioningService.provisionTenant() ← 🚀 NOVÉ
```

**Kroky provisioning:**

1. ✅ Zkontroluje, jestli už binding existuje
2. 🏢 Vytvoří Grafana organizaci (název: "Tenant: {tenantId}")
3. 🤖 Vytvoří service account v organizaci (název: "sa-{tenantId}")
4. 🔑 Vygeneruje service account token (název: "token-{tenantId}")
5. 💾 Uloží binding do databáze

### Smazání Tenantu

```
User → TenantManagementController
     → KeycloakRealmManagementService.deleteTenant()
     → GrafanaProvisioningService.deprovisionTenant() ← 🚀 NOVÉ
     → KeycloakAdminService.deleteRealm()
     → TenantService.deleteTenantFromRegistry()
```

**Kroky deprovisioning:**

1. 🔍 Najde binding v databázi
2. 🗑️ Smaže Grafana organizaci (kaskádově smaže SA + tokeny)
3. 💾 Smaže binding z databáze

## 📊 Databázové Schema

```sql
CREATE TABLE grafana_tenant_bindings (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL UNIQUE,
    grafana_org_id BIGINT NOT NULL,
    service_account_id BIGINT NOT NULL,
    service_account_name VARCHAR(200) NOT NULL,
    service_account_token VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_grafana_tenant_bindings_tenant_id ON grafana_tenant_bindings(tenant_id);
CREATE INDEX idx_grafana_tenant_bindings_grafana_org_id ON grafana_tenant_bindings(grafana_org_id);
```

Migrace: `V3__grafana_tenant_bindings.sql`

## ⚙️ Konfigurace

### application.properties

```properties
# Grafana Admin API connection (internal Docker network)
grafana.admin.url=${GRAFANA_ADMIN_URL:http://grafana:3000}
grafana.admin.username=${GRAFANA_ADMIN_USERNAME:admin}
grafana.admin.password=${GRAFANA_ADMIN_PASSWORD:admin}

# Provisioning settings
grafana.provisioning.enabled=${GRAFANA_PROVISIONING_ENABLED:true}
grafana.provisioning.service-account-role=${GRAFANA_SA_ROLE:Admin}
```

### Environment Variables (.env)

```bash
# Grafana Admin Credentials
GRAFANA_ADMIN_URL=http://grafana:3000
GRAFANA_ADMIN_USERNAME=admin
GRAFANA_ADMIN_PASSWORD=admin

# Provisioning Control
GRAFANA_PROVISIONING_ENABLED=true
GRAFANA_SA_ROLE=Admin
```

## 🧪 Testování

### 1. Vytvoření nového tenantu

```bash
# Přihlásit se jako admin
TOKEN="eyJhbGc..."

# Vytvořit tenant
curl -X POST https://admin.core-platform.local/api/admin/tenants \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "new-company",
    "displayName": "New Company Inc."
  }'
```

**Očekávaný výsledek:**

- ✅ Keycloak realm vytvořen
- ✅ Tenant v DB registrován
- ✅ Grafana organizace vytvořena (org ID např. 3)
- ✅ Service account vytvořen
- ✅ Token vygenerován
- ✅ Binding uložen v `grafana_tenant_bindings`

### 2. Ověření v Grafana UI

1. Otevřít http://localhost:3000
2. Přihlásit se jako admin
3. Configuration → Organizations
4. Měla by tam být nová organizace "Tenant: new-company"

### 3. Ověření v databázi

```sql
SELECT * FROM grafana_tenant_bindings 
WHERE tenant_id = 'new-company';
```

Výstup:
```
id | tenant_id   | grafana_org_id | service_account_id | service_account_name | service_account_token  | created_at | updated_at
---+-------------+----------------+--------------------+----------------------+------------------------+------------+------------
 1 | new-company |              3 |                  5 | sa-new-company       | glsa_...               | 2024-...   | 2024-...
```

### 4. Test Monitoring Dashboard

1. Přihlásit se jako user z `new-company` tenantu
2. Otevřít Dashboard → Monitoring
3. Dashboard by měl načíst data z Grafana org ID 3
4. Logy z Loki by měly být viditelné

## 🔍 Troubleshooting

### Chyba: "Grafana service unavailable"

**Příčina:** Grafana není dostupná na `http://grafana:3000`

**Řešení:**
```bash
# Zkontrolovat Grafana container
docker ps | grep grafana

# Zkontrolovat logy
docker logs grafana

# Zkontrolovat network
docker network inspect docker_core-net
```

### Chyba: "Authentication failed"

**Příčina:** Špatné admin credentials

**Řešení:**
```bash
# Zkontrolovat .env
grep GRAFANA .env

# Zkontrolovat Grafana admin heslo
docker exec grafana grafana-cli admin reset-admin-password newpassword
```

### Chyba: "Grafana binding already exists"

**Příčina:** Tenant byl vytvořen dříve

**Řešení:**
```sql
-- Zkontrolovat existující binding
SELECT * FROM grafana_tenant_bindings WHERE tenant_id = 'tenant-id';

-- Smazat (pokud je to test)
DELETE FROM grafana_tenant_bindings WHERE tenant_id = 'tenant-id';
```

### Chyba: "Circuit breaker open"

**Příčina:** Příliš mnoho chyb, circuit breaker otevřen

**Řešení:**
```bash
# Počkat 60 sekund (circuit breaker se sám zavře)
# Nebo restartovat backend
docker restart backend
```

### Dashboard se pořád točí (spinner)

**Příčina:** Token je špatný nebo service account nemá permissions

**Řešení:**
```sql
-- Zkontrolovat token
SELECT tenant_id, grafana_org_id, 
       LEFT(service_account_token, 15) || '***' as token_preview 
FROM grafana_tenant_bindings;

-- Verifikovat v Grafana UI:
-- Configuration → Service Accounts → sa-{tenant} → Tokens
```

## 🔒 Bezpečnost

### Token Storage

- ⚠️ **PRODUCTION:** Tokeny jsou v plaintextu v DB
- 🔐 **TODO:** Implementovat encryption at rest
- 💡 **Doporučení:** Použít Vault nebo AWS Secrets Manager

### Network Security

- ✅ Grafana Admin API je dostupná pouze na Docker internal network
- ✅ Backend komunikuje s Grafana přes http://grafana:3000 (ne přes public URL)
- ✅ Service account tokeny mají minimální scope (pouze daná org)

### RBAC

- ✅ Default role pro SA: **Admin** (konfigurovatelné)
- 💡 **Production:** Zvážit nižší roli (Editor/Viewer) podle potřeby

## 📈 Monitoring

### Backend Logs

```bash
# Sledovat provisioning
docker logs -f backend | grep -i grafana

# Klíčové zprávy:
# ✅ "Grafana organization created: tenant-id (orgId: X)"
# ✅ "Grafana service account created: sa-tenant-id (id: Y)"
# ✅ "Grafana provisioning completed for tenant: tenant-id"
# ⚠️ "Grafana provisioning failed for tenant: tenant-id"
```

### Metrics (Actuator)

```bash
# Circuit breaker state
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state

# HTTP client metrics
curl http://localhost:8080/actuator/metrics/http.client.requests
```

## 🚧 Limity a Známé Problémy

1. **Token Rotation:** 
   - ❌ Není implementována automatická rotace tokenů
   - 💡 TODO: Přidat scheduled job pro refresh

2. **Encryption:**
   - ⚠️ Tokeny v DB nejsou šifrovány
   - 💡 TODO: AES-256 encryption

3. **Error Recovery:**
   - ⚠️ Pokud provisioning selže, tenant se vytvoří bez Grafany
   - ✅ Monitoring dashboard zobrazí error message
   - 💡 Можно manually zavolat provisioning později

4. **Org Limits:**
   - ⚠️ Grafana má limit na počet organizací (defaultně neomezeno, ale závisí na licenci)
   - 💡 Monitorovat `grafana_tenant_bindings` count

## 🔄 Migration z Old System

### Stará implementace (Static Init)

```java
@PostConstruct
public void init() {
  loadTenantMapping("core-platform", 1L, "GRAFANA_SAT_CORE_PLATFORM");
  loadTenantMapping("test-tenant", 2L, "GRAFANA_SAT_TEST_TENANT");
}
```

### Nová implementace (Dynamic DB)

```java
private TenantBinding resolveTenantBinding(String tenantId) {
  GrafanaTenantBinding binding = bindingRepository.findByTenantId(tenantId)
    .orElseThrow(...);
  return new TenantBinding(...);
}
```

### Migration Script

Pro existující tenanty (`core-platform`, `test-tenant`):

```sql
-- Manual INSERT pro existující tenanty
INSERT INTO grafana_tenant_bindings 
  (tenant_id, grafana_org_id, service_account_id, service_account_name, service_account_token)
VALUES 
  ('core-platform', 1, 1, 'sa-core-platform', 'YOUR_REAL_TOKEN_HERE'),
  ('test-tenant', 2, 2, 'sa-test-tenant', 'YOUR_REAL_TOKEN_HERE');
```

**NEBO** použít Grafana API k vygenerování nových tokenů:

```bash
# Pro každý existující tenant zavolat provisioning
curl -X POST http://localhost:8080/api/admin/grafana/provision/core-platform \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## 📚 API Reference

### Internal Service Methods

```java
// Provision new tenant
GrafanaTenantBinding provisionTenant(String tenantId)

// Deprovision tenant
void deprovisionTenant(String tenantId)

// Get tenant binding
GrafanaTenantBinding getTenantBinding(String tenantId)

// Check if provisioned
boolean isTenantProvisioned(String tenantId)
```

### Grafana Admin API (via GrafanaAdminClient)

```java
// Organizations
CreateOrgResponse createOrganization(String orgName)
void deleteOrganization(Long orgId)

// Service Accounts
CreateServiceAccountResponse createServiceAccount(Long orgId, String name, String role)
List<ServiceAccountInfo> listServiceAccounts(Long orgId)

// Tokens
CreateServiceAccountTokenResponse createServiceAccountToken(Long orgId, Long saId, String tokenName)
```

## 🎯 Budoucí Vylepšení

- [ ] Token encryption at rest
- [ ] Automatic token rotation
- [ ] Retry mechanism s exponential backoff
- [ ] Webhook notifikace při provisioning failures
- [ ] Admin UI pro manual provisioning/repair
- [ ] Metrics dashboard pro provisioning success rate
- [ ] Support pro custom Grafana dashboards per tenant
- [ ] Bulk provisioning API endpoint

---

**Autor:** Core Platform Team  
**Datum:** 2024-10  
**Verze:** 1.0.0
