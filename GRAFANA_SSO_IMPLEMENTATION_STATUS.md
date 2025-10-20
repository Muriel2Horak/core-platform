# 🔐 Grafana SSO Implementation - Complete Summary

## ✅ Co jsme implementovali

### 1. **Backend (JWT Authentication)**
- ✅ `AuthRequestController` - endpoint `/internal/auth/grafana`
- ✅ Čte `at` cookie z HTTP-only cookie
- ✅ Validuje JWT přes `JwtDecoder`
- ✅ Vrací hlavičku `Grafana-Jwt` s Keycloak JWT tokenem
- ✅ Rate limiting (20 req/min)

### 2. **Nginx (Proxy + Auth Request)**
- ✅ Location `/_auth/grafana` - internal auth bridge
- ✅ Location `/core-admin/monitoring/` - Grafana proxy
- ✅ `auth_request /_auth/grafana` - validace před každým requestem
- ✅ `proxy_set_header X-Org-JWT $grafana_token` - předání JWT
- ✅ `proxy_set_header Cookie $http_cookie` - předání všech cookies
- ✅ Žádný `rewrite` - Grafana slouží z subpath
- ✅ CSP: `frame-ancestors 'self' https://*.core-platform.local`

### 3. **Grafana (JWT Auth + SSL)**
- ✅ Custom Dockerfile s Keycloak SSL certifikátem
- ✅ `update-ca-certificates` - trust Keycloak self-signed cert
- ✅ `GF_AUTH_JWT_ENABLED=true`
- ✅ `GF_AUTH_JWT_HEADER_NAME=X-Org-JWT`
- ✅ `GF_AUTH_JWT_JWK_SET_URL=https://keycloak:8443/.../certs` (HTTPS!)
- ✅ `GF_AUTH_JWT_USERNAME_CLAIM=preferred_username`
- ✅ `GF_SERVER_SERVE_FROM_SUB_PATH=true`
- ✅ `GF_SERVER_ROOT_URL=https://%(domain)s/core-admin/monitoring`
- ✅ `GF_AUTH_JWT_AUTO_SIGN_UP=true`

### 4. **Frontend (iframe embed)**
- ✅ `GrafanaEmbed.tsx` komponenta
- ✅ Relativní URL: `/core-admin/monitoring/d/...`
- ✅ Sandbox: `allow-scripts allow-same-origin allow-forms`
- ✅ `referrerPolicy="no-referrer"`
- ✅ Loading spinner

## 🔍 Aktuální stav

### ✅ Co funguje:
1. **JWT Authentication** - Backend vrací JWT token ✅
2. **Nginx auth_request** - Volá backend správně ✅
3. **Cookie forwarding** - Všechny cookies se předávají ✅
4. **SSL Certificate** - Keycloak cert je trustován ✅
5. **User Authentication** - `userId=5 uname=test_admin` v logách ✅

### ❌ Co nefunguje:
1. **Dashboard 404** - Grafana vrací 404 pro `/d/infra-overview`
2. **Redirect na /login** - I když je uživatel autentizován

## 🐛 Debugging kroky

### Test 1: Direct Grafana Access
Otevři přímo v prohlížeči (ne v iframe):
```
https://admin.core-platform.local/core-admin/monitoring/
```

**Očekávaný výsledek:** Grafana home page
**Pokud nefunguje:** Problém v Grafana JWT auth konfiguraci

### Test 2: Dashboard API
```bash
docker exec core-grafana curl -H "X-Org-JWT: <actual-jwt-from-logs>" \
  'http://localhost:3000/core-admin/monitoring/api/dashboards/uid/infra-overview'
```

**Očekávaný výsledek:** JSON s dashboard metadaty
**Pokud nefunguje:** Dashboard není provisionován správně

### Test 3: Nginx Headers
```bash
docker logs core-nginx 2>&1 | grep "X-Org-JWT"
```

**Očekávaný výsledek:** Header je přítomen v requestech
**Pokud nefunguje:** `auth_request_set` nefunguje správně

## 📝 Možné příčiny 404:

1. **Dashboard UID mismatch**
   - Frontend: `/d/infra-overview`
   - Provision: `"uid": "infra-overview"`
   - **Status:** ✅ Shoduje se

2. **Grafana serve_from_sub_path**
   - Config: `serve_from_sub_path=true`
   - Root URL: `https://%(domain)s/core-admin/monitoring`
   - **Status:** ✅ Správně

3. **Nginx path forwarding**
   - No rewrite rule
   - Full path passed: `/core-admin/monitoring/d/...`
   - **Status:** ✅ Správně

4. **JWT not attached to iframe requests**
   - Browser may not send cookies to iframe
   - SameSite=None required
   - **Status:** ⚠️ Možná příčina!

## 🔧 Možná řešení:

### Řešení 1: Test bez iframe
Pokud funguje direct access, ale ne iframe:
- Problém: Browser Cookie policy
- Fix: Zkontrolovat `SameSite=None; Secure` na cookie

### Řešení 2: Anonymous access pro dashboards
Temporary workaround:
```ini
[auth.anonymous]
enabled = true
org_role = Viewer
```

### Řešení 3: Fallback na Grafana OAuth
Použít Keycloak OAuth místo JWT:
- User klikne "Sign in with Keycloak"
- OAuth flow
- Session cookie

## 📊 Logy

### Backend - Auth Success:
```
Grafana auth request successful for user: test_admin
```

### Grafana - User Authenticated:
```
userId=5 orgId=1 uname=test_admin
```

### Grafana - 404 Error:
```
path=/core-admin/monitoring/d/infra-overview status=404
```

## 🎯 Next Steps:

1. Test direct Grafana access (bez iframe)
2. Pokud funguje → iframe sandbox/cookie issue
3. Pokud nefunguje → Grafana JWT config issue
4. Check browser DevTools → Network tab → Request headers
5. Verify `X-Org-JWT` header is present in dashboard requests

---

**Created:** 2025-10-20
**Status:** 🔴 Debugging - Dashboard 404 issue
