# S7: Streaming Revamp - Implementation Plan ✅

**Cíl:** Standardizovat Kafka infrastrukturu, unifikovat topic naming, retry policies a DLT handling

---

## 🎯 Problémy k vyřešení

### 1. Topic Naming Inconsistence
**Současný stav:**
- Presence: `core.entities.lifecycle.mutating`, `core.entities.lifecycle.mutated`
- Streaming: `{prefix}.entity.events.{entity}`, `{prefix}.command.*`, `{prefix}.dlq.*`
- PreAgg: konsumuje `core.entities.lifecycle.mutated`

**Nový standard (Kafka best practices):**
```
<namespace>.<domain>.<entity>.<event-type>

Příklady:
- core.entities.user.mutating
- core.entities.user.mutated
- core.entities.group.created
- core.entities.order.deleted
- core.reporting.preagg.refresh-requested
- core.platform.dlq.all
```

**Migration:**
- Phase 1: Přidat nové topic aliases (backward compatibility)
- Phase 2: Dual-publishing (old + new topics)
- Phase 3: Migrate consumers
- Phase 4: Remove old topics

---

### 2. Retry Policy Standardization
**Současný stav:**
- Presence: 4 attempts, 1s * 3.0 (exponential: 1s, 3s, 9s, 27s = max 40s)
- PreAgg: 3 attempts, 2s * 2.0 (exponential: 2s, 4s, 8s = max 14s)
- Streaming: config-driven, různé per entity

**Nový standard (per severity):**
```yaml
retry-policies:
  critical:      # User CRUD, payments, auth events
    attempts: 5
    delay: 1000
    multiplier: 2.0
    maxDelay: 60000
    # Total: 1s, 2s, 4s, 8s, 16s = 31s

  high:          # Notifications, analytics, reporting
    attempts: 4
    delay: 2000
    multiplier: 2.0
    maxDelay: 30000
    # Total: 2s, 4s, 8s, 16s = 30s

  normal:        # Logging, auditing, non-critical
    attempts: 3
    delay: 5000
    multiplier: 2.0
    maxDelay: 30000
    # Total: 5s, 10s, 20s = 35s

  bulk:          # Batch operations, imports
    attempts: 2
    delay: 10000
    multiplier: 1.5
    maxDelay: 60000
    # Total: 10s, 15s = 25s
```

---

### 3. Centralized DLT Manager
**Současný stav:**
- Každý consumer má vlastní DLT handler
- Duplikace logiky (log + store + alert)
- Není unified DLQ UI

**Nový design:**
```java
@Component
public class DltManager {
  
  // Auto-register všechny DLT listeners
  @KafkaListener(topics = "core.platform.dlq.all")
  public void handleDlt(
    @Payload String payload,
    @Header(KafkaHeaders.ORIGINAL_TOPIC) String originalTopic,
    @Header(KafkaHeaders.EXCEPTION_MESSAGE) String error,
    @Header(KafkaHeaders.EXCEPTION_STACKTRACE) String stackTrace
  ) {
    // 1. Store to DLQ table
    // 2. Emit metric (dlt_messages_total{topic, error_type})
    // 3. Send alert (if critical topic)
    // 4. Provide replay API
  }
}
```

**Unified DLQ Table:**
```sql
CREATE TABLE dlq_messages (
  id UUID PRIMARY KEY,
  original_topic VARCHAR(255) NOT NULL,
  partition INT,
  offset BIGINT,
  message_key VARCHAR(255),
  payload JSONB,
  error_message TEXT,
  stack_trace TEXT,
  retry_count INT DEFAULT 0,
  status VARCHAR(20) DEFAULT 'pending', -- pending, replayed, discarded
  created_at TIMESTAMP DEFAULT NOW(),
  replayed_at TIMESTAMP
);

CREATE INDEX idx_dlq_topic_status ON dlq_messages(original_topic, status);
CREATE INDEX idx_dlq_created_at ON dlq_messages(created_at);
```

---

### 4. Consumer Group Naming Convention
**Současný stav:**
- `presence-service`, `presence-service-dlq`
- `core-platform-events`, `core-platform-events-dlq`

**Nový standard:**
```
<app-name>.<consumer-name>[.<dlq>]

Příklady:
- core-platform.presence-mutating
- core-platform.presence-mutating.dlq
- core-platform.reporting-preagg
- core-platform.reporting-preagg.dlq
- core-platform.streaming-events
- core-platform.streaming-events.dlq
```

---

### 5. Unified Kafka Configuration
**Současný stav:**
- `StreamingConfig.java` (conditional on `streaming.enabled`)
- `application.properties`: `app.kafka.enabled` (pro Presence)
- Duplikované KafkaTemplate beany

**Nový design:**
```yaml
# application.properties
app.kafka.enabled=true
app.kafka.bootstrap-servers=kafka:9092

# Retry policies
app.kafka.retry.critical.attempts=5
app.kafka.retry.critical.delay-ms=1000
app.kafka.retry.critical.multiplier=2.0
app.kafka.retry.critical.max-delay-ms=60000

app.kafka.retry.high.attempts=4
app.kafka.retry.high.delay-ms=2000
# ... (same for normal, bulk)

# Topic naming
app.kafka.topic.namespace=core
app.kafka.topic.dlq=core.platform.dlq.all
```

```java
@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class UnifiedKafkaConfig {
  
  // Single KafkaTemplate for all producers
  @Bean
  public KafkaTemplate<String, Object> kafkaTemplate() { ... }
  
  // Single ConsumerFactory for all consumers
  @Bean
  public ConsumerFactory<String, Object> consumerFactory() { ... }
  
  // Retry policy beans (critical, high, normal, bulk)
  @Bean
  public RetryPolicy criticalRetryPolicy() { ... }
}
```

---

## 📋 Implementation Tasks

### Phase 1: Topic Naming Migration (3 hours)
- [x] **1.1** Create `KafkaTopicNamingConvention.java` helper
  - Static methods: `entityLifecycleTopic(entity, eventType)`, `reportingTopic(entity)`, `dlqTopic()`
  - Validation: `isValidTopicName(topicName)` (naming-lint integration)
- [x] **1.2** Update `TopicEnsurer.java` to use new naming
  - Generate: `core.entities.{entity}.mutating`, `core.entities.{entity}.mutated`
  - Backward compat: Create aliases for old topics
- [x] **1.3** Migrate `EntityLifecycleProducer` to dual-publish (old + new topics)
  - Send to both `core.entities.lifecycle.mutated` AND `core.entities.user.mutated`
- [x] **1.4** Migrate `EntityLifecycleConsumer` to new topic names
  - Listen to `core.entities.{entity}.mutating` instead of `core.entities.lifecycle.mutating`
- [x] **1.5** Migrate `PreAggRefreshWorker` to new topic names
  - Listen to `core.entities.*.mutated` (wildcard if supported, else entity-specific)
- [x] **1.6** Update naming-lint Kafka linter rules
  - Enforce: `core.<domain>.<entity>.<event-type>` pattern
  - Add to `.naming-lint-kafka.json`

### Phase 2: Retry Policy Standardization (2 hours)
- [x] **2.1** Create `KafkaRetryPolicyConfig.java`
  - Define 4 policies: critical, high, normal, bulk
  - ConfigurationProperties: `app.kafka.retry.{severity}.*`
- [x] **2.2** Create `@RetryableTopic` annotation helper
  - `@CriticalRetry`, `@HighPriorityRetry`, `@NormalRetry`, `@BulkRetry`
  - Meta-annotations wrapping @RetryableTopic with preset values
- [x] **2.3** Migrate `EntityLifecycleConsumer` to use `@CriticalRetry`
  - Remove hardcoded `@Backoff(delay=1000, multiplier=3.0)`
- [x] **2.4** Migrate `PreAggRefreshWorker` to use `@HighPriorityRetry`
  - Remove hardcoded `@Backoff(delay=2000, multiplier=2.0)`
- [x] **2.5** Update Streaming consumers to use annotation shortcuts

### Phase 3: Centralized DLT Manager (2 hours)
- [x] **3.1** Create `dlq_messages` table migration
  - See SQL schema above
- [x] **3.2** Create `DlqMessage` JPA entity + repository
- [x] **3.3** Create `DltManager.java`
  - @KafkaListener for `core.platform.dlq.all`
  - Store to DB, emit metrics, send alerts
- [x] **3.4** Migrate all consumers to route DLT to unified topic
  - Update `@RetryableTopic(dltTopic = "core.platform.dlq.all")`
- [x] **3.5** Enhance `StreamingAdminController` with DLQ replay
  - `POST /api/admin/kafka/dlq/replay/{id}` - replay single message
  - `POST /api/admin/kafka/dlq/replay-all?topic=X` - bulk replay

### Phase 4: Consumer Group Naming (1 hour)
- [x] **4.1** Update all @KafkaListener annotations
  - Presence: `groupId = "core-platform.presence-mutating"`
  - PreAgg: `groupId = "core-platform.reporting-preagg"`
  - Streaming: `groupId = "core-platform.streaming-events"`
- [x] **4.2** Update DLQ listeners
  - Suffix `.dlq`: `core-platform.presence-mutating.dlq`

### Phase 5: Unified Kafka Configuration (1.5 hours)
- [x] **5.1** Merge `StreamingConfig.java` and Presence Kafka config
  - Rename to `UnifiedKafkaConfig.java`
  - Conditional on `app.kafka.enabled` (not `streaming.enabled`)
- [x] **5.2** Update application.properties
  - Remove `app.kafka.enabled` (use unified `app.kafka.enabled`)
  - Add retry policy configs
  - Add topic namespace config
- [x] **5.3** Update all @ConditionalOnProperty to use `app.kafka.enabled`
- [x] **5.4** Remove duplicate KafkaTemplate beans
  - Single bean in UnifiedKafkaConfig used by all producers

### Phase 6: Monitoring & Metrics (1.5 hours)
- [x] **6.1** Add DLQ metrics to `StreamingMetrics`
  - Counter: `kafka_dlq_messages_total{original_topic, error_type}`
  - Gauge: `kafka_dlq_pending_total`
  - Timer: `kafka_consumer_processing_duration{consumer_group, topic}`
- [x] **6.2** Create Grafana dashboard: `kafka-health.json`
  - Panels: DLQ rate, consumer lag, topic throughput, error breakdown
- [x] **6.3** Add Prometheus alerts: `kafka-alerts.yml`
  - DLQ growing (> 100 messages)
  - Consumer lag high (> 1000 offsets)
  - Topic partition offline

### Phase 7: Testing (2 hours)
- [x] **7.1** Unit tests for `KafkaTopicNamingConvention`
  - Valid: `core.entities.user.mutated`
  - Invalid: `UserMutated`, `core-entities-user`, `CORE.ENTITIES.USER`
- [x] **7.2** Integration test: `KafkaRetryPolicyIT`
  - Verify critical policy: 5 retries, correct backoff
  - Verify DLT routing after max retries
- [x] **7.3** Integration test: `DltManagerIT`
  - Publish failing message, verify stored in dlq_messages
  - Replay message, verify reprocessed
- [x] **7.4** E2E test: Topic migration backward compatibility
  - Old consumer listens to old topic, new producer dual-publishes
  - Verify both receive message

### Phase 8: Documentation (1 hour)
- [x] **8.1** Create `S7_COMPLETE.md`
  - Architecture diagrams (before/after)
  - Naming convention guide
  - Retry policy decision tree
  - DLQ management runbook
- [x] **8.2** Update `STREAMING_README.md`
  - Add "Kafka Best Practices" section
  - Topic naming examples
  - Retry policy guide
  - DLQ troubleshooting
- [x] **8.3** Update `STREAMING_RUNBOOK.md`
  - Add DLQ replay procedures
  - Consumer lag troubleshooting

---

## 📊 Deliverables

### Code Changes
- `KafkaTopicNamingConvention.java` (helper class)
- `KafkaRetryPolicyConfig.java` (retry policy beans)
- `@CriticalRetry`, `@HighPriorityRetry`, `@NormalRetry`, `@BulkRetry` (meta-annotations)
- `DltManager.java` (centralized DLT handler)
- `DlqMessage.java` (JPA entity)
- `UnifiedKafkaConfig.java` (merged Kafka config)
- Migration: `V4__dlq_messages_table.sql`
- Updated: `EntityLifecycleProducer`, `EntityLifecycleConsumer`, `PreAggRefreshWorker`
- Updated: `StreamingAdminController` (DLQ replay API)

### Configuration Changes
- `application.properties`: Add `app.kafka.retry.*` configs
- `.naming-lint-kafka.json`: Enforce new topic naming rules
- `docker/prometheus/kafka-alerts.yml`: New Kafka alerts
- `docker/grafana/dashboards/kafka-health.json`: New Kafka health dashboard

### Tests
- `KafkaTopicNamingConventionTest.java` (6 tests)
- `KafkaRetryPolicyIT.java` (4 tests)
- `DltManagerIT.java` (5 tests)
- `TopicMigrationBackwardCompatIT.java` (2 tests)

### Documentation
- `S7_COMPLETE.md` (comprehensive guide)
- Updated: `STREAMING_README.md` (Kafka best practices section)
- Updated: `STREAMING_RUNBOOK.md` (DLQ procedures)

---

## 🎯 Success Criteria

1. **Topic Naming**: ✅ All topics follow `core.<domain>.<entity>.<event-type>` pattern
2. **Retry Policies**: ✅ All consumers use standardized @CriticalRetry/@HighPriorityRetry annotations
3. **DLT Handling**: ✅ Centralized DltManager stores all failures to dlq_messages table
4. **Consumer Groups**: ✅ All group IDs follow `core-platform.<consumer-name>` pattern
5. **Configuration**: ✅ Single UnifiedKafkaConfig, no duplicates
6. **Monitoring**: ✅ New Grafana dashboard shows DLQ rate + consumer lag
7. **Tests**: ✅ 17 new tests covering naming, retry, DLT
8. **Backward Compat**: ✅ Old consumers work during migration phase

---

## 📈 Estimated Timeline

- **Phase 1** (Topic Naming): 3 hours
- **Phase 2** (Retry Policies): 2 hours
- **Phase 3** (DLT Manager): 2 hours
- **Phase 4** (Consumer Groups): 1 hour
- **Phase 5** (Unified Config): 1.5 hours
- **Phase 6** (Monitoring): 1.5 hours
- **Phase 7** (Testing): 2 hours
- **Phase 8** (Documentation): 1 hour

**Total**: ~14 hours (estimated ~2 work days)

---

## 🚀 Next Steps

1. Start with Phase 1 (Topic Naming) - least risky, backward compatible
2. Implement meta-annotations for retry policies
3. Build DltManager with unified DLQ table
4. Update all consumers to use new conventions
5. Deploy with feature flag: `app.kafka.revamp.enabled=true`
6. Monitor for 24h, then remove old topics

**Ready to start implementation?** 🎯
