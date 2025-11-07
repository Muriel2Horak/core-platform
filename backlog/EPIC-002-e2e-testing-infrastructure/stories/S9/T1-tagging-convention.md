# T1: Tagging Convention Documentation

**Story:** [S9: Test Tagging System](../S9.md)  
**Task:** T1/4  
**Effort:** ~1h  
**LOC:** ~50

---

## 🎯 Objective

Definovat a zdokumentovat tagging convention pro mapování testů na User Stories.

---

## 📋 Requirements

### Tag Format

```
@CORE-XXX - mapuje na User Story CORE-XXX
```

**Examples:**
```typescript
// Playwright
test.describe('Login Flow @CORE-123', () => {
  test('should login with valid credentials @E2E-LOGIN-001', async ({ page }) => {
    // Test
  });
});

// JUnit
@Test
@UserStory("CORE-123")
void shouldCreateUser() { }
```

---

## 💻 Implementation

**File:** `docs/testing-tagging-conventions.md`

### Content Sections
1. **Why Tag Tests?** - Traceability, coverage tracking
2. **Tag Format** - @CORE-XXX rules
3. **Playwright Tags** - test.describe, test tags
4. **JUnit Annotations** - @UserStory usage
5. **Validation** - Pre-commit hooks
6. **Examples** - All test types

---

## ✅ Acceptance Criteria

- [ ] Documentation created
- [ ] Tag format defined (@CORE-XXX)
- [ ] Examples for E2E, Unit, Integration
- [ ] Validation rules documented
- [ ] Reviewed and approved

---

**Next:** [T2: Playwright Tag Support](./T2-playwright-tags.md)
