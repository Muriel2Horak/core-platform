# T1: Report Scheduler

**Story:** [S4: Scheduled Reports](README.md)  
**Effort:** 12 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Cron-based report scheduling.

---

## 🏗️ IMPLEMENTATION

```java
@Service
public class ReportScheduler {
  @Scheduled(cron = "0 0 8 * * MON")
  public void sendWeeklyReport() {
    // Generate + email report
  }
}
```

---

## ✅ DELIVERABLES

- [ ] Scheduler service
- [ ] Cron expressions
- [ ] Email delivery

---

## ✅ Acceptance Criteria

- [ ] Deliverables listed above are completed and reviewed.
- [ ] Relevant code/tests/docs updated for this task.
- [ ] Outcome verified locally (or in CI where applicable).

**Estimated:** 12 hours