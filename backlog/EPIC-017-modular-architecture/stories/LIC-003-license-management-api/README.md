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
