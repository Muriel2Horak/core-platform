# T3: Database Triggers
**Effort:** ~2h | **LOC:** ~100

## Goal
DB-level ochrana proti test datum v produkci.

## Tasks
- [ ] Pridat trigger pro blokaci `test_*` identifikatoru.
- [ ] Implementovat fail s jasnou chybou.
- [ ] Pokryt hlavni tabulky (users/tenants).

## Output
- Trigger, ktery blokuje test data v prod.

## Acceptance Criteria
- [ ] INSERT s `test_` username failne.
- [ ] Trigger je aktivni jen v prod prostredi.
