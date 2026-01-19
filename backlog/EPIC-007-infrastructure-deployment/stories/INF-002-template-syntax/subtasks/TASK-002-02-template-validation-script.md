# TASK-002-02: Template validation script

## Goal
Pridat skript pro validaci templatu proti .env a napojit ho na CI/pre-commit.

## Tasks
- [ ] Implementovat `scripts/templates/validate-syntax.sh` dle kriterii v US.
- [ ] Zahrnout kontrolu nepouzitych a chybejicich promennych.
- [ ] Pridat Makefile target (napr. `make validate-templates`).
- [ ] Zapojit do CI nebo pre-commit hooku.

## Output
- Automaticka kontrola templatu pri kazde zmene.

## Acceptance Criteria for This Subtask
- [ ] Skript failne pri chybejici promennne v .env.
- [ ] Skript hlasi nepouzite promennne v templatech.
- [ ] CI/pre-commit blokuje merge pri chybe.
