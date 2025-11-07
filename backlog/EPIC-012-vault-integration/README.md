# EPIC-012: HashiCorp Vault Integration (Prod-Like Staging)

> **Enterprise Secrets Management:** HashiCorp Vault HA Raft, PKI for edge TLS, KV migration, audit to Loki, CI/CD OIDC

---

## 🎯 Epic Goal

Integrate HashiCorp Vault into core-platform as **production-like staging** secrets management, replacing plain-text `.env` files with secure KV storage, automated PKI certificate issuance for NGINX, comprehensive audit logging to Loki, and CI/CD OIDC authentication.

## 🏗️ Architecture

```text
┌─────────────────────────────────────────────────────────────────┐
│ PROD-LIKE STAGING VAULT ARCHITECTURE                            │
└─────────────────────────────────────────────────────────────────┘

                        ┌──────────────────┐
                        │   GitHub Actions │
                        │   (CI/CD)        │
                        └────────┬─────────┘
                                 │ OIDC JWT
                                 ▼
┌────────────────────────────────────────────────────────────────┐
│                    HashiCorp Vault (HA Raft)                   │
├────────────────────────────────────────────────────────────────┤
│  Mounts:                                                       │
│  - pki_root      (Root CA, offline emulation)                 │
│  - pki_int       (Intermediate CA, *.core-platform.local)     │
│  - kv/core       (KV v2: backend, keycloak, grafana secrets)  │
│  - auth/approle  (Runtime: backend, keycloak, nginx agents)   │
│  - auth/oidc     (CI/CD: GitHub Actions JWT trust)            │
│  - audit/file    (→ Promtail → Loki)                          │
└────────────┬───────────────────────┬─────────────┬────────────┘
             │                       │             │
             ▼                       ▼             ▼
    ┌────────────────┐    ┌────────────────┐   ┌──────────────┐
    │ Vault Agent    │    │ Spring Cloud   │   │ Vault Agent  │
    │ (NGINX)        │    │ Vault          │   │ (Keycloak)   │
    │                │    │ (Backend)      │   │              │
    │ Templating:    │    │                │   │ Templating:  │
    │ - edge.crt.tpl │    │ KV v2 read:    │   │ - env vars   │
    │ - edge.key.tpl │    │ kv/core/       │   │              │
    │ → NGINX reload │    │   backend/*    │   │              │
    └────────────────┘    └────────────────┘   └──────────────┘
             │                       │                  │
             ▼                       ▼                  ▼
    ┌────────────────────────────────────────────────────────────┐
    │                    Docker Compose Services                 │
    │  - nginx (HTTPS with Vault-issued certs)                   │
    │  - backend (Spring Boot, secrets from Vault KV)            │
    │  - keycloak (DB password from Vault)                       │
    │  - grafana (admin password from Vault)                     │
    │  - vault (HA Raft, single node for local/staging)          │
    │  - promtail (scrapes Vault audit logs → Loki)              │
    └────────────────────────────────────────────────────────────┘
```

## 📊 Component Overview

| Component | Purpose | Port | Storage | Tech Stack |
|-----------|---------|------|---------|------------|
| **Vault** | Secrets management, PKI CA | 8200 | Raft (HA) | HashiCorp Vault 1.15+ |
| **Vault Agent (NGINX)** | PKI cert templating, auto-renewal | - | Shared volume | Vault Agent |
| **Vault Agent (Keycloak)** | Env templating for secrets | - | Shared volume | Vault Agent |
| **Spring Cloud Vault** | Backend KV v2 integration | - | - | Spring Boot 3.x |
| **Promtail** | Scrape Vault audit logs | - | - | Grafana Promtail |
| **Loki** | Centralized audit storage | 3100 | Filesystem | Grafana Loki |

## 🎯 Success Metrics

- **Security**: 0 plain-text secrets in `.env` (all in Vault KV)
- **PKI Automation**: NGINX certs auto-renewed every 24h (TTL configurable)
- **Audit Coverage**: 100% Vault operations logged to Loki
- **HA Readiness**: Raft storage configured (single node for local, 3+ for prod)
- **CI/CD**: GitHub Actions authenticate via OIDC (no static tokens)
- **DR**: Snapshot & restore tested, runbook documented

## 📋 Stories

### S-P0: Vault Skeleton (~600 LOC)

**Goal**: Deploy Vault HA Raft, enable audit device, create init/unseal automation

**Deliverables**:

- Vault Docker Compose service (HA Raft, single node for local/staging)
- Audit device (file → Promtail → Loki)
- Init script (5 shares, 3 threshold)
- Unseal script (automated with keys file)
- Makefile targets: `vault-up`, `vault-init`, `vault-unseal`, `vault-status`, `vault-smoke`

**Acceptance Criteria**:

- ✅ `make vault-smoke` passes (health check, audit log, Loki integration, metrics)
- ✅ Audit events visible in Grafana within 10s

**Effort**: ~6 hours | **Details**: [S-P0.md](./stories/S-P0.md)

---

### S-P1: PKI for Edge TLS (~800 LOC)

**Goal**: Vault PKI issues *.core-platform.local certs, Vault Agent templates to NGINX

**Deliverables**:

- PKI root CA (10y, offline emulation)
- PKI intermediate CA (1y, online)
- PKI role: `edge-tls` (*.core-platform.local, 24h TTL)
- Vault Agent config (NGINX sidecar)
- Templates: edge.crt.tpl, edge.key.tpl, edge.ca-bundle.tpl
- AppRole for NGINX
- NGINX SSL config update

**Acceptance Criteria**:

- ✅ `curl https://admin.core-platform.local` shows Vault-signed cert
- ✅ Certificate auto-renews before 24h expiry
- ✅ NGINX reloads without connection drops

**Effort**: ~8 hours | **Details**: [S-P1.md](./stories/S-P1.md)

---

### S-P2: Secrets Migration (~1,000 LOC)

**Goal**: Migrate .env secrets to Vault KV v2, integrate Spring Cloud Vault

**Deliverables**:

- KV v2 mount: `kv/core` (backend/, keycloak/, grafana/)
- Secrets migration: DB passwords, SMTP, OAuth client secrets, admin passwords
- Policies: nginx-policy, backend-policy, keycloak-policy, grafana-policy
- Spring Cloud Vault (Backend integration)
- Vault Agent templating (Keycloak, Grafana env files)
- .env cleanup (.gitignore update)
- Makefile targets: `vault-push-secrets`, `vault-list-secrets`

**Acceptance Criteria**:

- ✅ Backend/Keycloak/Grafana start without .env secrets
- ✅ `make vault-list-secrets` shows all migrated secrets
- ✅ 0 secrets in Git repository (.env in .gitignore)

**Effort**: ~12 hours | **Details**: [S-P2.md](./stories/S-P2.md)

---

### S-P3: Hardening + Rotation (~700 LOC)

**Goal**: Implement secret rotation, fail-safe mechanisms, audit queries

**Deliverables**:

- DB password rotation script (`make vault-rotate-backend-db-pass`)
- Backend fail-fast on missing secrets (Spring Boot)
- Metrics: `vault.fetch.failures` counter
- Loki audit queries (Grafana saved view "Vault Audit")
- Policies review (least privilege enforcement)
- Makefile targets: `vault-rotate-*`, `vault-smoke-runtime`

**Acceptance Criteria**:

- ✅ DB password rotation successful (Backend reconnects automatically)
- ✅ Metrics/audit evidence in Grafana
- ✅ All policies follow least privilege (no broad kv/* access)

**Effort**: ~8 hours | **Details**: [S-P3.md](./stories/S-P3.md)

---

### S-P4: CI/CD + DR (~900 LOC)

**Goal**: GitHub OIDC authentication, backup/restore, monitoring

**Deliverables**:

- GitHub Actions OIDC trust (JWT authentication)
- Backup/restore scripts (`make vault-snapshot`, `make vault-restore`)
- Break-glass procedure (time-limited root token)
- Prometheus metrics integration (Vault exporter)
- Alerting rules (seal/unseal, latency, cert expiry)
- VAULT_RUNBOOK.md (operations guide)

**Acceptance Criteria**:

- ✅ CI reads secret via `vault login -method=oidc`
- ✅ Snapshot restore completes <15min
- ✅ Alerts configured and tested (seal, latency >500ms, cert expiry <24h)

**Effort**: ~10 hours | **Details**: [S-P4.md](./stories/S-P4.md)

---

## 🔐 EPIC Security Model

- **Zero Trust**: No secrets in repository, .env in .gitignore
- **Audit**: File device → Promtail → Loki → Grafana saved view "Vault Audit"
- **Least Privilege**: Service-specific policies (nginx/backend/keycloak/grafana)
- **Rotation**: 30-90 day standard, automated via Make targets
- **Break-glass**: Time-limited root token with full audit trail
- **Multi-auth**: AppRole (runtime services), OIDC (CI/CD GitHub Actions)
- **PKI Automation**: Edge certs auto-renewed (24h TTL), no manual intervention

## 🚀 Phased Implementation

### Phase 1: Vault Foundation (Week 1)

- ✅ **S-P0**: Vault skeleton + audit
- **Targets**: `vault-up`, `vault-init`, `vault-unseal`, `vault-smoke`
- **DoD**: Vault unsealed, audit in Loki, health check passes

### Phase 2: Automated PKI (Week 2)

- ✅ **S-P1**: Edge TLS via Vault Agent
- **Targets**: `vault-pki-setup`, `vault-pki-issue`, `vault-pki-status`
- **DoD**: NGINX uses Vault certs, auto-renewal verified

### Phase 3: Secrets Centralization (Week 3)

- ✅ **S-P2**: .env migration + policies
- **Targets**: `vault-push-secrets`, `vault-list-secrets`
- **DoD**: Backend starts without .env, 0 secrets in Git

### Phase 4: Operational Hardening (Week 4)

- ✅ **S-P3**: Rotation + fail-safe
- **Targets**: `vault-rotate-backend-db-pass`, `vault-smoke-runtime`
- **DoD**: Rotation tested, metrics/audit evidence

### Phase 5: Production Readiness (Week 5)

- ✅ **S-P4**: CI OIDC + DR
- **Targets**: `vault-snapshot`, `vault-restore`
- **DoD**: CI authenticates, restore <15min, alerts ready

## 📚 Documentation Deliverables

- **VAULT_EPIC.md**: Living document (S-P0 to S-P4 chapters, updated per phase)
- **SECRETS_INVENTORY.md**: KV tree (kv/core/backend/*, keycloak/*, grafana/*, ownership)
- **VAULT_RUNBOOK.md**: Operations guide (rotation, audit queries, restore, break-glass)

## 🏁 EPIC Completion Criteria

- [ ] All 5 stories implemented with acceptance criteria met
- [ ] Vault HA Raft running, unsealed, healthy (`sys/health` 200 OK)
- [ ] Audit events visible in Loki (Grafana saved view "Vault Audit" created)
- [ ] PKI engine issues `*.core-platform.local` certs (24h TTL)
- [ ] NGINX certs auto-renewed via Vault Agent (no manual intervention)
- [ ] All .env secrets migrated to `kv/core/*` (0 secrets in repo)
- [ ] `.env` added to `.gitignore` (verified not in Git)
- [ ] Backend/Keycloak/Grafana start with Vault secrets (no .env dependency)
- [ ] Service policies enforced (least privilege, no broad `kv/*` access)
- [ ] Rotation tested (1 DB password rotated, Backend auto-reconnects)
- [ ] CI reads secrets via GitHub OIDC (`vault login -method=oidc` works)
- [ ] Backup/restore tested (successful recovery <15min)
- [ ] Break-glass procedure documented (VAULT_RUNBOOK.md)
- [ ] Monitoring alerts configured (seal/unseal, latency >500ms, cert expiry <24h)
- [ ] Documentation complete (VAULT_EPIC.md, SECRETS_INVENTORY.md, VAULT_RUNBOOK.md)

## 📝 Notes

- **HA Raft**: Single node for local/staging, 3-5 nodes for production
- **Secrets Engine**: KV v2 (versioned secrets, rollback capability)
- **PKI TTL Strategy**: 24h edge certs, 1y intermediate CA, 10y root CA
- **Auto-Renewal**: Vault Agent renews when >50% TTL elapsed
- **Audit Retention**: 90 days in Loki (configurable)
- **No External Dependencies**: Self-contained, no HashiCorp Cloud Platform (HCP) required

---

**Total Effort**: ~44 hours (6h + 8h + 12h + 8h + 10h)  
**Total LOC**: ~4,000 lines documentation  
**Dependencies**: Docker Compose, existing Loki/Promtail stack  
**Status**: 🔄 Documentation complete, awaiting implementation

**Last Updated**: 2025-10-27

### S-P0: Vault Skeleton (Staging, Prod-Like) (~600 LOC)

- Vault HA Raft deployment (single node for local/staging)
- Vault Agent Docker image (templating support)
- Audit device (file → Promtail → Loki)
- Make targets: `vault-up`, `vault-init`, `vault-status`, `vault-smoke`
- Health checks: `sys/health`, `sys/mounts`
- **Metrics**: Vault operational, audit events in Loki

### S-P1: PKI for Edge TLS (NGINX Certs from Vault) (~800 LOC)

- PKI mounts: `pki_root` (offline emulation), `pki_int` (intermediate CA)
- PKI role: `*.core-platform.local` (wildcard certs)
- Vault Agent templating: `edge.crt.tpl`, `edge.key.tpl`
- NGINX reload hook (inotify / exec)
- Short TTL (24h) for testing auto-renewal
- **Metrics**: NGINX serves Vault-issued certs, auto-renewal working

### S-P2: Secrets Migration (.env → Vault KV) + Policies (~1,000 LOC)

- KV v2 mount: `kv/core`
- Migrate secrets: DB passwords, SMTP, OAuth client secrets, Grafana admin
- Policies per service: `nginx-policy`, `backend-policy`, `keycloak-policy`, `grafana-policy`
- AppRole auth for runtime (role-id/secret-id via Docker secrets)
- Spring Cloud Vault integration (backend)
- Vault Agent templating (Keycloak, Grafana)
- **Metrics**: Backend/Keycloak/Grafana start without `.env` secrets

### S-P3: Hardening + Rotation + Smoketests (~700 LOC)

- Secret rotation demo: `make vault-rotate-backend-db-pass`
- Rotation runbook: 30-90 day cadence
- Fail-fast behavior (backend fails if critical secret missing)
- Metrics: `vault.fetch.failures` (Micrometer)
- Health endpoint warns if secret expires < 3 days
- Audit validation: Loki saved view "Vault Audit"
- Policies review: least privilege enforcement
- **Metrics**: Successful rotation + restart, audit evidence in Loki

### S-P4: CI/CD + DR + Runbooks (~900 LOC)

- CI OIDC: GitHub Actions → Vault JWT trust
- OIDC role/policy for build-time secrets (read-only)
- Backup & DR: `make vault-snapshot`, `make vault-restore`
- Test restore from snapshot (documented)
- Break-glass procedure (time-limited token, audited)
- Monitoring: Vault metrics export, alerts (seal/unseal, latency, cert expiry)
- Documentation: `VAULT_RUNBOOK.md`, `SECRETS_INVENTORY.md`
- **Metrics**: CI reads secrets via OIDC, successful DR restore, alerts operational

## 🔐 Security Features

- **Zero Plain-Text Secrets**: All secrets in Vault KV v2, encrypted at rest
- **Audit Logging**: Every Vault operation logged to Loki (tamper-proof)
- **Least Privilege**: Per-service policies (no broad `kv/*` access)
- **Automated PKI**: NGINX certs auto-renewed, no manual CSR/signing
- **AppRole Auth**: Role-id/secret-id rotation supported
- **OIDC for CI**: GitHub Actions JWT trust (no static tokens)
- **Break-Glass**: Documented emergency access (audited, time-limited)

## 🚀 Implementation Plan

### Phase 1: Foundation (Week 1)

- [ ] S-P0: Vault skeleton (HA Raft, audit device, Make targets)
- [ ] Smoketest: `make vault-up && make vault-init && make vault-smoke`
- [ ] Verify: Loki shows Vault audit events

### Phase 2: PKI Automation (Week 2)

- [ ] S-P1: PKI mounts (root + intermediate CA)
- [ ] Vault Agent templating for NGINX certs
- [ ] NGINX reload hook (inotify)
- [ ] Smoketest: `curl -vk https://admin.core-platform.local` shows Vault cert
- [ ] Verify: Auto-renewal (manually shorten TTL, observe reload)

### Phase 3: Secrets Migration (Week 3)

- [ ] S-P2: KV v2 mount `kv/core`
- [ ] Migrate secrets from `.env` to Vault
- [ ] Policies per service (nginx, backend, keycloak, grafana)
- [ ] Spring Cloud Vault integration (backend)
- [ ] Vault Agent templating (Keycloak, Grafana)
- [ ] Smoketest: Backend starts without `.env` secrets
- [ ] Verify: `make vault-list-secrets` shows migrated keys

### Phase 4: Hardening (Week 4)

- [ ] S-P3: Rotation demo + runbook
- [ ] Fail-fast + metrics (vault.fetch.failures)
- [ ] Health endpoint expiry warnings
- [ ] Loki saved view "Vault Audit"
- [ ] Policies review (least privilege)
- [ ] Smoketest: `make vault-rotate-backend-db-pass` + restart

### Phase 5: CI/CD + DR (Week 5)

- [ ] S-P4: CI OIDC setup (GitHub Actions JWT)
- [ ] Backup/restore scripts + testing
- [ ] Break-glass procedure documentation
- [ ] Monitoring: metrics export, alert rules
- [ ] Runbooks: `VAULT_RUNBOOK.md`, `SECRETS_INVENTORY.md`
- [ ] Smoketest: CI reads secret via OIDC, snapshot restore

## 📚 Documentation

- **docs/VAULT_EPIC.md**: Living document (S-P0...S-P4 steps, commands, diagrams)
- **docs/SECRETS_INVENTORY.md**: KV tree, key owners, rotation schedule
- **docs/VAULT_RUNBOOK.md**: Operations (rotation, audit, DR, break-glass)
- **docs/PKI_WORKFLOW.md**: Certificate issuance, renewal, troubleshooting

## 🎓 Dependencies

- **External**: HashiCorp Vault 1.15+, Loki (existing EPIC-003)
- **Infrastructure**: Docker, Promtail, NGINX, PostgreSQL
- **Skills**: Vault administration, PKI concepts, AppRole/OIDC auth, Spring Cloud Vault

## 🏁 Definition of Done

- [ ] All 5 stories implemented with acceptance criteria met
- [ ] Vault running in HA Raft mode (single node for local/staging)
- [ ] PKI: NGINX serves Vault-issued certs, auto-renewed every 24h
- [ ] KV: Backend/Keycloak/Grafana secrets migrated from `.env`
- [ ] Policies: Per-service least privilege (no broad `kv/*`)
- [ ] Audit: All Vault operations visible in Loki
- [ ] CI/CD: GitHub Actions authenticate via OIDC (no static tokens)
- [ ] DR: Snapshot/restore tested, documented in runbook
- [ ] Monitoring: Metrics exported, alerts configured (seal, latency, expiry)
- [ ] Documentation: VAULT_EPIC.md, SECRETS_INVENTORY.md, VAULT_RUNBOOK.md complete
- [ ] Security: `.env` in `.gitignore`, no plain-text secrets in repo
- [ ] Smoketests: All `make vault-*` targets pass

## ⚠️ Critical Rules

1. **No secrets in Git**: `.env`, `*.key.pem`, Vault tokens → `.gitignore`
2. **Incremental commits**: Small steps to `main`, no feature branches
3. **DoD per story**: Each story has smoketest, must pass before next story
4. **Prod-like staging**: HA Raft, audit, policies from day 1
5. **Dev simplicity**: AppRole + templating OK for local, prod uses OIDC

## 🔗 Related EPICs

- **EPIC-003**: Monitoring & Observability (Loki integration)
- **EPIC-007**: Platform Hardening (security baseline)
- **EPIC-011**: n8n Workflow Automation (secrets from Vault in future)

---

**Epic Owner**: Platform Team  
**Priority**: High  
**Target**: Q1 2026  
**Estimated Effort**: 5 weeks (1 engineer)
