# 🎯 FÁZE 2.4 - Query Layer Hardening

**Status:** ✅ IMPLEMENTOVÁNO  
**Datum:** 9. října 2025

## 📋 Přehled

Fáze 2.4 přidává robustní dotazovací vrstvu s:
- **Filter Parser** - Parsování filter stringů na jOOQ Conditions
- **Keyset Pagination** - Cursor-based stránkování pro velké datasety
- **Cache Invalidation** - Automatická invalidace Redis cache na změny

---

## 🔍 1. Filter Parser

### Supported Operators

#### Comparison Operators
- `eq` - equals (`age eq 18`)
- `ne` - not equals (`status ne 'inactive'`)
- `lt` - less than (`age lt 30`)
- `lte` - less than or equal (`age lte 30`)
- `gt` - greater than (`age gt 18`)
- `gte` - greater than or equal (`age gte 18`)
- `like` - pattern match (`name like '%John%'`)

#### List Operators
- `in` - in list (`status in ('active', 'pending')`)
- `notIn` - not in list (`status notIn ('deleted')`)

#### Boolean Operators
- `and` - logical AND
- `or` - logical OR  
- `()` - parentheses for grouping

### Examples

```java
// Simple comparison
FilterParser.parse("status eq 'active'");

// Multiple conditions
FilterParser.parse("(age gt 18) and (status eq 'active')");

// OR logic
FilterParser.parse("name like '%Smith%' or email like '%@example.com'");

// IN operator
FilterParser.parse("status in ('active', 'pending', 'approved')");

// Complex expression
FilterParser.parse("(age gte 18 and age lte 65) and status ne 'inactive'");
```

### Usage in Controllers

```java
@GetMapping
public ResponseEntity<?> list(
    @RequestParam(required = false) String filter,
    @RequestParam(defaultValue = "20") int limit
) {
    Condition condition = FilterParser.parse(filter);
    // Use condition in jOOQ query
    var results = dsl.select()
        .from(table)
        .where(condition)
        .limit(limit)
        .fetch();
    
    return ResponseEntity.ok(results);
}
```

---

## 📄 2. Keyset Pagination

### Why Keyset?

Traditional offset-based pagination (`OFFSET 1000 LIMIT 20`) has problems:
- **Performance degradation** with large offsets (DB must skip 1000 rows)
- **Inconsistent results** when data changes during pagination
- **Inefficient** for infinite scrolling

Keyset pagination uses a **cursor** (last seen value) instead of offset:
- ✅ **Constant performance** regardless of page depth
- ✅ **Consistent results** even when data changes
- ✅ **Efficient** for real-time data and infinite scroll

### Request Format

```json
GET /api/entities?limit=20&sortBy=created_at&sortOrder=desc&cursorNext=2024-10-09T10:30:00Z,uuid-123
```

Parameters:
- `limit` - Page size (default 20, max 100)
- `sortBy` - Sort field (default `created_at`)
- `sortOrder` - `asc` or `desc` (default `desc`)
- `cursorNext` - Cursor to get next page
- `cursorPrev` - Cursor to get previous page
- `withTotal` - Include total count (slower, default `false`)

### Response Format

```json
{
  "data": [...],
  "cursorNext": "2024-10-09T10:00:00Z,uuid-456",
  "cursorPrev": "2024-10-09T11:00:00Z,uuid-789",
  "hasNext": true,
  "hasPrev": true,
  "pageSize": 20,
  "total": 1234  // Only if withTotal=true
}
```

### Implementation Example

```java
@GetMapping
public ResponseEntity<KeysetPagination.Response<Entity>> list(
    @RequestParam(required = false) String cursorNext,
    @RequestParam(required = false) Integer limit,
    @RequestParam(required = false) String sortBy
) {
    var request = KeysetPagination.Request.builder()
        .cursorNext(cursorNext)
        .limit(limit)
        .sortBy(sortBy)
        .build();
    
    // Parse cursor
    var cursor = KeysetPagination.Cursor.decode(request.getCursorNext());
    
    // Build jOOQ query with cursor
    var query = dsl.select()
        .from(table)
        .where(/* cursor condition */)
        .orderBy(/* sort field */)
        .limit(request.getEffectiveLimit() + 1); // +1 to check hasNext
    
    var results = query.fetch();
    
    // Build response with cursors
    var response = KeysetPagination.Response.builder()
        .data(results)
        .cursorNext(/* encode cursor from last item */)
        .hasNext(results.size() > limit)
        .build();
    
    return ResponseEntity.ok(response);
}
```

---

## 🔄 3. Cache Invalidation

### Architecture

```
PostgreSQL             Redis               Application
┌─────────┐           ┌─────────┐         ┌─────────┐
│ TRIGGER │──NOTIFY──▶│ Pub/Sub │──event─▶│Listener │
│         │           │         │         │         │
│ INSERT  │           │ channel:│         │Invalidate
│ UPDATE  │           │ change_ │         │Cache    │
│ DELETE  │           │ events  │         │         │
└─────────┘           └─────────┘         └─────────┘
```

### PostgreSQL Trigger (Example)

```sql
-- Function to send change events
CREATE OR REPLACE FUNCTION notify_entity_change()
RETURNS TRIGGER AS $$
BEGIN
  PERFORM pg_notify(
    'change_events',
    json_build_object(
      'type', TG_OP,
      'table', TG_TABLE_NAME,
      'id', COALESCE(NEW.id::text, OLD.id::text),
      'tenant_id', COALESCE(NEW.tenant_id, OLD.tenant_id)
    )::text
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger on entity table
CREATE TRIGGER entity_change_trigger
AFTER INSERT OR UPDATE OR DELETE ON entity
FOR EACH ROW EXECUTE FUNCTION notify_entity_change();
```

### Cache Key Format

```
entity:{type}:{id}:{version}
list:{type}:{tenant_id}:{hash}
```

Examples:
- `entity:UserProfile:uuid-123:v42`
- `list:UserProfile:tenant-abc:hash-xyz`

### Automatic Invalidation

Cache listener automatically invalidates on entity changes:

1. **Entity updated** → PostgreSQL trigger fires
2. **NOTIFY sent** → Redis Pub/Sub receives event
3. **Listener processes** → Invalidates cache keys matching pattern
4. **Next API call** → Fetches fresh data from DB

### Manual Invalidation

```java
@Autowired
private CacheInvalidationListener cacheListener;

// Invalidate specific entity
cacheListener.invalidate("UserProfile", "uuid-123");

// Clear entire tenant cache
cacheListener.clearTenantCache("tenant-abc");
```

---

## 🎯 Benefits

### Filter Parser
- ✅ Type-safe SQL through jOOQ
- ✅ No SQL injection risk
- ✅ Support for complex boolean logic
- ✅ Easy to extend with new operators

### Keyset Pagination
- ✅ Consistent O(log n) performance
- ✅ No duplicate/missing results
- ✅ Efficient for infinite scroll
- ✅ Supports forward/backward navigation

### Cache Invalidation
- ✅ Automatic cache consistency
- ✅ No manual invalidation needed
- ✅ Handles multi-instance deployments
- ✅ Reduces DB load

---

## 🚀 Next Steps

### Integration Tasks
1. **Migrate MetamodelCrudService** to use FilterParser + Keyset Pagination
2. **Add PostgreSQL triggers** for all metamodel tables
3. **Implement HTTP caching** (ETag, If-Modified-Since, 304 responses)
4. **Add cache warming** for frequently accessed entities
5. **Monitoring** - Cache hit rate, invalidation events

### Performance Testing
- Benchmark filter parsing speed
- Compare keyset vs offset pagination at scale (1M+ rows)
- Measure cache hit rate in production
- Test cache invalidation latency

---

## 📚 References

- [jOOQ Documentation](https://www.jooq.org/doc/latest/manual/)
- [PostgreSQL NOTIFY/LISTEN](https://www.postgresql.org/docs/current/sql-notify.html)
- [Keyset Pagination](https://use-the-index-luke.com/no-offset)
- [Redis Pub/Sub](https://redis.io/docs/manual/pubsub/)
