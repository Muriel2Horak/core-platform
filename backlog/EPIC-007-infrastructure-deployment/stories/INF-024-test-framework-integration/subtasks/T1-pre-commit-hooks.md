# T1: Configure Pre-Commit Hooks & Linters

**Parent Story:** INF-024 Test Framework Integration  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 3 hours  
**Owner:** DevOps

---

## 🎯 Objective

Enforce code quality at commit-time: ESLint, Prettier, Checkstyle, TypeScript checks.

---

## 📋 Tasks

### 1. Install Husky & Lint-Staged

```bash
# Frontend hooks
cd frontend
npm install --save-dev husky lint-staged

# Initialize Husky
npx husky init
```

### 2. Configure Lint-Staged

**File:** `frontend/package.json`

```json
{
  "lint-staged": {
    "*.{ts,tsx}": [
      "eslint --fix",
      "prettier --write"
    ],
    "*.{json,md,yml}": [
      "prettier --write"
    ]
  },
  "scripts": {
    "prepare": "husky || true"
  }
}
```

### 3. Create Pre-Commit Hook

**File:** `frontend/.husky/pre-commit`

```bash
#!/bin/sh
. "$(dirname "$0")/_/husky.sh"

echo "🔍 Running pre-commit checks..."

# Lint-staged (frontend)
cd frontend && npx lint-staged

# TypeScript check
echo "🔧 TypeScript type checking..."
npm run typecheck

# Backend Checkstyle
echo "☕ Java Checkstyle..."
cd ../backend && ./mvnw checkstyle:check
```

### 4. Configure Checkstyle (Backend)

**File:** `backend/checkstyle.xml`

```xml
<?xml version="1.0"?>
<!DOCTYPE module PUBLIC
  "-//Checkstyle//DTD Checkstyle Configuration 1.3//EN"
  "https://checkstyle.org/dtds/configuration_1_3.dtd">

<module name="Checker">
  <property name="severity" value="error"/>
  
  <module name="TreeWalker">
    <!-- Naming conventions -->
    <module name="ConstantName"/>
    <module name="LocalFinalVariableName"/>
    <module name="LocalVariableName"/>
    <module name="MemberName"/>
    <module name="MethodName"/>
    <module name="PackageName"/>
    <module name="ParameterName"/>
    <module name="StaticVariableName"/>
    <module name="TypeName"/>
    
    <!-- Imports -->
    <module name="AvoidStarImport"/>
    <module name="UnusedImports"/>
    <module name="RedundantImport"/>
    
    <!-- Code quality -->
    <module name="EmptyBlock"/>
    <module name="NeedBraces"/>
    <module name="SimplifyBooleanExpression"/>
    <module name="SimplifyBooleanReturn"/>
    
    <!-- Whitespace -->
    <module name="WhitespaceAfter"/>
    <module name="WhitespaceAround"/>
  </module>
  
  <!-- File checks -->
  <module name="FileLength">
    <property name="max" value="500"/>
  </module>
  <module name="LineLength">
    <property name="max" value="120"/>
  </module>
</module>
```

**File:** `backend/pom.xml`

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-checkstyle-plugin</artifactId>
  <version>3.3.1</version>
  <configuration>
    <configLocation>checkstyle.xml</configLocation>
    <failOnViolation>true</failOnViolation>
    <violationSeverity>warning</violationSeverity>
  </configuration>
  <executions>
    <execution>
      <goals>
        <goal>check</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```

### 5. Configure ESLint

**File:** `frontend/.eslintrc.cjs`

```javascript
module.exports = {
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:react-hooks/recommended',
    'prettier'
  ],
  parser: '@typescript-eslint/parser',
  plugins: ['@typescript-eslint', 'react-refresh'],
  rules: {
    'react-refresh/only-export-components': 'warn',
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    '@typescript-eslint/explicit-function-return-type': 'off',
    'no-console': ['warn', { allow: ['warn', 'error'] }]
  }
}
```

### 6. Configure Prettier

**File:** `frontend/.prettierrc.json`

```json
{
  "semi": true,
  "singleQuote": true,
  "tabWidth": 2,
  "trailingComma": "es5",
  "printWidth": 100,
  "arrowParens": "avoid"
}
```

### 7. Test Pre-Commit Hooks

```bash
# Create intentionally bad code
echo "const x=1;console.log('test')" > frontend/src/bad.ts

# Try to commit (should fail)
git add frontend/src/bad.ts
git commit -m "test: Bad code"

# Expected output:
# ❌ ESLint errors found
# ❌ Prettier formatting needed
# ❌ Commit aborted

# Fix and retry
cd frontend
npm run lint -- --fix
npm run format

git add frontend/src/bad.ts
git commit -m "test: Fixed code"
# ✅ Pre-commit checks passed
```

---

## ✅ Acceptance Criteria

- [ ] Husky pre-commit hooks installed and working
- [ ] ESLint runs on staged `.ts/.tsx` files
- [ ] Prettier auto-formats staged files
- [ ] TypeScript type checking runs on commit
- [ ] Checkstyle runs on Java files
- [ ] Bad code commit rejected with clear error messages
- [ ] Hooks can be bypassed with `--no-verify` (emergency)

---

## 🔗 Dependencies

- **BLOCKS:** T2 (Testcontainers), T3 (Coverage Thresholds)
- Node.js 20+, Maven 3.9+
