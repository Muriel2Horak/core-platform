# Oprava: 403 Forbidden při vytváření tenantů

**Datum:** 7. října 2025  
**Status:** ✅ OPRAVENO

## 🔴 Problém

Při pokusu o vytvoření nového tenantu přes Admin UI se objevovala chyba:

```
Nepodařilo se vytvořit tenant: Failed to create tenant: 
Failed to parse realm configuration: Failed to create realm: 
403 Forbidden on POST request for "https://keycloak:8443/admin/realms"
```

## 🔍 Analýza

### Původní implementace:
- Backend používal **service account** `backend-admin-service` z `admin` realmu
- Token byl získáván pomocí `client_credentials` grant type
- Service account měl role: `manage-users`, `view-users`, `view-realm`, `manage-realm` v realm-management

### Problém:
- Pro vytváření **NOVÝCH REALMŮ** v Keycloak je potřeba být autentizován přes **MASTER realm**
- Service account v `admin` realmu nemá oprávnění vytvářet realmy na úrovni Keycloak instance
- Realm-level roles ≠ Master realm admin permissions

## ✅ Řešení

### 1. Přidána nová autentizační metoda pro Master realm

**Soubor:** `backend/src/main/java/cz/muriel/core/auth/KeycloakAdminService.java`

```java
// Nová konfigurace
@Value("${keycloak.master.username:admin}")
private String masterUsername;

@Value("${keycloak.master.password:admin123}")
private String masterPassword;

/**
 * 🔐 Get Master Realm Admin Token for realm management operations
 */
private String getMasterAdminToken() {
    // Cache token
    final String cacheKey = "master_admin_token";
    TokenCache cached = tokenCache.get(cacheKey);
    
    if (cached != null && cached.expiresAt > Instant.now().getEpochSecond() + 30) {
        return cached.token;
    }

    // Request new token from master realm
    String url = keycloakBaseUrl + "/realms/master/protocol/openid-connect/token";
    
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "password");
    form.add("client_id", "admin-cli");
    form.add("username", masterUsername);
    form.add("password", masterPassword);
    
    // ... process response and cache token
}
```

### 2. Upraveny realm management metody

```java
// Create Realm - nyní používá master token
public void createRealm(Map<String, Object> realmConfig) {
    String adminToken = getMasterAdminToken(); // ← změna
    String url = keycloakBaseUrl + "/admin/realms";
    // ... rest of implementation
}

// Delete Realm - nyní používá master token
public void deleteRealm(String realmName) {
    String adminToken = getMasterAdminToken(); // ← změna
    String url = keycloakBaseUrl + "/admin/realms/" + realmName;
    // ... rest of implementation
}

// List Realms - nyní používá master token
public List<Map<String, Object>> getAllRealms() {
    String adminToken = getMasterAdminToken(); // ← změna
    // ... rest of implementation
}
```

### 3. Přidána konfigurace

**`.env`:**
```bash
# Master realm credentials for realm management
KEYCLOAK_MASTER_USERNAME=admin
KEYCLOAK_MASTER_PASSWORD=admin123
```

**`application.properties`:**
```properties
# 🔐 Master realm credentials for realm management (create/delete realms)
keycloak.master.username=${KEYCLOAK_MASTER_USERNAME:admin}
keycloak.master.password=${KEYCLOAK_MASTER_PASSWORD:admin123}
```

## 🏗️ Architektura autentizace

Backend nyní používá **dva typy autentizace**:

### 1. Master Realm Admin (`getMasterAdminToken()`)
- **Použití:** Realm management (create/delete/list realms)
- **Credentials:** admin/admin123
- **Grant type:** password
- **Client:** admin-cli
- **Realm:** master
- **Endpoint:** `/realms/master/protocol/openid-connect/token`

### 2. Service Account (`getSecureAdminToken()`)
- **Použití:** Běžné admin operace v konkrétních realmech
- **Client:** backend-admin-service
- **Grant type:** client_credentials
- **Realm:** admin (nebo jiný cílový realm)
- **Endpoint:** `/realms/{realm}/protocol/openid-connect/token`
- **Roles:** manage-users, view-users, view-realm, manage-realm

## 🧪 Testování

### Test 1: Vytvoření tenantu
1. Přihlásit se jako admin
2. Přejít do Admin → Tenants
3. Kliknout na "Create Tenant"
4. Vyplnit formulář (klíč, název, domény)
5. Kliknout "Create"

**Očekávaný výsledek:** ✅ Tenant úspěšně vytvořen, realm vytvořen v Keycloak

### Test 2: Kontrola v Keycloak
```bash
docker exec -it core-keycloak /opt/keycloak/bin/kcadm.sh get realms \
  --server http://localhost:8080 \
  --realm master \
  --user admin \
  --password admin123
```

**Očekávaný výsledek:** Nově vytvořený realm je viditelný v seznamu

## 📊 Token Cache

Oba typy tokenů jsou cachovány samostatně:
- Master token: cache key `"master_admin_token"`
- Service account token: cache key `"admin_token"`
- TTL: token expiration - 30s (buffer)

## 🔒 Bezpečnostní poznámky

### Development:
- ✅ Master credentials v .env souboru
- ✅ HTTP komunikace v Docker síti (keycloak:8080)
- ✅ Token caching s TTL

### Production TODO:
- [ ] Použít Vault/Secret Manager pro master credentials
- [ ] Zvážit dedikovaný service account v master realmu místo direct admin
- [ ] HTTPS pro všechnu Keycloak komunikaci
- [ ] Rotace master password
- [ ] Audit logging všech realm operations

## 🎯 Výsledek

✅ **Problém vyřešen:** Tenanti lze nyní vytvářet bez 403 chyby  
✅ **Backend úspěšně restartován:** Změny aplikovány  
✅ **Dokumentace aktualizována:** ADMIN_UI_ISSUES_AND_FIXES.md

## 📚 Související soubory

- `backend/src/main/java/cz/muriel/core/auth/KeycloakAdminService.java`
- `backend/src/main/resources/application.properties`
- `.env`
- `docs/ADMIN_UI_ISSUES_AND_FIXES.md`
