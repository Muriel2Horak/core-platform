---
id: N8N3
epic: EPIC-011-n8n-workflow-automation
title: "Nginx Reverse Proxy Configuration"
priority: P1
status: ready
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "0.5 day"
path_mapping:
  code_paths:
    - docker/nginx/nginx-ssl.conf.template
    - docker/nginx/nginx.dev.conf
    - docker/docker-compose.yml
  test_paths: []
  docs_paths:
    - docs/keycloak-ssl-setup.md
---

# S3: Nginx Reverse Proxy Configuration

> **Routing:** Configure Nginx to proxy `/n8n/*` to OAuth2 Proxy with SSL termination

## 📋 Story

**As a** platform administrator  
**I want** Nginx configured to route n8n traffic securely  
**So that** users access n8n via HTTPS with proper authentication

## 🎯 Acceptance Criteria

**GIVEN** OAuth2 Proxy is running  
**WHEN** accessing `https://admin.core-platform.local/n8n`  
**THEN** traffic routes to OAuth2 Proxy → n8n  
**AND** webhooks at `/n8n/webhook/*` bypass authentication  
**AND** SSL termination works correctly  
**AND** static assets under `/n8nstatic/*` and `/n8nassets/*` load under the subpath  
**AND** `/n8n/rest/logout` is proxied without auth

## 🏗️ Implementation

### Nginx Configuration

**File:** `docker/nginx/nginx-ssl.conf.template`

```nginx
# n8n Reverse Proxy (via OAuth2 Proxy for SSO)
location /n8n/ {
    # Preserve original URL
    proxy_set_header X-Original-URI $request_uri;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-Host $host;
    proxy_set_header Host $host;
    
    # OAuth2 Proxy upstream
    proxy_pass http://oauth2-proxy-n8n:4180;
    
    # WebSocket support (for n8n editor)
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    
    # Timeouts (long-running workflows)
    proxy_read_timeout 300s;
    proxy_connect_timeout 75s;
    
    # Buffer settings
    proxy_buffering off;
    proxy_request_buffering off;
}

# n8n logout endpoint (no auth, for SLO)
location = /n8n/rest/logout {
    proxy_pass http://n8n-proxy:3000/rest/logout;
}

# Fix base-path.js for subpath hosting
location = /n8nstatic/base-path.js {
    return 200 "window.BASE_PATH = '/n8n/';\n";
}

# n8n static assets (served directly from n8n)
location ^~ /n8nstatic/ {
    rewrite ^/n8nstatic/(.*)$ /static/$1 break;
    proxy_pass http://n8n:5678;
}

location ^~ /n8nassets/ {
    rewrite ^/n8nassets/(.*)$ /assets/$1 break;
    proxy_pass http://n8n:5678;
}

location = /n8nfavicon.ico {
    proxy_pass http://n8n:5678/favicon.ico;
}

# n8n Webhooks (public access - no auth)
location /n8n/webhook/ {
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header Host $host;
    
    # Direct to n8n (bypass OAuth2 Proxy)
    proxy_pass http://n8n:5678/webhook/;
    
    # WebSocket support
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    
    # Timeouts
    proxy_read_timeout 300s;
}

# OAuth2 callback
location /oauth2/ {
    proxy_pass http://oauth2-proxy-n8n:4180;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

### Docker Compose Updates

**File:** `docker/docker-compose.yml`

```yaml
services:
  nginx:
    volumes:
      - ./docker/nginx/nginx-ssl.conf.template:/etc/nginx/templates/nginx-ssl.conf.template:ro
    environment:
      - DOMAIN=${DOMAIN}
    depends_on:
      - oauth2-proxy-n8n
```

## ✅ Testing

```bash
# 1. Rebuild Nginx
docker compose up -d nginx

# 2. Test HTTPS redirect
curl -I https://admin.core-platform.local/n8n
# Expected: 302 redirect to Keycloak

# 3. Test webhook (no auth)
curl -X POST https://admin.core-platform.local/n8n/webhook/test \
  -H "Content-Type: application/json" \
  -d '{"test": "data"}'
# Expected: 404 (webhook doesn't exist yet) - but NO 401!

# 4. Check SSL
openssl s_client -connect admin.core-platform.local:443 \
  -servername admin.core-platform.local < /dev/null
# Expected: Certificate verified

# 5. Browser test
# Open: https://admin.core-platform.local/n8n
# Expected: Redirect to Keycloak → n8n UI
```

## 🎯 Acceptance Checklist

- [x] Nginx config added to template
- [x] `/n8n/*` proxies to OAuth2 Proxy
- [x] `/n8n/webhook/*` bypasses auth
- [x] SSL termination works
- [x] WebSocket support enabled

---

**Effort**: ~2 hours  
**LOC**: ~200 lines
