---
id: INF-022
epic: EPIC-007-infrastructure-deployment
title: "BFF (Backend-for-Frontend) Layer"
priority: P1
status: todo
assignee: ""
created: 2025-11-08
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-007-infrastructure-deployment/stories/INF-022-bff-layer/README.md
    - backlog/EPIC-007-infrastructure-deployment/README.md
---

# INF-022: BFF (Backend-for-Frontend) Layer

**Epic:** EPIC-007 Infrastructure & Deployment  
**Status:** 🔴 TODO  
**Priority:** 🔥 HIGH  
**Effort:** 2 dny, ~700 LOC  
**Owner:** Frontend + Backend Team  
**Created:** 8. listopadu 2025

---

## 📋 OVERVIEW

### Problem Statement

**Current Architecture (problematická):**

```
Frontend → Direct API calls → Backend (Spring Boot)
  ├─ 10+ REST endpoints per feature
  ├─ Multiple round-trips (waterfall requests)
  ├─ Over-fetching data (client gets 100% but uses 20%)
  ├─ No tenant-specific transformations
  └─ Frontend tightly coupled to backend schema
```

**Issues:**
- Frontend dělá N+1 queries (users, then roles, then permissions)
- Over-fetching: Backend vrací celý user object, frontend potřebuje jen name + email
- Underfetching: Dashboard needs data from 5 endpoints → 5 round-trips
- Tenant-specific logic duplikována (frontend + backend)

### Goal

**BFF Pattern:**

```
Frontend
  ↓ 1 GraphQL query
BFF (Backend-for-Frontend)
  ├─ GraphQL → REST translation
  ├─ Parallel backend calls
  ├─ Data aggregation + transformation
  ├─ Tenant-specific response shaping
  └─ Caching hot paths
  ↓
Backend (unchanged REST APIs)
```

**Benefits:**
- 1 request instead of 5-10
- Fetch only needed fields
- Tenant-specific transformations (currency, locale, branding)
- Decouple frontend from backend schema evolution

---

## 🎯 ACCEPTANCE CRITERIA

### Functional Requirements

1. ✅ **BFF Service Deployment**
   - Node.js Express + Apollo GraphQL
   - Docker Compose integration
   - Multi-tenant context propagation

2. ✅ **GraphQL Schema**
   - Type-safe queries
   - Tenant-specific resolvers
   - Caching with DataLoader

3. ✅ **Backend Integration**
   - REST client for Spring Boot APIs
   - JWT token propagation
   - Circuit breaker pattern

4. ✅ **Performance**
   - Response time < 200ms (P95)
   - Redis caching for hot queries
   - Parallel backend calls

## Implementační tasky

- [TASK-022-01: BFF service scaffold + compose](subtasks/TASK-022-01-bff-service-compose.md)
- [TASK-022-02: GraphQL schema + resolvers](subtasks/TASK-022-02-graphql-schema-resolvers.md)
- [TASK-022-03: Auth + tenant context + caching](subtasks/TASK-022-03-auth-tenant-caching.md)

### Implementation

**File:** `docker-compose.yml` (BFF service)

```yaml
services:
  bff:
    build:
      context: ./bff
      dockerfile: Dockerfile
    container_name: core-bff
    environment:
      - NODE_ENV=production
      - PORT=4000
      - BACKEND_URL=http://backend:8080
      - REDIS_URL=redis://redis:6379
      - GRAPHQL_PLAYGROUND=true
    ports:
      - "4000:4000"
    depends_on:
      - backend
      - redis
    networks:
      - core-net
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:4000/health"]
      interval: 10s
      timeout: 3s
      retries: 3
```

**File:** `bff/package.json`

```json
{
  "name": "@core-platform/bff",
  "version": "1.0.0",
  "scripts": {
    "dev": "nodemon src/index.ts",
    "build": "tsc",
    "start": "node dist/index.js"
  },
  "dependencies": {
    "apollo-server-express": "^3.13.0",
    "express": "^4.18.2",
    "graphql": "^16.8.1",
    "dataloader": "^2.2.2",
    "ioredis": "^5.3.2",
    "axios": "^1.6.2",
    "opossum": "^8.1.0"
  },
  "devDependencies": {
    "@types/node": "^20.10.0",
    "typescript": "^5.3.0",
    "nodemon": "^3.0.0"
  }
}
```

**File:** `bff/src/index.ts` (GraphQL server)

```typescript
import express from 'express';
import { ApolloServer } from 'apollo-server-express';
import { typeDefs } from './schema';
import { resolvers } from './resolvers';
import { createContext } from './context';

const app = express();

const server = new ApolloServer({
  typeDefs,
  resolvers,
  context: ({ req }) => createContext(req),
  introspection: true,
  playground: process.env.GRAPHQL_PLAYGROUND === 'true',
});

await server.start();
server.applyMiddleware({ app, path: '/graphql' });

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`🚀 BFF running at http://localhost:${PORT}/graphql`);
});
```

**File:** `bff/src/schema.ts` (GraphQL schema)

```typescript
import { gql } from 'apollo-server-express';

export const typeDefs = gql`
  # Dashboard query (1 request instead of 5)
  type Query {
    dashboard: Dashboard!
    user(id: ID!): User
    tenant: TenantConfig!
  }

  type Dashboard {
    stats: DashboardStats!
    recentActivities: [Activity!]!
    topUsers: [User!]!
    alerts: [Alert!]!
  }

  type DashboardStats {
    totalUsers: Int!
    activeWorkflows: Int!
    successRate: Float!
    avgExecutionTime: Float!
  }

  type Activity {
    id: ID!
    type: String!
    user: User!
    timestamp: String!
    description: String!
  }

  type User {
    id: ID!
    name: String!
    email: String!
    role: Role!
    # No sensitive fields exposed!
  }

  type Role {
    id: ID!
    name: String!
    permissions: [String!]!
  }

  type Alert {
    severity: AlertSeverity!
    message: String!
    timestamp: String!
  }

  enum AlertSeverity {
    INFO
    WARNING
    ERROR
    CRITICAL
  }

  # Tenant-specific config
  type TenantConfig {
    name: String!
    logo: String!
    primaryColor: String!
    locale: String!
    currency: String!
    features: [String!]!
  }
`;
```

**File:** `bff/src/resolvers/dashboard.ts`

```typescript
import { backendClient } from '../clients/backend';
import { DashboardDataLoader } from '../dataloaders';

export const dashboardResolver = {
  Query: {
    dashboard: async (_, __, context) => {
      const { tenantId, dataloaders } = context;

      // Parallel backend calls (instead of sequential)
      const [stats, activities, users, alerts] = await Promise.all([
        backendClient.get(`/api/tenants/${tenantId}/stats`),
        backendClient.get(`/api/tenants/${tenantId}/activities`),
        backendClient.get(`/api/tenants/${tenantId}/users/top`),
        backendClient.get(`/api/tenants/${tenantId}/alerts`),
      ]);

      return {
        stats: stats.data,
        recentActivities: activities.data,
        topUsers: users.data,
        alerts: alerts.data,
      };
    },

    user: async (_, { id }, context) => {
      // Use DataLoader to batch requests
      return context.dataloaders.userLoader.load(id);
    },

    tenant: async (_, __, context) => {
      const { tenantId, redis } = context;

      // Try cache first (hot path optimization)
      const cached = await redis.get(`tenant:${tenantId}:config`);
      if (cached) return JSON.parse(cached);

      // Fetch from backend
      const { data } = await backendClient.get(
        `/api/tenants/${tenantId}/config`
      );

      // Cache for 5 minutes
      await redis.setex(`tenant:${tenantId}:config`, 300, JSON.stringify(data));

      return data;
    },
  },
};
```

**File:** `bff/src/dataloaders/user.ts` (N+1 query prevence)

```typescript
import DataLoader from 'dataloader';
import { backendClient } from '../clients/backend';

export const createUserLoader = (tenantId: string) => {
  return new DataLoader(async (userIds: readonly string[]) => {
    // Batch load users (1 request instead of N)
    const { data } = await backendClient.post(
      `/api/tenants/${tenantId}/users/batch`,
      { ids: userIds }
    );

    // Return in same order as requested
    return userIds.map(id => data.find(u => u.id === id));
  });
};
```

**File:** `bff/src/context.ts` (Request context)

```typescript
import { Request } from 'express';
import Redis from 'ioredis';
import { createUserLoader } from './dataloaders/user';

const redis = new Redis(process.env.REDIS_URL);

export const createContext = (req: Request) => {
  // Extract tenant from JWT token
  const token = req.headers.authorization?.replace('Bearer ', '');
  const decoded = jwt.decode(token) as { tenant_id: string };

  return {
    tenantId: decoded.tenant_id,
    token,
    redis,
    dataloaders: {
      userLoader: createUserLoader(decoded.tenant_id),
    },
  };
};
```

**File:** `bff/src/clients/backend.ts` (Circuit breaker)

```typescript
import axios from 'axios';
import CircuitBreaker from 'opossum';

const axiosInstance = axios.create({
  baseURL: process.env.BACKEND_URL,
  timeout: 5000,
});

// Add JWT token to all requests
axiosInstance.interceptors.request.use(config => {
  const token = context.token; // From GraphQL context
  config.headers.Authorization = `Bearer ${token}`;
  config.headers['X-Tenant-ID'] = context.tenantId;
  return config;
});

// Circuit breaker pattern
const options = {
  timeout: 5000,
  errorThresholdPercentage: 50,
  resetTimeout: 30000,
};

const breaker = new CircuitBreaker(
  async (url: string) => axiosInstance.get(url),
  options
);

breaker.on('open', () => {
  console.error('⚠️ Circuit breaker OPEN - backend failing');
});

export const backendClient = {
  get: (url: string) => breaker.fire(url),
  post: (url: string, data: any) => axiosInstance.post(url, data),
};
```

**File:** `docker/nginx/nginx-ssl.conf` (Routing)

```nginx
# BFF GraphQL endpoint
location /graphql {
    proxy_pass http://bff:4000/graphql;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}

# BFF playground (dev only)
location /graphql-playground {
    proxy_pass http://bff:4000/graphql;
    proxy_set_header Host $host;
}
```

**File:** `frontend/src/graphql/client.ts`

```typescript
import { ApolloClient, InMemoryCache } from '@apollo/client';

export const client = new ApolloClient({
  uri: '/graphql',
  cache: new InMemoryCache(),
  headers: {
    Authorization: `Bearer ${localStorage.getItem('token')}`,
  },
});
```

**File:** `frontend/src/pages/Dashboard.tsx` (Usage)

```tsx
import { useQuery, gql } from '@apollo/client';

const DASHBOARD_QUERY = gql`
  query Dashboard {
    dashboard {
      stats {
        totalUsers
        activeWorkflows
        successRate
      }
      recentActivities {
        id
        type
        user {
          name
        }
        timestamp
      }
      alerts {
        severity
        message
      }
    }
    tenant {
      name
      logo
      primaryColor
    }
  }
`;

export const Dashboard = () => {
  // 1 GraphQL query instead of 5 REST calls!
  const { data, loading } = useQuery(DASHBOARD_QUERY);

  if (loading) return <Spinner />;

  return (
    <div style={{ color: data.tenant.primaryColor }}>
      <h1>{data.tenant.name} Dashboard</h1>
      <Stats {...data.dashboard.stats} />
      <Activities items={data.dashboard.recentActivities} />
      <Alerts items={data.dashboard.alerts} />
    </div>
  );
};
```

**File:** `docker/prometheus/bff-metrics.yml`

```yaml
scrape_configs:
  - job_name: 'bff'
    static_configs:
      - targets: ['bff:4000']
    metrics_path: '/metrics'
```

**Effort:** 2 dny  
**LOC:** ~700  
**Priority:** 🔥 HIGH

---

## 🧪 TESTING

### Unit Tests

**File:** `bff/src/__tests__/resolvers.test.ts`

```typescript
import { dashboardResolver } from '../resolvers/dashboard';

test('dashboard aggregates data from multiple endpoints', async () => {
  const context = mockContext({ tenantId: '123' });
  
  const result = await dashboardResolver.Query.dashboard(null, {}, context);
  
  expect(result.stats).toBeDefined();
  expect(result.recentActivities).toHaveLength(5);
  expect(backendClient.get).toHaveBeenCalledTimes(4); // Parallel!
});
```

### E2E Tests

**File:** `e2e/specs/bff/graphql.spec.ts`

```typescript
test('GraphQL query returns dashboard data', async ({ request }) => {
  const response = await request.post('/graphql', {
    data: {
      query: `
        query {
          dashboard {
            stats { totalUsers }
          }
        }
      `,
    },
  });

  expect(response.status()).toBe(200);
  const { data } = await response.json();
  expect(data.dashboard.stats.totalUsers).toBeGreaterThan(0);
});
```

---

## 🔗 DEPENDENCIES

**Blocks:**
- Frontend performance optimization

**Requires:**
- Backend REST APIs functional
- Redis cache available

---

**Created:** 8. listopadu 2025  
**Status:** 🔴 Ready for Implementation
