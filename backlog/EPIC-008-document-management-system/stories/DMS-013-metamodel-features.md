# DMS-013: Metamodel Document Features Configuration

**Epic:** EPIC-008 Document Management System  
**Phase:** 5 - Metamodel Integration  
**Estimate:** 0.5 day  
**LOC:** ~400

## Story

**AS** metamodel designer  
**I WANT** document config per entity type  
**SO THAT** each entity has specific document requirements

## Implementation

### 1. Metamodel YAML Extension

**File:** `metamodel/entities/Contract.yml`

```yaml
entity:
  name: Contract
  features:
    documents:
      enabled: true
      allowedTypes:
        - "application/pdf"
        - "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        - "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      maxFileSize: 10485760  # 10 MB
      requiredDocuments:
        - type: "contract_signed"
          label: "Signed Contract"
          required: true
          minVersions: 1
          states: ["approved", "active"]
        - type: "invoice"
          label: "Invoice"
          required: false
      permissions:
        upload:
          roles: ["CONTRACT_MANAGER", "ADMIN"]
        download:
          roles: ["CONTRACT_MANAGER", "CONTRACT_VIEWER", "ADMIN"]
        delete:
          roles: ["ADMIN"]
```

### 2. Document Features Service

**File:** `backend/src/main/java/cz/muriel/core/metamodel/features/DocumentFeaturesService.java`

```java
@Service
public class DocumentFeaturesService {
    
    public DocumentFeatures getDocumentFeatures(String entityType) {
        MetamodelEntity entity = metamodelService.getEntity(entityType);
        return entity.getFeatures().getDocuments();
    }
    
    public void validateUpload(String entityType, String filename, long size, String contentType, String userRole) {
        DocumentFeatures features = getDocumentFeatures(entityType);
        
        if (!features.isEnabled()) {
            throw new ValidationException("Documents not enabled for " + entityType);
        }
        
        // Check file type
        if (!features.getAllowedTypes().contains(contentType)) {
            throw new ValidationException("File type not allowed: " + contentType);
        }
        
        // Check file size
        if (size > features.getMaxFileSize()) {
            throw new ValidationException("File size exceeds limit: " + features.getMaxFileSize());
        }
        
        // Check permissions
        if (!features.getPermissions().getUpload().getRoles().contains(userRole)) {
            throw new ForbiddenException("User role not allowed to upload");
        }
    }
    
    public List<RequiredDocument> getMissingDocuments(String entityType, UUID entityId, String state) {
        DocumentFeatures features = getDocumentFeatures(entityType);
        
        return features.getRequiredDocuments().stream()
            .filter(doc -> doc.isRequired() && doc.getStates().contains(state))
            .filter(doc -> !hasDocument(entityId, doc.getType()))
            .collect(Collectors.toList());
    }
}
```

### 3. Validation in Document Service

**Update:** `DocumentService.upload()`

```java
public Document upload(...) {
    // Validate against metamodel features
    documentFeaturesService.validateUpload(
        entityType,
        filename,
        inputStream.available(),
        contentType,
        getCurrentUserRole()
    );
    
    // Existing upload logic
    // ...
}
```

### 4. Required Documents Check

**Integration in Workflow Transitions:**

```java
public void validateTransition(WorkflowInstance instance, String toState) {
    List<RequiredDocument> missing = documentFeaturesService.getMissingDocuments(
        instance.getEntityType(),
        instance.getEntityId(),
        toState
    );
    
    if (!missing.isEmpty()) {
        throw new ValidationException("Missing required documents: " + 
            missing.stream().map(RequiredDocument::getLabel).collect(Collectors.joining(", ")));
    }
}
```

## API

GET `/api/metamodel/entities/{type}/document-features` - Get document config  
GET `/api/metamodel/entities/{type}/required-documents?state=approved` - Get required docs for state

## Acceptance Criteria

- [ ] Metamodel YAML supports `features.documents`
- [ ] DocumentFeaturesService validates uploads
- [ ] File type/size validation works
- [ ] Permission check per role
- [ ] Required documents check in workflow
- [ ] E2E: Upload document to Contract → validate against metamodel config

## Deliverables

- `DocumentFeatures.java` config class
- `DocumentFeaturesService.java`
- Metamodel YAML examples
- Validation integration
- Unit tests
