# WF14: KAFKA_COMMAND Executor - Async Command/Reply Pattern

**Typ:** TASK  
**Epic:** EPIC-006 (Workflow Engine - Internal Layer)  
**Fase:** Phase 2 (Typed Executors)  
**Priorita:** MEDIUM (async integration pattern)  
**Effort:** 600 LOC, 2 dny  
**Dependencies:** W7 (Executor Framework), Kafka infrastructure  
**Status:** ⏳ TODO

---

## 🎯 Cíl

Implementovat **KAFKA_COMMAND executor** pro asynchronní command/reply pattern přes Kafka. Executor:
- Publikuje command do Kafka topic
- Čeká na reply v reply topic (s timeout)
- Koreluje request/response přes correlation ID
- Podporuje DLQ (Dead Letter Queue) pro timeout/failure
- Validuje schema (AsyncAPI, Avro, JSON Schema)

**Use Case:**
```yaml
steps:
  - id: "trigger-batch-export"
    type: "KAFKA_COMMAND"
    config:
      commandTopic: "export-commands"
      replyTopic: "export-replies"
      command:
        type: "EXPORT_DATA"
        payload:
          entityType: "${workflow.context.entityType}"
          filters: "${workflow.context.filters}"
          format: "CSV"
      timeout: 300s  # 5 minut
      schema:
        type: "AVRO"
        registry: "http://schema-registry:8081"
        subject: "export-command-value"
      dlq:
        topic: "export-commands-dlq"
        onTimeout: true
        onError: true
      outputMapping:
        exportId: "$.exportId"
        downloadUrl: "$.downloadUrl"
```

---

## 📋 Požadavky

### Funkční Požadavky

1. **Kafka Producer**
   - Publikuje command message do topic
   - Correlation ID v header (`X-Correlation-ID`)
   - Timestamp, metadata (workflow instance ID, step ID)
   - Transactional producer (exactly-once semantics)

2. **Kafka Consumer (Reply Listener)**
   - Konzumuje reply topic
   - Filtruje zprávy podle correlation ID
   - Timeout detection (pokud reply nedorazí)
   - Commit offset po úspěšném zpracování

3. **Schema Validation**
   - AsyncAPI spec support
   - Avro schema (Schema Registry integration)
   - JSON Schema validation
   - Backward/forward compatibility check

4. **Error Handling**
   - Timeout → DLQ + workflow error
   - Deserialization error → DLQ + log
   - Consumer lag → alerting

5. **Correlation**
   - UUID correlation ID
   - Persistent storage (workflow_kafka_commands table)
   - Reply matching (correlation ID → workflow instance)

### Non-Funkční Požadavky

1. **Performance**
   - Max 10ms p99 publish latency
   - Consumer lag < 100ms p99
   - Batch publish (configurable batch size)

2. **Reliability**
   - Exactly-once producer (transactional)
   - At-least-once consumer (manual commit)
   - DLQ fallback (no message loss)

3. **Observability**
   - Prometheus metrics (publish rate, consumer lag, timeout rate)
   - Distributed tracing (OpenTelemetry)
   - Consumer group monitoring

---

## 🗄️ Database Schema

```sql
-- Command/reply tracking
CREATE TABLE workflow_kafka_commands (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_instance_id UUID NOT NULL REFERENCES workflow_instances(id) ON DELETE CASCADE,
    step_id VARCHAR(100) NOT NULL,
    correlation_id UUID NOT NULL UNIQUE,
    
    -- Command details
    command_topic VARCHAR(255) NOT NULL,
    command_payload JSONB NOT NULL,
    command_headers JSONB,
    
    -- Reply details
    reply_topic VARCHAR(255) NOT NULL,
    reply_payload JSONB,
    reply_headers JSONB,
    reply_received_at TIMESTAMP,
    
    -- Timing
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    timeout_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    
    -- Status
    status VARCHAR(50) NOT NULL, -- SENT, REPLY_RECEIVED, TIMEOUT, ERROR
    error_message TEXT,
    
    -- DLQ
    dlq_topic VARCHAR(255),
    dlq_sent_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kafka_commands_workflow_instance ON workflow_kafka_commands(workflow_instance_id);
CREATE INDEX idx_kafka_commands_correlation_id ON workflow_kafka_commands(correlation_id);
CREATE INDEX idx_kafka_commands_status ON workflow_kafka_commands(status);
CREATE INDEX idx_kafka_commands_timeout_at ON workflow_kafka_commands(timeout_at) WHERE status = 'SENT';
```

---

## 🔧 Implementace

### 1. Java Implementation

**File:** `backend/src/main/java/cz/muriel/core/workflow/executor/KafkaCommandExecutor.java`

```java
package cz.muriel.core.workflow.executor;

import cz.muriel.core.workflow.model.WorkflowExecution;
import cz.muriel.core.workflow.model.WorkflowStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaCommandExecutor implements WorkflowExecutor {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final WorkflowKafkaCommandRepository commandRepository;
    private final KafkaReplyWaiter replyWaiter;
    private final TemplateEngine templateEngine;
    private final MetricsService metricsService;
    
    @Override
    public boolean supports(String executorType) {
        return "KAFKA_COMMAND".equals(executorType);
    }
    
    @Override
    public Map<String, Object> execute(WorkflowExecution execution, WorkflowStep step) {
        log.info("Executing KAFKA_COMMAND step: workflowInstanceId={}, stepId={}", 
            execution.getInstanceId(), step.getId());
        
        KafkaCommandConfig config = parseConfig(step.getConfig());
        
        // 1. Generate correlation ID
        UUID correlationId = UUID.randomUUID();
        
        // 2. Resolve template variables in command payload
        String resolvedPayload = templateEngine.resolve(
            config.getCommand().toString(), 
            execution.getContext()
        );
        
        // 3. Create tracking record
        WorkflowKafkaCommand command = new WorkflowKafkaCommand();
        command.setWorkflowInstanceId(execution.getInstanceId());
        command.setStepId(step.getId());
        command.setCorrelationId(correlationId);
        command.setCommandTopic(config.getCommandTopic());
        command.setCommandPayload(resolvedPayload);
        command.setReplyTopic(config.getReplyTopic());
        command.setSentAt(Instant.now());
        command.setTimeoutAt(Instant.now().plus(Duration.ofSeconds(config.getTimeoutSeconds())));
        command.setStatus("SENT");
        commandRepository.save(command);
        
        // 4. Publish command to Kafka
        Map<String, String> headers = Map.of(
            "X-Correlation-ID", correlationId.toString(),
            "X-Workflow-Instance-ID", execution.getInstanceId().toString(),
            "X-Step-ID", step.getId()
        );
        
        try {
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(
                config.getCommandTopic(),
                correlationId.toString(), // key = correlation ID
                resolvedPayload
            );
            
            // Wait for ack
            future.get(5, TimeUnit.SECONDS);
            
            log.info("Command published: topic={}, correlationId={}", 
                config.getCommandTopic(), correlationId);
            
            metricsService.recordKafkaCommandSent(config.getCommandTopic());
            
        } catch (Exception e) {
            log.error("Failed to publish command: {}", e.getMessage(), e);
            command.setStatus("ERROR");
            command.setErrorMessage(e.getMessage());
            commandRepository.save(command);
            
            // Send to DLQ if configured
            if (config.getDlq() != null && config.getDlq().isOnError()) {
                sendToDLQ(config.getDlq().getTopic(), resolvedPayload, correlationId, e);
            }
            
            throw new WorkflowExecutionException("KAFKA_COMMAND publish failed: " + e.getMessage(), e);
        }
        
        // 5. Wait for reply (blocking with timeout)
        try {
            Map<String, Object> reply = replyWaiter.waitForReply(
                correlationId,
                config.getReplyTopic(),
                Duration.ofSeconds(config.getTimeoutSeconds())
            );
            
            // Update tracking record
            command.setReplyPayload(reply);
            command.setReplyReceivedAt(Instant.now());
            command.setStatus("REPLY_RECEIVED");
            command.setCompletedAt(Instant.now());
            commandRepository.save(command);
            
            log.info("Reply received: correlationId={}, latency={}ms", 
                correlationId, 
                Duration.between(command.getSentAt(), command.getReplyReceivedAt()).toMillis()
            );
            
            metricsService.recordKafkaCommandReply(config.getCommandTopic(), "success");
            
            // 6. Apply output mapping
            return applyOutputMapping(reply, config.getOutputMapping());
            
        } catch (TimeoutException e) {
            log.error("Reply timeout: correlationId={}, timeout={}s", 
                correlationId, config.getTimeoutSeconds());
            
            command.setStatus("TIMEOUT");
            command.setErrorMessage("Reply timeout after " + config.getTimeoutSeconds() + "s");
            commandRepository.save(command);
            
            // Send to DLQ if configured
            if (config.getDlq() != null && config.getDlq().isOnTimeout()) {
                sendToDLQ(config.getDlq().getTopic(), resolvedPayload, correlationId, e);
            }
            
            metricsService.recordKafkaCommandReply(config.getCommandTopic(), "timeout");
            
            throw new WorkflowExecutionException("KAFKA_COMMAND timeout: " + e.getMessage(), e);
        }
    }
    
    private void sendToDLQ(String dlqTopic, String payload, UUID correlationId, Exception error) {
        try {
            Map<String, String> dlqHeaders = Map.of(
                "X-Correlation-ID", correlationId.toString(),
                "X-Error-Message", error.getMessage(),
                "X-Error-Timestamp", Instant.now().toString()
            );
            
            kafkaTemplate.send(dlqTopic, correlationId.toString(), payload).get(5, TimeUnit.SECONDS);
            
            log.info("Sent to DLQ: topic={}, correlationId={}", dlqTopic, correlationId);
            
        } catch (Exception dlqError) {
            log.error("Failed to send to DLQ: {}", dlqError.getMessage(), dlqError);
        }
    }
    
    @Override
    public void compensate(WorkflowExecution execution, WorkflowStep step) {
        log.warn("KAFKA_COMMAND compensation not implemented");
        // TODO: Publish compensation command (e.g., CANCEL_EXPORT)
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/workflow/executor/KafkaReplyWaiter.java`

```java
@Component
public class KafkaReplyWaiter {
    
    private final Map<UUID, CompletableFuture<Map<String, Object>>> pendingReplies = new ConcurrentHashMap<>();
    
    @KafkaListener(topics = "${workflow.kafka.reply-topics}", groupId = "workflow-reply-listener")
    public void handleReply(ConsumerRecord<String, String> record) {
        String correlationIdHeader = record.headers()
            .lastHeader("X-Correlation-ID")
            .value();
        
        if (correlationIdHeader == null) {
            log.warn("Reply without correlation ID: topic={}, offset={}", 
                record.topic(), record.offset());
            return;
        }
        
        UUID correlationId = UUID.fromString(new String(correlationIdHeader));
        
        CompletableFuture<Map<String, Object>> future = pendingReplies.get(correlationId);
        if (future != null) {
            Map<String, Object> payload = parsePayload(record.value());
            future.complete(payload);
            pendingReplies.remove(correlationId);
            
            log.info("Reply matched: correlationId={}, topic={}", correlationId, record.topic());
        } else {
            log.warn("Unmatched reply: correlationId={}, topic={}", correlationId, record.topic());
        }
    }
    
    public Map<String, Object> waitForReply(UUID correlationId, String replyTopic, Duration timeout) 
            throws TimeoutException {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingReplies.put(correlationId, future);
        
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pendingReplies.remove(correlationId);
            throw e;
        } catch (Exception e) {
            pendingReplies.remove(correlationId);
            throw new RuntimeException("Reply wait failed: " + e.getMessage(), e);
        }
    }
}
```

---

### 2. Configuration

**File:** `backend/src/main/resources/application.yml`

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
      acks: all  # Ensure all replicas ack
      retries: 3
      enable-idempotence: true
    consumer:
      group-id: workflow-engine
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false  # Manual commit for at-least-once

workflow:
  kafka:
    reply-topics: export-replies,batch-replies,notification-replies
    dlq-topic: workflow-commands-dlq
```

---

### 3. Tests

**File:** `backend/src/test/java/cz/muriel/core/workflow/executor/KafkaCommandExecutorTest.java`

```java
@SpringBootTest
@Testcontainers
class KafkaCommandExecutorTest {
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(
        DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );
    
    @Autowired
    private KafkaCommandExecutor executor;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private WorkflowKafkaCommandRepository repository;
    
    @Test
    void shouldSendCommandAndReceiveReply() throws Exception {
        // Given: Setup reply listener
        String commandTopic = "test-commands";
        String replyTopic = "test-replies";
        
        // Simulate reply producer
        kafkaTemplate.send(replyTopic, "correlation-123", "{\"status\": \"success\"}");
        
        WorkflowStep step = WorkflowStep.builder()
            .id("send-command")
            .type("KAFKA_COMMAND")
            .config(Map.of(
                "commandTopic", commandTopic,
                "replyTopic", replyTopic,
                "command", Map.of("action", "PROCESS"),
                "timeout", 10
            ))
            .build();
        
        // When
        Map<String, Object> output = executor.execute(mockExecution(), step);
        
        // Then
        assertThat(output).containsEntry("status", "success");
        
        // Verify tracking record
        List<WorkflowKafkaCommand> commands = repository.findByWorkflowInstanceId(mockExecution().getInstanceId());
        assertThat(commands).hasSize(1);
        assertThat(commands.get(0).getStatus()).isEqualTo("REPLY_RECEIVED");
    }
    
    @Test
    void shouldHandleTimeout() {
        // Given: No reply listener (timeout scenario)
        WorkflowStep step = WorkflowStep.builder()
            .config(Map.of(
                "commandTopic", "test-commands",
                "replyTopic", "test-replies",
                "timeout", 2  // 2 seconds
            ))
            .build();
        
        // When/Then
        assertThatThrownBy(() -> executor.execute(mockExecution(), step))
            .isInstanceOf(WorkflowExecutionException.class)
            .hasMessageContaining("timeout");
        
        // Verify status
        List<WorkflowKafkaCommand> commands = repository.findByWorkflowInstanceId(mockExecution().getInstanceId());
        assertThat(commands.get(0).getStatus()).isEqualTo("TIMEOUT");
    }
    
    @Test
    void shouldSendToDLQOnTimeout() {
        // Given: DLQ configured
        String dlqTopic = "test-dlq";
        
        WorkflowStep step = WorkflowStep.builder()
            .config(Map.of(
                "timeout", 1,
                "dlq", Map.of("topic", dlqTopic, "onTimeout", true)
            ))
            .build();
        
        // When
        try {
            executor.execute(mockExecution(), step);
        } catch (WorkflowExecutionException e) {
            // Expected
        }
        
        // Then: Verify DLQ message
        ConsumerRecords<String, String> dlqRecords = consumeFromTopic(dlqTopic);
        assertThat(dlqRecords).hasSize(1);
    }
}
```

---

### 4. Metrics

```java
@Component
public class KafkaCommandMetrics {
    
    private final Counter commandsSent;
    private final Counter repliesReceived;
    private final Histogram replyLatency;
    
    public KafkaCommandMetrics(MeterRegistry registry) {
        this.commandsSent = Counter.builder("workflow_kafka_commands_sent_total")
            .tag("topic", "")
            .register(registry);
        
        this.repliesReceived = Counter.builder("workflow_kafka_replies_received_total")
            .tag("topic", "")
            .tag("status", "")
            .register(registry);
        
        this.replyLatency = Histogram.builder("workflow_kafka_reply_latency_ms")
            .tag("topic", "")
            .register(registry);
    }
}
```

---

## ✅ Acceptance Criteria

1. **Funkční:**
   - [ ] Command publikován do Kafka topic
   - [ ] Correlation ID v header
   - [ ] Reply listener filtruje zprávy podle correlation ID
   - [ ] Timeout detection (pokud reply nedorazí)
   - [ ] DLQ fallback (timeout/error)
   - [ ] Output mapping (reply → workflow context)

2. **Performance:**
   - [ ] Publish latency < 10ms p99
   - [ ] Consumer lag < 100ms p99

3. **Reliability:**
   - [ ] Exactly-once producer
   - [ ] At-least-once consumer
   - [ ] No message loss (DLQ)

4. **Testy:**
   - [ ] Integration test: KafkaContainer
   - [ ] Test reply matching (correlation ID)
   - [ ] Test timeout scenario
   - [ ] Test DLQ fallback

---

**Related Stories:**
- W7: Workflow Executor Framework
- WF13: REST_SYNC Executor
- WF15: EXTERNAL_TASK Executor
