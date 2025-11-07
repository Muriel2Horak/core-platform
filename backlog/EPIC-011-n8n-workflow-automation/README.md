# EPIC-011: n8n Workflow Automation Platform

> **Workflow Automation:** n8n Community Edition with Keycloak SSO integration and backend monitoring dashboard

---

## 🎯 Epic Goal

Integrate n8n workflow automation platform into core-platform with **Keycloak SSO authentication** for secure access, enabling users to create and manage workflows while monitoring their status through a centralized React dashboard via backend BFF API.

## 🏗️ Architecture

```text
┌─────────────────────────────────────────────────────────────┐
│                    User (Browser)                            │
└────────────┬────────────────────────────────┬────────────────┘
             │                                │
             │ Direct Access                  │ Dashboard Access
             │ (n8n UI)                       │ (React Frontend)
             ▼                                ▼
    ┌─────────────────┐              ┌──────────────────┐
    │  Nginx Proxy    │              │  React Frontend  │
    │  /n8n/*         │              │  WorkflowDashboard
    └────────┬────────┘              └────────┬─────────┘
             │                                │
             │ Keycloak                       │ GET /api/n8n/workflows
             │ SSO Login                      │ (JWT Bearer token)
             ▼                                ▼
    ┌─────────────────┐              ┌──────────────────┐
    │  Keycloak OIDC  │◄─────────────│  Backend BFF     │
    │  - JWT Auth     │  Validate    │  - JWT validation│
    │  - Roles:       │              │  - Role check    │
    │    n8n-users    │              │  - Cache (5min)  │
    │    n8n-admins   │              │  - Audit log     │
    └─────────────────┘              └────────┬─────────┘
                                              │
                                              │ n8n REST API
                                              ▼
                                     ┌──────────────────┐
                                     │  n8n Community   │
                                     │  - Workflows     │
                                     │  - Executions    │
                                     │  - PostgreSQL    │
                                     └──────────────────┘
```

## 📊 Component Overview

| Component | Purpose | Port | Public | Tech Stack |
|-----------|---------|------|--------|------------|
| **n8n** | Workflow automation engine | 5678 | 🔒 Via Nginx | Node.js, PostgreSQL |
| **Nginx** | Reverse proxy, /n8n/* routing | 443 | ✅ Yes | Nginx 1.25+ |
| **Backend BFF** | n8n API proxy, monitoring | 8080 | 🔒 Internal | Spring Boot, WebClient |
| **React Dashboard** | Workflow monitoring UI | - | ✅ Via Frontend | React, TypeScript |
| **Keycloak** | SSO identity provider (existing) | 8443 | ✅ Yes | Java, PostgreSQL |

## 🎯 Success Metrics

- **Security**: 100% n8n access requires Keycloak SSO login
- **Availability**: 99.9% uptime (n8n + backend BFF)
- **Performance**: <200ms BFF API latency (cached), <2s n8n UI load
- **Adoption**: 50+ workflows created within first month
- **Monitoring**: Real-time dashboard updates every 5 seconds

## 📋 Stories

### S1: n8n Platform Deployment (~400 LOC)

**Goal**: Deploy n8n Community Edition with PostgreSQL backend

**Deliverables**:
- Docker Compose service definition
- PostgreSQL database for n8n execution data
- Environment configuration (basic auth disabled, external auth expected)
- Webhook endpoint configuration
- Execution history retention (30 days)

**Acceptance Criteria**:
- ✅ n8n accessible at http://n8n:5678 (internal network)
- ✅ PostgreSQL stores workflow definitions and execution history
- ✅ Webhooks functional for external integrations

**Effort**: ~4 hours | **Details**: [S1.md](./stories/S1.md)

---

### S2: Keycloak SSO Integration (~300 LOC)

**Goal**: Configure Keycloak client for n8n SSO authentication

**Deliverables**:
- Keycloak client creation (n8n-client)
- Redirect URIs: https://admin.core-platform.local/n8n/*
- Client roles: n8n-users, n8n-admins
- User group mapping
- JWT token configuration

**Acceptance Criteria**:
- ✅ Users redirected to Keycloak login when accessing /n8n
- ✅ Successful login grants access to n8n UI
- ✅ Roles n8n-users and n8n-admins enforced

**Effort**: ~3 hours | **Details**: [S2.md](./stories/S2.md)

---

### S3: Nginx Reverse Proxy Configuration (~200 LOC)

**Goal**: Configure Nginx to proxy /n8n/* to n8n with SSO enforcement

**Deliverables**:
- Nginx location block for /n8n/*
- Proxy pass to http://n8n:5678
- Keycloak auth_request integration (via oauth2-proxy or native)
- Webhook bypass (public access for /n8n/webhook/*)
- SSL termination

**Acceptance Criteria**:
- ✅ https://admin.core-platform.local/n8n redirects to Keycloak
- ✅ Authenticated users access n8n UI
- ✅ Webhooks accessible without authentication

**Effort**: ~3 hours | **Details**: [S3.md](./stories/S3.md)

---

### S4: Workflow Templates & Documentation (~300 LOC)

**Goal**: Provide starter workflow templates and user documentation

**Deliverables**:
- Workflow templates (HTTP request, database query, email notification)
- User guide (creating workflows, managing executions)
- Best practices documentation
- Troubleshooting guide

**Acceptance Criteria**:
- ✅ 3+ starter workflow templates available
- ✅ User guide published (Markdown in docs/)
- ✅ Best practices documented

**Effort**: ~2 hours | **Details**: [S4.md](./stories/S4.md)

---

### S5: Monitoring & Alerting Integration (~300 LOC)

**Goal**: Integrate n8n metrics with Grafana monitoring

**Deliverables**:
- n8n Prometheus metrics exporter configuration
- Grafana dashboard (workflow executions, success/failure rate)
- Alerting rules (execution failures >10% in 5min)
- Loki log aggregation for n8n logs

**Acceptance Criteria**:
- ✅ n8n metrics visible in Grafana
- ✅ Dashboard shows workflow execution stats
- ✅ Alerts trigger on failure threshold

**Effort**: ~4 hours | **Details**: [S5.md](./stories/S5.md)

---

---

### S6: Backend n8n Integration (BFF Pattern) (~1,200 LOC)

**Goal**: Enable workflow monitoring in React frontend via backend BFF API

**Deliverables**:
- n8n REST API client (WebClient, timeout 10s, API key auth)
- Domain models (N8nWorkflow, N8nExecution, sanitized nodes)
- BFF proxy controller with role-based access (@PreAuthorize)
- DTOs (WorkflowSummaryDTO, ExecutionDTO, ExecutionDetailDTO)
- Cache configuration (Caffeine, 5 min TTL, n8n-workflows)
- Frontend React dashboard (WorkflowDashboard.tsx)
- TypeScript types (WorkflowSummary, Execution, ExecutionStatus)
- Real-time monitoring (5s polling, auto-refresh executions)
- Role-based UI (users: read-only, admins: activate/deactivate + n8n link)
- Integration tests (@SpringBootTest, MockMvc, WireMock)
- E2E tests (Playwright, workflow-dashboard.spec.ts)

**Acceptance Criteria**:
- ✅ GET /api/n8n/workflows returns workflow list (JWT authenticated)
- ✅ Frontend dashboard shows real-time workflow status
- ✅ Admin users can activate/deactivate workflows
- ✅ "Open in n8n" link visible only for admins
- ✅ Auto-refresh every 5 seconds
- ✅ Cache hit rate >80%

**Effort**: ~23 hours | **Details**: [S6.md](./stories/S6.md)

---

## 🔐 Security Features

- **SSO**: Keycloak OIDC integration (existing identity provider)
- **JWT Authentication**: All backend API calls require valid JWT token
- **Role-Based Access**: n8n-users (read-only), n8n-admins (full access)
- **Credential Sanitization**: BFF strips sensitive data from n8n API responses
- **Audit Logging**: All admin actions logged (activate/deactivate workflows)
- **Network Isolation**: n8n internal-only, accessible via Nginx proxy or BFF API
- **Webhook Security**: Public webhooks for integrations (no auth required)

## 🚀 Implementation Plan

### Phase 1: Foundation (Week 1)

- ✅ S1: Deploy n8n + PostgreSQL
- ✅ S2: Configure Keycloak SSO client
- ✅ S3: Nginx reverse proxy setup

**DoD**: n8n accessible via SSO at https://admin.core-platform.local/n8n

### Phase 2: Templates & Monitoring (Week 2)

- ✅ S4: Workflow templates + documentation
- ✅ S5: Grafana monitoring integration

**DoD**: Starter templates available, metrics in Grafana

### Phase 3: Backend Integration (Week 3)

- ✅ S6: Backend BFF API + React dashboard

**DoD**: Workflow monitoring dashboard operational, real-time updates

## 📚 Documentation

- **N8N_SETUP_GUIDE.md**: Installation and configuration
- **N8N_WORKFLOW_TEMPLATES.md**: Starter workflow examples
- **N8N_USER_GUIDE.md**: End-user workflow creation guide
- **N8N_API_DOCUMENTATION.md**: Backend BFF API reference

## 🎓 Dependencies

- **External**: Keycloak (existing EPIC-003 Monitoring & Observability)
- **Infrastructure**: Nginx, Docker, PostgreSQL
- **Backend**: Spring Boot, WebClient, Spring Security
- **Frontend**: React, TypeScript, Axios
- **Skills**: n8n workflow development, REST API integration, OAuth2/OIDC

## 🏁 Definition of Done

- [ ] All 6 stories implemented with acceptance criteria met
- [ ] n8n running in Docker Compose with PostgreSQL backend
- [ ] 100% UI access requires Keycloak SSO login
- [ ] Webhooks publicly accessible (no auth) at /n8n/webhook/*
- [ ] Keycloak client configured with n8n-users and n8n-admins roles
- [ ] Nginx proxy routes /n8n/* to n8n service
- [ ] Backend BFF API operational (GET /api/n8n/workflows, POST /api/n8n/workflows/:id/activate)
- [ ] Frontend dashboard showing workflow status (active, executions, success rate)
- [ ] Real-time execution monitoring (5s polling)
- [ ] Role-based access enforced (users: read-only, admins: activate/deactivate)
- [ ] Grafana dashboard displays n8n metrics (executions, success rate)
- [ ] Alerting configured (execution failures >10% threshold)
- [ ] Workflow templates available (3+ starter examples)
- [ ] Documentation complete (setup guide, user guide, API docs)
- [ ] E2E tests passing (n8n login, workflow monitoring dashboard)

---

**Epic Owner**: Platform Team  
**Priority**: Medium  
**Target**: Q1 2026  
**Estimated Effort**: ~40 hours (~1 week, 1 engineer)  
**Status**: 📝 Documentation complete, awaiting implementation

**Last Updated**: 2025-11-07
