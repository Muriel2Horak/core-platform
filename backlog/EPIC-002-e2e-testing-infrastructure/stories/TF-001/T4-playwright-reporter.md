# T4: Playwright Reporter

**Story:** [TF-001: Test Registry & Tracking](../TF-001.md)  
**Status:** 🔵 TODO  
**Effort:** ~2 hodiny  
**LOC:** ~150 řádků

---

## 🎯 Objective

Implementovat Playwright custom reporter pro automatickou registraci E2E testů do test registry.

---

## 📋 Requirements

### Funkcionalita

- Poslouchat `onTestEnd` event z Playwright
- Extrahovat `@CORE-XXX` tag z test tags
- Extrahovat nebo auto-generovat `E2E-XXX-NNN` test ID
- Volat `POST /api/test-registry` s test daty
- Error handling - nesmí blokovat test run

### Test Tags Format

```typescript
test('Login with valid credentials @CORE-123 @E2E-LOGIN-001', async ({ page }) => {
  // Test implementation
});
```

**Parsování:**
- `@CORE-123` → `userStoryId: "CORE-123"`
- `@E2E-LOGIN-001` → `testId: "E2E-LOGIN-001"`
- Pokud chybí `@E2E-XXX`, auto-generovat z file path a test title

---

## 💻 Implementation

### 1. Registry Reporter

**File:** `e2e/reporters/registry-reporter.ts`

```typescript
import type {
  FullConfig,
  FullResult,
  Reporter,
  Suite,
  TestCase,
  TestResult,
} from '@playwright/test/reporter';

interface TestRunPayload {
  testId: string;
  userStoryId?: string;
  testType: string;
  testName: string;
  filePath: string;
  status: 'PASS' | 'FAIL' | 'SKIP';
  durationMs: number;
}

/**
 * Custom Playwright reporter that registers test results to Test Registry API
 */
export default class RegistryReporter implements Reporter {
  private apiUrl: string;

  constructor(options: { apiUrl?: string } = {}) {
    this.apiUrl = options.apiUrl || 'http://localhost:8080/api/test-registry';
  }

  onBegin(config: FullConfig, suite: Suite) {
    console.log(`[RegistryReporter] Starting test run with ${suite.allTests().length} tests`);
  }

  async onTestEnd(test: TestCase, result: TestResult) {
    try {
      const payload = this.buildPayload(test, result);
      await this.sendToRegistry(payload);
      console.log(`[RegistryReporter] Registered test: ${payload.testId} (${payload.status})`);
    } catch (error) {
      console.error(`[RegistryReporter] Failed to register test:`, error);
      // Don't fail the test run if registry is down
    }
  }

  onEnd(result: FullResult) {
    console.log(`[RegistryReporter] Test run finished: ${result.status}`);
  }

  /**
   * Build payload for test registry API
   */
  private buildPayload(test: TestCase, result: TestResult): TestRunPayload {
    const testId = this.extractTestId(test);
    const userStoryId = this.extractStoryTag(test);
    const testType = this.determineTestType(test);
    const status = this.mapStatus(result.status);

    return {
      testId,
      userStoryId,
      testType,
      testName: test.title,
      filePath: test.location.file,
      status,
      durationMs: result.duration,
    };
  }

  /**
   * Extract @CORE-XXX tag from test title or tags
   */
  private extractStoryTag(test: TestCase): string | undefined {
    // Check test.title for @CORE-XXX
    const titleMatch = test.title.match(/@CORE-(\d+)/);
    if (titleMatch) {
      return `CORE-${titleMatch[1]}`;
    }

    // Check test tags array (Playwright 1.42+)
    const coreTag = test.tags?.find(tag => tag.startsWith('@CORE-'));
    if (coreTag) {
      return coreTag.replace('@', '');
    }

    return undefined;
  }

  /**
   * Extract @E2E-XXX-NNN tag or auto-generate test ID
   */
  private extractTestId(test: TestCase): string {
    // Check for explicit @E2E-XXX-NNN tag
    const titleMatch = test.title.match(/@(E2E-[A-Z]+-\d+)/);
    if (titleMatch) {
      return titleMatch[1];
    }

    const e2eTag = test.tags?.find(tag => tag.match(/@E2E-[A-Z]+-\d+/));
    if (e2eTag) {
      return e2eTag.replace('@', '');
    }

    // Auto-generate from file path and test title
    const fileName = test.location.file.split('/').pop()?.replace('.spec.ts', '') || 'unknown';
    const sanitizedTitle = test.title.replace(/[^a-zA-Z0-9]/g, '-').substring(0, 30);
    const hash = this.hashCode(test.title + test.location.file);

    return `E2E-${fileName.toUpperCase()}-${hash}`;
  }

  /**
   * Determine test type from file path
   */
  private determineTestType(test: TestCase): string {
    if (test.location.file.includes('/pre/')) {
      return 'E2E_SMOKE';
    }
    if (test.location.file.includes('/post/')) {
      return 'E2E_FULL';
    }
    if (test.location.file.includes('/a11y/')) {
      return 'A11Y';
    }
    return 'E2E_FULL';
  }

  /**
   * Map Playwright status to TestStatus enum
   */
  private mapStatus(status: string): 'PASS' | 'FAIL' | 'SKIP' {
    switch (status) {
      case 'passed':
        return 'PASS';
      case 'failed':
      case 'timedOut':
        return 'FAIL';
      case 'skipped':
        return 'SKIP';
      default:
        return 'FAIL';
    }
  }

  /**
   * Send test result to registry API
   */
  private async sendToRegistry(payload: TestRunPayload): Promise<void> {
    const response = await fetch(this.apiUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      throw new Error(`API returned ${response.status}: ${await response.text()}`);
    }
  }

  /**
   * Simple hash function for test ID generation
   */
  private hashCode(str: string): string {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = (hash << 5) - hash + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    return Math.abs(hash).toString().substring(0, 6);
  }
}
```

---

## 🔧 Playwright Configuration

**File:** `e2e/playwright.config.ts` (UPDATE)

```typescript
import { defineConfig, devices } from '@playwright/test';
import RegistryReporter from './reporters/registry-reporter';

export default defineConfig({
  testDir: './specs',
  
  // Add custom reporter
  reporter: [
    ['list'],
    ['html', { outputFolder: 'test-results/html' }],
    ['json', { outputFile: 'test-results/results.json' }],
    [RegistryReporter, { apiUrl: process.env.TEST_REGISTRY_API || 'http://localhost:8080/api/test-registry' }],
  ],

  use: {
    baseURL: process.env.BASE_URL || 'http://localhost',
    trace: 'on-first-retry',
  },

  projects: [
    {
      name: 'pre-deploy',
      testDir: './specs/pre',
    },
    {
      name: 'post-deploy',
      testDir: './specs/post',
    },
  ],
});
```

---

## 🧪 Testing

### Test Example with Tags

**File:** `e2e/specs/post/auth/login.spec.ts` (EXAMPLE UPDATE)

```typescript
import { test, expect } from '@playwright/test';

test.describe('Login Flow @CORE-123', () => {
  test('should login with valid credentials @E2E-LOGIN-001', async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'admin');
    await page.fill('[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    
    await expect(page).toHaveURL('/dashboard');
  });

  test('should show error for invalid credentials @E2E-LOGIN-002', async ({ page }) => {
    await page.goto('/login');
    await page.fill('[name="username"]', 'invalid');
    await page.fill('[name="password"]', 'wrong');
    await page.click('button[type="submit"]');
    
    await expect(page.locator('.error-message')).toBeVisible();
  });
});
```

### Manual Verification

```bash
# 1. Start backend
make up

# 2. Run E2E tests with registry reporter
cd e2e
npx playwright test

# 3. Check logs for registry registration
# [RegistryReporter] Registered test: E2E-LOGIN-001 (PASS)

# 4. Verify in database
psql -U core -d core -c "SELECT test_id, user_story_id, status FROM test_registry WHERE user_story_id = 'CORE-123';"

# 5. Verify via API
curl http://localhost:8080/api/test-registry/story/CORE-123 | jq
```

---

## 📊 Example Output

### Console Output
```
[RegistryReporter] Starting test run with 15 tests
[RegistryReporter] Registered test: E2E-LOGIN-001 (PASS)
[RegistryReporter] Registered test: E2E-LOGIN-002 (PASS)
[RegistryReporter] Registered test: E2E-CRUD-001 (FAIL)
[RegistryReporter] Test run finished: passed
```

### Database Record
```sql
test_id          | user_story_id | test_type  | status | duration_ms
-----------------+---------------+------------+--------+-------------
E2E-LOGIN-001    | CORE-123      | E2E_SMOKE  | PASS   | 2341
E2E-LOGIN-002    | CORE-123      | E2E_SMOKE  | PASS   | 1987
E2E-CRUD-001     | CORE-456      | E2E_FULL   | FAIL   | 4521
```

---

## ✅ Acceptance Criteria

- [ ] RegistryReporter class implemented
- [ ] `onTestEnd` hook works
- [ ] `@CORE-XXX` extraction from title/tags
- [ ] `@E2E-XXX-NNN` extraction or auto-generation
- [ ] POST to `/api/test-registry` successful
- [ ] Error handling - doesn't break test run
- [ ] Playwright config updated
- [ ] Example test with tags works
- [ ] Manual verification successful

---

## 🎯 Definition of Done

- [ ] Reporter implemented and tested
- [ ] Playwright config updated
- [ ] Tag extraction working (CORE + E2E)
- [ ] API integration working
- [ ] Error handling tested
- [ ] Example tests tagged
- [ ] Database records created
- [ ] Documentation updated

---

## 🔗 Related Tasks

- **T3**: [REST API Controller](./T3-rest-api-controller.md) - Backend API
- **T5**: [JUnit Listener](./T5-junit-listener.md) - Unit test registration

---

**Back to:** [TF-001 Tasks](./README.md) | [TF-001 Story](../TF-001.md)
