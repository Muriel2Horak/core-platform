# 📦 Implementace Automatického Grafana Provisioningu - Shrnutí

**Datum:** 2024-10-13  
**Status:** ✅ Implementováno, připraveno k testování  
**Autor:** GitHub Copilot + Martin Horák

---

## 🎯 Cíl

Automatizovat zakládání Grafana organizací, service accounts a tokenů při vytváření nových tenantů, aby se eliminovala nutnost manuální konfigurace a restartů.

## 📋 Problém

**Původní stav:**
- Hardcoded tenant-to-org mapping v `TenantOrgServiceImpl.init()`
- Tokeny načítány z environment variables (`GRAFANA_SAT_CORE_PLATFORM`, atd.)
- Placeholder tokeny pro development (`glsa_dev_placeholder_*`)
- Při vytvoření nového tenantu nutné:
  1. Manuálně vytvořit Grafana org
  2. Vytvořit service account
  3. Vygenerovat token
  4. Přidat env var do `.env`
  5. Restartovat backend

**Výsledek:** Dashboard loading spinner, nepracovní monitoring pro nové tenanty

---

## ✅ Implementované Komponenty

### 1. 🔧 **GrafanaAdminClient.java**
**Umístění:** `backend/src/main/java/cz/muriel/core/monitoring/grafana/GrafanaAdminClient.java`

**Funkce:**
- REST klient pro Grafana Admin API
- Circuit breaker pro resilience (Resilience4j)
- Basic Auth authentication

**Metody:**
```java
CreateOrgResponse createOrganization(String orgName)
CreateServiceAccountResponse createServiceAccount(Long orgId, String name, String role)
CreateServiceAccountTokenResponse createServiceAccountToken(Long orgId, Long saId, String tokenName)
void deleteOrganization(Long orgId)
List<ServiceAccountInfo> listServiceAccounts(Long orgId)
```

**Konfigurace:**
- `grafana.admin.url` - Grafana URL (default: http://grafana:3000)
- `grafana.admin.username` - Admin username (default: admin)
- `grafana.admin.password` - Admin password (default: admin)

---

### 2. 🚀 **GrafanaProvisioningService.java**
**Umístění:** `backend/src/main/java/cz/muriel/core/monitoring/grafana/GrafanaProvisioningService.java`

**Funkce:**
- Orchestrace provisioning workflow
- Transactionální zpracování (@Transactional)
- Error handling s graceful degradation

**Hlavní metody:**
```java
@Transactional
GrafanaTenantBinding provisionTenant(String tenantId)

@Transactional
void deprovisionTenant(String tenantId)

GrafanaTenantBinding getTenantBinding(String tenantId)
boolean isTenantProvisioned(String tenantId)
```

**Workflow `provisionTenant()`:**
1. ✅ Check if already provisioned
2. 🏢 Create Grafana organization (name: "Tenant: {tenantId}")
3. 🤖 Create service account (name: "sa-{tenantId}", role: Admin)
4. 🔑 Generate service account token (name: "token-{tenantId}")
5. 💾 Save binding to database

**Konfigurace:**
- `grafana.provisioning.enabled` - Enable/disable provisioning (default: true)
- `grafana.provisioning.service-account-role` - SA role (default: Admin)

---

### 3. 🗄️ **GrafanaTenantBinding.java** (Entity)
**Umístění:** `backend/src/main/java/cz/muriel/core/monitoring/grafana/entity/GrafanaTenantBinding.java`

**Schema:**
```java
@Entity
@Table(name = "grafana_tenant_bindings")
class GrafanaTenantBinding {
  Long id;
  String tenantId;        // UNIQUE
  Long grafanaOrgId;
  Long serviceAccountId;
  String serviceAccountName;
  String serviceAccountToken;  // ⚠️ Plaintext (TODO: encrypt)
  Instant createdAt;
  Instant updatedAt;
}
```

**Indexes:**
- `idx_tenant_id` (unique)
- `idx_grafana_org_id`

---

### 4. 📂 **GrafanaTenantBindingRepository.java**
**Umístění:** `backend/src/main/java/cz/muriel/core/monitoring/grafana/repository/GrafanaTenantBindingRepository.java`

**Metody:**
```java
Optional<GrafanaTenantBinding> findByTenantId(String tenantId)
Optional<GrafanaTenantBinding> findByGrafanaOrgId(Long grafanaOrgId)
void deleteByTenantId(String tenantId)
boolean existsByTenantId(String tenantId)
```

---

### 5. 🔄 **TenantOrgServiceImpl.java** (Refactored)
**Umístění:** `backend/src/main/java/cz/muriel/core/monitoring/bff/service/TenantOrgServiceImpl.java`

**Změny:**
- ❌ Odstraněn `@PostConstruct init()` + `loadTenantMapping()`
- ❌ Odstraněn `Map<String, TenantBinding> tenantOrgMap`
- ✅ Přidána dependency: `GrafanaTenantBindingRepository`
- ✅ Implementace: `@Cacheable` dynamic resolution z DB

**Nová implementace `resolve()`:**
```java
@Cacheable(value = "tenantOrgBindings", key = "#tenantId")
public TenantBinding resolve(Jwt jwt) {
  String tenantId = extractTenantId(jwt);
  GrafanaTenantBinding binding = bindingRepository.findByTenantId(tenantId)
    .orElseThrow(() -> new IllegalStateException("Grafana org not configured"));
  return new TenantBinding(binding.getTenantId(), binding.getGrafanaOrgId(), binding.getServiceAccountToken());
}
```

---

### 6. 🏢 **KeycloakRealmManagementService.java** (Updated)
**Umístění:** `backend/src/main/java/cz/muriel/core/service/KeycloakRealmManagementService.java`

**Změny:**
- ✅ Přidána dependency: `GrafanaProvisioningService`
- ✅ Hook v `createTenant()` - step 7: Automatic Grafana provisioning
- ✅ Hook v `deleteTenant()` - step 1: Automatic Grafana deprovisioning

**createTenant() - nový krok:**
```java
// 7. 🚀 AUTOMATIC GRAFANA PROVISIONING
try {
  grafanaProvisioningService.provisionTenant(tenantKey);
  log.info("✅ Grafana provisioning completed for tenant: {}", tenantKey);
} catch (Exception e) {
  log.error("⚠️ Grafana provisioning failed (tenant created but monitoring unavailable)", e);
  // Don't fail entire tenant creation
}
```

**deleteTenant() - nový krok:**
```java
// 1. 🗑️ AUTOMATIC GRAFANA DEPROVISIONING
try {
  grafanaProvisioningService.deprovisionTenant(tenantKey);
  log.info("✅ Grafana deprovisioning completed for tenant: {}", tenantKey);
} catch (Exception e) {
  log.error("⚠️ Grafana deprovisioning failed (continuing with tenant deletion)", e);
}
```

---

### 7. 🗃️ **V3__grafana_tenant_bindings.sql** (Flyway Migration)
**Umístění:** `backend/src/main/resources/db/migration/V3__grafana_tenant_bindings.sql`

**SQL:**
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

**Status:** ⏳ Bude aplikováno při příštím startu backendu

---

### 8. ⚙️ **application.properties** (Updated)
**Umístění:** `backend/src/main/resources/application.properties`

**Nová konfigurace:**
```properties
# ====== GRAFANA PROVISIONING CONFIGURATION ======
# 🚀 Automatic Grafana organization and service account provisioning for new tenants

# Grafana Admin API connection (internal Docker network)
grafana.admin.url=${GRAFANA_ADMIN_URL:http://grafana:3000}
grafana.admin.username=${GRAFANA_ADMIN_USERNAME:admin}
grafana.admin.password=${GRAFANA_ADMIN_PASSWORD:admin}

# Provisioning settings
grafana.provisioning.enabled=${GRAFANA_PROVISIONING_ENABLED:true}
grafana.provisioning.service-account-role=${GRAFANA_SA_ROLE:Admin}
```

---

### 9. 📦 **DTO Classes**
**Umístění:** `backend/src/main/java/cz/muriel/core/monitoring/grafana/dto/`

**Soubory:**
- `CreateOrgRequest.java` - Request pro vytvoření org
- `CreateOrgResponse.java` - Response s orgId
- `CreateServiceAccountRequest.java` - Request pro SA
- `CreateServiceAccountResponse.java` - Response s SA ID
- `CreateServiceAccountTokenRequest.java` - Request pro token
- `CreateServiceAccountTokenResponse.java` - Response s tokenem
- `ServiceAccountInfo.java` - Info o SA
- `ServiceAccountSearchResponse.java` - List SA response

---

### 10. 🚨 **Exception Classes**
**Umístění:** `backend/src/main/java/cz/muriel/core/monitoring/grafana/`

**Soubory:**
- `GrafanaApiException.java` - Chyby při komunikaci s Grafana API
- `GrafanaProvisioningException.java` - Chyby při provisioning workflow

---

### 11. 📚 **GRAFANA_PROVISIONING_README.md**
**Umístění:** `GRAFANA_PROVISIONING_README.md`

**Obsah:**
- Architektura a workflow
- Konfigurace
- Testování
- Troubleshooting
- Security considerations
- Migration guide
- API reference
- Budoucí vylepšení

---

## 📊 Souhrn Změn

### Nové Soubory (15)
```
backend/src/main/java/cz/muriel/core/monitoring/grafana/
  ├── GrafanaAdminClient.java
  ├── GrafanaProvisioningService.java
  ├── GrafanaApiException.java
  ├── GrafanaProvisioningException.java
  ├── dto/
  │   ├── CreateOrgRequest.java
  │   ├── CreateOrgResponse.java
  │   ├── CreateServiceAccountRequest.java
  │   ├── CreateServiceAccountResponse.java
  │   ├── CreateServiceAccountTokenRequest.java
  │   ├── CreateServiceAccountTokenResponse.java
  │   ├── ServiceAccountInfo.java
  │   └── ServiceAccountSearchResponse.java
  ├── entity/
  │   └── GrafanaTenantBinding.java
  └── repository/
      └── GrafanaTenantBindingRepository.java

backend/src/main/resources/db/migration/
  └── V3__grafana_tenant_bindings.sql

GRAFANA_PROVISIONING_README.md
```

### Upravené Soubory (3)
```
backend/src/main/java/cz/muriel/core/monitoring/bff/service/
  └── TenantOrgServiceImpl.java                    (refactored)

backend/src/main/java/cz/muriel/core/service/
  └── KeycloakRealmManagementService.java         (hooks added)

backend/src/main/resources/
  └── application.properties                       (config added)
```

---

## 🧪 Testovací Plán

### Před spuštěním

1. **Zkontrolovat Grafana admin credentials:**
   ```bash
   grep GRAFANA .env
   # GRAFANA_ADMIN_PASSWORD=admin (nebo vaše heslo)
   ```

2. **Zkontrolovat Grafana dostupnost:**
   ```bash
   docker ps | grep grafana
   curl -u admin:admin http://localhost:3000/api/health
   ```

### Testovací Scénáře

#### ✅ Test 1: Vytvoření nového tenantu
```bash
# 1. Přihlásit se jako admin
# 2. POST /api/admin/tenants
{
  "key": "test-company",
  "displayName": "Test Company"
}
# 3. Zkontrolovat backend logy
docker logs -f backend | grep -i grafana
# Očekávaný výstup:
# ✅ Grafana organization created: test-company (orgId: 3)
# ✅ Grafana service account created: sa-test-company (id: 5)
# ✅ Grafana provisioning completed for tenant: test-company
```

#### ✅ Test 2: Ověření v databázi
```sql
SELECT * FROM grafana_tenant_bindings WHERE tenant_id = 'test-company';
```

#### ✅ Test 3: Ověření v Grafana UI
```
1. Otevřít http://localhost:3000
2. Přihlásit jako admin
3. Configuration → Organizations
4. Měla by tam být "Tenant: test-company"
```

#### ✅ Test 4: Monitoring Dashboard
```
1. Přihlásit se jako user z test-company
2. Otevřít Dashboard → Monitoring
3. Dashboard by měl načíst data (ne spinner)
```

#### ✅ Test 5: Smazání tenantu
```bash
# DELETE /api/admin/tenants/test-company
# Zkontrolovat:
# - Grafana org smazána
# - Binding v DB smazán
```

---

## 🚀 Deployment Postup

### 1. Zastavit backend
```bash
docker stop backend
```

### 2. Rebuild backend image
```bash
cd /Users/martinhorak/Projects/core-platform
docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml build backend
```

### 3. Spustit backend
```bash
docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml up -d backend
```

### 4. Sledovat startup
```bash
docker logs -f backend
# Čekat na:
# ✅ Started BackendApplication in X seconds
# ✅ Flyway migration V3 applied
```

### 5. Verifikovat migraci
```bash
docker exec -it db psql -U core -d core -c "SELECT * FROM flyway_schema_history WHERE version = '3';"
```

### 6. Testovat provisioning
- Vytvořit testovacího tenantu přes admin UI
- Zkontrolovat backend logy
- Verifikovat v Grafana UI

---

## ⚠️ Známé Limity

1. **Token Security:**
   - ⚠️ Tokeny v DB nejsou šifrovány (plaintext)
   - 💡 TODO: Implementovat encryption at rest

2. **Error Recovery:**
   - ⚠️ Pokud provisioning selže, tenant se vytvoří bez Grafany
   - ✅ Nepřeruší vytvoření tenantu (graceful degradation)
   - 💡 Можno manually zavolat provisioning později

3. **Token Rotation:**
   - ❌ Není implementována automatická rotace
   - 💡 TODO: Scheduled job pro refresh

4. **Concurrent Provisioning:**
   - ⚠️ Concurrent vytváření stejného tenantu může způsobit race condition
   - ✅ UNIQUE constraint na `tenant_id` to zachytí
   - 💡 TODO: Distributed lock (Redis)

---

## 📈 Metriky a Monitoring

### Backend Logs
```bash
# Real-time provisioning monitoring
docker logs -f backend | grep -E "Grafana (provisioning|organization|service account)"
```

### Actuator Metrics
```bash
# Circuit breaker state
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.state

# HTTP client metrics
curl http://localhost:8080/actuator/metrics/http.client.requests | jq
```

### Database Monitoring
```sql
-- Počet provisionovaných tenantů
SELECT COUNT(*) FROM grafana_tenant_bindings;

-- Nejnovější provisionované tenanty
SELECT tenant_id, grafana_org_id, created_at 
FROM grafana_tenant_bindings 
ORDER BY created_at DESC 
LIMIT 10;
```

---

## 🔄 Rollback Plán

Pokud by nastal problém:

### 1. Disable Provisioning
```bash
# V .env
GRAFANA_PROVISIONING_ENABLED=false

# Restart backend
docker restart backend
```

### 2. Revert to Static Init (Emergency)
```bash
git revert HEAD
docker compose build backend
docker compose up -d backend
```

### 3. Manual Cleanup
```sql
-- Smazat všechny bindings
DELETE FROM grafana_tenant_bindings;

-- Rollback Flyway migration
DELETE FROM flyway_schema_history WHERE version = '3';
DROP TABLE grafana_tenant_bindings;
```

---

## ✅ Hotovo - Co Dál?

1. **Immediate:** Testování na development prostředí
2. **Short-term:** Token encryption implementation
3. **Mid-term:** Automatic token rotation
4. **Long-term:** Multi-region Grafana support

---

## 📞 Support

**Chyby?** Zkontrolovat:
1. Backend logy: `docker logs backend | grep -i grafana`
2. Grafana logy: `docker logs grafana`
3. Database: `SELECT * FROM grafana_tenant_bindings;`
4. README: `GRAFANA_PROVISIONING_README.md`

**Kontakt:** Core Platform Team

---

**Status:** ✅ Ready for Testing  
**Verze:** 1.0.0  
**Datum:** 2024-10-13
