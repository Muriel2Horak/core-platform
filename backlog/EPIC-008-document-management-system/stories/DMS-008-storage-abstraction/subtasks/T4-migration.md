---
id: DMS-008-T4
story: DMS-008
title: "Storage Factory"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-008-T4: Storage Factory

> **Parent Story:** [DMS-008](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Storage Factory

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `backend/src/main/java/cz/muriel/core/dms/storage/StorageServiceFactory.java`

## 🔧 Implementation Details

### Code Example 1 (java)

```java
@Component
public class StorageServiceFactory {
    public StorageService getStorageService(String tenantId) {
        Tenant tenant = tenantRepo.findById(tenantId).orElseThrow();
        return switch (tenant.getStorageProvider()) {
            case MINIO -> minioStorageService;
            case SHAREPOINT -> sharePointStorageService;
            case GOOGLE_DRIVE -> googleDriveStorageService;
        };
    }
}
```

**For complete implementation details, see parent story [`../README.md`](../README.md).**

## 🧪 Testing

- [ ] Unit tests for new code
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for user workflows (if applicable)
- [ ] Test coverage >80%

**Test scenarios:** See parent story Testing section.

## 📝 Notes

This subtask is part of DMS-008. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-008-T4): Storage Factory`
