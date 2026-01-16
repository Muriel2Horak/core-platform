---
id: N8N12
epic: EPIC-011-n8n-workflow-automation
title: "Webhook Security and Signature Verification"
priority: P1
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "1.5 days"
path_mapping:
  code_paths:
    - backend/src/main/java/com/platform/workflow/executor/impl/WebhookNotificationExecutor.java
  test_paths: []
  docs_paths:
    - docs/SECURITY_RUNBOOK.md
    - docs/SECURITY_MIGRATION_GUIDE.md
---


# N8N12: Webhook Security and Signature Verification

Status: ✅ DONE
Priority: High
Estimate: 1.5 days (~350 LOC)
Dependencies: N8N8

## Story
As a platform administrator, I want n8n webhooks to be HMAC-signed and verified so that public webhook endpoints cannot be abused or spoofed.

## Acceptance Criteria
- Incoming /api/v1/webhooks/n8n/* requests verify X-Signature (sha256=...).
- Per-hook secrets are supported via env/json file mapping.
- Missing/invalid signatures return 401 and are logged with trace IDs.
- A reference n8n workflow template includes signature verification in a Function node.

## Implementation Notes (Java/Spring Boot)
- Add a webhook signature verifier utility.
- Load secrets from Vault (default + per-hook overrides).
- Ensure raw body is available for HMAC computation.
- Provide a test workflow export and docs for configuration.

## Implementation Mapping
code_paths:
  - backend/src/main/java/com/platform/n8n/webhooks/WebhookSignatureVerifier.java
  - backend/src/main/java/com/platform/n8n/webhooks/N8nWebhookController.java
  - backend/src/main/java/com/platform/n8n/webhooks/N8nWebhookSecrets.java

test_paths:
  - backend/src/test/java/com/platform/n8n/webhooks/WebhookSignatureVerifierTest.java
  - backend/src/test/java/com/platform/n8n/webhooks/N8nWebhookControllerTest.java

docs_paths:
  - docs/security/webhook-signature.md
  - docs/n8n/webhook-security.md
  - workflows/n8n/secure-webhook-guard.json

## References (isp-migration-tool)
- backend/app/services/webhook_signature.py
- backend/app/api/n8n_webhooks.py
- reports/security/US-025/webhook-tests/secure-webhook-guard.n8n-export.json
