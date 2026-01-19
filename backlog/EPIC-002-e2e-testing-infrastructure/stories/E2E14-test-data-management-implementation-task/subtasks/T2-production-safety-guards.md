# T2: Production Safety Guards
**Effort:** ~2h | **LOC:** ~150

## Goal
Fail-fast ochrana proti test datum v produkci.

## Tasks
- [ ] Pridat startup check na `test_` data.
- [ ] Zablokovat start pri detekci test dat v prod.
- [ ] Logovat jasnou chybu a navod na cleanup.

## Output
- Ochrana proti omylnemu nasazeni test dat.

## Acceptance Criteria
- [ ] Production start failne pri test datech.
- [ ] Log obsahuje jasny duvod a instrukce.
