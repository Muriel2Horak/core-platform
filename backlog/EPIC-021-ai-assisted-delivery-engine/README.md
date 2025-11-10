# EPIC-021: AI-Assisted Delivery Engine (Codex + Copilot)

**Status:** ⚪ **0% – PLANNED** (Foundations in backlog)  
**Priority:** 🔥 **HIGH** (delivery acceleration & quality gate initiative)  
**Effort:** ~30–35 dní (2 týmy, sdílené mezi core-platform & isp-migration-tool)  
**Dependencies:** EPIC-001 (backlog system), EPIC-005 (metamodel studio & docs), EPIC-016 (AI tooling/MCP), EPIC-020 (secure SDLC quality gates)

> **Mission:** Vybudovat opakovatelný, bezpečný a auditovatelný proces, ve kterém AI agenti (Codex, Copilot, další MCP klienti) automatizovaně zpracují DevTasky z oficiálního backlogu, napíší kód/testy/PR, provedou AI review a při nejasnostech navrhnou doplňující úkoly – to vše v sandboxu, s řízenými právy, měřitelnými KPI a jasným human override flow.

---

## 🎯 Vision

1. **AI jako plnohodnotný člen delivery týmu**, který zvládne repetitivní DevTasky = rychlejší lead time, víc kapacity pro seniory.  
2. **Security & Governance by design:** jasně popsané role botů, žádný přístup do produkce, audit trail každého kroku.  
3. **Plug & Play orchestrace napříč repy:** stejné workflow pro `core-platform` i `isp-migration-tool`, sdílené šablony DevTasků, AGENTS.md a CI/CD integrace.  
4. **Měřitelnost:** KPI dashboard pro pilot (lead time, počet revizí, bug rate, ušetřený čas), aby bylo možné rozhodnout o škálování.  
5. **Human-first governance:** AI navrhuje / implementuje, člověk má override, review a finální rozhodnutí.

### KPI (pilot core-platform)

| Metric | Baseline | Pilot Target | Notes |
|--------|----------|--------------|-------|
| Lead time AI-eligible DevTask | 5 dní | ≤2 dny | měřeno od Ready → Merged |
| Počet revizí průměrného AI PR | 3 kola | ≤1 kolo | lidské review po AI review |
| AI PR bug rate (do 2 sprintů) | 15 % | ≤5 % | bug = follow-up issue na PR |
| Escalace na člověka | N/A | ≤30 % | AI hlásí blokéry místo guess |
| Ušetřený čas seniorů | N/A | ≥20 h/sprint | odhad přes timesheet/retro |

---

## 📦 Scope

### In Scope ✅
- DevTask template + DoR/DoD guidelines s AI-ELIGIBLE flagem.  
- AGENTS.md + engineering standards (coding, testing, logging, DB changes, security).  
- Sdílený setup Codex/Copilot + CI integrace s bot účty.  
- Automovaný flow DevTask → branch → AI implementace → PR → AI review → human review.  
- Integrace na backlog systém (Git-based, Jira/YouTrack/GitHub Issues dle projektu).  
- Sandbox prostředí (repo fork, oddělené secrets, test DB, mock services).  
- Security & governance policy, audit log stream, KPI dashboard.  
- Multi-agent coordination a noise-control filtry.

### Out of Scope ❌
- Full autopilot merge do mainu (human approval je mandatory).  
- Refaktorování kompletních modulů (pilot řeší menší/mid DevTasky).  
- Přístup AI do produkčních DB/secrets.  
- Nahrazení produktového rozhodování (AI nevytváří backlog sama).  

---

## 👥 Stakeholders

| Role | Zodpovědnost |
|------|--------------|
| **CTO / Sponsor** | Rozhodnutí o investici, schválení rollout na oba projekty |
| **Tech Leads (core-platform & isp-migration-tool)** | DoR/DoD definice, human override, security approval |
| **Security & Compliance Lead** | Bot identity, secret management, audit remediation |
| **DevOps / Platform Team** | Sandbox infra, CI/CD integrace, telemetry stack |
| **AI Agent Owners (Codex/Copilot)** | Prompting, MCP tools, agent orchestrace |
| **Developers / Reviewers** | Konsumace AI PR, human approvals, feedback loop |

---

## 🔐 Security & Governance Non-Negotiables

1. **Bot identity & least privilege:** AI běží na dedikovaných GitHub/Jira účtech bez práva push do `main`/`prod`.  
2. **Sandbox only:** žádné produkční endpointy, secrets ani DB; jen test/fake data.  
3. **Audit trail:** každý krok AI (task claim, prompt, diff, test run, PR, review) logovaný do sdíleného úložiště (Git + Loki/ELK).  
4. **Human override:** definovaný STOP flow (nedostupné API, chybějící specifikace, opakované test fail).  
5. **KPI visibility:** dashboard v Grafaně (nebo jiném stacku) dostupný pro management, porovnání s baseline.

---

## 🛠️ Target Architecture (High-Level Flow)

```
Backlog (Git/Jira) → AI Orchestrator → Sandbox Repo/Branch → Codex/Copilot agent
   ↓ claim lock             ↓ audit log                   ↓
   Status update      Bot identity (PAT)           Implementation scripts
   ↓                        ↓                            ↓
 DevTask metadata    Test DB / mock services      PR (lint+tests+coverage)
   ↓                        ↓                            ↓
 KPI collector  ←  Audit log & artifacts ←  AI review (structured) ← Human review/merge
```

---

## 📅 Phases & Milestones

| Phase | Focus | Stories |
|-------|-------|---------|
| **Phase 0 – Foundations** | Template, standards, bot setup | AIDE-001 → AIDE-003 |
| **Phase 1 – Implementation Loop** | AI DevTask → PR, AI review, governance | AIDE-004 → AIDE-007 |
| **Phase 2 – Orchestration** | Backlog intake, override/escalace, sandbox, multi-agent | AIDE-008 → AIDE-012 |
| **Phase 3 – Quality & Insights** | KPIs, noise control, rollout | AIDE-010 + AIDE-013 |

Target pilot: core-platform sprint (2 týdny). Rollout: po ověření KPI i do `isp-migration-tool`.

---

## 📋 User Stories Overview

| ID | Story | Status | Priority | Dependencies |
|----|-------|--------|----------|--------------|
| **AIDE-001** | AI-ready DevTask Template | ⏳ PLANNED | 🔥 HIGH | EPIC-001 |
| **AIDE-002** | AGENTS.md & Engineering Standards | ⏳ PLANNED | 🔥 HIGH | EPIC-005, EPIC-020 |
| **AIDE-003** | Codex/Copilot Setup & Bot Accounts | ⏳ PLANNED | 🔥 HIGH | EPIC-016 |
| **AIDE-004** | AI Implementation Agent: DevTask → PR | ⏳ PLANNED | 🔥 HIGH | AIDE-001..003 |
| **AIDE-005** | AI Code Review & Quality Gate | ⏳ PLANNED | 🔥 HIGH | AIDE-004, EPIC-020 |
| **AIDE-006** | Security & Governance Guardrails | ⏳ PLANNED | 🔥 HIGH | Security team |
| **AIDE-007** | Audit & Logging of AI Actions | ⏳ PLANNED | 🔥 HIGH | AIDE-006 |
| **AIDE-008** | Task Intake & Backlog Integration | ⏳ PLANNED | 🟡 MEDIUM | EPIC-001 |
| **AIDE-009** | Human Override & Escalation Flow | ⏳ PLANNED | 🟡 MEDIUM | AIDE-004 |
| **AIDE-010** | Telemetry & KPI Dashboard | ⏳ PLANNED | 🟡 MEDIUM | AIDE-007, EPIC-003 |
| **AIDE-011** | Sandbox / Test Environment Automation | ⏳ PLANNED | 🔥 HIGH | DevOps, EPIC-007 |
| **AIDE-012** | Multi-Agent Coordination & Task Claim | ⏳ PLANNED | 🟡 MEDIUM | AIDE-008 |
| **AIDE-013** | Noise Control & Quality Filters | ⏳ PLANNED | 🟡 MEDIUM | AIDE-004, AIDE-005 |

---

## 📝 Detailed User Stories & Acceptance Criteria

### AIDE-001: AI-ready DevTask Template
**Jako** Tech Lead  
**chci** mít strukturovaný DevTask formát s jasným DoR/DoD  
**aby** AI agent věděl přesně, co implementovat.

**Acceptance Criteria**
- Šablona DevTasku obsahuje sekce: Kontext (EPIC/US), Scope, Constraints, DoD, odkazy na reference a nový flag `AI-ELIGIBLE: Yes/No`.  
- DevTask s `AI-ELIGIBLE: Yes` musí mít vyplněný kontext, DoD a odkazy na testy/logy; jinak validátor zakáže automatické vyzvednutí.  
- DoR checklist doplněn o AI readiness (flag, odkazy, test strategy).  
- Template publikována v `CONTRIBUTING.md` a `BACKLOG_GUIDE.md`, sdíleno v obou projektech.

### AIDE-002: AGENTS.md & Engineering Standards
**Jako** AI Agent Owner  
**chci** mít v každém repu AGENTS.md + guidelines  
**aby** Codex/Copilot psali kód/testy/logy konzistentně a bezpečně.

**Acceptance Criteria**
- `AGENTS.md` obsahuje: architekturu, code style, test strategy (JUnit/PyTest/Playwright), logging conventions, pravidla pro DB změny.  
- Zahrnuta sekce **Security & Sensitive Data**: jak maskovat secrets, jak popisovat config bez leaků, zákaz commitování credentials.  
- Odkaz na dokument v tomto EPICu + cross-link do EPIC-020 (quality gates).  
- Validováno linterem (např. markdown lint) a kontrolováno v CI.

### AIDE-003: Codex/Copilot Setup & Bot Accounts
**Jako** vývojář  
**chci** mít jednotnou konfiguraci pro AI nástroje  
**aby** běžely konzistentně a bezpečně.

**Acceptance Criteria**
- Definované bot účty (GitHub, CI, backlog) → AI nikdy necommituje pod osobním účtem.  
- Bot účty nemají právo pushnout do `main` / `prod`; pouze PR z feature branch/fork.  
- Přístup k repu omezen na nutné scope (read repo, PR create, status update).  
- Konfigurace sdílena (např. `.vscode/settings.json`, `config.toml`, CLI scripts) a popsána v repo docs.

### AIDE-004: AI Implementation Agent – DevTask → PR
**Jako** delivery tým  
**chci**, aby AI agent z DevTasku vytvořil PR s kódem a testy  
**aby** se snížila doba implementace rutin.

**Acceptance Criteria**
- Workflow (GitHub Action / n8n / CLI) načte DevTask (ID, popis, DoD, flag) a připraví sandbox branch.  
- AI PR vždy obsahuje odkaz na DevTask/US, popis řešení, seznam změněných souborů a přidané testy.  
- Každý PR musí projít lint + unit testy + minimální code coverage pro novou logiku.  
- Pokud lint/test spadne, agent provede max 2 auto-fix pokusy; při dalším failu přidá komentář a označí DevTask jako „Needs human“.  
- Žádný auto-merge; PR čeká na lidské schválení.

### AIDE-005: AI Code Review & Quality Gate
**Jako** reviewer  
**chci**, aby AI připravila review a spustila testy  
**aby** review bylo rychlejší a konzistentní.

**Acceptance Criteria**
- AI reviewer workflow (Action/n8n) se spustí při otevřeném PR, spustí testy a analyzuje diff.  
- Výstup = strukturované review komentáře (security, breaking changes, performance, logging).  
- AI reviewer nikdy PR nemerguje ani nemaže labely.  
- Povinné quality gates: ✅ lint, ✅ testy, ✅ AI review, ✅ human approval (min. 1 člověk; 2 pro kritické části).  
- Výsledek review logován (viz AIDE-007).

### AIDE-006: Security & Governance Guardrails
**Jako** Security / Tech Lead  
**chci** mít daná pravidla pro AI agenty  
**aby** nedocházelo k leakům ani obcházení procesů.

**Acceptance Criteria**
- Dokument „AI Security & Governance Policy“ popisuje bot identity, role (GitHub, CI, backlog), secret management a proces rotace tokenů.  
- Bot tokeny mají omezený scope, expiraci a jsou uložené v trezoru (Vault, 1Password).  
- AI agenty nemají přístup k produkčním secrets/DB.  
- Policy odkazuje na EPIC-020 (quality gates) a je vyžadována v security checklistu při onboarding AI do projektu.

### AIDE-007: Audit & Logging of AI Actions
**Jako** Owner/Lead  
**chci** mít audit trail AI aktivit  
**aby** bylo jasné, kdo/co/na základě čeho změnil.

**Acceptance Criteria**
- Logujeme: který agent vzal DevTask (čas, ID), shrnutí promptů (sanitized), generované diffy, výsledky testů, odkazy na PR.  
- Logy dostupné v projektu (repo `logs/ai/`, Loki stack, případně S3).  
- Možnost dohledat historii pro konkrétní DevTask/PR (CLI/API).  
- Retence logů min. 90 dní, chráněné proti editaci.  
- Napojení na KPI sběr (AIDE-010).

### AIDE-008: Task Intake & Backlog Integration
**Jako** AI Orchestrator  
**chci**, aby AI agent bral DevTasky pouze z oficiálního backlogu  
**aby** byl řízený tok práce a nedocházelo k duplikacím.

**Acceptance Criteria**
- Integrace (API/connector) na backlog nástroj (Git-based stories, Jira/YouTrack/GitHub Issues – definováno per projekt).  
- AI vybírá pouze DevTasky se statusem Ready a flagem `AI-ELIGIBLE`.  
- Po claimu se DevTask označí (In Progress, assigned agent) → žádný double-claim.  
- Po vytvoření PR se status updatne (In Review), po merge (Done).  
- Pokud DevTask není k dispozici, agent čeká (no busy loop).  

### AIDE-009: Human Override & Escalation
**Jako** Tech Lead  
**chci** vědět, kdy AI narazila na blokér a jak eskaluje  
**aby** se proces nezacyklil.

**Acceptance Criteria**
- Definovaná kritéria STOP: chybějící specifikace, závislost bez přístupu, test fail > N pokusů, bezpečnostní riziko.  
- V případě STOP AI vytvoří komentář / doplňující DevTask se soupisem otázek/logů.  
- Notifikace pro vlastníka DevTasku (Slack/Email/GitHub).  
- Human rozhodne o dalším postupu (upravit zadání, převzít ručně, odložit).  
- Eskalace logována (navazuje na AIDE-007).

### AIDE-010: Telemetry & KPI Dashboard
**Jako** Sponsor / Lead  
**chci** mít metriky a dashboard  
**aby** bylo možné posoudit přínosy pilotu.

**Acceptance Criteria**
- Sledované metriky: lead time (pre vs post), počet revizí PR od AI, bug rate AI PR, počet eskalací na člověka, odhad ušetřeného času.  
- Dashboard (Grafana / Metabase) čte data z audit logů + backlogu.  
- KPI dostupné pro obě projekty, filtr podle repo nebo sprintu.  
- Report po pilotu (PDF/markdown) s interpretací dat a doporučení pro rollout.

### AIDE-011: Sandbox / Test Environment for AI
**Jako** DevOps / Lead  
**chci**, aby AI pracovala jen ve sandboxu  
**aby** neovlivnila produkci.

**Acceptance Criteria**
- AI pracuje na fork/feature branch, používá test databáze nebo mock services.  
- Script/infra (Makefile, Terraform, n8n) pro rychlé vytvoření sandboxu, reuse existujících nástrojů core-platform.  
- Žádné odkazy na prod endpointy v AI kontextu; secrets = fake / scoped na test.  
- Sandbox cleanup automatizovaný (po merge/abort).  
- Kontrolní checklist v CI (fail, pokud AI PR obsahuje prod config).

### AIDE-012: Multi-Agent Coordination & Task Claim
**Jako** Orchestrator  
**chci**, aby více agentů nekolidovalo na stejném DevTasku  
**aby** nevznikaly konflikty a race conditions.

**Acceptance Criteria**
- Mechanismus claim (API flag, lock soubor, Git branch naming) → když agent začne práci, označí DevTask.  
- Pokud je DevTask „claimed“, další agent ho nebere; po timeoutu se claim uvolní.  
- Evidence claimů v audit logu + backlog tool (assigned agent).  
- Konflikty řeší orchestrátor nebo člověk (manual override).  
- Ošetřený use-case: agent spadne → claim se po T minutách uvolní.

### AIDE-013: Noise Control & Quality Filters
**Jako** Reviewer  
**chci**, aby AI nevytvářela nekvalitní PR  
**aby** review nezabralo víc času než ruční práce.

**Acceptance Criteria**
- Minimální threshold: PR musí projít lint + testy + guidelines check → jinak se nevytvoří (agent opraví nebo vrátí DevTask).  
- PR obsahuje pouze související změny (no churn), auto-check na velikost diffu a nepotřebné soubory.  
- AI přikládá shrnutí a self-review checklist (co implementovala, jak testovala).  
- Pokud AI není schopná kvalitu doručit, DevTask se vrátí s komentářem (bez PR).  
- Statistiky noise vs accepted PR navazují na KPI dashboard (AIDE-010).

---

## 🚀 Pilot & Rollout Plan

1. **Pilot (core-platform):** vybrat 5–8 AI-eligible DevTasků ze sprint backlogu, spustit plný proces, sledovat KPI.  
2. **Retrospektiva & úpravy:** vyhodnocení guardrails, noise a lidské zkušenosti.  
3. **Rollout na isp-migration-tool:** re-use šablon, updatovat AGENTS.md, nastavit sandbox (repo fork), přidat KPI do dashboardu.  
4. **Long-term:** možnost přidat další agenty (např. AI QA bot, AI infra bot) a navázat na EPIC-016 (AI tools) + EPIC-020 (quality gates).

---

## 📚 Linked Artefacts

- `CONTRIBUTING.md` + `BACKLOG_GUIDE.md` (AI-ready template, DoR/DoD)  
- `AGENTS.md` (per repo)  
- `AI Security & Governance Policy` (tbd v `/docs/security/`)  
- `AI Delivery Runbook` (workflow diagrams, troubleshooting)  
- KPI dashboard link (Grafana/Metabase) – po AIDE-010  

---

**Next Steps:** Prioritizovat Phase 0 stories (AIDE-001 → AIDE-003) v následujícím sprintu, připravit security review pro AIDE-006/011, a definovat pilot backlog.

