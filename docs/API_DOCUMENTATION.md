# API Documentation Guide

**Version:** 1.0.0  
**Last Updated:** 2025-10-12  
**Status:** ✅ Production Ready

---

## 📚 Overview

The Core Platform API provides comprehensive endpoints for user management, tenant administration, role-based access control, and analytics integration. All endpoints require JWT authentication and support multi-tenancy.

---

## 🔗 Access Points

### Local Development
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api/docs
- **OpenAPI YAML**: http://localhost:8080/api/docs.yaml

### Production
- **Swagger UI**: https://api.core-platform.local/swagger-ui.html
- **OpenAPI JSON**: https://api.core-platform.local/api/docs

---

## 🔐 Authentication

All API endpoints require JWT Bearer authentication via Keycloak.

### Obtaining a Token

```bash
curl -X POST https://test-tenant.core-platform.local/realms/test-tenant/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=backend" \
  -d "username=admin@test.com" \
  -d "password=admin"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_expires_in": 1800,
  "refresh_token": "...",
  "token_type": "Bearer",
  "scope": "email profile"
}
```

### Using Token in Requests

**HTTP Header:**
```
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Swagger UI:**
1. Click **Authorize** button (top right)
2. Paste `access_token` value (without "Bearer " prefix)
3. Click **Authorize**
4. Try API endpoints

---

## 📑 API Categories

### 1. User Management (`/api/users`)

CRUD operations for user accounts, role assignments, and password management.

**Required Permission:** `CORE_ROLE_USER_MANAGER` or `CORE_ROLE_ADMIN`

**Endpoints:**
- `GET /api/users` - Search users with filters
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create new user
- `PUT /api/users/{id}` - Update user profile
- `PUT /api/users/{id}/password` - Reset password
- `DELETE /api/users/{id}` - Delete user
- `GET /api/users/{id}/roles` - Get user roles
- `POST /api/users/{id}/roles` - Assign role to user
- `DELETE /api/users/{id}/roles/{roleName}` - Remove role from user
- `GET /api/users/{id}/groups` - Get user groups

**Example: Search Users**
```bash
curl -X GET "http://localhost:8080/api/users?email=admin&first=0&max=10" \
  -H "Authorization: Bearer $TOKEN"
```

**Example: Create User**
```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "email": "john.doe@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "enabled": true
  }'
```

---

### 2. Tenant Management (`/api/tenants`)

Manage tenants, create tenant-specific realms, and configure tenant settings.

**Required Permission:** `CORE_ROLE_ADMIN`

**Endpoints:**
- `GET /api/tenants` - List all tenants
- `GET /api/tenants/me` - Get current tenant
- `POST /api/tenants` - Create new tenant
- `PUT /api/tenants/{id}` - Update tenant
- `DELETE /api/tenants/{id}` - Delete tenant

**Example: Get Current Tenant**
```bash
curl -X GET "http://localhost:8080/api/tenants/me" \
  -H "Authorization: Bearer $TOKEN"
```

---

### 3. Role Management (`/api/roles`)

CRUD operations for roles and permission management.

**Required Permission:** `CORE_ROLE_ADMIN`

**Endpoints:**
- `GET /api/roles` - List all roles
- `GET /api/roles/{id}` - Get role by ID
- `POST /api/roles` - Create new role
- `PUT /api/roles/{id}` - Update role
- `DELETE /api/roles/{id}` - Delete role
- `GET /api/roles/{id}/permissions` - Get role permissions
- `POST /api/roles/{id}/permissions` - Add permission to role

---

### 4. Cube.js Model Generation (`/api/cube/modelgen`)

Generate Cube.js schemas from YAML definitions for analytics.

**Required Permission:** `CORE_ROLE_ADMIN`

**Endpoints:**
- `POST /api/cube/modelgen/generate` - Generate Cube.js model from YAML
- `GET /api/cube/modelgen/schema` - Get generated schema
- `POST /api/cube/modelgen/validate` - Validate YAML definition

**Example: Generate Model**
```bash
curl -X POST "http://localhost:8080/api/cube/modelgen/generate" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "yamlContent": "cubes:\n  - name: Users\n    sql: SELECT * FROM users",
    "outputPath": "/cube/schema/Users.js"
  }'
```

---

### 5. Cube.js Pre-Aggregations (`/api/cube/preagg`)

Manage pre-aggregation refresh workflows.

**Required Permission:** `CORE_ROLE_ADMIN`

**Endpoints:**
- `POST /api/cube/preagg/refresh` - Trigger pre-aggregation refresh
- `GET /api/cube/preagg/status` - Get refresh status
- `GET /api/cube/preagg/metadata` - List all pre-aggregations

**Example: Trigger Refresh**
```bash
curl -X POST "http://localhost:8080/api/cube/preagg/refresh" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cubeName": "Users",
    "preAggName": "main"
  }'
```

---

### 6. Grafana Integration (`/api/monitoring/proxy`)

Proxy for Grafana API with multi-tenant isolation.

**Required Permission:** Based on Grafana organization membership

**Endpoints:**
- `GET /api/monitoring/grafana/*` - Proxy to Grafana API
- `POST /api/monitoring/grafana/*` - Proxy POST requests
- `PUT /api/monitoring/grafana/*` - Proxy PUT requests
- `DELETE /api/monitoring/grafana/*` - Proxy DELETE requests

---

### 7. Presence Tracking (`/api/presence`)

Real-time user presence and activity tracking.

**Required Permission:** Authenticated user

**Endpoints:**
- `POST /api/presence/heartbeat` - Update presence status
- `GET /api/presence/active` - Get active users
- `GET /api/presence/user/{id}` - Get user presence status

---

## 🏗️ Common Request/Response Patterns

### Pagination

Most list endpoints support pagination via query parameters:

```
GET /api/users?first=20&max=10
```

- `first`: Offset (0-based)
- `max`: Maximum results per page (default: 20)

### Error Responses

**401 Unauthorized:**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token is invalid or expired",
  "path": "/api/users"
}
```

**403 Forbidden:**
```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions. Required: CORE_ROLE_ADMIN",
  "path": "/api/tenants"
}
```

**404 Not Found:**
```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found with ID: 550e8400-e29b-41d4-a716-446655440000",
  "path": "/api/users/550e8400-e29b-41d4-a716-446655440000"
}
```

**400 Bad Request (Validation):**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "errors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    },
    {
      "field": "username",
      "message": "must not be blank"
    }
  ],
  "path": "/api/users"
}
```

---

## 🧪 Testing with cURL

### Full Workflow Example

```bash
# 1. Get JWT token
TOKEN=$(curl -s -X POST \
  "https://test-tenant.core-platform.local/realms/test-tenant/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=backend" \
  -d "username=admin@test.com" \
  -d "password=admin" \
  | jq -r '.access_token')

# 2. Search users
curl -X GET "http://localhost:8080/api/users?max=5" \
  -H "Authorization: Bearer $TOKEN" | jq

# 3. Get current tenant
curl -X GET "http://localhost:8080/api/tenants/me" \
  -H "Authorization: Bearer $TOKEN" | jq

# 4. Create new user
curl -X POST "http://localhost:8080/api/users" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test.user",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "enabled": true
  }' | jq

# 5. Assign role to user
USER_ID="<user-id-from-step-4>"
curl -X POST "http://localhost:8080/api/users/$USER_ID/roles" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"roleName": "CORE_ROLE_USER"}' | jq
```

---

## 🔧 Advanced Features

### OpenAPI Customization

The API documentation can be customized via `application.properties`:

```properties
# API metadata
springdoc.info.title=Core Platform API
springdoc.info.version=1.0.0
springdoc.info.description=Multi-tenant platform API

# Paths
springdoc.api-docs.path=/api/docs
springdoc.swagger-ui.path=/swagger-ui.html

# UI configuration
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
springdoc.swagger-ui.tryItOutEnabled=true

# Show Actuator endpoints
springdoc.show-actuator=true
```

### Security Schemes

The API supports JWT Bearer authentication:

```yaml
securitySchemes:
  bearerAuth:
    type: http
    scheme: bearer
    bearerFormat: JWT
    description: JWT token from Keycloak OIDC
```

All endpoints automatically require `bearerAuth` security via `@SecurityRequirement` annotation.

---

## 📊 Rate Limiting

Currently, no rate limiting is enforced. Planned for future release with per-tenant limits:

- **Standard Tier**: 100 requests/minute
- **Premium Tier**: 1000 requests/minute
- **Enterprise Tier**: Unlimited

---

## 🔗 Related Documentation

- [SECURITY_RUNBOOK.md](./SECURITY_RUNBOOK.md) - Security incident response
- [PERFORMANCE_PROFILING.md](./PERFORMANCE_PROFILING.md) - API performance optimization
- [DEPLOYMENT_DOCKER_COMPOSE.md](./DEPLOYMENT_DOCKER_COMPOSE.md) - Local deployment
- [DEPLOYMENT_KUBERNETES.md](./DEPLOYMENT_KUBERNETES.md) - Production deployment
- [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) - Common API issues

---

## 📝 Changelog

### v1.0.0 (2025-10-12) - S9: Initial API Documentation
- Full OpenAPI 3.0 specification
- Interactive Swagger UI
- Comprehensive endpoint documentation
- JWT authentication examples
- cURL examples for all major workflows

---

**Need Help?**
- 📧 Email: support@core-platform.local
- 💬 Slack: #core-platform-api
- 📖 Swagger UI: http://localhost:8080/swagger-ui.html
