# 🏢 Grafana Multi-Tenancy Integration Guide

**Datum:** 2025-10-09  
**Status:** 📋 Architecture Documentation

## 🎯 Požadavky

1. **Backend synchronizace** - Uživatelé s MONITORING rolemi ze všech realmů se synchronizují do Grafany
2. **Frontend přístup** - Uživatelé ze všech realmů se mohou přihlásit do Grafany
3. **Iframe embedding** - Grafana funguje v iframe s JWT autentizací
4. **Data isolation** - Každý tenant vidí jen svá data

## 🏗️ Aktuální Architektura

### Backend Sync (✅ FUNGUJE PRO VŠECHNY REALMY)

```
Keycloak (admin realm)
Keycloak (test-tenant realm)  
Keycloak (company-b realm)
         ↓
    CDC Events (change_events table)
         ↓
GrafanaUserSyncService
    realm(realmId).users().get(userId)  ← Dynamický realm!
         ↓
Grafana Admin API
    create/update user in Grafana
```

**✅ Funguje správně:**
```java
// Backend čte z libovolného realmu
UserRepresentation user = keycloakAdminClient
    .realm(realmId)  // ← test-tenant, company-b, admin...
    .users()
    .get(userId)
    .toRepresentation();
```

### Frontend Auth (⚠️ OMEZENÍ - JEN ADMIN REALM)

```yaml
# Grafana OAuth - HARDCODED na admin realm
GF_AUTH_GENERIC_OAUTH_AUTH_URL=.../realms/admin/auth
GF_AUTH_GENERIC_OAUTH_TOKEN_URL=.../realms/admin/token

# JWT validace - HARDCODED na admin realm
GF_AUTH_JWT_JWK_SET_URL=.../realms/admin/certs
```

**❌ Problém:**
- Uživatelé z `test-tenant` nebo `company-b` realmů **nemohou** použít OAuth login
- JWT tokeny z jiných realmů **selžou** na validaci

## 🔍 Proč je to problém?

### Grafana Limitations

**Grafana 10.4 nepodporuje:**
- ❌ Multiple OAuth providers (nelze mít "Login via tenant-a" a "Login via tenant-b")
- ❌ Dynamic JWK URL based on JWT claims
- ❌ Realm selection UI

**Grafana Enterprise má:**
- ✅ Multi-tenancy plugin ($$$ placené)
- ✅ Team-based isolation

## 💡 Řešení

### **Doporučení: Unified Admin Realm** ⭐

**Princip:**
- Všichni monitoring uživatelé jsou v **admin realmu**
- Tenant affiliation je v JWT claims (tenant_id, tenant_key)
- Grafana data isolation přes **Organizations** nebo **Teams**

**Implementace:**

#### 1. Keycloak Setup

```
admin realm:
  users:
    - admin@core.local (CORE_ROLE_ADMIN)
    - monitor-tenant-a@core.local (CORE_ROLE_TENANT_MONITORING)
      attributes:
        tenant_id: "tenant-a"
        tenant_key: "test-tenant"
    - monitor-tenant-b@core.local (CORE_ROLE_TENANT_MONITORING)
      attributes:
        tenant_id: "tenant-b"
        tenant_key: "company-b"

test-tenant realm:
  users:
    - user1@tenant-a.com (business users)
    - user2@tenant-a.com

company-b realm:
  users:
    - user1@company-b.com (business users)
```

**Rozdělení:**
- **Admin realm** = Monitoring users (access Grafana)
- **Tenant realms** = Business users (access core platform)

#### 2. Grafana Config (NO CHANGE NEEDED)

```yaml
grafana:
  environment:
    # OAuth na admin realm
    - GF_AUTH_GENERIC_OAUTH_AUTH_URL=https://admin.${DOMAIN}/realms/admin/auth
    - GF_AUTH_GENERIC_OAUTH_TOKEN_URL=https://keycloak:8443/realms/admin/token
    
    # JWT validace z admin realmu
    - GF_AUTH_JWT_JWK_SET_URL=https://keycloak:8443/realms/admin/certs
    
    # Role mapping (už máme)
    - GF_AUTH_JWT_ROLE_ATTRIBUTE_PATH=contains(realm_access.roles[*], 'CORE_ROLE_ADMIN') && 'Admin' || ...
```

#### 3. GrafanaUserSyncService (AKTUALIZOVAT)

```java
public void handleUserRoleChange(Map<String, Object> event) {
    String userId = (String) event.get("entity_id");
    String realmId = (String) event.get("realm_id");
    
    // ✅ NOVÉ: Sync jen z admin realmu
    if (!"admin".equals(realmId)) {
        log.debug("Skipping Grafana sync for non-admin realm: {}", realmId);
        return;
    }
    
    // Pokračovat se synchronizací...
    UserRepresentation user = getUserFromKeycloak(userId, realmId);
    // ...
}
```

#### 4. Grafana Data Isolation

**Pomocí Dashboards Folders + Permissions:**

```javascript
// Dashboard provisioning
{
  "title": "Tenant A Monitoring",
  "uid": "tenant-a-overview",
  "folder": "Tenant A",
  "permissions": [
    {
      "role": "Viewer",
      "permission": 1  // View
    }
  ],
  "templating": {
    "list": [
      {
        "name": "tenant",
        "type": "constant",
        "current": {
          "value": "test-tenant"
        }
      }
    ]
  }
}
```

**Loki Queries s Tenant Filter:**

```promql
{service="backend", tenant="${tenant}"}
```

### Alternativa: Multi-Realm Support (KOMPLEXNÍ)

Pokud **MUSÍME** podporovat přihlášení z více realmů:

#### 1. Frontend Realm Selector

```typescript
// Frontend pre-login
const tenant = await detectTenant(); // z URL, subdomény, nebo user selection

// Redirect na správný Keycloak realm
const realmAuthUrl = `https://admin.${domain}/realms/${tenant}/auth?...`;
window.location = realmAuthUrl;
```

#### 2. Grafana s Reverse Proxy

```nginx
# Nginx routes requests based on tenant header
location /monitoring {
    if ($http_x_tenant = "test-tenant") {
        proxy_pass http://grafana:3000;
        proxy_set_header X-Auth-Realm "test-tenant";
    }
    if ($http_x_tenant = "company-b") {
        proxy_pass http://grafana:3000;
        proxy_set_header X-Auth-Realm "company-b";
    }
}
```

#### 3. Custom Grafana Auth Proxy

Vytvoříme vlastní auth proxy, který:
- Přijme JWT z libovolného realmu
- Validuje proti správnému JWK (podle `iss` claim)
- Přeloží do Grafana formátu
- Předá jako proxy header

**⚠️ Toto je VELMI komplexní a nedoporučuje se pro prvotní release.**

## 📊 Porovnání Variant

| Feature | Unified Admin Realm | Multi-Realm Support |
|---------|-------------------|---------------------|
| Complexity | ⭐ Low | ⭐⭐⭐⭐ High |
| Keycloak Setup | Simple | Complex |
| Grafana Config | Static | Dynamic |
| User Management | Admin realm only | All realms |
| Iframe Embedding | ✅ Works | ⚠️ Complicated |
| Data Isolation | Folders/Teams | Organizations |
| Maintenance | Easy | Difficult |

## 🎯 Doporučení

### Fáze 1: MVP (AKTUÁLNÍ)
✅ Unified Admin Realm
- Monitoring users v admin realmu
- Business users v tenant realmech
- Jednoduchá implementace
- Rychlé nasazení

### Fáze 2: Production (BUDOUCNOST)
🔄 Zvážit Grafana Enterprise
- Multi-tenancy plugin
- Advanced RBAC
- Better data isolation

### Fáze 3: Custom Solution (POKUD NUTNÉ)
🛠️ Custom auth proxy
- Jen pokud Grafana Enterprise není option
- High maintenance cost
- Vyžaduje dedikovaný tým

## 🔧 Implementační Kroky

### Krok 1: Vyčistit monitoring users (HOTFIX)

```sql
-- Najít monitoring users v non-admin realmech
SELECT u.*, r.name as role_name, ur.realm_id
FROM users_directory u
JOIN user_roles ur ON u.keycloak_user_id = ur.user_id
JOIN roles r ON ur.role_id = r.id
WHERE r.name IN ('CORE_ROLE_MONITORING', 'CORE_ROLE_TENANT_MONITORING', 'CORE_ROLE_ADMIN')
  AND ur.realm_id != 'admin';

-- Tyto uživatele buď:
-- A) Přesunout do admin realmu (Keycloak Admin Console)
-- B) Nebo duplikovat s role mapping
```

### Krok 2: Upravit GrafanaUserSyncService

```java
// Add realm filter
if (!"admin".equals(realmId)) {
    log.debug("Skipping Grafana sync for non-admin realm: {}", realmId);
    return;
}
```

### Krok 3: Dokumentovat pro team

```markdown
# Grafana Access Policy

## Who can access Grafana?
- Users with CORE_ROLE_MONITORING in **admin realm**
- Users with CORE_ROLE_ADMIN in **admin realm**
- Users with CORE_ROLE_TENANT_MONITORING in **admin realm**

## How to grant Grafana access?
1. Create user in **admin realm** (not tenant realm!)
2. Assign monitoring role
3. Set tenant_key attribute (for data isolation)
4. Wait for CDC sync (~10s)
5. User can login via Keycloak SSO

## Tenant data isolation?
- Use Loki label filters: {tenant="test-tenant"}
- Use Grafana folder permissions
- Use dashboard variables
```

## 🧪 Testing Checklist

- [ ] Admin realm user s CORE_ROLE_MONITORING → sync do Grafany
- [ ] Test-tenant realm user s CORE_ROLE_MONITORING → SKIP sync
- [ ] OAuth login z admin realmu → SUCCESS
- [ ] JWT token z admin realmu v iframe → SUCCESS
- [ ] JWT token z test-tenant realmu v iframe → FAIL (očekáváno)
- [ ] Loki query s tenant filter → funguje
- [ ] Dashboard folder permissions → funguje

## 📚 Související Dokumentace

- [GRAFANA_USER_SYNC_ARCHITECTURE.md](./GRAFANA_USER_SYNC_ARCHITECTURE.md)
- [MULTITENANCY_ARCHITECTURE.md](./MULTITENANCY_ARCHITECTURE.md)
- [GRAFANA_INTEGRATION.md](./GRAFANA_INTEGRATION.md)

---

**Status:** 📋 Architecture Documented  
**Decision:** Use Unified Admin Realm (MVP)  
**Owner:** Martin Horak + AI Assistant  
**Review Date:** 2025-10-09
