# Platform Hardening Epic - Documentation

This directory contains tracking documents for the **Platform Hardening Epic** (S1-S8).

## 📁 Structure

```
docs/epics/
├── README.md                          # This file
├── PLATFORM_HARDENING_EPIC.md         # Epic overview & tracking
├── S1_NAMING_TODO.md                  # S1 detailed task list
├── S1_SUMMARY.md                      # S1 completion summary
└── (future: S2_TODO.md, S2_SUMMARY.md, etc.)
```

---

## 🎯 Epic Overview

**Goal:** Deliver a unified, production-ready platform with:
- Unified naming conventions
- Real-time presence with Kafka "stale" mode
- Event-driven Cube pre-aggregations
- Comprehensive audit (streaming, backup, Grafana)
- Automated Metamodel ↔ Cube sync
- Streaming infrastructure review & activation
- Complete documentation
- Full test coverage & security scans

---

## 📊 Current Status

| Phase | Status | Progress | Docs |
|-------|--------|----------|------|
| **S1: Naming** | ✅ Phase 1 Complete | 90% | [TODO](./S1_NAMING_TODO.md) \| [Summary](./S1_SUMMARY.md) |
| **S2: Presence** | 📅 Planned | 0% | - |
| **S3: Cube Kafka** | 📅 Planned | 0% | - |
| **S4: Audit** | 📅 Planned | 0% | - |
| **S5: Metamodel→Cube** | 📅 Planned | 0% | - |
| **S6: Streaming** | 📅 Planned | 0% | - |
| **S7: Docs** | 📅 Planned | 0% | - |
| **S8: Tests & Security** | 📅 Planned | 0% | - |

**Overall Epic Progress:** 11.25% (1/8 phases ~90% complete)

---

## 📖 Quick Links

### Epic Documents
- [Epic Tracking](./PLATFORM_HARDENING_EPIC.md) - Full roadmap, DoD, merge gates
- [NAMING_GUIDE](../NAMING_GUIDE.md) - Naming conventions reference

### S1: Naming Standards
- [S1 TODO](./S1_NAMING_TODO.md) - Detailed task checklist
- [S1 Summary](./S1_SUMMARY.md) - What was done, metrics, next steps
- [Lint Tools](../../tools/naming-lint/) - Automated validators

### CI/CD
- [Naming Lint Workflow](../../.github/workflows/naming-lint.yml)

---

## 🚀 How to Use These Documents

### For Implementors
1. **Start:** Read `PLATFORM_HARDENING_EPIC.md` for context
2. **Work:** Follow `Sx_TODO.md` for current phase
3. **Track:** Update checkboxes in `Sx_TODO.md` as you go
4. **Complete:** Write `Sx_SUMMARY.md` when phase is done

### For Reviewers
1. **Context:** Read `PLATFORM_HARDENING_EPIC.md`
2. **Details:** Check `Sx_SUMMARY.md` for what was done
3. **Verify:** Compare against DoD in `PLATFORM_HARDENING_EPIC.md`

### For Project Managers
1. **Progress:** Check status table in `PLATFORM_HARDENING_EPIC.md`
2. **Blockers:** Look for "Issues Found" in `Sx_TODO.md`
3. **Estimates:** Review time tracking in `Sx_SUMMARY.md`

---

## 🔄 Workflow

```
┌─────────────────┐
│ Read Epic Plan  │
│ (PLATFORM_...)  │
└────────┬────────┘
         │
         v
┌─────────────────┐
│ Create Sx_TODO  │
│ (checklist)     │
└────────┬────────┘
         │
         v
┌─────────────────┐
│ Implement       │
│ (code, tests,   │
│  docs)          │
└────────┬────────┘
         │
         v
┌─────────────────┐
│ Update TODO     │
│ (✅ checkboxes) │
└────────┬────────┘
         │
         v
┌─────────────────┐
│ Write Sx_SUMMARY│
│ (what, metrics, │
│  next)          │
└────────┬────────┘
         │
         v
┌─────────────────┐
│ Update Epic     │
│ (progress %)    │
└────────┬────────┘
         │
         v
┌─────────────────┐
│ Create PR       │
│ (with docs)     │
└─────────────────┘
```

---

## 📏 Standards

### Document Naming
- `Sx_TODO.md` - Task checklist for phase X
- `Sx_SUMMARY.md` - Completion summary for phase X
- `PLATFORM_HARDENING_EPIC.md` - Epic overview (single file)

### TODO Document Structure
```markdown
# Sx: [Phase Name] - Implementation TODO

**Status:** 🚧 In Progress
**Estimate:** Xh

## ✅ Completed
- [x] Task 1
- [x] Task 2

## 🔧 TODO
- [ ] Task 3
- [ ] Task 4

## ⏱️ Time Tracking
| Task | Estimate | Actual |
|------|----------|--------|
| ...  | ...      | ...    |
```

### SUMMARY Document Structure
```markdown
# Sx: [Phase Name] - Implementation Summary

**Status:** ✅ Complete / ✅ Phase 1 Complete

## 🎯 Objectives
## ✅ Completed
## 🔧 Changes Made
## 🧪 Testing
## 📋 DoD Status
## 📈 Metrics
## 🚀 Next Steps
```

---

## 🎨 Status Emojis

| Emoji | Meaning |
|-------|---------|
| 📅 | Planned (not started) |
| 🚧 | In Progress |
| ✅ | Completed |
| ⚠️ | Blocked / Issues |
| 🔄 | In Review |
| 📝 | Needs Documentation |
| 🧪 | Needs Testing |

---

## 🤝 Contributing

When working on a phase:

1. **Create branch** from `feature/platform-hardening-epic`
   ```bash
   git checkout -b feature/s2-presence-kafka-stale
   ```

2. **Create Sx_TODO.md** if not exists
   ```bash
   cp docs/epics/S1_TODO.md docs/epics/S2_TODO.md
   # Edit S2_TODO.md with phase-specific tasks
   ```

3. **Work & Update**
   - Implement code
   - Write tests
   - Update docs
   - Check off tasks in Sx_TODO.md

4. **Write Sx_SUMMARY.md**
   - What was done
   - Metrics (files changed, LOC, time)
   - DoD checklist
   - Next steps

5. **Update PLATFORM_HARDENING_EPIC.md**
   - Update status table
   - Update progress %
   - Add PR link

6. **Create PR**
   - Title: `S2: Presence + Kafka Stale Mode`
   - Description: Link to Sx_SUMMARY.md
   - Labels: `epic:platform-hardening`, `phase:s2`

---

## 📞 Contact

- **Epic Owner:** Platform Team
- **Questions:** Open issue with label `epic:platform-hardening`
- **Updates:** Check `PLATFORM_HARDENING_EPIC.md` progress table

---

**Last Updated:** 11. října 2025  
**Epic Started:** 11. října 2025  
**Epic Branch:** `feature/platform-hardening-epic`
