# T2: Playwright Tag Support

**Story:** [TF-002](../TF-002.md) | **Task:** T2/4 | **Effort:** ~2h | **LOC:** ~100

## Objective
Implementovat tag support v Playwright testech (@CORE-XXX extraction).

## Implementation
- `e2e/helpers/tag-extractor.ts` - extract @CORE-XXX from test title/tags
- `playwright.config.ts` - tag filtering support
- Update existing tests with tags

## Acceptance Criteria
- [ ] Tag extraction working
- [ ] Filter by tag (`npm run test:e2e -- --grep @CORE-123`)
- [ ] Tests tagged in codebase
