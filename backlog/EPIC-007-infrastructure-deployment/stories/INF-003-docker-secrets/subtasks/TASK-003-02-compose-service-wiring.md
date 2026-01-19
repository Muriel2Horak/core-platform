# TASK-003-02: Compose + sluzby (secrets + fallback)

## 🎯 Goal
Napojit sluzby na Docker secrets a zachovat fallback pro dev rezim.

## 📋 Tasks
- [ ] Definovat `secrets:` sekci v `docker/docker-compose.yml`.
- [ ] Nastavit `*_FILE` promennne pro DB, Keycloak, Grafana, MinIO a dalsi sluzby.
- [ ] Dopsat fallback na env var, pokud secret soubor neexistuje (dev mode).
- [ ] Overit, ze sluzby nastartuji s secret soubory.

## 📤 Output
- Aktualizovany docker-compose s secrets wiring.
- Sluzby cteji secrety z `/run/secrets`.

## ✅ Acceptance Criteria for This Subtask
- [ ] S existujicimi secret soubory sluzby startuji bez chyb.
- [ ] Bez secret souboru je pouzit fallback a v logu je warning.
