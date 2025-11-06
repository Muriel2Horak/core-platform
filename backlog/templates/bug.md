---
# 🐛 BUG TEMPLATE
# Usage: Copy this template when reporting bugs
# Replace placeholders with actual values
# Keep YAML frontmatter format strict (valid YAML)

id: BUG-XXX                           # Auto-assigned by system (e.g., BUG-001)
type: bug                             # Fixed value: "bug"
severity: high                        # critical | high | medium | low
status: reported                      # reported | investigating | in-progress | fixed | verified | closed
created: YYYY-MM-DD                   # Creation date
updated: YYYY-MM-DD                   # Last update

# 🔗 Traceability - CRITICAL for root cause analysis
caused_by_story: CORE-XXX             # Which user story introduced this bug? (e.g., CORE-005)
caused_by_commit: abc1234             # Which commit caused the bug? (git commit hash)
found_in_version: v1.2.3              # Version where bug was discovered
fixed_in_version: v1.2.4              # Version where bug was fixed (empty until fixed)

# 🧪 Regression Prevention - MANDATORY
regression_test: path/to/test.spec.ts # Path to test preventing recurrence
regression_test_status: not-written   # not-written | written | passing | failing

# 👤 Assignment
reporter: user@example.com            # Who reported the bug
assignee: dev@example.com             # Who is fixing it
verifier: qa@example.com              # Who will verify the fix

# 🏷️ Classification
labels:
  - bug
  - regression
  - critical                          # Add relevant labels

# 📊 Metrics
time_to_detect: 2 days                # Time from introduction to detection
time_to_fix: 4 hours                  # Time from detection to fix (empty until fixed)
---

# 🐛 Bug Title

**Brief, descriptive title** (e.g., "Login fails for users with special characters in email")

---

## 📝 Description

### Expected Behavior
> What SHOULD happen?

Popis očekávaného chování podle původní story nebo requirements.

**Example:**
```
Uživatel s emailem obsahujícím '+' znak (např. user+test@example.com) 
by měl být schopen se přihlásit bez problémů.
```

### Actual Behavior
> What DOES happen?

Detailní popis co se skutečně děje, včetně error messages.

**Example:**
```
Login formulář vrací chybu "Invalid email format" při zadání 
emailu s '+' znakem. Uživatel nemůže pokračovat.

Error message:
  "Email validation failed: Special characters not allowed"
```

### Impact
> Koho a jak moc to ovlivňuje?

- **Severity Justification:** Proč critical/high/medium/low?
- **User Impact:** Kolik uživatelů je postiženo?
- **Business Impact:** Jaké jsou business důsledky?

**Example:**
```
Severity: HIGH
- 15% uživatelů používá '+' alias v emailu (Gmail feature)
- Blokuje onboarding nových uživatelů
- Potenciální revenue loss: $5000/month
```

---

## 🔬 Reproduction Steps

> **Krok-za-krokem** instrukce jak bug reprodukovat

### Prerequisites
- [ ] Test environment: dev | staging | production
- [ ] User role: admin | user | guest
- [ ] Browser: Chrome 120 | Firefox 119 | Safari 17
- [ ] Test data: Specific user account or data needed

### Steps to Reproduce
1. Otevři login page: `https://admin.core-platform.local/login`
2. Zadej email: `user+test@example.com`
3. Zadej heslo: `ValidPassword123!`
4. Klikni "Sign In"
5. **Observe:** Error message "Invalid email format"

### Actual Result
```
❌ Login fails with error:
   "Email validation failed: Special characters not allowed"
```

### Expected Result
```
✅ User should be logged in and redirected to dashboard
```

### Reproduction Rate
- [ ] 100% (always reproducible)
- [ ] 50-99% (frequently reproducible)
- [ ] 10-49% (occasionally reproducible)
- [ ] <10% (rarely reproducible)

---

## 🔍 Root Cause Analysis

> **Pouze po investigation** - Proč se bug stal?

### Caused By
- **Story:** [CORE-003: User Authentication](../EPIC-001-auth/stories/CORE-003-user-auth/README.md)
- **Commit:** `abc1234` - "feat(CORE-003): Add email validation"
- **File:** `backend/src/main/java/cz/muriel/core/auth/validators/EmailValidator.java:42`
- **Code:**
  ```java
  // PROBLEMATIC CODE (line 42)
  if (email.matches("^[a-zA-Z0-9@._-]+$")) {
      return true;
  }
  // Missing '+' character in allowed pattern!
  ```

### Why Bug Occurred
- AC in CORE-003 didn't specify email alias support ('+' character)
- Email validation regex too restrictive
- Missing test case for special characters in email
- Code review didn't catch edge case

### Prevention
> Co mohlo zabránit tomuto bugu?

- [ ] Better AC definition (include email alias examples)
- [ ] Test case for special characters
- [ ] Code review checklist for validation logic
- [ ] Stricter validation against RFC 5322 email spec

---

## ✅ Acceptance Criteria for Fix

> **Given/When/Then** - Jak poznat že bug je vyřešen?

### AC1: Email with '+' character allowed
**Given** user has email `user+test@example.com`  
**When** user submits login form  
**Then** email validation passes ✅

**Test:**
```bash
# E2E test verifying fix
npx playwright test specs/auth/login-email-alias.spec.ts
```

### AC2: Other special characters work
**Given** user has email with `.`, `_`, `-` characters  
**When** user submits login form  
**Then** all valid RFC 5322 characters are accepted ✅

**Test:**
```bash
# Unit test for validator
cd backend && ./mvnw test -Dtest=EmailValidatorTest#testSpecialCharacters
```

### AC3: Invalid emails still rejected
**Given** user has email `invalid@` (incomplete)  
**When** user submits login form  
**Then** validation fails with clear error message ✅

---

## 🧪 Regression Test

> **MANDATORY** - Test který zajistí že bug se nikdy nevrátí

### Test File
**Path:** `e2e/specs/auth/login-email-alias.spec.ts`

**Tags:** `@BUG-XXX @regression @auth`

### Test Code
```typescript
import { test, expect } from '@playwright/test';

test('login works with email containing + character @BUG-XXX @regression', async ({ page }) => {
  // Arrange
  const email = 'user+test@example.com';
  const password = 'ValidPassword123!';
  
  // Act
  await page.goto('/login');
  await page.fill('input[name="email"]', email);
  await page.fill('input[name="password"]', password);
  await page.click('button[type="submit"]');
  
  // Assert
  await expect(page).toHaveURL('/dashboard');
  await expect(page.locator('text="Welcome"')).toBeVisible();
});

test('login works with other valid email aliases @BUG-XXX @regression', async ({ page }) => {
  // Test cases for '.', '_', '-' characters
  const testEmails = [
    'user.name@example.com',
    'user_name@example.com',
    'user-name@example.com'
  ];
  
  for (const email of testEmails) {
    await page.goto('/login');
    await page.fill('input[name="email"]', email);
    await page.fill('input[name="password"]', 'ValidPassword123!');
    await page.click('button[type="submit"]');
    
    await expect(page).toHaveURL('/dashboard');
    await page.click('button[aria-label="Logout"]'); // Reset for next iteration
  }
});
```

### Running Regression Test
```bash
# Run this specific regression test
npx playwright test specs/auth/login-email-alias.spec.ts

# Run ALL regression tests
npx playwright test --grep @regression

# Run all tests for this bug
npx playwright test --grep @BUG-XXX
```

### Test Status
- [ ] Test written
- [ ] Test failing (red phase)
- [ ] Fix implemented
- [ ] Test passing (green phase)
- [ ] Refactored
- [ ] Merged to main

---

## 📋 Fix Definition of Done

> Checklist před close bugem

### 🔧 Code Fix
- [ ] Root cause identified (file + line number)
- [ ] Fix implemented (with code review)
- [ ] Fix doesn't break existing functionality
- [ ] Code follows project conventions

### 🧪 Testing
- [ ] **Regression test written** (MANDATORY!)
- [ ] Regression test tagged `@BUG-XXX @regression`
- [ ] Regression test passing
- [ ] Original AC from caused_by_story still passing
- [ ] No new failing tests introduced
- [ ] Manual testing completed

### 📚 Documentation
- [ ] Bug updated with root cause analysis
- [ ] Bug updated with fix commit hash
- [ ] Bug updated with fixed_in_version
- [ ] CHANGELOG.md entry added
- [ ] caused_by_story updated (if needed)

### 🤝 Verification
- [ ] Fix verified by reporter
- [ ] Fix verified by QA (if applicable)
- [ ] Fix deployed to staging
- [ ] Fix verified in staging
- [ ] Fix deployed to production

### 🚀 Deployment
- [ ] Fix merged to main
- [ ] Version tagged (e.g., v1.2.4)
- [ ] Release notes updated
- [ ] Bug status: closed

---

## 🔗 Related Items

### Related Stories
- [CORE-003: User Authentication](../EPIC-001-auth/stories/CORE-003-user-auth/README.md) - Story that introduced bug

### Related Commits
- `abc1234` - "feat(CORE-003): Add email validation" (caused bug)
- `def5678` - "fix(BUG-XXX): Allow '+' in email validation" (fixed bug)

### Related Bugs
- BUG-042: Similar validation issue with phone numbers
- BUG-051: Email case sensitivity bug

### Related Documentation
- [RFC 5322: Email Address Specification](https://tools.ietf.org/html/rfc5322)
- [Test-Driven Workflow](../../docs/development/test-driven-workflow.md)

---

## 💬 Comments / Discussion

### 2025-11-06 - Reporter
> Discovered during onboarding flow testing. Multiple users reported inability to login.

### 2025-11-06 - Developer
> Root cause identified: EmailValidator regex missing '+' character. Fix in progress.

### 2025-11-07 - QA
> Verified fix in staging. Tested 10 different email formats. All passing. ✅

---

## 📊 Timeline

| Date | Event | By |
|------|-------|-----|
| 2025-11-06 09:00 | Bug reported | QA Team |
| 2025-11-06 10:30 | Assigned to developer | Team Lead |
| 2025-11-06 11:00 | Root cause identified | Developer |
| 2025-11-06 13:00 | Fix implemented | Developer |
| 2025-11-06 14:00 | Regression test written | Developer |
| 2025-11-06 15:00 | Code review approved | Tech Lead |
| 2025-11-06 16:00 | Merged to main | Developer |
| 2025-11-07 09:00 | Deployed to staging | DevOps |
| 2025-11-07 10:00 | Verified in staging | QA |
| 2025-11-07 14:00 | Deployed to production | DevOps |
| 2025-11-07 15:00 | Verified in production | QA |
| 2025-11-07 15:30 | Bug closed | Team Lead |

**Total Time:** 1 day 6.5 hours (detection to production fix)

---

## 🏷️ Attachments

### Screenshots
- `bug-xxx-login-error.png` - Error message screenshot
- `bug-xxx-console-log.png` - Browser console error

### Logs
```
2025-11-06 09:15:42 ERROR [EmailValidator] Email validation failed for user+test@example.com
  at EmailValidator.validate(EmailValidator.java:42)
  at LoginService.authenticate(LoginService.java:87)
```

### Test Data
- Test user email: `user+test@example.com`
- Test user password: `ValidPassword123!`

---

**Bug Template Version:** 1.0  
**Last Updated:** 2025-11-06  
**Template:** backlog/templates/bug.md
