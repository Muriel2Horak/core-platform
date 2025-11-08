# T2: Permission System

**Story:** [S10: Dashboard & View Sharing](README.md)  
**Effort:** 5 hours  
**Priority:** P1  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Enforce share permissions (view-only vs edit).

---

## 🎯 ACCEPTANCE CRITERIA

1. Check permissions on load
2. Disable edit for view-only
3. Backend validation
4. Share token expiration

---

## 🏗️ IMPLEMENTATION

```java
// backend/src/main/java/cz/muriel/core/sharing/ShareService.java
@Service
public class ShareService {
  
  public boolean canEdit(Dashboard dashboard, User user) {
    if (dashboard.getOwner().equals(user)) return true;
    
    return shareRepository.findByDashboardAndUser(dashboard, user)
      .map(share -> share.getPermission() == SharePermission.EDIT)
      .orElse(false);
  }
  
  public Dashboard getByToken(String token) {
    DashboardShare share = shareRepository.findByToken(token)
      .orElseThrow(() -> new NotFoundException("Invalid share link"));
    
    if (share.getExpiresAt() != null && share.getExpiresAt().isBefore(LocalDateTime.now())) {
      throw new ExpiredException("Share link expired");
    }
    
    return share.getDashboard();
  }
}
```

---

## ✅ DELIVERABLES

- [ ] Permission checks
- [ ] Token validation
- [ ] Read-only mode

---

**Estimated:** 5 hours
