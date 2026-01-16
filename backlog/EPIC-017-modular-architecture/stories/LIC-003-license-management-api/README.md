---
id: LIC-003
epic: EPIC-017-modular-architecture
title: "License Management API"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "2 days"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-017-modular-architecture/stories/LIC-003-license-management-api/README.md
    - backlog/EPIC-017-modular-architecture/README.md
---


# LIC-003: License Management API

**Status:** ⏳ **PENDING**  
**Effort:** 2 dny  
**Priority:** 🟡 MEDIUM  
**Dependencies:** LIC-001  
**Category:** Licensing

---

## 📖 User Story

**As a platform admin**,  
I want to upload and manage module licenses,  
So that I can activate modules for tenants.

---

## 🎯 Acceptance Criteria

- ⏳ Upload license (JWT token) for tenant + module
- ⏳ View all licenses (expiry dates, usage stats)
- ⏳ Revoke license manually
- ⏳ Download license file (for backup)
- ⏳ Email notifications before expiry (30 days, 7 days, 1 day)

---

## 🧩 Implementační tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | CRUD endpoints + validation | 0.5d | LIC-001 |
| 2 | Assign/revoke license per tenant | 0.5d | 1 |
| 3 | Import/export license keys | 0.25d | 1 |
| 4 | Audit log events | 0.25d | 1 |
| 5 | API tests | 0.25d | 2 |

---

## 💻 Implementation

### API Endpoints

```java
@RestController
@RequestMapping("/api/admin/licenses")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class LicenseController {
    
    @PostMapping
    public LicenseInfo uploadLicense(@RequestBody UploadLicenseRequest request) {
        // Validate JWT token
        // Store in database
        // Return license info (expiry, user limit)
    }
    
    @GetMapping
    public List<LicenseInfo> listLicenses(
        @RequestParam(required = false) String tenantId,
        @RequestParam(required = false) String status  // "active", "expiring", "expired"
    ) {
        // Return filtered licenses
    }
    
    @DeleteMapping("/{licenseId}")
    public void revokeLicense(@PathVariable Long licenseId) {
        // Soft delete (mark as revoked)
        // Clear cache
    }
}
```

---

**Last Updated:** 9. listopadu 2025
