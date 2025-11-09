---
id: DMS-014-T1
story: DMS-014
title: "Documents Tab Component"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-014-T1: Documents Tab Component

> **Parent Story:** [DMS-014](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Documents Tab Component

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `frontend/src/features/dms/components/DocumentsTab.tsx`

## 🔧 Implementation Details

### Code Example 1 (tsx)

```tsx
interface DocumentsTabProps {
    entityType: string;
    entityId: string;
    readonly?: boolean;
}

export function DocumentsTab({ entityType, entityId, readonly }: DocumentsTabProps) {
    const [documents, setDocuments] = useState<Document[]>([]);
    const [features, setFeatures] = useState<DocumentFeatures>();
    const { uploadDocument, deleteDocument, downloadDocument } = useDocuments();
    
    useEffect(() => {
        // Load documents for entity
        api.get(`/api/dms/entities/${entityType}/${entityId}/documents`)
            .then(res => setDocuments(res.data));
        
        // Load metamodel features
        api.get(`/api/metamodel/entities/${entityType}/document-features`)
            .then(res => setFeatures(res.data));
    }, [entityType, entityId]);
    
    const handleUpload = async (file: File) => {
        if (!features) return;
        
        // Validate file type
        if (!features.allowedTypes.includes(file.type)) {
            toast.error(`File type not allowed. Allowed: ${features.allowedTypes.join(', ')}`);
            return;
        }
        
        // Validate file size
        if (file.size > features.maxFileSize) {
            toast.error(`File size exceeds ${features.maxFileSize / 1024 / 1024} MB`);
            return;
        }
        
        await uploadDocument(entityType, entityId, file);
        refreshDocuments();
    };
    
    return (
        <Box>
            {!readonly && (
                <FileUploadZone
                    onUpload={handleUpload}
                    acceptedTypes={features?.allowedTypes}
                    maxSize={features?.maxFileSize}
                />
            )}
            
            <DocumentGrid
                documents={documents}
                onDownload={downloadDocument}
                onDelete={readonly ? undefined : deleteDocument}
                showVersions={true}
                showACL={true}
            />
            
            {features?.requiredDocuments && (
                <RequiredDocumentsChecklist

// ... (see parent story for complete code)
```

**For complete implementation details, see parent story [`../README.md`](../README.md).**

## 🧪 Testing

- [ ] Unit tests for new code
- [ ] Integration tests for API endpoints (if applicable)
- [ ] E2E tests for user workflows (if applicable)
- [ ] Test coverage >80%

**Test scenarios:** See parent story Testing section.

## 📝 Notes

This subtask is part of DMS-014. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-014-T1): Documents Tab Component`
