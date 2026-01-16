---
id: WF17
epic: EPIC-006-workflow-engine
title: "Workflow Instance Runtime - Step-by-Step Orchestration"
priority: P2
status: done
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "48 hours"
path_mapping:
  code_paths:
    - backend/src/main/java/cz/muriel/core/workflow
  test_paths:
    - backend/src/test/java/cz/muriel/core/workflow
  docs_paths:
    - backlog/EPIC-006-workflow-engine/stories/WF17-workflow-instance-runtime/README.md
    - backlog/EPIC-006-workflow-engine/README.md
---

# WF17: Workflow Instance Runtime - Step-by-Step Orchestration

**Typ:** TASK  
**Epic:** EPIC-006 (Workflow Engine - Internal Layer)  
**Fase:** Phase 2 (Typed Executors)  
**Priorita:** 🔴 CRITICAL (foundation pro step execution)  
**Effort:** 1,500 LOC, 6 dní  
**Dependencies:** W7 (Executor Framework), WF12-WF16 (Typed Executors)  
**Status:** ✅ **DONE**

---

## 🎯 Cíl

Implementovat **Workflow Instance Runtime** - orchestrátor step-by-step execution s:
- Vytváření workflow instances z entity state transitions
- Step execution loop (sequential, parallel, conditional)
- Context management (pass data mezi steps)
- Error handling & retry per step
- Compensation flow (rollback při failure)
- Persistence (workflow_instances, workflow_step_executions)

**Architecture:**

```
WorkflowInstanceRuntime
  ├─ createInstance(entityType, entityId, workflowVersionId)
  ├─ executeNextStep(instanceId)
  ├─ handleStepSuccess(instanceId, stepId, output)
  ├─ handleStepFailure(instanceId, stepId, error)
  └─ compensate(instanceId)

WorkflowStepOrchestrator
  ├─ executeStep(instance, step, context)
  ├─ resolveExecutor(stepType)
  ├─ applyRetryPolicy(step, attemptNumber)
  └─ updateStepExecution(stepExecutionId, status, output)
```

---

## ✅ Acceptance Criteria

1. **Instance lifecycle**
   - Instance je vytvorena z workflow verze a ulozena do DB.
   - Statusy RUNNING/COMPLETED/FAILED jsou konzistentni.

2. **Step orchestrace**
   - Step executory jsou volane sekvencne podle definice.
   - Output stepu se merge do contextu.

3. **Error handling**
   - Retry policy per step funguje.
   - Po vycerpani retry se spusti compensation (pokud definovano).

4. **Parallel + conditional**
   - Parallel steps maji join point.
   - Condition skipuje steps dle expression.

5. **Observability**
   - Každé spusteni stepu je logovano a metricky pokryto.

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Instance lifecycle + persistence model | 14h | W5 |
| 2 | Step loop + context merge + template resolver | 14h | T1 |
| 3 | Retry/error/compensation engine | 10h | T2 |
| 4 | Parallel/conditional execution | 6h | T2 |
| 5 | Testy + docs | 4h | T2, T3, T4 |

---

## 📋 Požadavky

### Funkční Požadavky

1. **Instance Lifecycle**
   - Create instance from workflow version
   - Initialize context (entity data, variables)
   - Execute steps sequentially (nebo parallel pokud defined)
   - Track current step
   - Complete workflow (SUCCESS, FAILED, CANCELLED)

2. **Step Execution Loop**
   ```
   WHILE (hasNextStep) {
     step = getNextStep()
     executor = resolveExecutor(step.type)
     try {
       output = executor.execute(instance, step, context)
       context.merge(output)
       markStepSuccess(step)
       moveToNextStep()
     } catch (error) {
       if (shouldRetry(step)) {
         retry(step)
       } else {
         markStepFailed(step)
         if (compensationEnabled) {
           runCompensation()
         }
         markWorkflowFailed()
         break
       }
     }
   }
   markWorkflowCompleted()
   ```

3. **Context Management**
   - Initial context: `{entity: {...}, workflow: {instanceId, version}}`
   - Step output → context merge: `context.previousStepOutput = output`
   - Template resolution: `${context.entity.name}`, `${context.previousStepOutput.ticketId}`

4. **Error Handling**
   - Per-step retry policy (maxAttempts, backoff)
   - Circuit breaker per executor type
   - Error classification (retriable vs. fatal)
   - Compensation flow (rollback executed steps)

5. **Conditional Execution**
   - Skip step if condition false: `condition: "${context.entity.priority} == 'HIGH'"`
   - Branching: `if-then-else` logic
   - Loops: `forEach` over array

6. **Parallel Execution**
   - Execute multiple steps concurrently
   - Join point (wait for all to complete)
   - Partial failure handling

---

## 🗄️ Database Schema

```sql
-- Workflow instances (runtime execution)
CREATE TABLE workflow_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Entity association
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    
    -- Workflow definition
    workflow_version_id BIGINT NOT NULL REFERENCES workflow_versions(id),
    workflow_name VARCHAR(255),
    
    -- Execution state
    status VARCHAR(50) NOT NULL, -- RUNNING, COMPLETED, FAILED, CANCELLED
    current_step_id VARCHAR(100),
    
    -- Context (runtime variables)
    context JSONB NOT NULL DEFAULT '{}'::jsonb,
    
    -- Timing
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP,
    
    -- Error tracking
    error_message TEXT,
    error_step_id VARCHAR(100),
    
    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    -- Optimistic locking
    version BIGINT DEFAULT 0
);

CREATE INDEX idx_workflow_instances_entity ON workflow_instances(entity_type, entity_id);
CREATE INDEX idx_workflow_instances_status ON workflow_instances(status);
CREATE INDEX idx_workflow_instances_version ON workflow_instances(workflow_version_id);

-- Step executions (audit trail)
CREATE TABLE workflow_step_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_instance_id UUID NOT NULL REFERENCES workflow_instances(id) ON DELETE CASCADE,
    
    -- Step definition
    step_id VARCHAR(100) NOT NULL,
    step_type VARCHAR(50) NOT NULL, -- APPROVAL, REST_SYNC, KAFKA_COMMAND, etc.
    step_config JSONB,
    
    -- Execution state
    status VARCHAR(50) NOT NULL, -- PENDING, RUNNING, SUCCESS, FAILED, SKIPPED
    
    -- Input/Output
    input JSONB,
    output JSONB,
    
    -- Error tracking
    error TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    
    -- Timing
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    duration_ms INTEGER,
    
    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_step_executions_instance ON workflow_step_executions(workflow_instance_id);
CREATE INDEX idx_step_executions_step_id ON workflow_step_executions(step_id);
CREATE INDEX idx_step_executions_status ON workflow_step_executions(status);
```

---

## 🔧 Implementace

### 1. Runtime Orchestrator

**File:** `backend/src/main/java/cz/muriel/core/workflow/runtime/WorkflowInstanceRuntime.java`

```java
package cz.muriel.core.workflow.runtime;

import cz.muriel.core.workflow.model.*;
import cz.muriel.core.workflow.executor.WorkflowExecutor;
import cz.muriel.core.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowInstanceRuntime {
    
    private final WorkflowInstanceRepository instanceRepository;
    private final WorkflowStepExecutionRepository stepExecutionRepository;
    private final WorkflowVersionRepository versionRepository;
    private final WorkflowStepOrchestrator stepOrchestrator;
    private final ContextManager contextManager;
    
    /**
     * Create workflow instance and start execution
     */
    @Transactional
    public WorkflowInstance createAndStart(String entityType, String entityId, Long workflowVersionId) {
        log.info("Creating workflow instance: entityType={}, entityId={}, versionId={}", 
            entityType, entityId, workflowVersionId);
        
        // 1. Load workflow version
        WorkflowVersion version = versionRepository.findById(workflowVersionId)
            .orElseThrow(() -> new IllegalArgumentException("Workflow version not found: " + workflowVersionId));
        
        // 2. Initialize context
        Map<String, Object> initialContext = contextManager.initializeContext(entityType, entityId, version);
        
        // 3. Create instance
        WorkflowInstance instance = new WorkflowInstance();
        instance.setEntityType(entityType);
        instance.setEntityId(entityId);
        instance.setWorkflowVersionId(workflowVersionId);
        instance.setWorkflowName(version.getName());
        instance.setStatus("RUNNING");
        instance.setContext(initialContext);
        instance.setStartedAt(Instant.now());
        
        // Parse workflow definition
        WorkflowDefinition definition = version.getSchemaDefinition(); // From JSONB
        List<WorkflowStep> steps = definition.getSteps();
        
        if (!steps.isEmpty()) {
            instance.setCurrentStepId(steps.get(0).getId());
        }
        
        instance = instanceRepository.save(instance);
        
        log.info("Workflow instance created: id={}, steps={}", instance.getId(), steps.size());
        
        // 4. Start execution (async or sync)
        executeAsync(instance);
        
        return instance;
    }
    
    /**
     * Execute workflow instance to completion
     */
    @Transactional
    public void execute(UUID instanceId) {
        WorkflowInstance instance = instanceRepository.findById(instanceId)
            .orElseThrow(() -> new IllegalArgumentException("Instance not found: " + instanceId));
        
        if (!"RUNNING".equals(instance.getStatus())) {
            log.warn("Instance not in RUNNING state: id={}, status={}", instanceId, instance.getStatus());
            return;
        }
        
        WorkflowVersion version = versionRepository.findById(instance.getWorkflowVersionId())
            .orElseThrow();
        WorkflowDefinition definition = version.getSchemaDefinition();
        
        // Execute steps sequentially
        while (instance.getCurrentStepId() != null) {
            WorkflowStep currentStep = findStep(definition, instance.getCurrentStepId());
            
            if (currentStep == null) {
                log.error("Step not found: stepId={}", instance.getCurrentStepId());
                failWorkflow(instance, "Step not found: " + instance.getCurrentStepId());
                break;
            }
            
            try {
                // Execute step
                Map<String, Object> stepOutput = stepOrchestrator.executeStep(instance, currentStep);
                
                // Update context
                instance.getContext().put("lastStepOutput", stepOutput);
                instance.getContext().put(currentStep.getId() + "_output", stepOutput);
                
                // Move to next step
                WorkflowStep nextStep = findNextStep(definition, currentStep);
                instance.setCurrentStepId(nextStep != null ? nextStep.getId() : null);
                instanceRepository.save(instance);
                
            } catch (Exception e) {
                log.error("Step execution failed: instanceId={}, stepId={}, error={}", 
                    instanceId, currentStep.getId(), e.getMessage(), e);
                
                // Handle failure
                if (shouldCompensate(instance, currentStep)) {
                    compensate(instance);
                }
                
                failWorkflow(instance, e.getMessage());
                break;
            }
        }
        
        // All steps completed
        if (instance.getCurrentStepId() == null && "RUNNING".equals(instance.getStatus())) {
            completeWorkflow(instance);
        }
    }
    
    private void completeWorkflow(WorkflowInstance instance) {
        instance.setStatus("COMPLETED");
        instance.setCompletedAt(Instant.now());
        instanceRepository.save(instance);
        
        log.info("Workflow completed: id={}, duration={}s", 
            instance.getId(), 
            Duration.between(instance.getStartedAt(), instance.getCompletedAt()).getSeconds()
        );
    }
    
    private void failWorkflow(WorkflowInstance instance, String errorMessage) {
        instance.setStatus("FAILED");
        instance.setErrorMessage(errorMessage);
        instance.setCompletedAt(Instant.now());
        instanceRepository.save(instance);
        
        log.error("Workflow failed: id={}, error={}", instance.getId(), errorMessage);
    }
    
    private void compensate(WorkflowInstance instance) {
        log.info("Starting compensation flow: instanceId={}", instance.getId());
        
        // Get all completed steps in reverse order
        List<WorkflowStepExecution> completedSteps = stepExecutionRepository
            .findByWorkflowInstanceIdAndStatus(instance.getId(), "SUCCESS");
        
        Collections.reverse(completedSteps);
        
        completedSteps.forEach(stepExecution -> {
            try {
                stepOrchestrator.compensateStep(instance, stepExecution);
            } catch (Exception e) {
                log.error("Compensation failed: stepId={}, error={}", 
                    stepExecution.getStepId(), e.getMessage(), e);
            }
        });
    }
    
    private WorkflowStep findStep(WorkflowDefinition definition, String stepId) {
        return definition.getSteps().stream()
            .filter(s -> s.getId().equals(stepId))
            .findFirst()
            .orElse(null);
    }
    
    private WorkflowStep findNextStep(WorkflowDefinition definition, WorkflowStep currentStep) {
        List<WorkflowStep> steps = definition.getSteps();
        int currentIndex = steps.indexOf(currentStep);
        
        if (currentIndex >= 0 && currentIndex < steps.size() - 1) {
            return steps.get(currentIndex + 1);
        }
        
        return null;
    }
    
    private boolean shouldCompensate(WorkflowInstance instance, WorkflowStep step) {
        // TODO: Check step config for compensation strategy
        return true;
    }
    
    private void executeAsync(WorkflowInstance instance) {
        CompletableFuture.runAsync(() -> execute(instance.getId()));
    }
}
```

---

### 2. Step Orchestrator

**File:** `backend/src/main/java/cz/muriel/core/workflow/runtime/WorkflowStepOrchestrator.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowStepOrchestrator {
    
    private final WorkflowExecutorRegistry executorRegistry;
    private final WorkflowStepExecutionRepository stepExecutionRepository;
    private final TemplateEngine templateEngine;
    
    @Transactional
    public Map<String, Object> executeStep(WorkflowInstance instance, WorkflowStep step) {
        log.info("Executing step: instanceId={}, stepId={}, type={}", 
            instance.getId(), step.getId(), step.getType());
        
        // 1. Create step execution record
        WorkflowStepExecution execution = new WorkflowStepExecution();
        execution.setWorkflowInstanceId(instance.getId());
        execution.setStepId(step.getId());
        execution.setStepType(step.getType());
        execution.setStepConfig(step.getConfig());
        execution.setStatus("RUNNING");
        execution.setStartedAt(Instant.now());
        
        // Resolve input from context
        Map<String, Object> input = resolveInput(step, instance.getContext());
        execution.setInput(input);
        
        execution = stepExecutionRepository.save(execution);
        
        // 2. Get executor
        WorkflowExecutor executor = executorRegistry.getExecutor(step.getType())
            .orElseThrow(() -> new IllegalStateException("No executor for type: " + step.getType()));
        
        // 3. Execute with retry
        Map<String, Object> output;
        try {
            output = executeWithRetry(executor, instance, step, execution);
            
            // Mark success
            execution.setStatus("SUCCESS");
            execution.setOutput(output);
            execution.setCompletedAt(Instant.now());
            execution.setDurationMs(
                (int) Duration.between(execution.getStartedAt(), execution.getCompletedAt()).toMillis()
            );
            stepExecutionRepository.save(execution);
            
            log.info("Step completed: stepId={}, duration={}ms", 
                step.getId(), execution.getDurationMs());
            
            return output;
            
        } catch (Exception e) {
            execution.setStatus("FAILED");
            execution.setError(e.getMessage());
            execution.setCompletedAt(Instant.now());
            stepExecutionRepository.save(execution);
            
            throw new WorkflowExecutionException("Step failed: " + step.getId(), e);
        }
    }
    
    private Map<String, Object> executeWithRetry(
            WorkflowExecutor executor, 
            WorkflowInstance instance, 
            WorkflowStep step,
            WorkflowStepExecution execution
    ) {
        int maxRetries = step.getRetry() != null ? step.getRetry().getMaxAttempts() : 3;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                execution.setRetryCount(attempt - 1);
                stepExecutionRepository.save(execution);
                
                return executor.execute(
                    new WorkflowExecution(instance.getId(), instance.getContext()), 
                    step
                );
                
            } catch (Exception e) {
                log.warn("Step execution attempt {} failed: {}", attempt, e.getMessage());
                
                if (attempt == maxRetries) {
                    throw e;
                }
                
                // Exponential backoff
                long delay = (long) (1000 * Math.pow(2, attempt - 1));
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            }
        }
        
        throw new IllegalStateException("Retry logic failed");
    }
    
    public void compensateStep(WorkflowInstance instance, WorkflowStepExecution stepExecution) {
        log.info("Compensating step: instanceId={}, stepId={}", 
            instance.getId(), stepExecution.getStepId());
        
        WorkflowExecutor executor = executorRegistry.getExecutor(stepExecution.getStepType())
            .orElseThrow();
        
        WorkflowStep step = new WorkflowStep();
        step.setId(stepExecution.getStepId());
        step.setType(stepExecution.getStepType());
        step.setConfig(stepExecution.getStepConfig());
        
        executor.compensate(
            new WorkflowExecution(instance.getId(), instance.getContext()), 
            step
        );
    }
    
    private Map<String, Object> resolveInput(WorkflowStep step, Map<String, Object> context) {
        // TODO: Template resolution for step input
        return step.getInput() != null ? step.getInput() : Map.of();
    }
}
```

---

### 3. Context Manager

**File:** `backend/src/main/java/cz/muriel/core/workflow/runtime/ContextManager.java`

```java
@Component
@RequiredArgsConstructor
public class ContextManager {
    
    private final EntityRepository entityRepository;
    
    public Map<String, Object> initializeContext(String entityType, String entityId, WorkflowVersion version) {
        Map<String, Object> context = new HashMap<>();
        
        // Load entity data
        Object entity = loadEntity(entityType, entityId);
        context.put("entity", entity);
        
        // Workflow metadata
        context.put("workflow", Map.of(
            "name", version.getName(),
            "version", version.getVersion()
        ));
        
        return context;
    }
    
    private Object loadEntity(String entityType, String entityId) {
        // TODO: Generic entity loader
        return Map.of("type", entityType, "id", entityId);
    }
}
```

---

## ✅ Acceptance Criteria

1. **Funkční:**
   - [ ] Create workflow instance from version
   - [ ] Execute steps sequentially
   - [ ] Context management (pass data between steps)
   - [ ] Error handling per step
   - [ ] Retry logic (exponential backoff)
   - [ ] Compensation flow (rollback)

2. **Performance:**
   - [ ] Instance creation < 100ms
   - [ ] Step transition < 50ms

3. **Testy:**
   - [ ] Integration test: 3-step workflow end-to-end
   - [ ] Test retry logic
   - [ ] Test compensation flow
   - [ ] Test context passing

---

**Related Stories:**
- W7: Executor Framework
- WF12-WF16: Typed Executors
- WF18: Workflow Steps Schema
