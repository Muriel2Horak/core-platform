# T4: Pre-commit Tag Validation

**Story:** [TF-002](../TF-002.md) | **Task:** T4/4 | **Effort:** ~1h | **LOC:** ~100

## Objective
Pre-commit hook validující že všechny testy mají @CORE-XXX tag.

## Implementation
- `.husky/pre-commit` hook
- `scripts/validate-test-tags.sh` validation script
- Check E2E tests (grep @CORE-)
- Check JUnit tests (grep @UserStory)

## Acceptance Criteria
- [ ] Hook rejects commits without tags
- [ ] Helpful error messages
- [ ] Hook installed in project
