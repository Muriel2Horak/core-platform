# TASK-009-01: Flyway configs + directories

## Goal
Zavest strukturu migraci pro core/keycloak/grafana a konfiguraci Flyway.

## Tasks
- [ ] Vytvorit slozky `db/migration/{core,keycloak,grafana}`.
- [ ] Pridat Flyway config soubory pro kazdou DB.
- [ ] Zapnout Flyway v aplikaci a nastavit locations.

## Output
- Sjednocena struktura migraci pro vsechny DB.

## Acceptance Criteria for This Subtask
- [ ] Flyway najde migrace pro vsechny DB.
- [ ] Naming konvence V/U je popsana a pouzita.
- [ ] Validace migraci projde bez chyb.
