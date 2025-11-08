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

**Estimated:** 12 hours
