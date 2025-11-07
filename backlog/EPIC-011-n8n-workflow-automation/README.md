# EPIC-011: n8n Workflow Automation Platform

> **Enterprise Integration:** n8n community edition with Authelia SSO, Keycloak OIDC, 2FA, granular ACL

---

## 🎯 Epic Goal

Integrate n8n workflow automation platform into core-platform with **enterprise-grade security** via Authelia authentication gateway, enabling SSO through Keycloak OIDC while maintaining public webhook access for integrations.

## 🏗️ Architecture

```
┌─────────────┐
│    User     │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────┐
│   Nginx Reverse Proxy (443)     │
│   - SSL Termination              │
│   - Forward Auth to Authelia     │
│   - Webhook Bypass (no auth)     │
└────────┬───────────────────┬────┘
         │                   │
         │                   ▼
         │         ┌──────────────────┐
         │         │  Authelia (9091) │
         │         │  - OIDC Client   │
         │         │  - 2FA (TOTP)    │
         │         │  - ACL Rules     │
         │         │  - Session Mgmt  │
         │         └────────┬─────────┘
         │                  │
         │                  ▼
         │         ┌──────────────────┐
         │         │  Keycloak OIDC   │
         │         │  - Identity      │
         │         │  - Groups/Roles  │
         │         └──────────────────┘
         │
         ▼
┌─────────────────────┐
│   n8n (5678)        │
│   - Community Ed.    │
│   - Internal Only    │
│   - No Direct Access │
└─────────────────────┘
```

## 📊 Component Overview

| Component | Purpose | Port | Public | Tech Stack |
|-----------|---------|------|--------|------------|
| **Nginx** | Reverse proxy, SSL, auth gateway | 443 | ✅ Yes | Nginx 1.25+ |
| **Authelia** | Auth gateway, 2FA, ACL | 9091 | 🔒 Via Nginx | Go, Redis |
| **Authelia Redis** | Session storage | 6379 | ❌ No | Redis 7.0 |
| **n8n** | Workflow automation | 5678 | ❌ No | Node.js, PostgreSQL |
| **Keycloak** | Identity provider (existing) | 8443 | ✅ Yes | Java, PostgreSQL |

## 🎯 Success Metrics

- **Security**: 100% n8n UI access via SSO (0 bypass except webhooks)
- **Availability**: 99.9% uptime (Authelia + n8n combined)
- **Performance**: <500ms auth overhead, <2s n8n page load
- **Adoption**: 80% users enable 2FA within 30 days
- **Integration**: 50+ webhook integrations (public, no auth)

## 📋 Stories

### S1: Authelia Authentication Gateway (~800 LOC)
- Docker Compose setup (Authelia + Redis)
- Secret generation scripts
- OIDC client configuration
- Session management (Redis backend)
- 2FA support (TOTP, WebAuthn)
- **Metrics**: <200ms auth latency, 99.9% availability

### S2: Keycloak OIDC Integration (~500 LOC)
- n8n OIDC client creation in Keycloak
- Client credentials configuration
- Redirect URI setup
- User/group mappers
- Token validation
- **Metrics**: 100% SSO success rate, <3s login flow

### S3: Nginx Forward Auth Configuration (~600 LOC)
- Forward auth to Authelia
- Webhook bypass rules (no auth)
- SSL termination
- Rate limiting (10 req/sec per IP)
- Access logs
- **Metrics**: <100ms proxy overhead, 1M+ req/day

### S4: Access Control & ACL Rules (~700 LOC)
- Granular path-based ACL
- Group-based policies (n8n-admins, n8n-users, n8n-viewers)
- 2FA enforcement for admin workflows
- 1FA for regular users
- Bypass for health checks + webhooks
- **Metrics**: 0 unauthorized access, 90% 2FA adoption

### S5: n8n Platform Integration (~600 LOC)
- n8n Docker deployment (PostgreSQL backend)
- Webhook endpoint configuration
- Execution history retention (30 days)
- Workflow templates
- Monitoring integration (Prometheus metrics)
- **Metrics**: 500+ workflows, 10k+ executions/day

## 🔐 Security Features

- **Zero Trust**: Default deny policy, explicit allow rules
- **SSO**: Keycloak OIDC integration (existing users)
- **2FA**: TOTP (Google Authenticator) + WebAuthn (YubiKey)
- **Session Management**: Redis-backed, 1h expiration, 5min inactivity timeout
- **Secrets**: Docker secrets, PBKDF2 hashing, RSA-2048 keys
- **Network Isolation**: n8n internal-only, no direct public access

## 🚀 Implementation Plan

### Phase 1: Foundation (Week 1)
- ✅ S1: Deploy Authelia + Redis
- ✅ S2: Configure Keycloak OIDC client
- ✅ Generate secrets, hash client credentials

### Phase 2: Integration (Week 2)
- ✅ S3: Nginx forward auth configuration
- ✅ S4: ACL rules + group policies
- ✅ Testing: Login flow, 2FA, webhooks

### Phase 3: n8n Deployment (Week 3)
- ✅ S5: n8n Docker setup + PostgreSQL
- ✅ Workflow templates
- ✅ Monitoring + alerting

### Phase 4: Production (Week 4)
- ✅ SSL certificates (Let's Encrypt)
- ✅ Backup strategy (Authelia DB + n8n DB)
- ✅ Documentation + runbook
- ✅ User training (2FA setup)

## 📚 Documentation

- **AUTHELIA_INTEGRATION_GUIDE.md**: Complete setup guide
- **N8N_WORKFLOW_TEMPLATES.md**: Starter workflows
- **SECURITY_RUNBOOK.md**: Incident response procedures
- **USER_GUIDE.md**: End-user 2FA setup

## 🎓 Dependencies

- **External**: Keycloak (existing EPIC-003 Monitoring)
- **Infrastructure**: Nginx, Docker, PostgreSQL, Redis
- **Skills**: OIDC/OAuth2, Docker Compose, Nginx config, n8n workflows

## 🏁 Definition of Done

- [ ] All 5 stories implemented with acceptance criteria met
- [ ] Authelia + n8n + Redis running in Docker Compose
- [ ] 100% UI access requires SSO authentication
- [ ] Webhooks publicly accessible (no auth)
- [ ] 2FA enabled for admin users
- [ ] Keycloak OIDC integration tested
- [ ] ACL rules enforced (groups: admins, users, viewers)
- [ ] Monitoring dashboards (Authelia + n8n metrics)
- [ ] Security audit passed (0 public n8n exposure)
- [ ] Documentation complete (setup guide + runbook)

---

**Epic Owner**: Platform Team  
**Priority**: High  
**Target**: Q1 2026  
**Estimated Effort**: 3 weeks (1 engineer)
