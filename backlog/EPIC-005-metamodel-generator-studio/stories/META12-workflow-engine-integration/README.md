---
id: META-012
epic: EPIC-005-metamodel-generator-studio
title: "Workflow Engine Integration"
priority: P1
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "160 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-005-metamodel-generator-studio/stories/META12-workflow-engine-integration/README.md
    - backlog/EPIC-005-metamodel-generator-studio/README.md
---

# META-012: Workflow Engine Integration

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🔴 **CRITICAL**  
**Priorita:** P1 (High Priority)  
**Estimated LOC:** ~2,000 řádků  
**Effort:** 4 týdny (160 hodin)

---

## 📋 Story Description

Jako **platform developer**, chci **integrovat workflow engine s metamodelem**, abych **mohl definovat stavy, přechody a workflow logiku deklarativně v YAML**.

---

## 🎯 Business Value

**HIGH-LEVEL požadavek:**
> 5️⃣ Workflow integrace: Metamodel a workflow jsou svázané - entita může mít definovaný `workflowDefinition` (stavy, přechody, guardy, akce). Workflow krok (REST, Kafka, Timer, Approval, External Task) je definovaný deklarativně (type, inputMap, onSuccess, onError, retry, timeout). FE z toho umí ukázat „kde jsem", „co proběhlo", „co může následovat".

---

## 🎯 Acceptance Criteria

### AC1: Workflow Definition v Metamodelu
```yaml
entity: Order
workflow:
  states:
    - name: DRAFT
      initial: true
    - name: PENDING_APPROVAL
    - name: APPROVED
    - name: REJECTED
      terminal: true
  
  transitions:
    - from: DRAFT
      to: PENDING_APPROVAL
      action: submit_for_approval
      guards: ["hasAllRequiredFields"]
    
    - from: PENDING_APPROVAL
      to: APPROVED
      action: approve
      guards: ["isApprover"]
      steps:
        - type: REST
          url: "/api/notifications/send"
          method: POST
        - type: Kafka
          topic: "order.approved"
```

### AC2: State Machine Runtime
- **GIVEN** Order v stavu `DRAFT`
- **WHEN** volám `POST /api/orders/123/transition?action=submit_for_approval`
- **THEN**:
  - Guard `hasAllRequiredFields` se vyhodnotí
  - Pokud pass → state = `PENDING_APPROVAL`
  - Workflow steps se spustí (REST call, Kafka event)
  - Response: `{ "newState": "PENDING_APPROVAL", "success": true }`

### AC3: Guards Evaluation
```yaml
guards:
  - name: hasAllRequiredFields
    expression: "entity.customerName != null && entity.totalAmount > 0"
  
  - name: isApprover
    expression: "user.roles.contains('APPROVER')"
```

### AC4: Workflow Steps Execution
- **REST Step**: HTTP call to external API
- **Kafka Step**: Publish event to topic
- **Timer Step**: Delay execution (e.g., 24h)
- **Approval Step**: Wait for human approval
- **External Task**: Call external service (n8n, Camunda)

### AC5: UI Workflow Components
- **State Badge**: `<Chip color="blue">PENDING_APPROVAL</Chip>`
- **Transition Buttons**: `<Button onClick={approve}>Approve</Button>`
- **Workflow History**: Timeline view (Draft → Pending → Approved)

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Rozsirit metamodel workflow schema + parser | 20h | META-001 |
| 2 | Runtime state machine + guard evaluator + step executors | 60h | T1 |
| 3 | API pro transitions + audit/history | 35h | T2 |
| 4 | UI workflow komponenty (badge, actions, timeline) | 25h | T3, EPIC-014 |
| 5 | Testy (happy path + guard fail + retry) | 20h | T2, T3 |

---

## 🏗️ Implementation

```java
@Service
public class WorkflowEngine {
    
    public void executeTransition(Object entity, String action, EntitySchema schema) {
        WorkflowDefinition workflow = schema.getWorkflow();
        String currentState = (String) getFieldValue(entity, "state");
        
        // Find transition
        TransitionConfig transition = workflow.getTransitions().stream()
            .filter(t -> t.getFrom().equals(currentState) && t.getAction().equals(action))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No transition for action: " + action));
        
        // Evaluate guards
        for (String guardName : transition.getGuards()) {
            if (!evaluateGuard(guardName, entity, workflow)) {
                throw new WorkflowException("Guard failed: " + guardName);
            }
        }
        
        // Execute workflow steps
        for (WorkflowStep step : transition.getSteps()) {
            executeStep(step, entity);
        }
        
        // Update state
        setFieldValue(entity, "state", transition.getTo());
        repository.save(entity);
        
        // Publish state change event (META-011 Streaming)
        eventPublisher.publish(new StateChangedEvent(entity, currentState, transition.getTo()));
    }
    
    private void executeStep(WorkflowStep step, Object entity) {
        switch (step.getType()) {
            case REST:
                restTemplate.exchange(step.getUrl(), step.getMethod(), buildRequest(step, entity), String.class);
                break;
            case KAFKA:
                kafkaTemplate.send(step.getTopic(), buildPayload(step, entity));
                break;
            case TIMER:
                scheduler.schedule(() -> continueWorkflow(entity), step.getDelay());
                break;
            case APPROVAL:
                approvalService.requestApproval(entity, step.getApprover());
                break;
        }
    }
}
```

---

**Story Owner:** Backend Team  
**Priority:** P1  
**Effort:** 4 týdny
