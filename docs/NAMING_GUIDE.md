# Naming Conventions Guide - Core Platform

**Version**: 1.0.0  
**Status**: Active Standard  
**Last Updated**: 2025-10-11

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Entity & Domain Model](#entity--domain-model)
3. [Database](#database)
4. [REST API](#rest-api)
5. [JSON (Backend & Frontend)](#json-backend--frontend)
6. [Cube.js Analytics](#cubejs-analytics)
7. [Kafka Topics & Events](#kafka-topics--events)
8. [Prometheus Metrics](#prometheus-metrics)
9. [Java Code](#java-code)
10. [JavaScript/TypeScript Code](#javascripttypescript-code)
11. [Validation & Linting](#validation--linting)

---

## Overview

This guide establishes **consistent naming conventions** across all layers of the core-platform monorepo. Consistent naming improves:
- **Code readability** & maintainability
- **Cross-team collaboration** (BE/FE/DevOps)
- **Automated tooling** (code generation, linting)
- **Onboarding** new developers

### Key Principles
1. **Singular for entities, plural for collections**
2. **Case consistency within each layer**
3. **Predictable transformations** (e.g., `User` → `users` → `/users` → `user_id`)
4. **Self-documenting** names (avoid abbreviations unless standard)

---

## Entity & Domain Model

**Convention**: `PascalCase` (singular)

### Rules
- Java classes: `User`, `Company`, `Order`, `UserDirectory`
- Metamodel JSON files: `User.json`, `Company.json`
- Single responsibility: One entity = one concept

### Examples
✅ **Good**:
```
User
Company
Order
UserDirectory
TenantSettings
```

❌ **Bad**:
```
Users          // Plural
user           // Lowercase
usr            // Abbreviated
UserClass      // Redundant suffix
```

### Compound Names
- Use `PascalCase` without separators
- Prefer clarity over brevity

**Examples**:
- `UserDirectory` (not `UserDir`, `User_Directory`)
- `OrderLineItem` (not `OrderLine`, `OrderItem`)
- `TenantSettings` (not `TenantConfig`, `TenantPrefs`)

---

## Database

**Tables**: `snake_case` (plural)  
**Columns**: `snake_case` (singular)

### Table Names

**Rule**: Plural form of entity name in `snake_case`

```
User         → users
Company      → companies
Order        → orders
UserDirectory → user_directories
TenantSettings → tenant_settings
```

**Multi-word transformations**:
```
PascalCase      →  snake_case (plural)
─────────────────────────────────────
User            →  users
UserDirectory   →  user_directories
OrderLineItem   →  order_line_items
TenantSettings  →  tenant_settings
```

### Column Names

**Rule**: `snake_case` (singular)

```
id                  // Primary key (always)
tenant_id           // Partition key (always for RLS)
created_at          // Timestamp
updated_at          // Timestamp
created_by_user_id  // FK with descriptive prefix
is_active           // Boolean prefix: is_, has_, can_
user_count          // Aggregates suffix with type
```

### Mandatory Columns (Multi-Tenant)
Every table MUST have:
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `tenant_id UUID NOT NULL` (except system tables)
- `created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()`
- `updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()`
- `version INTEGER DEFAULT 1` (for optimistic locking)

### Foreign Keys
**Pattern**: `{referenced_entity}_id`

```
company_id          // References companies(id)
created_by_user_id  // References users(id)
parent_group_id     // References groups(id)
```

### Indexes
**Pattern**: `idx_{table}_{columns}`

```
idx_users_tenant                    // Single column
idx_users_tenant_created            // Composite
idx_orders_company_status           // Composite
idx_user_directories_tenant_active  // Composite with boolean
```

---

## REST API

**URLs**: `kebab-case` (plural for collections)  
**HTTP Methods**: Standard REST verbs

### URL Structure

**Pattern**: `/api/{resource-plural}/{id?}/{sub-resource-plural?}`

```
GET    /api/users                    // List users
GET    /api/users/{id}                // Get user by ID
POST   /api/users                    // Create user
PATCH  /api/users/{id}                // Update user (partial)
DELETE /api/users/{id}                // Delete user

GET    /api/companies/{id}/users     // Nested resource
POST   /api/entities/users/bulk-update // Bulk operation
```

### Resource Names

**Rule**: Kebab-case plural

```
Entity Class    →  REST URL
────────────────────────────
User            →  /api/users
Company         →  /api/companies
Order           →  /api/orders
UserDirectory   →  /api/user-directories
TenantSettings  →  /api/tenant-settings
```

### Query Parameters
**Rule**: `camelCase`

```
GET /api/users?sortBy=createdAt&pageSize=50&includeInactive=false
GET /api/orders?companyId=123&status=pending
```

### Controller Naming (Java)

**Pattern**: `{EntityPlural}Controller`

```
@RestController
@RequestMapping("/api/users")
public class UsersController { ... }

@RestController
@RequestMapping("/api/user-directories")
public class UserDirectoriesController { ... }
```

---

## JSON (Backend & Frontend)

**Convention**: `camelCase` for all JSON keys

### Request/Response Bodies

```json
{
  "id": "uuid",
  "tenantId": "uuid",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john@example.com",
  "createdAt": "2025-10-11T10:00:00Z",
  "updatedAt": "2025-10-11T10:00:00Z",
  "isActive": true,
  "userCount": 42
}
```

### Java DTOs

```java
public record UserDTO(
    UUID id,
    UUID tenantId,
    String firstName,    // camelCase
    String lastName,
    String email,
    Instant createdAt,
    Instant updatedAt,
    Boolean isActive
) {}
```

### TypeScript Interfaces

```typescript
interface User {
  id: string;
  tenantId: string;
  firstName: string;
  lastName: string;
  email: string;
  createdAt: string;   // ISO 8601
  updatedAt: string;
  isActive: boolean;
}
```

---

## Cube.js Analytics

**Cubes**: `PascalCase` (plural)  
**Measures/Dimensions**: `camelCase`

### Cube Names

**Rule**: Plural PascalCase matching table name

```
Table           →  Cube Name
─────────────────────────────
users           →  Users
companies       →  Companies
user_directories →  UserDirectories
```

### File Names

**Pattern**: `{CubeName}.js`

```
docker/cube/schema/Users.js
docker/cube/schema/Companies.js
docker/cube/schema/UserDirectories.js
```

### Cube Definition

```javascript
cube('Users', {  // Plural PascalCase
  sql: `SELECT * FROM users WHERE tenant_id = ${SECURITY_CONTEXT.tenant_id.unsafeValue()}`,
  
  measures: {
    count: {      // camelCase
      type: 'count'
    },
    avgAge: {     // camelCase
      type: 'avg',
      sql: 'age'
    }
  },
  
  dimensions: {
    id: {         // camelCase
      sql: 'id',
      type: 'string',
      primaryKey: true
    },
    firstName: {  // camelCase (matches JSON)
      sql: 'first_name',
      type: 'string'
    },
    createdAt: {  // camelCase
      sql: 'created_at',
      type: 'time'
    }
  },
  
  preAggregations: {
    usersByDay: { // camelCase
      type: 'rollup',
      // ...
    }
  }
});
```

### Joins

```javascript
joins: {
  Companies: {   // Target cube (PascalCase plural)
    sql: `${Users.tenantId} = ${Companies.tenantId} AND ${Users.companyId} = ${Companies.id}`,
    relationship: 'belongsTo'
  }
}
```

---

## Kafka Topics & Events

**Topics**: `product.context.entity.event` in `kebab-case`  
**Suffixes**: `-retry`, `-dlq`

### Topic Naming Pattern

**Structure**: `{product}.{bounded-context}.{entity}.{event-type}`

```
core.user-management.user.created
core.user-management.user.updated
core.user-management.user.deleted
core.company-management.company.created
core.reporting.preagg.refresh
core.entity.lifecycle.mutating
core.entity.lifecycle.mutated
```

### Retry & DLQ Topics

```
core.user-management.user.created-retry
core.user-management.user.created-dlq
core.entity.lifecycle.mutating-retry
core.entity.lifecycle.mutating-dlq
```

### Event Payload (JSON)

**Keys**: `camelCase`

```json
{
  "eventId": "uuid",
  "eventType": "user.created",
  "eventTime": "2025-10-11T10:00:00Z",
  "tenantId": "uuid",
  "aggregateId": "uuid",
  "aggregateType": "User",
  "payload": {
    "userId": "uuid",
    "firstName": "John",
    "email": "john@example.com"
  },
  "metadata": {
    "correlationId": "uuid",
    "causationId": "uuid",
    "userId": "uuid"
  }
}
```

### Consumer Group IDs

**Pattern**: `{service-name}-{topic-name}-consumer`

```
backend-user-created-consumer
reporting-worker-preagg-refresh-consumer
audit-log-entity-lifecycle-consumer
```

---

## Prometheus Metrics

**Convention**: `snake_case` with standard suffixes

### Metric Naming

**Pattern**: `{namespace}_{subsystem}_{metric_name}_{unit}`

```
reporting_query_duration_seconds         // Histogram
reporting_query_total                    // Counter
reporting_circuit_breaker_state          // Gauge (0/0.5/1)
reporting_preagg_build_duration_seconds  // Histogram
reporting_preagg_build_failures_total    // Counter
kafka_consumer_lag_records               // Gauge
kafka_producer_record_send_total         // Counter
http_server_requests_seconds             // Histogram (Spring Boot Actuator)
```

### Suffixes (Required)

| Unit | Suffix | Example |
|------|--------|---------|
| Seconds | `_seconds` | `query_duration_seconds` |
| Bytes | `_bytes` | `response_size_bytes` |
| Total count | `_total` | `requests_total` |
| Ratio (0-1) | `_ratio` | `error_ratio` |
| Percentage (0-100) | none | `cpu_usage` |

### Labels

**Rule**: `snake_case`

```
reporting_query_total{tenant_id="t1", entity_type="User", status="success"}
kafka_consumer_lag_records{topic="core.user.created", partition="0"}
```

---

## Java Code

### Packages
**Convention**: `lowercase` (no separators)

```
cz.muriel.core.reporting.api
cz.muriel.core.reporting.service
cz.muriel.core.reporting.security
cz.muriel.core.usermanagement.domain
cz.muriel.core.kafka.producer
```

### Classes

| Type | Convention | Example |
|------|------------|---------|
| Entity | `PascalCase` (singular) | `User`, `Company` |
| Controller | `{EntityPlural}Controller` | `UsersController` |
| Service | `{Entity}Service` | `UserService` |
| Repository | `{Entity}Repository` | `UserRepository` |
| DTO | `{Entity}DTO` | `UserDTO` |
| Request | `{Action}{Entity}Request` | `CreateUserRequest` |
| Response | `{Action}{Entity}Response` | `CreateUserResponse` |
| Exception | `{Entity}{Error}Exception` | `UserNotFoundException` |

### Methods
**Convention**: `camelCase` (verb + noun)

```java
public User findById(UUID id) { ... }
public List<User> findByTenantId(UUID tenantId) { ... }
public User createUser(CreateUserRequest request) { ... }
public void deleteUser(UUID id) { ... }
public boolean isUserActive(UUID id) { ... }
```

### Constants
**Convention**: `UPPER_SNAKE_CASE`

```java
public static final int MAX_PAGE_SIZE = 1000;
public static final String DEFAULT_SORT_FIELD = "createdAt";
public static final Duration CACHE_TTL = Duration.ofMinutes(5);
```

### Variables
**Convention**: `camelCase`

```java
UUID userId = ...;
String firstName = ...;
Instant createdAt = ...;
boolean isActive = ...;
```

---

## JavaScript/TypeScript Code

### Files
**Components**: `PascalCase.tsx`  
**Utilities**: `camelCase.ts`  
**Hooks**: `use{Name}.ts`

```
components/Reporting/ExplorerGrid.tsx
components/Reporting/ChartPanel.tsx
utils/formatDate.ts
hooks/usePresence.ts
services/apiClient.ts
```

### Functions & Variables
**Convention**: `camelCase`

```typescript
function fetchUsers(tenantId: string): Promise<User[]> { ... }
const userId = '...';
const isActive = true;
```

### React Components
**Convention**: `PascalCase`

```typescript
export function ExplorerGrid({ entityName, query }: Props) { ... }
export const ChartPanel = ({ data, chartType }: Props) => { ... }
```

### Constants
**Convention**: `UPPER_SNAKE_CASE`

```typescript
const MAX_RETRIES = 3;
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;
const DEFAULT_PAGE_SIZE = 50;
```

---

## Validation & Linting

### Naming Lints (CI)

We provide automated linters in `tools/naming-lint/`:

#### 1. `lint:metamodel`
Validates metamodel JSON files:
- Entity names are PascalCase singular
- Field names are camelCase
- Required fields exist (id, tenantId, createdAt, updatedAt)

```bash
npm run lint:metamodel
# ✓ User.json - OK
# ✗ users.json - FAIL: Entity name must be PascalCase singular
```

#### 2. `lint:api`
Validates Spring controllers:
- Base path is kebab-case plural
- Controller class name matches path

```bash
npm run lint:api
# ✓ UsersController (@RequestMapping("/api/users")) - OK
# ✗ UserController (@RequestMapping("/api/user")) - FAIL: Path must be plural
```

#### 3. `lint:kafka`
Validates Kafka topic names:
- Pattern: `{product}.{context}.{entity}.{event}`
- All parts are kebab-case

```bash
npm run lint:kafka
# ✓ core.user-management.user.created - OK
# ✗ core.UserManagement.user.created - FAIL: Context must be kebab-case
```

#### 4. `lint:db`
Validates database migrations:
- Migration file: `V{YYYYMMDDHHMM}__{description}.sql`
- Tables have `tenant_id` (except allowlist)

```bash
npm run lint:db
# ✓ V20251011100000__create_users_table.sql - OK
# ✗ create_users.sql - FAIL: Must follow V{timestamp}__ pattern
```

### CI Integration

`.github/workflows/naming-lint.yml`:
```yaml
- name: Run naming lints
  run: |
    npm run lint:metamodel
    npm run lint:api
    npm run lint:kafka
    npm run lint:db
```

**Merge gate**: All lints must pass ✅

---

## Migration & Compatibility

### Breaking Changes
When renaming existing resources, follow this process:

#### 1. Add Alias (Deprecation Period)
```java
@RestController
@RequestMapping("/api/users")
public class UsersController {
    // New name (preferred)
}

@RestController
@RequestMapping("/api/user")  // Old name (deprecated)
@Deprecated(since = "2.0.0", forRemoval = true)
public class UserControllerAlias extends UsersController {
    // Redirect to new controller
}
```

#### 2. Update Nginx (301 Redirect)
```nginx
location /api/user {
    return 301 /api/users$request_uri;
}
```

#### 3. Update Documentation
Add to `CHANGELOG.md`:
```markdown
### [2.0.0] - 2025-10-11
#### Deprecated
- `/api/user` → Use `/api/users` (will be removed in 3.0.0)
```

#### 4. Remove After Deprecation Period
- Monitor usage metrics for 2 releases
- Remove alias in next major version

---

## Examples by Layer

### Full Stack Example: "User" Entity

| Layer | Convention | Example |
|-------|------------|---------|
| **Domain Entity** | PascalCase singular | `User` |
| **Database Table** | snake_case plural | `users` |
| **Database Columns** | snake_case singular | `id`, `tenant_id`, `first_name`, `created_at` |
| **REST URL** | kebab-case plural | `/api/users` |
| **Controller** | PascalCase + Controller | `UsersController` |
| **JSON Key** | camelCase | `firstName`, `createdAt` |
| **Cube.js Cube** | PascalCase plural | `Users` |
| **Cube.js Dimension** | camelCase | `firstName`, `createdAt` |
| **Kafka Topic** | kebab-case | `core.user-management.user.created` |
| **Kafka Key** | camelCase | `userId` |
| **Prometheus Metric** | snake_case | `user_created_total` |

### Full Stack Example: "UserDirectory" (Compound)

| Layer | Example |
|-------|---------|
| **Domain** | `UserDirectory` |
| **DB Table** | `user_directories` |
| **DB Column (FK)** | `user_directory_id` |
| **REST URL** | `/api/user-directories` |
| **Controller** | `UserDirectoriesController` |
| **JSON** | `userDirectoryId` |
| **Cube** | `UserDirectories` |
| **Cube Dimension** | `directoryName` |
| **Kafka** | `core.user-management.user-directory.created` |
| **Prometheus** | `user_directory_sync_duration_seconds` |

---

## Anti-Patterns

### ❌ Avoid These

1. **Mixed Case in URLs**
   ```
   ❌ /api/Users
   ❌ /api/user_directories
   ✅ /api/users
   ✅ /api/user-directories
   ```

2. **Inconsistent Pluralization**
   ```
   ❌ GET /api/user (should be /users)
   ❌ POST /api/users/{id}/role (should be /roles)
   ✅ GET /api/users
   ✅ POST /api/users/{id}/roles
   ```

3. **Abbreviations Without Context**
   ```
   ❌ usrDir, tenantCfg, ordLnItm
   ✅ userDirectory, tenantSettings, orderLineItem
   ```

4. **Redundant Suffixes**
   ```
   ❌ UserEntity, UserDTO (in entity package)
   ✅ User (entity package), UserDTO (api package)
   ```

5. **Snake Case in Java**
   ```
   ❌ public void create_user() { ... }
   ✅ public void createUser() { ... }
   ```

---

## Checklist for New Features

When adding a new entity, verify:

- [ ] Entity class: `PascalCase` singular (e.g., `Order`)
- [ ] DB table: `snake_case` plural (e.g., `orders`)
- [ ] DB columns: `snake_case` singular (e.g., `order_status`)
- [ ] DB migration: `V{YYYYMMDDHHMM}__create_orders_table.sql`
- [ ] Mandatory columns: `id`, `tenant_id`, `created_at`, `updated_at`, `version`
- [ ] REST URL: `kebab-case` plural (e.g., `/api/orders`)
- [ ] Controller: `{EntityPlural}Controller` (e.g., `OrdersController`)
- [ ] JSON keys: `camelCase` (e.g., `orderStatus`)
- [ ] Cube: `PascalCase` plural (e.g., `Orders`)
- [ ] Cube measures/dimensions: `camelCase` (e.g., `totalAmount`)
- [ ] Kafka topic: `core.{context}.{entity-kebab}.{event}` (e.g., `core.order-management.order.created`)
- [ ] Prometheus metrics: `snake_case` with suffix (e.g., `order_created_total`)

---

## Tools & Resources

### Naming Lint Tools
- **Location**: `tools/naming-lint/`
- **Install**: `cd tools/naming-lint && npm install`
- **Run All**: `npm run lint:all`
- **CI**: Auto-runs on every PR

### Conversion Utilities
```bash
# PascalCase → snake_case
echo "UserDirectory" | sed 's/\([A-Z]\)/_\L\1/g' | sed 's/^_//'
# Output: user_directory

# Pluralize
echo "User" | sed 's/$/s/'
# Output: Users
```

### Editor Plugins
- **IntelliJ IDEA**: CheckStyle plugin (custom rules)
- **VS Code**: ESLint + custom naming rules

---

## References

- **REST API Design**: https://restfulapi.net/resource-naming/
- **Cube.js Naming**: https://cube.dev/docs/schema/fundamentals/naming
- **Kafka Naming**: https://cnr.sh/essays/how-paint-bike-shed-kafka-topic-naming-conventions
- **Prometheus Naming**: https://prometheus.io/docs/practices/naming/

---

**Maintained By**: Platform Team  
**Questions**: platform-team@muriel.cz  
**Last Review**: 2025-10-11
