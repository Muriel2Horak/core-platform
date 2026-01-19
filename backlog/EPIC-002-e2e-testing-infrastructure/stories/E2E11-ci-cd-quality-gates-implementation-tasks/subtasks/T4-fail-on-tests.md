# T4: Build Fail on Test Failure
**Effort:** ~1h | **LOC:** ~50

## Goal
Fail-fast chovani v CI pri selhani testu.

## Tasks
- [ ] Nastavit `set -e` ve workflow scriptu.
- [ ] Zastavit pipeline pri prvni chybe.
- [ ] Ujistit se, ze artifacts se ulozi pred exit.

## Output
- CI pipeline se zastavi pri prvnim failu.

## Acceptance Criteria
- [ ] Selhani testu ukonci job s non-zero.
- [ ] Reporty jsou stale dostupne.
