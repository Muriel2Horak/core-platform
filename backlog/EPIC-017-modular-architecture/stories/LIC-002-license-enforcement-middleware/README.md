---
id: LIC-002
epic: EPIC-017-modular-architecture
title: "License Enforcement Middleware"
priority: P1
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-017-modular-architecture/stories/LIC-002-license-enforcement-middleware/README.md
    - backlog/EPIC-017-modular-architecture/README.md
---

# LIC-002: License Enforcement Middleware

**Status:** ⏳ **PENDING**  
**Effort:** 2 dny  
**Priority:** 🔥 HIGH  
**Dependencies:** LIC-001  
**Category:** Licensing

---

## 📖 User Story

**As a platform**,  
I want to enforce license checks before API access,  
So that unlicensed modules return 403 Forbidden.

---

## 🎯 Acceptance Criteria

- ⏳ Spring interceptor checks license before module API calls
- ⏳ Returns `403 Forbidden` if license expired/invalid
- ⏳ Returns `402 Payment Required` if user limit exceeded
- ⏳ Caches validation result (Redis, 5 min TTL)
- ⏳ Bypass check for platform admin role
- ⏳ Audit log records license denials

---

## 🧩 Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Middleware hook in API/BFF | 0.5d | LIC-001 |
| 2 | Tenant-module lookup + policy evaluation | 0.5d | 1 |
| 3 | Deny/allow response handling | 0.25d | 2 |
| 4 | Metrics + audit logging | 0.25d | 2 |
| 5 | Integration tests | 0.25d | 2 |

---

## 💻 Implementation

### Spring Interceptor

```java
@Component
public class LicenseEnforcementInterceptor implements HandlerInterceptor {
    
    private static final Logger log = LoggerFactory.getLogger(LicenseEnforcementInterceptor.class);
    
    private final LicenseValidator licenseValidator;
    private final TenantContextHolder tenantContext;
    private final RedisTemplate<String, Boolean> redisTemplate;
    
    @Override
    public boolean preHandle(
        HttpServletRequest request, 
        HttpServletResponse response, 
        Object handler
    ) throws Exception {
        
        // 1. Extract module ID from path
        String path = request.getRequestURI();
        String moduleId = extractModuleId(path);
        
        if (moduleId == null) {
            return true; // Not a module API call
        }
        
        // 2. Bypass for platform admin
        if (hasRole("PLATFORM_ADMIN")) {
            return true;
        }
        
        // 3. Get tenant ID
        String tenantId = tenantContext.getCurrentTenantId();
        
        // 4. Check cache first
        String cacheKey = "license:valid:" + tenantId + ":" + moduleId;
        Boolean cachedValid = redisTemplate.opsForValue().get(cacheKey);
        
        if (Boolean.TRUE.equals(cachedValid)) {
            return true; // License valid (cached)
        }
        
        // 5. Validate license
        try {
            LicenseValidationResult result = licenseValidator.validate(moduleId, tenantId);
            
            if (!result.isValid()) {
                log.warn("License validation failed: {} for tenant {}", 
                    result.getError(), tenantId);
                
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write(String.format(
                    "{\"error\": \"License invalid\", \"reason\": \"%s\"}", 
                    result.getError()
                ));
                
                // Audit log
                auditLog(tenantId, moduleId, "LICENSE_DENIED", result.getError());
                
                return false;
            }
            
            // 6. Check user limit
            int activeUsers = countActiveUsers(tenantId);
            if (activeUsers > result.getMaxUsers()) {
                log.warn("User limit exceeded: {} > {} for tenant {}", 
                    activeUsers, result.getMaxUsers(), tenantId);
                
                response.setStatus(402); // Payment Required
                response.setContentType("application/json");
                response.getWriter().write(String.format(
                    "{\"error\": \"User limit exceeded\", \"active\": %d, \"max\": %d}", 
                    activeUsers, result.getMaxUsers()
                ));
                
                return false;
            }
            
            // 7. Cache valid result (5 min)
            redisTemplate.opsForValue().set(cacheKey, true, 5, TimeUnit.MINUTES);
            
            return true;
            
        } catch (Exception e) {
            log.error("License validation error", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return false;
        }
    }
    
    private String extractModuleId(String path) {
        // /api/modules/helpdesk/tickets → "helpdesk"
        if (path.startsWith("/api/modules/")) {
            String[] parts = path.split("/");
            if (parts.length >= 4) {
                return parts[3];
            }
        }
        return null;
    }
}
```

### Configuration

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Autowired
    private LicenseEnforcementInterceptor licenseInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(licenseInterceptor)
            .addPathPatterns("/api/modules/**")
            .excludePathPatterns("/api/modules/catalog"); // Public catalog
    }
}
```

---

## 🧪 Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class LicenseEnforcementTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private LicenseValidator licenseValidator;
    
    @Test
    void shouldReturn403WhenLicenseExpired() throws Exception {
        // Given: Expired license
        when(licenseValidator.validate("helpdesk", "customer-a"))
            .thenReturn(LicenseValidationResult.invalid("License expired"));
        
        // When/Then: Request denied
        mockMvc.perform(get("/api/modules/helpdesk/tickets"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("License invalid"))
            .andExpect(jsonPath("$.reason").value("License expired"));
    }
    
    @Test
    void shouldReturn402WhenUserLimitExceeded() throws Exception {
        // Given: Valid license but user limit exceeded
        when(licenseValidator.validate("helpdesk", "customer-a"))
            .thenReturn(LicenseValidationResult.valid(50)); // max 50 users
        
        // Mock 60 active users
        
        // When/Then: Payment required
        mockMvc.perform(get("/api/modules/helpdesk/tickets"))
            .andExpect(status().is(402))
            .andExpect(jsonPath("$.error").value("User limit exceeded"));
    }
}
```

---

**Last Updated:** 9. listopadu 2025
