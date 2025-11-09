# EPIC-010: Agile Work Management - Part 2/3

**Boards, Backlog, DMS & Reporting** (Themes 3-5, Stories AWM7-AWM13)

---

## Theme 3: Boards & Backlog

### AWM7: Backlog View

**As a** user  
**I want** backlog view s filtrováním a drag&drop  
**So that** můžu organizovat a prioritizovat práci

**Acceptance Criteria:**

✅ **Filtrovaný seznam work items:**
- Filter podle:
  - Project (multi-select dropdown)
  - Type (Epic, Story, Task, Bug)
  - Labels (tag cloud, multi-select)
  - Assignee (user picker)
  - Status (workflow states)
  - Sprint (current, next, backlog, specific sprint)
- Kombinace filtrů (AND logic)
- Save filter as "My View" (per-user preference)

✅ **Drag & Drop prioritizace:**
- Vertical list, draggable items
- Drag to reorder → updates `priority_order` field
- Visual feedback (drag handle, drop zone highlighting)

✅ **Grouping:**
- Group by: Epic, Assignee, Type, None
- Collapsible groups
- Group headers show count + story points sum

✅ **Bulk actions:**
- Multi-select (checkbox)
- Bulk assign, bulk label, bulk transition

✅ **Infinite scroll / Pagination:**
- Load 50 items initially, infinite scroll for more
- Performance: index on priority_order + tenant_id

**BE Impact:**
- Priority ordering field (`priority_order` integer, tenant-scoped)
- Bulk update API (POST `/api/v1/work-items/bulk`)
- Filter query builder (dynamic WHERE clause)

**FE Impact:**
- Backlog page component
- React DnD (drag & drop library)
- Filter bar (multi-select dropdowns)
- Infinite scroll (Intersection Observer API)

**Security/Tenancy:**
- Backlog items filtered by tenant_id
- RBAC: user must have `read` permission on work items
- Bulk actions require `edit` permission

**Test Coverage:**
- Unit: Priority order calculation, filter query building
- Integration: Bulk update API, drag-drop order persistence
- E2E: Filter backlog → drag items → verify order saved

**LOC:** ~600 (BE: ~200, FE: ~350, tests: ~50)  
**Effort:** 14h  
**Priority:** MUST HAVE (Phase 2)

---

### AWM8: Board (Kanban/Scrum)

**As a** user  
**I want** board view s drag&drop mezi sloupci  
**So that** můžu vizuálně řídit workflow

**Acceptance Criteria:**

✅ **Konfigurovatelné sloupce:**
- Sloupce mapované na workflow states
- Admin může přidat/odebrat/přejmenovat sloupce
- Každý sloupec = filter na status (např. "In Progress" sloupec = všechny items ve stavu "In Progress")

✅ **Swimlanes:**
- Horizontal grouping: Epic, Assignee, Priority, None
- Collapsible lanes
- Lane headers show count + story points

✅ **Drag & Drop workflow transition:**
- Drag work item mezi sloupci → automaticky spustí workflow transition
- Guard validation before drop (pokud guard fails → drop disabled + tooltip proč)
- Visual feedback (column highlighting, disabled states)

✅ **WIP (Work In Progress) limit:**
- Konfigurovat max items per column
- Pokud WIP překročen → visual warning (red border, counter badge)
- Optional: hard limit (disable drop pokud WIP exceeded)

✅ **Card design:**
- Compact card: ID, summary, assignee avatar, type icon, priority badge
- Hover → expand tooltip (description preview, labels, due date)
- Click → open detail modal (nebo navigate to detail page)

✅ **Quick actions na card:**
- Assign to me
- Add label
- Set priority
- Open detail

**BE Impact:**
- Board configuration entity (columns, swimlanes, WIP limits)
- Board state API (GET `/api/v1/boards/{id}/state` - všechny cards s positions)
- Transition validation API (check guard before drag-drop)

**FE Impact:**
- Board page component
- React DnD (multi-column drag-drop)
- Swimlane layout engine
- WIP limit counter component
- Card component (compact + hover tooltip)

**Security/Tenancy:**
- Board config tenant-scoped
- User must have `transition` permission to drag cards
- Cards filtered by board project scope

**Test Coverage:**
- Unit: WIP limit calculation, guard validation
- Integration: Board state API, transition via drag-drop
- E2E: Drag card → verify transition executed, WIP warning shown

**LOC:** ~800 (BE: ~250, FE: ~500, tests: ~50)  
**Effort:** 18h  
**Priority:** MUST HAVE (Phase 2)

---

### AWM9: Sprints

**As a** scrum master  
**I want** sprint management (planning, active, completion)  
**So that** můžu řídit scrum proces

**Acceptance Criteria:**

✅ **Sprint entity:**
- Fields: name, goal (text), start_date, end_date, status (planning/active/completed)
- One active sprint per board max
- Sprint backlog = subset of work items assigned to sprint

✅ **Sprint planning:**
- Drag items z backlog do sprint
- Estimate capacity (total story points)
- Visual indicator pokud capacity exceeded (warning, not hard block)

✅ **Start sprint:**
- Button "Start Sprint" → status = active, start_date = now
- Only one active sprint per board
- Cannot start pokud previous sprint not completed

✅ **Active sprint view:**
- Same as board, but filtered to sprint items only
- Burndown chart (story points remaining over days)
- Progress bar (completed / total)

✅ **Complete sprint:**
- Button "Complete Sprint" → status = completed, end_date = now
- Unfinished items → auto-move to backlog nebo next sprint (user choice)
- Sprint report: velocity, completed vs planned, carryover items

✅ **Sprint history:**
- List of past sprints
- Click → view sprint report (burndown, velocity, team notes)

**BE Impact:**
- Sprint entity (CRUD API)
- Sprint assignment (work_item.sprint_id foreign key)
- Burndown calculation API (GET `/api/v1/sprints/{id}/burndown`)
- Sprint completion logic (handle unfinished items)

**FE Impact:**
- Sprint planning page (drag-drop backlog → sprint)
- Active sprint board (filtered view)
- Burndown chart component (Chart.js or Recharts)
- Sprint completion dialog (handle unfinished items)
- Sprint history page

**Security/Tenancy:**
- Sprints tenant-scoped
- Only `AWM_ADMIN` or `AWM_SCRUM_MASTER` role can start/complete sprint
- Regular users can add items to sprint during planning

**Test Coverage:**
- Unit: Burndown calculation, capacity validation
- Integration: Sprint CRUD, completion logic
- E2E: Plan sprint → start → move cards → complete → verify report

**LOC:** ~700 (BE: ~300, FE: ~350, tests: ~50)  
**Effort:** 16h  
**Priority:** SHOULD HAVE (Phase 2)

---

## Theme 4: DMS & Komentáře

### AWM10: Propojení s DMS

**As a** user  
**I want** přiložit dokumenty k work item  
**So that** mám vše na jednom místě (specs, screenshoty, logs)

**Acceptance Criteria:**

✅ **Tab "Files" v detailu work item:**
- Seznam přiložených dokumentů
- Upload button → file picker dialog
- Drag & drop upload area

✅ **Document metadata:**
- Filename, size, upload date, uploader
- Document type (spec, screenshot, log, other)
- Version history (pokud DMS podporuje)

✅ **Preview:**
- Images → thumbnail + lightbox
- PDFs → inline preview (PDF.js)
- Text files → syntax highlighting
- Other → download link

✅ **Permissions:**
- Work item assignee + reporter can upload
- `AWM_ADMIN` can delete attachments
- Read permissions inherited from work item visibility

**BE Impact:**
- Reuse EPIC-008 DMS API
- Link table: `work_item_documents` (work_item_id, document_id)
- Document upload proxy (POST `/api/v1/work-items/{id}/attachments`)

**FE Impact:**
- Files tab component
- Upload widget (dropzone)
- Document list with preview
- Lightbox modal (image viewer)

**Security/Tenancy:**
- Documents tenant-scoped (via work item tenant_id)
- RBAC: upload requires `edit` on work item

**Test Coverage:**
- Unit: Document link creation
- Integration: Upload via API, verify DMS link
- E2E: Upload file → verify visible in Files tab → download

**LOC:** ~400 (BE: ~100, FE: ~250, tests: ~50)  
**Effort:** 8h  
**Priority:** SHOULD HAVE (Phase 2)  
**Dependency:** EPIC-008 DMS

---

### AWM11: Komentáře & Diskuze

**As a** user  
**I want** komentovat work items  
**So that** můžu diskutovat řešení, klást dotazy, sdílet progress

**Acceptance Criteria:**

✅ **Comment thread:**
- Chronological list (newest first or oldest first - user preference)
- Rich-text editor (Markdown supported)
- @mention support (autocomplete users)

✅ **Comment metadata:**
- Author (user), timestamp, edit history
- "Edited" badge pokud comment modified
- Delete button (only author nebo `AWM_ADMIN`)

✅ **Notifications:**
- @mentioned users → email/in-app notification
- Work item watchers → notify on new comment
- Optional: integrate s EPIC-003 notifications (pokud existuje)

✅ **Comment types:**
- General comment (default)
- Work log (time tracking - log hours spent)
- Internal note (visible only to team, hidden from customers - pokud external access)

**BE Impact:**
- Comment entity (work_item_id, author_id, content, type, created_at, updated_at)
- Comment CRUD API (POST/GET/PUT/DELETE `/api/v1/work-items/{id}/comments`)
- @mention parser (extract usernames → send notifications)

**FE Impact:**
- Comment thread component
- Rich-text editor (TipTap or similar)
- @mention autocomplete (user search)
- Notification integration (toast/badge)

**Security/Tenancy:**
- Comments tenant-scoped
- RBAC: read comments requires `read` on work item, add comment requires `comment` permission
- Internal notes visible only to users with `AWM_INTERNAL` role

**Test Coverage:**
- Unit: @mention parsing, notification triggering
- Integration: Comment CRUD, edit history
- E2E: Add comment → @mention user → verify notification sent

**LOC:** ~500 (BE: ~200, FE: ~250, tests: ~50)  
**Effort:** 10h  
**Priority:** SHOULD HAVE (Phase 2)

---

## Theme 5: Reporting & Monitoring

### AWM12: Základní přehledy & reporty

**As a** manager  
**I want** vidět statistiky a trendy  
**So that** můžu sledovat team performance a bottlenecks

**Acceptance Criteria:**

✅ **Dashboard widgets:**
- **Work item count by status** (pie chart: To Do, In Progress, Done)
- **Work item count by type** (bar chart: Story, Bug, Task)
- **Work item count by assignee** (bar chart: top 10 assignees)
- **Open vs Done trend** (line chart: last 30 days)

✅ **Lead time & Cycle time:**
- **Lead time**: Created → Done (total time from open to completion)
- **Cycle time**: In Progress → Done (active work time)
- Histogram distribution (p50, p75, p95)

✅ **Velocity (for Scrum):**
- Story points completed per sprint (bar chart)
- Trend over last 5-10 sprints

✅ **Bottleneck detection:**
- Work items stuck in status > SLA threshold
- Table view: ID, summary, status, days in state, assignee

✅ **Export:**
- CSV export (work item list with filters applied)
- PDF report (dashboard snapshot)

**BE Impact:**
- Reporting API endpoints:
  - GET `/api/v1/reports/work-items/summary` (counts, breakdown)
  - GET `/api/v1/reports/work-items/lead-time` (histogram data)
  - GET `/api/v1/reports/sprints/velocity` (sprint story points)
- Query optimization (aggregations, indexes)

**FE Impact:**
- Reports page (dashboard layout)
- Chart components (Chart.js or Recharts)
- Filter bar (date range, project, type)
- Export buttons (CSV, PDF)

**Security/Tenancy:**
- Reports tenant-scoped (filtered by tenant_id)
- RBAC: `AWM_VIEWER` role minimum for reports

**Test Coverage:**
- Unit: Lead time calculation, velocity aggregation
- Integration: Reports API returns correct data
- E2E: View reports → verify charts render → export CSV

**LOC:** ~600 (BE: ~250, FE: ~300, tests: ~50)  
**Effort:** 14h  
**Priority:** SHOULD HAVE (Phase 2)

---

### AWM13: Observabilita & Metrics (Loki/Prometheus)

**As a** platform engineer  
**I want** AWM metriky v monitoring infrastruktuře  
**So that** můžu sledovat usage, performance, health AWM modulu

**Acceptance Criteria:**

✅ **Logování workflow eventů do Loki:**
- Event: work item created, transitioned, assigned, completed
- Labels: tenant_id, user_id, work_item_type, workflow_state
- Structured JSON logs (parsable)

✅ **Prometheus metrics:**
- `awm_work_items_total` (counter) - celkový počet work items per tenant
- `awm_work_items_by_status` (gauge) - count per status (To Do, In Progress, Done)
- `awm_workflow_transitions_total` (counter) - počet transitions per type
- `awm_time_in_state_seconds` (histogram) - čas strávený ve stavu (lead time, cycle time)
- `awm_api_requests_total` (counter) - API call count per endpoint

✅ **Grafana dashboards (admin-only):**
- AWM Usage dashboard:
  - Work items created per day (line chart)
  - Active users (unique users creating/transitioning items)
  - Top projects by activity
- AWM Performance dashboard:
  - API response times (p50, p95, p99)
  - Slow queries (> 1s)
  - Error rate (4xx, 5xx)

✅ **Alerting (optional):**
- Alert pokud work items stuck > 7 days in "In Progress"
- Alert pokud error rate > 5%

**BE Impact:**
- Logging middleware (log all work item operations)
- Metrics exporter (Prometheus client library)
- Instrumentation (timers, counters at API endpoints)

**FE Impact:**
- Minimal (FE doesn't export metrics directly)
- Optional: Client-side performance metrics (page load times)

**Security/Tenancy:**
- Metrics aggregated per tenant (tenant_id label)
- Dashboards accessible only to `CORE_PLATFORM_ADMIN` role (admin realm)

**Test Coverage:**
- Unit: Metrics increment logic
- Integration: Verify metrics endpoint returns data
- E2E: Execute work item operations → verify metrics updated

**LOC:** ~400 (logging: ~150, metrics: ~150, dashboards: ~50, tests: ~50)  
**Effort:** 10h  
**Priority:** SHOULD HAVE (Phase 2)  
**Dependency:** EPIC-003 Monitoring & Observability

---

## 📦 Story Overview (Part 2 - Themes 3-5)

| Story | Name | Theme | LOC | Effort | Phase | Priority |
|-------|------|-------|-----|--------|-------|----------|
| AWM7 | Backlog View | 3 | ~600 | 14h | 2 | MUST |
| AWM8 | Board (Kanban/Scrum) | 3 | ~800 | 18h | 2 | MUST |
| AWM9 | Sprints | 3 | ~700 | 16h | 2 | SHOULD |
| AWM10 | DMS Integrace | 4 | ~400 | 8h | 2 | SHOULD |
| AWM11 | Komentáře | 4 | ~500 | 10h | 2 | SHOULD |
| AWM12 | Reporty | 5 | ~600 | 14h | 2 | SHOULD |
| AWM13 | Observabilita | 5 | ~400 | 10h | 2 | SHOULD |
| **SUBTOTAL** | **Themes 3-5** | | **~4,000** | **90h** | | |

---

**Cumulative (Part 1 + Part 2):**
- **13 stories (AWM1-AWM13)**
- **~6,500 LOC**
- **~144h**

**Next: Part 3/3 (Tenancy, RBAC, Licensing, AI/MCP) - AWM14-AWM17**

---

## 🔗 Závislosti (Updated)

**Part 2 přidává závislosti:**
- EPIC-008: DMS (AWM10 - document attachments)
- EPIC-003: Monitoring (AWM13 - Grafana dashboards)

**Stále platí:**
- EPIC-005: Metamodel (entity definitions)
- EPIC-006: Workflow Engine (workflow execution)
- EPIC-000: Security & RBAC (permissions)
- EPIC-007: Multi-tenant (tenant isolation)
