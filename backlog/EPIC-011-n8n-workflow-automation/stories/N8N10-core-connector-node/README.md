---
id: N8N10
epic: EPIC-011-n8n-workflow-automation
title: "Core API Connector Node"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "1 day"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - docs/MODULE_REGISTRY.md
---

# N8N10: Core API Connector Node

Status: TODO
Priority: Medium
Dependencies: N8N8, N8N9

## Story
As a workflow designer, I want a safe Core API connector node in n8n so that tenant-scoped API calls are enforced without manual header handling.

## Acceptance Criteria
- The node injects X-Core-Tenant based on the n8n account/tenant mapping.
- Only whitelisted Core API routes are allowed by the node configuration.
- Credentials are stored in Vault and never embedded in workflow JSON.
- Node execution logs include tenant and request correlation IDs.

## Implementation Notes
- Implement as a custom n8n node package (TypeScript).
- Provide a strict URL allowlist (Core API base URL only).
- Add a minimal UI to pick Core resources and operations.

## Implementation Mapping
code_paths:
  - services/n8n-node-core-connector/package.json
  - services/n8n-node-core-connector/src/CoreConnector.node.ts
  - services/n8n-node-core-connector/src/credentials/CoreApi.credentials.ts

test_paths:
  - services/n8n-node-core-connector/test/core-connector.spec.ts

docs_paths:
  - docs/n8n/core-connector-node.md
