# T1: Grafana Dashboard JSON
**Effort:** ~3h | **LOC:** ~200

## Goal
Vytvorit Grafana dashboard JSON pro test coverage.

## Tasks
- [ ] Navrhnout dashboard layout (overview + coverage by story).
- [ ] Pridat panely pro coverage trend a test types.
- [ ] Ulozit JSON do provisioning slozky.

## Output
- `docker/grafana/dashboards/test-coverage.json` s defaultni sadou panelu.

## Acceptance Criteria
- [ ] Dashboard lze importovat bez chyb.
- [ ] Panely se zobrazi v Grafane po restartu.
- [ ] Panel pro coverage trend je pripraven na data z DB.
