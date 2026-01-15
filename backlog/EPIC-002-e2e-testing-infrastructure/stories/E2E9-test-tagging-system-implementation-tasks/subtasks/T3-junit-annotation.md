# T3: JUnit @UserStory Annotation

**Story:** Test Tagging System | **Task:** T3/4 | **Effort:** ~2h | **LOC:** ~150

## Goal
Vytvorit @UserStory anotaci pro JUnit testy.

## Tasks
- [ ] Implementovat @UserStory annotation.
- [ ] Zajistit extractor pro story ID.
- [ ] Pridat priklady v existujicich testech.

## Output
- JUnit testy maji standardni story tag.

## Implementation
- `@UserStory("CORE-XXX")` annotation
- Annotation processor (extract at compile/runtime)
- Usage examples in existing tests

## Acceptance Criteria
- [ ] Annotation created
- [ ] Processor extracts story ID
- [ ] Tests annotated
