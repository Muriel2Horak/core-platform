---
id: CORE-008
epic: EPIC-001-backlog-system
title: "Story Schema Validator & Quality Checker"
priority: P1
status: ready
assignee: ""
created: 2025-11-06
updated: 2025-11-06
estimate: "2 days"

path_mapping:
  code_paths:
    - scripts/backlog/story_validator.py
    - scripts/backlog/lib/schema_checker.py
    - scripts/backlog/lib/quality_scorer.py
  test_paths:
    - scripts/backlog/test_story_validator.py
  docs_paths:
    - backlog/README.md
    - docs/development/story-quality-guide.md
    - CHANGELOG.md
---

# CORE-008: Story Schema Validator & Quality Checker

> **Epic:** [EPIC-001](../../README.md) | **Priority:** P1 | **Estimate:** 2 days

## 👤 Role / Potřeba / Benefit

Jako **developer** potřebuji **automatickou validaci kvality story** abych **zajistil kompletní zadání před implementací**.

**Problem:** Nekvalitní stories → ambiguous implementace, chybějící AC, incomplete DoD

**Value:** Quality gate PŘED implementací, automated checks, quality score (0-100%)

---

## ✅ Definition of Ready

- [x] Role/Need/Benefit defined
- [x] All AC measurable
- [x] Path mapping filled
- [x] Dependencies: CORE-001, 006, 007
- [x] Technical approach: Python validator extending path_validator
- [x] Estimate: 2 days

---

## 🎯 Acceptance Criteria

### AC1: Schema Validation - 8 Required Sections

**Given** story README.md
**When** validator parses
**Then** verify ALL sections present:
1. YAML frontmatter
2. Role/Need/Benefit
3. DoR
4. AC
5. AC→Test Mapping
6. Implementation Mapping
7. DoD
8. Subtasks

**Test:** `python3 scripts/backlog/story_validator.py --story CORE-005`

---

### AC2: DoR/DoD Completeness (% checked items)

**Given** DoR/DoD checklists
**When** count [x] vs [ ]
**Then** report % + missing items

Minimum: DoR 75%, DoD 80%

---

### AC3: AC Testability (Given/When/Then + tests)

**Given** AC sections
**When** parse format
**Then** verify:
- Given/When/Then present
- Test section exists
- AC→Test mapping table present

---

### AC4: YAML Frontmatter Validation

**Given** YAML frontmatter
**When** parse
**Then** check:
- Valid syntax
- Required fields (id, epic, title, priority, status)
- path_mapping defined
- Valid enum values (P1/P2/P3, ready/in-progress/done)

---

### AC5: Quality Score (0-100%)

**Formula:**
- Schema: 40pts (8 sections × 5)
- DoR: 15pts
- DoD: 15pts
- AC testability: 15pts
- Path mapping: 10pts
- YAML: 5pts

**Levels:**
- 90-100%: ✅ EXCELLENT
- 70-89%: ⚠️ GOOD
- 50-69%: ⚠️ FAIR
- 0-49%: ❌ POOR

---

### AC6: CLI & Output Formats

**Options:**
```bash
--story CORE-XXX
--epic EPIC-XXX
--check-schema|--check-dod|--check-ac|--check-yaml
--score
--format text|json
--min-score NN
--strict
```

**Outputs:** Text (colored) + JSON (for CI/CD)

---

## 📂 Implementation Mapping

See `path_mapping` in YAML frontmatter.

---

## ✅ Definition of Done

### Code
- [ ] `story_validator.py` created (main CLI)
- [ ] `lib/schema_checker.py` (section validation)
- [ ] `lib/quality_scorer.py` (score calculation)
- [ ] Integrates with existing path_validator

### Testing
- [ ] Integration tests cover all 6 AC
- [ ] Test on CORE-001, 005, 006, 007
- [ ] Edge cases: missing sections, invalid YAML
- [ ] All tests passing

### Documentation
- [ ] `docs/development/story-quality-guide.md` created
- [ ] backlog/README.md updated
- [ ] CHANGELOG.md entry

---

**Story complete, ready for implementation!**

