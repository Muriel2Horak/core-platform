# Phase 2 - Quick Start Guide

## 🚀 Spuštění služeb

```bash
# Start všech služeb včetně Redis a MinIO
docker compose -f docker/docker-compose.yml -f .devcontainer/docker-compose.devcontainer.yml --env-file .env up -d

# Ověř, že běží
docker ps | grep -E "(redis|minio)"
```

## 📝 API Příklady

### 1. Workflow & States

```bash
# Get JWT token
TOKEN=$(curl -s -X POST https://admin.admin.core-platform.local/realms/admin/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=web" \
  -d "grant_type=password" \
  -d "username=test_admin" \
  -d "password=admin123" \
  --insecure | jq -r '.access_token')

# Get current state
curl -X GET "http://localhost:8080/api/entities/UserProfile/123/state" \
  -H "Authorization: Bearer $TOKEN"

# Get allowed transitions
curl -X GET "http://localhost:8080/api/entities/UserProfile/123/transitions" \
  -H "Authorization: Bearer $TOKEN"

# Apply transition
curl -X POST "http://localhost:8080/api/entities/UserProfile/123/transition/ACTIVATE" \
  -H "Authorization: Bearer $TOKEN"
```

### 2. Documents

```bash
# Upload document
curl -X POST "http://localhost:8080/api/documents" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@document.pdf" \
  -F "entityType=UserProfile" \
  -F "entityId=123"

# List documents
curl -X GET "http://localhost:8080/api/documents?entityType=UserProfile&entityId=123" \
  -H "Authorization: Bearer $TOKEN"

# Download document (redirects to MinIO presigned URL)
curl -L "http://localhost:8080/api/documents/{documentId}/download" \
  -H "Authorization: Bearer $TOKEN" \
  -o downloaded.pdf

# Delete document
curl -X DELETE "http://localhost:8080/api/documents/{documentId}" \
  -H "Authorization: Bearer $TOKEN"
```

### 3. Fulltext Search

```bash
# Search in entities and documents
curl -X GET "http://localhost:8080/api/search?q=john&types=UserProfile,Document&limit=20" \
  -H "Authorization: Bearer $TOKEN"

# Response:
{
  "results": [
    {
      "type": "entity",
      "entityType": "UserProfile",
      "id": "123",
      "title": "UserProfile 123",
      "highlights": ["<b>John</b> Doe works in IT department"],
      "score": 0.95
    },
    {
      "type": "document",
      "entityType": "Document",
      "id": "456",
      "title": "report.pdf",
      "highlights": ["This report covers <b>John</b>'s performance"],
      "score": 0.87,
      "metadata": {
        "entityType": "UserProfile",
        "entityId": "123"
      }
    }
  ],
  "total": 2,
  "durationMs": 45
}
```

### 4. WebSocket Presence

```javascript
// Frontend WebSocket connection
const ws = new WebSocket('ws://localhost:8080/ws/presence');

ws.onopen = () => {
  // Join entity
  ws.send(JSON.stringify({
    action: 'join',
    entityType: 'UserProfile',
    entityId: '123'
  }));
  
  // Send heartbeat every 10s
  setInterval(() => {
    ws.send(JSON.stringify({
      action: 'heartbeat',
      entityType: 'UserProfile',
      entityId: '123'
    }));
  }, 10000);
};

ws.onmessage = (event) => {
  const data = JSON.parse(event.data);
  console.log('Presence event:', data);
};

// Start editing field
ws.send(JSON.stringify({
  action: 'editStart',
  entityType: 'UserProfile',
  entityId: '123',
  fieldId: 'email'
}));

// Stop editing
ws.send(JSON.stringify({
  action: 'editStop',
  entityType: 'UserProfile',
  entityId: '123',
  fieldId: 'email'
}));
```

```bash
# REST Fallback API
# Get online users
curl "http://localhost:8080/api/presence/UserProfile/123" \
  -H "Authorization: Bearer $TOKEN"

# Get editing indicators
curl "http://localhost:8080/api/presence/UserProfile/123/editing" \
  -H "Authorization: Bearer $TOKEN"
```

## 🔧 Redis CLI

```bash
# Connect to Redis
docker exec -it core-redis redis-cli

# Check presence keys
127.0.0.1:6379> KEYS presence:*
127.0.0.1:6379> GET presence:admin:UserProfile:123:user-456
127.0.0.1:6379> SMEMBERS presence_members:admin:UserProfile:123
127.0.0.1:6379> TTL presence:admin:UserProfile:123:user-456

# Check editing keys
127.0.0.1:6379> KEYS editing:*
```

## 📦 MinIO Console

```bash
# Open MinIO console
open http://localhost:9001

# Login: minioadmin / minioadmin

# Nebo přes MinIO CLI
docker exec -it core-minio mc alias set local http://localhost:9000 minioadmin minioadmin
docker exec -it core-minio mc ls local/
docker exec -it core-minio mc ls local/tenant-admin/
```

## 🗄️ Database Queries

```sql
-- Connect to DB
docker exec -it core-db psql -U core -d core

-- Check workflow tables
SELECT * FROM entity_state;
SELECT * FROM state_transition;
SELECT * FROM entity_state_log ORDER BY changed_at DESC LIMIT 10;

-- Check documents
SELECT id, filename, entity_type, entity_id, size_bytes, uploaded_at 
FROM document ORDER BY uploaded_at DESC;

-- Fulltext search
SELECT d.filename, ts_rank(di.content_tsv, to_tsquery('english', 'report:*')) as rank
FROM document d
JOIN document_index di ON d.id = di.document_id
WHERE di.content_tsv @@ to_tsquery('english', 'report:*')
ORDER BY rank DESC;

-- SLA breach check
SELECT entity_type, entity_id, state_code, 
  calculate_sla_status(since, 60) as sla_status
FROM entity_state;
```

## 🧪 Testing

```bash
# Run integration tests
cd backend
./mvnw test -Dtest=Phase2IntegrationTest

# Run all tests
./mvnw test
```

## 📊 Monitoring

### Prometheus Metrics

```bash
# Check metrics endpoint
curl http://localhost:8080/actuator/prometheus | grep -E "(websocket|workflow|document|cache)"
```

Metrics to watch:
- `websocket_connections_active`
- `workflow_transitions_total`
- `document_uploads_total`
- `cache_hit_rate`
- `fulltext_search_duration_seconds`

### Logs (Loki/Grafana)

```bash
# View backend logs
docker logs -f core-backend | grep -E "(WebSocket|Workflow|Document|Search)"
```

## ⚙️ Configuration

Klíčové environment variables v `.env`:

```bash
# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# MinIO
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# WebSocket
WEBSOCKET_ALLOWED_ORIGINS=https://*.core-platform.local,http://localhost:*

# Fulltext
app.fulltext.max-results=100
app.fulltext.min-query-length=3

# Cache
spring.cache.redis.time-to-live=600000  # 10 min
```

## 🐛 Troubleshooting

### MinIO not accessible
```bash
# Check MinIO is running
docker ps | grep minio

# Check logs
docker logs core-minio

# Test connection
curl http://localhost:9000/minio/health/live
```

### Redis connection refused
```bash
# Check Redis
docker exec -it core-redis redis-cli ping
# Should return: PONG

# Check from backend container
docker exec -it core-backend-1 ping redis -c 1
```

### WebSocket connection fails
```bash
# Check JWT token is valid
echo $TOKEN | cut -d. -f2 | base64 -d | jq .

# Check CORS origins
grep WEBSOCKET_ALLOWED_ORIGINS .env

# Check backend logs
docker logs core-backend-1 | grep WebSocket
```

### Document upload fails
```bash
# Check MinIO bucket permissions
docker exec -it core-minio mc ls local/tenant-admin/

# Check file size limits
grep "spring.servlet.multipart.max-file-size" backend/src/main/resources/application.properties

# Check Tika extraction errors
docker logs core-backend-1 | grep Tika
```

## 📚 Next Steps

1. **Frontend Integration**
   - Implement `usePresence` hook
   - Implement `useEditingIndicator` hook
   - Add workflow UI components

2. **Performance Optimization**
   - jOOQ query migration
   - Keyset pagination
   - HTTP caching (ETag, If-Modified-Since)

3. **Production Readiness**
   - Redis Cluster for HA
   - MinIO distributed mode
   - WebSocket scaling (Redis Pub/Sub)
   - Async Tika extraction (queue)

---

**Dokumentace**: Viz [METAMODEL_PHASE_2.md](../docs/METAMODEL_PHASE_2.md) pro kompletní API referenci.
