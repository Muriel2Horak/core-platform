---
id: DMS-005-T5
story: DMS-005
title: "Frontend Template Editor (2h)"
status: todo
assignee: ""
estimate: "2 hours"
created: 2025-11-09
updated: 2025-11-09
---

# DMS-005-T5: Frontend Template Editor (2h)

> **Parent Story:** [DMS-005](../README.md)  
> **Status:** todo | **Estimate:** 2 hours

## 🎯 Subtask Goal

Frontend Template Editor (2h)

## ✅ Acceptance Criteria

- [ ] Implementation matches parent story specification
- [ ] All files created/modified as specified
- [ ] Code follows project coding standards
- [ ] Unit tests written and passing (coverage >80%)
- [ ] Integration tests (if applicable)
- [ ] Code review approved
- [ ] Documentation updated

## 📂 Files to Modify/Create

- `frontend/src/components/TemplateEditor.tsx`

## 🔧 Implementation Details

### Code Example 1 (tsx)

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

This subtask is part of DMS-005. Complete specification including database migrations, API contracts, and UI mockups is documented in the parent story README.md.

## ✅ Definition of Done

- [ ] Code implemented per parent story
- [ ] All tests passing (unit + integration)
- [ ] Code review completed and approved
- [ ] Documentation updated (if needed)
- [ ] Committed with message: `feat(DMS-005-T5): Frontend Template Editor (2h)`
