---
id: DMS-001-T1
story: DMS-001
title: "Database Migration (V2__document_versioning.sql)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-08
updated: 2025-11-08
---

# DMS-001-T1: Database Migration (V2__document_versioning.sql)

> **Parent Story:** [DMS-001: Document Versioning](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

[IMPLEMENTATION DETAILS TO BE FILLED - See parent story README.md for complete specification]

Tento subtask implementuje **Database Migration (V2__document_versioning.sql)** pro Document Versioning story.

## ✅ Acceptance Criteria

Tento subtask je hotový pokud:

- [ ] Implementace podle specifikace v parent story README.md
- [ ] Kód prošel code review
- [ ] Všechny testy procházejí (unit + integration)
- [ ] Dokumentace aktualizovaná (pokud relevantní)
- [ ] Commit message: `feat(DMS-001-T1): Database Migration (V2__document_versioning.sql)`

## 📂 Files to Modify/Create

[Files will be listed based on parent story Implementation Mapping section]

## 🔧 Implementation Steps

[Detailed steps based on parent story specification]

## 🧪 Testing

[Test scenarios based on parent story Acceptance Criteria]

## 📝 Notes

- **Storage Key Format:** `{tenantId}/{documentId}/v{versionNumber}/{filename}`
  - Example: `tenant1/abc-123-def/v1/contract.pdf`
  - Example: `tenant1/abc-123-def/v2/contract.pdf` (after update)

- **Migration Strategy:** Existing documents become version 1 automatically

- **Checksum:** SHA-256 hash used for integrity + deduplication (delta storage optimization future)

## 📂 Implementation Mapping

### Files to Create
- `backend/src/main/resources/db/migration/V2__document_versioning.sql`

### Related Files (for context)
- `backend/src/main/resources/db/migration/V1__init.sql` (original schema)

## ✅ Definition of Done

- [ ] Migration file created with correct filename (V2__document_versioning.sql)
- [ ] Table `document_version` created with all columns (document_id, version_number, storage_key, checksum_sha256, size_bytes, mime_type, created_by, created_at, change_comment, signature fields)
- [ ] Function `get_next_version_number(p_document_id UUID)` works correctly
- [ ] Existing documents migrated to version 1 (INSERT SELECT from document table)
- [ ] `current_version_id` column added to `document` table
- [ ] All indexes created (idx_document_version_document, idx_document_version_created_at, idx_document_version_signed_by)
- [ ] Foreign key constraints work (CASCADE on delete)
- [ ] UNIQUE constraint on (document_id, version_number) enforced
- [ ] Flyway migration passes (`make db-migrate` nebo `mvn flyway:migrate`)
- [ ] Manual verification: `\d document_version` shows correct schema
- [ ] Code review approved
- [ ] Committed with message: `feat(DMS-001-T1): Add document_version table migration`
