# T4: Monitoring Isolation (Grafana/Loki)

## Goal
Oddelit monitoring a logy per tenant tak, aby tenanti nevideli data jinych tenantu.

## Tasks
- [ ] Nastavit Grafana org/folder per tenant a RBAC pro tenant role.
- [ ] Pridat tenant labely do Loki a upravit query/retention podle tenantu.
- [ ] Pridat tenant labely do Prometheus metrik (relabeling).
- [ ] Pridat per-tenant dashboardy nebo templating s tenant selector.
- [ ] Overit pristupy tenant vs admin.

## Output
- Monitoring data izolovana na urovni tenant labelu a Grafana RBAC.

## Acceptance Criteria for This Subtask
- [ ] Tenant uzivatel vidi jen svuj org/folder a svoje dashboardy.
- [ ] Loki query bez tenant labelu nevraci cizi logy.
- [ ] Prometheus metriky nesou tenant label.
- [ ] Admin pristup vidi vsechny tenanty.
