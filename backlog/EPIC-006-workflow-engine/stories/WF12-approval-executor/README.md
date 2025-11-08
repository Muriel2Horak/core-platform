# WF12: APPROVAL Executor

> **Typ kroku:** Automatický / Čekající na schválení  
> **Estimate:** 800 LOC, 3 developer days  
> **Priority:** 🔴 HIGH  
> **Dependencies:** W5 (Runtime Foundation), W7 (Executor Interface)

---

## 🎯 Story Goal

Implementovat **APPROVAL executor** pro workflow kroky vyžadující schválení (single approver, unanimous, first-wins, quorum) s role-based access control, SLA tracking, escalations a email notifications.

---

## 📋 Requirements

### Approval Types

1. **SINGLE**: Jeden konkrétní uživatel musí schválit
2. **ALL_OF**: Všichni uživatelé v roli musí schválit (unanimous)
3. **ANY_OF**: První schválení stačí (first-wins)
4. **QUORUM**: Alespoň N z M schvalovatelů musí schválit (threshold)

### Configuration Schema

```json
{
  "id": "step-approval",
  "type": "APPROVAL",
  "config": {
    "approvalType": "ALL_OF",  // SINGLE, ALL_OF, ANY_OF, QUORUM
    "roles": ["ROLE_MANAGER", "ROLE_FINANCE"],
    "users": ["user-123"],  // Optional: concrete user IDs
    "quorumThreshold": 2,  // For QUORUM type: min required approvals
    "slaMinutes": 60,  // Timeout before escalation
    "escalateTo": ["ROLE_SENIOR_MANAGER"],  // On SLA breach
    "notifyChannels": ["email", "slack"],
    "autoApprove": false  // Skip if guard condition met
  },
  "onSuccess": "step-next",
  "onError": "step-rejected"
}
```

---

## 🏗️ Implementation

### Database Schema

```sql
-- Approval requests tracking
CREATE TABLE workflow_approval_requests (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workflow_instance_id UUID NOT NULL REFERENCES workflow_instances(id),
  step_id VARCHAR(100) NOT NULL,
  approval_type VARCHAR(50) NOT NULL,  -- SINGLE, ALL_OF, ANY_OF, QUORUM
  required_approvals INTEGER,  -- For QUORUM
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  due_at TIMESTAMP,  -- SLA deadline
  escalated BOOLEAN DEFAULT false,
  status VARCHAR(50) DEFAULT 'PENDING',  -- PENDING, APPROVED, REJECTED, ESCALATED
  
  INDEX idx_approval_status (status),
  INDEX idx_approval_due (due_at)
);

-- Individual approver responses
CREATE TABLE workflow_approval_responses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  approval_request_id UUID NOT NULL REFERENCES workflow_approval_requests(id),
  user_id VARCHAR(100) NOT NULL,
  response VARCHAR(50) NOT NULL,  -- APPROVE, REJECT
  comment TEXT,
  responded_at TIMESTAMP NOT NULL DEFAULT NOW(),
  
  UNIQUE (approval_request_id, user_id)
);
```

### Java Interface

```java
public interface ApprovalExecutor extends WorkflowExecutor {
  
  /**
   * Create approval request
   */
  ApprovalRequest createApprovalRequest(
    WorkflowInstance instance,
    WorkflowStep step,
    Map<String, Object> config
  );
  
  /**
   * Process approval/rejection
   */
  ExecutionResult processResponse(
    UUID approvalRequestId,
    String userId,
    ApprovalResponse response,
    String comment
  );
  
  /**
   * Check if approval complete (enough responses)
   */
  boolean isApprovalComplete(UUID approvalRequestId);
  
  /**
   * Check for SLA breach and escalate
   */
  void checkSlaAndEscalate(UUID approvalRequestId);
}
```

### Implementation Class

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ApprovalExecutorImpl implements ApprovalExecutor {
  
  private final JdbcTemplate jdbcTemplate;
  private final EmailService emailService;
  private final SlackService slackService;
  private final WorkflowMetricsService metricsService;
  
  @Override
  @Transactional
  public ApprovalRequest createApprovalRequest(
    WorkflowInstance instance,
    WorkflowStep step,
    Map<String, Object> config
  ) {
    String approvalType = (String) config.get("approvalType");
    List<String> roles = (List<String>) config.get("roles");
    List<String> users = (List<String>) config.getOrDefault("users", List.of());
    Integer slaMinutes = (Integer) config.get("slaMinutes");
    
    // Resolve approvers from roles
    List<String> approvers = resolveApprovers(roles, users);
    
    if (approvers.isEmpty()) {
      throw new IllegalStateException("No approvers found for roles: " + roles);
    }
    
    // Calculate quorum threshold
    Integer requiredApprovals = switch (approvalType) {
      case "SINGLE" -> 1;
      case "ALL_OF" -> approvers.size();
      case "ANY_OF" -> 1;
      case "QUORUM" -> (Integer) config.get("quorumThreshold");
      default -> throw new IllegalArgumentException("Unknown approval type: " + approvalType);
    };
    
    // Create request
    UUID requestId = UUID.randomUUID();
    Instant dueAt = slaMinutes != null 
      ? Instant.now().plus(Duration.ofMinutes(slaMinutes)) 
      : null;
    
    jdbcTemplate.update(
      "INSERT INTO workflow_approval_requests " +
      "(id, workflow_instance_id, step_id, approval_type, required_approvals, due_at) " +
      "VALUES (?, ?, ?, ?, ?, ?)",
      requestId, instance.getId(), step.getId(), approvalType, requiredApprovals, dueAt
    );
    
    // Send notifications
    sendNotifications(requestId, approvers, config);
    
    metricsService.recordApprovalCreated(instance.getEntityType(), step.getId());
    
    return ApprovalRequest.builder()
      .id(requestId)
      .approvalType(approvalType)
      .approvers(approvers)
      .requiredApprovals(requiredApprovals)
      .dueAt(dueAt)
      .build();
  }
  
  @Override
  @Transactional
  public ExecutionResult processResponse(
    UUID approvalRequestId,
    String userId,
    ApprovalResponse response,
    String comment
  ) {
    // Record response
    jdbcTemplate.update(
      "INSERT INTO workflow_approval_responses " +
      "(approval_request_id, user_id, response, comment) " +
      "VALUES (?, ?, ?, ?)",
      approvalRequestId, userId, response.name(), comment
    );
    
    log.info("Approval response recorded: requestId={}, user={}, response={}", 
             approvalRequestId, userId, response);
    
    // Check if approval complete
    if (isApprovalComplete(approvalRequestId)) {
      // Update request status
      jdbcTemplate.update(
        "UPDATE workflow_approval_requests SET status = ? WHERE id = ?",
        "APPROVED", approvalRequestId
      );
      
      metricsService.recordApprovalCompleted(approvalRequestId);
      
      return ExecutionResult.success("Approval complete");
    } else if (isApprovalRejected(approvalRequestId)) {
      jdbcTemplate.update(
        "UPDATE workflow_approval_requests SET status = ? WHERE id = ?",
        "REJECTED", approvalRequestId
      );
      
      metricsService.recordApprovalRejected(approvalRequestId);
      
      return ExecutionResult.failure("Approval rejected");
    } else {
      return ExecutionResult.pending("Waiting for more approvals");
    }
  }
  
  @Override
  public boolean isApprovalComplete(UUID approvalRequestId) {
    ApprovalRequest request = getApprovalRequest(approvalRequestId);
    
    long approveCount = jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM workflow_approval_responses " +
      "WHERE approval_request_id = ? AND response = 'APPROVE'",
      Long.class, approvalRequestId
    );
    
    return approveCount >= request.getRequiredApprovals();
  }
  
  private boolean isApprovalRejected(UUID approvalRequestId) {
    // ANY rejection → whole request rejected
    long rejectCount = jdbcTemplate.queryForObject(
      "SELECT COUNT(*) FROM workflow_approval_responses " +
      "WHERE approval_request_id = ? AND response = 'REJECT'",
      Long.class, approvalRequestId
    );
    
    return rejectCount > 0;
  }
  
  @Override
  @Scheduled(fixedRate = 60000)  // Check every minute
  public void checkSlaAndEscalate(UUID approvalRequestId) {
    List<UUID> overdueRequests = jdbcTemplate.query(
      "SELECT id FROM workflow_approval_requests " +
      "WHERE status = 'PENDING' AND due_at < NOW() AND escalated = false",
      (rs, rowNum) -> UUID.fromString(rs.getString("id"))
    );
    
    for (UUID requestId : overdueRequests) {
      escalateApproval(requestId);
    }
  }
  
  private void escalateApproval(UUID requestId) {
    // Load config
    Map<String, Object> config = getApprovalConfig(requestId);
    List<String> escalateTo = (List<String>) config.get("escalateTo");
    
    if (escalateTo == null || escalateTo.isEmpty()) {
      log.warn("No escalation target for approval: {}", requestId);
      return;
    }
    
    // Mark as escalated
    jdbcTemplate.update(
      "UPDATE workflow_approval_requests SET escalated = true WHERE id = ?",
      requestId
    );
    
    // Resolve escalation approvers
    List<String> escalationApprovers = resolveApprovers(escalateTo, List.of());
    
    // Send escalation notifications
    sendEscalationNotifications(requestId, escalationApprovers);
    
    metricsService.recordApprovalEscalated(requestId);
    
    log.info("Approval escalated: requestId={}, escalateTo={}", requestId, escalateTo);
  }
  
  private List<String> resolveApprovers(List<String> roles, List<String> users) {
    // Query Keycloak for users with given roles
    // OR return concrete user IDs if provided
    // TODO: Integrate with KeycloakService
    return users; // Placeholder
  }
  
  private void sendNotifications(UUID requestId, List<String> approvers, Map<String, Object> config) {
    List<String> channels = (List<String>) config.getOrDefault("notifyChannels", List.of("email"));
    
    for (String approver : approvers) {
      if (channels.contains("email")) {
        emailService.sendApprovalRequest(approver, requestId);
      }
      if (channels.contains("slack")) {
        slackService.sendApprovalRequest(approver, requestId);
      }
    }
  }
}
```

---

## 🎨 Frontend Components

### ApprovalWidget.tsx

```typescript
export function ApprovalWidget({ approvalRequestId }: { approvalRequestId: string }) {
  const [request, setRequest] = useState<ApprovalRequest | null>(null);
  const [comment, setComment] = useState('');
  
  const handleApprove = async () => {
    await fetch(`/api/workflows/approvals/${approvalRequestId}/approve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ comment }),
    });
    // Refresh
  };
  
  const handleReject = async () => {
    await fetch(`/api/workflows/approvals/${approvalRequestId}/reject`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ comment }),
    });
  };
  
  return (
    <Card>
      <CardHeader title="Approval Required" />
      <CardContent>
        <Typography>Workflow: {request?.workflowName}</Typography>
        <Typography>Step: {request?.stepLabel}</Typography>
        <Typography>Type: {request?.approvalType}</Typography>
        
        <TextField
          label="Comment"
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          multiline
          rows={3}
        />
      </CardContent>
      <CardActions>
        <Button onClick={handleApprove} color="success">Approve</Button>
        <Button onClick={handleReject} color="error">Reject</Button>
      </CardActions>
    </Card>
  );
}
```

---

## ✅ Acceptance Criteria

1. **Single Approver:**
   - ✅ Create approval request with `approvalType=SINGLE`, `users=["user-123"]`
   - ✅ User 123 can approve/reject
   - ✅ Workflow continues on approve, fails on reject

2. **Unanimous (ALL_OF):**
   - ✅ Create approval with `approvalType=ALL_OF`, `roles=["ROLE_MANAGER"]`
   - ✅ All managers must approve
   - ✅ Workflow continues only after all approve
   - ✅ Any rejection → workflow fails

3. **First-Wins (ANY_OF):**
   - ✅ Create approval with `approvalType=ANY_OF`, `roles=["ROLE_FINANCE", "ROLE_OPS"]`
   - ✅ First approval → workflow continues
   - ✅ First rejection → workflow fails

4. **Quorum:**
   - ✅ Create approval with `approvalType=QUORUM`, `roles=["ROLE_MANAGER"]`, `quorumThreshold=2`
   - ✅ At least 2 managers must approve
   - ✅ Workflow continues after threshold reached

5. **SLA & Escalation:**
   - ✅ Create approval with `slaMinutes=60`, `escalateTo=["ROLE_SENIOR_MANAGER"]`
   - ✅ After 60 minutes without approval → escalate to senior managers
   - ✅ Senior managers get notification
   - ✅ Metrics: `workflow_approval_escalations_total` incremented

6. **Notifications:**
   - ✅ Email sent to approvers on approval creation
   - ✅ Slack message sent if `notifyChannels=["slack"]`
   - ✅ Email/Slack on escalation

---

## 🧪 Testing

### Unit Tests

```java
@Test
void testSingleApprovalComplete() {
  ApprovalRequest request = executor.createApprovalRequest(
    instance,
    step,
    Map.of("approvalType", "SINGLE", "users", List.of("user-123"))
  );
  
  executor.processResponse(request.getId(), "user-123", ApprovalResponse.APPROVE, "LGTM");
  
  assertTrue(executor.isApprovalComplete(request.getId()));
}

@Test
void testAllOfApprovalRequiresAll() {
  ApprovalRequest request = executor.createApprovalRequest(
    instance,
    step,
    Map.of("approvalType", "ALL_OF", "users", List.of("user-1", "user-2"))
  );
  
  executor.processResponse(request.getId(), "user-1", ApprovalResponse.APPROVE, "OK");
  assertFalse(executor.isApprovalComplete(request.getId()));
  
  executor.processResponse(request.getId(), "user-2", ApprovalResponse.APPROVE, "OK");
  assertTrue(executor.isApprovalComplete(request.getId()));
}

@Test
void testQuorumThreshold() {
  ApprovalRequest request = executor.createApprovalRequest(
    instance,
    step,
    Map.of("approvalType", "QUORUM", "users", List.of("u1", "u2", "u3"), "quorumThreshold", 2)
  );
  
  executor.processResponse(request.getId(), "u1", ApprovalResponse.APPROVE, "");
  assertFalse(executor.isApprovalComplete(request.getId()));
  
  executor.processResponse(request.getId(), "u2", ApprovalResponse.APPROVE, "");
  assertTrue(executor.isApprovalComplete(request.getId()));  // Threshold reached
}
```

### Integration Test

```java
@Test
@DirtiesContext
void testApprovalWorkflowEndToEnd() {
  // 1. Start workflow with approval step
  WorkflowInstance instance = workflowService.startWorkflow("Order", "order-123", "tenant-1");
  
  // 2. Verify approval request created
  UUID approvalId = getApprovalRequestId(instance.getId());
  assertNotNull(approvalId);
  
  // 3. Manager approves
  approvalExecutor.processResponse(approvalId, "manager-1", ApprovalResponse.APPROVE, "Approved");
  
  // 4. Verify workflow continues to next step
  WorkflowInstance updated = workflowService.getInstance(instance.getId());
  assertEquals("step-next", updated.getCurrentStepId());
}
```

---

## 📊 Metrics

- `workflow_approval_requests_created_total` - Counter
- `workflow_approval_requests_approved_total` - Counter
- `workflow_approval_requests_rejected_total` - Counter
- `workflow_approval_requests_escalated_total` - Counter
- `workflow_approval_response_time_seconds` - Histogram (time to first response)
- `workflow_approval_pending` - Gauge (current pending approvals)

---

## 📌 Notes

- Approval responses jsou idempotent (upsert based on user_id)
- Escalation netriggeruje automaticky nové SLA - je to jen notifikace
- Auto-approve feature: pokud guard condition je true → skip approval
- Rejection handling: ANY rejection → celá request rejected

---

## 🔗 Related Stories

- **WF5:** Runtime Foundation (provides WorkflowExecutor interface)
- **WF7:** Node Executors (defines executor pattern)
- **WF17:** Workflow Instance Runtime (orchestrates execution)
