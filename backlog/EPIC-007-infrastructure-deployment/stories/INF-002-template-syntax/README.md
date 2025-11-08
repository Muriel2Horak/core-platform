# INF-002: Template Syntax Standardization

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** HIGH  
**Effort:** 3 dny, ~600 LOC  
**Owner:** Platform Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current State - 3 Different Substitution Mechanisms:**

```bash
# 1. envsubst (Bash)
envsubst < realm-admin.template.json > realm-admin.json
# Syntax: ${VARIABLE} or $VARIABLE

# 2. Docker Compose (native)
environment:
  - DATABASE_URL=${DATABASE_URL}
# Syntax: ${VARIABLE}

# 3. Spring Boot (Java)
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://db:5432/core}
# Syntax: ${VARIABLE:default}
```

**Confusion:**
- Developer neví který syntax použít kde
- Různé escape rules (`$$` vs `\$`)
- Hard to validate (každý mechanismus jiný error)

### Goal

**Standardize on envsubst syntax everywhere:**

```bash
# ALL templates use envsubst:
${VARIABLE}              # Required variable
${VARIABLE:-default}     # Optional with fallback

# Generated files use native syntax:
# - docker-compose.yml → Docker native ${VAR}
# - application.yml → Spring ${VAR:default}
```

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **Convert All Templates to envsubst**
   - docker-compose.template.yml
   - realm-admin.template.json
   - nginx-ssl.conf.template
   - application.yml templates

2. ✅ **Validation Script**
   - `scripts/validate-template-syntax.sh`
   - Check: všechny `${VAR}` mají hodnotu v .env
   - Report: chybějící nebo nepoužité variables

3. ✅ **Documentation**
   - Template syntax guide: `docs/templates.md`
   - Migration guide pro existing templates

### Implementation

**File:** `scripts/templates/validate-syntax.sh`

```bash
#!/bin/bash
set -euo pipefail

# Find all ${VARIABLE} placeholders in templates
TEMPLATE_VARS=$(grep -oP '\$\{[A-Z_]+\}' *.template* | sort -u)

# Check against .env
source .env
MISSING=()
for VAR in $TEMPLATE_VARS; do
    VAR_NAME=${VAR:2:-1}  # Strip ${ and }
    if [[ -z "${!VAR_NAME:-}" ]]; then
        MISSING+=($VAR_NAME)
    fi
done

if [ ${#MISSING[@]} -eq 0 ]; then
    echo "✅ All template variables defined"
    exit 0
else
    echo "❌ Missing variables:"
    printf '%s\n' "${MISSING[@]}"
    exit 1
fi
```

**Effort:** 3 dny  
**LOC:** ~600  
**Blocks:** INF-001 (uses output)

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
