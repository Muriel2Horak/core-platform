---
id: META-011
epic: EPIC-005-metamodel-generator-studio
title: "Streaming & Priority Queue Runtime ⚡"
priority: P0
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "160 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-005-metamodel-generator-studio/stories/META11-streaming-priority-queue-runtime/README.md
    - backlog/EPIC-005-metamodel-generator-studio/README.md
---

# META-011: Streaming & Priority Queue Runtime ⚡

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🔴 **CRITICAL** - Schema existuje, runtime CHYBÍ!  
**Priorita:** P0 (Critical - Start Immediately)  
**Estimated LOC:** ~2,000 řádků  
**Effort:** 4 týdny (160 hodin)

---

## 📋 Story Description

Jako **platform developer**, chci **runtime implementaci streaming & priority queue systému**, abych **mohl publikovat entity changes do Kafky s prioritizací a zajistil event-driven architekturu**.

---

## 🎯 Business Value

**Proč je to KRITICKÉ:**
- **Schema EXISTUJE** (`StreamingEntityConfig`, `PriorityWeights`) ale **ŽÁDNÝ RUNTIME**!
- **Původní vize**: Fronta pro DB zápisy s prioritizací (CRITICAL → HIGH → NORMAL → BULK)
- **Blokuje**: CDC, real-time updates, workflow events, reporting streams
- **Impact**: Bez toho není event-driven architecture

**HIGH-LEVEL požadavek:**
> 6️⃣ Streaming & Eventy: Metamodel určuje které entity jsou event-sourcované / streamované, jaký payload se posílá do Kafky (minimálně: tenantId, entityType, entityId, changeType, version, timestamp, user), sekvence / versioning (optimistic locking, ordering), napojení na presence (online lock / kdo edituje), refresh FE po změně, pre-agg/reporting (CDC/streams).

---

## 🎯 Acceptance Criteria

### AC1: Kafka Producer z Entity Changes
- **GIVEN** entita s `streaming.enabled: true`:
  ```yaml
  entity: Order
  streaming:
    enabled: true
    topicPrefix: "core.orders"
    eventPayloadMode: "diff"
  ```
- **WHEN** provedu `POST /api/orders` (create)
- **THEN** Kafka event se publikuje:
  ```json
  {
    "eventType": "OrderCreated",
    "tenantId": 123,
    "entityType": "Order",
    "entityId": "456",
    "changeType": "CREATE",
    "version": 1,
    "timestamp": "2025-11-08T10:30:00Z",
    "user": "user@example.com",
    "payload": {
      "orderId": 456,
      "status": "pending",
      "totalAmount": 299.99
    }
  }
  ```
- **Topic**: `core.orders.order.created`

### AC2: Priority Queue Executor
- **GIVEN** priority weights config:
  ```yaml
  streaming:
    priorityWeights:
      CRITICAL: 1000   # Process first
      HIGH: 100
      NORMAL: 10
      BULK: 1          # Process last
  ```
- **WHEN** v queue jsou:
  - 5 BULK events (e.g., batch import)
  - 1 CRITICAL event (e.g., payment failure)
  - 3 NORMAL events
- **THEN** executor zpracuje v pořadí:
  1. CRITICAL event (okamžitě)
  2. 3 NORMAL events
  3. 5 BULK events (throttled)

### AC3: CDC Event Format (Change Data Capture)
- **GIVEN** entity update:
  ```sql
  UPDATE products SET price = 150.00 WHERE id = 42;
  ```
- **AND** `eventPayloadMode: "diff"`
- **THEN** Kafka event obsahuje:
  ```json
  {
    "eventType": "ProductUpdated",
    "changeType": "UPDATE",
    "entityId": "42",
    "version": 5,
    "changes": {
      "price": {
        "old": 120.00,
        "new": 150.00
      }
    },
    "fullSnapshot": null  // Only diff, not full entity
  }
  ```

### AC4: Event Payload Modes
- **GIVEN** `eventPayloadMode` konfigurace
- **THEN** podporuje:
  - `minimal`: Pouze `entityId`, `changeType`, `version`
  - `diff`: Minimal + changed fields (old/new values)
  - `full`: Diff + complete entity snapshot

### AC5: DLQ (Dead Letter Queue) Handling
- **GIVEN** Kafka send selhání (network error)
- **WHEN** retry po 3 pokusech selže
- **THEN**:
  - Event se přesune do `dlq.events` topic
  - Log ERROR: "Event delivery failed after 3 retries"
  - Admin notifikace (email/Slack)

### AC6: Optimistic Locking & Versioning
- **GIVEN** entity s `version` field
- **WHEN** 2 uživatelé editují najednou:
  - User A: GET entity (version=5) → UPDATE
  - User B: GET entity (version=5) → UPDATE
- **THEN**:
  - User A: UPDATE succeeds (version=6), event published
  - User B: UPDATE fails `409 Conflict` (stale version)
  - Error message: "Entity was modified by another user"

### AC7: Presence Tracking (Online Lock)
- **GIVEN** entity s `presence.enabled: true`
- **WHEN** User A otevře detail Order #456
- **THEN**:
  - Websocket message: `{ type: "lock", entityId: 456, user: "userA" }`
  - User B vidí banner: "User A is editing this order"
  - Po 5 min inactivity → auto-unlock

### AC8: Tenant Scoping v Events
- **GIVEN** multi-tenant entita
- **WHEN** publikuji event
- **THEN** VŠECHNY events obsahují:
  ```json
  {
    "tenantId": 123,  // POVINNÉ!
    "entityType": "Order",
    "entityId": "456"
  }
  ```
- **AND** Kafka consumers mohou filtrovat per-tenant

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Outbox + priority queue storage + scheduler | 45h | META-001 |
| 2 | Kafka producer + payload modes + topic naming | 40h | T1, META-010 |
| 3 | CDC diff + versioning + optimistic lock hooky | 35h | T1 |
| 4 | DLQ/retry + presence integration | 25h | T2, EPIC-003 |
| 5 | Integration testy + docs (event schema) | 15h | T1, T2, T3, T4 |

---

## 🏗️ Implementation Design

### Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│ API Layer (POST /api/orders)                            │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ EntityEventPublisher (JPA Listener)                     │
│  @PostPersist, @PostUpdate, @PostRemove                 │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ PriorityEventQueue                                      │
│  - calculatePriority(event, config)                     │
│  - enqueue(event, priority)                             │
└────────────────┬────────────────────────────────────────┘
                 │
    ┌────────────┼────────────┬────────────┐
    ▼            ▼            ▼            ▼
┌─────────┐ ┌──────────┐ ┌─────────┐ ┌──────────┐
│CRITICAL │ │  HIGH    │ │ NORMAL  │ │  BULK    │
│ Queue   │ │  Queue   │ │  Queue  │ │  Queue   │
│(instant)│ │(100ms)   │ │(1s)     │ │(10s)     │
└─────────┘ └──────────┘ └─────────┘ └──────────┘
    │            │            │            │
    └────────────┴────────────┴────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ KafkaEventProducer                                      │
│  - send(topic, event)                                   │
│  - retry(3 attempts)                                    │
│  - DLQ fallback                                         │
└─────────────────────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ Kafka Topics                                            │
│  - core.orders.order.created                            │
│  - core.orders.order.updated                            │
│  - dlq.events                                           │
└─────────────────────────────────────────────────────────┘
```

### Core Components

**1. EntityEventPublisher (JPA Listener)**
```java
@Component
public class EntityEventPublisher {
    
    private final PriorityEventQueue eventQueue;
    private final MetamodelLoader metamodelLoader;
    
    @PostPersist
    public void onEntityCreated(Object entity) {
        EntitySchema schema = metamodelLoader.getSchemaForEntity(entity.getClass());
        
        if (schema.getStreaming() == null || !schema.getStreaming().isEnabled()) {
            return; // Streaming disabled
        }
        
        EntityEvent event = EntityEvent.builder()
            .eventType(schema.getEntity() + "Created")
            .tenantId(extractTenantId(entity))
            .entityType(schema.getEntity())
            .entityId(extractId(entity))
            .changeType(ChangeType.CREATE)
            .version(extractVersion(entity))
            .timestamp(Instant.now())
            .user(SecurityContextHolder.getContext().getAuthentication().getName())
            .payload(buildPayload(entity, schema, null))
            .build();
        
        // Enqueue with priority
        String priority = determinePriority(event, schema);
        eventQueue.enqueue(event, priority);
    }
    
    @PostUpdate
    public void onEntityUpdated(Object entity) {
        // Similar to onEntityCreated, but changeType = UPDATE
        // + detect changed fields for "diff" mode
    }
    
    @PostRemove
    public void onEntityDeleted(Object entity) {
        // changeType = DELETE
        // payload = minimal (just ID)
    }
    
    private String determinePriority(EntityEvent event, EntitySchema schema) {
        // Check if entity has explicit priority field (e.g., order.urgency)
        if (schema.getStreaming().getPriorityField() != null) {
            Object priorityValue = getFieldValue(event.getPayload(), schema.getStreaming().getPriorityField());
            return String.valueOf(priorityValue); // e.g., "CRITICAL"
        }
        
        // Default to workflow transition priority
        return "NORMAL";
    }
}
```

**2. PriorityEventQueue**
```java
@Component
public class PriorityEventQueue {
    
    private final Map<String, BlockingQueue<EntityEvent>> queues = new ConcurrentHashMap<>();
    private final StreamingGlobalConfig config;
    
    @PostConstruct
    public void initQueues() {
        queues.put("CRITICAL", new LinkedBlockingQueue<>());
        queues.put("HIGH", new LinkedBlockingQueue<>());
        queues.put("NORMAL", new LinkedBlockingQueue<>());
        queues.put("BULK", new LinkedBlockingQueue<>());
        
        // Start consumer threads for each priority
        startConsumer("CRITICAL", config.getPriorityWeights().getCritical());
        startConsumer("HIGH", config.getPriorityWeights().getHigh());
        startConsumer("NORMAL", config.getPriorityWeights().getNormal());
        startConsumer("BULK", config.getPriorityWeights().getBulk());
    }
    
    public void enqueue(EntityEvent event, String priority) {
        BlockingQueue<EntityEvent> queue = queues.get(priority);
        if (queue == null) {
            queue = queues.get("NORMAL"); // Fallback
        }
        queue.offer(event);
        log.debug("Enqueued {} event with priority {}", event.getEventType(), priority);
    }
    
    private void startConsumer(String priority, int weight) {
        // Weight determines delay between processing
        long delayMs = calculateDelay(weight);
        
        Thread consumer = new Thread(() -> {
            while (true) {
                try {
                    EntityEvent event = queues.get(priority).take();
                    kafkaEventProducer.send(event);
                    
                    // Throttle based on weight
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        consumer.setName("EventConsumer-" + priority);
        consumer.start();
    }
    
    private long calculateDelay(int weight) {
        // Higher weight = lower delay
        // CRITICAL (1000) → 0ms
        // HIGH (100) → 10ms
        // NORMAL (10) → 100ms
        // BULK (1) → 1000ms
        return Math.max(0, 1000 - weight);
    }
}
```

**3. KafkaEventProducer**
```java
@Service
public class KafkaEventProducer {
    
    private final KafkaTemplate<String, EntityEvent> kafkaTemplate;
    private final StreamingGlobalConfig config;
    
    public void send(EntityEvent event) {
        String topic = buildTopicName(event);
        
        try {
            kafkaTemplate.send(topic, event.getEntityId(), event)
                .thenAccept(result -> {
                    log.info("Event published: {} to {}", event.getEventType(), topic);
                })
                .exceptionally(ex -> {
                    handleSendFailure(event, topic, ex);
                    return null;
                });
        } catch (Exception e) {
            handleSendFailure(event, topic, e);
        }
    }
    
    private String buildTopicName(EntityEvent event) {
        // core.orders.order.created
        return String.format("%s.%s.%s",
            config.getTopicPrefix(),
            event.getEntityType().toLowerCase(),
            event.getChangeType().toString().toLowerCase()
        );
    }
    
    private void handleSendFailure(EntityEvent event, String topic, Throwable error) {
        log.error("Failed to send event {} to {}: {}", event.getEventType(), topic, error.getMessage());
        
        // Retry 3x with exponential backoff
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Thread.sleep(attempt * 1000); // 1s, 2s, 3s
                kafkaTemplate.send(topic, event.getEntityId(), event).get();
                log.info("Event sent successfully on retry {}", attempt);
                return;
            } catch (Exception retryEx) {
                log.warn("Retry {} failed: {}", attempt, retryEx.getMessage());
            }
        }
        
        // All retries failed → DLQ
        sendToDLQ(event, error);
    }
    
    private void sendToDLQ(EntityEvent event, Throwable error) {
        String dlqTopic = config.getDlqTopic();
        
        DLQEvent dlqEvent = DLQEvent.builder()
            .originalEvent(event)
            .failureReason(error.getMessage())
            .failureTimestamp(Instant.now())
            .retryCount(3)
            .build();
        
        kafkaTemplate.send(dlqTopic, event.getEntityId(), dlqEvent);
        
        log.error("Event moved to DLQ: {}", event.getEventType());
        
        // Send admin notification
        notificationService.sendAlert(
            "Kafka Event Delivery Failed",
            String.format("Event %s failed after 3 retries. Check DLQ topic: %s", 
                event.getEventType(), dlqTopic)
        );
    }
}
```

**4. Payload Builder (diff/full/minimal modes)**
```java
@Component
public class EventPayloadBuilder {
    
    public Object buildPayload(Object entity, EntitySchema schema, Object oldEntity) {
        String mode = schema.getStreaming().getEventPayloadMode();
        
        switch (mode) {
            case "minimal":
                return Map.of(
                    "entityId", extractId(entity),
                    "version", extractVersion(entity)
                );
            
            case "diff":
                if (oldEntity == null) {
                    // CREATE → full entity
                    return serializeEntity(entity, schema);
                } else {
                    // UPDATE → only changed fields
                    return detectChanges(oldEntity, entity, schema);
                }
            
            case "full":
                return Map.of(
                    "snapshot", serializeEntity(entity, schema),
                    "changes", oldEntity != null ? detectChanges(oldEntity, entity, schema) : null
                );
            
            default:
                throw new IllegalArgumentException("Unknown payload mode: " + mode);
        }
    }
    
    private Map<String, FieldChange> detectChanges(Object oldEntity, Object newEntity, EntitySchema schema) {
        Map<String, FieldChange> changes = new HashMap<>();
        
        for (FieldSchema field : schema.getFields()) {
            Object oldValue = getFieldValue(oldEntity, field.getName());
            Object newValue = getFieldValue(newEntity, field.getName());
            
            if (!Objects.equals(oldValue, newValue)) {
                changes.put(field.getName(), new FieldChange(oldValue, newValue));
            }
        }
        
        return changes;
    }
}
```

**5. Optimistic Locking Support**
```java
@Entity
@Table(name = "products")
public class Product {
    
    @Id
    private Long id;
    
    @Version  // JPA optimistic locking
    private Long version;
    
    private String name;
    private BigDecimal price;
    
    // When 2 users update:
    // User A: version=5 → UPDATE succeeds (version=6)
    // User B: version=5 → OptimisticLockException (stale)
}

@ControllerAdvice
public class OptimisticLockExceptionHandler {
    
    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<?> handleStaleLock(OptimisticLockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of(
                "error", "EntityModifiedByAnotherUser",
                "message", "Entity was modified by another user. Please refresh and try again."
            ));
    }
}
```

**6. Presence Tracking (WebSocket)**
```java
@Component
public class PresenceTracker {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final Map<String, Set<String>> entityLocks = new ConcurrentHashMap<>();
    
    @MessageMapping("/presence/lock")
    public void lockEntity(@Payload LockRequest request, Principal user) {
        String entityKey = request.getEntityType() + ":" + request.getEntityId();
        
        entityLocks.computeIfAbsent(entityKey, k -> ConcurrentHashMap.newKeySet())
            .add(user.getName());
        
        // Broadcast to all users viewing this entity
        messagingTemplate.convertAndSend(
            "/topic/presence/" + entityKey,
            Map.of(
                "type", "locked",
                "user", user.getName(),
                "timestamp", Instant.now()
            )
        );
        
        // Auto-unlock after 5 minutes
        scheduleAutoUnlock(entityKey, user.getName(), Duration.ofMinutes(5));
    }
}
```

---

## 🧪 Testing Strategy

### Unit Tests
```java
@Test
void shouldEnqueueEventWithCorrectPriority() {
    EntityEvent criticalEvent = createEvent("PaymentFailed", "CRITICAL");
    EntityEvent normalEvent = createEvent("OrderCreated", "NORMAL");
    
    queue.enqueue(criticalEvent, "CRITICAL");
    queue.enqueue(normalEvent, "NORMAL");
    
    // CRITICAL should be processed first
    verify(kafkaProducer).send(argThat(e -> e.getEventType().equals("PaymentFailed")));
}

@Test
void shouldMoveToDLQAfter3Retries() {
    when(kafkaTemplate.send(anyString(), anyString(), any()))
        .thenThrow(new RuntimeException("Network error"));
    
    kafkaProducer.send(createEvent("OrderCreated", "NORMAL"));
    
    verify(kafkaTemplate, times(4)).send(anyString(), anyString(), any()); // 1 + 3 retries
    verify(kafkaTemplate).send(eq("dlq.events"), anyString(), any(DLQEvent.class));
}
```

---

## 📦 Deliverables

1. **Event Publishing** (~600 LOC)
   - `EntityEventPublisher.java`
   - `EventPayloadBuilder.java`

2. **Priority Queue** (~500 LOC)
   - `PriorityEventQueue.java`
   - `EventConsumerThread.java`

3. **Kafka Integration** (~400 LOC)
   - `KafkaEventProducer.java`
   - `DLQHandler.java`

4. **Presence & Locking** (~300 LOC)
   - `PresenceTracker.java`
   - `OptimisticLockHandler.java`

5. **Tests** (~500 LOC)

---

## 🎯 Success Metrics

- ✅ Events publikovány do Kafky pro všechny entity changes
- ✅ Priority queue funguje (CRITICAL → BULK pořadí)
- ✅ DLQ zachytí < 0.1% failed events
- ✅ Optimistic locking zabrání concurrent updates

---

**Story Owner:** Backend Team  
**Priority:** P0 - CRITICAL  
**Effort:** 4 týdny  
**Start ASAP:** Schema existuje, runtime MUSÍ být hotový!
