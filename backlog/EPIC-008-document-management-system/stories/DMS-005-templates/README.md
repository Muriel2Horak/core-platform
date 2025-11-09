# DMS-005: Document Templates & Generation

**Epic:** [EPIC-008 Document Management System](../README.md)  
**Priority:** 🟡 P2 (Advanced Features)  
**Status:** 📋 Not Started  
**Effort:** 1 den (~800 LOC)  
**Dependencies:** DMS-001 (Versioning), DMS-002 (Links)

---

## 🎯 Story Goal

Implementovat **document template engine** s možností:
- **Upload template files** (DOCX, ODT)
- **Field mappings** (`${entity.field}`, `${now}`, `${user.email}`)
- **Generate documents** from template + entity data
- **GUI template editor** (field mapping builder)

---

## 📊 Use Cases

### Use Case 1: Contract Generation
```yaml
# Template: contract-template.docx obsahuje:
Smlouva č. ${entity.contractNumber}

Smluvní strany:
- Dodavatel: ${entity.supplierName}, IČO: ${entity.supplierIco}
- Odběratel: ${entity.customerName}, IČO: ${entity.customerIco}

Předmět smlouvy: ${entity.subject}
Cena: ${entity.price} Kč
Platnost od: ${entity.validFrom} do: ${entity.validTo}

Datum: ${now}
Vygeneroval: ${user.fullName} (${user.email})
```

### Use Case 2: Invoice Generation
- Template: `invoice-template.xlsx`
- Entity: `Invoice` with items array
- Output: PDF invoice with line items

---

## 🛠️ Implementation Tasks

### Task 1: Database Migration (0.5h)

**File:** `backend/src/main/resources/db/migration/V6__document_templates.sql`

```sql
-- ===================================================================
-- V6: Document Templates
-- Author: Core Platform Team
-- Date: 8. listopadu 2025
-- ===================================================================

-- 1. Create document_template table
CREATE TABLE document_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id TEXT NOT NULL,
    
    -- Template metadata
    name TEXT NOT NULL,
    description TEXT,
    template_type TEXT NOT NULL,  -- CONTRACT | INVOICE | RECEIPT | REPORT | LETTER
    
    -- Template file (stored in DMS)
    template_file_id UUID NOT NULL REFERENCES document(id),
    
    -- Field mappings (JSONB)
    field_mappings JSONB NOT NULL DEFAULT '{}'::jsonb,
    -- Example:
    -- {
    --   "contractNumber": "${entity.contractNumber}",
    --   "supplierName": "${entity.supplierName}",
    --   "now": "${now}",
    --   "user.email": "${user.email}"
    -- }
    
    -- Output format
    output_format TEXT NOT NULL DEFAULT 'PDF',  -- PDF | DOCX | ODT | XLSX
    
    -- Versioning
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    -- Audit
    created_by TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_by TEXT,
    updated_at TIMESTAMPTZ,
    
    UNIQUE (tenant_id, name, version)
);

-- 2. Indexes
CREATE INDEX idx_document_template_tenant ON document_template(tenant_id);
CREATE INDEX idx_document_template_type ON document_template(template_type);
CREATE INDEX idx_document_template_active ON document_template(is_active) WHERE is_active = true;

COMMENT ON TABLE document_template IS 'Document templates for generation (contracts, invoices, reports...)';
COMMENT ON COLUMN document_template.field_mappings IS 'Mapping of placeholders to entity fields (e.g., {"contractNumber": "${entity.contractNumber}"})';
```

---

### Task 2: Template Entity & Repository (1h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentTemplate.java`

```java
package cz.muriel.core.document;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentTemplate {
    private UUID id;
    private String tenantId;
    
    // Metadata
    private String name;
    private String description;
    private TemplateType templateType;
    
    // Template file
    private UUID templateFileId;
    
    // Field mappings
    private JsonNode fieldMappings;
    
    // Output
    private OutputFormat outputFormat;
    
    // Versioning
    private Integer version;
    private Boolean isActive;
    
    // Audit
    private String createdBy;
    private Instant createdAt;
    private String updatedBy;
    private Instant updatedAt;
    
    public enum TemplateType {
        CONTRACT,
        INVOICE,
        RECEIPT,
        REPORT,
        LETTER
    }
    
    public enum OutputFormat {
        PDF,
        DOCX,
        ODT,
        XLSX
    }
}
```

---

### Task 3: Template Service (3h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentTemplateService.java`

```java
package cz.muriel.core.document;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.openxml4j.opc.OPCPackage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTemplateService {
    private final DocumentService documentService;
    private final ObjectMapper objectMapper;
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    
    /**
     * Generate document from template.
     */
    public UUID generateDocument(
        UUID templateId,
        Map<String, Object> entityData,
        Map<String, Object> userData,
        String userId
    ) throws IOException {
        // 1. Load template
        DocumentTemplate template = getTemplate(templateId);
        
        // 2. Download template file
        byte[] templateBytes = documentService.downloadDocumentBytes(template.getTemplateFileId());
        
        // 3. Process template (replace placeholders)
        byte[] processedBytes = processTemplate(
            templateBytes,
            template.getFieldMappings(),
            entityData,
            userData
        );
        
        // 4. Convert to output format (if needed)
        if (template.getOutputFormat() == DocumentTemplate.OutputFormat.PDF) {
            processedBytes = convertToPdf(processedBytes);
        }
        
        // 5. Upload generated document
        String generatedFilename = String.format(
            "%s-generated-%s.%s",
            template.getName(),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")),
            template.getOutputFormat().name().toLowerCase()
        );
        
        UUID documentId = documentService.uploadBytes(
            processedBytes,
            generatedFilename,
            getMimeType(template.getOutputFormat()),
            userId
        );
        
        log.info("Generated document {} from template {} (tenant {})",
            documentId, templateId, template.getTenantId());
        
        return documentId;
    }
    
    /**
     * Process DOCX template (Apache POI).
     */
    private byte[] processTemplate(
        byte[] templateBytes,
        JsonNode fieldMappings,
        Map<String, Object> entityData,
        Map<String, Object> userData
    ) throws IOException {
        try (InputStream is = new ByteArrayInputStream(templateBytes);
             XWPFDocument document = new XWPFDocument(OPCPackage.open(is));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Prepare replacement values
            Map<String, String> replacements = buildReplacements(fieldMappings, entityData, userData);
            
            // Replace in paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                replacePlaceholders(paragraph, replacements);
            }
            
            // Replace in tables
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        for (XWPFParagraph paragraph : cell.getParagraphs()) {
                            replacePlaceholders(paragraph, replacements);
                        }
                    }
                }
            }
            
            document.write(out);
            return out.toByteArray();
        }
    }
    
    /**
     * Build replacement map from field mappings + data.
     */
    private Map<String, String> buildReplacements(
        JsonNode fieldMappings,
        Map<String, Object> entityData,
        Map<String, Object> userData
    ) {
        Map<String, String> replacements = new HashMap<>();
        
        // Add entity data
        if (entityData != null) {
            entityData.forEach((key, value) -> 
                replacements.put("${entity." + key + "}", String.valueOf(value))
            );
        }
        
        // Add user data
        if (userData != null) {
            userData.forEach((key, value) -> 
                replacements.put("${user." + key + "}", String.valueOf(value))
            );
        }
        
        // Add system variables
        replacements.put("${now}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        replacements.put("${today}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        
        return replacements;
    }
    
    /**
     * Replace placeholders in paragraph.
     */
    private void replacePlaceholders(XWPFParagraph paragraph, Map<String, String> replacements) {
        for (XWPFRun run : paragraph.getRuns()) {
            String text = run.getText(0);
            if (text == null) continue;
            
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                text = text.replace(entry.getKey(), entry.getValue());
            }
            
            run.setText(text, 0);
        }
    }
    
    /**
     * Convert DOCX to PDF (using LibreOffice).
     */
    private byte[] convertToPdf(byte[] docxBytes) throws IOException {
        // TODO: Integrate LibreOffice or Apache PDFBox
        // For now, return DOCX as-is
        return docxBytes;
    }
    
    private String getMimeType(DocumentTemplate.OutputFormat format) {
        return switch (format) {
            case PDF -> "application/pdf";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ODT -> "application/vnd.oasis.opendocument.text";
            case XLSX -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        };
    }
}
```

---

### Task 4: REST API Endpoints (1h)

**File:** `backend/src/main/java/cz/muriel/core/document/DocumentTemplateController.java`

```java
package cz.muriel.core.document;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.security.Principal;
import java.util.*;

@RestController
@RequestMapping("/api/dms/templates")
@RequiredArgsConstructor
public class DocumentTemplateController {
    private final DocumentTemplateService templateService;
    
    /**
     * List templates for tenant.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<List<DocumentTemplate>> listTemplates(
        @RequestParam(required = false) DocumentTemplate.TemplateType type
    ) {
        List<DocumentTemplate> templates = templateService.listTemplates(type);
        return ResponseEntity.ok(templates);
    }
    
    /**
     * Get template by ID.
     */
    @GetMapping("/{templateId}")
    @PreAuthorize("hasAuthority('DOCUMENT_READ')")
    public ResponseEntity<DocumentTemplate> getTemplate(
        @PathVariable UUID templateId
    ) {
        DocumentTemplate template = templateService.getTemplate(templateId);
        return ResponseEntity.ok(template);
    }
    
    /**
     * Create template.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ResponseEntity<DocumentTemplate> createTemplate(
        @RequestBody CreateTemplateRequest request,
        Principal principal
    ) {
        DocumentTemplate template = templateService.createTemplate(
            request.getName(),
            request.getDescription(),
            request.getTemplateType(),
            request.getTemplateFileId(),
            request.getFieldMappings(),
            request.getOutputFormat(),
            principal.getName()
        );
        
        return ResponseEntity.ok(template);
    }
    
    /**
     * Generate document from template.
     */
    @PostMapping("/{templateId}/generate")
    @PreAuthorize("hasAuthority('DOCUMENT_WRITE')")
    public ResponseEntity<Map<String, UUID>> generateDocument(
        @PathVariable UUID templateId,
        @RequestBody GenerateRequest request,
        Principal principal
    ) throws IOException {
        UUID documentId = templateService.generateDocument(
            templateId,
            request.getEntityData(),
            request.getUserData(),
            principal.getName()
        );
        
        return ResponseEntity.ok(Map.of("documentId", documentId));
    }
}

// ===== REQUEST DTOs =====

@Data
class CreateTemplateRequest {
    private String name;
    private String description;
    private DocumentTemplate.TemplateType templateType;
    private UUID templateFileId;
    private JsonNode fieldMappings;
    private DocumentTemplate.OutputFormat outputFormat;
}

@Data
class GenerateRequest {
    private Map<String, Object> entityData;
    private Map<String, Object> userData;
}
```

---

### Task 5: Frontend Template Editor (2h)

**File:** `frontend/src/components/TemplateEditor.tsx`

```tsx
import React, { useState } from 'react';
import {
  Box,
  TextField,
  Select,
  MenuItem,
  Button,
  Chip,
  FormControl,
  InputLabel,
  Paper,
  Typography,
  IconButton
} from '@mui/material';
import { Add, Delete } from '@mui/icons-material';
import axios from 'axios';

interface FieldMapping {
  placeholder: string;
  entityField: string;
}

export const TemplateEditor: React.FC = () => {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [templateType, setTemplateType] = useState('CONTRACT');
  const [outputFormat, setOutputFormat] = useState('PDF');
  const [fieldMappings, setFieldMappings] = useState<FieldMapping[]>([
    { placeholder: 'contractNumber', entityField: 'entity.contractNumber' }
  ]);
  
  const handleAddField = () => {
    setFieldMappings([...fieldMappings, { placeholder: '', entityField: '' }]);
  };
  
  const handleRemoveField = (index: number) => {
    setFieldMappings(fieldMappings.filter((_, i) => i !== index));
  };
  
  const handleSaveTemplate = async () => {
    // Convert to JSONB format
    const mappings = fieldMappings.reduce((acc, mapping) => {
      acc[mapping.placeholder] = `\${${mapping.entityField}}`;
      return acc;
    }, {} as Record<string, string>);
    
    try {
      await axios.post('/api/dms/templates', {
        name,
        description,
        templateType,
        templateFileId: 'TODO-upload-template-file',
        fieldMappings: mappings,
        outputFormat
      });
      
      alert('Template created!');
    } catch (error) {
      console.error('Failed to create template:', error);
    }
  };
  
  return (
    <Paper sx={{ p: 3 }}>
      <Typography variant="h5" gutterBottom>
        Create Document Template
      </Typography>
      
      <TextField
        label="Template Name"
        fullWidth
        value={name}
        onChange={(e) => setName(e.target.value)}
        sx={{ mb: 2 }}
      />
      
      <TextField
        label="Description"
        fullWidth
        multiline
        rows={3}
        value={description}
        onChange={(e) => setDescription(e.target.value)}
        sx={{ mb: 2 }}
      />
      
      <FormControl fullWidth sx={{ mb: 2 }}>
        <InputLabel>Template Type</InputLabel>
        <Select value={templateType} onChange={(e) => setTemplateType(e.target.value)}>
          <MenuItem value="CONTRACT">Contract</MenuItem>
          <MenuItem value="INVOICE">Invoice</MenuItem>
          <MenuItem value="RECEIPT">Receipt</MenuItem>
          <MenuItem value="REPORT">Report</MenuItem>
          <MenuItem value="LETTER">Letter</MenuItem>
        </Select>
      </FormControl>
      
      <FormControl fullWidth sx={{ mb: 2 }}>
        <InputLabel>Output Format</InputLabel>
        <Select value={outputFormat} onChange={(e) => setOutputFormat(e.target.value)}>
          <MenuItem value="PDF">PDF</MenuItem>
          <MenuItem value="DOCX">DOCX</MenuItem>
          <MenuItem value="ODT">ODT</MenuItem>
        </Select>
      </FormControl>
      
      <Typography variant="h6" gutterBottom>
        Field Mappings
      </Typography>
      
      {fieldMappings.map((mapping, index) => (
        <Box key={index} sx={{ display: 'flex', gap: 2, mb: 2 }}>
          <TextField
            label="Placeholder"
            value={mapping.placeholder}
            onChange={(e) => {
              const newMappings = [...fieldMappings];
              newMappings[index].placeholder = e.target.value;
              setFieldMappings(newMappings);
            }}
            placeholder="contractNumber"
          />
          
          <TextField
            label="Entity Field"
            value={mapping.entityField}
            onChange={(e) => {
              const newMappings = [...fieldMappings];
              newMappings[index].entityField = e.target.value;
              setFieldMappings(newMappings);
            }}
            placeholder="entity.contractNumber"
            sx={{ flex: 1 }}
          />
          
          <IconButton onClick={() => handleRemoveField(index)}>
            <Delete />
          </IconButton>
        </Box>
      ))}
      
      <Button
        startIcon={<Add />}
        onClick={handleAddField}
        sx={{ mb: 2 }}
      >
        Add Field
      </Button>
      
      <Box sx={{ display: 'flex', gap: 2, mt: 3 }}>
        <Button variant="contained" onClick={handleSaveTemplate}>
          Save Template
        </Button>
      </Box>
    </Paper>
  );
};
```

---

## ✅ Acceptance Criteria

- [ ] **Database Migration V6** runs successfully
- [ ] **Create Template** works
  - Upload template file (DOCX)
  - Define field mappings (GUI)
  - Save template to DB

- [ ] **Generate Document** works
  - `POST /api/dms/templates/{id}/generate` with entity data
  - Placeholders replaced correctly
  - Generated document uploaded to DMS

- [ ] **Field Mappings** support:
  - `${entity.field}` - Entity data
  - `${user.field}` - User data
  - `${now}`, `${today}` - System variables

---

## 📦 Deliverables

1. ✅ Database migration: `V6__document_templates.sql`
2. ✅ Template service: `DocumentTemplateService.java` (Apache POI integration)
3. ✅ REST API: 4 endpoints (list, get, create, generate)
4. ✅ Frontend: `TemplateEditor.tsx` component

---

**Ready for Implementation** ✅  
**Estimated Completion:** 1 den (~800 LOC)
