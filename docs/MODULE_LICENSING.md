# Module Licensing System

**Version:** 1.0.0  
**Last Updated:** 2025-10-26  
**Status:** 🔮 PLANNED

---

## 📖 Overview

**Module Licensing** je systém pro validaci a enforce licencí na jednotlivé moduly per tenant. Umožňuje:
- ✅ Per-tenant licencování (tenant A má license, tenant B nemá)
- ✅ Per-module licencování (tenant může mít license jen na delivery-suite, ne na helpdesk)
- ✅ Časově omezené licence (expiration)
- ✅ Trial/demo licence (zkušební období)
- ✅ Edition-based licensing (enterprise vs standard)

---

## 🔑 Licensing Concepts

### Tenant-scoped

**Každá license je vázána na:**
- `tenant_id` - Keycloak realm / subdomain (`acme-corp`, `tenant-b`, ...)
- `module_id` - Module identifier (`delivery-suite`, `helpdesk`, ...)

**Důsledky:**
- ✅ Tenant A může mít licenci na `delivery-suite`, tenant B nemá → modul skrytý pro tenant B
- ✅ Tenant A může mít `enterprise` edici, tenant B má `standard` → různé features
- ✅ License nelze sdílet mezi tenant y (hard isolation)

---

### Module-scoped

**Licence platí pro konkrétní modul:**
- `delivery-suite` - Agile work management
- `helpdesk` - Support ticket system
- `crm-plus` - Customer relationship management

**Bundle licenses (future enhancement):**
- `enterprise-bundle` - Obsahuje delivery-suite + helpdesk + crm-plus
- Validace: pokud tenant má bundle, má i jednotlivé moduly

---

### License States

**Stored in:** `core_tenant_modules.license_state`

| State | Description | Module Behavior | UI Indicator |
|-------|-------------|-----------------|--------------|
| `VALID` | License is active and not expired | ✅ Fully functional | 🟢 "Active" |
| `EXPIRED` | License was valid but expired | ❌ Disabled (403 on API) | 🔴 "Expired - Renew" |
| `MISSING` | No license provided (module requires license) | ❌ Disabled | 🔵 "License Required" |
| `INVALID` | License signature/format invalid | ❌ Disabled | 🟠 "Invalid License" |
| `TRIAL` | Trial period active (limited time) | ✅ Functional (with warning) | 🟡 "Trial (15 days left)" |

**State Transitions:**

```
MISSING → (upload license) → VALID
VALID → (time passes) → EXPIRED
VALID → (admin deactivates) → MISSING
TRIAL → (trial ends) → EXPIRED
INVALID → (upload correct license) → VALID
```

---

## 🎫 License Token Format (JWT)

### JWT Structure

**License je JWT token** signed by vendor (CORE Team nebo third-party)

**Header:**
```json
{
  "alg": "HS256",       // HMAC-SHA256 (symmetric) or "RS256" (asymmetric)
  "typ": "JWT"
}
```

**Payload (Claims):**
```json
{
  "iss": "core-platform.com",              // Issuer (vendor domain)
  "sub": "module:delivery-suite",          // Subject (module_id)
  "aud": "tenant:acme-corp",               // Audience (tenant_id)
  "iat": 1704067200,                       // Issued at (Unix timestamp)
  "exp": 1735689600,                       // Expires at (Unix timestamp)
  
  "claims": {
    "tenant_id": "acme-corp",              // Tenant identifier (MUST match JWT aud)
    "module_id": "delivery-suite",         // Module identifier (MUST match JWT sub)
    "edition": "enterprise",               // License edition (standard, professional, enterprise)
    "max_users": 100,                      // User limit (optional)
    "max_items": 10000,                    // Item limit (optional, module-specific)
    "features": [                          // Enabled features (module-specific)
      "kanban",
      "sprints",
      "reports",
      "ai_suggestions"
    ],
    "valid_from": "2024-01-01",            // Start date (ISO 8601)
    "valid_to": "2025-12-31",              // End date (ISO 8601)
    "is_trial": false                      // Trial license flag
  }
}
```

**Signature:**
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret_key
)
```

**Full Token Example:**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjb3JlLXBsYXRmb3JtLmNvbSIsInN1YiI6Im1vZHVsZTpkZWxpdmVyeS1zdWl0ZSIsImF1ZCI6InRlbmFudDphY21lLWNvcnAiLCJpYXQiOjE3MDQwNjcyMDAsImV4cCI6MTczNTY4OTYwMCwiY2xhaW1zIjp7InRlbmFudF9pZCI6ImFjbWUtY29ycCIsIm1vZHVsZV9pZCI6ImRlbGl2ZXJ5LXN1aXRlIiwiZWRpdGlvbiI6ImVudGVycHJpc2UiLCJtYXhfdXNlcnMiOjEwMCwiZmVhdHVyZXMiOlsia2FuYmFuIiwic3ByaW50cyIsInJlcG9ydHMiXSwidmFsaWRfZnJvbSI6IjIwMjQtMDEtMDEiLCJ2YWxpZF90byI6IjIwMjUtMTItMzEiLCJpc190cmlhbCI6ZmFsc2V9fQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

---

### Signing Options

#### Option 1: HMAC-SHA256 (Symmetric Key)

**Use Case:** Development, testing, single-vendor deployments

**Pros:**
- ✅ Fast (symmetric encryption)
- ✅ Simple implementation
- ✅ Small key size (32 bytes)

**Cons:**
- ❌ Shared secret (backend must have secret key → security risk if leaked)
- ❌ Cannot verify third-party vendor signatures (all use same secret)

**Key Generation:**
```bash
openssl rand -base64 32
# Output: xK7t9vQ3wR8yN5pL2mH6jD1cF4gB0sA9z
```

**Backend Configuration:**
```yaml
# application.yml
license:
  signing:
    algorithm: HS256
    secret: ${LICENSE_SIGNING_SECRET}  # From environment variable
```

**Token Generation (Vendor):**
```java
String secret = "xK7t9vQ3wR8yN5pL2mH6jD1cF4gB0sA9z";
Algorithm algorithm = Algorithm.HMAC256(secret);

String token = JWT.create()
  .withIssuer("core-platform.com")
  .withSubject("module:delivery-suite")
  .withAudience("tenant:acme-corp")
  .withIssuedAt(new Date())
  .withExpiresAt(Date.from(Instant.parse("2025-12-31T23:59:59Z")))
  .withClaim("tenant_id", "acme-corp")
  .withClaim("module_id", "delivery-suite")
  .withClaim("edition", "enterprise")
  .sign(algorithm);
```

---

#### Option 2: RSA-SHA256 (Asymmetric Key)

**Use Case:** Production, multi-vendor ecosystems

**Pros:**
- ✅ Secure (vendor holds private key, backend has public key only)
- ✅ Third-party vendor support (each vendor has own keypair)
- ✅ Token cannot be forged (backend cannot create licenses, only validate)

**Cons:**
- ❌ Slower (RSA crypto)
- ❌ Larger key size (2048-4096 bits)
- ❌ More complex key management

**Key Generation:**
```bash
# Vendor generates keypair
openssl genrsa -out license_private.pem 4096
openssl rsa -in license_private.pem -pubout -out license_public.pem

# Vendor keeps: license_private.pem (SECRET!)
# Backend gets: license_public.pem (public, safe to share)
```

**Backend Configuration:**
```yaml
# application.yml
license:
  signing:
    algorithm: RS256
    public_key: |
      -----BEGIN PUBLIC KEY-----
      MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
      -----END PUBLIC KEY-----
```

**Token Generation (Vendor):**
```java
PrivateKey privateKey = loadPrivateKey("license_private.pem");
Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);

String token = JWT.create()
  .withIssuer("core-platform.com")
  .withSubject("module:delivery-suite")
  .withAudience("tenant:acme-corp")
  .withIssuedAt(new Date())
  .withExpiresAt(Date.from(Instant.parse("2025-12-31T23:59:59Z")))
  .withClaim("tenant_id", "acme-corp")
  .withClaim("module_id", "delivery-suite")
  .sign(algorithm);
```

---

## 🔐 LicenseService API

### Interface Definition

```java
package cz.muriel.core.license;

public interface LicenseService {
  
  /**
   * Validate if tenant has valid license for module
   * 
   * @param tenantId Realm/subdomain from JWT (e.g., "acme-corp")
   * @param moduleId Module identifier (e.g., "delivery-suite")
   * @return LicenseValidationResult (VALID, EXPIRED, MISSING, INVALID, TRIAL)
   */
  LicenseValidationResult validateLicense(String tenantId, String moduleId);
  
  /**
   * Get all enabled modules for tenant (only VALID or TRIAL)
   * Used by FE context endpoint
   * 
   * @param tenantId Tenant identifier
   * @return List of enabled modules with entrypoints
   */
  List<ModuleContext> getEnabledModules(String tenantId);
  
  /**
   * Upload and activate license for tenant+module
   * 
   * @param tenantId Tenant identifier
   * @param moduleId Module identifier
   * @param licenseToken JWT token (signed by vendor)
   * @throws InvalidLicenseException if token invalid
   */
  void uploadLicense(String tenantId, String moduleId, String licenseToken);
  
  /**
   * Deactivate module for tenant (revoke license)
   * 
   * @param tenantId Tenant identifier
   * @param moduleId Module identifier
   */
  void revokeLicense(String tenantId, String moduleId);
}
```

---

### Validation Logic

**Method:** `validateLicense(String tenantId, String moduleId)`

**Process:**

```java
@Service
public class LicenseServiceImpl implements LicenseService {
  
  @Override
  public LicenseValidationResult validateLicense(String tenantId, String moduleId) {
    // 1. Query tenant module record
    Optional<TenantModule> tenantModule = tenantModuleRepo
      .findByTenantIdAndModuleId(tenantId, moduleId);
    
    if (tenantModule.isEmpty()) {
      return LicenseValidationResult.MISSING;  // No record → not activated
    }
    
    TenantModule tm = tenantModule.get();
    
    // 2. Check if enabled
    if (!tm.isEnabled()) {
      return LicenseValidationResult.MISSING;  // Admin disabled
    }
    
    // 3. Parse JWT token
    String licenseToken = tm.getLicensePayload();
    if (licenseToken == null || licenseToken.isBlank()) {
      return LicenseValidationResult.MISSING;  // No license uploaded
    }
    
    try {
      DecodedJWT jwt = JWT.decode(licenseToken);
      
      // 4. Verify signature
      Algorithm algorithm = getAlgorithm();  // HS256 or RS256
      JWTVerifier verifier = JWT.require(algorithm)
        .withIssuer("core-platform.com")
        .withAudience("tenant:" + tenantId)
        .withSubject("module:" + moduleId)
        .build();
      
      verifier.verify(licenseToken);  // Throws if invalid signature
      
      // 5. Check expiration
      Instant expiresAt = jwt.getExpiresAt().toInstant();
      if (Instant.now().isAfter(expiresAt)) {
        return LicenseValidationResult.EXPIRED;  // Token expired
      }
      
      // 6. Validate claims
      String tokenTenantId = jwt.getClaim("tenant_id").asString();
      String tokenModuleId = jwt.getClaim("module_id").asString();
      
      if (!tokenTenantId.equals(tenantId) || !tokenModuleId.equals(moduleId)) {
        return LicenseValidationResult.INVALID;  // Mismatched claims
      }
      
      // 7. Check if trial
      boolean isTrial = jwt.getClaim("is_trial").asBoolean();
      if (isTrial) {
        return LicenseValidationResult.TRIAL;  // Trial license
      }
      
      // 8. All checks passed
      return LicenseValidationResult.VALID;
      
    } catch (JWTVerificationException e) {
      // Signature invalid or malformed token
      return LicenseValidationResult.INVALID;
    }
  }
}
```

**Return Values:**

| Result | Meaning | Action |
|--------|---------|--------|
| `VALID` | License is active, signature valid, not expired | ✅ Allow module access |
| `EXPIRED` | License was valid but expired | ❌ Deny access, show "License expired" |
| `MISSING` | No license found or disabled | ❌ Deny access, show "License required" |
| `INVALID` | Signature invalid or claims mismatch | ❌ Deny access, show "Invalid license" |
| `TRIAL` | Trial license, valid but time-limited | ✅ Allow with warning banner |

---

### Upload License Flow

**Method:** `uploadLicense(String tenantId, String moduleId, String licenseToken)`

**Process:**

```java
@Override
public void uploadLicense(String tenantId, String moduleId, String licenseToken) {
  // 1. Validate token format
  DecodedJWT jwt;
  try {
    jwt = JWT.decode(licenseToken);
  } catch (JWTDecodeException e) {
    throw new InvalidLicenseException("Malformed JWT token");
  }
  
  // 2. Verify signature
  Algorithm algorithm = getAlgorithm();
  JWTVerifier verifier = JWT.require(algorithm)
    .withIssuer("core-platform.com")
    .build();
  
  try {
    verifier.verify(licenseToken);
  } catch (JWTVerificationException e) {
    throw new InvalidLicenseException("Invalid signature");
  }
  
  // 3. Extract claims
  String tokenTenantId = jwt.getClaim("tenant_id").asString();
  String tokenModuleId = jwt.getClaim("module_id").asString();
  Instant expiresAt = jwt.getExpiresAt().toInstant();
  
  // 4. Validate claims match
  if (!tokenTenantId.equals(tenantId)) {
    throw new InvalidLicenseException("License tenant_id mismatch (expected: " + tenantId + ", got: " + tokenTenantId + ")");
  }
  if (!tokenModuleId.equals(moduleId)) {
    throw new InvalidLicenseException("License module_id mismatch");
  }
  
  // 5. Check expiration
  if (Instant.now().isAfter(expiresAt)) {
    throw new ExpiredLicenseException("License already expired");
  }
  
  // 6. Upsert tenant_module record
  TenantModule tm = tenantModuleRepo.findByTenantIdAndModuleId(tenantId, moduleId)
    .orElse(new TenantModule(tenantId, moduleId));
  
  tm.setLicensePayload(licenseToken);
  tm.setLicenseState(jwt.getClaim("is_trial").asBoolean() ? "TRIAL" : "VALID");
  tm.setLicenseExpiresAt(expiresAt);
  tm.setEnabled(true);
  tm.setUpdatedBy("admin");  // Or current user
  
  tenantModuleRepo.save(tm);
  
  // 7. Audit log
  auditLogService.log("LICENSE_UPLOADED", tenantId, moduleId, "License activated until " + expiresAt);
}
```

---

## 🛡️ Guard Integration (API Middleware)

### Annotation-based Enforcement

**Annotation Definition:**

```java
package cz.muriel.core.license.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresModule {
  /**
   * Module ID that must be licensed for tenant
   */
  String value();
  
  /**
   * Allow trial licenses? (default: true)
   */
  boolean allowTrial() default true;
}
```

**Controller Usage:**

```java
@RestController
@RequestMapping("/api/modules/delivery")
@RequiresModule("delivery-suite")  // ← License check applied to all endpoints
public class DeliveryController {
  
  @GetMapping("/items")
  public List<DeliveryItem> getItems(@TenantId String tenantId) {
    // Middleware already validated license for "delivery-suite"
    // tenantId auto-injected from JWT
    return deliveryService.findByTenant(tenantId);
  }
  
  @PostMapping("/items")
  public DeliveryItem createItem(@TenantId String tenantId, @RequestBody CreateItemRequest req) {
    // License check passed → allow creation
    return deliveryService.create(tenantId, req);
  }
}
```

---

### Interceptor Implementation

```java
package cz.muriel.core.license.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ModuleLicenseInterceptor implements HandlerInterceptor {
  
  private final LicenseService licenseService;
  private final TenantContextService tenantContext;
  
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    // 1. Check if handler has @RequiresModule annotation
    if (!(handler instanceof HandlerMethod)) {
      return true;  // Not a controller method
    }
    
    HandlerMethod handlerMethod = (HandlerMethod) handler;
    RequiresModule annotation = handlerMethod.getBeanType().getAnnotation(RequiresModule.class);
    
    if (annotation == null) {
      return true;  // No license check required
    }
    
    // 2. Extract tenant_id from JWT
    String tenantId = tenantContext.getCurrentTenantId();  // From JWT "realm" claim
    if (tenantId == null) {
      throw new UnauthorizedException("Tenant context missing");
    }
    
    // 3. Validate license
    String moduleId = annotation.value();
    LicenseValidationResult result = licenseService.validateLicense(tenantId, moduleId);
    
    // 4. Check result
    if (result == LicenseValidationResult.VALID) {
      return true;  // License valid → proceed
    }
    
    if (result == LicenseValidationResult.TRIAL && annotation.allowTrial()) {
      return true;  // Trial allowed → proceed
    }
    
    // 5. License invalid → deny access
    String errorMessage = switch (result) {
      case EXPIRED -> "Module '" + moduleId + "' license expired. Contact sales to renew.";
      case MISSING -> "Module '" + moduleId + "' requires a license. Contact sales.";
      case INVALID -> "Module '" + moduleId + "' license is invalid. Upload a valid license.";
      case TRIAL -> "Module '" + moduleId + "' is in trial mode. Upgrade to full license.";
      default -> "Module '" + moduleId + "' is not available.";
    };
    
    throw new ForbiddenException(errorMessage, "FEATURE_DISABLED");
  }
}
```

**WebMvcConfigurer Registration:**

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  
  private final ModuleLicenseInterceptor moduleLicenseInterceptor;
  
  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(moduleLicenseInterceptor)
      .addPathPatterns("/api/modules/**");  // Apply to all module APIs
  }
}
```

---

### Error Response Format

**HTTP Status:** `403 Forbidden`

**Response Body:**

```json
{
  "timestamp": "2025-01-15T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Module 'delivery-suite' requires a license. Contact sales.",
  "error_code": "FEATURE_DISABLED",
  "path": "/api/modules/delivery/items"
}
```

**Frontend Handling:**

```typescript
// frontend/src/api/client.ts
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 403 && error.response?.data?.error_code === 'FEATURE_DISABLED') {
      // Show license required modal
      showLicenseRequiredModal(error.response.data.message);
    }
    throw error;
  }
);
```

---

## 🌐 Frontend Context Endpoint

### API Specification

**Endpoint:** `GET /internal/modules/tenant-context`

**Purpose:** Provide frontend with list of enabled modules for current tenant (based on JWT)

**Authentication:** Bearer JWT token (tenant_id from `realm` claim)

**Response:**

```json
{
  "tenant_id": "acme-corp",
  "modules": [
    {
      "module_id": "delivery-suite",
      "name": "Delivery Suite",
      "license_state": "VALID",
      "license_expires_at": "2025-12-31T23:59:59Z",
      "trial": false,
      "entrypoints": {
        "fe": {
          "route": "/app/delivery",
          "menuLabel": "Delivery Board",
          "icon": "kanban",
          "weight": 100
        }
      },
      "config": {
        "max_items_per_sprint": 50,
        "enable_reports": true
      }
    },
    {
      "module_id": "helpdesk",
      "name": "Helpdesk Pro",
      "license_state": "TRIAL",
      "license_expires_at": "2025-02-01T23:59:59Z",
      "trial": true,
      "trial_days_left": 15,
      "entrypoints": {
        "fe": {
          "route": "/app/helpdesk",
          "menuLabel": "Support Tickets",
          "icon": "headset",
          "weight": 200
        }
      }
    }
  ]
}
```

**Filters:**
- ✅ Only modules with `license_state = VALID or TRIAL`
- ✅ Only modules with `enabled = true`
- ❌ Excludes modules with `license_state = EXPIRED, MISSING, INVALID`

---

### Backend Implementation

```java
@RestController
@RequestMapping("/internal/modules")
public class ModuleContextController {
  
  private final LicenseService licenseService;
  
  @GetMapping("/tenant-context")
  public ModuleTenantContext getTenantContext(@TenantId String tenantId) {
    List<ModuleContext> modules = licenseService.getEnabledModules(tenantId);
    
    return new ModuleTenantContext(tenantId, modules);
  }
}
```

**Service Logic:**

```java
@Override
public List<ModuleContext> getEnabledModules(String tenantId) {
  // 1. Query all tenant modules
  List<TenantModule> tenantModules = tenantModuleRepo.findByTenantId(tenantId);
  
  // 2. Filter by license state
  return tenantModules.stream()
    .filter(tm -> tm.isEnabled())
    .filter(tm -> {
      String state = tm.getLicenseState();
      return "VALID".equals(state) || "TRIAL".equals(state);
    })
    .map(tm -> {
      ModuleCatalog catalog = catalogRepo.findById(tm.getModuleId()).orElseThrow();
      
      ModuleContext ctx = new ModuleContext();
      ctx.setModuleId(tm.getModuleId());
      ctx.setName(catalog.getName());
      ctx.setLicenseState(tm.getLicenseState());
      ctx.setLicenseExpiresAt(tm.getLicenseExpiresAt());
      ctx.setTrial("TRIAL".equals(tm.getLicenseState()));
      ctx.setEntrypoints(catalog.getEntrypoints());
      ctx.setConfig(tm.getConfig());
      
      // Calculate trial days left
      if (ctx.isTrial()) {
        long daysLeft = ChronoUnit.DAYS.between(Instant.now(), tm.getLicenseExpiresAt());
        ctx.setTrialDaysLeft((int) daysLeft);
      }
      
      return ctx;
    })
    .collect(Collectors.toList());
}
```

---

### Frontend Usage

**1. Fetch context on app load:**

```typescript
// frontend/src/store/moduleContext.ts
import { create } from 'zustand';

interface ModuleContextStore {
  modules: ModuleInfo[];
  loading: boolean;
  loadContext: () => Promise<void>;
}

export const useModuleContext = create<ModuleContextStore>((set) => ({
  modules: [],
  loading: false,
  
  loadContext: async () => {
    set({ loading: true });
    const response = await fetch('/internal/modules/tenant-context');
    const data = await response.json();
    set({ modules: data.modules, loading: false });
  }
}));
```

**2. Build navigation menu:**

```tsx
// frontend/src/components/Navigation.tsx
import { useModuleContext } from '@/store/moduleContext';

export function Navigation() {
  const { modules } = useModuleContext();
  
  // Sort by weight (lower = higher in menu)
  const sortedModules = modules
    .filter(m => m.entrypoints?.fe)
    .sort((a, b) => (a.entrypoints.fe.weight || 999) - (b.entrypoints.fe.weight || 999));
  
  return (
    <nav>
      {sortedModules.map(module => (
        <NavItem
          key={module.module_id}
          href={module.entrypoints.fe.route}
          icon={module.entrypoints.fe.icon}
          label={module.entrypoints.fe.menuLabel}
          trial={module.trial}
          trialDaysLeft={module.trial_days_left}
        />
      ))}
    </nav>
  );
}
```

**3. Conditionally render routes:**

```tsx
// frontend/src/App.tsx
import { useModuleContext } from '@/store/moduleContext';

export function App() {
  const { modules } = useModuleContext();
  
  const hasModule = (moduleId: string) =>
    modules.some(m => m.module_id === moduleId);
  
  return (
    <Routes>
      {/* Core routes (always available) */}
      <Route path="/app/home" element={<Home />} />
      
      {/* Module routes (conditional) */}
      {hasModule('delivery-suite') && (
        <Route path="/app/delivery/*" element={<DeliverySuiteApp />} />
      )}
      
      {hasModule('helpdesk') && (
        <Route path="/app/helpdesk/*" element={<HelpdeskApp />} />
      )}
      
      {/* Fallback */}
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}
```

**4. Trial warning banner:**

```tsx
// frontend/src/components/TrialBanner.tsx
export function TrialBanner({ module }: { module: ModuleInfo }) {
  if (!module.trial) return null;
  
  const daysLeft = module.trial_days_left || 0;
  const variant = daysLeft < 7 ? 'destructive' : 'warning';
  
  return (
    <Alert variant={variant}>
      <AlertTitle>Trial License</AlertTitle>
      <AlertDescription>
        Your trial for {module.name} expires in {daysLeft} days.
        <Button variant="link" asChild>
          <a href="/admin/modules">Upgrade to Full License</a>
        </Button>
      </AlertDescription>
    </Alert>
  );
}
```

---

## 🔒 Security Considerations

### 1. Signature Validation

**CRITICAL:** Always verify JWT signature before trusting claims

**Vulnerabilities if skipped:**
- Attacker can forge license tokens
- Tenant A can copy license from Tenant B
- Expired licenses can be modified to extend expiration

**Protection:**

```java
// ✅ CORRECT: Verify signature
JWTVerifier verifier = JWT.require(algorithm)
  .withIssuer("core-platform.com")
  .build();
verifier.verify(licenseToken);  // Throws if invalid

// ❌ WRONG: Trust claims without verification
DecodedJWT jwt = JWT.decode(licenseToken);  // NO SIGNATURE CHECK!
String tenantId = jwt.getClaim("tenant_id").asString();  // Can be forged!
```

---

### 2. Expiration Checks

**CRITICAL:** Check `exp` claim AND `license_expires_at` timestamp

**Vulnerabilities if skipped:**
- Expired licenses remain functional
- No revenue from renewals

**Protection:**

```java
// Check JWT expiration
Instant expiresAt = jwt.getExpiresAt().toInstant();
if (Instant.now().isAfter(expiresAt)) {
  return LicenseValidationResult.EXPIRED;
}

// Double-check database record
if (Instant.now().isAfter(tenantModule.getLicenseExpiresAt())) {
  return LicenseValidationResult.EXPIRED;
}
```

---

### 3. Tenant Isolation

**CRITICAL:** License MUST be tenant-scoped, NEVER global

**Vulnerabilities if skipped:**
- Tenant A's license applies to Tenant B
- Multi-tenant isolation broken

**Protection:**

```java
// ✅ CORRECT: Check tenant_id from JWT matches license
String currentTenantId = tenantContext.getCurrentTenantId();  // From JWT
String licenseTenantId = jwt.getClaim("tenant_id").asString();

if (!currentTenantId.equals(licenseTenantId)) {
  throw new ForbiddenException("License tenant mismatch");
}

// ❌ WRONG: Trust license without tenant check
if (jwt.getClaim("module_id").asString().equals(moduleId)) {
  return VALID;  // Any tenant can use any license!
}
```

---

### 4. Secret Key Protection

**CRITICAL:** HMAC secret key MUST NOT be leaked

**Storage:**
- ✅ Environment variable (not in code/config files)
- ✅ Secrets manager (AWS Secrets Manager, HashiCorp Vault)
- ❌ Never commit to Git
- ❌ Never log/print

**Rotation:**
- Rotate key every 6-12 months
- Support multiple keys (for migration period)
- Invalidate old tokens after rotation

---

### 5. Frontend NEVER Validates Licenses

**CRITICAL:** Frontend only reads context, NEVER validates tokens

**Why:**
- Frontend code is public (can be inspected)
- JavaScript can be manipulated (browser DevTools)
- License logic MUST be backend-only

**Protection:**

```typescript
// ✅ CORRECT: Frontend only checks if module exists in context
const hasDeliverySuite = modules.some(m => m.module_id === 'delivery-suite');
if (hasDeliverySuite) {
  // Show menu item
}

// ❌ WRONG: Frontend validates JWT (can be bypassed!)
const jwt = localStorage.getItem('license_token');
const decoded = jwtDecode(jwt);  // Can be faked by attacker!
if (decoded.exp > Date.now()) {
  // Show menu item
}
```

---

### 6. Revocation Support

**Current:** License validation checks `enabled` flag in DB

**Future Enhancement:** Revocation list (compromised licenses)

**Implementation:**

```java
// Check revocation list before validation
if (revocationList.contains(licenseToken)) {
  return LicenseValidationResult.INVALID;
}
```

**Use Cases:**
- Compromised license (leaked to public)
- Fraudulent tenant (chargebacks)
- Vendor request (third-party module)

---

## 🛠️ Admin Workflows

### Workflow 1: Upload License (Happy Path)

**Actors:** Tenant Admin, CORE Platform

**Steps:**

1. **Admin navigates to:** `/admin/modules`
2. **Admin clicks:** "Activate Module" → "delivery-suite"
3. **Admin uploads:** JWT token file (or pastes token)
4. **Backend validates:**
   - Parse JWT
   - Verify signature
   - Check expiration
   - Validate tenant_id matches
5. **Backend saves:**
   - Insert/update `core_tenant_modules` record
   - Set `license_state = VALID` or `TRIAL`
   - Set `enabled = true`
6. **Frontend shows:**
   - ✅ "Module activated successfully"
   - Module appears in menu
7. **User can access:** `/app/delivery` route

---

### Workflow 2: License Expired

**Actors:** Tenant Admin, CORE Platform, Sales Team

**Steps:**

1. **System cron job** (daily):
   - Check all `core_tenant_modules` WHERE `license_expires_at < NOW`
   - Update `license_state = EXPIRED`
   - Send notification email to tenant admin
2. **User tries to access** `/app/delivery`:
   - API returns `403 Forbidden`
   - Frontend shows modal: "License expired. Contact sales."
3. **Admin contacts sales**:
   - Sales generates new JWT token (extended expiration)
   - Sends token to admin via email
4. **Admin uploads new token**:
   - Follow Workflow 1
   - `license_state` updated to `VALID`
5. **User can access** module again

---

### Workflow 3: Trial Expiring Soon

**Actors:** Tenant Admin, CORE Platform

**Steps:**

1. **System cron job** (daily):
   - Check all `core_tenant_modules` WHERE `license_state = TRIAL` AND `license_expires_at < NOW + 15 days`
   - Send notification email: "Trial expires in X days"
2. **Frontend shows banner**:
   - Yellow warning: "Trial expires in 10 days - Upgrade now"
   - Link to `/admin/modules` or sales contact
3. **If trial expires** without renewal:
   - Follow Workflow 2 (License Expired)

---

### Workflow 4: Invalid License Upload

**Actors:** Tenant Admin, CORE Platform

**Steps:**

1. **Admin uploads** invalid JWT token (e.g., wrong signature)
2. **Backend validation** fails:
   - `JWTVerificationException` thrown
3. **Frontend shows error**:
   - ❌ "Invalid license token. Signature verification failed."
   - Suggest: "Contact sales for a valid license."
4. **Admin re-uploads** correct token:
   - Follow Workflow 1

---

## 📊 Monitoring & Observability

### License Validation Metrics

**Prometheus Metrics:**

```java
@Component
public class LicenseMetrics {
  
  @Counter(name = "license_validation_total", description = "Total license validations", labelNames = {"result", "module_id"})
  public void recordValidation(LicenseValidationResult result, String moduleId) {
    // Increments counter by result (VALID, EXPIRED, etc.)
  }
  
  @Gauge(name = "licenses_expiring_soon", description = "Licenses expiring in <30 days")
  public int getExpiringLicensesCount() {
    return tenantModuleRepo.countByLicenseExpiresAtBetween(
      Instant.now(),
      Instant.now().plus(30, ChronoUnit.DAYS)
    );
  }
  
  @Gauge(name = "trial_licenses_active", description = "Active trial licenses")
  public int getTrialLicensesCount() {
    return tenantModuleRepo.countByLicenseState("TRIAL");
  }
}
```

**Grafana Dashboard:**

```
┌─────────────────────────────────────────────────┐
│ License Validation Rate (last 24h)             │
│ ✅ VALID: 15,234 (98.5%)                       │
│ 🔴 EXPIRED: 123 (0.8%)                         │
│ ❌ INVALID: 45 (0.3%)                          │
│ 🔵 MISSING: 89 (0.4%)                          │
└─────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────┐
│ Licenses Expiring Soon                          │
│ Next 7 days:   12 licenses                     │
│ Next 30 days:  34 licenses                     │
│ (Send renewal reminders)                        │
└─────────────────────────────────────────────────┘
```

---

### Audit Log

**Table:** `core_module_audit_log`

**Events:**

```json
{
  "event_type": "LICENSE_UPLOADED",
  "timestamp": "2025-01-15T10:30:00Z",
  "tenant_id": "acme-corp",
  "module_id": "delivery-suite",
  "user_id": "admin@acme-corp.com",
  "details": {
    "license_state": "VALID",
    "expires_at": "2025-12-31T23:59:59Z",
    "edition": "enterprise"
  }
}

{
  "event_type": "LICENSE_EXPIRED",
  "timestamp": "2025-12-31T23:59:59Z",
  "tenant_id": "acme-corp",
  "module_id": "delivery-suite",
  "details": {
    "previous_state": "VALID",
    "new_state": "EXPIRED"
  }
}

{
  "event_type": "LICENSE_VALIDATION_FAILED",
  "timestamp": "2025-01-15T10:35:00Z",
  "tenant_id": "acme-corp",
  "module_id": "delivery-suite",
  "user_id": "user@acme-corp.com",
  "details": {
    "reason": "INVALID_SIGNATURE",
    "ip_address": "192.168.1.100"
  }
}
```

---

## 🎓 Vendor Guide (License Generation)

### For CORE Team (Generating Licenses)

**Tool:** License Generator CLI

```bash
# Generate enterprise license for tenant
./license-generator.sh \
  --tenant-id acme-corp \
  --module-id delivery-suite \
  --edition enterprise \
  --valid-from 2025-01-01 \
  --valid-to 2025-12-31 \
  --max-users 100 \
  --features kanban,sprints,reports,ai \
  --output acme-corp-delivery-suite.jwt

# Output:
# ✅ License generated: acme-corp-delivery-suite.jwt
# Expires: 2025-12-31T23:59:59Z
# Edition: enterprise
# Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Implementation:**

```bash
#!/bin/bash
# license-generator.sh

TENANT_ID=""
MODULE_ID=""
EDITION="standard"
VALID_FROM=$(date +%Y-%m-%d)
VALID_TO=$(date -d "+1 year" +%Y-%m-%d)
MAX_USERS=50
FEATURES=""

# Parse arguments...

# Generate JWT using auth0 library or online tool
# Sign with HMAC-SHA256 or RSA-SHA256
# Output to file
```

---

### For Third-Party Vendors (Self-Licensing)

**Scenario:** Vendor builds custom module, wants to license it themselves

**Steps:**

1. **Vendor generates** RSA keypair:
   ```bash
   openssl genrsa -out vendor_private.pem 4096
   openssl rsa -in vendor_private.pem -pubout -out vendor_public.pem
   ```

2. **Vendor sends** public key to CORE Team:
   ```
   Subject: Public Key for Module: vendor-custom-module
   
   Please register our public key for license validation:
   
   -----BEGIN PUBLIC KEY-----
   MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
   -----END PUBLIC KEY-----
   
   Module ID: vendor-custom-module
   Vendor: Partner s.r.o.
   ```

3. **CORE Team configures** backend:
   ```yaml
   # application.yml
   license:
     vendors:
       - module_id: vendor-custom-module
         public_key: |
           -----BEGIN PUBLIC KEY-----
           MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
           -----END PUBLIC KEY-----
   ```

4. **Vendor generates** licenses for their clients:
   ```java
   PrivateKey privateKey = loadPrivateKey("vendor_private.pem");
   Algorithm algorithm = Algorithm.RSA256(null, (RSAPrivateKey) privateKey);
   
   String token = JWT.create()
     .withIssuer("partner.com")  // Vendor domain
     .withSubject("module:vendor-custom-module")
     .withAudience("tenant:client-a")
     .withExpiresAt(Date.from(Instant.parse("2025-12-31T23:59:59Z")))
     .withClaim("tenant_id", "client-a")
     .withClaim("module_id", "vendor-custom-module")
     .sign(algorithm);
   ```

5. **Vendor's client uploads** license via CORE Platform admin UI

---

## 📚 References

- **Module Registry:** [MODULE_REGISTRY.md](MODULE_REGISTRY.md)
- **JWT Specification:** [RFC 7519](https://datatracker.ietf.org/doc/html/rfc7519)
- **HMAC:** [RFC 2104](https://datatracker.ietf.org/doc/html/rfc2104)
- **RSA Signatures:** [RFC 8017](https://datatracker.ietf.org/doc/html/rfc8017)

---

**Last Updated:** 2025-10-26  
**Version:** 1.0.0  
**Status:** 🔮 PLANNED
