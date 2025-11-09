# LIC-001: License JWT Validation

**Status:** ⏳ **PENDING**  
**Effort:** 3 dny  
**Priority:** 🔥 HIGH (kritické pro komerční moduly)  
**Dependencies:** -  
**Category:** Licensing

---

## 📖 User Story

**As a platform owner**,  
I want to validate module licenses using JWT signatures,  
So that only licensed tenants can use premium modules and tampering is prevented.

---

## 🎯 Acceptance Criteria

- ⏳ License is JWT token signed with RSA private key
- ⏳ Backend validates signature using public key
- ⏳ License contains: `moduleId`, `tenantId`, `validFrom`, `validTo`, `maxUsers`
- ⏳ Expired licenses rejected (403 Forbidden)
- ⏳ Tampered licenses rejected (invalid signature)
- ⏳ User count enforcement: If license allows 100 users, 101st user blocked
- ⏳ License cached (Redis) with TTL to avoid DB lookup on every request

---

## 🔐 License Structure (JWT)

### Claims

```json
{
  "iss": "core-platform.com",
  "sub": "module:helpdesk",
  "aud": "tenant:ivigee",
  "iat": 1704067200,
  "exp": 1735689600,
  "claims": {
    "moduleId": "helpdesk",
    "tenantId": "ivigee",
    "validFrom": "2024-01-01",
    "validTo": "2025-01-01",
    "maxUsers": 100,
    "features": ["sla", "automations", "reports"],
    "licenseType": "premium",
    "issuer": "CORE Team",
    "signature": "RSA-SHA256"
  }
}
```

### Example License Token

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJjb3JlLXBsYXRmb3JtLmNvbSIsInN1YiI6Im1vZHVsZTpoZWxwZGVzayIsImF1ZCI6InRlbmFudDppdmlnZWUiLCJpYXQiOjE3MDQwNjcyMDAsImV4cCI6MTczNTY4OTYwMCwiY2xhaW1zIjp7Im1vZHVsZUlkIjoiaGVscGRlc2siLCJ0ZW5hbnRJZCI6Iml2aWdlZSIsInZhbGlkRnJvbSI6IjIwMjQtMDEtMDEiLCJ2YWxpZFRvIjoiMjAyNS0wMS0wMSIsIm1heFVzZXJzIjoxMDAsImZlYXR1cmVzIjpbInNsYSIsImF1dG9tYXRpb25zIiwicmVwb3J0cyJdLCJsaWNlbnNlVHlwZSI6InByZW1pdW0iLCJpc3N1ZXIiOiJDT1JFIFRlYW0ifX0.signature_here
```

---

## 💻 Implementation

### Database Schema

**Table:** `module_licenses`

```sql
CREATE TABLE module_licenses (
    id BIGSERIAL PRIMARY KEY,
    module_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    license_token TEXT NOT NULL,              -- Full JWT
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    max_users INT,
    features JSONB,                           -- ["sla", "automations"]
    license_type VARCHAR(50) NOT NULL,        -- "free", "premium", "enterprise"
    issuer VARCHAR(100),
    uploaded_at TIMESTAMP DEFAULT NOW(),
    uploaded_by BIGINT,                       -- Admin user who uploaded
    
    UNIQUE(module_id, tenant_id),
    FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

CREATE INDEX idx_license_tenant_module ON module_licenses(tenant_id, module_id);
CREATE INDEX idx_license_expiry ON module_licenses(valid_to);
```

**Table:** `license_validation_log` (audit)

```sql
CREATE TABLE license_validation_log (
    id BIGSERIAL PRIMARY KEY,
    module_id VARCHAR(100) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    validation_result VARCHAR(20) NOT NULL,   -- "valid", "expired", "invalid_signature", "user_limit_exceeded"
    error_message TEXT,
    validated_at TIMESTAMP DEFAULT NOW(),
    
    CHECK (validation_result IN ('valid', 'expired', 'invalid_signature', 'tampered', 'user_limit_exceeded', 'not_found'))
);

CREATE INDEX idx_license_log_tenant ON license_validation_log(tenant_id);
CREATE INDEX idx_license_log_result ON license_validation_log(validation_result);
```

---

### Backend Implementation

**Service:** `LicenseValidator.java`

```java
package cz.muriel.core.licensing;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class LicenseValidator {
    
    private static final Logger log = LoggerFactory.getLogger(LicenseValidator.class);
    
    private final PublicKey publicKey;
    private final LicenseRepository licenseRepository;
    private final LicenseValidationLogger validationLogger;
    private final TenantUserCounter tenantUserCounter;
    
    public LicenseValidator(
        PublicKey publicKey,
        LicenseRepository licenseRepository,
        LicenseValidationLogger validationLogger,
        TenantUserCounter tenantUserCounter
    ) {
        this.publicKey = publicKey;
        this.licenseRepository = licenseRepository;
        this.validationLogger = validationLogger;
        this.tenantUserCounter = tenantUserCounter;
    }
    
    /**
     * Validate license for module + tenant.
     * Returns ValidationResult with status and error message.
     * 
     * Cached for 5 minutes to avoid DB lookup on every request.
     */
    @Cacheable(value = "licenses", key = "#moduleId + ':' + #tenantId")
    public LicenseValidationResult validate(String moduleId, String tenantId) {
        try {
            // 1. Fetch license from DB
            Optional<ModuleLicense> licenseOpt = licenseRepository
                .findByModuleIdAndTenantId(moduleId, tenantId);
            
            if (licenseOpt.isEmpty()) {
                log.warn("License not found: module={}, tenant={}", moduleId, tenantId);
                return LicenseValidationResult.notFound();
            }
            
            ModuleLicense license = licenseOpt.get();
            
            // 2. Parse JWT
            Claims claims;
            try {
                claims = Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(license.getLicenseToken())
                    .getBody();
            } catch (JwtException e) {
                log.error("Invalid JWT signature: module={}, tenant={}", moduleId, tenantId, e);
                validationLogger.logValidation(moduleId, tenantId, "invalid_signature", e.getMessage());
                return LicenseValidationResult.invalidSignature(e.getMessage());
            }
            
            // 3. Verify claims
            LicenseClaims licenseClaims = parseClaims(claims);
            
            // 3.1 Check module ID matches
            if (!licenseClaims.getModuleId().equals(moduleId)) {
                String error = String.format("Module ID mismatch: expected=%s, actual=%s", 
                                           moduleId, licenseClaims.getModuleId());
                log.error(error);
                return LicenseValidationResult.tampered(error);
            }
            
            // 3.2 Check tenant ID matches
            if (!licenseClaims.getTenantId().equals(tenantId)) {
                String error = String.format("Tenant ID mismatch: expected=%s, actual=%s", 
                                           tenantId, licenseClaims.getTenantId());
                log.error(error);
                return LicenseValidationResult.tampered(error);
            }
            
            // 3.3 Check expiration
            LocalDate now = LocalDate.now();
            if (now.isBefore(licenseClaims.getValidFrom())) {
                String error = String.format("License not yet valid: validFrom=%s", 
                                           licenseClaims.getValidFrom());
                log.warn(error);
                validationLogger.logValidation(moduleId, tenantId, "not_yet_valid", error);
                return LicenseValidationResult.expired(error);
            }
            
            if (now.isAfter(licenseClaims.getValidTo())) {
                String error = String.format("License expired: validTo=%s", 
                                           licenseClaims.getValidTo());
                log.warn(error);
                validationLogger.logValidation(moduleId, tenantId, "expired", error);
                return LicenseValidationResult.expired(error);
            }
            
            // 3.4 Check user count limit
            if (licenseClaims.getMaxUsers() != null) {
                int activeUsers = tenantUserCounter.countActiveUsers(tenantId);
                if (activeUsers > licenseClaims.getMaxUsers()) {
                    String error = String.format("User limit exceeded: active=%d, max=%d", 
                                               activeUsers, licenseClaims.getMaxUsers());
                    log.warn(error);
                    validationLogger.logValidation(moduleId, tenantId, "user_limit_exceeded", error);
                    return LicenseValidationResult.userLimitExceeded(error);
                }
            }
            
            // 4. All checks passed
            log.debug("License valid: module={}, tenant={}", moduleId, tenantId);
            validationLogger.logValidation(moduleId, tenantId, "valid", null);
            return LicenseValidationResult.valid(licenseClaims);
            
        } catch (Exception e) {
            log.error("License validation error: module={}, tenant={}", moduleId, tenantId, e);
            return LicenseValidationResult.error(e.getMessage());
        }
    }
    
    /**
     * Parse JWT claims to LicenseClaims POJO.
     */
    private LicenseClaims parseClaims(Claims claims) {
        @SuppressWarnings("unchecked")
        var claimsMap = (Map<String, Object>) claims.get("claims");
        
        return LicenseClaims.builder()
            .moduleId((String) claimsMap.get("moduleId"))
            .tenantId((String) claimsMap.get("tenantId"))
            .validFrom(LocalDate.parse((String) claimsMap.get("validFrom")))
            .validTo(LocalDate.parse((String) claimsMap.get("validTo")))
            .maxUsers((Integer) claimsMap.get("maxUsers"))
            .features((List<String>) claimsMap.get("features"))
            .licenseType((String) claimsMap.get("licenseType"))
            .issuer((String) claimsMap.get("issuer"))
            .build();
    }
}
```

**POJO:** `LicenseValidationResult.java`

```java
package cz.muriel.core.licensing;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LicenseValidationResult {
    boolean valid;
    String status; // "valid", "expired", "invalid_signature", "user_limit_exceeded", "not_found", "tampered"
    String errorMessage;
    LicenseClaims claims;
    
    public static LicenseValidationResult valid(LicenseClaims claims) {
        return LicenseValidationResult.builder()
            .valid(true)
            .status("valid")
            .claims(claims)
            .build();
    }
    
    public static LicenseValidationResult expired(String message) {
        return LicenseValidationResult.builder()
            .valid(false)
            .status("expired")
            .errorMessage(message)
            .build();
    }
    
    public static LicenseValidationResult invalidSignature(String message) {
        return LicenseValidationResult.builder()
            .valid(false)
            .status("invalid_signature")
            .errorMessage(message)
            .build();
    }
    
    public static LicenseValidationResult tampered(String message) {
        return LicenseValidationResult.builder()
            .valid(false)
            .status("tampered")
            .errorMessage(message)
            .build();
    }
    
    public static LicenseValidationResult userLimitExceeded(String message) {
        return LicenseValidationResult.builder()
            .valid(false)
            .status("user_limit_exceeded")
            .errorMessage(message)
            .build();
    }
    
    public static LicenseValidationResult notFound() {
        return LicenseValidationResult.builder()
            .valid(false)
            .status("not_found")
            .errorMessage("License not found for this module and tenant")
            .build();
    }
    
    public static LicenseValidationResult error(String message) {
        return LicenseValidationResult.builder()
            .valid(false)
            .status("error")
            .errorMessage(message)
            .build();
    }
}
```

**POJO:** `LicenseClaims.java`

```java
package cz.muriel.core.licensing;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;

@Value
@Builder
public class LicenseClaims {
    String moduleId;
    String tenantId;
    LocalDate validFrom;
    LocalDate validTo;
    Integer maxUsers;
    List<String> features;
    String licenseType;
    String issuer;
}
```

---

### License Generation Tool (Vendor-side)

**CLI Tool:** `LicenseGenerator.java` (runs offline)

```java
package cz.muriel.core.licensing.tools;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CLI tool to generate module licenses.
 * Usage: java LicenseGenerator --module helpdesk --tenant ivigee --months 12 --users 100
 */
public class LicenseGenerator {
    
    public static void main(String[] args) throws Exception {
        // Parse CLI args
        String moduleId = getArg(args, "--module");
        String tenantId = getArg(args, "--tenant");
        int months = Integer.parseInt(getArg(args, "--months"));
        int maxUsers = Integer.parseInt(getArg(args, "--users"));
        
        // Load private key from file (or generate for demo)
        KeyPair keyPair = generateKeyPair(); // In prod: load from secure storage
        
        // Generate license
        String license = generateLicense(
            moduleId, 
            tenantId, 
            months, 
            maxUsers, 
            keyPair
        );
        
        System.out.println("Generated License:");
        System.out.println(license);
    }
    
    private static String generateLicense(
        String moduleId,
        String tenantId,
        int months,
        int maxUsers,
        KeyPair keyPair
    ) {
        LocalDate now = LocalDate.now();
        LocalDate validTo = now.plusMonths(months);
        
        Map<String, Object> claims = new HashMap<>();
        claims.put("moduleId", moduleId);
        claims.put("tenantId", tenantId);
        claims.put("validFrom", now.toString());
        claims.put("validTo", validTo.toString());
        claims.put("maxUsers", maxUsers);
        claims.put("features", List.of("sla", "automations", "reports"));
        claims.put("licenseType", "premium");
        claims.put("issuer", "CORE Team");
        
        return Jwts.builder()
            .setIssuer("core-platform.com")
            .setSubject("module:" + moduleId)
            .setAudience("tenant:" + tenantId)
            .setIssuedAt(new Date())
            .setExpiration(Date.from(validTo.atStartOfDay(ZoneId.systemDefault()).toInstant()))
            .claim("claims", claims)
            .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
            .compact();
    }
    
    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        return keyGen.generateKeyPair();
    }
    
    private static String getArg(String[] args, String flag) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(flag)) {
                return args[i + 1];
            }
        }
        throw new IllegalArgumentException("Missing argument: " + flag);
    }
}
```

---

## 🧪 Testing

### Unit Test

```java
@SpringBootTest
class LicenseValidatorTest {
    
    @Autowired
    private LicenseValidator validator;
    
    @Autowired
    private LicenseRepository licenseRepository;
    
    private KeyPair keyPair;
    
    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();
    }
    
    @Test
    void shouldValidateValidLicense() {
        // Given: Valid license in DB
        String license = generateValidLicense("helpdesk", "ivigee", 12);
        licenseRepository.save(new ModuleLicense(
            "helpdesk", "ivigee", license, 
            LocalDate.now(), LocalDate.now().plusMonths(12), 100
        ));
        
        // When: Validate
        LicenseValidationResult result = validator.validate("helpdesk", "ivigee");
        
        // Then: Valid
        assertThat(result.isValid()).isTrue();
        assertThat(result.getStatus()).isEqualTo("valid");
    }
    
    @Test
    void shouldRejectExpiredLicense() {
        // Given: Expired license (validTo in past)
        String license = generateExpiredLicense("helpdesk", "ivigee");
        licenseRepository.save(new ModuleLicense(
            "helpdesk", "ivigee", license, 
            LocalDate.now().minusMonths(12), LocalDate.now().minusDays(1), 100
        ));
        
        // When: Validate
        LicenseValidationResult result = validator.validate("helpdesk", "ivigee");
        
        // Then: Expired
        assertThat(result.isValid()).isFalse();
        assertThat(result.getStatus()).isEqualTo("expired");
    }
    
    @Test
    void shouldRejectTamperedLicense() {
        // Given: License with tampered claims (changed moduleId)
        String validLicense = generateValidLicense("helpdesk", "ivigee", 12);
        String tamperedLicense = validLicense.replace("helpdesk", "crm");
        
        licenseRepository.save(new ModuleLicense(
            "crm", "ivigee", tamperedLicense, 
            LocalDate.now(), LocalDate.now().plusMonths(12), 100
        ));
        
        // When: Validate
        LicenseValidationResult result = validator.validate("crm", "ivigee");
        
        // Then: Invalid signature
        assertThat(result.isValid()).isFalse();
        assertThat(result.getStatus()).isEqualTo("invalid_signature");
    }
    
    @Test
    void shouldEnforceUserLimit() {
        // Given: License with maxUsers=100, but tenant has 105 active users
        String license = generateValidLicense("helpdesk", "ivigee", 12);
        licenseRepository.save(new ModuleLicense(
            "helpdesk", "ivigee", license, 
            LocalDate.now(), LocalDate.now().plusMonths(12), 100
        ));
        
        // Mock: 105 active users
        when(tenantUserCounter.countActiveUsers("ivigee")).thenReturn(105);
        
        // When: Validate
        LicenseValidationResult result = validator.validate("helpdesk", "ivigee");
        
        // Then: User limit exceeded
        assertThat(result.isValid()).isFalse();
        assertThat(result.getStatus()).isEqualTo("user_limit_exceeded");
    }
    
    private String generateValidLicense(String moduleId, String tenantId, int months) {
        // Use LicenseGenerator logic
    }
}
```

---

## 📊 Success Metrics

- **Validation time:** <10ms (with Redis cache)
- **Cache hit ratio:** >95% (licenses rarely change)
- **Tamper detection:** 100% invalid signatures rejected
- **User limit enforcement:** 100% accuracy (no unauthorized users)

---

## 🚀 Rollout Plan

1. **Key generation** (0.5 day)
   - Generate RSA key pair (2048-bit)
   - Store private key securely (vault)
   - Distribute public key to backend

2. **Database schema** (0.5 day)
   - Create `module_licenses` table
   - Create `license_validation_log` table

3. **Validator implementation** (1 day)
   - Implement `LicenseValidator`
   - JWT parsing + signature validation
   - Claims verification (expiry, user count)

4. **License generator tool** (0.5 day)
   - CLI tool for vendors
   - Generate licenses offline

5. **Testing** (0.5 day)
   - Unit tests (valid, expired, tampered)
   - Integration tests with Redis cache

**Total:** 3 days

---

## 🔒 Security Considerations

1. **Private key protection:**
   - NEVER commit to Git
   - Store in HashiCorp Vault / AWS Secrets Manager
   - Rotate annually

2. **Public key distribution:**
   - Embedded in backend JAR
   - Can be updated via config (hot-reload)

3. **License revocation:**
   - Manual: Delete from `module_licenses` table
   - Automated: License server with revocation list (future)

4. **Audit logging:**
   - All validation attempts logged
   - Failed validations trigger alerts

---

## 📚 References

- **JWT:** <https://jwt.io/>
- **JJWT Library:** <https://github.com/jwtk/jjwt>
- **EPIC-017:** Modular Architecture & Licensing
- **LIC-002:** License Enforcement Middleware (next story)

---

**Last Updated:** 9. listopadu 2025  
**Status:** ⏳ PENDING  
**Next Action:** Generate RSA key pair + implement validator
