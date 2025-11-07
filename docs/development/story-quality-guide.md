# Story Quality Validation Guide

> Part of **CORE-008**: Story Schema Validator & Quality Checker

## 📋 Overview

The story validator ensures **100% reliable story quality** before implementation starts. It checks:

- ✅ **Schema**: All 8 required sections present
- ✅ **DoR/DoD**: Completeness % (minimum thresholds)
- ✅ **AC Testability**: Given/When/Then format + test mapping
- ✅ **YAML**: Valid frontmatter syntax
- ✅ **Path Mapping**: Code, test, and docs paths defined
- ✅ **Quality Score**: 0-100% overall rating

---

## 🚀 Quick Start

### Validate Single Story

```bash
# Basic validation
python3 scripts/backlog/story_validator.py --story CORE-008

# With quality score
python3 scripts/backlog/story_validator.py --story CORE-008 --score

# JSON output for CI/CD
python3 scripts/backlog/story_validator.py --story CORE-008 --score --format json
```

### Check Specific Aspects

```bash
# Schema only
python3 scripts/backlog/story_validator.py --story CORE-008 --check-schema

# DoR/DoD only
python3 scripts/backlog/story_validator.py --story CORE-008 --check-dod

# AC format only
python3 scripts/backlog/story_validator.py --story CORE-008 --check-ac

# YAML only
python3 scripts/backlog/story_validator.py --story CORE-008 --check-yaml
```

### Enforce Quality Gates

```bash
# Require minimum 80% score
python3 scripts/backlog/story_validator.py --story CORE-008 --score --min-score 80

# Exit with error if below threshold (for CI/CD)
python3 scripts/backlog/story_validator.py --story CORE-008 --score --min-score 80 --strict
```

---

## 📊 Quality Scoring System

### Formula (Total: 100 points)

| Component | Points | Description |
|-----------|--------|-------------|
| **Schema** | 40 | 8 required sections × 5 points each |
| **DoR Completeness** | 15 | Percentage of checked DoR items |
| **DoD Completeness** | 15 | Percentage of checked DoD items |
| **AC Testability** | 15 | Given/When/Then format + test mapping |
| **Path Mapping** | 10 | Code, test, and docs paths defined |
| **YAML Validity** | 5 | Valid frontmatter syntax |

### Quality Levels

| Score | Level | Emoji | Meaning |
|-------|-------|-------|---------|
| 90-100% | ✅ **EXCELLENT** | ✅ | Ready to implement - high quality |
| 70-89% | ⚠️ **GOOD** | ⚠️ | Can implement - minor issues |
| 50-69% | ⚠️ **FAIR** | ⚠️ | Needs improvement before starting |
| 0-49% | ❌ **POOR** | ❌ | Cannot start - critical issues |

---

## 📖 Validation Details

### 1. Schema Validation (40 points)

Checks that ALL 8 required sections are present:

1. **YAML frontmatter** (`---` delimited)
2. **Role / Potřeba / Benefit** (user story)
3. **Definition of Ready** (checklist)
4. **Acceptance Criteria** (numbered AC1, AC2, ...)
5. **AC to Test Mapping** (test tables per AC)
6. **Implementation Mapping** (path_mapping in YAML)
7. **Definition of Done** (DoD checklist)
8. **Subtasks** (breakdown)

**Scoring:** `(sections_found / 8) × 40 points`

**Example:**
- 8/8 sections = 40 points ✅
- 6/8 sections = 30 points ⚠️
- 4/8 sections = 20 points ❌

---

### 2. DoR Completeness (15 points)

Counts checked items in Definition of Ready:

```markdown
## ✅ Definition of Ready

- [x] Role/Need/Benefit defined
- [x] All AC measurable
- [ ] Path mapping filled        ← Unchecked
- [x] Dependencies identified
```

**Scoring:** `(checked / total) × 15 points`

**Minimum:** 75% recommended (11.25 points)

---

### 3. DoD Completeness (15 points)

Counts checked items in Definition of Done:

```markdown
## ✅ Definition of Done

### Code
- [x] Implementation complete
- [ ] All AC implemented         ← Unchecked

### Testing
- [x] Unit tests passing
- [ ] Integration tests passing  ← Unchecked
```

**Scoring:** `(checked / total) × 15 points`

**Minimum:** 80% recommended (12 points)

---

### 4. AC Testability (15 points)

Validates Acceptance Criteria format:

**Required per AC:**
- ✅ **Given** clause (context)
- ✅ **When** clause (action)
- ✅ **Then** clause (expected result)
- ✅ **Test** section (how to verify)
- ✅ AC→Test Mapping table (test paths)

**Example:**

```markdown
### AC1: Schema Validation

**Given** story README.md
**When** validator parses
**Then** verify ALL sections present

**Test:** `python3 scripts/backlog/story_validator.py --story CORE-005`
```

**Scoring:** `(testable_ac / total_ac) × 15 points`

---

### 5. Path Mapping (10 points)

Checks YAML frontmatter has `path_mapping` defined:

```yaml
path_mapping:
  code_paths:
    - scripts/backlog/story_validator.py
  test_paths:
    - scripts/backlog/test_story_validator.py
  docs_paths:
    - docs/development/story-quality-guide.md
```

**Scoring:** All or nothing - 10 points if present, 0 if missing

---

### 6. YAML Validity (5 points)

Validates YAML frontmatter syntax:

```yaml
---
id: CORE-008
epic: EPIC-001-backlog-system
title: "Story Schema Validator"
priority: P1
status: ready
estimate: "2 days"
---
```

**Scoring:** All or nothing - 5 points if valid, 0 if malformed

---

## 🎯 Usage Examples

### Example 1: Pre-Implementation Check

```bash
# Before starting work on CORE-012
python3 scripts/backlog/story_validator.py --story CORE-012 --score --min-score 70

# If score < 70, validator exits with code 1
# Developer fixes issues BEFORE implementing
```

### Example 2: CI/CD Quality Gate

```yaml
# .github/workflows/pr-validation.yml
- name: Validate Story Quality
  run: |
    STORY_ID=$(git branch --show-current | grep -oE 'CORE-[0-9]+')
    python3 scripts/backlog/story_validator.py \
      --story $STORY_ID \
      --score \
      --min-score 80 \
      --format json
```

### Example 3: Batch Validation

```bash
# Validate all stories in EPIC-001
for story in CORE-{001..008}; do
  echo "Validating $story..."
  python3 scripts/backlog/story_validator.py --story $story --score
done
```

### Example 4: JSON Processing

```bash
# Get quality score for dashboard
python3 scripts/backlog/story_validator.py \
  --story CORE-008 \
  --score \
  --format json \
  | jq '.quality_score.total'

# Output: 70.0
```

---

## 🔧 Integration with Existing Tools

### Combined Validation

```bash
# Full story validation (all tools)
STORY=CORE-008

# 1. Schema & quality
python3 scripts/backlog/story_validator.py --story $STORY --score

# 2. Path validation
python3 scripts/backlog/path_validator.py --story $STORY

# 3. Test coverage
bash scripts/backlog/test_validator.sh --story $STORY

# 4. Git tracking
python3 scripts/backlog/git_tracker.py --story $STORY
```

### Future: Makefile Integration (CORE-009)

```makefile
validate-story:
    @python3 scripts/backlog/story_validator.py --story $(STORY) --score
    @python3 scripts/backlog/path_validator.py --story $(STORY)
    @bash scripts/backlog/test_validator.sh --story $(STORY)
    @echo "✅ All validations passed!"

pre-merge-check:
    @STORY=$$(git branch --show-current | grep -oE 'CORE-[0-9]+')
    @make validate-story STORY=$$STORY
```

---

## 📈 Improving Story Quality

### Common Issues & Fixes

#### Issue: Low Schema Score (< 30/40)

**Symptoms:** Missing sections

**Fix:**
1. Use story template: `backlog/templates/story.md`
2. Check all 8 sections present
3. Run `--check-schema` to see what's missing

#### Issue: Low DoR/DoD Score (< 10/15)

**Symptoms:** Unchecked items

**Fix:**
1. Review checklist items
2. Mark completed items as `[x]`
3. Remove non-applicable items
4. Ensure 75%+ checked before starting

#### Issue: Low AC Testability (< 10/15)

**Symptoms:** Missing Given/When/Then

**Fix:**
1. Rewrite AC in Given/When/Then format
2. Add **Test:** section with verification command
3. Fill AC→Test Mapping table
4. Link to test files

#### Issue: Missing Path Mapping (0/10)

**Symptoms:** No `path_mapping` in YAML

**Fix:**
```yaml
path_mapping:
  code_paths:
    - path/to/implementation.py
  test_paths:
    - path/to/test.py
  docs_paths:
    - docs/feature.md
```

---

## 🧪 Testing

### Run Integration Tests

```bash
# All tests (13 test cases)
python3 scripts/backlog/test_story_validator.py

# Expected output:
# Ran 13 tests in 0.5s
# OK
# Success rate: 100.0%
```

### Test Coverage

- ✅ Schema validation
- ✅ DoR/DoD completeness
- ✅ AC format validation
- ✅ YAML frontmatter
- ✅ Quality scoring
- ✅ Level classification
- ✅ Min-score enforcement
- ✅ JSON output format
- ✅ Error handling (invalid story)
- ✅ Multi-story comparison

---

## 📚 References

- **Story Template:** `backlog/templates/story.md`
- **CORE-008 Story:** `backlog/EPIC-001-backlog-system/stories/CORE-008-.../README.md`
- **Validator Source:** `scripts/backlog/story_validator.py`
- **Schema Checker:** `scripts/backlog/lib/schema_checker.py`
- **Quality Scorer:** `scripts/backlog/lib/quality_scorer.py`
- **Integration Tests:** `scripts/backlog/test_story_validator.py`

---

## 🎓 Best Practices

### 1. Validate Early

Run validator **immediately after** creating story:

```bash
make backlog-new STORY="My Feature" ...
python3 scripts/backlog/story_validator.py --story CORE-XXX --score
```

### 2. Fix Before Implementing

**DO NOT start coding** if score < 70%. Fix story quality first.

### 3. Use in CI/CD

Add quality gate to pull requests - prevent merging low-quality stories.

### 4. Track Quality Over Time

```bash
# Generate quality report for all stories
for story in $(find backlog -name "CORE-*" -type d); do
  id=$(basename $story | grep -oE 'CORE-[0-9]+')
  score=$(python3 scripts/backlog/story_validator.py --story $id --score --format json 2>/dev/null | jq -r '.quality_score.total // 0')
  echo "$id: $score"
done | sort -t: -k2 -nr
```

### 5. Continuous Improvement

Review low-scoring stories and refactor templates based on common issues.

---

**Quality gates prevent waste!** 🎯  
Better to spend 10 minutes fixing story than 2 hours implementing wrong thing.
