---
id: S9
epic: EPIC-002-e2e-testing-infrastructure
title: "Test Tagging System - Implementation Tasks"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "6 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-002-e2e-testing-infrastructure/stories/E2E9-test-tagging-system-implementation-tasks/README.md
    - backlog/EPIC-002-e2e-testing-infrastructure/README.md
---


# S9: Test Tagging System (Implementation)

**Story:** Test ID Tagging System  
**Status:** 🔵 TODO  
**Effort:** ~6h

---

## 📋 Story Description

Jako **developer/QA**, chci **jednotne tagovani testu**, abych **mohl mapovat testy na user stories a sledovat coverage**.

---

## 🎯 Acceptance Criteria

1. **Tag format**
   - Jednotny format `@CORE-XXX` pro vsechny typy testu

2. **Framework support**
   - Playwright podporuje filter `--grep @CORE-XXX`
   - JUnit ma `@UserStory("CORE-XXX")`

3. **Validation**
   - Pre-commit hook hlida, ze testy maji story tag
   - Report ukazuje chybejici tagy

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | [T1: Tagging Convention (@CORE-XXX)](subtasks/T1-tagging-convention.md) | 1h | none |
| 2 | [T2: Playwright Tag Support](subtasks/T2-playwright-tags.md) | 2h | T1 |
| 3 | [T3: JUnit @UserStory Annotation](subtasks/T3-junit-annotation.md) | 2h | T1 |
| 4 | [T4: Pre-commit Tag Validation](subtasks/T4-pre-commit-hook.md) | 1h | T2, T3 |

---

## 🔄 Implementation Order

### Phase 1: Convention & Documentation (T1)
- Define tagging format (@CORE-XXX)
- Document conventions
- Examples for all test types

### Phase 2: Framework Integration (T2-T3)
- Playwright tag support (test.describe)
- JUnit @UserStory annotation
- Tag extraction utilities

### Phase 3: Automation (T4)
- Pre-commit hook (validate tags)
- Git hook installation
- Tag validation rules

---

## 🧪 Testing Strategy

### Tag Extraction Tests
- Parse @CORE-XXX from test titles
- Parse @CORE-XXX from test tags
- Handle missing tags gracefully

### Validation Tests
- Pre-commit hook rejects missing tags
- Pre-commit hook accepts valid tags
- Pre-commit hook shows helpful errors

---

## 📦 Deliverables

1. **Documentation**: `docs/testing-tagging-conventions.md`
2. **Playwright Config**: Tag support in `playwright.config.ts`
3. **JUnit Annotation**: `@UserStory("CORE-XXX")`
4. **Pre-commit Hook**: `.husky/pre-commit` tag validation
5. **Validation Script**: `scripts/validate-test-tags.sh`
6. **Examples**: Tagged tests in codebase

---

## ✅ Definition of Done

- [ ] Tagging convention documented
- [ ] Playwright tests use tags (@CORE-XXX)
- [ ] JUnit tests use @UserStory annotation
- [ ] Pre-commit hook validates tags
- [ ] Examples added to codebase
- [ ] Documentation updated

---

**Back to EPIC:** [EPIC-002](../../README.md)
