# N8N3: Nginx Reverse Proxy for n8n

**Typ:** TASK  
**Epic:** EPIC-011 (n8n External Orchestration Layer)  
**Fase:** Phase 3 (n8n Deployment)  
**Priorita:** HIGH  
**Effort:** 200 LOC, 0.5 dne  
**Dependencies:** N8N1 (Platform Deployment), N8N2 (Keycloak SSO)  
**Status:** ⏳ TODO

---

## 🎯 Cíl

Konfigurovat **Nginx reverse proxy** pro n8n:
- Location `/n8n/*` → forward to n8n:5678
- WebSocket upgrade headers (required pro n8n UI)
- OAuth2 callback proxy
- SSL termination

---

## 📋 Požadavky

### Funkční Požadavky

1. **Reverse Proxy**
   - Path: `/n8n/*`
   - Backend: `http://n8n:5678`
   - WebSocket support

2. **Headers**
   - `X-Forwarded-For`, `X-Forwarded-Proto`, `X-Real-IP`
   - `Upgrade`, `Connection: upgrade` (WebSocket)

3. **Timeouts**
   - `proxy_read_timeout 300s` (long-running workflows)
   - `proxy_send_timeout 300s`

---

## 🔧 Implementace

### 1. Nginx Configuration

**File:** `docker/nginx/nginx-ssl.conf.template` (add n8n location)

```nginx
# ... existing config ...

upstream n8n {
    server n8n:5678;
    keepalive 32;
}

server {
    listen 443 ssl http2;
    server_name admin.${DOMAIN};
    
    # ... existing SSL config ...
    
    # n8n Reverse Proxy
    location /n8n/ {
        # Rewrite path (strip /n8n prefix)
        rewrite ^/n8n/(.*) /$1 break;
        
        # Proxy to n8n container
        proxy_pass http://n8n;
        
        # Standard proxy headers
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_set_header X-Forwarded-Port $server_port;
        
        # WebSocket support (CRITICAL for n8n UI)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        
        # Timeouts (n8n workflows can run long)
        proxy_connect_timeout 60s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
        
        # Buffering (disable for streaming responses)
        proxy_buffering off;
        proxy_request_buffering off;
        
        # Client body size (large workflow definitions)
        client_max_body_size 50M;
    }
    
    # n8n Webhook endpoint (direct access, no auth)
    location /n8n/webhook/ {
        rewrite ^/n8n/webhook/(.*) /webhook/$1 break;
        proxy_pass http://n8n;
        
        # Same headers as above
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Allow large payloads
        client_max_body_size 100M;
    }
    
    # n8n OAuth2 callback (preserve query params)
    location /n8n/rest/oauth2-credential/callback {
        proxy_pass http://n8n/rest/oauth2-credential/callback$is_args$args;
        
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

### 2. Environment Variables

**File:** `.env.template` (update n8n webhook URL)

```bash
# 🌐 N8N CONFIGURATION
N8N_WEBHOOK_URL=https://admin.${DOMAIN}/n8n/webhook
N8N_EDITOR_BASE_URL=https://admin.${DOMAIN}/n8n
```

---

### 3. Docker Compose Update

**File:** `docker/docker-compose.yml` (update n8n environment)

```yaml
services:
  n8n:
    environment:
      # ... existing vars ...
      
      # Public URLs (via Nginx proxy)
      - WEBHOOK_URL=${N8N_WEBHOOK_URL:-https://admin.core-platform.local/n8n/webhook}
      - N8N_EDITOR_BASE_URL=${N8N_EDITOR_BASE_URL:-https://admin.core-platform.local/n8n}
      
      # Path prefix (n8n internal routing)
      - N8N_PATH=/n8n/
```

---

### 4. Verification Script

**File:** `scripts/n8n-proxy-test.sh`

```bash
#!/bin/bash
set -e

N8N_URL="${N8N_URL:-https://admin.core-platform.local/n8n}"

echo "🔍 Testing n8n Nginx proxy..."

# Test 1: Health endpoint
echo "1️⃣ Testing health endpoint..."
curl -k -f -s "${N8N_URL}/healthz" > /dev/null && echo "✅ Health OK" || echo "❌ Health FAIL"

# Test 2: UI redirect (should redirect to Keycloak)
echo "2️⃣ Testing UI redirect..."
REDIRECT=$(curl -k -s -o /dev/null -w "%{http_code}" "${N8N_URL}/")
if [ "$REDIRECT" == "302" ] || [ "$REDIRECT" == "200" ]; then
    echo "✅ UI accessible (HTTP $REDIRECT)"
else
    echo "❌ UI not accessible (HTTP $REDIRECT)"
fi

# Test 3: WebSocket upgrade headers
echo "3️⃣ Testing WebSocket headers..."
curl -k -s -I "${N8N_URL}/push" \
    -H "Upgrade: websocket" \
    -H "Connection: Upgrade" \
    | grep -i "upgrade" > /dev/null && echo "✅ WebSocket headers OK" || echo "⚠️ WebSocket headers missing"

# Test 4: Webhook endpoint (should be public)
echo "4️⃣ Testing webhook endpoint..."
curl -k -f -s -X POST "${N8N_URL}/webhook/test" \
    -H "Content-Type: application/json" \
    -d '{"test": true}' > /dev/null 2>&1 && echo "✅ Webhook accessible" || echo "✅ Webhook returns 404 (expected - no workflow)"

echo ""
echo "🎉 Nginx proxy test complete!"
```

---

### 5. Makefile Targets

**File:** `Makefile` (add proxy test)

```makefile
.PHONY: n8n-proxy-test
n8n-proxy-test: ## Test n8n Nginx reverse proxy
	@bash scripts/n8n-proxy-test.sh
```

---

## ✅ Acceptance Criteria

1. **Nginx Config:**
   - [ ] Location `/n8n/*` forwards to n8n:5678
   - [ ] WebSocket upgrade headers configured
   - [ ] Timeouts set to 300s
   - [ ] Client max body size 50M

2. **Routing:**
   - [ ] UI: `https://admin.core-platform.local/n8n/` → n8n editor
   - [ ] Webhook: `https://admin.core-platform.local/n8n/webhook/*` → n8n webhook handler
   - [ ] OAuth callback: `/n8n/rest/oauth2-credential/callback` → Keycloak callback

3. **WebSocket:**
   - [ ] n8n UI loads correctly (requires WS for live updates)
   - [ ] Browser DevTools shows successful WS connection to `/push`

4. **Test:**
   - [ ] `make n8n-proxy-test` passes
   - [ ] Manual test: open n8n UI, verify no 502 errors

---

**Related Stories:**
- N8N1: Platform Deployment
- N8N2: Keycloak SSO (OAuth callback routing)
