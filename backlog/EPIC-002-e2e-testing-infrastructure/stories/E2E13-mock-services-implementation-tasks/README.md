---
id: S13
epic: EPIC-002-e2e-testing-infrastructure
title: "Mock Services - Implementation Tasks"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "12 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-002-e2e-testing-infrastructure/stories/E2E13-mock-services-implementation-tasks/README.md
    - backlog/EPIC-002-e2e-testing-infrastructure/README.md
---

# S13: Mock Services (Implementation)

**Status:** 🔵 TODO  
**Effort:** ~12h | **LOC:** ~800  
**Owner:** QA + Backend

---

## 📋 Story Description

Jako **tester**, chci **deterministicke mocky externich zavislosti**, abych **mel stabilni E2E a integration testy**.

---

## 🎯 Acceptance Criteria

1. **Mock stack**
   - WireMock/Testcontainers jako zaklad
   - Mock pro Keycloak, MinIO, n8n webhooks

2. **Test utilities**
   - Helpery pro setup/teardown mocku
   - Jednotne fixtures pro responses

3. **Determinismus**
   - Predem definovane response sety
   - Zadne flaky volani na realne systemy

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: WireMock Setup (Testcontainers)](subtasks/T1-wiremock-setup.md) | 2h | none |
| 2 | [T2: Keycloak Mock Server](subtasks/T2-keycloak-mock.md) | 3h | T1 |
| 3 | [T3: MinIO (S3) Mock](subtasks/T3-minio-mock.md) | 2h | T1 |
| 4 | [T4: n8n Webhook Mock](subtasks/T4-nn-webhook-mock.md) | 2h | T1 |
| 5 | [T5: External API Mocks](subtasks/T5-external-api-mocks.md) | 1h | T1 |
| 6 | [T6: Mock Utilities & Helpers](subtasks/T6-mock-utilities.md) | 2h | T1, T2, T3 |

---

## 🔗 Zavisnosti

- **E2E1:** Playwright setup  
- **E2E3:** Test data factories
