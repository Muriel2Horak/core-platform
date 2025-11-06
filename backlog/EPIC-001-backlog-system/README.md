# EPIC-001: Git-Native Backlog Management System

> **Meta-Epic:** Implementace backlog systému dokumentovaného v backlogu samotném (dogfooding)

## 🎯 Epic Goal

Vytvořit lightweight, Git-first backlog management systém pro core-platform, který:
- Používá Markdown jako primární formát (optimální pro GitHub Copilot)
- Propojuje User Stories s implementací (code/test/docs paths)
- Automaticky trackuje progress přes Git commits
- Eliminuje potřebu JIRA pro vývoj (Git je source of truth)

## 📊 Epic Scope

### In Scope ✅
- Markdown struktura pro Stories a Subtasks
- Template systém pro konzistentní stories
- Git-based tracking (commit → story mapping)
- Path mapping (story → code/test/docs)
- Validation tooling (schema, coverage)
- Makefile integration
- GitHub Copilot optimization

### Out of Scope ❌
- JIRA import/export (Phase 2)
- Web UI (VS Code + GitHub je dost)
- External dependencies (lightweight Python only)
- AI-powered analysis (Copilot handles this)

## 👥 Stakeholders

- **Development Team** - Primární uživatelé (píšou stories v Markdown)
- **GitHub Copilot** - Konzumuje stories jako context pro code generation
- **Product Owner** - Čte backlog v GitHub (Markdown rendering)
- **Tech Lead** - Validuje DoD před merge

## 📅 Timeline

- **Start:** 6. listopadu 2025
- **Target Completion:** 13. listopadu 2025 (1 týden)
- **Phase 1 (MVP):** 3 dny (templates + basic tooling)
- **Phase 2 (Advanced):** 4 dny (git tracking + automation)

## 📋 User Stories (5 stories)

### Priority 1 (Must Have)
1. **[CORE-001](stories/CORE-001-markdown-structure/README.md)** - Markdown struktura a templates
   - Základní adresářová struktura
   - Story template s DoR/AC/DoD
   - Subtask template
   - Example stories

2. **[CORE-002](stories/CORE-002-path-mapping/README.md)** - Implementation path mapping
   - YAML schema pro code/test/docs paths
   - Template rozšíření o path mapping
   - Validation path existence

3. **[CORE-003](stories/CORE-003-story-generator/README.md)** - Story generator script
   - `scripts/backlog/new_story.sh`
   - Interactive wizard (name, type, priority)
   - Auto-create Git branch
   - Open in VS Code

### Priority 2 (Should Have)
4. **[CORE-004](stories/CORE-004-git-tracker/README.md)** - Git commit tracker
   - Parse commit messages (feat(002): pattern)
   - Map commits → stories via paths
   - Coverage report (code/test/docs)
   - Auto-update story checklist

5. **[CORE-005](stories/CORE-005-validation/README.md)** - Story validator & DoD checker
   - Validate story schema (required sections)
   - Check path mapping correctness
   - Verify DoD completeness
   - Pre-merge validation

### Priority 3 (Nice to Have - Phase 2)
6. **CORE-006** - Makefile integration (make backlog-*)
7. **CORE-007** - Git hooks (auto-update on commit)
8. **CORE-008** - Backlog dashboard (index.md generator)
9. **CORE-009** - JIRA sync (optional export)

## 🎯 Success Criteria

### Technical Metrics
- ✅ 5 stories v Markdown struktuře
- ✅ 100% stories mají path mapping
- ✅ Git tracker funguje (commit → story)
- ✅ Validator detekuje incomplete stories
- ✅ DoD automated check před mergem

### Adoption Metrics
- ✅ Dev team používá pro nové features
- ✅ GitHub Copilot generuje kód ze stories
- ✅ 80%+ stories mají complete path mapping
- ✅ Zero JIRA závislost pro vývoj

### Quality Metrics
- ✅ Story template coverage > 90%
- ✅ Path mapping accuracy > 95%
- ✅ Git tracking false positives < 5%

## 🔗 Dependencies

### Upstream Dependencies
- ✅ Git repository (core-platform)
- ✅ Python 3.8+ (pro scripty)
- ✅ Make (orchestrace)

### Downstream Dependencies
- Všechny budoucí features budou používat tento systém
- Development workflow bude závislý na backlog struktuře

## 🚀 Implementation Approach

### Phase 1: Foundation (3 dny)
**Goal:** Basic struktura + templates + manual workflow

**Day 1: Struktura**
- [x] Vytvořit `backlog/` adresář
- [ ] Story CORE-001: Markdown struktura
- [ ] Example story (EPIC-001 sám sebe)

**Day 2: Templates**
- [ ] Story CORE-002: Path mapping
- [ ] Story CORE-003: Story generator

**Day 3: Validation**
- [ ] Story CORE-005: Basic validator
- [ ] Manual testing všech stories

### Phase 2: Automation (4 dny)
**Goal:** Git integration + auto-tracking

**Day 4-5: Git Tracker**
- [ ] Story CORE-004: Git commit tracker
- [ ] Commit → story mapping
- [ ] Coverage reporting

**Day 6-7: Integration**
- [ ] Makefile targets
- [ ] Git hooks (optional)
- [ ] Documentation

## 📊 Epic Metrics Dashboard

### Story Progress
```
CORE-001: ⏳ Not Started (0% DoD)
CORE-002: ⏳ Not Started (0% DoD)
CORE-003: ⏳ Not Started (0% DoD)
CORE-004: ⏳ Not Started (0% DoD)
CORE-005: ⏳ Not Started (0% DoD)

Epic Progress: 0/5 stories (0%)
```

### Velocity Tracking
```
Week 1 (Nov 6-13):
  Planned: 5 stories
  Completed: 0 stories
  Velocity: 0 SP/week (first sprint)
```

## 🔍 Risks & Mitigations

### Risk 1: Dev Team Adoption
**Risk:** Team nechce opustit JIRA  
**Probability:** Medium  
**Impact:** High  
**Mitigation:** 
- Začít s optional backlog (JIRA parallel)
- Ukázat výhody (Copilot, Git-native)
- Phase out JIRA postupně

### Risk 2: Schema Evolution
**Risk:** Story template se bude měnit  
**Probability:** High  
**Impact:** Medium  
**Mitigation:**
- Version schema v templates
- Migration script pro updates
- Backward compatibility

### Risk 3: Git Tracker Accuracy
**Risk:** False positives/negatives v commit → story mappingu  
**Probability:** Medium  
**Impact:** Low  
**Mitigation:**
- Strict commit message format
- Manual override možnost
- Regular audits

## 📖 References

### Inspiration
- **backlog-analyzer** projekt (Martin Horak)
  - Path: `/Users/martinhorak/Projects/backlog-analyzer`
  - Sample story: `examples/sample-story/README.md`
  - Použijeme: Markdown struktura, validator patterns

### External Standards
- [Conventional Commits](https://www.conventionalcommits.org/) - Commit message format
- [Semantic Versioning](https://semver.org/) - Story versioning
- [GitHub Markdown](https://github.github.com/gfm/) - Markdown flavor

### Internal Docs
- `.github/copilot-instructions.md` - Development guidelines
- `README.md` - Project overview

## 🎓 Lessons from backlog-analyzer

### What to Keep ✅
1. **Markdown struktura** - Epic → Story → Subtask hierarchy
2. **DoR/AC/DoD pattern** - Clear story definition
3. **Path mapping concept** - Code/test/docs links
4. **Validator patterns** - Schema enforcement

### What to Simplify 🔧
1. **No JIRA import** - Git-native from start
2. **No Streamlit GUI** - VS Code is enough
3. **No AI analyzer** - Copilot does this
4. **Lightweight tooling** - Python scripts only

### What to Add 🚀
1. **Git integration** - Commit → story tracking
2. **Copilot optimization** - Better prompt structure
3. **Auto-validation** - Pre-merge DoD checks
4. **Coverage tracking** - Code/test/docs completeness

## ✅ Epic Definition of Done

- [ ] All 5 priority 1 stories completed
- [ ] Templates tested on 3 real features
- [ ] Documentation complete (README + examples)
- [ ] Dev team trained (workshop done)
- [ ] Makefile targets functional
- [ ] Validator catches common errors
- [ ] Git tracker accuracy > 90%
- [ ] Zero JIRA dependency proven
- [ ] Epic dogfooded (this epic managed in backlog)

---

**Epic Owner:** Development Team  
**Created:** 2025-11-06  
**Status:** 🚀 **IN PROGRESS** (bootstrapping)  
**Next Action:** Create CORE-001 story (Markdown struktura)
