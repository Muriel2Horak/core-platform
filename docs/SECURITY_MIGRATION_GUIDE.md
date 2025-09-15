# 🚨 SECURITY MIGRATION GUIDE

**KRITICKÉ: Tento dokument obsahuje kroky pro opravu závažných bezpečnostních děr v produkci.**

## 🔴 **IMMEDIATE ACTIONS (DO 24 HODIN):**

### 1. **Zastavte používání současného KeycloakClient**
```bash
# Dočasně zakažte admin operace
# Komentujte nebo deaktivujte changePassword a updateUserProfile endpointy
```

### 2. **Vytvořte bezpečný Service Account v Keycloak**
```bash
# Přihlaste se do Keycloak Admin (http://localhost:8081)
# 1. Clients → Create Client
# 2. Client ID: "backend-admin-service"
# 3. Client authentication: ON
# 4. Service accounts roles: ON
# 5. Credentials → Client Secret (zkopírujte!)
# 6. Service Account Roles → Assign role → "manage-users" (pouze potřebné role!)
```

### 3. **Nastavte Environment Variables**
```bash
# Zkopírujte .env.template do .env
cp .env.template .env

# Vyplňte bezpečné hodnoty:
KEYCLOAK_ADMIN_CLIENT_SECRET=your_actual_client_secret_from_step_2
DB_PASSWORD=$(openssl rand -base64 32)
APP_SECRET_KEY=$(openssl rand -base64 64)
```

### 4. **Aktualizujte Docker Compose**
```yaml
# V docker-compose.yml přidejte env_file:
services:
  backend:
    env_file:
      - .env
    environment:
      - KEYCLOAK_ADMIN_BASE_URL=${KEYCLOAK_ADMIN_BASE_URL}
      - KEYCLOAK_ADMIN_CLIENT_ID=${KEYCLOAK_ADMIN_CLIENT_ID}
      - KEYCLOAK_ADMIN_CLIENT_SECRET=${KEYCLOAK_ADMIN_CLIENT_SECRET}
      # ... ostatní env vars
```

## 🔧 **IMPLEMENTATION STEPS:**

### Step 1: Replace KeycloakClient with KeycloakAdminService
```java
// V AuthController.java nahraďte:
@Autowired
private KeycloakClient kc;  // ❌ REMOVE

@Autowired  
private KeycloakAdminService adminService;  // ✅ ADD

// Aktualizujte metody:
public ResponseEntity<?> changePassword(@RequestBody PasswordChangeRequest req, @AuthenticationPrincipal Jwt jwt) {
    String userId = jwt.getSubject();
    String adminUserId = jwt.getClaim("preferred_username");
    adminService.changeUserPassword(userId, req.getNewPassword(), adminUserId);
    return ResponseEntity.noContent().build();
}
```

### Step 2: Remove Hardcoded Credentials
```java
// REMOVE z SimpleLoginPage.js:
<Typography variant="body2" color="textSecondary" align="center" sx={{ mt: 2 }}>
  Testovací údaje: test / Test.1234  // ❌ DELETE THIS
</Typography>
```

### Step 3: Secure Token Storage  
```javascript
// V frontend nahraďte localStorage s httpOnly cookies
// nebo implementujte secure token storage s encryption
```

## 📋 **VERIFICATION CHECKLIST:**

- [ ] **Service Account vytvořen** s omezenými právy
- [ ] **Client Secret** uložen v .env (ne v kódu!)
- [ ] **KeycloakAdminService** implementován a testován
- [ ] **Audit logging** funguje pro všechny admin operace
- [ ] **Input validation** implementována
- [ ] **Token caching** funkční s TTL
- [ ] **Hardcoded credentials** odstraněny z kódu
- [ ] **.env v .gitignore** (secrets se necommitují)
- [ ] **Rate limiting** nakonfigurován
- [ ] **Security headers** nastaveny

## 🧪 **TESTING:**

```bash
# Test bezpečného admin tokenu:
curl -X POST http://localhost:8080/api/auth/change-password \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"currentPassword":"old","newPassword":"new"}'

# Zkontrolujte audit logy:
docker logs core-backend | grep "AUDIT"
```

## ⚠️ **SECURITY WARNINGS:**

1. **NIKDY** necommitujte .env soubor
2. **ROTUJTE** secrets každé 3 měsíce  
3. **MONITORUJTE** audit logy podezřelé aktivity
4. **TESTUJTE** security changes v izolovaném prostředí
5. **DOKUMENTUJTE** všechny admin operace

## 🚨 **ROLLBACK PLAN:**

Pokud něco selže:
```bash
# 1. Vraťte se k předchozí verzi kódu
git revert HEAD

# 2. Deaktivujte nový service account v Keycloak
# 3. Dočasně povolte starý KeycloakClient (pouze pro emergency!)
# 4. Okamžitě implementujte opravu
```

## 📞 **INCIDENT RESPONSE:**

V případě security incidentu:
1. **Okamžitě** deaktivujte kompromitované účty
2. **Rotujte** všechny secrets a tokeny  
3. **Auditujte** logy za posledních 30 dní
4. **Informujte** stakeholders o opatřeních
5. **Dokumentujte** incident a lessons learned

---

**⚠️ DŮLEŽITÉ: Tento migration musí být dokončen PŘED produkčním nasazením!**