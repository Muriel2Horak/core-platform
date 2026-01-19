# TASK-001-01: Generator skripty + Makefile target

## 🎯 Goal
Sjednotit generovani konfiguraci do jednoho master skriptu a definovat source-of-truth pro templates.

## 📋 Tasks
- [ ] Ujasnit zdrojove soubory pro generovani (realm, nginx, compose) a sjednotit .env template.
- [ ] Vytvorit `scripts/generate-all-configs.sh` a zapojit `docker/keycloak/generate-realm.sh`.
- [ ] Pridat `scripts/templates/generate-nginx.sh` a `scripts/templates/generate-compose.sh` (nebo ekvivalentni reseni).
- [ ] Pridat `make generate-configs` target a popsat usage.

## 📤 Output
- Master generator + per-template skripty.
- Makefile target pro generovani konfiguraci.

## ✅ Acceptance Criteria for This Subtask
- [ ] `make generate-configs` dokonci bez chyby pri validni .env.
- [ ] Generovani je idempotentni (opakovane spusteni nema diff).
- [ ] Realm a Nginx config jsou regenerovane nebo validovane.
