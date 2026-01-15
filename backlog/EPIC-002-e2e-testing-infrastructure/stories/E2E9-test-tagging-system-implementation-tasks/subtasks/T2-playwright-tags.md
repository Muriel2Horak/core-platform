# T2: Playwright Tag Support

**Story:** Test Tagging System | **Task:** T2/4 | **Effort:** ~2h | **LOC:** ~100

## Goal
Implementovat tag support v Playwright testech (@CORE-XXX extraction).

## Tasks
- [ ] Pridat tag extractor pro @CORE-XXX z test titles/tags.
- [ ] Nakonfigurovat Playwright `--grep` pro tag filtering.
- [ ] Otagovat minimalni sadu smoke testu.

## Output
- Tagy funkcni v Playwright a pripraveny pro CI filter.

## Implementation
- `e2e/helpers/tag-extractor.ts` - extract @CORE-XXX from test title/tags
- `playwright.config.ts` - tag filtering support
- Update existing tests with tags

## Acceptance Criteria
- [ ] Tag extraction working
- [ ] Filter by tag (`npm run test:e2e -- --grep @CORE-123`)
- [ ] Tests tagged in codebase
