# TASK-022-01: BFF service scaffold + compose

## Goal
Vytvorit BFF service a zapojit ho do docker-compose.

## Tasks
- [ ] Vytvorit `bff/` project (TypeScript, build/start skripty).
- [ ] Pridat Dockerfile a compose service pro BFF.
- [ ] Nastavit healthcheck a basic `/health` endpoint.

## Output
- BFF service bezi lokalne v compose.

## Acceptance Criteria for This Subtask
- [ ] `docker compose up bff` nastartuje bez chyby.
- [ ] `/health` endpoint vraci 200.
- [ ] Service je dostupna v core-net.
