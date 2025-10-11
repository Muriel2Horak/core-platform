# Naming Convention Linters

Automated linters to enforce naming conventions across the core-platform monorepo.

## Installation

```bash
cd tools/naming-lint
npm install
```

## Usage

### Lint All
```bash
npm run lint:all
```

### Individual Lints

#### 1. Metamodel Lint
Validates metamodel JSON files:
- Entity names: PascalCase singular
- Field names: camelCase
- Required fields: id, tenantId, createdAt, updatedAt, version

```bash
npm run lint:metamodel
```

**Example Output**:
```
Metamodel Naming Lint
=====================
✗ 2 error(s):
  backend/src/main/resources/metamodel/users.json
    File name "users" must be PascalCase (e.g., User, UserDirectory)
  backend/src/main/resources/metamodel/User.json
    Missing required field: "tenantId"

Checked: 5 files
```

#### 2. REST API Lint
Validates Spring controllers:
- Path: kebab-case plural
- Controller name matches path

```bash
npm run lint:api
```

**Example Output**:
```
REST API Naming Lint
====================
✗ 1 error(s):
  backend/src/main/java/.../api/UserController.java
    Resource "user" must be plural (e.g., users, user-directories)

Checked: 8 files
```

#### 3. Kafka Topic Lint
Validates Kafka topic names:
- Pattern: `product.context.entity.event`
- All segments: kebab-case
- Suffixes: `-retry`, `-dlq`

```bash
npm run lint:kafka
```

**Example Output**:
```
Kafka Topic Naming Lint
=======================
✗ 1 error(s):
  backend/src/main/java/.../kafka/UserEventProducer.java
    Topic "core.UserManagement.user.created" must follow pattern: product.context.entity.event (kebab-case, dot-separated)

Checked: 3 files
```

#### 4. Database Migration Lint
Validates Flyway migrations:
- File name: `V{YYYYMMDDHHMM}__{description}.sql`
- Tables: snake_case, must have `tenant_id`
- Columns: snake_case

```bash
npm run lint:db
```

**Example Output**:
```
Database Migration Naming Lint
==============================
✗ 2 error(s):
  backend/src/main/resources/db/migration/create_users.sql
    Migration file name must match pattern: V{YYYYMMDDHHMM}__{description}.sql
  backend/src/main/resources/db/migration/V20251011100000__create_users_table.sql
    Table "users" must have a tenant_id column (RLS requirement)

Checked: 12 files
```

## CI Integration

Add to `.github/workflows/naming-lint.yml`:

```yaml
name: Naming Lint

on:
  pull_request:
    branches: [main, develop]

jobs:
  naming-lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
      
      - name: Install dependencies
        working-directory: tools/naming-lint
        run: npm ci
      
      - name: Run naming lints
        working-directory: tools/naming-lint
        run: npm run lint:all
```

## Exit Codes

- `0`: All checks passed ✅
- `1`: One or more errors found ❌

## Naming Rules Reference

See [docs/NAMING_GUIDE.md](../../docs/NAMING_GUIDE.md) for complete naming conventions.

### Quick Reference

| Layer | Convention | Example |
|-------|------------|---------|
| Entity | PascalCase singular | `User` |
| DB Table | snake_case plural | `users` |
| DB Column | snake_case singular | `first_name` |
| REST URL | kebab-case plural | `/api/users` |
| JSON Key | camelCase | `firstName` |
| Cube | PascalCase plural | `Users` |
| Kafka Topic | kebab-case dot-separated | `core.user-management.user.created` |
| Prometheus | snake_case with suffix | `user_created_total` |

## Development

### Project Structure
```
tools/naming-lint/
├── package.json
├── README.md
├── src/
│   ├── lint-metamodel.js    # Metamodel linter
│   ├── lint-api.js           # REST API linter
│   ├── lint-kafka.js         # Kafka topic linter
│   ├── lint-db.js            # DB migration linter
│   └── utils/
│       ├── casing.js         # Casing utilities
│       └── reporter.js       # Results reporter
└── __tests__/
    └── *.test.js             # Unit tests
```

### Adding New Lints

1. Create `src/lint-{name}.js`
2. Import `Reporter` and casing utils
3. Add `lint:{name}` script to `package.json`
4. Update `lint:all` script
5. Add documentation to this README

### Running Tests

```bash
npm test
```

## Troubleshooting

### No files found
Linters search from repo root (`../..` relative to `tools/naming-lint`). Ensure you run from the correct directory.

### False positives
Update allowlists in respective lint files:
- `lint-db.js`: `TENANT_ID_ALLOWLIST` for system tables
- `lint-api.js`: Skip specific paths if needed

## Maintenance

- **Owner**: Platform Team
- **Questions**: platform-team@muriel.cz
- **Last Updated**: 2025-10-11
