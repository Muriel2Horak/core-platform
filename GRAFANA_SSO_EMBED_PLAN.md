# Grafana SSO Embed - Implementační Plán

## Přehled
Kompletní migrace z @grafana/scenes na bezpečný iframe embed s per-user SSO přes Keycloak → BFF → Grafana JWT.

---

## ✅ HOTOVO: A1) Odstranění @grafana/scenes

- [x] Odstraněn `@grafana/scenes` z `package.json`
- [x] Smazány složky `src/scenes` a `src/components/Grafana` 
- [x] Vytvořena nová `GrafanaEmbed.tsx` komponenta s bezpečným iframe

---

## 🔄 TODO: A2) Integrace do UI + CSP Headers

### Frontend Pages/Router
**Soubor:** `frontend/src/pages/Admin/MonitoringComprehensivePage.tsx`

Nahradit:
```tsx
// STARÉ - odstraněno
import { SystemResourcesSceneWrapper } from '../../components/Grafana/SystemResourcesSceneWrapper';

<SystemResourcesSceneWrapper height={1200} />
```

Novým:
```tsx
import { GrafanaEmbed } from '../../components/GrafanaEmbed';

<GrafanaEmbed 
  path="/d/system-resources?orgId=1&theme=light&kiosk" 
  height="1200px" 
/>
```

**Dashboard UIDs** (budou vytvořeny v D8):
- System Resources: `/d/core-system-resources?orgId=1`
- Application Performance: `/d/core-app-performance?orgId=1`
- Platform Health: `/d/core-platform-health?orgId=1`
- Security: `/d/core-security?orgId=1`
- Audit: `/d/core-audit?orgId=1`
- Logs: `/d/core-logs?orgId=1`

### CSP Headers
**Soubor:** `docker/nginx/nginx.conf` (všechny server bloky)

Aktualizovat CSP:
```nginx
add_header Content-Security-Policy "default-src 'self'; \
  script-src 'self' 'unsafe-inline' 'unsafe-eval'; \
  style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; \
  font-src 'self' https://fonts.gstatic.com https://fonts.googleapis.com; \
  img-src 'self' data: https:; \
  connect-src 'self' https://core-platform.local https://*.core-platform.local wss://core-platform.local wss://*.core-platform.local; \
  frame-src 'self' https://admin.core-platform.local; \
  child-src 'self' https://admin.core-platform.local; \
  " always;

add_header Permissions-Policy "camera=(), microphone=(), geolocation=(), payment=()" always;
```

---

## 🔄 TODO: B) BFF Grafana JWT Endpoint

### B4) Java Backend Implementace

**1. GrafanaJwtService** (`backend/src/main/java/cz/muriel/core/monitoring/GrafanaJwtService.java`)

```java
@Service
public class GrafanaJwtService {
    
    private final RSAPrivateKey privateKey;
    private final RedisTemplate<String, String> redis;
    
    @Value("${grafana.jwt.ttl:120}") // 2 minuty
    private int jwtTtl;
    
    /**
     * Mint short-lived Grafana JWT z Keycloak tokenu
     */
    public String mintGrafanaJwt(KeycloakAuthenticationToken kcToken) {
        String realm = extractRealm(kcToken);
        String tenantId = extractTenantId(kcToken, realm);
        int grafanaOrgId = tenantRegistry.getGrafanaOrgId(tenantId);
        String role = mapKeycloakRoleToGrafana(kcToken);
        
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        
        String jwt = JWT.create()
            .withSubject(kcToken.getName())
            .withClaim("email", extractEmail(kcToken))
            .withClaim("name", extractName(kcToken))
            .withClaim("orgId", grafanaOrgId)
            .withClaim("role", role) // Viewer|Editor|Admin
            .withIssuedAt(now)
            .withExpiresAt(now.plusSeconds(jwtTtl))
            .withJWTId(jti)
            .sign(Algorithm.RSA256(null, privateKey));
        
        // Store JTI for replay protection
        redis.opsForValue().set("grafana:jti:" + jti, "used", jwtTtl, TimeUnit.SECONDS);
        
        return jwt;
    }
    
    private String mapKeycloakRoleToGrafana(KeycloakAuthenticationToken token) {
        if (hasRole(token, "CORE_ROLE_ADMIN")) return "Admin";
        if (hasRole(token, "CORE_ROLE_MONITORING")) return "Editor";
        if (hasRole(token, "CORE_TENANT_ADMIN")) return "Editor";
        if (hasRole(token, "CORE_ROLE_TENANT_MONITORING")) return "Viewer";
        return "Viewer";
    }
}
```

**2. AuthRequestController** (`backend/src/main/java/cz/muriel/core/monitoring/AuthRequestController.java`)

```java
@RestController
@RequestMapping("/internal/auth")
public class AuthRequestController {
    
    @Autowired
    private GrafanaJwtService jwtService;
    
    /**
     * Internal endpoint for Nginx auth_request
     * Returns 200 + Grafana-JWT header if valid
     */
    @GetMapping("/grafana")
    @RateLimiter(name = "grafana-auth", fallbackMethod = "rateLimitFallback")
    public ResponseEntity<Void> authenticateForGrafana(
        @AuthenticationPrincipal KeycloakAuthenticationToken token
    ) {
        if (token == null || !token.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            String grafanaJwt = jwtService.mintGrafanaJwt(token);
            
            return ResponseEntity.ok()
                .header("Grafana-JWT", grafanaJwt)
                .build();
                
        } catch (Exception e) {
            log.error("Failed to mint Grafana JWT", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    public ResponseEntity<Void> rateLimitFallback(Exception e) {
        return ResponseEntity.status(429).build();
    }
}
```

**3. TenantRegistry** (cache pro tenant → orgId mapping)

```java
@Component
public class TenantGrafanaRegistry {
    
    private final Map<String, Integer> tenantToOrgId = new ConcurrentHashMap<>();
    
    public int getGrafanaOrgId(String tenantId) {
        return tenantToOrgId.computeIfAbsent(tenantId, 
            tid -> provisionTenant(tid));
    }
    
    private int provisionTenant(String tenantId) {
        // Call provisioning script or Grafana API
        // Returns orgId for tenant
    }
}
```

### B5) Unit + IT Testy

**GrafanaJwtServiceTest.java**
- `testMintJwt_ValidToken_ReturnsJwt()`
- `testMintJwt_ExpiredAfterTtl()`
- `testRoleMapping_AdminRole_ReturnsAdmin()`
- `testJtiStored_InRedis()`

**AuthRequestControllerIT.java**
- `testAuthRequest_ValidUser_Returns200WithJwt()`
- `testAuthRequest_InvalidUser_Returns401()`
- `testAuthRequest_RateLimit_Returns429()`

---

## 🔄 TODO: C) Nginx Auth Bridge

**Soubor:** `docker/nginx/nginx.conf`

### 1. Internal Auth Endpoint

```nginx
# Internal auth bridge - mint Grafana JWT
location = /_auth/grafana {
    internal;
    proxy_pass http://backend:8080/internal/auth/grafana;
    proxy_set_header Authorization $http_authorization;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_pass_request_body off;
    proxy_set_header Content-Length "";
}
```

### 2. Capture JWT from Response

```nginx
# Extract JWT from BFF response
map $upstream_http_grafana_jwt $grafana_sso_jwt { 
    default $upstream_http_grafana_jwt; 
}

# WebSocket upgrade support
map $http_upgrade $connection_upgrade {
    default upgrade;
    '' close;
}
```

### 3. Monitoring Proxy s Auth

**core-platform.local:**
```nginx
location /monitoring/ {
    auth_request /_auth/grafana;
    auth_request_set $grafana_token $upstream_http_grafana_jwt;
    
    proxy_set_header X-Org-JWT $grafana_token;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    
    # WebSocket support
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection $connection_upgrade;
    
    # Timeouts
    proxy_connect_timeout 60s;
    proxy_send_timeout 60s;
    proxy_read_timeout 60s;
    
    # CSP for iframe embedding
    add_header Content-Security-Policy "frame-ancestors 'self' https://admin.core-platform.local" always;
    
    proxy_pass http://grafana:3000/;
}
```

**admin.core-platform.local** (full UI):
```nginx
location /monitoring/ {
    proxy_pass http://grafana:3000/;
    # ... stejné hlavičky ale BEZ auth_request (OAuth2 v Grafaně)
}
```

---

## 🔄 TODO: D) Grafana Konfigurace + Provisioning

### D7) grafana.ini

**Soubor:** `docker/grafana/grafana.ini`

```ini
[server]
protocol = http
http_port = 3000
domain = admin.core-platform.local
root_url = https://%(domain)s/monitoring
serve_from_sub_path = true

[security]
allow_embedding = true
cookie_secure = true
cookie_samesite = none

[auth.jwt]
enabled = true
header_name = X-Org-JWT
username_claim = preferred_username
email_claim = email
jwk_set_url = https://keycloak:8443/realms/admin/protocol/openid-connect/certs
cache_ttl = 60m
auto_sign_up = true
role_attribute_path = contains(realm_access.roles[*], 'CORE_ROLE_ADMIN') && 'Admin' || contains(realm_access.roles[*], 'CORE_ROLE_MONITORING') && 'Editor' || 'Viewer'
skip_org_role_sync = false

[auth]
disable_login_form = false

[auth.generic_oauth]
enabled = true
name = Keycloak
allow_sign_up = true
client_id = grafana
client_secret = ${GRAFANA_OAUTH_SECRET}
scopes = openid profile email
auth_url = https://admin.core-platform.local/realms/admin/protocol/openid-connect/auth
token_url = https://keycloak:8443/realms/admin/protocol/openid-connect/token
api_url = https://keycloak:8443/realms/admin/protocol/openid-connect/userinfo
role_attribute_path = contains(realm_access.roles[*], 'CORE_ROLE_ADMIN') && 'Admin' || contains(realm_access.roles[*], 'CORE_ROLE_MONITORING') && 'Editor' || 'Viewer'
tls_skip_verify_insecure = true
```

### D8) Provisioning Script

**Soubor:** `docker/grafana/provision-all.sh`

```bash
#!/bin/bash
set -eo pipefail

GRAFANA_URL="${GRAFANA_URL:-http://grafana:3000}"
ADMIN_USER="${GRAFANA_ADMIN_USER:-admin}"
ADMIN_PASS="${GRAFANA_ADMIN_PASSWORD:-admin}"

# Get service account token
TOKEN=$(curl -s -X POST "$GRAFANA_URL/api/serviceaccounts" \
  -u "$ADMIN_USER:$ADMIN_PASS" \
  -H "Content-Type: application/json" \
  -d '{"name":"provisioner","role":"Admin"}' | jq -r '.id')

SA_TOKEN=$(curl -s -X POST "$GRAFANA_URL/api/serviceaccounts/$TOKEN/tokens" \
  -u "$ADMIN_USER:$ADMIN_PASS" \
  -H "Content-Type: application/json" \
  -d '{"name":"provisioner-key"}' | jq -r '.key')

# Provision each tenant
for TENANT in $TENANTS; do
    echo "Provisioning tenant: $TENANT"
    
    # 1. Create Org
    ORG_ID=$(curl -s -X POST "$GRAFANA_URL/api/orgs" \
      -H "Authorization: Bearer $SA_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"name\":\"$TENANT\"}" | jq -r '.orgId')
    
    # 2. Create Datasources (Prometheus + Loki)
    curl -X POST "$GRAFANA_URL/api/datasources" \
      -H "Authorization: Bearer $SA_TOKEN" \
      -H "X-Grafana-Org-Id: $ORG_ID" \
      -H "Content-Type: application/json" \
      -d @- <<EOF
{
  "name": "Prometheus",
  "type": "prometheus",
  "url": "http://prometheus:9090",
  "access": "proxy",
  "isDefault": true,
  "jsonData": {
    "httpHeaderName1": "X-Scope-OrgID"
  },
  "secureJsonData": {
    "httpHeaderValue1": "$TENANT"
  }
}
EOF

    curl -X POST "$GRAFANA_URL/api/datasources" \
      -H "Authorization: Bearer $SA_TOKEN" \
      -H "X-Grafana-Org-Id: $ORG_ID" \
      -H "Content-Type: application/json" \
      -d @- <<EOF
{
  "name": "Loki",
  "type": "loki",
  "url": "http://loki:3100",
  "access": "proxy",
  "jsonData": {
    "httpHeaderName1": "X-Scope-OrgID"
  },
  "secureJsonData": {
    "httpHeaderValue1": "$TENANT"
  }
}
EOF
    
    # 3. Create Folders
    for FOLDER in "System Resources" "Application Performance" "Platform Health" "Security" "Audit" "Logs"; do
        curl -X POST "$GRAFANA_URL/api/folders" \
          -H "Authorization: Bearer $SA_TOKEN" \
          -H "X-Grafana-Org-Id: $ORG_ID" \
          -H "Content-Type: application/json" \
          -d "{\"title\":\"$FOLDER\"}"
    done
    
    # 4. Create Teams
    for TEAM in "Ops" "Dev" "Viewer"; do
        curl -X POST "$GRAFANA_URL/api/teams" \
          -H "Authorization: Bearer $SA_TOKEN" \
          -H "X-Grafana-Org-Id: $ORG_ID" \
          -H "Content-Type: application/json" \
          -d "{\"name\":\"$TEAM\",\"email\":\"$TEAM@$TENANT.local\"}"
    done
    
    # 5. Import Dashboards (UIDs podle A2 sekce)
    # ... import JSON dashboardů
    
    echo "✅ Tenant $TENANT provisioned with orgId=$ORG_ID"
done
```

### D9) OSS Teams Sync (Keycloak groups → Grafana Teams)

**Soubor:** `backend/.../GrafanaTeamSyncService.java`

```java
@Service
public class GrafanaTeamSyncService {
    
    @Autowired
    private GrafanaApiClient grafanaApi;
    
    @Cacheable(value = "grafana-team-sync", unless = "#result == false")
    public boolean syncUserTeams(String username, int orgId, List<String> kcGroups) {
        try {
            // 1. Get or create user in Grafana org
            int userId = grafanaApi.getOrCreateUser(username, orgId);
            
            // 2. Map KC groups to Grafana teams
            List<String> teamNames = kcGroups.stream()
                .map(this::mapGroupToTeam)
                .filter(Objects::nonNull)
                .toList();
            
            // 3. Get existing teams
            List<Team> existingTeams = grafanaApi.listTeams(orgId);
            
            // 4. Create missing teams
            for (String teamName : teamNames) {
                if (existingTeams.stream().noneMatch(t -> t.getName().equals(teamName))) {
                    grafanaApi.createTeam(orgId, teamName);
                }
            }
            
            // 5. Sync user membership
            List<Team> currentMembership = grafanaApi.getUserTeams(userId, orgId);
            
            // Add to new teams
            for (String teamName : teamNames) {
                if (currentMembership.stream().noneMatch(t -> t.getName().equals(teamName))) {
                    int teamId = grafanaApi.getTeamIdByName(orgId, teamName);
                    grafanaApi.addUserToTeam(teamId, userId);
                }
            }
            
            // Remove from old teams
            for (Team team : currentMembership) {
                if (!teamNames.contains(team.getName())) {
                    grafanaApi.removeUserFromTeam(team.getId(), userId);
                }
            }
            
            return true;
        } catch (Exception e) {
            log.error("Failed to sync user teams for {}", username, e);
            return false;
        }
    }
    
    private String mapGroupToTeam(String kcGroup) {
        if (kcGroup.contains("MONITORING_OPS")) return "Ops";
        if (kcGroup.contains("MONITORING_DEV")) return "Dev";
        return null;
    }
}
```

Volat v `GrafanaJwtService.mintGrafanaJwt()` před return:
```java
List<String> groups = extractGroups(kcToken);
teamSyncService.syncUserTeams(username, grafanaOrgId, groups);
```

---

## 🔄 TODO: F) Testy

### F10) Pre-Deploy Testy

**1. Unit Testy (již v B5)**

**2. Playwright Smoke Test**

**Soubor:** `e2e/tests/grafana-embed-smoke.spec.ts`

```typescript
import { test, expect } from '@playwright/test';

test.describe('Grafana Embed Smoke Test', () => {
  test.beforeEach(async ({ page }) => {
    // Login via Keycloak
    await page.goto('https://core-platform.local');
    await page.fill('input[name="username"]', 'test');
    await page.fill('input[name="password"]', 'Test.1234');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');
  });
  
  test('should load Grafana iframe without tokens in URL', async ({ page }) => {
    await page.goto('https://core-platform.local/monitoring-comprehensive');
    
    // Wait for iframe to load
    const iframe = page.frameLocator('iframe[src^="/monitoring"]').first();
    await iframe.locator('body').waitFor({ timeout: 10000 });
    
    // Check iframe src doesn't contain tokens
    const iframeSrc = await page.locator('iframe[src^="/monitoring"]').first().getAttribute('src');
    expect(iframeSrc).not.toContain('token=');
    expect(iframeSrc).not.toContain('auth=');
    
    // Check Grafana UI loaded
    await expect(iframe.locator('[aria-label*="Grafana"]')).toBeVisible({ timeout: 15000 });
    
    // Screenshot
    await page.screenshot({ path: 'test-results/grafana-embed-smoke.png', fullPage: true });
  });
  
  test('should not require additional login', async ({ page }) => {
    await page.goto('https://core-platform.local/monitoring-comprehensive');
    
    const iframe = page.frameLocator('iframe[src^="/monitoring"]').first();
    
    // Should NOT see login form
    await expect(iframe.locator('input[name="user"]')).not.toBeVisible({ timeout: 5000 });
    
    // Should see dashboard content
    await expect(iframe.locator('.dashboard-container, [data-testid="dashboard"]')).toBeVisible({ timeout: 15000 });
  });
});
```

**3. ZAP Baseline Scan**

**Soubor:** `.github/workflows/security-scan.yml`

```yaml
- name: ZAP Baseline Scan - Grafana Embed
  run: |
    docker run -t owasp/zap2docker-stable zap-baseline.py \
      -t https://core-platform.local/monitoring-comprehensive \
      -r zap-report.html \
      -c zap-rules.conf
    
    # Check for critical issues
    grep -q "FAIL-NEW: 0" zap-report.html || exit 1
```

**Soubor:** `zap-rules.conf`
```
# Fail on:
# - Tokens in URL/HTML
# - Missing frame-ancestors
# - Insecure cookies
10029 FAIL  # Cookies without Secure flag
10030 FAIL  # Cookies without HttpOnly flag
10049 FAIL  # Token in URL
10038 FAIL  # Missing CSP frame-ancestors
```

### F11) E2E Post-Deploy Testy

**Soubor:** `e2e/tests/grafana-embed-e2e.spec.ts`

```typescript
test.describe('Grafana Embed E2E - Post-Deploy', () => {
  
  test('tenant isolation: user A cannot access org B', async ({ page, context }) => {
    // Login as tenant A user
    await loginAs(page, 'user-a@tenant-a.local', 'password');
    await page.goto('https://tenant-a.core-platform.local/monitoring-comprehensive');
    
    const iframe = page.frameLocator('iframe[src^="/monitoring"]').first();
    await expect(iframe.locator('.dashboard-container')).toBeVisible();
    
    // Try to access tenant B dashboard via URL manipulation
    await page.goto('https://core-platform.local/monitoring/d/system-resources?orgId=2'); // Org 2 = tenant B
    
    // Should be redirected or show error
    await expect(page.locator('text=/Access Denied|Forbidden|Not Found/i')).toBeVisible({ timeout: 5000 });
  });
  
  test('RBAC: Viewer cannot edit dashboard', async ({ page }) => {
    await loginAs(page, 'viewer@tenant-a.local', 'password');
    await page.goto('https://tenant-a.core-platform.local/monitoring/d/system-resources?orgId=1');
    
    const iframe = page.frameLocator('iframe[src^="/monitoring"]').first();
    
    // Edit button should be hidden or disabled
    await expect(iframe.locator('button:has-text("Edit")')).not.toBeVisible();
  });
  
  test('RBAC: Editor can save dashboard', async ({ page }) => {
    await loginAs(page, 'editor@tenant-a.local', 'password');
    await page.goto('https://tenant-a.core-platform.local/monitoring/d/system-resources?orgId=1');
    
    const iframe = page.frameLocator('iframe[src^="/monitoring"]').first();
    
    // Edit button should be visible
    await expect(iframe.locator('button:has-text("Edit")')).toBeVisible();
    await iframe.locator('button:has-text("Edit")').click();
    
    // Save should work
    await iframe.locator('button:has-text("Save")').click();
    await expect(iframe.locator('text=/Dashboard saved/i')).toBeVisible({ timeout: 5000 });
  });
  
  test('Teams sync: adding KC group reflects in Grafana', async ({ page, request }) => {
    const username = 'test-user@tenant-a.local';
    
    // 1. Add user to KC group "CORE_MONITORING_OPS"
    await addUserToKeycloakGroup(request, username, 'CORE_MONITORING_OPS');
    
    // 2. Login (triggers team sync)
    await loginAs(page, username, 'password');
    
    // 3. Check Grafana team membership via API
    const teams = await getGrafanaUserTeams(request, username, 1); // orgId=1
    expect(teams).toContainEqual(expect.objectContaining({ name: 'Ops' }));
  });
  
  test('WebSocket: Explore live data works', async ({ page }) => {
    await loginAs(page, 'editor@tenant-a.local', 'password');
    await page.goto('https://tenant-a.core-platform.local/monitoring/explore');
    
    const iframe = page.frameLocator('iframe[src^="/monitoring"]').first();
    
    // Wait for Explore UI
    await iframe.locator('input[placeholder*="metric"]').waitFor();
    
    // Check WebSocket connection established (via network tab or logs)
    const wsMessages = [];
    page.on('websocket', ws => {
      ws.on('framereceived', event => wsMessages.push(event.payload));
    });
    
    await page.waitForTimeout(2000);
    expect(wsMessages.length).toBeGreaterThan(0);
  });
  
  test('TTL/Refresh: JWT expires but seamlessly refreshes', async ({ page }) => {
    await loginAs(page, 'viewer@tenant-a.local', 'password');
    await page.goto('https://tenant-a.core-platform.local/monitoring-comprehensive');
    
    const iframe = page.frameLocator('iframe[src^="/monitoring"]').first();
    await expect(iframe.locator('.dashboard-container')).toBeVisible();
    
    // Wait for JWT to expire (TTL=120s, wait 130s)
    await page.waitForTimeout(130 * 1000);
    
    // Navigate within iframe (triggers new request)
    await iframe.locator('a:has-text("Explore")').click();
    
    // Should still work (BFF minted new JWT)
    await expect(iframe.locator('input[placeholder*="metric"]')).toBeVisible({ timeout: 10000 });
  });
});
```

---

## 🔄 TODO: G) Dokumentace

### G12) Dokumentace

**Soubor:** `docs/GRAFANA_EMBED_SSO.md`

```markdown
# Grafana Secure Embed with SSO

## Architecture

```
User → FE (React) → <iframe src="/monitoring/..."> 
                      ↓
                    Nginx auth_request → BFF /internal/auth/grafana
                                           ↓
                                         Mint JWT (60s TTL, jti, RS256)
                                           ↓
                    Nginx → Grafana (X-Org-JWT header)
                              ↓
                            [auth.jwt] validates JWT
                              ↓
                            User auto-created, assigned to Org, Role applied
```

## Security

1. **No Tokens in URL**: JWT only in `X-Org-JWT` header (Nginx → Grafana)
2. **Short TTL**: 60-120s, automatic refresh on navigation
3. **Replay Protection**: JTI stored in Redis with TTL
4. **Sandbox**: `allow-scripts allow-same-origin allow-forms`
5. **CSP**: `frame-ancestors 'self' https://admin.core-platform.local`
6. **Rate Limiting**: 20 req/min per user on `/internal/auth/grafana`

## RBAC

### Keycloak Role → Grafana Role
- `CORE_ROLE_ADMIN` → Admin
- `CORE_ROLE_MONITORING` → Editor
- `CORE_TENANT_ADMIN` → Editor
- `CORE_ROLE_TENANT_MONITORING` → Viewer
- Default → Viewer

### Folder Permissions
- Folders created per org: "System Resources", "Application Performance", etc.
- Teams: "Ops" (Editor), "Dev" (Editor), "Viewer" (Viewer)
- Dashboards inherit folder permissions

### Teams Sync (OSS Fallback)
- On JWT mint, BFF calls Grafana API to:
  - Create missing teams
  - Add/remove user from teams based on KC groups
- Cached per user (5 min TTL)

## Datasource Isolation

**Ideal (Mimir/Loki multi-tenant):**
```json
{
  "jsonData": {
    "httpHeaderName1": "X-Scope-OrgID"
  },
  "secureJsonData": {
    "httpHeaderValue1": "tenant-a"
  }
}
```

**Current (single Prometheus/Loki):**
- Datasource per org, but shared backend
- Query-level isolation not enforced (best-effort)
- **TODO**: Migrate to Mimir/Loki with X-Scope-OrgID

## Provisioning

Run `docker/grafana/provision-all.sh`:
1. Creates org per tenant
2. Creates datasources (Prometheus + Loki)
3. Creates folders
4. Creates teams
5. Imports dashboards

**Idempotent**: Safe to re-run.

## Troubleshooting

### "Container element not found"
- Old issue from Scenes, no longer applicable

### 401 Unauthorized in iframe
- Check BFF logs: `/internal/auth/grafana` endpoint
- Verify KC token is valid and not expired
- Check Redis for JTI replay issues

### WebSocket connection fails
- Verify Nginx `proxy_set_header Upgrade $http_upgrade`
- Check `Connection $connection_upgrade` mapping
- Ensure `proxy_http_version 1.1`

### User not in correct org
- Check `TenantGrafanaRegistry` mapping
- Verify provisioning script created org
- Check BFF logs for `mintGrafanaJwt` orgId claim

### Teams not syncing
- Check KC groups in token: `realm_access.roles` or `groups` claim
- Verify `GrafanaTeamSyncService.mapGroupToTeam()` logic
- Check Grafana API logs for team creation/membership calls
```

**README Update:**
```markdown
## Monitoring

Grafana dashboards are embedded via secure iframe with SSO.

- **Frontend**: `<GrafanaEmbed path="/d/dashboard-uid?orgId=1" />`
- **Authentication**: Automatic via Keycloak → BFF JWT → Grafana
- **No Scenes**: Removed `@grafana/scenes` dependency
- **RBAC**: Per-org, folder, team permissions from Keycloak roles

See [docs/GRAFANA_EMBED_SSO.md](docs/GRAFANA_EMBED_SSO.md) for details.
```

---

## Priority Order

1. **B4-B5**: BFF endpoint + testy (critical path)
2. **C6**: Nginx auth bridge (blocking iframe embed)
3. **D7**: Grafana.ini JWT config (blocking auth)
4. **A2**: FE komponenta integrace (user-visible)
5. **D8-D9**: Provisioning + Teams sync (tenant isolation)
6. **F10-F11**: Testy (validation)
7. **G12**: Dokumentace (maintenance)

---

## Notes

- Použít existující `core-platform.local` certs, neměnit compose
- Malé commity do `main`, žádné nové větve
- Dashboard UIDs budou vytvořeny během D8 provisioningu
- Pokud Mimir/Loki není k dispozici, poznamenat do README jako TODO

