# AI-009: AI Test Generation from Acceptance Criteria

**Status:** ⏳ **PENDING** (30% design complete)  
**Effort:** 2 dny (prompt engineering + tooling)  
**Priority:** 🟡 MEDIUM  
**LOC:** ~600 (estimated)  
**Source:** EPIC-009 AI-003

---

## 📖 User Story

**As a developer**,  
I want AI to generate Playwright E2E tests from user story acceptance criteria,  
So that every story has automated tests without manual boilerplate writing.

---

## 🎯 Acceptance Criteria

- ⏳ AI reads story README (acceptance criteria section)
- ⏳ AI generates Playwright test code following project patterns
- ⏳ Generated tests use `e2e/helpers/fixtures.ts` (authenticatedPage, etc.)
- ⏳ Tests validate all acceptance criteria points
- ⏳ Developer reviews + commits generated test
- ⏳ CI enforces: Every story must have tests (or fail build)

---

## 🏗️ Planned Implementation

### Workflow

```
1. User creates story: backlog/EPIC-XXX/stories/STORY-YYY/README.md

2. Story contains Acceptance Criteria:
   
   ## Acceptance Criteria
   - GIVEN user is logged in as CORE_ADMIN
   - WHEN user clicks "Create Entity" button
   - THEN entity form is displayed
   - AND form has fields: name, description, type
   - AND "Save" button is enabled

3. Developer runs:
   npm run generate-tests STORY-YYY

4. AI (via Claude API + MCP) generates Playwright test:
   
   // e2e/specs/generated/STORY-YYY.spec.ts
   test('Create entity flow', async ({ authenticatedPage: page }) => {
     // Login as CORE_ADMIN (fixture handles this)
     await page.goto('/admin/entities');
     
     // WHEN: Click create button
     await page.getByRole('button', { name: 'Create Entity' }).click();
     
     // THEN: Form displayed
     await expect(page.getByLabel('Name')).toBeVisible();
     await expect(page.getByLabel('Description')).toBeVisible();
     await expect(page.getByLabel('Type')).toBeVisible();
     
     // AND: Save button enabled
     await expect(page.getByRole('button', { name: 'Save' })).toBeEnabled();
   });

5. Developer reviews generated test, adjusts if needed, commits

6. CI runs test on every PR
```

---

## 💻 Technical Design

### Tool: `generate-test.ts`

**File:** `tools/test-generator/generate-from-story.ts`

```typescript
#!/usr/bin/env node
import { Anthropic } from "@anthropic-ai/sdk";
import fs from "fs/promises";
import path from "path";

const anthropic = new Anthropic({ 
  apiKey: process.env.ANTHROPIC_API_KEY 
});

async function generateTest(storyId: string) {
  // 1. Read story README
  const storyPath = `backlog/EPIC-XXX/stories/${storyId}/README.md`;
  const storyContent = await fs.readFile(storyPath, "utf-8");
  
  // 2. Extract acceptance criteria section
  const criteriaMatch = storyContent.match(
    /## Acceptance Criteria\n([\s\S]*?)(?=\n##|$)/
  );
  const acceptanceCriteria = criteriaMatch?.[1] || "";
  
  if (!acceptanceCriteria) {
    console.error("❌ No acceptance criteria found in story");
    process.exit(1);
  }
  
  // 3. Read project test patterns (for context)
  const fixturesCode = await fs.readFile(
    "e2e/helpers/fixtures.ts", 
    "utf-8"
  );
  
  // 4. Generate test via Claude API
  const response = await anthropic.messages.create({
    model: "claude-3-5-sonnet-20241022",
    max_tokens: 4000,
    system: `You are a test generation assistant for Core Platform.
    
Your task:
1. Read user story acceptance criteria
2. Generate Playwright E2E test that validates ALL criteria
3. Follow Core Platform testing patterns (see fixtures.ts)

Test conventions:
- Use authenticatedPage fixture (handles login)
- Use getByRole() for accessibility (not CSS selectors)
- Group assertions with expect()
- Add comments mapping to acceptance criteria (GIVEN/WHEN/THEN)

Example output:
\`\`\`typescript
import { test, expect } from '@playwright/test';

test('Story XYZ validation', async ({ authenticatedPage: page }) => {
  // GIVEN: User is logged in (fixture handles this)
  await page.goto('/admin/entities');
  
  // WHEN: User clicks create button
  await page.getByRole('button', { name: 'Create Entity' }).click();
  
  // THEN: Form is displayed
  await expect(page.getByLabel('Name')).toBeVisible();
});
\`\`\`

Now generate test for these acceptance criteria:
${acceptanceCriteria}

Available fixtures (from fixtures.ts):
${fixturesCode}
`,
    messages: [{
      role: "user",
      content: `Generate Playwright test for story ${storyId}:\n\n${acceptanceCriteria}`
    }]
  });
  
  // 5. Extract code from AI response
  const aiResponse = response.content[0].text;
  const codeMatch = aiResponse.match(/```typescript\n([\s\S]*?)\n```/);
  const testCode = codeMatch?.[1] || aiResponse;
  
  // 6. Write test file
  const testPath = `e2e/specs/generated/${storyId}.spec.ts`;
  await fs.mkdir(path.dirname(testPath), { recursive: true });
  await fs.writeFile(testPath, testCode);
  
  console.log(`✅ Generated test: ${testPath}`);
  console.log(`\n📝 Review the test and commit if correct.`);
}

// CLI usage
const storyId = process.argv[2];
if (!storyId) {
  console.error("Usage: npm run generate-tests STORY-001");
  process.exit(1);
}

generateTest(storyId).catch(console.error);
```

---

### Integration with CI/CD

**GitHub Actions Workflow:** `.github/workflows/test-coverage.yml`

```yaml
name: Test Coverage Check

on: [pull_request]

jobs:
  check-story-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Check all stories have tests
        run: |
          #!/bin/bash
          
          # Find all story READMEs
          stories=$(find backlog -name "README.md" -path "*/stories/*")
          
          missing_tests=()
          
          for story in $stories; do
            # Extract story ID (e.g., AI-001, DMS-003)
            story_id=$(basename $(dirname $story))
            
            # Check if test exists
            test_file="e2e/specs/generated/${story_id}.spec.ts"
            
            if [[ ! -f "$test_file" ]]; then
              missing_tests+=("$story_id")
            fi
          done
          
          if [[ ${#missing_tests[@]} -gt 0 ]]; then
            echo "❌ Stories without tests:"
            printf '%s\n' "${missing_tests[@]}"
            echo ""
            echo "Generate tests with: npm run generate-tests STORY-ID"
            exit 1
          fi
          
          echo "✅ All stories have tests"
```

---

## 🎯 Example: Generated Test

**Input Story:** `backlog/EPIC-001/stories/RBAC-001-entity-crud-permissions/README.md`

**Acceptance Criteria:**
```markdown
## Acceptance Criteria
- GIVEN user with CORE_VIEWER role
- WHEN user navigates to /admin/entities
- THEN user can view entity list (GET /api/entities)
- AND user CANNOT create entities (Create button hidden)
- AND user CANNOT edit entities (Edit buttons disabled)
- AND user CANNOT delete entities (Delete buttons hidden)
```

**Generated Test:** `e2e/specs/generated/RBAC-001.spec.ts`

```typescript
import { test, expect } from '@playwright/test';

test.describe('RBAC-001: Entity CRUD Permissions', () => {
  test('CORE_VIEWER role restrictions', async ({ authenticatedPage: page }) => {
    // GIVEN: User with CORE_VIEWER role
    // (authenticatedPage fixture logs in as CORE_VIEWER by default)
    
    // WHEN: User navigates to /admin/entities
    await page.goto('/admin/entities');
    
    // THEN: User can view entity list
    await expect(page.getByRole('heading', { name: 'Entities' })).toBeVisible();
    await expect(page.getByRole('table')).toBeVisible();
    
    // AND: Create button hidden
    await expect(
      page.getByRole('button', { name: 'Create Entity' })
    ).not.toBeVisible();
    
    // AND: Edit buttons disabled (check first row)
    const firstRow = page.getByRole('row').nth(1);
    await expect(
      firstRow.getByRole('button', { name: 'Edit' })
    ).toBeDisabled();
    
    // AND: Delete buttons hidden
    await expect(
      firstRow.getByRole('button', { name: 'Delete' })
    ).not.toBeVisible();
  });
});
```

---

## ✅ Value Proposition

### Speed
- **Manual Test Writing**: 30-60 min per story
- **AI Generation**: 2-5 min per story
- **10x faster** test creation

### Coverage
- **Before**: ~60% stories have tests (manual effort barrier)
- **After**: 100% stories have tests (CI enforces)
- **Quality Gate**: PR cannot merge without tests

### Consistency
- **Manual Tests**: Variable quality, different patterns
- **AI Tests**: Consistent style (follows fixtures.ts patterns)
- **Maintenance**: Easier to update (predictable structure)

### Documentation
- **Tests as Spec**: Generated tests are executable acceptance criteria
- **Living Documentation**: Tests reflect current story requirements
- **Onboarding**: New devs read tests to understand features

---

## 🧪 Testing the Generator

### Manual Test
```bash
# 1. Set API key
export ANTHROPIC_API_KEY="sk-ant-..."

# 2. Create test story
mkdir -p backlog/EPIC-TEST/stories/TEST-001
cat > backlog/EPIC-TEST/stories/TEST-001/README.md << 'EOF'
# TEST-001: Login Flow

## Acceptance Criteria
- GIVEN user at /login page
- WHEN user enters valid credentials
- THEN user is redirected to /admin/dashboard
- AND navigation menu is visible
EOF

# 3. Generate test
npm run generate-tests TEST-001

# 4. Review generated test
cat e2e/specs/generated/TEST-001.spec.ts

# 5. Run test
npx playwright test TEST-001
```

### Validation Checklist
- ✅ Generated test compiles (TypeScript)
- ✅ Test uses `authenticatedPage` fixture correctly
- ✅ All acceptance criteria points covered
- ✅ Uses `getByRole()` (accessibility)
- ✅ GIVEN/WHEN/THEN comments present
- ✅ Test passes when run against app

---

## 📦 Dependencies

- **@anthropic-ai/sdk** (Claude API client)
- **Playwright** (already in project)
- **Node.js** 18+ (for async/await)
- **e2e/helpers/fixtures.ts** (project test patterns)

---

## 🚀 Rollout Plan

### Phase 1: Prototype (1 week)
- ✅ Create `generate-from-story.ts` tool
- ✅ Test with 3-5 sample stories
- ✅ Validate output quality
- ✅ Tune AI prompt for accuracy

### Phase 2: CI Integration (1 week)
- ⏳ Add GitHub Actions workflow (test coverage check)
- ⏳ Enforce: Every story must have test
- ⏳ Document workflow in README

### Phase 3: Team Adoption (ongoing)
- ⏳ Onboard team to workflow
- ⏳ Collect feedback on generated tests
- ⏳ Iterate on prompt for better quality

---

## 📊 Success Metrics

**Target KPIs:**
- **Test Coverage**: 100% stories with tests (up from 60%)
- **Test Creation Time**: 5 min avg (down from 30-60 min)
- **Test Quality**: 90%+ generated tests pass without modification
- **Developer Satisfaction**: 8/10 (internal survey)

**Tracking:**
- CI reports: % stories with tests
- PR metrics: Time from story creation to test commit
- Code review: How many generated tests need fixes

---

## 🛠️ Developer Workflow

```bash
# 1. Create story
mkdir -p backlog/EPIC-016/stories/AI-NEW
vim backlog/EPIC-016/stories/AI-NEW/README.md
# Add acceptance criteria

# 2. Generate test
npm run generate-tests AI-NEW

# 3. Review generated test
vim e2e/specs/generated/AI-NEW.spec.ts

# 4. Run test to validate
npx playwright test AI-NEW

# 5. Commit if correct
git add e2e/specs/generated/AI-NEW.spec.ts
git commit -m "test: Add E2E test for AI-NEW story"

# 6. PR will pass CI (test exists)
```

---

## 📚 References

- [Anthropic Claude API](https://docs.anthropic.com/claude/reference/messages)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- Project: `e2e/helpers/fixtures.ts` (test patterns)
- EPIC-016 AI-001: AI Metamodel Designer (similar AI integration)

---

**Last Updated:** October 2024  
**Status:** ⏳ PENDING (30% design complete)  
**Next Action:** Create prototype tool + test with 3 sample stories
