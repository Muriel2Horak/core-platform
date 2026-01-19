# TASK-002-01: Template inventory + conversion

## Goal
Zmapovat vsechny templaty a sjednotit je na envsubst syntax.

## Tasks
- [ ] Sepsat seznam vsech template souboru (compose, nginx, realm, app config).
- [ ] Prepsat templaty na jednotny syntax `${VAR}` a `${VAR:-default}`.
- [ ] Opravit escaping a doplnit chybne placeholdery.
- [ ] Aktualizovat generovane soubory tak, aby pouzivaly nativni syntax (compose, Spring).

## Output
- Konzistentni templaty pro generovani konfiguraci.

## Acceptance Criteria for This Subtask
- [ ] Vsechny templaty pouzivaji jednotny envsubst syntax.
- [ ] Generovane soubory zustavaji kompatibilni s Docker/Spring syntax.
- [ ] Nechybi zadny placeholder definovany v .env.
