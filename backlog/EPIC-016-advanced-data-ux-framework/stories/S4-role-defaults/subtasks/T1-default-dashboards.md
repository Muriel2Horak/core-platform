# T1: Default Dashboards

**Story:** [S4: Role-based Dashboard Defaults](README.md)  
**Effort:** 20 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Vytvořit default dashboardy pro Admin, Tenant Manager, Analyst.

---

## 🎯 ACCEPTANCE CRITERIA

1. Admin default: Tenants + Users + System Health
2. Tenant Manager default: My Tenants + Workflows + Activity
3. Analyst default: Reports + Charts + KPIs
4. Auto-create on first login

---

## 🏗️ IMPLEMENTATION

```java
// backend/src/main/java/cz/muriel/core/dashboard/DefaultDashboardService.java
@Service
public class DefaultDashboardService {
  
  public Dashboard createDefaultForRole(User user) {
    return switch (user.getRole()) {
      case ADMIN -> createAdminDashboard(user);
      case TENANT_MANAGER -> createTenantManagerDashboard(user);
      case ANALYST -> createAnalystDashboard(user);
      default -> createBasicDashboard(user);
    };
  }
  
  private Dashboard createAdminDashboard(User user) {
    Dashboard dashboard = new Dashboard();
    dashboard.setName("Admin Overview");
    dashboard.setOwner(user);
    dashboard.setWidgets(List.of(
      new Widget("kpi", "Total Tenants", "{ entity: 'Tenants', metric: 'count' }"),
      new Widget("chart", "User Growth", "{ entity: 'Users', chartType: 'line' }"),
      new Widget("table", "System Health", "{ entity: 'SystemMetrics' }")
    ));
    return dashboardRepository.save(dashboard);
  }
}
```

---

## ✅ DELIVERABLES

- [ ] 3 default dashboard configs
- [ ] Auto-create service
- [ ] Unit tests

---

**Estimated:** 20 hours
