# T1: Implement Nginx Subdomain Routing

**Parent Story:** INF-020 Multi-Tenancy Architecture  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 4 hours  
**Owner:** DevOps

---

## 🎯 Objective

Configure Nginx to route requests based on subdomain and inject X-Tenant-ID header.

---

## 📋 Tasks

### 1. Update SSL Certificate (Wildcard)

```bash
# Generate wildcard cert for *.core-platform.local
bash docker/ssl/generate-ssl.sh

# Verify SAN includes wildcard
openssl x509 -in docker/ssl/server.crt.pem -text -noout | grep DNS
# Should show: DNS:*.core-platform.local, DNS:core-platform.local
```

### 2. Update nginx-ssl.conf

**File:** `docker/nginx/nginx-ssl.conf.template`

```nginx
# Tenant extraction from subdomain
map $http_host $tenant_subdomain {
    ~^(?<subdomain>[^.]+)\.${DOMAIN}$ $subdomain;
    default admin;  # Fallback to admin tenant
}

# Tenant ID lookup (can be from DB later)
map $tenant_subdomain $tenant_id {
    admin 1;
    tenant-a 2;
    tenant-b 3;
    default 1;  # Default tenant
}

server {
    listen 443 ssl;
    server_name *.${DOMAIN} ${DOMAIN};

    ssl_certificate /etc/nginx/ssl/server.crt.pem;
    ssl_certificate_key /etc/nginx/ssl/server.key.pem;

    # Inject tenant header to all backend requests
    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header X-Tenant-ID $tenant_id;
        proxy_set_header X-Tenant-Subdomain $tenant_subdomain;
        proxy_set_header Host $host;
    }

    # Frontend serves tenant-specific bundles
    location / {
        proxy_pass http://frontend:3000;
        proxy_set_header X-Tenant-ID $tenant_id;
        proxy_set_header Host $host;
    }
}
```

### 3. Add Tenant DNS Entries

**File:** `/etc/hosts` (dev only)

```
127.0.0.1 admin.core-platform.local
127.0.0.1 tenant-a.core-platform.local
127.0.0.1 tenant-b.core-platform.local
```

### 4. Test Routing

```bash
# Test admin subdomain
curl -k https://admin.core-platform.local/api/tenants/current
# Expected: {"id": 1, "name": "Admin Tenant"}

# Test tenant-a subdomain
curl -k https://tenant-a.core-platform.local/api/tenants/current
# Expected: {"id": 2, "name": "Tenant A"}

# Verify header injection
docker exec core-nginx cat /var/log/nginx/access.log | grep "X-Tenant-ID"
```

---

## ✅ Acceptance Criteria

- [ ] Wildcard SSL certificate generated
- [ ] Nginx extracts tenant from subdomain
- [ ] X-Tenant-ID header injected on all backend requests
- [ ] Different subdomains route to different tenants
- [ ] Logs show tenant context in access logs

---

## 🔗 Dependencies

- Requires wildcard DNS/hosts entries
- Blocks backend tenant filter (INF-007 T2)
