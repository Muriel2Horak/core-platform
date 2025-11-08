# T2: Workflow Actions

**Story:** [S9: Enhanced Tile Actions & Workflows](README.md)  
**Effort:** 30 hours  
**Priority:** P0  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Workflow state transitions (Draft → Review → Approved).

---

## 🎯 ACCEPTANCE CRITERIA

1. State machine definition
2. Transition buttons
3. Backend validation
4. Audit logging

---

## 🏗️ IMPLEMENTATION

```java
// backend/src/main/java/cz/muriel/core/workflow/WorkflowStateMachine.java
@Service
public class WorkflowStateMachine {
  private final Map<String, List<String>> transitions = Map.of(
    "DRAFT", List.of("REVIEW"),
    "REVIEW", List.of("APPROVED", "REJECTED"),
    "APPROVED", List.of("ARCHIVED"),
    "REJECTED", List.of("DRAFT")
  );
  
  public boolean canTransition(String from, String to) {
    return transitions.getOrDefault(from, List.of()).contains(to);
  }
  
  @Transactional
  public void transition(Workflow workflow, String toState) {
    if (!canTransition(workflow.getStatus(), toState)) {
      throw new IllegalStateException("Invalid transition");
    }
    
    workflow.setStatus(toState);
    workflowRepository.save(workflow);
    auditService.log("workflow.transition", workflow.getId(), toState);
  }
}
```

```typescript
// frontend/src/components/workflow/WorkflowActions.tsx
export const WorkflowActions: React.FC<{ workflow: Workflow }> = ({ workflow }) => {
  const transitions = useWorkflowTransitions(workflow.status);
  
  return (
    <ButtonGroup>
      {transitions.map(t => (
        <Button key={t} onClick={() => transitionWorkflow(workflow.id, t)}>
          {t}
        </Button>
      ))}
    </ButtonGroup>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] State machine
- [ ] Transition API
- [ ] UI buttons
- [ ] Audit log

---

**Estimated:** 30 hours
