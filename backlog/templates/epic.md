---
id: EPIC-XXX
title: "Epic Title (Short)"
status: planning  # planning | in-progress | on-hold | done
owner: ""
team: ""
created: YYYY-MM-DD
updated: YYYY-MM-DD
start_date: YYYY-MM-DD
target_date: YYYY-MM-DD
---

# EPIC-XXX: [Epic Title]

> **Status:** [planning/in-progress/on-hold/done]  
> **Owner:** [Team/Person]  
> **Timeline:** [Start Date] → [Target Date]

## 🎯 Epic Goal

[1-2 paragrafy popisující vysokoúrovňový cíl tohoto epicu]

**Co chceme dosáhnout:**
[Konkrétní outcome/deliverable - co bude na konci epicu existovat?]

**Proč to děláme:**
[Business reason, user pain point, strategic direction]

**Hodnota pro business/uživatele:**
[Měřitelný benefit - revenue, cost savings, user satisfaction, atd.]

---

## 📊 Scope

### In Scope (Co BUDEME dělat)

- ✅ [Feature/capability #1]
- ✅ [Feature/capability #2]
- ✅ [Feature/capability #3]
- ✅ [Platform/component affected]
- ✅ [User personas affected]

### Out of Scope (Co NEBUDEME dělat v tomto epicu)

- ❌ [Feature explicitně vynechaná - důvod]
- ❌ [Future enhancement - will be separate epic]
- ❌ [Technical debt - will address later]
- ❌ [Platform NOT affected]

**Důvody pro out-of-scope:**
[Vysvětlení proč některé věci nejsou v tomto epicu - time constraints, dependencies, priorities]

---

## 📋 User Stories

> **Celkový počet:** X stories (Y priority 1, Z priority 2)

### Priority 1: Must Have (MVP)

| ID | Story | Estimate | Status | Assignee |
|----|-------|----------|--------|----------|
| [CORE-XXX](stories/CORE-XXX-name/README.md) | [Story Title] | X days | Ready | [Name] |
| [CORE-YYY](stories/CORE-YYY-name/README.md) | [Story Title] | X days | In Progress | [Name] |
| [CORE-ZZZ](stories/CORE-ZZZ-name/README.md) | [Story Title] | X days | Done ✅ | [Name] |

**Total P1:** Y stories, Z days estimated

### Priority 2: Should Have

| ID | Story | Estimate | Status | Assignee |
|----|-------|----------|--------|----------|
| [CORE-AAA](stories/CORE-AAA-name/README.md) | [Story Title] | X days | Backlog | [Name] |
| [CORE-BBB](stories/CORE-BBB-name/README.md) | [Story Title] | X days | Backlog | [Name] |

**Total P2:** Y stories, Z days estimated

### Priority 3: Nice to Have (Future)

| ID | Story | Estimate | Status | Notes |
|----|-------|----------|--------|-------|
| CORE-CCC | [Story Title] | X days | Backlog | May move to EPIC-YYY |
| CORE-DDD | [Story Title] | X days | Backlog | Depends on external API |

**Total P3:** Y stories, Z days estimated

---

## 📅 Timeline & Milestones

```
Timeline: [Start Date] ────────────────────► [Target Date]
          
Phase 1: Foundation       Phase 2: Implementation    Phase 3: Polish
[Week 1-2]                [Week 3-5]                 [Week 6-7]
│                         │                          │
├─ CORE-XXX (Setup)       ├─ CORE-YYY (Feature A)    ├─ CORE-AAA (Testing)
├─ CORE-ZZZ (Design)      ├─ CORE-BBB (Feature B)    ├─ CORE-CCC (Docs)
└─ Milestone 1 ✓          ├─ CORE-DDD (Feature C)    └─ EPIC COMPLETE ✓
                          └─ Milestone 2 ✓
```

### Phase 1: Foundation (X days)
**Goal:** Setup infrastructure, design, technical spike

**Stories:**
- CORE-XXX: [Story name]
- CORE-YYY: [Story name]

**Deliverables:**
- [ ] Architecture design document
- [ ] Database schema
- [ ] API contracts defined
- [ ] Development environment setup

**Milestone 1 Success Criteria:**
- ✅ All P1 stories in "Ready" state
- ✅ Technical spike completed
- ✅ Team aligned on approach

---

### Phase 2: Implementation (X days)
**Goal:** Core features implemented and tested

**Stories:**
- CORE-AAA: [Story name]
- CORE-BBB: [Story name]
- CORE-CCC: [Story name]

**Deliverables:**
- [ ] Backend APIs implemented
- [ ] Frontend UI components built
- [ ] Integration tests passing
- [ ] Documentation started

**Milestone 2 Success Criteria:**
- ✅ All P1 stories "Done"
- ✅ Core functionality working end-to-end
- ✅ Unit test coverage >80%

---

### Phase 3: Polish & Release (X days)
**Goal:** Testing, documentation, deployment

**Stories:**
- CORE-DDD: [Story name]
- CORE-EEE: [Story name]

**Deliverables:**
- [ ] E2E tests complete
- [ ] User documentation
- [ ] Deployment to staging
- [ ] Production rollout plan

**Epic Complete Success Criteria:**
- ✅ All P1 + P2 stories "Done"
- ✅ E2E tests passing
- ✅ Deployed to production
- ✅ Zero critical bugs

---

## ✅ Success Criteria

### Technical Success

- [ ] **Functionality:** All P1 stories delivered and working
- [ ] **Quality:** Code coverage >80%, zero critical bugs
- [ ] **Performance:** [Specific metric, např. "API response <500ms p95"]
- [ ] **Security:** Security review passed, no vulnerabilities
- [ ] **Documentation:** API docs, user guide, architecture docs complete

### Business Success

- [ ] **User Adoption:** [Metric, např. "50% of users use new feature within 1 month"]
- [ ] **User Satisfaction:** [Metric, např. "NPS >40 for feature"]
- [ ] **Business Metric:** [Metric, např. "20% reduction in support tickets"]
- [ ] **Revenue Impact:** [Metric, např. "$X ARR from new capability"]

### Team Success

- [ ] **Knowledge Sharing:** Team demo completed, docs shared
- [ ] **Process:** Backlog workflow used successfully (dogfooding)
- [ ] **Retrospective:** Lessons learned documented
- [ ] **Velocity:** Team velocity maintained or improved

---

## 🔗 Dependencies

### Internal Dependencies (Other Epics/Stories)

| Dependency | Type | Status | Impact |
|------------|------|--------|--------|
| [EPIC-YYY: Other Epic](../EPIC-YYY/README.md) | Blocks us | In Progress | Need API from EPIC-YYY before starting CORE-XXX |
| [CORE-AAA: Story](../EPIC-YYY/stories/CORE-AAA/README.md) | We block | Waiting | CORE-AAA can't start until our CORE-XXX done |

### External Dependencies

| Dependency | Owner | Status | Mitigation |
|------------|-------|--------|------------|
| External API v2 release | 3rd Party | Q2 2025 | Use v1 API for now, migrate later |
| Database migration | DevOps | Pending approval | Submit request ASAP, have rollback plan |
| Design system update | UX Team | In progress | Use current DS, update later |

---

## 🚀 Implementation Approach

### Architecture Overview

[High-level architektura - komponenty, data flow, integration points]

**Příklad:**
```
Frontend (React)
    ↓ REST API
Backend (Spring Boot)
    ↓ JDBC
Database (PostgreSQL)
    ↓ CDC Events
Kafka → Analytics
```

### Key Technical Decisions

**Decision 1: [Technology/Pattern Choice]**
- **What:** [Např. "Use GraphQL instead of REST"]
- **Why:** [Reasoning - performance, flexibility, team expertise]
- **Alternatives Considered:** [What we didn't choose and why]
- **Trade-offs:** [What we gain vs. what we lose]

**Decision 2: [Architecture Pattern]**
- **What:** [Např. "Event-driven architecture with Kafka"]
- **Why:** [Scalability, decoupling, real-time processing]
- **Impact:** [How it affects development, testing, operations]

### Development Strategy

**Parallel Work Streams:**
1. **Backend Team:** Stories CORE-XXX, CORE-YYY (Week 1-3)
2. **Frontend Team:** Stories CORE-ZZZ, CORE-AAA (Week 2-4)
3. **QA Team:** Test automation (Week 3-5)

**Integration Points:**
- Week 2: Backend API ready for frontend integration
- Week 4: E2E integration testing
- Week 6: UAT and final polish

---

## ⚠️ Risks & Mitigations

### Risk 1: [High Impact Risk]

**Description:** [Co může jít špatně]

**Probability:** High/Medium/Low  
**Impact:** High/Medium/Low  
**Risk Level:** 🔴 Critical / 🟡 Medium / 🟢 Low

**Mitigation:**
- [ ] [Preventive action #1]
- [ ] [Preventive action #2]
- [ ] [Contingency plan if risk materializes]

**Owner:** [Person responsible for monitoring]

---

### Risk 2: [Technical Risk]

**Description:** [Např. "External API may not be stable enough"]

**Probability:** Medium  
**Impact:** High  
**Risk Level:** 🟡 Medium

**Mitigation:**
- [ ] Build API abstraction layer (can swap implementation)
- [ ] Have fallback to v1 API
- [ ] Monitor API health with alerts
- [ ] Weekly sync with 3rd party team

**Owner:** Backend Lead

---

### Risk 3: [Resource Risk]

**Description:** [Např. "Key developer may be unavailable"]

**Probability:** Low  
**Impact:** High  
**Risk Level:** 🟡 Medium

**Mitigation:**
- [ ] Cross-train team members (pair programming)
- [ ] Document critical decisions in ADRs
- [ ] Have backup assignees for each story

**Owner:** Engineering Manager

---

## 📚 Lessons from Similar Projects

### What Worked Well (Keep Doing)

✅ [Practice #1 from previous project]
- **Example:** "Using feature flags allowed gradual rollout"
- **Apply to this epic:** Use feature flags for all new endpoints

✅ [Practice #2]
- **Example:** "Weekly demos kept stakeholders informed"
- **Apply to this epic:** Schedule weekly epic demos

### What Didn't Work (Avoid)

❌ [Anti-pattern #1 from previous project]
- **Example:** "Starting UI before API design caused rework"
- **Avoid in this epic:** API-first design, contracts before code

❌ [Anti-pattern #2]
- **Example:** "Skipping integration tests led to prod bugs"
- **Avoid in this epic:** Integration tests mandatory before merge

### New Things to Try

💡 [Innovation #1]
- **What:** "Use GitHub Copilot for test generation from stories"
- **Why:** Speed up testing, improve coverage
- **How to measure success:** Test creation time, coverage increase

💡 [Innovation #2]
- **What:** "Dogfood backlog system (manage this epic with itself)"
- **Why:** Validate workflow, find issues early
- **How to measure success:** Team adoption, story completeness

---

## 📊 Epic Metrics

### Progress Tracking

**Story Completion:**
```
Total Stories: X
Completed:     Y (Z%)
In Progress:   A
Blocked:       B
Backlog:       C

Progress: [████████░░░░░░░░░░░░] 40%
```

**Velocity:**
- **Sprint 1:** X stories completed
- **Sprint 2:** Y stories completed
- **Average:** Z stories/sprint
- **Forecast:** Epic complete in N sprints

### Quality Metrics

**Code Quality:**
- Test Coverage: XX% (target: >80%)
- Code Review: 100% of PRs reviewed
- Bug Count: X critical, Y major, Z minor

**Performance:**
- API Response Time: XXms p95 (target: <500ms)
- Frontend Load Time: XXms (target: <2s)
- Database Query Time: XXms (target: <100ms)

---

## ✅ Epic Definition of Done

This epic is COMPLETE when:

### Functionality
- [ ] All Priority 1 (Must Have) stories are "Done"
- [ ] At least 80% of Priority 2 (Should Have) stories are "Done"
- [ ] All Acceptance Criteria across all stories are met

### Quality
- [ ] Code coverage >80% for all new code
- [ ] Zero critical bugs, <5 major bugs
- [ ] Security review passed (no high/critical vulnerabilities)
- [ ] Performance benchmarks met (response times, load times)

### Documentation
- [ ] API documentation complete (Swagger + Markdown)
- [ ] User guide / feature documentation published
- [ ] Architecture Decision Records (ADRs) for major decisions
- [ ] Runbook for operations/support team

### Testing
- [ ] Unit tests passing (100%)
- [ ] Integration tests passing (100%)
- [ ] E2E tests covering all user flows
- [ ] Manual UAT completed by Product Owner

### Deployment
- [ ] Deployed to DEV environment
- [ ] Deployed to STAGING environment
- [ ] Deployed to PRODUCTION (or ready for release)
- [ ] Rollback plan documented and tested

### Team & Process
- [ ] Epic demo completed (all stakeholders)
- [ ] Retrospective held, lessons documented
- [ ] Knowledge sharing session with team
- [ ] Metrics/analytics instrumented for monitoring

### Business Validation
- [ ] Product Owner acceptance
- [ ] Success criteria met (see "Success Criteria" section)
- [ ] User feedback collected (if applicable)

---

## 🏷️ Tags

`epic` `[feature-area]` `backend` `frontend` `infrastructure` `[quarter]`

---

## 📎 Attachments & Resources

### Design Assets
- [Epic Overview Presentation](attachments/epic-overview.pdf)
- [Architecture Diagram](attachments/architecture.svg)
- [User Flow Diagram](attachments/user-flow.png)

### Documentation
- [Technical Design Document](attachments/tech-design.md)
- [API Specification](attachments/api-spec.yaml)
- [Database Schema](attachments/schema.sql)

### External Links
- [JIRA Epic](https://jira.company.com/browse/EPIC-XXX) (if applicable)
- [Slack Channel](https://company.slack.com/archives/epic-xxx)
- [Confluence Page](https://wiki.company.com/epic-xxx)

---

## 📝 Change Log

| Date | Change | Author |
|------|--------|--------|
| YYYY-MM-DD | Epic created | [Name] |
| YYYY-MM-DD | Added CORE-XXX story | [Name] |
| YYYY-MM-DD | Phase 1 complete | [Name] |
| YYYY-MM-DD | Epic closed | [Name] |

---

**Epic Version:** 1.0  
**Created:** YYYY-MM-DD  
**Last Updated:** YYYY-MM-DD  
**Owner:** [Team/Person Name]
