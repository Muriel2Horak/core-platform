# DMS-014: Generic Documents Tab Component

**Epic:** EPIC-008 Document Management System  
**Phase:** 5 - Metamodel Integration  
**Estimate:** 0.5 day  
**LOC:** ~500

## Story

**AS** developer  
**I WANT** generic <DocumentsTab> component  
**SO THAT** all entity detail pages have consistent document UI

## Implementation

### 1. Documents Tab Component

**File:** `frontend/src/features/dms/components/DocumentsTab.tsx`

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
                    required={features.requiredDocuments}
                    uploaded={documents}
                />
            )}
        </Box>
    );
}
```

### 2. Usage in Entity Detail Pages

**Example:** `frontend/src/features/contracts/ContractDetail.tsx`

```tsx
export function ContractDetail() {
    const { contractId } = useParams();
    
    return (
        <Container>
            <Tabs>
                <Tab label="Details" />
                <Tab label="Workflow" />
                <Tab label="Documents" />
            </Tabs>
            
            <TabPanel value={activeTab} index={0}>
                <ContractForm contractId={contractId} />
            </TabPanel>
            
            <TabPanel value={activeTab} index={2}>
                <DocumentsTab
                    entityType="Contract"
                    entityId={contractId}
                    readonly={false}
                />
            </TabPanel>
        </Container>
    );
}
```

### 3. Required Documents Checklist

**Component:** Shows missing required documents

```tsx
function RequiredDocumentsChecklist({ required, uploaded }: Props) {
    const missing = required.filter(req => 
        !uploaded.some(doc => doc.documentType === req.type)
    );
    
    return (
        <Alert severity={missing.length > 0 ? "warning" : "success"}>
            <Typography variant="subtitle2">Required Documents:</Typography>
            <List>
                {required.map(req => (
                    <ListItem key={req.type}>
                        <ListItemIcon>
                            {uploaded.some(d => d.documentType === req.type) 
                                ? <CheckCircle color="success" /> 
                                : <Warning color="warning" />}
                        </ListItemIcon>
                        <ListItemText primary={req.label} />
                    </ListItem>
                ))}
            </List>
        </Alert>
    );
}
```

### 4. Document Grid with Actions

```tsx
function DocumentGrid({ documents, onDownload, onDelete, showVersions, showACL }: Props) {
    return (
        <Grid container spacing={2}>
            {documents.map(doc => (
                <Grid item xs={12} md={6} lg={4} key={doc.id}>
                    <Card>
                        <CardContent>
                            <Typography variant="h6">{doc.filename}</Typography>
                            <Typography variant="caption">{doc.contentType}</Typography>
                            <Typography variant="caption">{formatBytes(doc.size)}</Typography>
                        </CardContent>
                        <CardActions>
                            <IconButton onClick={() => onDownload(doc.id)}>
                                <Download />
                            </IconButton>
                            {showVersions && (
                                <IconButton onClick={() => openVersionHistory(doc.id)}>
                                    <History />
                                </IconButton>
                            )}
                            {showACL && (
                                <IconButton onClick={() => openACLDialog(doc.id)}>
                                    <Lock />
                                </IconButton>
                            )}
                            {onDelete && (
                                <IconButton onClick={() => onDelete(doc.id)} color="error">
                                    <Delete />
                                </IconButton>
                            )}
                        </CardActions>
                    </Card>
                </Grid>
            ))}
        </Grid>
    );
}
```

## API

Uses existing DMS API endpoints.

## Acceptance Criteria

- [ ] <DocumentsTab> component created
- [ ] Works with any entityType/entityId
- [ ] Validates uploads against metamodel features
- [ ] Shows required documents checklist
- [ ] Integrated in Contract detail page
- [ ] E2E: Open Contract → Documents tab → Upload file → See in grid

## Deliverables

- `DocumentsTab.tsx` component
- `DocumentGrid.tsx` component
- `RequiredDocumentsChecklist.tsx` component
- `FileUploadZone.tsx` (reusable)
- Integration examples
- Storybook stories
