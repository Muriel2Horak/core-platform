# T4: Pre-commit Tag Validation

**Story:** Test Tagging System | **Task:** T4/4 | **Effort:** ~1h | **LOC:** ~100

## Goal
Pre-commit hook validujici, ze vsechny testy maji @CORE-XXX tag.

## Tasks
- [ ] Pridat `.husky/pre-commit` hook.
- [ ] Implementovat `scripts/validate-test-tags.sh`.
- [ ] Zahrnout kontrolu Playwright a JUnit testu.

## Output
- Commit gate, ktery hlida story tagy.

## Implementation
- `.husky/pre-commit` hook
- `scripts/validate-test-tags.sh` validation script
- Check E2E tests (grep @CORE-)
- Check JUnit tests (grep @UserStory)

## Acceptance Criteria
- [ ] Hook rejects commits without tags
- [ ] Helpful error messages
- [ ] Hook installed in project
