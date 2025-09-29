# 🔐 Dynamic JWT Decoder & Multi-Tenant Keycloak Setup

## 🚨 **VYŘEŠENÉ KRITICKÉ PROBLÉMY:**

### **1. 🔐 DYNAMIC ISSUERS - Tenant-Specific Realms**
```yaml
PŘED:
  issuer: "https://core-platform.local/realms/core-platform"  # ❌ Hardcoded!

PO:
  - core-platform: "https://core-platform.local/realms/core-platform"
  - ivigee: "https://ivigee.core-platform.local/realms/ivigee"  
  - acme: "https://acme.core-platform.local/realms/acme"
```

### **2. 🔗 WILDCARD REDIRECT URIs**
```json
PŘED:
"redirectUris": ["https://core-platform.local/*"]

PO: 
"redirectUris": [
  "https://core-platform.local/*",
  "https://*.core-platform.local/*",  // ✅ Wildcard subdomény!
  "http://localhost:3000/*"
]
```

### **3. 🌐 WILDCARD HOSTS**
```bash
# ❌ TOTO NEFUNGUJE v /etc/hosts:
127.0.0.1   *.core-platform.local

# ✅ ŘEŠENÍ - dnsmasq wildcard:
make setup-wildcard
```

---

## 🚀 **NOVÉ WORKFLOW PRO VYTVOŘENÍ TENANTU:**

### **1. Automatické vytvoření (doporučeno)**
```bash
# Vytvoř tenant 'ivigee' s kompletním setupem
curl -X POST https://core-platform.local/api/admin/tenants \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "key": "ivigee", 
    "displayName": "Ivigee Corporation"
  }'

# Response:
{
  "tenantKey": "ivigee",
  "subdomainUrl": "https://ivigee.core-platform.local",
  "keycloakRealmUrl": "https://ivigee.core-platform.local/realms/ivigee",
  "adminConsoleUrl": "https://ivigee.core-platform.local/admin/ivigee/console/",
  "components": {
    "database": "✅ Created",
    "keycloak_realm": "✅ Created", 
    "hosts_entry": "✅ Added"
  }
}
```

### **2. Testování tenant subdomény**
```bash
# Test 1: Tenant subdoména funguje
curl -H "Host: ivigee.core-platform.local" https://core-platform.local/api/tenants/me

# Test 2: Keycloak realm existuje
curl https://ivigee.core-platform.local/realms/ivigee/.well-known/openid_configuration

# Test 3: Admin console přístupný
open https://ivigee.core-platform.local/admin/ivigee/console/
```

---

## 🔧 **TECHNICKÉ DETAILY:**

### **Dynamic JWT Decoder Logic**
```java
// 1. Dekóduje JWT payload bez validace
String tenantKey = extractTenantFromPayload(jsonNode);

// 2. Sestaví issuer URI pro tenant
if ("core-platform".equals(tenantKey)) {
    issuer = "https://core-platform.local/realms/core-platform";
} else {
    issuer = "https://" + tenantKey + ".core-platform.local/realms/" + tenantKey;
}

// 3. JWK Set URI (interní Docker)
jwkSetUri = "http://keycloak:8080/realms/" + tenantKey + "/protocol/openid-connect/certs";

// 4. Vytvoří a cache decoder pro tenant
```

### **Tenant Resolution Priority**
```java
1. HTTP Header (nginx): X-Tenant-Key
2. Hostname: tenant.core-platform.local → "tenant"  
3. JWT Claim: "tenant" claim z tokenu
4. FAIL: Vyhodí TenantNotFoundException
```

### **Keycloak Realm Template**
```bash
docker/keycloak/realm-tenant-template.json  # Template pro nové realmy
├── {TENANT_KEY} → realm name
├── {TENANT_NAME} → display name
└── Redirect URIs: https://{TENANT_KEY}.core-platform.local/*
```

---

## 🧪 **TESTOVACÍ SCÉNÁŘE:**

### **Test 1: Vytvoření tenantu 'testfirma'**
```bash
# 1. Vytvoř tenant
curl -X POST https://core-platform.local/api/admin/tenants \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"key": "testfirma", "displayName": "Test Firma s.r.o."}'

# 2. Ověř hosts entry
cat /etc/hosts | grep testfirma
# → 127.0.0.1   testfirma.core-platform.local

# 3. Test Keycloak realm
curl https://testfirma.core-platform.local/realms/testfirma/.well-known/openid_configuration

# 4. Test frontend přesměrování
open https://testfirma.core-platform.local
```

### **Test 2: JWT Token Validation**
```bash
# 1. Získej token z tenant realm
TOKEN=$(curl -X POST https://testfirma.core-platform.local/realms/testfirma/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=web&username=test&password=Test.1234" | jq -r '.access_token')

# 2. Test API call s tenant tokenem
curl -H "Authorization: Bearer $TOKEN" \
     -H "Host: testfirma.core-platform.local" \
     https://core-platform.local/api/tenants/me

# Expected response:
{
  "key": "testfirma",
  "name": "Test Firma s.r.o.", 
  "realm": "testfirma"
}
```

### **Test 3: Cross-Tenant Security**
```bash
# 1. Zkus použít core-platform token na testfirma subdoméně
CORE_TOKEN=$(curl -X POST https://core-platform.local/realms/core-platform/protocol/openid-connect/token ...)

curl -H "Authorization: Bearer $CORE_TOKEN" \
     -H "Host: testfirma.core-platform.local" \
     https://core-platform.local/api/tenants/me

# Expected: 401 Unauthorized (issuer mismatch)
```

---

## 🚨 **TROUBLESHOOTING:**

### **Problém: "Wildcard subdomény nefungují"**
```bash
# Řešení: Nastavit dnsmasq wildcard
make setup-wildcard

# Nebo manuálně přidat doménu
make add-tenant-domain TENANT=testfirma
```

### **Problém: "JWT validation fails"**
```bash
# Check 1: Verify issuer v tokenu
echo $JWT_TOKEN | jwt decode

# Check 2: Verify JWK Set dostupnost
curl http://keycloak:8080/realms/testfirma/protocol/openid-connect/certs

# Check 3: Debug logs
docker logs core-platform-backend-1 | grep "DynamicJwtDecoder"
```

### **Problém: "Keycloak realm neexistuje"**
```bash
# Check admin console
open https://core-platform.local/admin/master/console/#/realms/testfirma

# Vytvoř realm manuálně
curl -X POST https://core-platform.local/api/admin/tenants/testfirma/realm \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

## 📋 **CHECKLIST PRO PRODUKCI:**

### **Před nasazením:**
- [ ] ✅ Wildcard SSL certifikát pro `*.yourdomain.com`
- [ ] ✅ DNS wildcard záznam: `*.yourdomain.com → IP`
- [ ] ✅ Nastavit `DOMAIN=yourdomain.com` v .env
- [ ] ✅ Otestovat vytvoření tenantu přes API
- [ ] ✅ Ověřit JWT validation napříč tenanty
- [ ] ✅ Test izolace dat mezi tenanty

### **Monitoring:**
```bash
# JWT decoder cache statistiky
curl https://yourdomain.com/actuator/metrics/jwt.decoder.cache.size

# Tenant resolution metriky  
curl https://yourdomain.com/actuator/metrics/tenant.resolution.success

# Error rates per tenant
curl https://yourdomain.com/actuator/metrics/http.server.requests?tag=tenant:testfirma
```

---

## 🎯 **VÝHODY NOVÉHO SYSTÉMU:**

### **✅ Kompletní tenant izolace**
- Každý tenant má vlastní Keycloak realm
- Vlastní issuer a signing keys  
- Vlastní user base a role

### **✅ Automatizovaný setup**
- API call vytvoří kompletní tenant
- Automatické Keycloak realm creation
- Hosts file management

### **✅ Bezpečnost**
- Cross-tenant token validation impossible
- Tenant-specific CORS policies
- Isolated JWK sets

### **✅ Škálovatelnost**  
- Cached JWT decoders per tenant
- Lazy realm creation
- Horizontal scaling ready