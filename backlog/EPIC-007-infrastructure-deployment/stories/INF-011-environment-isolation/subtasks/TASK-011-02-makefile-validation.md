# TASK-011-02: Makefile wiring + validation

## Goal
Napojit ENV volbu do Makefile a validovat spravne hodnoty.

## Tasks
- [ ] Pridat `ENV` param do Makefile targetu (up/down/deploy).
- [ ] Implementovat `validate-env` target.
- [ ] Upravit compose command s `--env-file` override.

## Output
- Jednoduche prepinani prostredi pres `ENV`.

## Acceptance Criteria for This Subtask
- [ ] `make up ENV=staging` pouzije `docker/.env.staging`.
- [ ] Neplatny ENV se ukonci chybou.
- [ ] Log jasne vypise zvolene prostredi.
