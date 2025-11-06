# Backlog Scripts

Automation tools for Git-native backlog system (EPIC-001).

## 🛠️ Available Tools

### 1. Story Generator (`new_story.sh`)

Creates new user stories from template with auto-generated story IDs.

**Usage:**
```bash
bash scripts/backlog/new_story.sh \
  --title "My Feature" \
  --epic "EPIC-001-backlog-system" \
  --priority "P1" \
  --estimate "3 days"
```

**Features:**
- Auto-increments story IDs (CORE-001, CORE-002, ...)
- Creates Git feature branch
- Fills template with metadata
- See CORE-003 story for details

---

### 2. Git Commit Tracker (`git_tracker.sh`)

Tracks which Git commits belong to which stories.

**Usage:**
```bash
# Show commits for epic
bash scripts/backlog/git_tracker.sh --epic EPIC-001-backlog-system

# Show commits for specific story
bash scripts/backlog/git_tracker.sh --story CORE-005

# JSON output
bash scripts/backlog/git_tracker.sh --epic EPIC-001 --format json

# Include stories with 0 commits
bash scripts/backlog/git_tracker.sh --epic EPIC-001 --show-zero
```

**Output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 Git Activity Report: EPIC-001-backlog-system
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ CORE-001:     1 commit(s) (f6332b6)
✅ CORE-003:     2 commit(s) (f6332b6,0d523e7)
✅ CORE-005:     5 commit(s) (83ff64b,19731a6,...)

📈 Summary: 8 commits across 3 stories
```

**See:** CORE-005 story for full documentation

---

### 3. Path Validator (`path_validator.py`) ⭐ NEW

Validates path mappings in story YAML frontmatter and generates coverage reports.

**Installation:**
```bash
# PyYAML required
pip3 install pyyaml --user
```

**Usage:**

#### Validate Single Story
```bash
# Text report (default)
python3 scripts/backlog/path_validator.py --story CORE-005

# JSON report
python3 scripts/backlog/path_validator.py --story CORE-005 --format json
```

**Output:**
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 Path Mapping Coverage: CORE-005
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ code_paths   1/1 (100%)
   scripts/backlog/git_tracker.sh

⚠️ test_paths   0/1 (0%)
   ❌ MISSING (1):
      - scripts/backlog/test_git_tracker.sh

✅ docs_paths   3/3 (100%)
   backlog/README.md, docs/development/backlog-workflow.md, CHANGELOG.md

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📈 Overall: 80% (4/5 paths exist)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

#### Validate Entire Epic
```bash
# All stories in epic
python3 scripts/backlog/path_validator.py --epic EPIC-001

# Include 0% stories
python3 scripts/backlog/path_validator.py --epic EPIC-001 --show-zero

# JSON output (for automation)
python3 scripts/backlog/path_validator.py --epic EPIC-001 --format json | jq .
```

**JSON Output Structure:**
```json
{
  "story_id": "CORE-005",
  "coverage": {
    "code_paths": {
      "total": 1,
      "exist": 1,
      "missing": [],
      "percentage": 100.0
    },
    "test_paths": {
      "total": 1,
      "exist": 0,
      "missing": ["scripts/backlog/test_git_tracker.sh"],
      "percentage": 0.0
    },
    "docs_paths": {
      "total": 3,
      "exist": 3,
      "missing": [],
      "percentage": 100.0
    }
  },
  "overall": {
    "total": 5,
    "exist": 4,
    "percentage": 80.0
  }
}
```

**Features:**
- ✅ Validates file existence for code_paths, test_paths, docs_paths
- ✅ Supports glob patterns (`backend/**/*.java`)
- ✅ Story-level and epic-level reporting
- ✅ Text (human-readable) and JSON (machine-readable) outputs
- ✅ Performance: <5s for 100 stories (actual: ~130ms)

**See:** CORE-006 story for full specification

---

## 📂 Library Modules

Internal modules (in `scripts/backlog/lib/`):

### `yaml_parser.py`
- Parse YAML frontmatter from story README.md
- Extract path_mapping section
- Get story ID

### `path_checker.py`
- Validate file existence
- Support glob patterns
- Calculate coverage percentages

### `coverage_reporter.py`
- Generate text reports (with emojis)
- Generate JSON reports
- Story and epic aggregation

---

## 🧪 Testing

Run integration tests:
```bash
python3 scripts/backlog/test_integration.py
```

**Test Coverage:**
- ✅ YAML parsing (5 assertions)
- ✅ Path validation (7 assertions)
- ✅ Text reporting (6 assertions)
- ✅ JSON reporting (8 assertions)
- ✅ Performance (2 assertions)
- ✅ Edge cases (5 assertions)

**Total:** 33/33 assertions passing ✅

---

## 📖 Story References

- **CORE-001:** Markdown structure & templates
- **CORE-003:** Story generator (`new_story.sh`)
- **CORE-005:** Git commit tracker (`git_tracker.sh`)
- **CORE-006:** Path validator (`path_validator.py`) ⭐

---

## 🔮 Future Tools (Planned)

- **CORE-007:** Story validator & DoD checker
- **CORE-008:** Makefile integration (`make backlog-validate`)
- **CORE-009:** Git pre-commit hooks
- **CORE-010:** Dashboard/reporting UI
