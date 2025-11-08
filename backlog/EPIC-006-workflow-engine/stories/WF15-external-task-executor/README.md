# WF15: EXTERNAL_TASK Executor - n8n Integration Bridge

**Typ:** TASK  
**Epic:** EPIC-006 (Workflow Engine - Internal Layer)  
**Fase:** Phase 2 (Typed Executors)  
**Priorita:** 🔴 CRITICAL (klíč pro n8n integraci)  
**Effort:** 800 LOC, 3 dny  
**Dependencies:** W7 (Executor Framework), N8N6 (BFF API)  
**Status:** ⏳ TODO

---

## 🎯 Cíl

Implementovat **EXTERNAL_TASK executor** - polling pattern pro externí workers (primárně n8n). Executor:
- Vytvoří external task record (PENDING status)
- Worker (n8n) polluje tasks via REST API
- Worker lockne task, provede workflow, vrátí výsledek
- Executor čeká na completion (s timeout)
- Podporuje heartbeat (keep-alive), retry, DLQ

**Use Case:**
```yaml
steps:
  - id: "create-jira-ticket-via-n8n"
    type: "EXTERNAL_TASK"
    config:
      taskType: "n8n-jira-create-ticket"
      input:
        projectKey: "${workflow.context.projectKey}"
        summary: "${workflow.context.ticketTitle}"
        description: "${workflow.context.description}"
        priority: "HIGH"
      timeout: 600s  # 10 minut
      lockDuration: 300s  # 5 minut
      retries: 3
      heartbeat:
        interval: 60s  # Worker musí poslat heartbeat každou minutu
        maxMissed: 2   # Max 2 missed heartbeats → unlock task
      outputMapping:
        jiraTicketId: "$.result.ticketId"
        jiraTicketUrl: "$.result.url"
```

**n8n Worker Workflow:**
```json
{
  "nodes": [
    {
      "name": "Poll Tasks",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "url": "https://admin.core-platform.local/api/n8n/external-tasks/poll?taskType=n8n-jira-create-ticket",
        "method": "GET"
      }
    },
    {
      "name": "Create Jira Ticket",
      "type": "n8n-nodes-base.jira",
      "parameters": {
        "operation": "create",
        "projectKey": "={{$node['Poll Tasks'].json.input.projectKey}}",
        "summary": "={{$node['Poll Tasks'].json.input.summary}}"
      }
    },
    {
      "name": "Complete Task",
      "type": "n8n-nodes-base.httpRequest",
      "parameters": {
        "url": "https://admin.core-platform.local/api/n8n/external-tasks/{{$node['Poll Tasks'].json.id}}/complete",
        "method": "POST",
        "body": {
          "result": {
            "ticketId": "={{$node['Create Jira Ticket'].json.id}}",
            "url": "={{$node['Create Jira Ticket'].json.self}}"
          }
        }
      }
    }
  ]
}
```

---

## 📋 Požadavky

### Funkční Požadavky

1. **Task Creation**
   - Vytvoří workflow_external_tasks record (status: PENDING)
   - Input data v JSONB (template substitution)
   - Timeout timestamp (started_at + timeout duration)
   - Task type classifier (pro routing k správnému workeru)

2. **Worker Polling API**
   - `GET /api/n8n/external-tasks/poll?taskType=X&workerId=Y`
   - Long-polling support (wait 30s pokud žádná task)
   - Task locking (PENDING → LOCKED, set locked_at, worker_id)
   - Lock duration enforcement

3. **Task Completion API**
   - `POST /api/n8n/external-tasks/{id}/complete`
   - Output data v JSONB
   - Status: LOCKED → COMPLETED
   - Workflow continuation (notify executor)

4. **Task Failure API**
   - `POST /api/n8n/external-tasks/{id}/fail`
   - Error message, stack trace
   - Retry logic (retry_count < max_retries → PENDING, else → FAILED)

5. **Heartbeat API**
   - `POST /api/n8n/external-tasks/{id}/heartbeat`
   - Update last_heartbeat_at timestamp
   - Keep task locked (prevent timeout)

6. **Timeout Detection**
   - Scheduled job (každou minutu)
   - Najde tasks kde `NOW() > timeout_at AND status = 'LOCKED'`
   - Unlock task (LOCKED → PENDING) nebo fail (pokud max retries)

7. **Worker Healthcheck**
   - Scheduled job (každou minutu)
   - Najde tasks kde `NOW() > last_heartbeat_at + heartbeat_interval * max_missed`
   - Unlock task (worker zemřel)

### Non-Funkční Požadavky

1. **Performance**
   - Poll latency < 100ms (no tasks) nebo < 50ms (task available)
   - Lock acquisition atomic (optimistic locking via version)
   - Connection pooling (max 100 workers)

2. **Reliability**
   - At-least-once execution (retry on failure)
   - No task loss (DLQ for max retries exceeded)
   - Graceful shutdown (workers unlock tasks on exit)

3. **Observability**
   - Prometheus metrics (pending tasks, locked tasks, completed tasks, timeout rate)
   - Worker metrics (active workers, poll rate, completion rate)
   - Distributed tracing

---

## 🗄️ Database Schema

```sql
-- External tasks (polling pattern)
CREATE TABLE workflow_external_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_instance_id UUID NOT NULL REFERENCES workflow_instances(id) ON DELETE CASCADE,
    step_id VARCHAR(100) NOT NULL,
    
    -- Task classification
    task_type VARCHAR(100) NOT NULL, -- e.g., "n8n-jira-create-ticket"
    priority INTEGER DEFAULT 5, -- 1 (highest) to 10 (lowest)
    
    -- Task data
    input JSONB NOT NULL,
    output JSONB,
    
    -- Worker assignment
    worker_id VARCHAR(100), -- e.g., "n8n-worker-1"
    locked_at TIMESTAMP,
    lock_duration_seconds INTEGER DEFAULT 300, -- 5 minut
    
    -- Heartbeat
    last_heartbeat_at TIMESTAMP,
    heartbeat_interval_seconds INTEGER DEFAULT 60,
    heartbeat_max_missed INTEGER DEFAULT 2,
    
    -- Timeout
    timeout_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    
    -- Status
    status VARCHAR(50) NOT NULL, -- PENDING, LOCKED, COMPLETED, FAILED, CANCELLED
    
    -- Retry
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    error_message TEXT,
    
    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Optimistic locking
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_external_tasks_workflow_instance ON workflow_external_tasks(workflow_instance_id);
CREATE INDEX idx_external_tasks_poll ON workflow_external_tasks(task_type, status, priority, created_at) 
    WHERE status = 'PENDING';
CREATE INDEX idx_external_tasks_timeout ON workflow_external_tasks(timeout_at, status) 
    WHERE status IN ('PENDING', 'LOCKED');
CREATE INDEX idx_external_tasks_heartbeat ON workflow_external_tasks(last_heartbeat_at, status) 
    WHERE status = 'LOCKED';

-- Worker registry (tracking active workers)
CREATE TABLE workflow_external_workers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    worker_id VARCHAR(100) NOT NULL UNIQUE,
    worker_type VARCHAR(100), -- e.g., "n8n", "lambda", "airflow"
    
    -- Task types handled
    task_types VARCHAR(100)[], -- ["n8n-jira-*", "n8n-confluence-*"]
    
    -- Health
    last_poll_at TIMESTAMP,
    last_heartbeat_at TIMESTAMP,
    status VARCHAR(50), -- ACTIVE, IDLE, DEAD
    
    -- Metrics
    tasks_completed INTEGER DEFAULT 0,
    tasks_failed INTEGER DEFAULT 0,
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_external_workers_worker_id ON workflow_external_workers(worker_id);
CREATE INDEX idx_external_workers_status ON workflow_external_workers(status);
```

---

## 🔧 Implementace

### 1. Java Executor

**File:** `backend/src/main/java/cz/muriel/core/workflow/executor/ExternalTaskExecutor.java`

```java
package cz.muriel.core.workflow.executor;

import cz.muriel.core.workflow.model.WorkflowExecution;
import cz.muriel.core.workflow.model.WorkflowStep;
import cz.muriel.core.workflow.model.WorkflowExternalTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalTaskExecutor implements WorkflowExecutor {
    
    private final WorkflowExternalTaskRepository taskRepository;
    private final ExternalTaskCompletionWaiter completionWaiter;
    private final TemplateEngine templateEngine;
    private final MetricsService metricsService;
    
    @Override
    public boolean supports(String executorType) {
        return "EXTERNAL_TASK".equals(executorType);
    }
    
    @Override
    public Map<String, Object> execute(WorkflowExecution execution, WorkflowStep step) {
        log.info("Executing EXTERNAL_TASK step: workflowInstanceId={}, stepId={}", 
            execution.getInstanceId(), step.getId());
        
        ExternalTaskConfig config = parseConfig(step.getConfig());
        
        // 1. Resolve template variables in input
        Map<String, Object> resolvedInput = templateEngine.resolveMap(
            config.getInput(), 
            execution.getContext()
        );
        
        // 2. Create external task record
        WorkflowExternalTask task = new WorkflowExternalTask();
        task.setWorkflowInstanceId(execution.getInstanceId());
        task.setStepId(step.getId());
        task.setTaskType(config.getTaskType());
        task.setPriority(config.getPriority());
        task.setInput(resolvedInput);
        task.setStatus("PENDING");
        task.setLockDurationSeconds(config.getLockDurationSeconds());
        task.setTimeoutAt(Instant.now().plus(Duration.ofSeconds(config.getTimeoutSeconds())));
        task.setMaxRetries(config.getMaxRetries());
        
        if (config.getHeartbeat() != null) {
            task.setHeartbeatIntervalSeconds(config.getHeartbeat().getIntervalSeconds());
            task.setHeartbeatMaxMissed(config.getHeartbeat().getMaxMissed());
        }
        
        task = taskRepository.save(task);
        
        log.info("External task created: id={}, taskType={}, timeout={}s", 
            task.getId(), task.getTaskType(), config.getTimeoutSeconds());
        
        metricsService.recordExternalTaskCreated(config.getTaskType());
        
        // 3. Wait for task completion (blocking with timeout)
        try {
            Map<String, Object> output = completionWaiter.waitForCompletion(
                task.getId(),
                Duration.ofSeconds(config.getTimeoutSeconds())
            );
            
            log.info("External task completed: id={}, duration={}s", 
                task.getId(), 
                Duration.between(task.getCreatedAt(), Instant.now()).getSeconds()
            );
            
            metricsService.recordExternalTaskCompleted(config.getTaskType(), "success");
            
            // 4. Apply output mapping
            return applyOutputMapping(output, config.getOutputMapping());
            
        } catch (TimeoutException e) {
            log.error("External task timeout: id={}, timeout={}s", 
                task.getId(), config.getTimeoutSeconds());
            
            // Update task status
            task.setStatus("FAILED");
            task.setErrorMessage("Timeout after " + config.getTimeoutSeconds() + "s");
            taskRepository.save(task);
            
            metricsService.recordExternalTaskCompleted(config.getTaskType(), "timeout");
            
            throw new WorkflowExecutionException("EXTERNAL_TASK timeout: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void compensate(WorkflowExecution execution, WorkflowStep step) {
        log.info("Compensating EXTERNAL_TASK step: stepId={}", step.getId());
        
        // TODO: Create compensation external task (e.g., "n8n-jira-delete-ticket")
    }
}
```

---

### 2. REST API Controller

**File:** `backend/src/main/java/cz/muriel/core/workflow/api/ExternalTaskController.java`

```java
package cz.muriel.core.workflow.api;

import cz.muriel.core.workflow.model.WorkflowExternalTask;
import cz.muriel.core.workflow.service.ExternalTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/n8n/external-tasks")
@RequiredArgsConstructor
public class ExternalTaskController {
    
    private final ExternalTaskService taskService;
    
    /**
     * Poll for pending tasks (worker API)
     * Long-polling: wait 30s if no tasks available
     */
    @GetMapping("/poll")
    public ResponseEntity<List<WorkflowExternalTask>> pollTasks(
            @RequestParam String taskType,
            @RequestParam(required = false) String workerId,
            @RequestParam(defaultValue = "1") int maxTasks,
            @RequestParam(defaultValue = "30") int waitSeconds
    ) {
        log.debug("Polling tasks: taskType={}, workerId={}, maxTasks={}", 
            taskType, workerId, maxTasks);
        
        List<WorkflowExternalTask> tasks = taskService.pollTasks(
            taskType, 
            workerId, 
            maxTasks, 
            waitSeconds
        );
        
        if (tasks.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        
        log.info("Tasks polled: count={}, taskType={}, workerId={}", 
            tasks.size(), taskType, workerId);
        
        return ResponseEntity.ok(tasks);
    }
    
    /**
     * Complete task (worker callback)
     */
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<Void> completeTask(
            @PathVariable UUID taskId,
            @RequestBody Map<String, Object> output
    ) {
        log.info("Completing task: taskId={}", taskId);
        
        taskService.completeTask(taskId, output);
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Fail task (worker callback)
     */
    @PostMapping("/{taskId}/fail")
    public ResponseEntity<Void> failTask(
            @PathVariable UUID taskId,
            @RequestBody Map<String, Object> error
    ) {
        log.warn("Failing task: taskId={}, error={}", taskId, error.get("message"));
        
        taskService.failTask(taskId, (String) error.get("message"));
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * Heartbeat (worker keep-alive)
     */
    @PostMapping("/{taskId}/heartbeat")
    public ResponseEntity<Void> heartbeat(@PathVariable UUID taskId) {
        log.debug("Heartbeat: taskId={}", taskId);
        
        taskService.heartbeat(taskId);
        
        return ResponseEntity.ok().build();
    }
}
```

---

### 3. Service Layer

**File:** `backend/src/main/java/cz/muriel/core/workflow/service/ExternalTaskService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalTaskService {
    
    private final WorkflowExternalTaskRepository taskRepository;
    private final ExternalTaskCompletionWaiter completionWaiter;
    
    @Transactional
    public List<WorkflowExternalTask> pollTasks(String taskType, String workerId, int maxTasks, int waitSeconds) {
        // 1. Try to find pending tasks immediately
        List<WorkflowExternalTask> tasks = taskRepository.findPendingTasks(taskType, maxTasks);
        
        if (!tasks.isEmpty()) {
            // Lock tasks
            tasks.forEach(task -> {
                task.setStatus("LOCKED");
                task.setWorkerId(workerId);
                task.setLockedAt(Instant.now());
                task.setLastHeartbeatAt(Instant.now());
            });
            taskRepository.saveAll(tasks);
            return tasks;
        }
        
        // 2. Long-polling: wait for tasks
        if (waitSeconds > 0) {
            try {
                Thread.sleep(waitSeconds * 1000L);
                return pollTasks(taskType, workerId, maxTasks, 0); // Retry without wait
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return List.of();
            }
        }
        
        return List.of();
    }
    
    @Transactional
    public void completeTask(UUID taskId, Map<String, Object> output) {
        WorkflowExternalTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        
        if (!"LOCKED".equals(task.getStatus())) {
            throw new IllegalStateException("Task not locked: " + taskId);
        }
        
        task.setStatus("COMPLETED");
        task.setOutput(output);
        task.setCompletedAt(Instant.now());
        taskRepository.save(task);
        
        // Notify waiting executor
        completionWaiter.notifyCompletion(taskId, output);
    }
    
    @Transactional
    public void failTask(UUID taskId, String errorMessage) {
        WorkflowExternalTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        
        task.setRetryCount(task.getRetryCount() + 1);
        task.setErrorMessage(errorMessage);
        
        if (task.getRetryCount() >= task.getMaxRetries()) {
            task.setStatus("FAILED");
            task.setCompletedAt(Instant.now());
            completionWaiter.notifyFailure(taskId, new Exception(errorMessage));
        } else {
            // Retry
            task.setStatus("PENDING");
            task.setWorkerId(null);
            task.setLockedAt(null);
        }
        
        taskRepository.save(task);
    }
    
    @Transactional
    public void heartbeat(UUID taskId) {
        WorkflowExternalTask task = taskRepository.findById(taskId)
            .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        
        task.setLastHeartbeatAt(Instant.now());
        taskRepository.save(task);
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/workflow/service/ExternalTaskCompletionWaiter.java`

```java
@Component
public class ExternalTaskCompletionWaiter {
    
    private final Map<UUID, CompletableFuture<Map<String, Object>>> pendingTasks = new ConcurrentHashMap<>();
    
    public Map<String, Object> waitForCompletion(UUID taskId, Duration timeout) throws TimeoutException {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pendingTasks.put(taskId, future);
        
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pendingTasks.remove(taskId);
            throw e;
        } catch (Exception e) {
            pendingTasks.remove(taskId);
            throw new RuntimeException("Task wait failed: " + e.getMessage(), e);
        }
    }
    
    public void notifyCompletion(UUID taskId, Map<String, Object> output) {
        CompletableFuture<Map<String, Object>> future = pendingTasks.remove(taskId);
        if (future != null) {
            future.complete(output);
        }
    }
    
    public void notifyFailure(UUID taskId, Exception error) {
        CompletableFuture<Map<String, Object>> future = pendingTasks.remove(taskId);
        if (future != null) {
            future.completeExceptionally(error);
        }
    }
}
```

---

### 4. Scheduled Jobs

**File:** `backend/src/main/java/cz/muriel/core/workflow/job/ExternalTaskTimeoutJob.java`

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalTaskTimeoutJob {
    
    private final WorkflowExternalTaskRepository taskRepository;
    
    @Scheduled(fixedDelay = 60000) // Každou minutu
    public void checkTimeouts() {
        List<WorkflowExternalTask> timedOutTasks = taskRepository.findTimedOutTasks(Instant.now());
        
        timedOutTasks.forEach(task -> {
            log.warn("Task timeout detected: id={}, taskType={}, workerId={}", 
                task.getId(), task.getTaskType(), task.getWorkerId());
            
            task.setRetryCount(task.getRetryCount() + 1);
            
            if (task.getRetryCount() >= task.getMaxRetries()) {
                task.setStatus("FAILED");
                task.setErrorMessage("Timeout after " + task.getMaxRetries() + " retries");
                task.setCompletedAt(Instant.now());
            } else {
                // Unlock & retry
                task.setStatus("PENDING");
                task.setWorkerId(null);
                task.setLockedAt(null);
            }
        });
        
        taskRepository.saveAll(timedOutTasks);
        
        if (!timedOutTasks.isEmpty()) {
            log.info("Processed {} timed-out tasks", timedOutTasks.size());
        }
    }
}
```

---

## ✅ Acceptance Criteria

1. **Funkční:**
   - [ ] External task vytvoření (PENDING status)
   - [ ] Worker polling API (GET /poll)
   - [ ] Task locking (optimistic lock via version)
   - [ ] Task completion (POST /complete)
   - [ ] Task failure (POST /fail, retry logic)
   - [ ] Heartbeat (POST /heartbeat)
   - [ ] Timeout detection (scheduled job)

2. **Performance:**
   - [ ] Poll latency < 100ms
   - [ ] Lock acquisition atomic

3. **Reliability:**
   - [ ] At-least-once execution
   - [ ] No task loss

4. **Testy:**
   - [ ] Integration test: Poll → Lock → Complete
   - [ ] Test timeout scenario
   - [ ] Test retry logic
   - [ ] Test heartbeat keep-alive

---

**Related Stories:**
- W7: Workflow Executor Framework
- N8N6: BFF API (integration bridge)
- WF17: Workflow Instance Runtime
