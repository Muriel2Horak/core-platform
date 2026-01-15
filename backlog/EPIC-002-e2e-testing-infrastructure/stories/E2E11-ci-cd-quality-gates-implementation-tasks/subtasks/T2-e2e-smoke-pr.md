# T2: E2E Smoke in PR Checks
**Effort:** ~1h | **LOC:** ~100

## Goal
Spoustet smoke E2E testy v PR kontrolach.

## Tasks
- [ ] Pridat step pro `npm run test:pre` do workflow.
- [ ] Zajistit tag filter `@SMOKE`.
- [ ] Ulozit HTML report do artifacts.

## Output
- PR gate s rychlymi smoke testy.

## Acceptance Criteria
- [ ] Smoke testy bezi na kazdy PR.
- [ ] Fail pipeline pri selhani.
- [ ] Report je dostupny v artifacts.
