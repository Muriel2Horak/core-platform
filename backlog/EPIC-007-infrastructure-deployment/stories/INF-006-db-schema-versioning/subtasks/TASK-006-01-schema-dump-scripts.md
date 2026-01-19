# TASK-006-01: Schema dump scripts + targets

## Goal
Ulozit databazove schemata do Gitu a zajistit jejich pravidelny dump.

## Tasks
- [ ] Vytvorit `db/schema/` strukturu pro core/keycloak/grafana.
- [ ] Implementovat `scripts/db/dump-schema.sh` (schema-only).
- [ ] Pridat Makefile targety `db-schema-dump` a `db-schema-update`.

## Output
- Schemata DB v Gitu a skript pro aktualizaci.

## Acceptance Criteria for This Subtask
- [ ] Script generuje pouze strukturu (bez dat).
- [ ] Schemata pro core/keycloak/grafana jsou v `db/schema/`.
- [ ] Makefile targety spusti dump bez chyb.
