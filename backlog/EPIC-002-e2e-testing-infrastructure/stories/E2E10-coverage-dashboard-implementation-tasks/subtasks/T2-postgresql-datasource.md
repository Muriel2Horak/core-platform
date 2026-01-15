# T2: PostgreSQL Data Source
**Effort:** ~2h | **LOC:** ~100

## Goal
Nastavit Grafana datasource na PostgreSQL `test_registry`.

## Tasks
- [ ] Pridat datasource config do provisioning.
- [ ] Zajistit credentials z env/secrets.
- [ ] Otestovat spojeni v Grafana UI.

## Output
- `docker/grafana/provisioning/datasources/postgres.yml` s test registry DB.

## Acceptance Criteria
- [ ] Datasource je viditelny v Grafana.
- [ ] Connection test je OK.
- [ ] Query na `test_registry` vraci data.
