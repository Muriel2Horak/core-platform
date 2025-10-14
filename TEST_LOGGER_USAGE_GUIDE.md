# 🎨 Test Progress Logger - Usage Guide

## 📋 Přehled

Vytvořili jsme **TestLogger utility** pro krásné UX ve všech testech. Nyní můžeš snadno přidat progress reporting do jakéhokoli testu!

## 🚀 E2E Testy (TypeScript/Playwright)

### Import
```typescript
import { TestLogger } from '../../helpers/test-logger';
// nebo destructuring:
import { testStart, testEnd, step, success } from '../../helpers/test-logger';
```

### Základní použití

```typescript
test.describe('My Test Suite', () => {
  test.beforeAll(async () => {
    TestLogger.suiteStart('MY TEST SUITE');
  });

  test.afterAll(async () => {
    TestLogger.suiteEnd('MY TEST SUITE');
  });

  test('my first test', async () => {
    TestLogger.testStart('My First Test', 1, 5);
    
    TestLogger.step('Step 1: Logging in...');
    await login(page, 'admin', 'admin');
    TestLogger.success('Login successful');
    
    TestLogger.step('Step 2: Navigating to dashboard...');
    await page.goto('/dashboard');
    TestLogger.success('Dashboard loaded');
    
    TestLogger.verify('Verifying dashboard elements...');
    expect(page.locator('h1')).toBeVisible();
    TestLogger.success('All elements verified');
    
    TestLogger.testEnd();
  });
});
```

### Všechny dostupné metody

```typescript
// Suite management
TestLogger.suiteStart('Test Suite Name');
TestLogger.suiteEnd('Test Suite Name');

// Test management
TestLogger.testStart('Test Name', 1, 10); // s číslem
TestLogger.testStart('Test Name'); // bez čísla
TestLogger.testEnd(); // success
TestLogger.testEnd(false); // failed

// Steps & Actions
TestLogger.step('Doing something...', 1); // s číslem kroku
TestLogger.step('Doing something...'); // bez čísla
TestLogger.action('Performing action...');
TestLogger.verify('Verifying results...');
TestLogger.search('Searching for item...');
TestLogger.setup('Setting up test data...');
TestLogger.cleanup('Cleaning up...');

// Results
TestLogger.success('Action completed'); // ✓
TestLogger.info('Information message'); // ℹ️
TestLogger.warn('Warning message'); // ⚠️
TestLogger.error('Error message'); // ❌

// Data logging
TestLogger.data('User ID', userId);
TestLogger.tree([
  { label: 'Name', value: 'John' },
  { label: 'Email', value: 'john@example.com' },
  { label: 'Role', value: 'Admin', isLast: true }
]);

// Visual elements
TestLogger.separator();
TestLogger.doubleSeparator();

// Progress indicator
await TestLogger.progressDots(5000, 500); // 5s s tečkami každých 500ms
```

## 🔧 Backend Testy (Java/JUnit)

### Import
```java
import cz.muriel.core.test.helpers.TestLogger;
```

### Základní použití

```java
@Slf4j
class MyIntegrationTest {

  @BeforeEach
  void setUp() {
    TestLogger.suiteStart("MY TEST SUITE");
  }

  @AfterEach
  void tearDown() {
    TestLogger.suiteEnd("MY TEST SUITE");
  }

  @Test
  void myFirstTest() {
    TestLogger.testStart("My First Test", 1, 5);
    
    TestLogger.step("Step 1: Creating test data...", 1);
    User user = createTestUser();
    TestLogger.success("Test user created");
    
    TestLogger.step("Step 2: Calling service...", 2);
    Result result = myService.doSomething(user);
    TestLogger.success("Service call completed");
    
    TestLogger.verify("Verifying results...");
    assertThat(result).isNotNull();
    TestLogger.success("All assertions passed");
    
    TestLogger.testEnd();
  }
}
```

### Všechny dostupné metody

```java
// Suite management
TestLogger.suiteStart("Test Suite Name");
TestLogger.suiteEnd("Test Suite Name");

// Test management
TestLogger.testStart("Test Name", 1, 10); // s číslem
TestLogger.testStart("Test Name"); // bez čísla
TestLogger.testEnd(); // success
TestLogger.testEnd(false); // failed

// Steps & Actions
TestLogger.step("Doing something...", 1); // s číslem kroku
TestLogger.step("Doing something..."); // bez čísla
TestLogger.action("Performing action...");
TestLogger.verify("Verifying results...");
TestLogger.search("Searching for item...");
TestLogger.setup("Setting up test data...");
TestLogger.cleanup("Cleaning up...");

// Results
TestLogger.success("Action completed"); // with indent
TestLogger.successNoIndent("Action completed"); // no indent
TestLogger.info("Information message");
TestLogger.infoNoIndent("Information message");
TestLogger.warn("Warning message");
TestLogger.warnNoIndent("Warning message");
TestLogger.error("Error message");
TestLogger.errorNoIndent("Error message");

// Data logging
TestLogger.data("User ID", userId);
TestLogger.treeItem("Name", "John", false);
TestLogger.treeItem("Email", "john@example.com", false);
TestLogger.treeItem("Role", "Admin", true); // last item

// Visual elements
TestLogger.separator();
TestLogger.doubleSeparator();
```

## 📊 Příklady výstupu

### E2E Test Output
```
🚀 ═══════════════════════════════════════════════════
🚀  LOGIN TEST SUITE - STARTING
🚀 ═══════════════════════════════════════════════════

📝 TEST 1/3: Admin Login Flow
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔧 Step 1: Navigating to login page...
   ✓ Page loaded

🔧 Step 2: Entering credentials...
   ✓ Username entered
   ✓ Password entered

🚀 Submitting login form...
   ✓ Form submitted

🧪 Verifying redirect to dashboard...
   ✓ Dashboard URL correct
   ✓ User menu visible

✅ TEST PASSED - All assertions successful!
```

### Backend Test Output
```
🧹 ═══════════════════════════════════════════════════
🧹  TEST SETUP - Cleaning existing test data
🧹 ═══════════════════════════════════════════════════
✅ Setup complete

📝 TEST 1/8: Create User - Happy Path
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

🔧 Step 1: Preparing test data...
   ✓ User object created

🚀 Step 2: Calling userService.create()...
   ✓ Service call completed

🧪 Step 3: Verifying results...
   ✓ User saved to database
   📊 User ID: 12345
   📊 Username: testuser

✅ TEST PASSED - All assertions successful!
```

## 🎯 Migrace existujících testů

### Před (bez progress)
```typescript
test('my test', async () => {
  await login(page, 'admin', 'admin');
  await page.goto('/dashboard');
  expect(page.locator('h1')).toBeVisible();
});
```

### Po (s progress)
```typescript
test('my test', async () => {
  TestLogger.testStart('Dashboard Access Test', 1, 5);
  
  TestLogger.step('Logging in as admin...', 1);
  await login(page, 'admin', 'admin');
  TestLogger.success('Login successful');
  
  TestLogger.step('Navigating to dashboard...', 2);
  await page.goto('/dashboard');
  TestLogger.success('Dashboard loaded');
  
  TestLogger.verify('Verifying dashboard elements...');
  expect(page.locator('h1')).toBeVisible();
  TestLogger.success('Dashboard header visible');
  
  TestLogger.testEnd();
});
```

## 🔥 Quick Start - Kopíruj & Vložit

### E2E Test Template
```typescript
import { test, expect } from '@playwright/test';
import { TestLogger } from '../../helpers/test-logger';

test.describe('My Feature', () => {
  test.beforeAll(() => {
    TestLogger.suiteStart('MY FEATURE TESTS');
  });

  test.afterAll(() => {
    TestLogger.suiteEnd('MY FEATURE TESTS');
  });

  test('scenario 1', async ({ page }) => {
    TestLogger.testStart('Scenario 1', 1, 3);
    
    TestLogger.step('Step 1: ...', 1);
    // your code
    TestLogger.success('Step completed');
    
    TestLogger.verify('Verifying...');
    // assertions
    TestLogger.success('Verified');
    
    TestLogger.testEnd();
  });
});
```

### Backend Test Template
```java
import cz.muriel.core.test.helpers.TestLogger;

@SpringBootTest
class MyFeatureTest {

  @BeforeEach
  void setUp() {
    TestLogger.suiteStart("MY FEATURE TESTS");
  }

  @AfterEach
  void tearDown() {
    TestLogger.suiteEnd("MY FEATURE TESTS");
  }

  @Test
  void scenario1() {
    TestLogger.testStart("Scenario 1", 1, 3);
    
    TestLogger.step("Step 1: ...", 1);
    // your code
    TestLogger.success("Step completed");
    
    TestLogger.verify("Verifying...");
    // assertions
    TestLogger.success("Verified");
    
    TestLogger.testEnd();
  }
}
```

## 📝 Best Practices

1. **Vždy používej testStart/testEnd** - ohraničení testu
2. **Čísluj kroky** - usnadňuje debugging
3. **Loguj úspěchy** - ne jen chyby
4. **Používej správné ikony**:
   - 🔧 setup/steps
   - 🚀 actions
   - 🧪 verifications
   - 🔍 searches
   - 🧹 cleanup
   - ✓ success
   - ❌ errors

5. **Odděl logické bloky** - použij `step()` mezi hlavní části testu

## 🚀 Automatická migrace (hromadná úprava)

Pokud chceš migrovat všechny testy najednou, můžeš použít tento pattern:

1. Přidej import na začátek souboru
2. Obal každý test do `testStart()` / `testEnd()`
3. Přidej `step()` pro hlavní akce
4. Přidej `success()` po úspěšných operacích

**Tip:** Použij VS Code Replace with Regex pro hromadné úpravy!

---

**Status:** ✅ TestLogger helpers jsou připravené k použití ve všech testech!

**Next Steps:**
1. ✅ Import TestLogger do testů
2. ✅ Přidat testStart/testEnd
3. ✅ Přidat step() logging
4. ✅ Přidat success() checkpoints
5. 🎉 Enjoy beautiful test output!
