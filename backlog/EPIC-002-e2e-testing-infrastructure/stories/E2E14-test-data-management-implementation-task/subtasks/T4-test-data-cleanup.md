# T4: Test Data Cleanup
**Effort:** ~2h | **LOC:** ~150

## Goal
Zajistit automaticky cleanup test dat po kazdem testu.

## Tasks
- [ ] Implementovat TestDataManager s cleanup hookem.
- [ ] Zajistit @AfterEach cleanup pro E2E.
- [ ] Osetrit fail scenario (cleanup retry).

## Output
- Konzistentni cleanup bez zbytkovych dat.

## Acceptance Criteria
- [ ] Test data po runu nezustavaji v DB.
- [ ] Cleanup funguje i pri failu testu.

