# TASK-019-03: Multi-tenant mode + user management

## Goal
Zapnout multi-tenant rezim n8n a zajistit per-tenant uzivatele s mapovanim roli z Keycloak realm.

## Tasks
- [ ] Zapnout multi-tenant rezim v n8n (`N8N_MULTI_TENANT_ENABLED`, `N8N_TENANT_MODE`).
- [ ] Definovat tenant registry (tabulka nebo config) pro mapovani subdomen na tenanty.
- [ ] Nastavit provisioning uzivatelu v n8n na zaklade Keycloak realm roli.
- [ ] Osetrit izolaci tenantu (uzivatele nevidi workflows ani data jinych tenantu).
- [ ] Doplnit kontrolu tenant kontextu pri volani webhooku/API.

## Output
- Multi-tenant n8n s oddelenymi workspaces/uzivateli.
- Mapovani roli z Keycloak realm na n8n role.

## Acceptance Criteria for This Subtask
- [ ] Pro kazdy tenant existuje samostatny n8n uzivatel/workspace.
- [ ] Role z Keycloak realm jsou mapovane na n8n role.
- [ ] Uzivatel tenant-A nema pristup k workflow a credentialum tenant-B.
- [ ] N8N API/webhooky respektuji tenant kontext.
