---
id: META-014
epic: EPIC-005-metamodel-generator-studio
title: "DMS Integration"
priority: P2
status: todo
assignee: ""
created: 2026-01-15
updated: 2026-01-15
estimate: "80 hours"
path_mapping:
  code_paths: []
  test_paths: []
  docs_paths:
    - backlog/EPIC-005-metamodel-generator-studio/stories/META14-dms-integration/README.md
    - backlog/EPIC-005-metamodel-generator-studio/README.md
---

# META-014: DMS Integration

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟡 **PLANNED**  
**Priorita:** P2 (Medium)  
**Estimated LOC:** ~1,200 řádků  
**Effort:** 2 týdny (80 hodin)

---

## 📋 Story Description

Jako **platform developer**, chci **document management z metamodelu**, abych **mohl připojit soubory k entitám deklarativně**.

---

## 🎯 Business Value

**HIGH-LEVEL požadavek:**
> 8️⃣ DMS & dokumenty: V metamodelu zda entita podporuje dokumenty (`attachments.enabled = true`), typy vazeb (primary doc, multiple, typ dokumentu), viditelnost, verzování, povolené operace (download, update, delete), napojení na interní MinIO nebo externí úložiště (M365/SharePoint, Google Drive) per-tenant. UI: záložka/tab "Dokumenty" driven z metamodelu.

---

## 🎯 Acceptance Criteria

### AC1: Attachments Config v YAML

```yaml
entity: Contract
attachments:
  enabled: true
  multiple: true
  allowedTypes: ["pdf", "docx", "xlsx"]
  maxSize: 10MB
  storage: MinIO  # or M365, GoogleDrive
  versioning: true
```

### AC2: Upload API

- **WHEN** volám `POST /api/contracts/123/attachments`
- **THEN** soubor se uloží do MinIO
- **Response**:

```json
{
  "id": "doc-456",
  "filename": "contract.pdf",
  "size": 524288,
  "mimeType": "application/pdf",
  "url": "/api/contracts/123/attachments/doc-456"
}
```

### AC3: Document Tab UI

- **GIVEN** entita s `attachments.enabled: true`
- **THEN** detail view má záložku "Documents"
- **Obsahuje**: Seznam souborů, upload button, preview, download

---

## Implementacni tasky

| Order | Task | Estimate | Depends on |
| --- | --- | --- | --- |
| 1 | Attachments schema + validation + storage config | 15h | META-001 |
| 2 | Backend API + storage providers (MinIO/external) | 35h | T1 |
| 3 | UI Document tab + permissions | 20h | T2, EPIC-014 |
| 4 | Testy + docs (upload/download/versioning) | 10h | T2, T3 |

---

## 🏗️ Implementation

```java
@Service
public class DocumentService {
    private final MinioClient minioClient;
    
    public DocumentMetadata upload(Long entityId, String entityType, MultipartFile file) {
        String objectName = String.format("%s/%s/%s", entityType, entityId, file.getOriginalFilename());
        
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket("entity-documents")
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build()
        );
        
        return DocumentMetadata.builder()
            .filename(file.getOriginalFilename())
            .size(file.getSize())
            .mimeType(file.getContentType())
            .url("/api/" + entityType + "/" + entityId + "/attachments/" + objectName)
            .build();
    }
}
```

---

**Story Owner:** Backend Team  
**Priority:** P2  
**Effort:** 2 týdny
