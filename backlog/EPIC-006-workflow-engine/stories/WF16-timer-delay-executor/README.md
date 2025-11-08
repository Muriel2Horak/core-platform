# WF16: TIMER/DELAY Executor - Workflow Delays & Scheduled Actions

**Typ:** TASK  
**Epic:** EPIC-006 (Workflow Engine - Internal Layer)  
**Fase:** Phase 2 (Typed Executors)  
**Priorita:** MEDIUM  
**Effort:** 400 LOC, 2 dny  
**Dependencies:** W7 (Executor Framework), W8 (Timers & SLA)  
**Status:** ⏳ TODO

---

## 🎯 Cíl

Implementovat **TIMER/DELAY executor** pro časové prodlevy a plánované akce v workflow. Executor:

- Čeká zadanou dobu před pokračováním workflow
- Scheduleuje akci na konkrétní čas
- Podporuje reminder notifications (email, Slack)
- Integruje s WorkflowTimerService (W8)

**Use Cases:**

```yaml
# Use Case 1: Simple delay
steps:
  - id: "wait-before-reminder"
    type: "TIMER"
    config:
      delay: "2h"  # Čekej 2 hodiny
      
# Use Case 2: Schedule konkrétní čas
steps:
  - id: "schedule-nightly-report"
    type: "TIMER"
    config:
      scheduleAt: "2025-11-09T02:00:00Z"  # ISO 8601 timestamp
      
# Use Case 3: Reminder s notification
steps:
  - id: "approval-reminder"
    type: "TIMER"
    config:
      delay: "24h"
      reminder:
        enabled: true
        channel: "EMAIL"  # EMAIL, SLACK, WEBHOOK
        recipients: ["manager@company.com"]
        message: "Approval pending for ticket ${workflow.context.ticketId}"
```

---

## 📋 Požadavky

### Funkční Požadavky

1. **Delay Support**
   - Relativní delay: `1h`, `30m`, `2d`
   - ISO 8601 duration: `PT1H30M`, `P2D`
   - Čeká před completion workflow step

2. **Scheduled Execution**
   - Absolutní timestamp: `2025-11-09T02:00:00Z`
   - Cron expression: `0 2 * * *` (každý den v 2:00)
   - Time zone support (UTC default)

3. **Reminder Notifications**
   - Email reminder (SMTP)
   - Slack webhook
   - Generic webhook (HTTP POST)
   - Template substitution (`${workflow.context.*}`)

4. **Integration s WorkflowTimerService**
   - Reuse existující workflow_timers table
   - Scheduled job checks timers
   - Trigger completion callback

---

## 🗄️ Database Schema

**Reuse existující schema z W8:**

```sql
-- Již existuje z W8 (Timers & SLA)
CREATE TABLE workflow_timers (
    id UUID PRIMARY KEY,
    workflow_instance_id UUID REFERENCES workflow_instances(id),
    step_id VARCHAR(100),
    timer_type VARCHAR(50), -- DELAY, SCHEDULED, REMINDER
    trigger_at TIMESTAMP NOT NULL,
    triggered BOOLEAN DEFAULT FALSE,
    triggered_at TIMESTAMP,
    reminder_config JSONB, -- {channel: "EMAIL", recipients: [...]}
    created_at TIMESTAMP DEFAULT NOW()
);
```

**Rozšíření pro reminders:**

```sql
-- Reminder execution log
CREATE TABLE workflow_timer_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timer_id UUID NOT NULL REFERENCES workflow_timers(id),
    channel VARCHAR(50) NOT NULL, -- EMAIL, SLACK, WEBHOOK
    recipients JSONB NOT NULL, -- ["email@company.com"] nebo ["slack_channel_id"]
    message TEXT NOT NULL,
    sent_at TIMESTAMP NOT NULL DEFAULT NOW(),
    status VARCHAR(50) NOT NULL, -- SENT, FAILED
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_timer_reminders_timer_id ON workflow_timer_reminders(timer_id);
```

---

## 🔧 Implementace

### 1. Java Executor

**File:** `backend/src/main/java/cz/muriel/core/workflow/executor/TimerExecutor.java`

```java
package cz.muriel.core.workflow.executor;

import cz.muriel.core.workflow.model.WorkflowExecution;
import cz.muriel.core.workflow.model.WorkflowStep;
import cz.muriel.core.workflow.model.WorkflowTimer;
import cz.muriel.core.workflow.service.WorkflowTimerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimerExecutor implements WorkflowExecutor {
    
    private final WorkflowTimerService timerService;
    private final ReminderService reminderService;
    private final TemplateEngine templateEngine;
    
    @Override
    public boolean supports(String executorType) {
        return "TIMER".equals(executorType) || "DELAY".equals(executorType);
    }
    
    @Override
    public Map<String, Object> execute(WorkflowExecution execution, WorkflowStep step) {
        log.info("Executing TIMER step: workflowInstanceId={}, stepId={}", 
            execution.getInstanceId(), step.getId());
        
        TimerConfig config = parseConfig(step.getConfig());
        
        // 1. Calculate trigger time
        Instant triggerAt;
        if (config.getDelay() != null) {
            // Relative delay
            Duration delay = parseDuration(config.getDelay());
            triggerAt = Instant.now().plus(delay);
        } else if (config.getScheduleAt() != null) {
            // Absolute timestamp
            triggerAt = ZonedDateTime.parse(config.getScheduleAt()).toInstant();
        } else {
            throw new IllegalArgumentException("Either 'delay' or 'scheduleAt' must be specified");
        }
        
        // 2. Create timer record
        WorkflowTimer timer = new WorkflowTimer();
        timer.setWorkflowInstanceId(execution.getInstanceId());
        timer.setStepId(step.getId());
        timer.setTimerType(config.getDelay() != null ? "DELAY" : "SCHEDULED");
        timer.setTriggerAt(triggerAt);
        timer.setTriggered(false);
        
        if (config.getReminder() != null && config.getReminder().isEnabled()) {
            timer.setReminderConfig(config.getReminder());
        }
        
        timer = timerService.createTimer(timer);
        
        log.info("Timer created: id={}, triggerAt={}, type={}", 
            timer.getId(), triggerAt, timer.getTimerType());
        
        // 3. Wait for timer trigger (blocking)
        long waitMillis = Duration.between(Instant.now(), triggerAt).toMillis();
        
        if (waitMillis > 0) {
            try {
                log.info("Waiting {}ms for timer trigger", waitMillis);
                TimeUnit.MILLISECONDS.sleep(waitMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WorkflowExecutionException("Timer wait interrupted", e);
            }
        }
        
        // 4. Send reminder if configured
        if (config.getReminder() != null && config.getReminder().isEnabled()) {
            sendReminder(config.getReminder(), execution, timer);
        }
        
        // 5. Mark timer as triggered
        timerService.markTriggered(timer.getId());
        
        log.info("Timer triggered: id={}, actualDelay={}ms", 
            timer.getId(), Duration.between(timer.getCreatedAt(), Instant.now()).toMillis());
        
        return Map.of("triggeredAt", Instant.now().toString());
    }
    
    private Duration parseDuration(String delay) {
        // Support formats: "1h", "30m", "2d", "PT1H30M"
        if (delay.startsWith("PT") || delay.startsWith("P")) {
            return Duration.parse(delay);
        }
        
        // Simple format: "1h", "30m", "2d"
        String unit = delay.substring(delay.length() - 1);
        long value = Long.parseLong(delay.substring(0, delay.length() - 1));
        
        return switch (unit) {
            case "s" -> Duration.ofSeconds(value);
            case "m" -> Duration.ofMinutes(value);
            case "h" -> Duration.ofHours(value);
            case "d" -> Duration.ofDays(value);
            default -> throw new IllegalArgumentException("Invalid delay format: " + delay);
        };
    }
    
    private void sendReminder(ReminderConfig reminder, WorkflowExecution execution, WorkflowTimer timer) {
        String message = templateEngine.resolve(reminder.getMessage(), execution.getContext());
        
        switch (reminder.getChannel()) {
            case "EMAIL" -> reminderService.sendEmail(
                reminder.getRecipients(), 
                "Workflow Timer Reminder", 
                message
            );
            case "SLACK" -> reminderService.sendSlack(
                reminder.getRecipients(), 
                message
            );
            case "WEBHOOK" -> reminderService.sendWebhook(
                reminder.getRecipients().get(0), 
                Map.of("message", message, "timerId", timer.getId())
            );
            default -> log.warn("Unknown reminder channel: {}", reminder.getChannel());
        }
        
        log.info("Reminder sent: channel={}, recipients={}", 
            reminder.getChannel(), reminder.getRecipients());
    }
    
    @Override
    public void compensate(WorkflowExecution execution, WorkflowStep step) {
        log.info("Compensating TIMER step: stepId={}", step.getId());
        // Cancel timer if still pending
        timerService.cancelTimer(execution.getInstanceId(), step.getId());
    }
}
```

---

### 2. Config Model

**File:** `backend/src/main/java/cz/muriel/core/workflow/executor/TimerConfig.java`

```java
@Data
public class TimerConfig {
    private String delay; // "1h", "30m", "PT1H30M"
    private String scheduleAt; // ISO 8601: "2025-11-09T02:00:00Z"
    private ReminderConfig reminder;
}

@Data
class ReminderConfig {
    private boolean enabled = false;
    private String channel; // EMAIL, SLACK, WEBHOOK
    private List<String> recipients;
    private String message; // Template: "Approval pending for ${workflow.context.ticketId}"
}
```

---

### 3. Reminder Service

**File:** `backend/src/main/java/cz/muriel/core/workflow/service/ReminderService.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {
    
    private final JavaMailSender mailSender;
    private final WebClient.Builder webClientBuilder;
    private final WorkflowTimerReminderRepository reminderRepository;
    
    public void sendEmail(List<String> recipients, String subject, String message) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setTo(recipients.toArray(new String[0]));
            mail.setSubject(subject);
            mail.setText(message);
            mail.setFrom("workflow@core-platform.local");
            
            mailSender.send(mail);
            
            log.info("Email reminder sent: recipients={}", recipients);
            
        } catch (Exception e) {
            log.error("Failed to send email reminder: {}", e.getMessage(), e);
        }
    }
    
    public void sendSlack(List<String> channels, String message) {
        channels.forEach(webhookUrl -> {
            try {
                webClientBuilder.build()
                    .post()
                    .uri(webhookUrl)
                    .bodyValue(Map.of("text", message))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
                
                log.info("Slack reminder sent: channel={}", webhookUrl);
                
            } catch (Exception e) {
                log.error("Failed to send Slack reminder: {}", e.getMessage(), e);
            }
        });
    }
    
    public void sendWebhook(String webhookUrl, Map<String, Object> payload) {
        try {
            webClientBuilder.build()
                .post()
                .uri(webhookUrl)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
            
            log.info("Webhook reminder sent: url={}", webhookUrl);
            
        } catch (Exception e) {
            log.error("Failed to send webhook reminder: {}", e.getMessage(), e);
        }
    }
}
```

---

### 4. Tests

**File:** `backend/src/test/java/cz/muriel/core/workflow/executor/TimerExecutorTest.java`

```java
@SpringBootTest
class TimerExecutorTest {
    
    @Autowired
    private TimerExecutor executor;
    
    @Autowired
    private WorkflowTimerService timerService;
    
    @Test
    void shouldDelayExecution() {
        // Given
        WorkflowStep step = WorkflowStep.builder()
            .id("wait-step")
            .type("TIMER")
            .config(Map.of("delay", "2s"))
            .build();
        
        Instant startedAt = Instant.now();
        
        // When
        executor.execute(mockExecution(), step);
        
        Instant completedAt = Instant.now();
        
        // Then
        Duration actualDelay = Duration.between(startedAt, completedAt);
        assertThat(actualDelay.getSeconds()).isGreaterThanOrEqualTo(2);
    }
    
    @Test
    void shouldScheduleAtSpecificTime() {
        // Given
        Instant futureTime = Instant.now().plus(Duration.ofSeconds(3));
        
        WorkflowStep step = WorkflowStep.builder()
            .config(Map.of("scheduleAt", futureTime.toString()))
            .build();
        
        // When
        Map<String, Object> output = executor.execute(mockExecution(), step);
        
        // Then
        assertThat(output).containsKey("triggeredAt");
        Instant triggeredAt = Instant.parse((String) output.get("triggeredAt"));
        assertThat(triggeredAt).isAfterOrEqualTo(futureTime);
    }
    
    @Test
    void shouldParseDurationFormats() {
        assertThat(executor.parseDuration("1h")).isEqualTo(Duration.ofHours(1));
        assertThat(executor.parseDuration("30m")).isEqualTo(Duration.ofMinutes(30));
        assertThat(executor.parseDuration("2d")).isEqualTo(Duration.ofDays(2));
        assertThat(executor.parseDuration("PT1H30M")).isEqualTo(Duration.ofMinutes(90));
    }
}
```

---

## ✅ Acceptance Criteria

1. **Funkční:**
   - [ ] Delay support (`1h`, `30m`, `PT1H30M`)
   - [ ] Scheduled execution (ISO 8601 timestamp)
   - [ ] Email reminder
   - [ ] Slack reminder
   - [ ] Integration s WorkflowTimerService

2. **Performance:**
   - [ ] Timer precision ±1s

3. **Testy:**
   - [ ] Test delay execution
   - [ ] Test scheduled execution
   - [ ] Test duration parsing

---

**Related Stories:**
- W8: Timers & SLA
- WF12: APPROVAL Executor (reminder use case)
