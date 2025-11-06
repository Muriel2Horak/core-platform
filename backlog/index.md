# Core Platform - Backlog

> **Git-Native Backlog Management** - User Stories pro GitHub Copilot

## 📊 Backlog Dashboard

### Epic Progress

| Epic | Stories | Progress | Status | Target |
|------|---------|----------|--------|--------|
| [EPIC-001](EPIC-001-backlog-system/README.md) | 5 | 0/5 (0%) | 🚀 In Progress | Nov 13, 2025 |

### Active Stories (In Progress)

| ID | Story | Epic | Progress | Assignee | Status |
|----|-------|------|----------|----------|--------|
| [CORE-001](EPIC-001-backlog-system/stories/CORE-001-markdown-structure/README.md) | Markdown Structure & Templates | EPIC-001 | 0/11 DoD | Dev Team | 🔄 In Progress |

### Upcoming Stories (Ready)

| ID | Story | Epic | Dependencies | Priority |
|----|-------|------|--------------|----------|
| CORE-002 | Implementation Path Mapping | EPIC-001 | CORE-001 | P1 |
| CORE-003 | Story Generator Script | EPIC-001 | CORE-001, CORE-002 | P1 |

### Backlog (To Do)

| ID | Story | Epic | Priority | Estimate |
|----|-------|------|----------|----------|
| CORE-004 | Git Commit Tracker | EPIC-001 | P2 | 2 days |
| CORE-005 | Story Validator & DoD Checker | EPIC-001 | P2 | 1 day |

## 🎯 Current Sprint (Nov 6-13, 2025)

**Sprint Goal:** Bootstrap backlog management system (MVP)

**Sprint Capacity:** 5 stories  
**Sprint Progress:** 0/5 (0%)  
**Days Remaining:** 7 days

### Sprint Burndown
```
Stories Remaining
5 |■■■■■
4 |■■■■
3 |■■■
2 |■■
1 |■
0 |____________________
  Wed Thu Fri Mon Tue Wed
```

## 📈 Metrics

### Velocity
- **Last Sprint:** N/A (first sprint)
- **This Sprint:** 0 stories completed
- **Average:** N/A

### Quality
- **DoD Compliance:** N/A (no completed stories)
- **Story Completeness:** N/A
- **Test Coverage:** N/A

## 🔍 Quick Search

### By Epic
- [EPIC-001: Backlog System](EPIC-001-backlog-system/README.md) - Git-native backlog management

### By Status
- **In Progress:** CORE-001
- **Ready:** CORE-002, CORE-003
- **Blocked:** None
- **Done:** None

### By Priority
- **P1 (Must Have):** CORE-001, CORE-002, CORE-003
- **P2 (Should Have):** CORE-004, CORE-005

## 📚 Documentation

### Getting Started
- [Backlog System Overview](README.md) - How it works
- [Template Usage Guide](templates/README.md) - How to write stories
- [Development Workflow](../docs/development/backlog-workflow.md) - Developer guide

### Templates
- [Story Template](templates/story.md) - For User Stories
- [Subtask Template](templates/subtask.md) - For implementation tasks
- [Epic Template](templates/epic.md) - For large initiatives

## 🚀 Quick Actions

```bash
# Create new story
make backlog-new STORY="Feature Name"

# Validate stories
make backlog-validate

# Generate report
make backlog-report

# Track commits
make backlog-track
```

## 📊 System Stats

- **Total Epics:** 1
- **Total Stories:** 5
- **Completed:** 0 (0%)
- **In Progress:** 1 (20%)
- **Ready:** 2 (40%)
- **Blocked:** 0 (0%)
- **Backlog:** 2 (40%)

---

**Last Updated:** 2025-11-06  
**Next Review:** 2025-11-13 (Sprint End)
