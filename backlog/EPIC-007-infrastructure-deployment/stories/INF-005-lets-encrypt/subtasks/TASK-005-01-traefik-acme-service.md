# TASK-005-01: Traefik + ACME service

## Goal
Nasadit Traefik jako reverse proxy s ACME (Let's Encrypt) klientem.

## Tasks
- [ ] Pridat Traefik service do `docker-compose.yml`.
- [ ] Nastavit entrypointy 80/443 a ACME konfiguraci.
- [ ] Nastavit provider Docker a zakladni routery pro backend/frontend.

## Output
- Traefik bezi jako primarni proxy s ACME.

## Acceptance Criteria for This Subtask
- [ ] Traefik startuje bez chyby a nasloucha na 80/443.
- [ ] ACME konfigurace je aktivni a uklada data do volume.
- [ ] Zakladni routy pro domeny funguji.
