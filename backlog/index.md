# Core Platform - Backlog Dashboard

This dashboard is the canonical overview of backlog state. Status values are taken
from each epic README to avoid drift.

Last updated: 2026-01-15

## Epic Overview

| Epic | Status (from README) | Notes | Docs |
|------|----------------------|-------|------|
| EPIC-000 Security Platform Hardening | MASTER REFERENCE (continuous) | Security baseline and principles | EPIC-000-security-platform-hardening/README.md |
| EPIC-001 Backlog System | COMPLETE (Phase 1 delivered, Phase 2 deferred) | Git-native backlog system | EPIC-001-backlog-system/README.md |
| EPIC-002 E2E Testing Infrastructure | IN PROGRESS | Wave 1 done, later phases pending | EPIC-002-e2e-testing-infrastructure/README.md |
| EPIC-003 Monitoring & Observability | 70% COMPLETE | Core stack done, frontend dashboards pending | EPIC-003-monitoring-observability/README.md |
| EPIC-004 Reporting Analytics Infrastructure | 100% COMPLETE | Cube.js infrastructure | EPIC-004-reporting-analytics-infrastructure/README.md |
| EPIC-005 Metamodel Generator Studio | Phase 1-3 implemented, Phase 4+ planned | Metamodel generator and studio | EPIC-005-metamodel-generator-studio/README.md |
| EPIC-006 Workflow Engine | 70% COMPLETE | Phase 1 done, Phase 2 in progress | EPIC-006-workflow-engine/README.md |
| EPIC-007 Infrastructure Deployment | COMPLETE (OPEN FOR FOLLOW-UPS) | Deployment/runtime foundation | EPIC-007-infrastructure-deployment/README.md |
| EPIC-018 Platform Hardening | 100% COMPLETE | Security + runtime hardening | EPIC-018-platform-hardening/README.md |
| EPIC-008 Document Management System | 20% COMPLETE | MinIO backend + upload API in prod | EPIC-008-document-management-system/README.md |
| EPIC-010 Agile Work Management | PLANNED | First reference business module | EPIC-010-agile-work-management/README.md |
| EPIC-011 n8n Workflow Automation | 0% IMPLEMENTED | n8n external orchestration layer | EPIC-011-n8n-workflow-automation/README.md |
| EPIC-012 Vault Integration | IN PROGRESS (S-P0/S-P1 done) | Secrets management and PKI | EPIC-012-vault-integration/README.md |
| EPIC-013 Reporting Module | 100% COMPLETE | Reporting UI module | EPIC-013-reporting-module/README.md |
| EPIC-014 UX/UI Design System | 20% COMPLETE | MUI theme + component library | EPIC-014-ux-ui-design-system/README.md |
| EPIC-016 Advanced Data UX Framework | PLANNED | Data UX framework | EPIC-016-advanced-data-ux-framework/README.md |
| EPIC-015 AI Metamodel Collaboration | 30% IN PROGRESS | MCP server + Copilot integration | EPIC-015-ai-metamodel-collaboration/README.md |
| EPIC-017 Modular Architecture | PLANNED | Module system and licensing | EPIC-017-modular-architecture/README.md |
| EPIC-020 Secure SDLC Quality Gates | COMPLETE (OPEN FOR FOLLOW-UPS) | Secure SDLC gates | EPIC-020-secure-sdlc-quality-gates/README.md |
| EPIC-021 AI Assisted Delivery Engine | PLANNED | AI delivery engine | EPIC-021-ai-assisted-delivery-engine/README.md |

## Cleanup Notes

- EPIC-016 AI scope was renumbered to EPIC-015 to remove the duplicate EPIC number.
- EPIC-004 (analytics infrastructure) and EPIC-013 (reporting module) overlap by domain but cover different layers. Treat them as separate epics.
- EPIC-009 appears in legacy dashboards but has no directory; scope is currently covered by EPIC-015-ai-metamodel-collaboration.

## Update Rules

- Update the status in the epic README first, then refresh this dashboard.
- Keep this file short; detailed story specs live in each epic directory.
