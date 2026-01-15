# TASK-011-01: Environment override files

## Goal
Zavest environment-specific .env soubory a default template.

## Tasks
- [ ] Pridat `docker/.env.development`, `docker/.env.staging`, `docker/.env.production`.
- [ ] Udrzet `docker/.env.template` jako default hodnoty.
- [ ] Overit, ze citlive hodnoty nejsou hardcoded.

## Output
- Oddelene konfigurace pro dev/stage/prod.

## Acceptance Criteria for This Subtask
- [ ] Env soubory existuji a pokryvaji hlavni promennne.
- [ ] Produkcni hodnoty nejsou ve vyvojovych souborech.
- [ ] Default template neobsahuje secrety.
