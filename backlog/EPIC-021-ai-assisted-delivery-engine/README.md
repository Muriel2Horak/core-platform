# EPIC-021: AI-Assisted Delivery Engine (Codex + Copilot)

**Status:** ⚪ **0% – PLANNED** (Foundations in backlog)  
**Definice:** ✅ **100%** (AIDE-001..AIDE-013 specifikováno s AC + tasky)  
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

## 📦 Scope (core-platform + isp-migration-tool)

### In Scope ✅
- DevTask template + DoR/DoD guidelines s AI-ELIGIBLE flagem.  
- AGENTS.md + engineering standards (coding, logging, testing, DB changes, errors).  
- Sdílený setup Codex/Copilot + CI integrace s bot účty.  
- Automovaný flow DevTask → branch → AI implementace → PR → AI review → human review.  
- **MVP backlog integrace:** Jira Cloud (core-platform), reused pro isp-migration-tool po pilotu.  
- Sandbox prostředí (repo fork, oddělené secrets, test DB, mock services).  
- Security & governance policy, audit log stream, KPI dashboard.  
- Multi-agent coordination a noise-control filtry.

**Dual rollout:** core-platform slouží jako pilot (Phases 0–3). Jakmile KPI splní cíle, stejné šablony/orchestrátor/CI konfigurace se aplikují na `isp-migration-tool` (minimálně AGENTS.md, DevTask template, bot setup a orchestrátor napojený na jejich Jira projekt).

### Out of Scope ❌
- Full autopilot merge do mainu (human approval je mandatory).  
- Refaktorování kompletních modulů (pilot řeší menší/mid DevTasky).  
- Přístup AI do produkčních DB/secrets.  
- Nahrazení produktového rozhodování (AI nevytváří backlog sama).  

---

## 🔍 GAP analýza (Current vs Target)

| Oblast | Story | Gap / Riziko |
| --- | --- | --- |
| DevTask standard | AIDE-001 | Bez AI-ready template nelze bezpečně automatizovat |
| AGENTS & standards | AIDE-002 | Chybí jednotné instrukce pro agenty |
| Bot setup | AIDE-003 | Nejsou identity a least-privilege účty |
| AI implementation loop | AIDE-004 | Není automatické DevTask → PR flow |
| AI review gates | AIDE-005 | Bez AI review je vyšší noise a riziko regresí |
| Security guardrails | AIDE-006 | Chybí sandbox politika a governance |
| Audit trail | AIDE-007 | Neexistuje audit a evidence akcí |
| Backlog intake | AIDE-008 | Chybí automatický claim a sync se statusem |
| Human override | AIDE-009 | Není definovaný stop/escalation flow |
| KPI dashboard | AIDE-010 | Chybí měřitelnost pilotu |
| Sandbox env | AIDE-011 | Bez izolace není bezpečný provoz AI |
| Multi-agent coordination | AIDE-012 | Riziko kolizí a dvojí práce |
| Noise control | AIDE-013 | Bez filtrů hrozí spam a nekvalitní PR |

## 🧩 DEV tasky (PENDING) - popis a scope

| DEV task | Popis (high-level) | Výstup |
| --- | --- | --- |
| AIDE-001: DevTask Template | Šablona + validator | AI-ready backlog |
| AIDE-002: AGENTS.md | Engineering standards | Konzistentní agent output |
| AIDE-003: Bot Setup | Bot identity + config | Bezpečný access |
| AIDE-004: DevTask → PR | Implementační loop | AI PR pipeline |
| AIDE-005: AI Review | Review workflow + gates | AI review gate |
| AIDE-006: Guardrails | Security policy | Governance |
| AIDE-007: Audit Log | Audit stream | Evidence |
| AIDE-008: Intake | Jira sync + claim | Backlog intake |
| AIDE-009: Escalation | Human override | STOP flow |
| AIDE-010: KPI Dashboard | Telemetry + Grafana | KPI visibility |
| AIDE-011: Sandbox | Test env automation | Izolované prostředí |
| AIDE-012: Multi-agent | Coordination + locks | Bez kolizí |
| AIDE-013: Noise Control | Filtry + heuristiky | Kvalita výstupů |

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

### Orchestrátor (Phase 2 MVP)
- Implementace: jednoduchá služba (cronovaný Python skript, nebo n8n flow, případně GitHub Action na schedule).  
- Kroky:  
  1. Dotaz na Jira → najdi DevTasky `status=Ready` + `AI-ELIGIBLE = Yes`.  
  2. Ověř, že nejsou In Progress (claim field empty).  
  3. Označ DevTask jako „Claimed by <agent>“ (status change + field).  
  4. Spusť workflow (AIDE-004) → vytvoř branch, update audit log.  
- Orchestrátor loguje každou akci do `logs/ai/` a může být spuštěn ručně (CLI) pro urgentní úkoly.

---

## 📅 Phases & Milestones

| Phase | Focus | Stories | Poznámky |
|-------|-------|---------|----------|
| **Phase 0 – Scope & Decisions** | Zafixovat backlog/CI/repo scope | AIDE-000 (1 pager) + AIDE-001 → AIDE-003 | Jira + GitHub Actions + core-platform/isp scope |
| **Phase 1 – Governance Foundations (MVP)** | Šablony, standardy, security, human override | AIDE-001, AIDE-002, AIDE-006, AIDE-009 | AI ještě nic nenasazuje |
| **Phase 2 – Orchestrátor & Integrace** | Task intake, claim, audit log | AIDE-008, AIDE-012, AIDE-007 | Jednoduchý orchestrátor (cron/n8n/python) |
| **Phase 3 – AI Implementation Loop (Pilot)** | DevTask → PR, AI review, sandbox, KPI | AIDE-004, AIDE-005, AIDE-011, AIDE-013, AIDE-010 | GitHub Actions CI, telemetry |
| **Phase 4 – Hardening & Scale** | Threat model, enterprise orchestrátor, multi-agent specializace | follow-up (tbd) | Rollout do obou projektů + advanced features |

Target pilot: core-platform sprint (2 týdny). Po vyhodnocení KPI se pattern replikuje do `isp-migration-tool`.

### Phase 0 – Scope & Assumptions (AIDE-000)
- Backlog systém = **Jira Cloud** (core-platform board).  
- CI/CD stack = **GitHub Actions** + branch policies, sdílené i pro isp-migration-tool.  
- AI agenti pracují výhradně na GitHub feature branches s test DB/mocks.  
- Výstup = 1-stránkový dokument „AI Delivery – Scope & Assumptions“ popisující systémy, owners orchestrátoru, security review sloty a očekávání pilotu.

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
- Šablona DevTasku obsahuje sekce: Kontext (EPIC/US), Scope, Constraints, Dependencies, DoD, odkazy na reference a flag `AI-ELIGIBLE: Yes/No`.  
- DevTask s `AI-ELIGIBLE: Yes` musí mít vyplněný kontext, DoD, testovací odkazy a sandbox požadavky; jinak validátor zakáže automatické vyzvednutí.  
- DoR checklist doplněn o AI readiness (flag, odkazy, test strategy).  
- Template publikována v `CONTRIBUTING.md` a `BACKLOG_GUIDE.md`, sdíleno v obou projektech (core-platform + isp-migration-tool).


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Definovat DevTask template + AI-ELIGIBLE flag | 0.5d | none |
| 2 | DoR/DoD checklist + examples | 0.5d | 1 |
| 3 | Validator (CI/pre-commit) pro AI-ELIGIBLE kompletnost | 0.5d | 1 |
| 4 | Sync do isp-migration-tool | 0.25d | 1 |

### AIDE-002: AGENTS.md & Engineering Standards
**Jako** AI Agent Owner  
**chci** mít v každém repu AGENTS.md + guidelines  
**aby** Codex/Copilot psali kód/testy/logy konzistentně a bezpečně.

**Acceptance Criteria**
- `AGENTS.md` obsahuje: architekturu, code style, logging conventions, jak psát testy (JUnit/PyTest/Playwright), jak zachytávat chyby a fallback scénáře, pravidla pro DB změny.  
- Sekce **Security & Sensitive Data** popisuje práci se secrets, prompt hygiene (neposílat citlivá data), konfigurace sandboxu a chyby.  
- Odkaz na tento EPIC + EPIC-020 (quality gates) + instrukce pro core-platform i isp-migration-tool.  
- Validováno CI (markdown lint) a distribuováno v obou repozitářích.


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Draft AGENTS.md base template | 0.5d | none |
| 2 | Security & sensitive data sekce | 0.25d | 1 |
| 3 | Repo-specific addendum + odkazy na EPIC-020 | 0.25d | 1 |
| 4 | Markdown lint/CI check | 0.25d | 1 |

### AIDE-003: Codex/Copilot Setup & Bot Accounts
**Jako** vývojář  
**chci** mít jednotnou konfiguraci pro AI nástroje  
**aby** běžely konzistentně a bezpečně.

**Acceptance Criteria**
- Definované bot účty (GitHub, Jira, CI) → AI nikdy necommituje pod osobním účtem.  
- Bot účty nemají právo pushnout do `main` / `prod`; pouze PR z feature branch/fork, CI token má jen status/report scope.  
- Přístup k repu omezen na nutné scope (read repo, PR create, status update).  
- Konfigurace sdílena (např. `.vscode/settings.json`, `config.toml`, CLI scripts) a popsána v repo docs.


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Zalozit bot ucty + least-privilege scope | 0.5d | none |
| 2 | Ulozit tokeny v Vault/CI secrets | 0.25d | 1 |
| 3 | Konfigurace tooling (config files, CLI) | 0.5d | 1 |
| 4 | Rotace a access policy dokumentace | 0.25d | 2 |

### AIDE-004: AI Implementation Agent – DevTask → PR
**Jako** delivery tým  
**chci**, aby AI agent z DevTasku vytvořil PR s kódem a testy  
**aby** se snížila doba implementace rutin.

**Acceptance Criteria**
- Workflow (GitHub Action / n8n / CLI) načte DevTask (ID, popis, DoD, flag) a připraví sandbox branch.  
- AI PR vždy obsahuje odkaz na DevTask/US, popis řešení, seznam změněných souborů a přidané testy.  
- Každý PR musí projít lint + unit testy + minimální code coverage pro novou logiku.  
- Pokud lint/test spadne, agent provede max 2 auto-fix pokusy; při dalším failu přidá komentář a označí DevTask jako „Needs human“.  
- Sandbox = feature branch + test DB/mock služby; CI = GitHub Actions pipeline (lint/test).  
- Žádný auto-merge; PR čeká na lidské schválení.


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Workflow: claim DevTask + create branch | 0.5d | AIDE-001..003 |
| 2 | Agent runner: implementace + test run | 1d | 1 |
| 3 | PR template + evidence (tests, diffs) | 0.25d | 2 |
| 4 | Auto-fix retry policy + escalation | 0.25d | 2 |
| 5 | CI integration (lint/test) | 0.25d | 2 |

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


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | AI review workflow (diff analysis) | 0.5d | AIDE-004 |
| 2 | Quality gate enforcement (lint/test/AI/human) | 0.5d | 1 |
| 3 | Structured review comments + summary | 0.25d | 1 |
| 4 | Audit log integration | 0.25d | 1 |

### AIDE-006: Security & Governance Guardrails
**Jako** Security / Tech Lead  
**chci** mít daná pravidla pro AI agenty  
**aby** nedocházelo k leakům ani obcházení procesů.

**Acceptance Criteria**
- Dokument „AI Security & Governance Policy“ popisuje bot identity, role (GitHub, CI, backlog), secret management a proces rotace tokenů.  
- Bot tokeny mají omezený scope, expiraci a jsou uložené v trezoru (Vault, 1Password).  
- AI agenty nemají přístup k produkčním secrets/DB.  
- Policy odkazuje na EPIC-020 (quality gates), obsahuje prompt hygiene pravidla (maskování secrets) a je vyžadována v security checklistu při onboarding AI do projektu.


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Define guardrails policy + threat model | 0.5d | none |
| 2 | Sandbox enforcement (env allowlist, no prod) | 0.5d | 1 |
| 3 | Bot identity checks + permissions | 0.25d | 1 |
| 4 | Security review checklist | 0.25d | 1 |

### AIDE-007: Audit & Logging of AI Actions
**Jako** Owner/Lead  
**chci** mít audit trail AI aktivit  
**aby** bylo jasné, kdo/co/na základě čeho změnil.

**Acceptance Criteria**
- MVP logging: JSON lines (`logs/ai/ai-delivery-log.jsonl`) s možností forwardu do Loki/ELK + Git history.  
- Logujeme: který agent vzal DevTask (čas, ID), shrnutí promptů (sanitized), generované diffy, výsledky testů, odkazy na PR.  
- Možnost dohledat historii pro konkrétní DevTask/PR (CLI/API).  
- Retence logů min. 90 dní; export do central log stacku.  
- Napojení na KPI sběr (AIDE-010).


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Audit schema + storage target | 0.5d | AIDE-006 |
| 2 | Emit events for claim/implement/test/PR/review | 0.5d | 1 |
| 3 | Ship to Loki + Grafana dashboard | 0.5d | 1 |
| 4 | Retention + access policy | 0.25d | 1 |

### AIDE-008: Task Intake & Backlog Integration
**Jako** AI Orchestrator  
**chci**, aby AI agent bral DevTasky pouze z oficiálního backlogu  
**aby** byl řízený tok práce a nedocházelo k duplikacím.

**Acceptance Criteria**
- MVP integrace na **Jira Cloud** (REST API / Webhook) – definovaný board, label `AI-ELIGIBLE`.  
- AI vybírá pouze DevTasky se statusem Ready a flagem `AI-ELIGIBLE`.  
- Po claimu se DevTask označí (custom pole `AI-CLAIMED BY`, status In Progress) → žádný double-claim.  
- Po vytvoření PR se status updatne (In Review), po merge (Done).  
- Pokud DevTask není k dispozici, orchestrátor čeká (cron/n8n/python běží 1×/30 min a lze ho ručně spustit).


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Jira intake query + filters | 0.5d | AIDE-001 |
| 2 | Claim/lock update (status + field) | 0.5d | 1 |
| 3 | Sync status updates back to backlog | 0.25d | 2 |
| 4 | Dry-run + error handling | 0.25d | 1 |

### AIDE-009: Human Override & Escalation
**Jako** Tech Lead  
**chci** vědět, kdy AI narazila na blokér a jak eskaluje  
**aby** se proces nezacyklil.

**Acceptance Criteria**
- Definovaná kritéria STOP: chybějící specifikace, závislost bez přístupu, test fail > N pokusů, bezpečnostní riziko.  
- V případě STOP AI vytvoří Jira sub-task `AI-NEEDS-INPUT` + komentář v DevTasku se soupisem otázek/logů.  
- Notifikace pro vlastníka DevTasku (Slack/Email/GitHub).  
- Human rozhodne o dalším postupu (upravit zadání, převzít ručně, odložit).  
- Eskalace logována (navazuje na AIDE-007).


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Define escalation triggers | 0.25d | AIDE-004 |
| 2 | Status transition + Needs human | 0.25d | 1 |
| 3 | Notifications (Slack/Email) | 0.25d | 1 |
| 4 | Runbook + examples | 0.25d | 1 |

### AIDE-010: Telemetry & KPI Dashboard
**Jako** Sponsor / Lead  
**chci** mít metriky a dashboard  
**aby** bylo možné posoudit přínosy pilotu.

**Acceptance Criteria**
- Sledované metriky: lead time (pre vs post), počet revizí PR od AI, bug rate AI PR, počet eskalací na člověka, odhad ušetřeného času.  
- Dashboard (Grafana / Metabase) čte data z audit logů + Jira/GitHub (issue timestamps, PR events).  
- KPI dostupné pro obě projekty, filtr podle repo nebo sprintu.  
- Report po pilotu (PDF/markdown) s interpretací dat a doporučení pro rollout.


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | KPI schema + data sources | 0.5d | AIDE-007 |
| 2 | Collector from PR/CI events | 0.5d | 1 |
| 3 | Grafana dashboard | 0.5d | 2 |
| 4 | Baseline vs target reporting | 0.25d | 2 |

### AIDE-011: Sandbox / Test Environment for AI
**Jako** DevOps / Lead  
**chci**, aby AI pracovala jen ve sandboxu  
**aby** neovlivnila produkci.

**Acceptance Criteria**
- AI pracuje na fork/feature branch, používá test databáze nebo mock services (GitHub Actions + docker compose).  
- Script/infra (Makefile, Terraform, n8n) pro rychlé vytvoření sandboxu, reuse existujících nástrojů core-platform.  
- Žádné odkazy na prod endpointy v AI kontextu; secrets = fake / scoped na test.  
- Sandbox cleanup automatizovaný (po merge/abort).  
- Kontrolní checklist v CI (fail, pokud AI PR obsahuje prod config).


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Provision sandbox env (DB/mocks) | 0.5d | EPIC-007 |
| 2 | Reset/teardown automation | 0.5d | 1 |
| 3 | Secrets isolation + mock creds | 0.25d | 1 |
| 4 | Sandbox smoke tests | 0.25d | 1 |

### AIDE-012: Multi-Agent Coordination & Task Claim
**Jako** Orchestrator  
**chci**, aby více agentů nekolidovalo na stejném DevTasku  
**aby** nevznikaly konflikty a race conditions.

**Acceptance Criteria**
- Mechanismus claim přes Jira (custom field + status) + audit log + branch naming → když agent začne práci, označí DevTask.  
- Pokud je DevTask „claimed“, další agent ho nebere; po timeoutu se claim uvolní.  
- Evidence claimů v audit logu + backlog tool (assigned agent).  
- Konflikty řeší orchestrátor nebo člověk (manual override).  
- Ošetřený use-case: agent spadne → claim se po T minutách uvolní (cron unclaim).


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Agent registry + heartbeat | 0.5d | AIDE-008 |
| 2 | Task claim lock (Jira field/DB) | 0.5d | 1 |
| 3 | Conflict resolution policy | 0.25d | 1 |
| 4 | Throughput metrics | 0.25d | 1 |

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


#### Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Quality filters (min AC/tests) | 0.5d | AIDE-004 |
| 2 | Noise suppression rules for PR comments | 0.25d | 1 |
| 3 | Risky pattern detection allow/deny list | 0.25d | 1 |
| 4 | Feedback loop tuning | 0.25d | 2 |

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

**Next Steps:** Dodělat AIDE-000 (Scope & Assumptions), následně spustit Phase 1 stories (AIDE-001, AIDE-002, AIDE-006, AIDE-009), připravit security review pro AIDE-006/011 a připravit pilotní Jira backlog.
