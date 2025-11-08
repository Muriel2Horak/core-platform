# INF-005: Let's Encrypt SSL Automation

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** HIGH  
**Effort:** 2 dny, ~600 LOC  
**Owner:** Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State:**
```bash
# Manual SSL cert generation:
bash docker/ssl/generate-ssl.sh
# → Self-signed certificate (90 days validity)
# → Manual renewal required
# → Browser warnings (not trusted CA)
```

**Git Evidence:**
- Multiple commits fixing SSL verification issues
- `fix(grafana): Skip TLS verification for Keycloak`
- Hardcoded cert paths in configs

### Goal

**Auto SSL with Let's Encrypt:**

```yaml
# Traefik as ACME client
traefik:
  certificatesResolvers:
    letsencrypt:
      acme:
        email: admin@core-platform.local
        storage: /letsencrypt/acme.json
        httpChallenge:
          entryPoint: web
```

**Benefits:**
- ✅ Trusted certificates (no browser warnings)
- ✅ Auto-renewal (80 days before expiry)
- ✅ Wildcard support (*.core-platform.local)
- ✅ Zero manual intervention

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Traefik Integration**
   - Replace Nginx with Traefik (reverse proxy + ACME)
   - HTTP-01 or DNS-01 challenge support
   - Auto-renewal cron job

2. ✅ **Certificate Storage**
   - Volume: `/letsencrypt/acme.json`
   - Permissions: 600 (secure)
   - Backup procedure

3. ✅ **Monitoring**
   - Prometheus metric: `ssl_certificate_expiry_days`
   - Alert: Cert expires <30 days
   - Grafana dashboard

### Implementation

**File:** `docker-compose.yml` (Traefik service)

```yaml
services:
  traefik:
    image: traefik:v3.0
    command:
      - --api.insecure=true
      - --providers.docker=true
      - --entrypoints.web.address=:80
      - --entrypoints.websecure.address=:443
      - --certificatesresolvers.letsencrypt.acme.email=admin@${DOMAIN}
      - --certificatesresolvers.letsencrypt.acme.storage=/letsencrypt/acme.json
      - --certificatesresolvers.letsencrypt.acme.httpchallenge.entrypoint=web
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - letsencrypt:/letsencrypt
    labels:
      - traefik.http.routers.backend.rule=Host(`admin.${DOMAIN}`)
      - traefik.http.routers.backend.tls=true
      - traefik.http.routers.backend.tls.certresolver=letsencrypt

volumes:
  letsencrypt:
```

**Renewal Check:**

```bash
#!/bin/bash
# scripts/ssl/check-expiry.sh
CERT_FILE=/letsencrypt/acme.json
EXPIRY=$(jq -r '.letsencrypt.Certificates[0].certificate' $CERT_FILE | \
         openssl x509 -noout -enddate | cut -d= -f2)
DAYS_LEFT=$(( ($(date -d "$EXPIRY" +%s) - $(date +%s)) / 86400 ))

if [ $DAYS_LEFT -lt 30 ]; then
    echo "⚠️  Certificate expires in $DAYS_LEFT days!"
    # Trigger renewal
    docker restart traefik
fi
```

**Effort:** 2 dny  
**LOC:** ~600  
**Blocks:** Production deployment

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
