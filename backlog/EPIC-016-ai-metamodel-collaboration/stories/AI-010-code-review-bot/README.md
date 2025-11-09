# AI-010: AI Code Review Bot

**Status:** 🔮 **PLANNED** (0% complete - future enhancement)  
**Effort:** 3 dny (GitHub Actions + Claude API + prompt tuning)  
**Priority:** 🟢 LOW  
**LOC:** ~800 (estimated)  
**Source:** EPIC-009 AI-004

---

## 📖 User Story

**As a developer**,  
I want AI to automatically review my PRs for common issues,  
So that I get faster feedback and catch bugs/violations before human review.

---

## 🎯 Acceptance Criteria

- 🔮 AI bot comments on PRs within 2 minutes of creation
- 🔮 Checks for: naming conventions, hardcoded secrets, missing tests, security issues
- 🔮 Comments use GitHub PR review API (inline + summary)
- 🔮 Bot does NOT block PR merge (advisory only)
- 🔮 Developer can dismiss/resolve bot comments
- 🔮 Bot learns from developer feedback (accepted/rejected suggestions)

---

## 🏗️ Planned Features

### 1. **Naming Convention Checks**

Validates naming patterns (integrates with existing `naming-lint.ts`):

```java
// PR changes:
@GetMapping("/api/users-directory")  // ❌ Should be /api/user-directories

// AI bot comments:
⚠️ **Naming Convention Violation** (line 23)

Path should use kebab-case plural: `/api/user-directories`

See: .github/naming-conventions.md
```

### 2. **Security Checks**

Detects hardcoded secrets, SQL injection, XSS vulnerabilities:

```java
// PR changes:
String apiKey = "abc123";  // ❌ Hardcoded secret

// AI bot comments:
🔴 **Security Issue: Hardcoded Secret** (line 78)

Detected hardcoded API key. Use environment variable:

```java
@Value("${API_KEY}")
private String apiKey;
```

Risk: Credentials in Git history. Rotate key after fix.
```

### 3. **Best Practices**

Suggests improvements (e.g., missing `@Transactional`):

```java
// PR changes:
public void updateUser(User user) {  // ❌ Missing @Transactional
    userRepository.save(user);
}

// AI bot comments:
🟡 **Best Practice: Missing @Transactional** (line 45)

Database write operations should be transactional:

```java
@Transactional
public void updateUser(User user) {
    userRepository.save(user);
}
```

Prevents data corruption on failure.
```

### 4. **Test Coverage**

Warns if PR lacks tests:

```java
// PR adds new UserService.updateEmail() method
// No corresponding test file changed

// AI bot comments:
⚠️ **Missing Tests** (UserService.java)

New method `updateEmail()` has no tests.

Add test in `UserServiceTest.java`:

```java
@Test
void shouldUpdateEmail() {
    // Arrange
    User user = createTestUser();
    
    // Act
    userService.updateEmail(user.getId(), "new@example.com");
    
    // Assert
    assertThat(user.getEmail()).isEqualTo("new@example.com");
}
```

Coverage impact: -2.2% (85.2% → 83.0%)
```

### 5. **Documentation Checks**

Requests docs for public APIs:

```java
// PR changes:
public void exportData(String format) {  // ❌ No JavaDoc

// AI bot comments:
📝 **Missing Documentation** (line 56)

Public API methods should have JavaDoc:

```java
/**
 * Exports user data in specified format.
 * 
 * @param format Export format (csv, json, xml)
 * @throws IllegalArgumentException if format unsupported
 */
public void exportData(String format) {
    // ...
}
```
```

---

## 💻 Technical Implementation

### Architecture

```
┌─────────────────┐
│  GitHub PR      │ (Developer opens PR)
└────────┬────────┘
         │ Webhook
         ▼
┌─────────────────┐
│ GitHub Actions  │ (Workflow: .github/workflows/ai-review.yml)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Review Script  │ (Node.js: tools/ai-reviewer/review-pr.ts)
└────────┬────────┘
         │
         ├─► 1. Fetch PR diff (GitHub API)
         ├─► 2. Analyze code (Claude API)
         ├─► 3. Generate comments (structured output)
         └─► 4. Post review (GitHub API)
```

---

### GitHub Actions Workflow

**File:** `.github/workflows/ai-review.yml`

```yaml
name: AI Code Review

on:
  pull_request:
    types: [opened, synchronize]

jobs:
  ai-review:
    runs-on: ubuntu-latest
    permissions:
      pull-requests: write
      contents: read
    
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0  # Full history for git diff
      
      - uses: actions/setup-node@v4
        with:
          node-version: '18'
      
      - name: Install dependencies
        run: |
          cd tools/ai-reviewer
          npm ci
      
      - name: Run AI Review
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
          PR_NUMBER: ${{ github.event.pull_request.number }}
        run: |
          node tools/ai-reviewer/review-pr.js
```

---

### Review Script

**File:** `tools/ai-reviewer/review-pr.ts`

```typescript
#!/usr/bin/env node
import { Anthropic } from "@anthropic-ai/sdk";
import { Octokit } from "@octokit/rest";

const anthropic = new Anthropic({ apiKey: process.env.ANTHROPIC_API_KEY! });
const octokit = new Octokit({ auth: process.env.GITHUB_TOKEN! });

const PR_NUMBER = parseInt(process.env.PR_NUMBER!);
const REPO_OWNER = "core-platform";
const REPO_NAME = "core-platform";

async function reviewPR() {
  // 1. Fetch PR diff
  const { data: pr } = await octokit.pulls.get({
    owner: REPO_OWNER,
    repo: REPO_NAME,
    pull_number: PR_NUMBER,
  });

  const { data: files } = await octokit.pulls.listFiles({
    owner: REPO_OWNER,
    repo: REPO_NAME,
    pull_number: PR_NUMBER,
  });

  // 2. Analyze each changed file
  const reviews: Array<{ file: string; line: number; comment: string }> = [];

  for (const file of files) {
    if (!file.patch) continue; // Skip binary files

    const analysis = await analyzeCode(file.filename, file.patch);
    reviews.push(...analysis);
  }

  // 3. Post review comments
  if (reviews.length > 0) {
    await octokit.pulls.createReview({
      owner: REPO_OWNER,
      repo: REPO_NAME,
      pull_number: PR_NUMBER,
      event: "COMMENT",
      comments: reviews.map((r) => ({
        path: r.file,
        line: r.line,
        body: r.comment,
      })),
      body: generateSummary(reviews),
    });
    
    console.log(`✅ Posted ${reviews.length} review comments`);
  } else {
    console.log("✅ No issues found - code looks good!");
  }
}

async function analyzeCode(
  filename: string,
  patch: string
): Promise<Array<{ file: string; line: number; comment: string }>> {
  
  const response = await anthropic.messages.create({
    model: "claude-3-5-sonnet-20241022",
    max_tokens: 4000,
    system: `You are a code reviewer for Core Platform.

Review code changes and identify:
1. Naming convention violations (kebab-case for URLs, PascalCase for classes)
2. Hardcoded secrets (API keys, passwords)
3. Missing @Transactional on database writes
4. Missing tests for new methods
5. Security issues (SQL injection, XSS)
6. Missing JavaDoc on public APIs

Output format (JSON array):
[
  {
    "line": 23,
    "severity": "warning",
    "category": "naming",
    "message": "Path should use kebab-case plural: /api/user-directories"
  }
]

If no issues, return empty array [].
`,
    messages: [
      {
        role: "user",
        content: `Review this code change:

File: ${filename}

Diff:
${patch}
`,
      },
    ],
  });

  const aiOutput = response.content[0].text;
  
  try {
    const issues = JSON.parse(aiOutput);
    
    return issues.map((issue: any) => ({
      file: filename,
      line: issue.line,
      comment: formatComment(issue),
    }));
  } catch (error) {
    console.error("Failed to parse AI output:", aiOutput);
    return [];
  }
}

function formatComment(issue: {
  severity: string;
  category: string;
  message: string;
}): string {
  const emoji = {
    error: "🔴",
    warning: "⚠️",
    info: "💡",
  }[issue.severity] || "ℹ️";

  const title = {
    naming: "Naming Convention",
    security: "Security Issue",
    "best-practice": "Best Practice",
    testing: "Missing Tests",
    documentation: "Missing Documentation",
  }[issue.category] || "Code Review";

  return `${emoji} **${title}**\n\n${issue.message}`;
}

function generateSummary(
  reviews: Array<{ file: string; line: number; comment: string }>
): string {
  const errorCount = reviews.filter((r) => r.comment.includes("🔴")).length;
  const warningCount = reviews.filter((r) => r.comment.includes("⚠️")).length;

  return `## 🤖 AI Code Review

### Summary
- 🔴 ${errorCount} error(s)
- ⚠️ ${warningCount} warning(s)

Review each comment and address issues before merging.

---
*This review was generated by AI. Human review still required.*
`;
}

// Run review
reviewPR().catch((error) => {
  console.error("❌ Review failed:", error);
  process.exit(1);
});
```

---

## 🎯 Example PR Review

**PR #123:** "Add user export feature"

**AI Bot Comment (Summary):**

```markdown
## 🤖 AI Code Review

### ⚠️ Issues Found

1. **Naming Convention Violation** (UserController.java:23)
   Path should use kebab-case plural: `/api/user-exports`
   
2. **Missing @Transactional** (UserService.java:45)
   Database write operations should be transactional.
   
3. **Hardcoded Secret** (ExportService.java:78)
   Detected hardcoded API key. Use `${S3_API_KEY}` environment variable.
   
4. **Missing Tests** (ExportService.java:56)
   New method `exportToCsv()` has no tests.

### ✅ Good Practices Detected

- ✅ Uses @PreAuthorize for RBAC
- ✅ Includes Swagger @Operation annotations
- ✅ Proper error handling with try-catch

### 📊 Coverage Impact

- **Before**: 85.2% line coverage
- **After**: 83.0% line coverage (-2.2%)
- **Missing**: `ExportService.exportToCsv()` has no tests

---

*This review was generated by AI. Human review still required.*
```

---

## ✅ Value Proposition

### Speed
- **Human Review**: 2-4 hours (waiting for reviewer)
- **AI Review**: 2 minutes (immediate feedback)
- **Faster Iterations**: Developer fixes issues before human review

### Quality
- **Catches Common Mistakes**: Naming, security, best practices
- **Consistent Standards**: AI never misses hardcoded secrets
- **Frees Human Reviewers**: Focus on business logic, not style

### Learning
- **Educational**: Developers learn conventions from AI comments
- **Living Documentation**: AI references project docs in comments
- **Onboarding**: New devs get instant feedback

---

## 🧪 Testing the Bot

### Manual Test
```bash
# 1. Create test PR with intentional issues
cat > test-file.java << 'EOF'
@GetMapping("/api/users-list")  // ❌ Should be /api/users
public List<User> getUsers() {
    String password = "admin123";  // ❌ Hardcoded secret
    return userRepository.findAll();  // ❌ Missing @Transactional
}
EOF

git checkout -b test-ai-review
git add test-file.java
git commit -m "test: Trigger AI review"
git push origin test-ai-review

# 2. Open PR on GitHub
# Wait 2 minutes

# 3. Check PR for AI review comments
# Should see 3 warnings (naming, hardcoded secret, missing @Transactional)
```

---

## 📦 Dependencies

- **@anthropic-ai/sdk** (Claude API)
- **@octokit/rest** (GitHub API client)
- **GitHub Actions** (CI/CD)
- **Node.js** 18+ (for async/await)

---

## 🚀 Rollout Plan

### Phase 1: Prototype (2 weeks)
- ⏳ Create review script (`review-pr.ts`)
- ⏳ Test with sample PRs
- ⏳ Tune AI prompts for accuracy

### Phase 2: Pilot (1 week)
- ⏳ Enable on test repository
- ⏳ Collect feedback from team
- ⏳ Measure false positive rate

### Phase 3: Production (ongoing)
- ⏳ Enable on main repository
- ⏳ Monitor bot performance
- ⏳ Iterate on prompts based on feedback

---

## 📊 Success Metrics

**Target KPIs:**
- **Review Time**: <2 min (AI) + 30 min (human)
- **Issue Detection**: 90%+ common violations caught
- **False Positive Rate**: <10%
- **Developer Satisfaction**: 8/10

**Tracking:**
- PR metrics: Time to first review (AI vs human)
- Code quality: % PRs with violations (before vs after bot)
- Developer survey: Usefulness of AI comments

---

## 🛠️ Developer Workflow

```bash
# 1. Create PR as usual
git checkout -b feature/new-export
git commit -m "feat: Add CSV export"
git push origin feature/new-export

# 2. Open PR on GitHub
# AI bot reviews within 2 minutes

# 3. Review AI comments
# Fix issues:
# - Rename /api/exports-list → /api/exports
# - Move hardcoded key to .env
# - Add @Transactional

# 4. Push fixes
git add .
git commit -m "fix: Address AI review comments"
git push

# 5. Human review focuses on business logic
# (AI already validated style/security)
```

---

## 📚 References

- [GitHub REST API - Pull Requests](https://docs.github.com/en/rest/pulls)
- [Anthropic Claude API](https://docs.anthropic.com/claude/reference/messages)
- [GitHub Actions](https://docs.github.com/en/actions)
- Project: `tools/naming-lint.ts` (existing naming validation)

---

**Last Updated:** October 2024  
**Status:** 🔮 PLANNED (0% complete - future enhancement)  
**Next Action:** Create prototype review script + test with sample PRs
