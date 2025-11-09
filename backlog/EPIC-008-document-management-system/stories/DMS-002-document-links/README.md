# DMS-002: Document Links (M:N Entity Vazby)

**Epic:** [EPIC-008 Document Management System](../README.md)  
**Priority:** 🔴 P1 (KRITICKÉ - Core First-Class Component)  
**Status:** 📋 Not Started  
**Effort:** 1 den (~500 LOC)  
**Dependencies:** DMS-001 (Versioning)

---

## 🎯 Story Goal

Implementovat **M:N vazby mezi dokumenty a entitami**, umožnit:
- **1 dokument → N entit** (např. contract attachnut ke Case, UserProfile, Invoice)
- **1 entita → N dokumentů** (např. Case má primary contract + 5 attachments)
- **Link roles** (primary, attachment, contract, evidence)
- **Display order** pro seřazení příloh

---

## 📊 Current State vs. Target

### ❌ SOUČASNÝ STAV
```sql
-- V1__init.sql má pouze 1:1 vazbu
document (
  entity_type TEXT,  -- ❌ Pouze 1 entita!
  entity_id TEXT,
  ...
)
```

**Problém:** Dokument může být pouze u **1 entity**. Pokud chci stejný contract přiložit ke Case A i Case B, musím ho uploadnout 2x (duplicita).

### ✅ TARGET STAV
```sql
-- Nová tabulka: document_link (M:N)
document_link (
  id UUID PRIMARY KEY,
  document_id UUID REFERENCES document(id),
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  link_role TEXT NOT NULL,  -- primary | attachment | contract | evidence
  display_order INTEGER DEFAULT 0,
  linked_by TEXT NOT NULL,
  linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  metadata JSONB,
  UNIQUE (document_id, entity_type, entity_id, link_role)
);
```

**Výhoda:**
- 1 dokument může být u **více entit** (Case, UserProfile, Invoice...)
- Různé **role** (primary contract vs. attachment)
- **Display order** pro seřazení příloh v UI
- **Metadata** (custom fields per link)

---

## 🛠️ Implementation Tasks

### Task 1: Database Migration (0.5h)

**File:** `backend/src/main/resources/db/migration/V3__document_links.sql`

```sql
-- ===================================================================
-- V3: Document Links (M:N Entity Relationships)
-- Author: Core Platform Team
-- Date: 8. listopadu 2025
-- ===================================================================

-- 1. Create document_link table (M:N vazba)
CREATE TABLE document_link (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES document(id) ON DELETE CASCADE,
    
    -- Entity reference (polymorphic)
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    
    -- Link metadata
    link_role TEXT NOT NULL DEFAULT 'attachment',
    display_order INTEGER NOT NULL DEFAULT 0,
    
    -- Audit
    linked_by TEXT NOT NULL,
    linked_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    
    -- Custom metadata (flexible attributes)
    metadata JSONB DEFAULT '{}'::jsonb,
    
    -- Prevent duplicate links (same doc + entity + role)
    UNIQUE (document_id, entity_type, entity_id, link_role),
    
    -- Validate link_role enum
    CHECK (link_role IN ('primary', 'attachment', 'contract', 'evidence', 'invoice', 'receipt'))
);

-- 2. Indexes for performance
CREATE INDEX idx_document_link_document ON document_link(document_id);
CREATE INDEX idx_document_link_entity ON document_link(entity_type, entity_id);
CREATE INDEX idx_document_link_role ON document_link(link_role);
CREATE INDEX idx_document_link_display_order ON document_link(entity_type, entity_id, display_order);

-- 3. Migrate existing document.entity_type/entity_id to document_link
INSERT INTO document_link (document_id, entity_type, entity_id, link_role, linked_by, linked_at)
SELECT 
    id AS document_id,
    entity_type,
    entity_id,
    'primary' AS link_role,  -- Existing docs = primary
    uploaded_by AS linked_by,
    uploaded_at AS linked_at
FROM document
WHERE entity_type IS NOT NULL AND entity_id IS NOT NULL;

-- 4. Drop old columns (no longer needed)
ALTER TABLE document DROP COLUMN entity_type;
ALTER TABLE document DROP COLUMN entity_id;

-- 5. Function to get next display order
CREATE OR REPLACE FUNCTION get_next_display_order(
    p_entity_type TEXT,
    p_entity_id TEXT
) RETURNS INTEGER AS $$
DECLARE
    v_max_order INTEGER;
BEGIN
    SELECT COALESCE(MAX(display_order), -1) + 1
    INTO v_max_order
    FROM document_link
    WHERE entity_type = p_entity_type AND entity_id = p_entity_id;
    
    RETURN v_max_order;
END;
$$ LANGUAGE plpgsql;

COMMENT ON TABLE document_link IS 'M:N links between documents and entities (Case, UserProfile, Invoice...)';
COMMENT ON COLUMN document_link.link_role IS 'Role of link: primary (main doc), attachment, contract, evidence, invoice, receipt';
COMMENT ON COLUMN document_link.display_order IS 'Order for UI display (0 = first, 1 = second, etc.)';
COMMENT ON COLUMN document_link.metadata IS 'Custom attributes per link (e.g., {"category": "legal", "required": true})';
```

---

### Task 2: Java Entity & Repository (1h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentLink.java`

```java
package cz.muriel.core.document;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * M:N link between Document and Entity (Case, UserProfile, Invoice...).
 * Allows 1 document to be attached to multiple entities with different roles.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentLink {
    private UUID id;
    private UUID documentId;
    
    // Polymorphic entity reference
    private String entityType;  // Case, UserProfile, Invoice...
    private String entityId;
    
    // Link metadata
    private LinkRole linkRole;
    private Integer displayOrder;
    
    // Audit
    private String linkedBy;
    private Instant linkedAt;
    
    // Custom metadata (flexible)
    private JsonNode metadata;
    
    public enum LinkRole {
        PRIMARY,      // Main document (e.g., primary contract)
        ATTACHMENT,   // Generic attachment
        CONTRACT,     // Contract document
        EVIDENCE,     // Evidence/proof document
        INVOICE,      // Invoice document
        RECEIPT       // Payment receipt
    }
}
```

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentLinkRepository.java`

```java
package cz.muriel.core.document;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DocumentLinkRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    private final RowMapper<DocumentLink> ROW_MAPPER = (rs, rowNum) -> {
        try {
            return DocumentLink.builder()
                .id(UUID.fromString(rs.getString("id")))
                .documentId(UUID.fromString(rs.getString("document_id")))
                .entityType(rs.getString("entity_type"))
                .entityId(rs.getString("entity_id"))
                .linkRole(DocumentLink.LinkRole.valueOf(rs.getString("link_role").toUpperCase()))
                .displayOrder(rs.getInt("display_order"))
                .linkedBy(rs.getString("linked_by"))
                .linkedAt(rs.getTimestamp("linked_at").toInstant())
                .metadata(rs.getString("metadata") != null 
                    ? objectMapper.readTree(rs.getString("metadata")) 
                    : null)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to map DocumentLink", e);
        }
    };
    
    /**
     * Create link between document and entity.
     */
    public DocumentLink save(DocumentLink link) {
        if (link.getId() == null) {
            link.setId(UUID.randomUUID());
        }
        
        String sql = """
            INSERT INTO document_link (
                id, document_id, entity_type, entity_id,
                link_role, display_order, linked_by, metadata
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb)
            ON CONFLICT (document_id, entity_type, entity_id, link_role)
            DO UPDATE SET
                display_order = EXCLUDED.display_order,
                metadata = EXCLUDED.metadata
            RETURNING *
            """;
        
        return jdbcTemplate.queryForObject(sql, ROW_MAPPER,
            link.getId(),
            link.getDocumentId(),
            link.getEntityType(),
            link.getEntityId(),
            link.getLinkRole().name().toLowerCase(),
            link.getDisplayOrder(),
            link.getLinkedBy(),
            link.getMetadata() != null ? link.getMetadata().toString() : "{}"
        );
    }
    
    /**
     * List all documents linked to entity (ordered by display_order).
     */
    public List<DocumentLink> findByEntity(String entityType, String entityId) {
        String sql = """
            SELECT * FROM document_link
            WHERE entity_type = ? AND entity_id = ?
            ORDER BY display_order ASC
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, entityType, entityId);
    }
    
    /**
     * List all entities linked to document.
     */
    public List<DocumentLink> findByDocument(UUID documentId) {
        String sql = """
            SELECT * FROM document_link
            WHERE document_id = ?
            ORDER BY linked_at DESC
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, documentId);
    }
    
    /**
     * Get specific link.
     */
    public Optional<DocumentLink> findById(UUID linkId) {
        String sql = "SELECT * FROM document_link WHERE id = ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, linkId)
            .stream().findFirst();
    }
    
    /**
     * Delete link (unlink document from entity).
     */
    public void delete(UUID linkId) {
        jdbcTemplate.update("DELETE FROM document_link WHERE id = ?", linkId);
    }
    
    /**
     * Get next display order for entity.
     */
    public int getNextDisplayOrder(String entityType, String entityId) {
        String sql = "SELECT get_next_display_order(?, ?)";
        return jdbcTemplate.queryForObject(sql, Integer.class, entityType, entityId);
    }
    
    /**
     * Check if link exists.
     */
    public boolean exists(UUID documentId, String entityType, String entityId, DocumentLink.LinkRole role) {
        String sql = """
            SELECT COUNT(*) FROM document_link
            WHERE document_id = ? AND entity_type = ? AND entity_id = ? AND link_role = ?
            """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
            documentId, entityType, entityId, role.name().toLowerCase());
        return count != null && count > 0;
    }
}
```

---

### Task 3: Document Service Extensions (1h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentService.java`

```java
// ===== ADD TO EXISTING DocumentService.java =====

@RequiredArgsConstructor
@Service
public class DocumentService {
    private final DocumentLinkRepository linkRepository;
    // ... existing fields ...
    
    /**
     * Link document to entity.
     */
    public DocumentLink linkDocumentToEntity(
        UUID documentId,
        String entityType,
        String entityId,
        DocumentLink.LinkRole role,
        String userId
    ) {
        // 1. Verify document exists and user has access
        Document document = getDocument(documentId, getCurrentTenantId())
            .orElseThrow(() -> new NotFoundException("Document not found: " + documentId));
        
        // 2. Check if link already exists
        if (linkRepository.exists(documentId, entityType, entityId, role)) {
            throw new IllegalStateException(
                String.format("Document %s already linked to %s/%s with role %s",
                    documentId, entityType, entityId, role)
            );
        }
        
        // 3. Get next display order
        int displayOrder = linkRepository.getNextDisplayOrder(entityType, entityId);
        
        // 4. Create link
        DocumentLink link = DocumentLink.builder()
            .documentId(documentId)
            .entityType(entityType)
            .entityId(entityId)
            .linkRole(role)
            .displayOrder(displayOrder)
            .linkedBy(userId)
            .build();
        
        DocumentLink savedLink = linkRepository.save(link);
        
        log.info("Linked document {} to entity {}/{} with role {} (tenant {})",
            documentId, entityType, entityId, role, getCurrentTenantId());
        
        return savedLink;
    }
    
    /**
     * Unlink document from entity.
     */
    public void unlinkDocumentFromEntity(UUID linkId, String userId) {
        // 1. Verify link exists
        DocumentLink link = linkRepository.findById(linkId)
            .orElseThrow(() -> new NotFoundException("Link not found: " + linkId));
        
        // 2. Verify user has access to document
        getDocument(link.getDocumentId(), getCurrentTenantId())
            .orElseThrow(() -> new NotFoundException("Document not found"));
        
        // 3. Delete link
        linkRepository.delete(linkId);
        
        log.info("Unlinked document {} from entity {}/{} (tenant {})",
            link.getDocumentId(), link.getEntityType(), link.getEntityId(), getCurrentTenantId());
    }
    
    /**
     * List all documents linked to entity (with metadata).
     */
    public List<DocumentWithLink> listDocumentsForEntity(
        String entityType,
        String entityId
    ) {
        // 1. Get all links for entity
        List<DocumentLink> links = linkRepository.findByEntity(entityType, entityId);
        
        // 2. Load document metadata for each link
        return links.stream()
            .map(link -> {
                Optional<Document> docOpt = getDocument(link.getDocumentId(), getCurrentTenantId());
                return docOpt.map(doc -> new DocumentWithLink(doc, link)).orElse(null);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    
    /**
     * List all entities linked to document.
     */
    public List<DocumentLink> listEntitiesForDocument(UUID documentId) {
        // Verify access
        getDocument(documentId, getCurrentTenantId())
            .orElseThrow(() -> new NotFoundException("Document not found"));
        
        return linkRepository.findByDocument(documentId);
    }
    
    /**
     * Reorder documents for entity (update display_order).
     */
    public void reorderDocuments(
        String entityType,
        String entityId,
        List<UUID> linkIdsInOrder
    ) {
        for (int i = 0; i < linkIdsInOrder.size(); i++) {
            UUID linkId = linkIdsInOrder.get(i);
            jdbcTemplate.update(
                "UPDATE document_link SET display_order = ? WHERE id = ?",
                i, linkId
            );
        }
        
        log.info("Reordered {} documents for entity {}/{}", 
            linkIdsInOrder.size(), entityType, entityId);
    }
}

/**
 * DTO: Document + Link metadata.
 */
@Data
@AllArgsConstructor
public class DocumentWithLink {
    private Document document;
    private DocumentLink link;
}
```

---

### Task 4: REST API Endpoints (1h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentController.java`

```java
// ===== ADD TO EXISTING DocumentController.java =====

@RestController
@RequestMapping("/api/dms/documents")
@RequiredArgsConstructor
public class DocumentController {
    
    // ===== DOCUMENT LINK ENDPOINTS =====
    
    /**
     * Link document to entity.
     */
    @PostMapping("/{documentId}/links")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ResponseEntity<DocumentLink> linkToEntity(
        @PathVariable UUID documentId,
        @RequestBody LinkRequest request,
        Principal principal
    ) {
        DocumentLink link = documentService.linkDocumentToEntity(
            documentId,
            request.getEntityType(),
            request.getEntityId(),
            request.getRole(),
            principal.getName()
        );
        
        return ResponseEntity.ok(link);
    }
    
    /**
     * Unlink document from entity.
     */
    @DeleteMapping("/links/{linkId}")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ResponseEntity<Void> unlinkFromEntity(
        @PathVariable UUID linkId,
        Principal principal
    ) {
        documentService.unlinkDocumentFromEntity(linkId, principal.getName());
        return ResponseEntity.noContent().build();
    }
    
    /**
     * List all documents for entity (ordered by display_order).
     */
    @GetMapping("/entities/{entityType}/{entityId}")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<List<DocumentWithLink>> listDocumentsForEntity(
        @PathVariable String entityType,
        @PathVariable String entityId
    ) {
        List<DocumentWithLink> documents = documentService.listDocumentsForEntity(
            entityType, entityId
        );
        return ResponseEntity.ok(documents);
    }
    
    /**
     * List all entities linked to document.
     */
    @GetMapping("/{documentId}/links")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<List<DocumentLink>> listEntitiesForDocument(
        @PathVariable UUID documentId
    ) {
        List<DocumentLink> links = documentService.listEntitiesForDocument(documentId);
        return ResponseEntity.ok(links);
    }
    
    /**
     * Reorder documents for entity (drag & drop).
     */
    @PutMapping("/entities/{entityType}/{entityId}/reorder")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ResponseEntity<Void> reorderDocuments(
        @PathVariable String entityType,
        @PathVariable String entityId,
        @RequestBody ReorderRequest request
    ) {
        documentService.reorderDocuments(entityType, entityId, request.getLinkIds());
        return ResponseEntity.noContent().build();
    }
}

// ===== REQUEST DTOs =====

@Data
class LinkRequest {
    private String entityType;
    private String entityId;
    private DocumentLink.LinkRole role;
}

@Data
class ReorderRequest {
    private List<UUID> linkIds;  // In desired display order
}
```

---

### Task 5: Frontend Document List Component (1.5h)

**File:** `frontend/src/components/EntityDocumentList.tsx`

```tsx
import React, { useState, useEffect } from 'react';
import {
  List,
  ListItem,
  ListItemText,
  ListItemIcon,
  ListItemSecondaryAction,
  IconButton,
  Chip,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Select,
  MenuItem,
  FormControl,
  InputLabel
} from '@mui/material';
import {
  Description,
  Delete,
  Download,
  Link as LinkIcon,
  DragIndicator
} from '@mui/icons-material';
import { DragDropContext, Droppable, Draggable } from 'react-beautiful-dnd';
import axios from 'axios';

interface DocumentWithLink {
  document: {
    id: string;
    filename: string;
    mimeType: string;
    sizeBytes: number;
    uploadedBy: string;
    uploadedAt: string;
  };
  link: {
    id: string;
    documentId: string;
    entityType: string;
    entityId: string;
    linkRole: 'PRIMARY' | 'ATTACHMENT' | 'CONTRACT' | 'EVIDENCE' | 'INVOICE' | 'RECEIPT';
    displayOrder: number;
    linkedBy: string;
    linkedAt: string;
  };
}

interface Props {
  entityType: string;
  entityId: string;
}

export const EntityDocumentList: React.FC<Props> = ({ entityType, entityId }) => {
  const [documents, setDocuments] = useState<DocumentWithLink[]>([]);
  const [linkDialogOpen, setLinkDialogOpen] = useState(false);
  const [selectedDocumentId, setSelectedDocumentId] = useState<string>('');
  const [selectedRole, setSelectedRole] = useState<string>('ATTACHMENT');
  
  useEffect(() => {
    loadDocuments();
  }, [entityType, entityId]);
  
  const loadDocuments = async () => {
    try {
      const response = await axios.get(
        `/api/dms/documents/entities/${entityType}/${entityId}`
      );
      setDocuments(response.data);
    } catch (error) {
      console.error('Failed to load documents:', error);
    }
  };
  
  const handleLinkDocument = async () => {
    try {
      await axios.post(`/api/dms/documents/${selectedDocumentId}/links`, {
        entityType,
        entityId,
        role: selectedRole
      });
      setLinkDialogOpen(false);
      loadDocuments();
    } catch (error) {
      console.error('Failed to link document:', error);
    }
  };
  
  const handleUnlink = async (linkId: string) => {
    if (!confirm('Unlink this document from entity?')) return;
    
    try {
      await axios.delete(`/api/dms/documents/links/${linkId}`);
      loadDocuments();
    } catch (error) {
      console.error('Failed to unlink document:', error);
    }
  };
  
  const handleDownload = async (documentId: string) => {
    try {
      const response = await axios.get(`/api/dms/documents/${documentId}/download`);
      window.open(response.data.downloadUrl, '_blank');
    } catch (error) {
      console.error('Failed to download document:', error);
    }
  };
  
  const handleDragEnd = async (result: any) => {
    if (!result.destination) return;
    
    const items = Array.from(documents);
    const [reorderedItem] = items.splice(result.source.index, 1);
    items.splice(result.destination.index, 0, reorderedItem);
    
    setDocuments(items);
    
    // Update server
    try {
      await axios.put(
        `/api/dms/documents/entities/${entityType}/${entityId}/reorder`,
        { linkIds: items.map(item => item.link.id) }
      );
    } catch (error) {
      console.error('Failed to reorder documents:', error);
      loadDocuments(); // Reload on error
    }
  };
  
  const getRoleColor = (role: string) => {
    switch (role) {
      case 'PRIMARY': return 'primary';
      case 'CONTRACT': return 'success';
      case 'EVIDENCE': return 'warning';
      case 'INVOICE': return 'info';
      default: return 'default';
    }
  };
  
  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16 }}>
        <h3>Documents ({documents.length})</h3>
        <Button
          variant="contained"
          startIcon={<LinkIcon />}
          onClick={() => setLinkDialogOpen(true)}
        >
          Link Document
        </Button>
      </div>
      
      <DragDropContext onDragEnd={handleDragEnd}>
        <Droppable droppableId="documents">
          {(provided) => (
            <List {...provided.droppableProps} ref={provided.innerRef}>
              {documents.map((item, index) => (
                <Draggable
                  key={item.link.id}
                  draggableId={item.link.id}
                  index={index}
                >
                  {(provided) => (
                    <ListItem
                      ref={provided.innerRef}
                      {...provided.draggableProps}
                      sx={{ border: '1px solid #ddd', marginBottom: 1, borderRadius: 1 }}
                    >
                      <ListItemIcon {...provided.dragHandleProps}>
                        <DragIndicator />
                      </ListItemIcon>
                      
                      <ListItemIcon>
                        <Description />
                      </ListItemIcon>
                      
                      <ListItemText
                        primary={item.document.filename}
                        secondary={`${(item.document.sizeBytes / 1024).toFixed(1)} KB`}
                      />
                      
                      <Chip
                        label={item.link.linkRole}
                        color={getRoleColor(item.link.linkRole) as any}
                        size="small"
                        sx={{ marginRight: 1 }}
                      />
                      
                      <ListItemSecondaryAction>
                        <IconButton
                          edge="end"
                          onClick={() => handleDownload(item.document.id)}
                          sx={{ marginRight: 1 }}
                        >
                          <Download />
                        </IconButton>
                        
                        <IconButton
                          edge="end"
                          onClick={() => handleUnlink(item.link.id)}
                        >
                          <Delete />
                        </IconButton>
                      </ListItemSecondaryAction>
                    </ListItem>
                  )}
                </Draggable>
              ))}
              {provided.placeholder}
            </List>
          )}
        </Droppable>
      </DragDropContext>
      
      {/* Link Document Dialog */}
      <Dialog open={linkDialogOpen} onClose={() => setLinkDialogOpen(false)}>
        <DialogTitle>Link Document to {entityType}</DialogTitle>
        <DialogContent>
          <FormControl fullWidth sx={{ marginTop: 2 }}>
            <InputLabel>Document</InputLabel>
            <Select
              value={selectedDocumentId}
              onChange={(e) => setSelectedDocumentId(e.target.value)}
            >
              {/* TODO: Load available documents from API */}
              <MenuItem value="doc-1">Contract Template.pdf</MenuItem>
              <MenuItem value="doc-2">Evidence Photo.jpg</MenuItem>
            </Select>
          </FormControl>
          
          <FormControl fullWidth sx={{ marginTop: 2 }}>
            <InputLabel>Link Role</InputLabel>
            <Select
              value={selectedRole}
              onChange={(e) => setSelectedRole(e.target.value)}
            >
              <MenuItem value="PRIMARY">Primary</MenuItem>
              <MenuItem value="ATTACHMENT">Attachment</MenuItem>
              <MenuItem value="CONTRACT">Contract</MenuItem>
              <MenuItem value="EVIDENCE">Evidence</MenuItem>
              <MenuItem value="INVOICE">Invoice</MenuItem>
              <MenuItem value="RECEIPT">Receipt</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setLinkDialogOpen(false)}>Cancel</Button>
          <Button
            onClick={handleLinkDocument}
            variant="contained"
            disabled={!selectedDocumentId}
          >
            Link
          </Button>
        </DialogActions>
      </Dialog>
    </div>
  );
};
```

---

## ✅ Acceptance Criteria

- [ ] **Database Migration V3** runs successfully
  - `document_link` table created
  - Existing `document.entity_type/entity_id` migrated to links
  - Old columns dropped from `document` table
  - Indexes created for performance

- [ ] **Link Document to Entity** works
  - `POST /api/dms/documents/{id}/links` creates M:N link
  - Can link 1 document to multiple entities (Case, UserProfile, Invoice...)
  - Link role specified (primary, attachment, contract, evidence)
  - Display order auto-assigned (sequential)

- [ ] **Unlink Document** works
  - `DELETE /api/dms/documents/links/{linkId}` removes link
  - Document itself NOT deleted (only link removed)
  - Other links to same document preserved

- [ ] **List Documents for Entity** works
  - `GET /api/dms/documents/entities/{type}/{id}` returns all linked documents
  - Ordered by `display_order` ASC
  - Response includes: document metadata + link metadata (role, displayOrder)

- [ ] **List Entities for Document** works
  - `GET /api/dms/documents/{id}/links` returns all entities linked to document
  - Shows where document is used (multiple entities)

- [ ] **Reorder Documents** works (Drag & Drop)
  - `PUT /api/dms/documents/entities/{type}/{id}/reorder` updates display_order
  - Frontend drag & drop updates order
  - Order persisted to database

- [ ] **Frontend EntityDocumentList** works
  - Shows all documents linked to entity
  - Drag & drop to reorder
  - Role badges (primary, contract, evidence...)
  - Download button per document
  - Unlink button (removes link, keeps document)

---

## 🧪 Testing Plan

### Unit Tests (JUnit)

```java
@Test
void testLinkDocumentToMultipleEntities() {
    // Upload document
    DocumentVersion doc = service.uploadDocument(tenantId, file, "user1");
    
    // Link to Case A
    DocumentLink link1 = service.linkDocumentToEntity(
        doc.getDocumentId(), "Case", "case-a", LinkRole.PRIMARY, "user1"
    );
    assertEquals(0, link1.getDisplayOrder());
    
    // Link SAME document to Case B
    DocumentLink link2 = service.linkDocumentToEntity(
        doc.getDocumentId(), "Case", "case-b", LinkRole.ATTACHMENT, "user1"
    );
    assertEquals(0, link2.getDisplayOrder());
    
    // Both links exist
    List<DocumentLink> linksForA = linkRepository.findByEntity("Case", "case-a");
    assertEquals(1, linksForA.size());
    
    List<DocumentLink> linksForB = linkRepository.findByEntity("Case", "case-b");
    assertEquals(1, linksForB.size());
}

@Test
void testReorderDocuments() {
    // Create 3 document links
    DocumentLink link1 = createDocumentLink("Case", "case-1", 0);
    DocumentLink link2 = createDocumentLink("Case", "case-1", 1);
    DocumentLink link3 = createDocumentLink("Case", "case-1", 2);
    
    // Reorder: [link3, link1, link2]
    service.reorderDocuments("Case", "case-1", 
        List.of(link3.getId(), link1.getId(), link2.getId()));
    
    // Verify new order
    List<DocumentLink> links = linkRepository.findByEntity("Case", "case-1");
    assertEquals(link3.getId(), links.get(0).getId());
    assertEquals(0, links.get(0).getDisplayOrder());
}
```

---

## 📦 Deliverables

1. ✅ Database migration: `V3__document_links.sql`
2. ✅ Java entities: `DocumentLink.java`, `DocumentLinkRepository.java`
3. ✅ Service methods: `linkDocumentToEntity()`, `unlinkDocumentFromEntity()`, `reorderDocuments()`
4. ✅ REST API: 5 new endpoints (link, unlink, list by entity, list by document, reorder)
5. ✅ Frontend: `EntityDocumentList.tsx` component s drag & drop
6. ✅ Unit tests: 3+ test cases

---

## 🔗 Dependencies

- **Depends on:** DMS-001 (Versioning) - needs `document.id` reference
- **Blocks:** DMS-014 (Documents Tab) - uses this API
- **Related:** DMS-003 (ACL) - permissions per link

---

**Ready for Implementation** ✅  
**Estimated Completion:** 1 den (~500 LOC)
